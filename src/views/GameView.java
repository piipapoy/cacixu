package src.views;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import src.models.*; // Import model termasuk Skill
import java.awt.geom.AffineTransform;

public class GameView extends JPanel {
    private int skor, meleset, sisaPeluru, scrap, wave;
    private double waveProgress;
    private int playerX, playerY; 
    private double angle = 0;
    private double playerAngle = 0;
    private int[][] batuPosisi; 
    private List<Alien> listAlien;
    private List<Bullet> listBullet;
    private List<DropItem> listDrop;
    private int bSize = 50;

    private Image pFwd1, pFwd2, pLeft1, pLeft2, pRight1, pRight2, pBack1, pBack2;
    private int animTimer = 0;
    private int animFrame = 0;

    private Image aimNormalImg, aimDashImg;

    private Image[] pShootFwd = new Image[4];
    private Image[] pShootLeft = new Image[4];
    private Image[] pShootRight = new Image[4];
    private Image pShootBack;
    private boolean isShooting = false;
    private boolean isShootingAnimLocked = false;
    private int shootAnimTimer = 0;
    private int shootAnimFrame = 0;
    public void setIsShooting(boolean shooting) {
        if (shooting && !this.isShooting) {
            // Paksa Reset Animasi ke Awal
            shootAnimFrame = 0;
            shootAnimTimer = 0;
            isShootingAnimLocked = true; // Langsung kunci biar frame 0 ke-render
        }
        
        this.isShooting = shooting; 
    }

    private Image bulletPlayerImg;
    private Image playerDeadImg;

    // Status Toko & Game Over
    private boolean showQuickShop = false, showIntermissionShop = false, isGameOver = false;
    private String shopInfo = "";
    
    // Skill Pasif/Consumable (Kanan Bawah)
    private int skillSlots = 1;
    private String[] currentSkills = {"EMPTY", "EMPTY", "EMPTY"};
    
    // Status Player
    private boolean isShieldActive = false, isPhaseActive = false;
    private boolean isInvincible = false; // Visual Aura Dewa

    private int dispSpeedLevel = 1;
    private int dispAtkSpeedLevel = 1;
    private int dispBurstLevel = 1;

    private Image[] footStepLR = new Image[3];
    private Image[] footStepUD = new Image[3];
    private boolean isMoving = false; // Status apakah player lagi jalan?
    private int footAnimTimer = 0;
    private int footAnimFrame = 0;
    
    // Setter buat Presenter
    public void setIsMoving(boolean moving) { this.isMoving = moving; }

    // DATA SKILL PERMANEN (Kiri Bawah) - Baru
    private Skill[] activeSkills;

    // Variabel Animasi EMP
    private boolean isEmpActive = false;
    private int empRadius = 0;
    private int empMaxRadius = 0;

    private boolean isTimeFreezeActive = false;
    public void setTimeFreezeEffect(boolean active) { this.isTimeFreezeActive = active; }

    private boolean isAimingDash = false;
    private int dashRange = 0;

    private Image groundImg;
    private Image[] wallImgs = new Image[5];
    private int[][] wallDimensions = {
        {70, 48}, {70, 50}, {61, 70}, {70, 63}, {55, 70}
    };
    private int[] batuType;

    public void setPlayerAngle(double angle) { this.playerAngle = angle; }

    private void loadTextures() {
        try {
            groundImg = new ImageIcon("src/assets/textures/maps/ground.png").getImage();
            for(int i=0; i<5; i++) {
                wallImgs[i] = new ImageIcon("src/assets/textures/maps/wall" + (i+1) + ".png").getImage();
            }

            pFwd1 = new ImageIcon("src/assets/textures/players/forward1.png").getImage();
            pFwd2 = new ImageIcon("src/assets/textures/players/forward2.png").getImage();
            
            pLeft1 = new ImageIcon("src/assets/textures/players/left1.png").getImage();
            pLeft2 = new ImageIcon("src/assets/textures/players/left2.png").getImage();
            
            pRight1 = new ImageIcon("src/assets/textures/players/right1.png").getImage();
            pRight2 = new ImageIcon("src/assets/textures/players/right2.png").getImage();
            
            pBack1 = new ImageIcon("src/assets/textures/players/back1.png").getImage();
            pBack2 = new ImageIcon("src/assets/textures/players/back2.png").getImage();

            aimNormalImg = new ImageIcon("src/assets/textures/ui/arrowFire.png").getImage();
            aimDashImg = new ImageIcon("src/assets/textures/ui/arrowDash.png").getImage();

            for(int i=0; i<4; i++) {
                pShootFwd[i] = new ImageIcon("src/assets/textures/players/forwardShoot" + (i+1) + ".png").getImage();
            }
            
            // Left (1-4)
            for(int i=0; i<4; i++) {
                pShootLeft[i] = new ImageIcon("src/assets/textures/players/leftShoot" + (i+1) + ".png").getImage();
            }

            // Right (1-4)
            for(int i=0; i<4; i++) {
                pShootRight[i] = new ImageIcon("src/assets/textures/players/rightShoot" + (i+1) + ".png").getImage();
            }

            // Back (Cuma 1)
            pShootBack = new ImageIcon("src/assets/textures/players/backShoot.png").getImage();

            bulletPlayerImg = new ImageIcon("src/assets/textures/weapons/bulletPlayer.png").getImage();

            for(int i=0; i<3; i++) {
                // Load LR (Left Right)
                footStepLR[i] = new ImageIcon("src/assets/textures/players/LRfootStep" + (i+1) + ".png").getImage();
                
                // Load UD (Up Down)
                footStepUD[i] = new ImageIcon("src/assets/textures/players/UDfootStep" + (i+1) + ".png").getImage();
            }

            playerDeadImg = new ImageIcon("src/assets/textures/players/dead.png").getImage();

        } catch(Exception e) { e.printStackTrace(); }
    }

    public void setPlayerStats(int speedLvl, int atkSpdLvl, int burstLvl) {
        this.dispSpeedLevel = speedLvl;
        this.dispAtkSpeedLevel = atkSpdLvl;
        this.dispBurstLevel = burstLvl;
    }

    // Setter buat Presenter
    public void setDashState(boolean aiming, int range) {
        this.isAimingDash = aiming;
        this.dashRange = range;
    }

    public GameView(JFrame window) { 
        this.setBackground(Color.BLACK); 
        this.setFocusable(true); 
        loadTextures();
    }

    // --- SETTERS ---
    public void setPlayerPos(int x, int y) { this.playerX = x; this.playerY = y; }
    public void setAimAngle(double angle) { this.angle = angle; }
    public void setAliens(List<Alien> aliens) { this.listAlien = aliens; }
    public void setBullets(List<Bullet> bullets) { this.listBullet = bullets; }
    public void setDrops(List<DropItem> drops) { this.listDrop = drops; }
    
    public void setEmpEffect(boolean active, int radius) {
        this.isEmpActive = active;
        this.empRadius = radius;
    }

    // Update Info UI Utama
    public void updateData(int skor, int meleset, int sisa, int scrap, int wave, double progress) { 
        this.skor = skor; this.meleset = meleset; 
        this.sisaPeluru = sisa; this.scrap = scrap; 
        this.wave = wave; this.waveProgress = progress;
    }

    // Info Slot Kanan (Consumables)
    public void setSkillInfo(int slots, String[] skills) { this.skillSlots = slots; this.currentSkills = skills; }
    
    // Status Aktif Visual
    public void setShieldActive(boolean active) { this.isShieldActive = active; }
    public void setPhaseActive(boolean active) { this.isPhaseActive = active; }
    public void setInvincible(boolean active) { this.isInvincible = active; }
    public void setGameOver(boolean state) { this.isGameOver = state; }
    
    // Setter Skill Permanen (Kiri Bawah)
    public void setActiveSkills(Skill[] skills) { 
        this.activeSkills = skills; 
    }

    // Shop Overlay Info
    public void setShopStates(boolean quick, boolean intermission, String info) {
        this.showQuickShop = quick; this.showIntermissionShop = intermission; this.shopInfo = info;
    }

    // Batu Logic (Dipake buat Save/Load juga)
    public int[][] getBatuPosisi() { return batuPosisi; }
    public int getBatuSize() { return bSize; }
    public void setBatuPosisi(int[][] savedBatu) {
        this.batuPosisi = savedBatu;
        this.repaint();
    }

    public void generateBatu(int jumlah) {
        batuPosisi = new int[jumlah][2];
        batuType = new int[jumlah];
        int count = 0;
        while (count < jumlah) {
            int nX = (int) (Math.random() * 650) + 50; 
            int nY = (int) (Math.random() * 300) + 120;
            
            boolean overlap = false; // <--- TAMBAHKAN DEKLARASI INI
            
            for (int i = 0; i < count; i++) {
                if (Math.sqrt(Math.pow(nX - batuPosisi[i][0], 2) + Math.pow(nY - batuPosisi[i][1], 2)) < 120) overlap = true;
            }
            
            if (Math.sqrt(Math.pow(nX - 380, 2) + Math.pow(nY - 280, 2)) < 150) overlap = true;

            if (!overlap) { 
                batuPosisi[count][0] = nX; 
                batuPosisi[count][1] = nY; 
                batuType[count] = (int)(Math.random() * 5);
                count++; 
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (groundImg != null) {
            // Gambar tanah (stretch ke 800x600)
            g.drawImage(groundImg, 0, 0, 800, 600, null);
        }

        if (batuPosisi != null) {
            for (int i = 0; i < batuPosisi.length; i++) {
                int type = batuType[i];
                int w = wallDimensions[type][0];
                int h = wallDimensions[type][1];
                g.drawImage(wallImgs[type], batuPosisi[i][0], batuPosisi[i][1], w, h, null);
                g.setColor(Color.RED);
                g.drawRect(batuPosisi[i][0], batuPosisi[i][1], w, h);
            }
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ==========================================
        // ANIMASI FOOTSTEP (LR & UD SUPPORT)
        // ==========================================
        if (isMoving) {
            footAnimTimer++;
            if (footAnimTimer > 5) { 
                footAnimFrame = (footAnimFrame + 1) % 3; 
                footAnimTimer = 0;
            }
            
            // 1. Hitung Posisi Pusat Kaki
            double footCenterX = playerX + 25; 
            double footCenterY = playerY + 35; 
            
            // Titik tengah debu (selalu di belakang arah gerak)
            double dustAngle = playerAngle + Math.PI; 
            int offsetDist = 25;
            int midX = (int) (footCenterX + Math.cos(dustAngle) * offsetDist);
            int midY = (int) (footCenterY + Math.sin(dustAngle) * offsetDist);

            double deg = Math.toDegrees(playerAngle);
            Image imgToDraw = null;
            
            // Mode Render: 0=Normal, 1=Flip, 2=UD(TopDown), 3=Rotated(Diagonal)
            int renderMode = 0; 
            double rotationRad = 0;
            boolean diagonalFlip = false;

            // --- LOGIKA 8 ARAH (Threshold 22.5 derajat biar presisi) ---
            
            // 1. KANAN (Range -22.5 s/d 22.5)
            if (deg > -22.5 && deg <= 22.5) {
                imgToDraw = footStepLR[footAnimFrame];
                renderMode = 1; // Flip Horizontal (Debu ke Kiri)
            }
            // 2. KANAN BAWAH (Diagonal SE: 22.5 s/d 67.5)
            else if (deg > 22.5 && deg <= 67.5) {
                imgToDraw = footStepLR[footAnimFrame];
                renderMode = 3; // Rotated
                // Target: NW (-135). Gambar Asli: NE (-45). Selisih: -90
                rotationRad = Math.toRadians(-90); 
            }
            // 3. BAWAH (Range 67.5 s/d 112.5)
            else if (deg > 67.5 && deg <= 112.5) {
                imgToDraw = footStepUD[footAnimFrame];
                renderMode = 2; // UD (Geser Kanan dikit)
                midX += 6; 
            }
            // 4. KIRI BAWAH (Diagonal SW: 112.5 s/d 157.5)
            else if (deg > 112.5 && deg <= 157.5) {
                imgToDraw = footStepLR[footAnimFrame];
                renderMode = 3; // Rotated
                // Target: NE (-45). Gambar Asli: NE (-45). Selisih: 0
                rotationRad = Math.toRadians(0); // Pake gambar asli
            }
            // 5. KIRI (Range 157.5 s/d 180 atau -180 s/d -157.5)
            else if (deg > 157.5 || deg <= -157.5) {
                imgToDraw = footStepLR[footAnimFrame];
                renderMode = 0; // Normal (Debu ke Kanan Atas dikit, looks natural)
            }
            // 6. KIRI ATAS (Diagonal NW: -157.5 s/d -112.5)
            else if (deg > -157.5 && deg <= -112.5) {
                imgToDraw = footStepLR[footAnimFrame];
                renderMode = 3; // Rotated
                // Target: SE (45). Gambar Asli: NE (-45). Selisih: 90
                rotationRad = Math.toRadians(45);
                midX += 15;
            }
            // 7. ATAS (Range -112.5 s/d -67.5)
            else if (deg > -112.5 && deg <= -67.5) {
                imgToDraw = footStepUD[footAnimFrame];
                renderMode = 2; // UD
                midX += 6;
            }
            // 8. KANAN ATAS (Diagonal NE: -67.5 s/d -22.5)
            else if (deg > -67.5 && deg <= -22.5) {
                imgToDraw = footStepLR[footAnimFrame];
                renderMode = 3; // Rotated
                // Target: SW (135). Gambar Asli: NE (-45). Selisih: 180
                rotationRad = Math.toRadians(-45);
                diagonalFlip = true;
                midX -= 12;
            }

            // --- EKSEKUSI RENDER ---
            if (imgToDraw != null) {
                if (renderMode == 3) {
                    // --- DIAGONAL (ROTASI) ---
                    AffineTransform old = g2.getTransform();
                    g2.translate(midX, midY); // Pindah ke titik debu
                    g2.rotate(rotationRad);   // Putar sesuai diagonal

                    if (diagonalFlip) {
                        // GAMBAR FLIP (X mulai dari 15, Lebar -30)
                        g2.drawImage(imgToDraw, 15, -15, -30, 30, null);
                    } else {
                        // GAMBAR NORMAL
                        g2.drawImage(imgToDraw, -15, -15, 30, 30, null);
                    }

                    g2.setTransform(old);     // Reset
                } 
                else if (renderMode == 1) {
                    // --- FLIP (KANAN) ---
                    g.drawImage(imgToDraw, midX + 15, midY - 15, -30, 30, null);
                } 
                else {
                    // --- NORMAL (KIRI / UD) ---
                    g.drawImage(imgToDraw, midX - 15, midY - 15, 30, 30, null);
                }
            }
            
        } else {
            footAnimFrame = 0;
            footAnimTimer = 0;
        }

        // --- HUD ATAS ---
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("WAVE: " + wave, 20, 25);
        g.setColor(new Color(60, 60, 60)); g.fillRect(20, 35, 120, 8);
        g.setColor(Color.CYAN); g.fillRect(20, 35, (int)(waveProgress * 120), 8);
        g.setColor(Color.WHITE);
        g.drawString("SCRAP: " + scrap, 20, 65);
        g.drawString("SCORE: " + skor, 20, 85);
        g.setColor(sisaPeluru > 0 ? Color.GREEN : Color.RED);
        g.drawString("AMMO : " + sisaPeluru, 20, 105);
        
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(">>> TEKAN [ENTER] BUKA SHOP | [SPACE] EXIT <<<", 240, 25);

        // ============================================================
        // BAGIAN INI YANG PENTING (KIRI DAN KANAN ADA SEMUA)
        // ============================================================

        // --- HUD BAWAH KIRI (Active Skills - Q, E, F) ---
        // INI YANG TADI ILANG
        if (activeSkills != null) {
            for (int i = 0; i < activeSkills.length; i++) {
                Skill s = activeSkills[i];
                int bx = 20 + (i * 70); 
                int by = 500;           
                int size = 60;

                // --- 1. GAMBAR TOMBOL UPGRADE (Hanya jika belum MAX) ---
                // Posisi: Di atas kotak utama (Y = 480 s/d 500)
                if (s.getLevel() < 5) {
                    // Cek duit cukup gak? Kalau cukup Hijau, kalau kurang Merah/Abu
                    if (scrap >= s.getCostNextLevel()) g.setColor(new Color(0, 180, 0)); // Hijau (Bisa Beli)
                    else g.setColor(new Color(100, 100, 100)); // Abu (Gak cukup)
                    
                    g.fillRect(bx, by - 20, size, 18); // Kotak Upgrade
                    g.setColor(Color.WHITE);
                    g.drawRect(bx, by - 20, size, 18); // Border
                    
                    // Teks Harga
                    g.setFont(new Font("Arial", Font.BOLD, 10));
                    g.drawString("UP: " + s.getCostNextLevel(), bx + 5, by - 8);
                } else {
                    // Kalau MAX Level
                    g.setColor(Color.ORANGE);
                    g.setFont(new Font("Arial", Font.BOLD, 10));
                    g.drawString("MAXED", bx + 10, by - 8);
                }

                // --- 2. KOTAK UTAMA (ACTIVATION) ---
                // Background
                if (s.getLevel() == 0) {
                    g.setColor(new Color(30, 30, 30)); // Locked
                } else {
                    if (i == 0) g.setColor(new Color(0, 255, 255)); 
                    else if (i == 1) g.setColor(new Color(255, 215, 0)); 
                    else g.setColor(new Color(255, 0, 255)); 
                }
                g.fillRect(bx, by, size, size);

                // Cooldown Overlay
                if (s.getLevel() > 0 && s.getCooldown() > 0) {
                    float ratio = s.getCooldownRatio();
                    g.setColor(new Color(0, 0, 0, 180));
                    int h = (int)(size * ratio);
                    g.fillRect(bx, by + (size - h), size, h); 
                }

                // Border & Text Skill
                g.setColor(Color.WHITE);
                g.drawRect(bx, by, size, size);
                
                g.setFont(new Font("Arial", Font.BOLD, 10));
                String label = (i == 0) ? "EMP" : (i == 1) ? "DASH" : "SWARM";
                g.drawString(label, bx + 5, by + 15);

                // Info Level di dalam kotak
                g.setFont(new Font("Arial", Font.BOLD, 20));
                if (s.getLevel() == 0) {
                    g.setColor(Color.GRAY);
                    g.drawString("LOCK", bx + 5, by + 40);
                } else {
                    g.setColor(Color.WHITE);
                    g.drawString("LV " + s.getLevel(), bx + 10, by + 40);
                }
            }
        }

        // --- HUD BAWAH KANAN (Item Slots - Consumables) ---
        // INI YANG BARU
        for (int i = 0; i < 3; i++) { 
            int bx = 580 + (i * 70); // Posisi X mulai 580
            int by = 500;            // Posisi Y 500 (Sejajar)
            int size = 60;

            // 1. Background
            if (i < skillSlots) g.setColor(new Color(50, 50, 50)); 
            else g.setColor(new Color(20, 20, 20)); // Locked Gelap
            g.fillRect(bx, by, size, size);

            // 2. Border
            g.setColor(Color.WHITE);
            g.drawRect(bx, by, size, size);

            // 3. Text
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 9));
            g.drawString("SLOT " + (i+1), bx + 5, by + 12);

            g.setFont(new Font("Arial", Font.BOLD, 11));
            if (i < skillSlots) {
                String itemName = currentSkills[i];
                if (itemName.equals("EMPTY")) {
                    g.setColor(Color.GRAY); g.drawString("EMPTY", bx + 12, by + 35);
                } else {
                    g.setColor(Color.GREEN); g.drawString(itemName, bx + 5, by + 35);
                }
            } else {
                g.setColor(Color.RED); g.drawString("LOCKED", bx + 10, by + 35);
            }
        }

        // ============================================================
        // [BARU] HUD KANAN ATAS - PLAYER STATS
        // ============================================================
        int trX = getWidth() - 140; // Geser 140px dari kanan
        int trY = 25;
        
        // Background Kotak Transparan (Biar tulisan kebaca jelas)
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(trX - 10, trY - 15, 130, 80);
        
        // Border Kotak
        g.setColor(Color.WHITE);
        g.drawRect(trX - 10, trY - 15, 130, 80);

        // Text Stats
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(Color.CYAN);
        g.drawString("PLAYER STATS", trX, trY);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("SPEED  : Lv " + dispSpeedLevel, trX, trY + 20);
        g.drawString("ATK SPD: Lv " + dispAtkSpeedLevel, trX, trY + 35);
        g.drawString("BURST  : Lv " + dispBurstLevel, trX, trY + 50);
        // ============================================================


        // --- OBJECTS GAME ---
        // g.setColor(new Color(100, 60, 20)); 
        // if(batuPosisi != null) for (int[] pos : batuPosisi) g.fillOval(pos[0], pos[1], bSize, bSize); 
        
        g.setColor(Color.WHITE);
        if (listDrop != null) for (DropItem d : listDrop) g.fillOval(d.getX(), d.getY(), d.getSize(), d.getSize());

        if (listBullet != null) {
            for (Bullet b : listBullet) {
                g.setColor(b.isFromAlien() ? Color.YELLOW : Color.CYAN);
                g.fillOval((int)b.getX(), (int)b.getY(), b.getSize(), b.getSize());
            }
        }

        if (listAlien != null) {
            for (Alien a : listAlien) {
                g.setColor(a.getColor()); g.fillOval(a.getX(), a.getY(), a.getSize(), a.getSize());
                g.setColor(Color.RED); g.fillRect(a.getX(), a.getY() - 8, a.getSize(), 3);
                g.setColor(Color.GREEN);
                int hW = (int) ((double) a.getHp() / a.getMaxHp() * a.getSize());
                g.fillRect(a.getX(), a.getY() - 8, hW, 3);
            }
        }

        // --- PLAYER & EFFECTS ---
        if (isEmpActive) {
            g2.setColor(new Color(0, 255, 255, 150)); 
            g2.setStroke(new BasicStroke(5)); 
            g2.drawOval(playerX + 25 - empRadius, playerY + 25 - empRadius, empRadius * 2, empRadius * 2);
            g2.setStroke(new BasicStroke(1));
        }

        if (listBullet != null) {
            for (Bullet b : listBullet) {
                if (b.isFromAlien()) { // CUMA PELURU ALIEN
                    g.setColor(Color.YELLOW);
                    g.fillOval((int)b.getX(), (int)b.getY(), b.getSize(), b.getSize());
                }
            }
        }

        if (isTimeFreezeActive) {
            // Gambar kotak transparan biru muda menutupi semua alien & background
            g.setColor(new Color(200, 230, 255, 60)); // Biru Es Transparan
            g.fillRect(0, 0, getWidth(), getHeight());
            
            // Tambah efek border Vignette biar dramatis (Opsional)
            g2.setStroke(new BasicStroke(20));
            g.setColor(new Color(0, 100, 255, 50));
            g.drawRect(0, 0, getWidth(), getHeight());
            g2.setStroke(new BasicStroke(1));
        }

        

        // --- 3. GAMBAR OBJEK YANG KEBAL WAKTU (Player & Peluru Player) ---
        // Pindahin Render Peluru Player ke sini biar di ATAS filter
        if (listBullet != null) {
            for (Bullet b : listBullet) {
                if (!b.isFromAlien()) { // CUMA PELURU PLAYER
                    
                    if (bulletPlayerImg != null) {
                        // 1. Hitung Titik Tengah Peluru
                        double cX = b.getX() + (b.getSize() / 2.0);
                        double cY = b.getY() + (b.getSize() / 2.0);

                        // 2. Hitung Sudut Terbang (Dari VX dan VY)
                        double bulletAngle = Math.atan2(b.getVy(), b.getVx());
                        
                        // --- FIX ROTASI: KOREKSI OFFSET GAMBAR ---
                        // Karena gambar aslinya madep "Kanan Atas" (-45 derajat),
                        // Kita putar balik +45 derajat biar lurus sejajar sumbu X
                        bulletAngle += Math.toRadians(45); 

                        // 3. Setup Rotasi
                        AffineTransform old = g2.getTransform();
                        g2.translate(cX, cY); 
                        g2.rotate(bulletAngle); 

                        // 4. Gambar (Center di 0,0 relatif terhadap translate)
                        int bW = 20; 
                        int bH = 20;
                        g2.drawImage(bulletPlayerImg, -bW/2, -bH/2, bW, bH, null);

                        g2.setTransform(old); // Balikin posisi
                    } else {
                        // Fallback (Logic Lama)
                        g.setColor(Color.CYAN);
                        g.fillOval((int)b.getX(), (int)b.getY(), b.getSize(), b.getSize());
                    }
                    
                }
            }
        }

        if (isInvincible) {
            g.setColor(new Color(255, 215, 0, 100)); 
            g.fillOval(playerX - 10, playerY - 10, 70, 70); 
            g.setColor(Color.YELLOW); g.drawOval(playerX - 10, playerY - 10, 70, 70);
        }
        if (isShieldActive) {
            g.setColor(new Color(0, 191, 255, 130));
            g.fillOval(playerX - 10, playerY - 10, 70, 70);
        }
        if (isPhaseActive) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

        if (isAimingDash) {
            g2.setColor(new Color(0, 255, 255, 100)); // Cyan Transparan
            g2.setStroke(new BasicStroke(2));
            // Gambar lingkaran putus-putus biar beda sama EMP
            float[] dash = {10.0f};
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.drawOval(playerX + 25 - dashRange, playerY + 25 - dashRange, dashRange * 2, dashRange * 2);
            g2.setStroke(new BasicStroke(1)); // Reset garis
        }
        
        g.setColor(new Color(0, 0, 0, 100)); 
        if (isGameOver) {
            g.fillOval(playerX - 5, playerY + 33, 60, 20); 
        } else {
            // --- BAYANGAN NORMAL (DI KAKI) ---
            g.fillOval(playerX + 5, playerY + 40, 40, 15); 
        }

        // Trigger Lock kalau ada sinyal nembak
        if (isShooting) {
            isShootingAnimLocked = true;
        }

        // Kalau animasi terkunci, jalankan timer & frame
        if (isShootingAnimLocked) {
            shootAnimTimer++;
            if (shootAnimTimer > 2) { // Speed animasi
                shootAnimFrame++;     // Pindah frame
                shootAnimTimer = 0;
                
                // Cek apakah sequence frame 0-1-2-3 sudah selesai?
                // (Kita cek > 3, berarti pas frame jadi 4)
                if (shootAnimFrame > 3) {
                    shootAnimFrame = 0; // Reset ke awal loop
                    
                    // KUNCI LOGIC DISINI:
                    // Cuma buka kunci (Unlock) kalau user udah LEPAS mouse (!isShooting).
                    // Kalau user masih tahan mouse, Lock tetep True, jadi dia looping lagi.
                    if (!isShooting) {
                        isShootingAnimLocked = false;
                    }
                }
            }
        } else {
            // Kalau gak lagi nembak, reset frame nembak biar next time mulai dari 0 lagi
            shootAnimFrame = 0;
            shootAnimTimer = 0;
        }

        // ==========================================
        // 2. LOGIC RENDER (MILIH GAMBAR DOANG)
        // ==========================================
        // JANGAN ADA KODE shootAnimTimer++ LAGI DISINI!
        
        double deg = Math.toDegrees(playerAngle); 
        Image playerImg = null;

        if (isGameOver) {
            playerImg = playerDeadImg;
        } else if (isShootingAnimLocked) {
            // --- MODE NEMBAK ---
            // Tinggal pake variable 'shootAnimFrame' yang udah dihitung di atas
            
            if (deg > -45 && deg <= 45) {
                playerImg = pShootRight[shootAnimFrame];
            } 
            else if (deg > 45 && deg <= 135) {
                playerImg = pShootFwd[shootAnimFrame];
            } 
            else if (deg > 135 || deg <= -135) {
                playerImg = pShootLeft[shootAnimFrame];
            } 
            else if (deg > -135 && deg <= -45) {
                playerImg = pShootBack; // Back cuma 1 frame statis
            }
            
        } else {
            // --- MODE JALAN / IDLE ---
            animTimer++;
            if (animTimer > 15) { 
                animFrame = 1 - animFrame; 
                animTimer = 0;
            }

            if (deg > -45 && deg <= 45) { playerImg = (animFrame == 0) ? pRight1 : pRight2; } 
            else if (deg > 45 && deg <= 135) { playerImg = (animFrame == 0) ? pFwd1 : pFwd2; } 
            else if (deg > 135 || deg <= -135) { playerImg = (animFrame == 0) ? pLeft1 : pLeft2; } 
            else if (deg > -135 && deg <= -45) { playerImg = (animFrame == 0) ? pBack1 : pBack2; }
        }

        // Render Gambar
        if (playerImg != null) {
            g.drawImage(playerImg, playerX, playerY, 50, 50, null);
        } else {
            // Fallback (Kotak Hijau)
            g.setColor(Color.GREEN); g.fillRect(playerX, playerY, 50, 50);
        }

        // 3. Render Gambar
        if (playerImg != null) {
            g.drawImage(playerImg, playerX, playerY, 50, 50, null);
        } else {
            g.setColor(Color.GREEN); g.fillRect(playerX, playerY, 50, 50);
        }

        // 4. Outline Hitbox Merah (Debug)
        // g.setColor(Color.RED);
        // g.drawRect(playerX, playerY, 50, 50); 

        // Reset Transparansi
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        int cX = playerX + 25; 
        int cY = playerY + 25;

        // 2. Setup Grafik buat Rotasi
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(cX, cY); // Pindahin titik 0,0 ke tengah player
        g2.rotate(angle);     // Putar kanvas sesuai arah mouse

        // 3. Tentukan Gambar & Jarak
        Image cursorImg = isAimingDash ? aimDashImg : aimNormalImg;
        int distFromPlayer = 35; // Jarak panah dari badan player (makin gede makin jauh)

        if (cursorImg != null) {
            // Gambar panah di kanan (X positif) karena rotasi udah dihandle g2.rotate
            // Y geser -15 biar gambarnya (30x30) pas di tengah garis aim
            g.drawImage(cursorImg, distFromPlayer, -10, 20, 20, null);
        } else {
            // Fallback: Gambar segitiga manual kalo gambar gagal load
            g2.setColor(isAimingDash ? Color.CYAN : Color.RED);
            g2.fillPolygon(new int[]{distFromPlayer, distFromPlayer+12, distFromPlayer}, 
                           new int[]{-4, 0, 4}, 3);
        }

        // 4. Balikin Grafik ke Normal
        g2.setTransform(oldTransform);

        // --- OVERLAYS ---
        if (showQuickShop || showIntermissionShop) {
            g.setColor(new Color(0, 0, 0, 230)); g.fillRect(50, 50, 700, 500);
            g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.BOLD, 22));
            g.drawString(showIntermissionShop ? "WAVE UPGRADES" : "QUICK SHOP", 100, 100);
            g.setFont(new Font("Monospaced", Font.PLAIN, 16));
            int y = 200;
            for (String line : shopInfo.split("\n")) { g.drawString(line, 100, y); y += 35; }
            g.setColor(Color.YELLOW); g.drawString("SCRAP: " + scrap, 100, 140);
        }

        if (isGameOver) {
            g.setColor(new Color(150, 0, 0, 200));
            g.fillRect(0, 0, 800, 600);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("GAME OVER", 250, 280);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("FINAL SCORE: " + skor, 320, 330);
            g.drawString("Press [SPACE] to Return Menu", 270, 400);
        }
    }
    public int[][] getWallDimensions() { return wallDimensions; }
    public int[] getBatuTypes() { return batuType; }
}