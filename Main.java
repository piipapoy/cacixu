/* 
    Saya M. Raffa Mizanul Insan dengan NIM 2409119 mengerjakan evaluasi Tugas Masa Depan
    dalam mata kuliah Desain dan Pemrograman Berorientasi Objek untuk keberkahanNya maka saya
    tidak melakukan kecurangan seperti yang telah dispesifikasikan. Aamiin.
*/

import src.views.MenuUtama;
import src.presenters.MenuPresenter; // Impor Presenter
import java.awt.EventQueue;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            MenuUtama menu = new MenuUtama();
            
            // Inisialisasi Presenter dan panggil datanya
            MenuPresenter presenter = new MenuPresenter(menu);
            presenter.tampilkanData(); // Ini yang membuat tabel terisi otomatis saat start
            
            menu.setVisible(true);
        });
    }
}