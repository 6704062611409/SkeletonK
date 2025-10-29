package game;

import java.awt.*;

public class Platform {
    public int x, y, width, height;
    private boolean transparent;
    private float alpha;

    // Constructor สำหรับ platform ปกติ (ทึบ)
    public Platform(int x, int y, int width, int height) {
        this(x, y, width, height, false, 1.0f);
    }

    // Constructor สำหรับ platform ที่กำหนดความโปร่งใสได้
    public Platform(int x, int y, int width, int height, boolean transparent, float alpha) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.transparent = transparent;
        this.alpha = alpha;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g) {
        Composite old = g.getComposite();

        // ถ้าเป็น platform โปร่ง -> ตั้งค่าความโปร่งใส
        if (transparent) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        // วาดตัวสี่เหลี่ยม
        g.setColor(new Color(60, 60, 80));
        g.fillRect(x, y, width, height);

        // วาดขอบ
        g.setColor(new Color(100, 100, 130));
        g.drawRect(x, y, width, height);
        g.drawLine(x, y + 2, x + width, y + 2);

        // คืนค่าความโปร่งใสเดิม
        g.setComposite(old);
    }
}