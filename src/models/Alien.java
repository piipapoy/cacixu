package src.models;

import java.awt.Color;
import java.util.List;

public class Alien {
    private double x, y, vx, vy;
    private Color color;
    private double maxSpeed, steerStrength;
    private String type;
    private int size = 30;
    private int hp, maxHp;
    private double pushX = 0;
    private double pushY = 0;

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Alien(int spawnX, int spawnY, String type) {
        this.x = spawnX; this.y = spawnY; this.type = type;
        this.vx = 0; this.vy = 0;
        
        switch (type) {
            case "Grim": this.color = Color.GRAY; this.maxSpeed = 1.2; this.steerStrength = 0.03; this.maxHp = 1; break;
            case "Skarn": this.color = new Color(255, 140, 0); this.maxSpeed = 1.8; this.steerStrength = 0.04; this.maxHp = 2; break;
            case "Virex": this.color = Color.BLUE; this.maxSpeed = 2.8; this.steerStrength = 0.07; this.maxHp = 3; break;
            case "Noxar": this.color = new Color(128, 0, 128); this.maxSpeed = 3.5; this.steerStrength = 0.09; this.maxHp = 4; break;
            case "Zyphor": this.color = Color.RED; this.maxSpeed = 4.5; this.steerStrength = 0.12; this.maxHp = 5; break;
        }
        this.hp = this.maxHp;
    }

    public void takeDamage() { this.hp--; }
    public boolean isDead() { return hp <= 0; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public String getType() { return type; }

    // UPDATE: Sekarang nerima listAlien buat cek tabrakan antar alien
    public void move(int targetX, int targetY, int[][] batus, int[][] dims, int[] types, List<Alien> allAliens, double gameSpeed) {
        // 1. HITUNG STEERING (KEINGINAN GERAK ALIEN)
        double dx = targetX - x; double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) { dx /= dist; dy /= dist; }
        
        vx += (dx * maxSpeed - vx) * steerStrength;
        vy += (dy * maxSpeed - vy) * steerStrength;
        
        // 2. SEPARATION (BIAR GAK TUMPUK-TUMPUKAN)
        for (Alien other : allAliens) {
            if (other == this) continue;
            double diffX = x - other.x;
            double diffY = y - other.y;
            double distance = Math.sqrt(diffX * diffX + diffY * diffY);
            if (distance < size) {
                double overlap = size - distance;
                vx += (diffX / distance) * overlap * 0.1;
                vy += (diffY / distance) * overlap * 0.1;
            }
        }

        // 3. GABUNGKAN VEKTOR (GERAKAN SENDIRI + DORONGAN PUSH)
        // Ini kuncinya: alien tetap mencoba jalan (vx) meski sedang didorong (pushX)
        double totalVx = (vx + pushX) * gameSpeed; // <--- KALI SPEED
        double totalVy = (vy + pushY) * gameSpeed; // <--- KALI SPEED

        double nextX = x + totalVx; 
        double nextY = y + totalVy;
        boolean colX = false, colY = false;
        
        // 4. CEK TABRAKAN BATU (BERLAKU UNTUK JALAN BIASA MAUPUN DORONGAN)
        // Ini fix bug alien masuk batu pas didorong
        for (int i = 0; i < batus.length; i++) {
            int bW = dims[types[i]][0]; // Ambil lebar dari array dims
            int bH = dims[types[i]][1]; // Ambil tinggi dari array dims
            
            if (nextX < batus[i][0] + bW && nextX + size > batus[i][0] && y < batus[i][1] + bH && y + size > batus[i][1]) colX = true;
            if (x < batus[i][0] + bW && x + size > batus[i][0] && nextY < batus[i][1] + bH && nextY + size > batus[i][1]) colY = true;
        }
        
        // 5. UPDATE POSISI & PANTULAN
        if (!colX) {
            x = nextX;
        } else {
            vx *= -0.5;      // Pantulan steering
            pushX *= -0.5;   // Pantulan dorongan (biar mental dari batu)
        }

        if (!colY) {
            y = nextY;
        } else {
            vy *= -0.5;
            pushY *= -0.5;
        }

        // 6. KURANGI EFEK DORONGAN (GESEKAN)
        // Friction: makin kecil angkanya (misal 0.8), makin cepet berhenti licinnya
        double friction = 1.0 - (0.08 * gameSpeed); 
        pushX *= friction; 
        pushY *= friction;

        // Reset kalau dorongan sudah sangat lemah biar hemat komputasi
        if (Math.abs(pushX) < 0.1) pushX = 0;
        if (Math.abs(pushY) < 0.1) pushY = 0;
    }

    public void applyKnockback(double force, double angle) {
        // Zyphor (Boss): Cuma kena 20% dorongan (Berat banget)
        if (this.type.equals("Zyphor")) {
            force *= 0.2; 
        } 
        // Noxar (Semi-Boss): Kena 50% dorongan
        else if (this.type.equals("Noxar")) {
            force *= 0.5;
        }
        // Virex (Lincah): Kena 80% (Agak tahan dikit)
        else if (this.type.equals("Virex")) {
            force *= 0.8;
        }
        // Grim & Skarn (Kroco): Kena 100% (Mental jauh)
        
        this.pushX = Math.cos(angle) * force;
        this.pushY = Math.sin(angle) * force;
    }

    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
    public int getSize() { return size; }
    public Color getColor() { return color; }
}