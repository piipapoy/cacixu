package src.models;

public class Bullet {
    private double x, y;
    private double vx, vy;
    private boolean isFromAlien;
    private int size = 8;

    public Bullet(double x, double y, double targetX, double targetY, boolean isFromAlien) {
        this.x = x;
        this.y = y;
        this.isFromAlien = isFromAlien;

        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        double speed = isFromAlien ? 4 : 7; 
        this.vx = (dx / dist) * speed;
        this.vy = (dy / dist) * speed;
    }

    public void move(double speedMultiplier) {
        x += vx * speedMultiplier;
        y += vy * speedMultiplier;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public boolean isFromAlien() { return isFromAlien; }
    
    // TAMBAHAN BUAT SAVE SYSTEM
    public double getVx() { return vx; }
    public double getVy() { return vy; }
}