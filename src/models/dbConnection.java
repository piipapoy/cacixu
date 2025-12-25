package src.models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class dbConnection {
    private Connection con;
    private Statement stm;

    public dbConnection() {
        try {
            // Pastikan nama DB sesuai settingan laptop lo/dosen
            String url = "jdbc:mysql://localhost:3306/db_alien"; 
            String user = "root";
            String pass = "";
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);
            stm = con.createStatement();
        } catch (Exception e) {
            System.err.println("Koneksi Database Gagal: " + e.getMessage());
        }
    }

    public ResultSet executeQuery(String query) {
        ResultSet rs = null;
        try { rs = stm.executeQuery(query); } 
        catch (Exception e) { System.err.println("Error Query: " + e.getMessage()); }
        return rs;
    }

    public int executeUpdate(String query) {
        int result = 0;
        try { result = stm.executeUpdate(query); } 
        catch (Exception e) { System.err.println("Error Update: " + e.getMessage()); }
        return result;
    }

    public boolean isUsernameExists(String username) {
        try {
            ResultSet rs = executeQuery("SELECT * FROM tbenefit WHERE username='" + username + "'");
            return rs.next();
        } catch (Exception e) { return false; }
    }

    public void insertNewUser(String username) {
        executeUpdate("INSERT INTO tbenefit (username, skor, peluru_meleset, sisa_peluru) VALUES ('" + username + "', 0, 0, 0)");
    }

    // --- TAMBAHAN BARU: UPDATE SKOR ---
    public void updateScore(String username, int skor, int meleset, int sisaPeluru) {
        // Query ini akan menimpa data lama dengan data permainan terakhir
        String query = "UPDATE tbenefit SET skor=" + skor + 
                       ", peluru_meleset=" + meleset + 
                       ", sisa_peluru=" + sisaPeluru + 
                       " WHERE username='" + username + "'";
        executeUpdate(query);
        System.out.println("Data Database Updated untuk: " + username);
    }
}