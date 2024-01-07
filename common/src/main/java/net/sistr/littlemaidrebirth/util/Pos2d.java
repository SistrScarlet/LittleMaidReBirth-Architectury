package net.sistr.littlemaidrebirth.util;

public class Pos2d {
    private final int x;
    private final int y;

    public Pos2d(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }
}
