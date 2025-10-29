package game;

import java.awt.*;
import java.util.Random;

public class Particle {
    private static Random random = new Random();

    public double x, y, vx, vy;
    public int life;
    public Color color;

    public Particle(double x, double y) {
        this.x = x;
        this.y = y;
        double angle = random.nextDouble() * Math.PI * 2;
        double speed = 2 + random.nextDouble() * 3;
        vx = Math.cos(angle) * speed;
        vy = Math.sin(angle) * speed;
        life = 20 + random.nextInt(20);
        color = new Color(255, random.nextInt(100) + 100, 100);
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.3;
        vx *= 0.95;
        life--;
    }

    public void draw(Graphics2D g) {
        int alpha = (int)(255 * life / 40.0);
        alpha = Math.max(0, Math.min(255, alpha));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.fillOval((int)x - 3, (int)y - 3, 6, 6);
    }
}