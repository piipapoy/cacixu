package src.models;

public class DropItem {
    private int x, y;
    private int size = 10; // Titik putih kecil

    public DropItem(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getSize() { return size; }
}