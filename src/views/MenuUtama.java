package src.views;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import src.models.SaveManager;
import javax.sound.sampled.*;

public class MenuUtama extends JFrame {
    private JTextField tfUsername;
    private JTable tabelData;
    private JButton btnPlay, btnQuit;
    private DefaultTableModel tableModel;
    private Image bgImage;
    private Image bgPlayActive, bgQuitActive;
    private Image currentBg;
    private Clip bgmClip;
    private Clip[] keyClips = new Clip[5];
    private Clip[] spaceClips = new Clip[5]; // Kita kasih 5 slot buat spasi biar bisa overlap
    private int currentSpaceIndex = 0; // Buat nandain slot mana yang dipake
    private java.util.Random random = new java.util.Random();
    private boolean isActionLocked = false;

    // Koordinat Pixel (Disesuaikan dengan mainmenu.jpg 800x600)
    // Kalau nanti kurang pas, geser-geser angka ini aja
    private final int USER_X = 275, USER_Y = 151, USER_W = 250, USER_H = 30;
    private final int TABLE_X = 174, TABLE_Y = 224, TABLE_W = 451, TABLE_H = 212;
    private final int BTN_PLAY_X = 270, BTN_PLAY_Y = 512, BTN_W = 110, BTN_H = 30;
    private final int BTN_QUIT_X = 419, BTN_QUIT_Y = 512;

    public MenuUtama() {
        initUI();
    }

    public MenuUtama(JFrame oldWindow) {
        if (oldWindow != null) oldWindow.dispose();
        initUI();
        this.setVisible(true);
    }

    private void playBGM(String filePath) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(filePath));
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioIn);
            
            // --- LOGIKA PENGATURAN VOLUME ---
            if (bgmClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                
                // Kurangi volume sebesar 15 decibel (dB)
                // Angka negatif makin pelan (-10.0f sampai -20.0f biasanya pas buat BGM)
                float reduceVolume = -10.0f; 
                gainControl.setValue(reduceVolume);
            }
            // ---------------------------------
            
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
}

    public void stopBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
        }
    }

    private void initUI() {

        setTitle("CACIXU - Survival Expedition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        try {
            bgImage = ImageIO.read(new File("src/assets/mainmenu.jpg"));
            bgPlayActive = ImageIO.read(new File("src/assets/mainmenu_playActive.jpg"));
            bgQuitActive = ImageIO.read(new File("src/assets/mainmenu_quitActive.jpg"));
            currentBg = bgImage; // Default awal pake yang normal
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentBg != null) {
                    g.drawImage(currentBg, 0, 0, 800, 600, this);
                } else {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        backgroundPanel.setLayout(null); // ABSOLUTE POSITIONING

        backgroundPanel.setPreferredSize(new Dimension(800, 600)); 
        this.add(backgroundPanel); // Tambahkan panel ke JFrame
        this.pack(); // Ini yang bakal bikin window nge-fit 800x600 murni
        this.setLocationRelativeTo(null);

        // 1. Load Gambar Background

        // 2. Panel Utama dengan Custom Painting

        // --- KOMPONEN UI ---

        // A. INPUT USERNAME (Di atas lempengan besi kecil)
        tfUsername = new JTextField();

        ((javax.swing.text.AbstractDocument) tfUsername.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                // 1. Cek panjang karakter (Max 16)
                int currentLength = fb.getDocument().getLength();
                int overLimit = (currentLength + text.length() - length) - 16;
                
                if (overLimit > 0) {
                    text = text.substring(0, text.length() - overLimit);
                }

                // 2. Cek karakter (Hanya Alfabet, Angka, dan Spasi)
                // RegEx: [^a-zA-Z0-9 ] artinya selain itu bakal dihapus
                String filteredText = text.replaceAll("[^a-zA-Z0-9 ]", "");

                if (text.length() > 0 && filteredText.isEmpty()) {
                    // Jika user ngetik karakter ilegal (misal: @ atau #), jangan lakukan apa-apa
                    return; 
                }

                super.replace(fb, offset, length, filteredText, attrs);
            }
        });

        tfUsername.setBounds(USER_X, USER_Y, USER_W, USER_H);
        tfUsername.setFont(new Font("Monospaced", Font.BOLD, 16));
        tfUsername.setHorizontalAlignment(JTextField.CENTER);
        tfUsername.setBorder(null); // Hapus border kotak
        tfUsername.setOpaque(false); // Transparan backgroundnya
        tfUsername.setForeground(new Color(60, 40, 20)); // Coklat Tua (Rustic)
        // Placeholder text logic bisa ditambah di Presenter kalau mau, atau biarin kosong

        tfUsername.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                playKeySound(e.getKeyCode());
            }
        });
        
        backgroundPanel.add(tfUsername);

        // B. TABEL SCOREBOARD (Di dalam layar biru besar)
        String[] kolom = {"Username", "Skor", "Peluru Meleset", "Sisa Peluru"};
        tableModel = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Ini kuncinya! Paksa semua sel jadi TIDAK BISA diedit
                return false;
            }
        };

        tabelData = new JTable(tableModel);
        tabelData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelData.setCellSelectionEnabled(false); // User nggak bisa pilih sel satuan
        tabelData.setRowSelectionAllowed(true);   // Tapi tetep bisa pilih satu baris
        tabelData.setFocusable(false);
        tabelData.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabelData.setRowHeight(25);
        tabelData.setShowGrid(false); // Biar bersih
        tabelData.setOpaque(false); // Tabel transparan
        ((DefaultTableCellRenderer)tabelData.getDefaultRenderer(Object.class)).setOpaque(false);
        tabelData.setForeground(new Color(200, 230, 255)); // Biru Muda/Cyan pudar (biar kontras di layar gelap)
        tabelData.setIntercellSpacing(new Dimension(0, 0)); // Rapiin jarak antar sel
        tabelData.setBorder(null); // Matiin border internal tabel

        tabelData.getColumnModel().getColumn(0).setPreferredWidth(120); 
        tabelData.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabelData.getColumnModel().getColumn(2).setPreferredWidth(180); 
        tabelData.getColumnModel().getColumn(3).setPreferredWidth(160);
        tabelData.setRowHeight(26);

        tabelData.getSelectionModel().addListSelectionListener(e -> {
            // Pastikan event ini selesai (biar gak kepanggil dua kali saat klik)
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tabelData.getSelectedRow();
                if (selectedRow != -1) {
                    // Ambil data dari kolom ke-0 (Username) di baris yang diklik
                    String selectedUsername = tabelData.getValueAt(selectedRow, 0).toString();
                    
                    // Masukkan ke TextField Username
                    tfUsername.setText(selectedUsername);
                    playSound("src/assets/menuSelect.wav");
                    
                    // Opsional: Kasih feedback visual dikit biar keren
                    tfUsername.requestFocus(); 
                }
            }
        });
        
        // Header Tabel Transparan
        JTableHeader header = tabelData.getTableHeader();
        header.setFont(new Font("Monospaced", Font.BOLD, 14));
        header.setBackground(new Color(0, 0, 0, 100)); 
        header.setForeground(Color.ORANGE);
        header.setReorderingAllowed(false);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Biarkan super mengurus inisialisasi dasar
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // 1. ATUR WARNA TEKS (ORANGE)
                setForeground(Color.ORANGE);
                
                // 2. ATUR BACKGROUND (SEMI-TRANSPARAN HITAM)
                setBackground(new Color(0, 0, 0, 100)); // Hitam dengan transparansi 150
                
                // 3. PASTIKAN OPAQUE TRUE (Penting agar background tergambar)
                setOpaque(true); 
                
                // 4. ATUR POSISI & PADDING
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                return this;
            }
        });
        
        // ScrollPane Transparan
        JScrollPane scrollPane = new JScrollPane(tabelData);
        scrollPane.setBounds(TABLE_X, TABLE_Y, TABLE_W, TABLE_H);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Custom Renderer untuk Warna Save File (Hijau)
        tabelData.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
                String username = (String) table.getModel().getValueAt(row, 0);
                setOpaque(false);

                if (isSelected) {
                    // Jika diklik/pilih, font jadi BOLD dan ukuran sedikit lebih tegas
                    setFont(new Font("Monospaced", Font.BOLD, 13));
                } else {
                    // Font normal saat tidak dipilih
                    setFont(new Font("Monospaced", Font.PLAIN, 12));
                }
                
                // Reset warna dasar (penting karena kita override)
                c.setForeground(new Color(200, 230, 255)); 
                
                if (SaveManager.hasSave(username)) {
                    // Kalau ada save file, warnanya Emas/Kuning
                    c.setForeground(Color.YELLOW);
                    if (isSelected) c.setBackground(new Color(255, 255, 0, 50));
                    else c.setBackground(new Color(0, 0, 0, 0)); // Transparan
                } else {
                    if (isSelected) c.setBackground(new Color(0, 255, 255, 50));
                    else c.setBackground(new Color(0, 0, 0, 0)); // Transparan
                }
                
                ((JComponent)c).setOpaque(isSelected); // Cuma opaque kalau diselect
                return c;
            }
        });

        backgroundPanel.add(scrollPane);

        // C. TOMBOL PLAY & QUIT (Di atas tuas)
        // Kita bikin tombolnya "Invisible" di atas gambar tuas, tapi ada teksnya
        
        btnPlay = createInvisibleButton("PLAY");
        btnPlay.setBounds(BTN_PLAY_X, BTN_PLAY_Y, BTN_W, BTN_H);
        
        btnQuit = createInvisibleButton("QUIT");
        btnQuit.setBounds(BTN_QUIT_X, BTN_QUIT_Y, BTN_W, BTN_H);

        backgroundPanel.add(btnPlay);
        backgroundPanel.add(btnQuit);

        setContentPane(backgroundPanel);
        playBGM("src/assets/Rust in the Haze.wav");
        prepareKeyboardSounds();
    }

    private JButton createInvisibleButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial Black", Font.BOLD, 14));
        btn.setForeground(new Color(60, 40, 20)); // Coklat Tua (Biar kayak ukiran di besi)
        btn.setContentAreaFilled(false); // Transparan isinya
        btn.setBorderPainted(false);     // Hapus border
        btn.setFocusPainted(false);      // Hapus garis fokus
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Kursor tangan pas hover
        return btn;
    }

    public String getUsername() { return tfUsername.getText(); }
    public void setTableData(DefaultTableModel model) { 
        // Kita bungkus model dari Presenter agar isCellEditable selalu false
        this.tabelData.setModel(new DefaultTableModel() {
            @Override
            public int getRowCount() { return model.getRowCount(); }
            
            @Override
            public int getColumnCount() { return model.getColumnCount(); } // Pakai getColumnCount() langsung
            
            @Override
            public Object getValueAt(int row, int col) { return model.getValueAt(row, col); }
            
            @Override
            public String getColumnName(int col) { return model.getColumnName(col); }
            
            @Override
            public boolean isCellEditable(int row, int column) { return false; } // Kunci mati
        });

        // WAJIB: Atur ulang lebar kolom karena setModel() mereset pengaturan kolom
        tabelData.getColumnModel().getColumn(0).setPreferredWidth(120); 
        tabelData.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabelData.getColumnModel().getColumn(2).setPreferredWidth(180); 
        tabelData.getColumnModel().getColumn(3).setPreferredWidth(160);
    }

    public void playLeverAnimation(String type, Runnable callback) {
        if (isActionLocked) return; 
        
        isActionLocked = true; 

        // --- SARAN TERAKHIR: UBAH KURSOR JADI LOADING ---
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        btnPlay.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        btnQuit.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // ------------------------------------------------

        if (type.equals("PLAY")) {
            currentBg = bgPlayActive;
        } else if (type.equals("QUIT")) {
            currentBg = bgQuitActive;
        }
        
        repaint();
        playSound("src/assets/leverActive.wav");

        Timer timer = new Timer(1200, e -> {
            if (type.equals("PLAY")) {
                stopBGM();
            }
            callback.run(); 
            
            // Kembalikan kursor ke default (untuk jaga-jaga jika window tidak langsung tutup)
            this.setCursor(Cursor.getDefaultCursor());
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void playSound(String filePath) {
        try {
            File soundFile = new File(filePath);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareKeyboardSounds() {
        if (keyClips[0] != null) return;
        try {
            // Load 5 suara key biasa
            for (int i = 0; i < 5; i++) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File("src/assets/sounds/keyboard/key" + (i + 1) + ".wav"));
                keyClips[i] = AudioSystem.getClip();
                keyClips[i].open(ais);
            }

            // Load 5 slot buat Space (biar bisa overlap pas dispam)
            for (int i = 0; i < 5; i++) {
                AudioInputStream aisSpace = AudioSystem.getAudioInputStream(new File("src/assets/sounds/keyboard/keySpace.wav"));
                spaceClips[i] = AudioSystem.getClip();
                spaceClips[i].open(aisSpace);
            }
        } catch (Exception e) {
            System.out.println("Gagal load suara keyboard: " + e.getMessage());
        }
    }

    private void playKeySound(int keyCode) {
        if (keyCode == java.awt.event.KeyEvent.VK_SPACE) {
            // Logika Overlap Spasi
            Clip c = spaceClips[currentSpaceIndex];
            if (c != null) {
                c.setFramePosition(0); // Reset ke awal
                c.start();
                // Pindah ke slot berikutnya (0 sampai 4, lalu balik ke 0)
                currentSpaceIndex = (currentSpaceIndex + 1) % spaceClips.length;
            }
        } else {
            // Untuk key biasa, pake random aja (biasanya jarang dispam secepat spasi)
            int index = random.nextInt(5);
            if (keyClips[index] != null) {
                keyClips[index].setFramePosition(0);
                keyClips[index].start();
            }
        }
    }

    public JButton getBtnPlay() { return btnPlay; }
    public JButton getBtnQuit() { return btnQuit; }
}