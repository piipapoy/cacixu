package src.models;

import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

public class SaveManager {
    // Folder penyimpanan: users/
    private static final String FOLDER = "users/";

    // 1. Simpan String data ke file users/username.txt
    public static void saveGame(String username, String data) {
        try {
            // Pastikan folder users ada
            File directory = new File(FOLDER);
            if (!directory.exists()) {
                directory.mkdir();
            }
            
            // Tulis data ke file
            FileWriter writer = new FileWriter(FOLDER + username + ".txt");
            writer.write(data);
            writer.close();
            System.out.println("Game saved for: " + username);
        } catch (IOException e) {
            System.err.println("Gagal menyimpan game: " + e.getMessage());
        }
    }

    // 2. Baca isi file users/username.txt jadi satu String utuh
    public static String loadGame(String username) {
        File f = new File(FOLDER + username + ".txt");
        if (!f.exists()) return null; // Belum ada save-an
        
        try {
            Scanner scanner = new Scanner(f);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            return sb.toString();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    // 3. Hapus save file (misal pas mati)
    public static void deleteSave(String username) {
        File f = new File(FOLDER + username + ".txt");
        if (f.exists()) {
            f.delete();
            System.out.println("Save file deleted for: " + username);
        }
    }
    
    // 4. Cek apakah user punya save file
    public static boolean hasSave(String username) {
        return new File(FOLDER + username + ".txt").exists();
    }
}