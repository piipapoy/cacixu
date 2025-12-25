package src.presenters;

import src.views.GameView;
import src.views.MenuUtama;
import src.models.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList; // <--- IMPORT PENTING
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class GamePresenter implements Runnable {
    private GameView view;
    private JFrame window;
    private String username;
    private boolean isRunning;
    private Thread gameThread;
    
    // Player Stats
    private int posX = 380, posY = 280, pSize = 50;
    private int playerSpeed = 5;
    private int shootCooldown = 0, maxShootCooldown = 0; 
    private int burstLevel = 1, skillSlots = 1;
    private String[] skills = {"EMPTY", "EMPTY", "EMPTY"};
    private Skill[] activeSkills;
    private double lastMoveAngle = 0;
    private int footstepTimer = 0;
    private final int FOOTSTEP_DELAY = 15;
    private boolean wasMoving = false;
    
    // Skill Timers & States
    private int shieldTimer = 0, phaseTimer = 0;
    private boolean isShieldActive = false, isPhaseActive = false;
    private boolean isInvincible = false; // CACICU MODE
    private int pendingWaveJump = -1;
    private HashSet<Alien> empHitTargets = new HashSet<>();
    private boolean empActive = false;
    private int empRadius = 0;
    private int empMaxRadius = 0;
    private int empLevelCurrent = 0;

    // Dash State
    private boolean isAimingDash = false;
    private int dashMaxRange = 0;

    private boolean isTimeFreezeActive = false;
    private int timeFreezeTimer = 0;
    private double gameSpeed = 1.0; 
    private double frozenGameSpeed = 0.1;

    // Control
    private boolean up, down, left, right;
    private boolean isMouseDown = false; // TRACK MOUSE HOLD
    private StringBuilder cheatBuffer = new StringBuilder();
    private int mouseX, mouseY;
    private double aimAngle = 0;
    
    // Game Data
    private int skor = 0, meleset = 0, sisaPeluru = 0, scrap = 0;
    private int wave = 1;
    private double waveTimer = 0;
    private final double WAVE_DURATION_FRAMES = 900.0; 

    private boolean isQuickShopOpen = false, isIntermissionOpen = false, isGameOver = false;
    
    // --- UBAH TIPE LIST JADI THREAD-SAFE (CopyOnWriteArrayList) ---
    private List<Alien> listAlien = new CopyOnWriteArrayList<>();
    private List<Bullet> listBullet = new CopyOnWriteArrayList<>();
    private List<DropItem> listDrop = new CopyOnWriteArrayList<>();
    // --------------------------------------------------------------
    
    private int spawnTimer = 0, alienShootTimer = 0;

    public GamePresenter(GameView view, JFrame window, String username) {
        this.view = view;
        this.window = window;
        this.username = username;
        this.view.requestFocusInWindow(); 

        // --- INISIALISASI SKILL PERMANEN ---
        // Format: Nama, Cooldown (detik)
        activeSkills = new Skill[3];
        activeSkills[0] = new Skill("EMP", 10);   
        activeSkills[1] = new Skill("DASH", 5);   
        activeSkills[2] = new Skill("TIME", 35);
        
        // Kirim ke View biar digambar!
        view.setActiveSkills(activeSkills);
        // -----------------------------------

        initController(); 
        this.view.generateBatu(5); 
        this.view.setPlayerPos(posX, posY);
    }

    private void initController() {
        view.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                
                // CHEAT INPUT LOGIC
                char c = Character.toUpperCase(e.getKeyChar());
                if (Character.isLetter(c)) {
                    cheatBuffer.append(c);
                    if (cheatBuffer.length() > 20) cheatBuffer.delete(0, 1);
                    
                    if (cheatBuffer.toString().endsWith("TMDKEREN")) { 
                        scrap = 999; 
                        updateUI(); 
                    }
                    if (cheatBuffer.toString().endsWith("CACICU")) {
                        isInvincible = !isInvincible; 
                        cheatBuffer.setLength(0); 
                        System.out.println("RAMBO MODE: " + isInvincible);
                        updateUI();
                    }
                    if (cheatBuffer.toString().endsWith("WAVE")) {
                        cheatBuffer.setLength(0);
                        openWaveCheatMenu();
                    }
                }

                if (k == KeyEvent.VK_SPACE) { saveAndExit(); return; }
                if (isGameOver) return;
                
                if (k == KeyEvent.VK_ENTER) {
                    if (isIntermissionOpen || isQuickShopOpen) { isIntermissionOpen = false; isQuickShopOpen = false; }
                    else { isQuickShopOpen = true; }
                    updateUI(); return;
                }
                if (isIntermissionOpen || isQuickShopOpen) { handleShopInput(k); return; }

                if (k == KeyEvent.VK_LEFT) left = true; if (k == KeyEvent.VK_RIGHT) right = true;
                if (k == KeyEvent.VK_UP) up = true; if (k == KeyEvent.VK_DOWN) down = true;
            }
            @Override public void keyReleased(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_LEFT) left = false; if (k == KeyEvent.VK_RIGHT) right = false;
                if (k == KeyEvent.VK_UP) up = false; if (k == KeyEvent.VK_DOWN) down = false;
            }
        });

        view.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (isGameOver) return;

                if (SwingUtilities.isRightMouseButton(e)) {
                    isAimingDash = false;
                    updateUI();
                    return;
                }
                
                // =========================================================
                // 1. CEK KLIK SKILL PERMANEN (KIRI BAWAH) - Q, E, F
                // =========================================================
                // Koordinat: Y > 500, X antara 20 s/d 230 (Area 3 kotak kiri)
                if (e.getX() >= 20 && e.getX() <= 230) {
                    int idx = (e.getX() - 20) / 70; // Hitung indeks kolom
                    
                    if (activeSkills != null && idx >= 0 && idx < activeSkills.length) {
                        Skill s = activeSkills[idx];

                        // A. CEK AREA TOMBOL UPGRADE (Y: 480 s/d 500)
                        if (e.getY() >= 480 && e.getY() < 500) {
                            int cost = s.getCostNextLevel();
                            if (s.getLevel() < 5) {
                                if (scrap >= cost) {
                                    scrap -= cost;
                                    s.upgrade();
                                    System.out.println("UPGRADE SUKSES: " + s.getName());
                                    updateUI();
                                } else {
                                    System.out.println("UANG KURANG!");
                                }
                            }
                            return; // Stop, jangan lanjut ke bawah
                        }

                        // B. CEK AREA TOMBOL ACTIVE (Y: 500 s/d 560)
                        if (e.getY() >= 500 && e.getY() <= 560) {
                            if (s.getLevel() > 0 && s.isReady()) {
                                System.out.println("AKTIFKAN SKILL: " + s.getName());
                                
                                // --- LOGIKA PER SKILL ---
                                if (idx == 0) { // INDEX 0 = EMP
                                    performEMP(s.getLevel());
                                    s.activate(); // Mulai Cooldown
                                    updateUI();
                                }
                                if (idx == 1) { 
                                    // Masuk Mode Aiming, JANGAN langsung cooldown
                                    isAimingDash = true;
                                    // Rumus Range: Lv 1=200px, Lv 5=400px
                                    dashMaxRange = 150 + (s.getLevel() * 50); 
                                    System.out.println("DASH AIMING MODE: ON");
                                    updateUI();
                                    return; 
                                }
                                if (idx == 2) { 
                                    int lvl = s.getLevel();
                                    int durationSec = 5;
                                    int cooldownSec = 35;
                                    
                                    // SETTING PROGRESSION LEVEL
                                    switch (lvl) {
                                        case 1: 
                                            frozenGameSpeed = 0.7;
                                            durationSec = 8; 
                                            cooldownSec = 23; 
                                            break;
                                        case 2: 
                                            frozenGameSpeed = 0.5; 
                                            durationSec = 10; 
                                            cooldownSec = 25; 
                                            break;
                                        case 3: 
                                            frozenGameSpeed = 0.3; // Setengah speed
                                            durationSec = 12; 
                                            cooldownSec = 27; 
                                            break;
                                        case 4: 
                                            frozenGameSpeed = 0.2; 
                                            durationSec = 15; 
                                            cooldownSec = 30; 
                                            break;
                                        case 5: 
                                            frozenGameSpeed = 0.1; // Hampir berhenti (The World)
                                            durationSec = 20; 
                                            cooldownSec = 40; 
                                            break;
                                    }

                                    // 1. Update Cooldown Skill Sesuai Level
                                    s.setCooldown(cooldownSec); 

                                    // 2. Set Durasi Aktif (Konversi detik ke frame)
                                    timeFreezeTimer = durationSec * 60; 

                                    // 3. Aktifkan
                                    isTimeFreezeActive = true;
                                    s.activate(); 
                                    
                                    updateUI();
                                    System.out.println("ZA WARUDO! Speed: " + frozenGameSpeed + " | Durasi: " + durationSec + "s");
                                }
                            }
                            return; 
                        }
                    }
                }

                if (isAimingDash && e.getY() < 500) {
                    // 1. HITUNG SUDUT DARI TITIK TENGAH BARU (25px)
                    // (Karena player size sekarang 50, tengahnya 25)
                    double angle = Math.atan2(e.getY() - (posY + 25), e.getX() - (posX + 25));
                    double dist = Math.sqrt(Math.pow(e.getX()-(posX+25), 2) + Math.pow(e.getY()-(posY+25), 2));
                    
                    double actualDist = Math.min(dist, dashMaxRange);
                    
                    // 2. HITUNG POSISI BARU (Top-Left)
                    // Geser ke tengah (+25), gerak sejauh jarak, balikin ke pojok kiri atas (-25)
                    int newX = (int) ((posX + 25) + Math.cos(angle) * actualDist) - 25;
                    int newY = (int) ((posY + 25) + Math.sin(angle) * actualDist) - 25;
                    
                    // Boundary Check (Map 800x600)
                    // Max X dikurangin 50 (lebar player) -> 750
                    if(newX < 0) newX = 0; if(newX > 750) newX = 750;
                    if(newY < 0) newY = 0; if(newY > 530) newY = 530;

                    // --- 3. FIX CEK TABRAKAN (Box 50x50 vs Wall Dimensions) ---
                    int[][] batus = view.getBatuPosisi();
                    int[][] dims = view.getWallDimensions(); // Pake dimensi asli biar akurat
                    int[] types = view.getBatuTypes();
                    boolean nabrakBatu = false;

                    if (batus != null) {
                        for (int i = 0; i < batus.length; i++) {
                            int bW = dims[types[i]][0];
                            int bH = dims[types[i]][1];

                            // Cek Overlap: Player Box (50x50) vs Batu Box (bW x bH)
                            // Angka 30 diganti jadi 50 di sini!
                            if (newX < batus[i][0] + bW && newX + 50 > batus[i][0] && 
                                newY < batus[i][1] + bH && newY + 50 > batus[i][1]) {
                                nabrakBatu = true;
                                break;
                            }
                        }
                    }

                    if (nabrakBatu) {
                        System.out.println("GABISA TELEPORT: ADA BATU BRO!");
                        return; // BATALIN DASH
                    }
                    
                    // Kalau aman, baru pindah
                    posX = newX; 
                    posY = newY;
                    
                    activeSkills[1].activate(); 
                    isAimingDash = false;
                    
                    System.out.println("DASH EXECUTED!");
                    updateUI();
                    return; 
                }

                // =========================================================
                // 2. CEK KLIK SLOT ITEM (KANAN BAWAH) - Shield, Phase
                // =========================================================
                // Koordinat: Y > 500, X antara 580 s/d 790 (Area 3 kotak kanan)
                if (e.getY() >= 500 && e.getX() >= 580 && e.getX() <= 790) {
                    int idx = (e.getX() - 580) / 70;
                    if (idx >= 0 && idx < skillSlots) { 
                        activateSkill(idx); 
                    }
                    return; // Stop di sini, jangan nembak!
                }
                
                // =========================================================
                // 3. LOGIKA SHOOTING (Kalau klik di area main game)
                // =========================================================
                isMouseDown = true;
                if (!isInvincible) {
                    performShooting();
                }
            }
            
            @Override public void mouseReleased(MouseEvent e) {
                isMouseDown = false;
            }
        });

        view.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
            @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); } // Update aim pas ditahan
        });
    }

    private void performEMP(int level) {
        empActive = true;
        empRadius = 0;
        empLevelCurrent = level;
        empMaxRadius = (level == 5) ? 2000 : 200 + (level * 100);
        empHitTargets.clear(); // Reset daftar korban
        
        System.out.println("EMP ACTIVATED! Level: " + level);
    }

    private void openWaveCheatMenu() {
        try {
            String input = JOptionPane.showInputDialog(window, "DEBUG MODE\nJump to Wave:", String.valueOf(wave));
            if (input != null && !input.isEmpty()) {
                int targetWave = Integer.parseInt(input);
                if (targetWave > 0) {
                    this.pendingWaveJump = targetWave; 
                    System.out.println("Requesting Jump to Wave: " + targetWave);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    // --- LOGIKA NEMBAK DIPISAH BIAR BISA AUTO ---
    private void performShooting() {
        if (isIntermissionOpen || isQuickShopOpen) return;
        
        // Cek Cooldown
        if (shootCooldown > 0) return;

        if (!isInvincible && sisaPeluru <= 0) {
            src.utils.SoundPlayer.playEmptySound(); 
            return; // Gak jadi nembak
        }

        src.utils.SoundPlayer.playFireSound();

        // DOR!
        for(int i=0; i<burstLevel; i++) {
            double offset = (i - (burstLevel-1)/2.0) * 0.15;
            listBullet.add(new Bullet(posX+25, posY+25, (posX+25) + Math.cos(aimAngle+offset)*100, (posY+25) + Math.sin(aimAngle+offset)*100, false));
        }

        // Kurangi Peluru (Kecuali Mode Dewa)
        if (!isInvincible) {
            sisaPeluru--;
        }
        
        if (isInvincible) {
            shootCooldown = 1; 
        } else {
            shootCooldown = Math.max(5, 20 - maxShootCooldown); 
        }
        
        updateUI();
    }

    private void saveAndExit() {
        if (!isGameOver) {
            StringBuilder sb = new StringBuilder();
            
            // LINE 1: Stats Dasar
            sb.append(posX).append(",").append(posY).append(",")
              .append(skor).append(",").append(meleset).append(",")
              .append(sisaPeluru).append(",").append(scrap).append(",")
              .append(wave).append(",").append(waveTimer).append("\n");
            
            // LINE 2: Stats Upgrade Toko
            sb.append(playerSpeed).append(",").append(maxShootCooldown).append(",")
              .append(burstLevel).append(",").append(skillSlots).append("\n");

            // LINE 3: Skill Consumable & Slot Kanan
            sb.append(isShieldActive).append(",").append(shieldTimer).append(",")
              .append(isPhaseActive).append(",").append(phaseTimer).append(";");
            for(String s : skills) sb.append(s).append(",");
            sb.append("\n");

            // LINE 4: Posisi Batu
            int[][] batus = view.getBatuPosisi();
            for(int[] b : batus) sb.append(b[0]).append(",").append(b[1]).append(";");
            sb.append("\n");

            // LINE 5: Aliens
            for(Alien a : listAlien) sb.append(a.getX()).append(",").append(a.getY()).append(",").append(a.getHp()).append(",").append(a.getType()).append(";");
            sb.append("\n");

            // LINE 6: Bullets
            for(Bullet b : listBullet) sb.append(b.getX()).append(",").append(b.getY()).append(",").append(b.getVx()).append(",").append(b.getVy()).append(",").append(b.isFromAlien()).append(";");
            sb.append("\n");

            // LINE 7: Drops
            for(DropItem d : listDrop) sb.append(d.getX()).append(",").append(d.getY()).append(";");
            sb.append("\n");

            // LINE 8: DATA SKILL PERMANEN & CHEAT
            sb.append(isInvincible).append(";");
            for (Skill s : activeSkills) {
                sb.append(s.getLevel()).append(",");
            }
            sb.append("\n"); 

            // LINE 9: RUNTIME SKILL STATE
            sb.append(isAimingDash).append(",")
              .append(empActive).append(",").append(empRadius).append(",").append(empLevelCurrent).append(",")
              .append(isTimeFreezeActive).append(",").append(timeFreezeTimer).append(",").append(frozenGameSpeed).append(";");
            
            for (Skill s : activeSkills) {
                sb.append(s.getCooldown()).append(",");
            }
            sb.append("\n");
            
            SaveManager.saveGame(username, sb.toString());
        } else {
            SaveManager.deleteSave(username);
            dbConnection db = new dbConnection();
            db.updateScore(username, skor, meleset, sisaPeluru);
        }

        isRunning = false;
        window.dispose();
        MenuUtama newMenu = new MenuUtama();
        new MenuPresenter(newMenu);
        newMenu.setVisible(true);
    }

    // --- HELPER BARU: CEK APAKAH POSISI X,Y AMAN DARI SEMUA BATU? ---
    private boolean isValidPosition(int targetX, int targetY) {
        // 1. Cek Boundary Map dulu
        if (targetX < 0 || targetX > 750 || targetY < 0 || targetY > 530) return false;

        // 2. Ambil data terbaru
        int[][] batus = view.getBatuPosisi();
        int[][] dims = view.getWallDimensions();
        int[] types = view.getBatuTypes();

        if (batus != null) {
            for (int i = 0; i < batus.length; i++) {
                int bW = dims[types[i]][0];
                int bH = dims[types[i]][1];

                // Cek Tabrakan: Kotak Player (50x50) vs Batu
                if (targetX < batus[i][0] + bW && targetX + pSize > batus[i][0] && 
                    targetY < batus[i][1] + bH && targetY + pSize > batus[i][1]) {
                    return false; // NABRAK SATU AJA, BERARTI GAK VALID
                }
            }
        }
        return true; // AMAN, GAK NABRAK SIAPAPUN
    }

    // --- HELPER UNSTUCK VERSI RADIAL SEARCH (ANTI NYANGKUT ANTARA 2 BATU) ---
    private void unstuckPlayer() {
        // 1. Cek dulu, siapa tau posisinya udah aman (gak perlu teleport)
        if (isValidPosition(posX, posY)) return;

        System.out.println("Player Terjebak! Mencari titik aman...");

        // 2. Lakukan Pencarian Melingkar (Spiral Out)
        // Kita cari jarak 10px, 20px, ..., sampai 150px dari pusat player
        int step = 10;
        int maxDist = 200; 

        for (int dist = step; dist <= maxDist; dist += step) {
            // Cek 8 Arah Mata Angin + Diagonal
            // Urutan: Atas, Bawah, Kiri, Kanan, Pojok2
            int[][] directions = {
                {0, -dist}, {0, dist}, {-dist, 0}, {dist, 0},       // 4 Arah Utama
                {-dist, -dist}, {dist, -dist}, {-dist, dist}, {dist, dist} // 4 Diagonal
            };

            for (int[] dir : directions) {
                int tryX = posX + dir[0];
                int tryY = posY + dir[1];

                // Kalau titik ini valid (gak nabrak batu MANAPUN), pindah ke sini!
                if (isValidPosition(tryX, tryY)) {
                    posX = tryX;
                    posY = tryY;
                    view.setPlayerPos(posX, posY);
                    System.out.println("Unstuck Berhasil! Teleport ke jarak: " + dist);
                    return; // SELESAI
                }
            }
        }
        
        // Fallback terakhir kalau map penuh batu banget (jarang terjadi)
        System.out.println("Gagal Unstuck (Map Penuh?). Reset ke Tengah.");
        posX = 380; posY = 280; 
        view.setPlayerPos(posX, posY);
    }

    public void loadGame(String data) {
        try {
            String[] lines = data.split("\n");
            
            // --- LINE 1: STATS DASAR ---
            String[] stats = lines[0].split(",");
            posX = Integer.parseInt(stats[0]); posY = Integer.parseInt(stats[1]);
            skor = Integer.parseInt(stats[2]); meleset = Integer.parseInt(stats[3]);
            sisaPeluru = Integer.parseInt(stats[4]); scrap = Integer.parseInt(stats[5]);
            wave = Integer.parseInt(stats[6]); waveTimer = Double.parseDouble(stats[7]);

            // --- LINE 2: UPGRADE TOKO ---
            String[] upg = lines[1].split(",");
            playerSpeed = Integer.parseInt(upg[0]); maxShootCooldown = Integer.parseInt(upg[1]);
            burstLevel = Integer.parseInt(upg[2]); skillSlots = Integer.parseInt(upg[3]);

            // --- LINE 3: CONSUMABLES ---
            String[] skillLine = lines[2].split(";");
            String[] active = skillLine[0].split(",");
            isShieldActive = Boolean.parseBoolean(active[0]); shieldTimer = Integer.parseInt(active[1]);
            isPhaseActive = Boolean.parseBoolean(active[2]); phaseTimer = Integer.parseInt(active[3]);
            
            if (skillLine.length > 1) {
                String[] savedSkills = skillLine[1].split(",");
                for(int i=0; i<skills.length; i++) {
                    if (i < savedSkills.length) skills[i] = savedSkills[i];
                    else skills[i] = "EMPTY";
                }
            }

            // --- LINE 4: BATU ---
            if (lines.length > 3 && !lines[3].isEmpty()) {
                String[] stonePairs = lines[3].split(";");
                int[][] newBatu = new int[stonePairs.length][2];
                for(int i=0; i<stonePairs.length; i++) {
                    String[] coords = stonePairs[i].split(",");
                    newBatu[i][0] = Integer.parseInt(coords[0]);
                    newBatu[i][1] = Integer.parseInt(coords[1]);
                }
                view.setBatuPosisi(newBatu);
            }

            // --- LINE 5: ALIENS ---
            listAlien.clear();
            if (lines.length > 4 && !lines[4].isEmpty()) {
                String[] aliens = lines[4].split(";");
                for(String s : aliens) {
                    String[] aData = s.split(",");
                    Alien a = new Alien(Integer.parseInt(aData[0]), Integer.parseInt(aData[1]), aData[3]);
                    int targetHp = Integer.parseInt(aData[2]);
                    while(a.getHp() > targetHp) a.takeDamage();
                    listAlien.add(a);
                }
            }

            // --- LINE 6: BULLETS ---
            listBullet.clear();
            if (lines.length > 5 && !lines[5].isEmpty()) {
                String[] bulls = lines[5].split(";");
                for(String s : bulls) {
                    String[] bData = s.split(",");
                    double bx = Double.parseDouble(bData[0]);
                    double by = Double.parseDouble(bData[1]);
                    double bvx = Double.parseDouble(bData[2]);
                    double bvy = Double.parseDouble(bData[3]);
                    boolean isAlien = Boolean.parseBoolean(bData[4]);
                    listBullet.add(new Bullet(bx, by, bx + bvx, by + bvy, isAlien));
                }
            }

            // --- LINE 7: DROPS ---
            listDrop.clear();
            if (lines.length > 6 && !lines[6].isEmpty()) {
                String[] drops = lines[6].split(";");
                for(String s : drops) {
                    String[] dData = s.split(",");
                    listDrop.add(new DropItem(Integer.parseInt(dData[0]), Integer.parseInt(dData[1])));
                }
            }

            // --- LINE 8: SKILL PERMANEN & CHEAT ---
            if (lines.length > 7 && !lines[7].isEmpty()) {
                String[] permData = lines[7].split(";");
                this.isInvincible = Boolean.parseBoolean(permData[0]);
                
                if (permData.length > 1) {
                    String[] levels = permData[1].split(",");
                    for (int i = 0; i < activeSkills.length; i++) {
                        if (i < levels.length) {
                            int lvl = Integer.parseInt(levels[i]);
                            activeSkills[i].setLevel(lvl); 

                            if (i == 2 && lvl > 0) {
                                switch (lvl) {
                                    case 1: frozenGameSpeed = 0.8; activeSkills[i].setCooldown(35); break;
                                    case 2: frozenGameSpeed = 0.6; activeSkills[i].setCooldown(35); break;
                                    case 3: frozenGameSpeed = 0.5; activeSkills[i].setCooldown(38); break;
                                    case 4: frozenGameSpeed = 0.3; activeSkills[i].setCooldown(49); break;
                                    case 5: frozenGameSpeed = 0.1; activeSkills[i].setCooldown(50); break;
                                }
                            }
                        }
                    }
                }
            }

            if (lines.length > 8 && !lines[8].isEmpty()) {
                String[] runtimeParts = lines[8].split(";");
                
                String[] vars = runtimeParts[0].split(",");
                isAimingDash = Boolean.parseBoolean(vars[0]);
                empActive = Boolean.parseBoolean(vars[1]);
                empRadius = Integer.parseInt(vars[2]);
                empLevelCurrent = Integer.parseInt(vars[3]);

                if (empActive) {
                    empMaxRadius = (empLevelCurrent == 5) ? 2000 : 200 + (empLevelCurrent * 100);
                }
                
                isTimeFreezeActive = Boolean.parseBoolean(vars[4]);
                timeFreezeTimer = Integer.parseInt(vars[5]);
                frozenGameSpeed = Double.parseDouble(vars[6]);

                if (runtimeParts.length > 1) {
                    String[] cds = runtimeParts[1].split(",");
                    for (int i = 0; i < activeSkills.length; i++) {
                        if (i < cds.length) {
                            activeSkills[i].setCooldownTimer(Integer.parseInt(cds[i]));
                        }
                    }
                }

                if (isAimingDash) {
                    dashMaxRange = 150 + (activeSkills[1].getLevel() * 50);
                }
                
                if (isTimeFreezeActive) {
                    gameSpeed = frozenGameSpeed;
                } else {
                    gameSpeed = 1.0;
                }
                
                view.setEmpEffect(empActive, empRadius);
                view.setTimeFreezeEffect(isTimeFreezeActive);
                view.setDashState(isAimingDash, dashMaxRange);
            }
            
            updateUI();
            System.out.println("Game Loaded Successfully! Invincible: " + isInvincible);
        } catch (Exception e) {
            System.err.println("Gagal Load Save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleShopInput(int k) {
        if (k == KeyEvent.VK_ESCAPE) { isQuickShopOpen = false; isIntermissionOpen = false; }
        if (isIntermissionOpen) {
            int cs = 5+(playerSpeed-5)*5, ca = 5+(maxShootCooldown/2)*5, cb = 15*burstLevel, cl = 10*skillSlots;
            if (k == KeyEvent.VK_1 && scrap >= cs) { scrap -= cs; playerSpeed++; }
            else if (k == KeyEvent.VK_2 && scrap >= ca && maxShootCooldown < 15) { scrap -= ca; maxShootCooldown += 2; }
            else if (k == KeyEvent.VK_3 && scrap >= cb) { scrap -= cb; burstLevel++; }
            else if (k == KeyEvent.VK_4 && scrap >= cl && skillSlots < 3) { scrap -= cl; skillSlots++; }
        } else if (isQuickShopOpen) {
            if (k == KeyEvent.VK_1 && scrap >= 5) addSkill("SHIELD", 5);
            else if (k == KeyEvent.VK_2 && scrap >= 10) addSkill("PHASE", 10);
        }
        updateUI();
    }

    private void addSkill(String name, int cost) {
        for (int i = 0; i < skillSlots; i++) { if (skills[i].equals("EMPTY")) { scrap -= cost; skills[i] = name; break; } }
    }

    private void activateSkill(int idx) {
        if (skills[idx].equals("SHIELD")) { isShieldActive = true; shieldTimer = 300; skills[idx] = "EMPTY"; }
        else if (skills[idx].equals("PHASE")) { isPhaseActive = true; phaseTimer = 180; skills[idx] = "EMPTY"; }
        updateUI();
    }

    private void updateUI() {
        double progress = waveTimer / WAVE_DURATION_FRAMES;
        view.updateData(skor, meleset, sisaPeluru, scrap, wave, progress);
        view.setSkillInfo(skillSlots, skills);
        view.setShieldActive(isShieldActive); view.setPhaseActive(isPhaseActive);
        view.setInvincible(isInvincible); 
        view.setGameOver(isGameOver);
        
        String info = isIntermissionOpen ? "[1] Spd\n[2] AtkSpd\n[3] Burst\n[4] Slot" : "[1] Shield\n[2] Phase";
        view.setShopStates(isQuickShopOpen, isIntermissionOpen, info);
        view.setDashState(isAimingDash, dashMaxRange);

        // --- KIRIM STATS PLAYER KE POJOK KANAN ATAS ---
        int lvlSpeed = playerSpeed - 4; 
        int lvlAtk = 1 + (maxShootCooldown / 2);
        int lvlBurst = burstLevel;
        view.setPlayerStats(lvlSpeed, lvlAtk, lvlBurst);

        boolean canVisualShoot = isMouseDown && (sisaPeluru > 0 || isInvincible);
        
        view.setIsShooting(canVisualShoot);
    }

    public void startGame() { isRunning = true; gameThread = new Thread(this); gameThread.start(); }
    
    @Override public void run() {
        while (isRunning) { 
            try {
                if (!isIntermissionOpen && !isQuickShopOpen && !isGameOver) {
                    update(); 
                }
                view.repaint(); 
                Thread.sleep(16); 
            } catch (InterruptedException e) {
            } catch (Exception e) {
                System.err.println("CRASH DI GAME LOOP: " + e.getMessage());
                e.printStackTrace(); 
            }
        }
    }

    // --- LOGIKA UTAMA (UPDATE) DENGAN SAFE ITERATION ---
    private void update() {
        // 1. Hitung rencana posisi baru berdasarkan input keyboard
        int nX = posX, nY = posY;
        if (left) nX -= playerSpeed; if (right) nX += playerSpeed;
        if (up) nY -= playerSpeed; if (down) nY += playerSpeed;

        boolean canX = true, canY = true;

        boolean isMoving = (left || right || up || down);
        view.setIsMoving(isMoving);

        if (isMoving && !wasMoving) {
            // TRANSISI: Dari Diem -> Jalan
            // NYALAIN LOOP
            src.utils.SoundPlayer.startStep();
        } else if (!isMoving && wasMoving) {
            // TRANSISI: Dari Jalan -> Diem
            // CUT / STOP AUDIO
            src.utils.SoundPlayer.stopStep();
        }

        // Simpan status sekarang buat frame selanjutnya
        wasMoving = isMoving;

        // 2. Ambil data batu dan dimensi dinamis dari view
        int[][] batus = view.getBatuPosisi();
        int[][] dims = view.getWallDimensions(); // Data {W, H}
        int[] types = view.getBatuTypes();       // Tipe wall1-5

        if (pendingWaveJump != -1) {
            this.wave = pendingWaveJump;
            this.waveTimer = 0;
            listAlien.clear(); listBullet.clear(); listDrop.clear();
            isIntermissionOpen = false; isQuickShopOpen = false;
            pendingWaveJump = -1; 
        }

        // --- 1. LOGIKA WAKTU (GAME SPEED) ---
        if (isTimeFreezeActive) {
            gameSpeed = frozenGameSpeed; 
            timeFreezeTimer--;
            if (timeFreezeTimer <= 0) {
                isTimeFreezeActive = false;
                gameSpeed = 1.0; 
                System.out.println("Time Resume.");
            }
        } else {
            gameSpeed = 1.0;
        }
        view.setTimeFreezeEffect(isTimeFreezeActive);

        waveTimer++;
        if (waveTimer >= WAVE_DURATION_FRAMES) { wave++; waveTimer = 0; if (wave % 5 == 0) isIntermissionOpen = true; }
        if (shootCooldown > 0) shootCooldown--;
        if (isShieldActive) { shieldTimer--; if (shieldTimer <= 0) isShieldActive = false; }
        updateUI();

        for (Skill s : activeSkills) s.update();

        if (isInvincible && isMouseDown) performShooting();
        
        if (!isPhaseActive && batus != null) {
            for (int i = 0; i < batus.length; i++) {
                int bW = dims[types[i]][0];
                int bH = dims[types[i]][1];
                
                // Cek sumbu X
                if (nX < batus[i][0] + bW && nX + pSize > batus[i][0] && 
                    posY < batus[i][1] + bH && posY + pSize > batus[i][1]) canX = false;
                
                // Cek sumbu Y
                if (posX < batus[i][0] + bW && posX + pSize > batus[i][0] && 
                    nY < batus[i][1] + bH && nY + pSize > batus[i][1]) canY = false;
            }
        }

        if (canX) posX = nX;
        if (canY) posY = nY;

        if (isPhaseActive) { 
            phaseTimer--; 
            if (phaseTimer <= 0) { 
                unstuckPlayer(); // <--- INI PENTING
                isPhaseActive = false; 
                System.out.println("Phase Deactivated. Unstuck Check Complete.");
                
                // [OPTIONAL] Force Update posisi biar gak ketimpa nX yang lama
                // return; 
            } 
        }

        
        view.setPlayerPos(posX, posY);
        aimAngle = Math.atan2(mouseY - (posY + 25), mouseX - (posX + 25));
        view.setAimAngle(aimAngle);

        double mouseTheta = Math.atan2(mouseY - (posY + 25), mouseX - (posX + 25));
        
        // 2. Tentukan Sudut Badan
        double bodyTheta = mouseTheta; // Default (jaga-jaga)

        if (isMouseDown) {
            // KALO NEMBAK: Badan maksa ngadep Mouse (Strafing Mode)
            bodyTheta = mouseTheta;
        } else {
            // KALO GAK NEMBAK: Badan ngikutin arah jalan (WASD)
            int dx = 0, dy = 0;
            if (right) dx = 1;
            if (left) dx = -1;
            if (down) dy = 1;
            if (up) dy = -1;

            if (dx != 0 || dy != 0) {
                // Hitung sudut berdasarkan tombol yang ditekan
                bodyTheta = Math.atan2(dy, dx);
            } else {
                // Kalo diem, pake sudut terakhir yang dikirim ke view
                // (Kita skip update setPlayerAngle biar dia gak reset ke kanan)
                // Tapi karena kita harus kirim sesuatu, kita bisa simpen lastAngle
                // Atau biarin aja logic di view nahan gambar terakhir.
                // Disini kita pake trik: ambil aimAngle dari view kalo diem? 
                // Ah ribet, kita pake variabel global di presenter aja buat nyimpen lastMoveAngle.
            }
        }
        
        // UPDATE: Perlu variabel global 'lastMoveAngle' di Presenter biar pas diem gak reset
        if (!isMouseDown && (left || right || up || down)) {
             lastMoveAngle = bodyTheta;
        } else if (isMouseDown) {
             lastMoveAngle = mouseTheta;
        }

        // Kirim Data ke View
        view.setPlayerPos(posX, posY);
        view.setAimAngle(mouseTheta); // Arrow selalu ke Mouse
        view.setPlayerAngle(lastMoveAngle);

        // 3. Spawn Alien & Tembak
        spawnTimer++;
        if (listAlien.size() < 50 && spawnTimer >= Math.max(30, 120-(wave*5))) {
            int spawnCount = Math.min(4, 1 + (wave / 5)); 
            for(int i=0; i < spawnCount; i++) {
                listAlien.add(new Alien((int)(Math.random()*750), 650+(i*40), getRandomType())); 
            }
            spawnTimer = 0; 
        }

        // --- LOGIKA ALIEN SHOOT REVISI (SUPPLY & DEMAND) ---
        alienShootTimer++;
        int baseDelay = Math.max(15, 40 - (wave / 2)); 

        if (alienShootTimer >= baseDelay && !listAlien.isEmpty()) {
            Alien s = listAlien.get((int)(Math.random() * listAlien.size()));
            
            int bulletCount = 1;
            String type = s.getType();
            if (type.equals("Zyphor")) bulletCount = 3; 
            else if (type.equals("Noxar")) bulletCount = 2; 
            else if (type.equals("Virex") && wave > 10) bulletCount = 2; 
            
            double angleToPlayer = Math.atan2((posY+15) - (s.getY()+15), (posX+15) - (s.getX()+15));

            for (int i = 0; i < bulletCount; i++) {
                double spread = (bulletCount > 1) ? (i - (bulletCount-1)/2.0) * 0.25 : 0;
                double finalAngle = angleToPlayer + spread;
                
                double tX = s.getX() + 15 + Math.cos(finalAngle) * 200;
                double tY = s.getY() + 15 + Math.sin(finalAngle) * 200;
                
                listBullet.add(new Bullet(s.getX()+15, s.getY()+15, tX, tY, true));
            }
            alienShootTimer = 0;
        }

        // 4. Logika Peluru (SAFE ITERATION FOR CopyOnWriteArrayList)
        for (Bullet b : listBullet) { // Pake For-Loop biasa, bukan Iterator
            if (b.isFromAlien()) {
                b.move(gameSpeed); 
            } else {
                b.move(1.0); 
            }
            
            boolean removeMe = false;
            
            if (b.getX() < -50 || b.getX() > 850 || b.getY() < -50 || b.getY() > 650) { 
                if (b.isFromAlien()) { meleset++; sisaPeluru++; updateUI(); } 
                removeMe = true; 
            }
            
            if (!removeMe) {
                for (int i = 0; i < batus.length; i++) { 
                    // Ambil lebar dan tinggi dinamis untuk tiap batu
                    int bW = dims[types[i]][0];
                    int bH = dims[types[i]][1];

                    if (b.getX() > batus[i][0] && b.getX() < batus[i][0] + bW && 
                        b.getY() > batus[i][1] && b.getY() < batus[i][1] + bH) { 
                        if (b.isFromAlien()) { meleset++; sisaPeluru++; updateUI(); } 
                        removeMe = true; 
                        break; 
                    } 
                }
            }
            
            if (!removeMe) {
                if (b.isFromAlien()) {
                    if (Math.sqrt(Math.pow(b.getX()-(posX+25), 2) + Math.pow(b.getY()-(posY+25), 2)) < 25) {
                        if (isShieldActive || isPhaseActive || isInvincible) { 
                            removeMe = true; 
                        } else { 
                            isGameOver = true; updateUI(); 
                            src.utils.SoundPlayer.stopStep();
                            view.setIsMoving(false);
                            src.utils.SoundPlayer.playDeadSound();
                            updateUI();
                        }
                    }
                } else {
                    for (Alien a : listAlien) { 
                        // CEK TABRAKAN PELURU PLAYER DENGAN ALIEN 'a'
                        if (Math.sqrt(Math.pow(b.getX()-(a.getX()+15), 2) + Math.pow(b.getY()-(a.getY()+15), 2)) < 20) {
                            a.takeDamage(); 
                            removeMe = true;
                            if (a.isDead()) { 
                                if (Math.random() < 0.6) listDrop.add(new DropItem(a.getX()+10, a.getY()+10)); 
                                listAlien.remove(a); 
                                skor += 10; 
                            }
                            updateUI(); 
                            break; 
                        }
                    }
                }
            }
            // Hapus peluru jika menabrak sesuatu
            if (removeMe) listBullet.remove(b);
        }

        // 5. Ambil Scrap (SAFE ITERATION)
        for (DropItem d : listDrop) {
            if (Math.sqrt(Math.pow(posX+15-d.getX(), 2) + Math.pow(posY+15-d.getY(), 2)) < 25) { 
                scrap++; updateUI(); 
                listDrop.remove(d); // Hapus Drop Aman
            }
        }

        // 6. Alien Move (SAFE ITERATION)
        for (Alien al : listAlien) {
            al.move(posX, posY, batus, dims, types, listAlien, gameSpeed);
            double pCX = posX + 25;
            double pCY = posY + 25;
            double aCX = al.getX() + 15;
            double aCY = al.getY() + 15;

            // Jarak aman: Radius Player (20) + Radius Alien (15) = 35
            if (Math.sqrt(Math.pow(pCX - aCX, 2) + Math.pow(pCY - aCY, 2)) < 35) {
                if (!isShieldActive && !isPhaseActive && !isInvincible) { 
                    isGameOver = true; updateUI(); 
                }
            }
        }
        
        view.setAliens(listAlien); view.setBullets(listBullet); view.setDrops(listDrop);
        
        // 7. EMP Logic (SAFE ITERATION)
        if (empActive) {
            empRadius += 20 * gameSpeed; 
            
            for (Bullet b : listBullet) {
                if (b.isFromAlien()) {
                    double dist = Math.sqrt(Math.pow(b.getX()-(posX+15), 2) + Math.pow(b.getY()-(posY+15), 2));
                    if (dist < empRadius) listBullet.remove(b); 
                }
            }

            for (Alien a : listAlien) {
                if (empHitTargets.contains(a)) continue; 
                double dist = Math.sqrt(Math.pow(a.getX() - posX, 2) + Math.pow(a.getY() - posY, 2));
                
                if (dist < empRadius) {
                    empHitTargets.add(a); 
                    int damage = (empLevelCurrent == 5) ? 5000 : empLevelCurrent;
                    int dealt = 0;
                    while(dealt < damage && a.getHp() > 0) { a.takeDamage(); dealt++; }

                    if (a.getHp() <= 0) {
                        if (Math.random() < 0.6) listDrop.add(new DropItem(a.getX()+10, a.getY()+10));
                        skor += 10; 
                        listAlien.remove(a); // Hapus Alien Aman
                        continue; 
                    }

                    double angle = Math.atan2(a.getY() - posY, a.getX() - posX);
                    double force = (empLevelCurrent == 5) ? 60.0 : 10.0 + (empLevelCurrent * 5.0);
                    a.applyKnockback(force, angle);
                }
            }

            if (empRadius > empMaxRadius) empActive = false;
        }
        view.setEmpEffect(empActive, empRadius);
    }

    // --- BALANCING BARU: "COMFORT CURVE" (Unlock per 5 Wave) ---
    private String getRandomType() {
        int wGrim = Math.max(15, 120 - (wave * 2)); 
        int wSkarn = (wave < 5) ? 0 : Math.min(90, (wave - 4) * 10);
        int wVirex = (wave < 10) ? 0 : Math.min(80, (wave - 9) * 10);
        int wNoxar = (wave < 15) ? 0 : Math.min(70, (wave - 14) * 10);
        int wZyphor = (wave < 20) ? 0 : Math.min(60, (wave - 19) * 10);

        int totalWeight = wGrim + wSkarn + wVirex + wNoxar + wZyphor;
        int r = (int)(Math.random() * totalWeight);

        if (r < wGrim) return "Grim";
        r -= wGrim;
        if (r < wSkarn) return "Skarn";
        r -= wSkarn;
        if (r < wVirex) return "Virex";
        r -= wVirex;
        if (r < wNoxar) return "Noxar";
        
        return "Zyphor";
    }
}