package src.utils;

import java.io.*;
import javax.sound.sampled.*;

public class SoundPlayer {
    
    // Simpan data suara di RAM (Static) biar cuma diload sekali seumur hidup
    private static byte[] fireSoundData;
    private static AudioFormat fireFormat;
    private static DataLine.Info fireInfo;

    private static Clip emptyClip;
    private static Clip deadClip;

    // Helper Load Universal biar gak copas-copas kode
    private static Clip loadClip(String filename) {
        try {
            File file = new File("src/assets/sounds/players/" + filename);
            if (!file.exists()) {
                System.out.println("Audio Missing: " + filename);
                return null;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void playEmptySound() {
        if (emptyClip == null) emptyClip = loadClip("empty.wav");
        if (emptyClip != null) {
            // Stop dulu barangkali user spam klik cepet banget
            if (emptyClip.isRunning()) emptyClip.stop(); 
            emptyClip.setFramePosition(0);
            emptyClip.start();
        }
    }

    public static void playDeadSound() {
        if (deadClip == null) deadClip = loadClip("dead.png"); // Eh salah, dead.wav maksudnya
        // KOREKSI STRING DI BAWAH:
        if (deadClip == null) deadClip = loadClip("dead.wav");
        
        if (deadClip != null) {
            if (deadClip.isRunning()) deadClip.stop();
            deadClip.setFramePosition(0);
            deadClip.start();
        }
    }

    // Fungsi buat baca file ke RAM
    private static void loadFireSound() {
        try {
            File file = new File("src/assets/sounds/players/fireSound.wav");
            if (!file.exists()) return;

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            fireFormat = ais.getFormat();
            fireInfo = new DataLine.Info(Clip.class, fireFormat);

            // Baca semua byte file ke dalam ByteArrayOutputStream
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384]; // Buffer sementara
            while ((nRead = ais.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            fireSoundData = buffer.toByteArray(); // Simpan ke variable static
            ais.close();
            
            System.out.println("Audio Loaded to RAM: " + fireSoundData.length + " bytes");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playFireSound() {
        // Kalau data belum ada, load dulu
        if (fireSoundData == null) {
            loadFireSound();
        }

        // Kalau masih null (file gak ketemu), skip aja
        if (fireSoundData == null) return;

        // Jalankan di Thread baru biar GAK BIKIN LAG GAME LOOP
        new Thread(() -> {
            try {
                // Bikin Clip BARU tiap kali nembak (Inilah kunci OVERLAP)
                Clip clip = (Clip) AudioSystem.getLine(fireInfo);
                
                // Open pakai data dari RAM (Cepat banget!)
                clip.open(fireFormat, fireSoundData, 0, fireSoundData.length);
                
                // Atur Volume (Opsional)
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(-10.0f); // Turunin 10db biar gak budeg
                }

                // PENTING: Pasang Listener buat buang sampah memori kalau udah selesai bunyi
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close(); // Hapus resource clip biar RAM gak bocor
                    }
                });

                clip.start(); // MAINKAN!
                
            } catch (Exception e) {
                // Silent error
            }
        }).start();
    }

    private static byte[] stepSoundData;
    private static AudioFormat stepFormat;
    private static DataLine.Info stepInfo;
    private static Clip stepClip; // <--- KITA SIMPAN DI SINI BIAR BISA DI-STOP

    private static void loadStepSound() {
        try {
            File file = new File("src/assets/sounds/players/footStep.wav");
            if (!file.exists()) return;

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            stepFormat = ais.getFormat();
            stepInfo = new DataLine.Info(Clip.class, stepFormat);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = ais.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            stepSoundData = buffer.toByteArray();
            ais.close();
            
            // PRE-LOAD CLIP NYA SEKARANG
            stepClip = (Clip) AudioSystem.getLine(stepInfo);
            stepClip.open(stepFormat, stepSoundData, 0, stepSoundData.length);
            
            // SET VOLUME DI AWAL
            if (stepClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) stepClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-15.0f); 
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    // FUNGSI BARU: START LOOPING
    public static void startStep() {
        if (stepSoundData == null) loadStepSound();
        if (stepClip == null) return;

        // Cuma start kalau belum jalan (biar gak ngereset-reset sendiri)
        if (!stepClip.isRunning()) {
            stepClip.loop(Clip.LOOP_CONTINUOUSLY); // ULANG TERUS SAMPE DISURUH STOP
        }
    }

    // FUNGSI BARU: STOP / CUT
    public static void stopStep() {
        if (stepClip != null && stepClip.isRunning()) {
            stepClip.stop();
        }
    }
}