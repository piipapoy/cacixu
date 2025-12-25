package src.models;

public class Skill {
    private String name;
    private int level; // 0 = Locked, 1-5 = Unlocked
    private int cooldownTimer;
    private int maxCooldown; // Dalam Frame (60 frame = 1 detik)
    private int baseMaxCooldown; // Simpan cooldown awal buat referensi

    public Skill(String name, int cooldownSeconds) {
        this.name = name;
        this.level = 0; // Default terkunci
        this.baseMaxCooldown = cooldownSeconds * 60; // Asumsi 60 FPS
        this.maxCooldown = baseMaxCooldown;
        this.cooldownTimer = 0;
    }

    public void update() {
        if (cooldownTimer > 0) cooldownTimer--;
    }

    public boolean isReady() {
        return level > 0 && cooldownTimer <= 0;
    }

    public void activate() {
        this.cooldownTimer = maxCooldown;
    }
    
    // Paksa reset cooldown (misal buat cheat/debug)
    public void resetCooldown() {
        this.cooldownTimer = 0;
    }

    public int getCostNextLevel() {
        switch (level) {
            case 0: return 20;  // Unlock
            case 1: return 40;
            case 2: return 70;
            case 3: return 100;
            case 4: return 150;
            default: return 9999; // Maxed
        }
    }

    public void upgrade() {
        if (level < 5) {
            level++;
            // Khusus DASH, di level 4 cooldown berkurang (sesuai spek)
            if (name.equals("DASH") && level >= 4) {
                maxCooldown = (int)(baseMaxCooldown * 0.6); // Diskon cooldown 40%
            }
        }
    }
    
    // Setter manual buat Load Game nanti
    public void setLevel(int lvl) {
        this.level = lvl;
        // Cek ulang cooldown reduction pas load
        if (name.equals("DASH") && level >= 4) {
            maxCooldown = (int)(baseMaxCooldown * 0.6);
        }
    }

    public void setCooldown(int seconds) {
        this.maxCooldown = seconds * 60; // Atau nama variabel cooldown lo apa
    }

    // Getters
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getCooldown() { return cooldownTimer; }
    public int getMaxCooldown() { return maxCooldown; }
    
    // Buat visual progress bar di icon (0.0 - 1.0)
    public float getCooldownRatio() {
        if (maxCooldown == 0) return 0;
        return (float) cooldownTimer / maxCooldown;
    }

    public void setCooldownTimer(int frames) {
        this.cooldownTimer = frames;
    }
}