package src.presenters;

import src.models.dbConnection;
import src.models.SaveManager; // Import save manager
import src.views.MenuUtama;
import src.views.GameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class MenuPresenter {
    private dbConnection db;
    private MenuUtama view;

    public MenuPresenter(MenuUtama view) {
        this.view = view;
        this.db = new dbConnection();
        initListeners();
        tampilkanData(); 
    }

    private void initListeners() {
        view.getBtnPlay().addActionListener(e -> {
            String username = view.getUsername();
            
            // 1. Validasi Input dulu
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(view, "Username tidak boleh kosong!");
                return;
            }

            // 2. Logika Database (Simpan user baru jika belum ada)
            if (!db.isUsernameExists(username)) {
                db.insertNewUser(username);
                tampilkanData(); // Refresh tabel biar nama baru muncul
            }

            // 3. Jalankan Animasi Lever
            // Logic pindah game ditaruh di dalam callback biar nunggu animasi selesai
            view.playLeverAnimation("PLAY", () -> {
                mulaiPermainan(username); // Panggil fungsi mulaiPermainan lo yang asli
                view.dispose(); // Tutup menu
            });
        });
        view.getBtnQuit().addActionListener(e -> {
            // Jalankan animasi tuas untuk QUIT
            view.playLeverAnimation("QUIT", () -> {
                // Callback: Setelah animasi & delay selesai, baru tutup aplikasi
                System.exit(0);
            });
        });
    }

    private void mulaiPermainan(String username) {
        view.dispose();
        JFrame gameWindow = new JFrame("Hide and Seek The Challenge - Player: " + username);
        GameView gameView = new GameView(gameWindow);
        
        gameWindow.add(gameView);
        gameWindow.setSize(800, 600);
        gameWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameWindow.setLocationRelativeTo(null);
        gameWindow.setResizable(false);

        // Kirim username ke GamePresenter
        GamePresenter gamePresenter = new GamePresenter(gameView, gameWindow, username);
        
        // --- LOGIKA LOAD SAVE ---
        if (SaveManager.hasSave(username)) {
            int jawab = JOptionPane.showConfirmDialog(gameWindow, 
                "Ditemukan Save Data untuk " + username + ".\nLanjutkan Game Terakhir?", 
                "Resume Game", JOptionPane.YES_NO_OPTION);
            
            if (jawab == JOptionPane.YES_OPTION) {
                String data = SaveManager.loadGame(username);
                if (data != null) {
                    gamePresenter.loadGame(data);
                }
            } else {
                // Hapus save lama kalau user milih New Game
                SaveManager.deleteSave(username);
            }
        }
        // ------------------------

        gameWindow.setVisible(true);
        gamePresenter.startGame();
    }

    public void tampilkanData() {
        String[] kolom = {"Username", "Skor", "Peluru Meleset", "Sisa Peluru"};
        DefaultTableModel model = new DefaultTableModel(kolom, 0);
        try {
            ResultSet rs = db.executeQuery("SELECT * FROM tbenefit ORDER BY skor DESC");
            while (rs.next()) {
                Object[] row = {
                    rs.getString("username"), rs.getInt("skor"),
                    rs.getInt("peluru_meleset"), rs.getInt("sisa_peluru")
                };
                model.addRow(row);
            }
            view.setTableData(model);
        } catch (Exception e) {
            System.err.println("Gagal memuat data tabel: " + e.getMessage());
        }
    }
}