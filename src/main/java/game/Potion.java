package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Potion {
    public enum Type { HEALTH, SPEED, POWER }

    public Type type;
    public double x, y;
    public int width = 32, height = 32;
    public boolean collected = false;

    private BufferedImage spriteSheet;
    private BufferedImage[] frames;
    private int frameCount = 7;
    private int frameWidth = 18; // 126 / 7
    private int frameHeight = 35;
    private int frameIndex = 0;
    private int frameTimer = 0;
    private int frameDelay = 8;

    public Potion(double x, double y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
        loadSpriteSheet();
    }

    /** โหลดภาพ (ใช้ระบบเดียวกับที่คุณให้มา) */
    private Image loadImage(String resourcePath) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url != null) return new ImageIcon(url).getImage();
        } catch (Exception ignored) {}
        try {
            return new ImageIcon(resourcePath.replaceFirst("/", "")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load image: " + resourcePath);
            return null;
        }
    }

    /** โหลด spritesheet แล้วแยกเป็น frame */
    private void loadSpriteSheet() {
        String path = switch (type) {
            case HEALTH -> "/assets/potion_health.png";
            case SPEED -> "/assets/potion_speed.png";
            case POWER -> "/assets/potion_power.png";
        };

        try {
            Image img = loadImage(path);
            if (img != null) {
                // แปลง Image → BufferedImage
                spriteSheet = new BufferedImage(
                        img.getWidth(null),
                        img.getHeight(null),
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2 = spriteSheet.createGraphics();
                g2.drawImage(img, 0, 0, null);
                g2.dispose();

                int totalFrames = spriteSheet.getWidth() / frameWidth;
                frames = new BufferedImage[totalFrames];
                for (int i = 0; i < totalFrames; i++) {
                    frames[i] = spriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
                }
            } else {
                System.err.println("❌ Potion spritesheet not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load potion spritesheet: " + path);
            e.printStackTrace();
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public void update() {
        if (frames == null || collected) return;
        frameTimer++;
        if (frameTimer >= frameDelay) {
            frameTimer = 0;
            frameIndex = (frameIndex + 1) % frameCount;
        }
    }

    public void draw(Graphics2D g) {
        if (collected) return;
        if (frames != null && frames[frameIndex] != null) {
            g.drawImage(frames[frameIndex], (int) x, (int) y, width, height, null);
        } else {
            g.setColor(switch (type) {
                case HEALTH -> Color.RED;
                case SPEED -> Color.CYAN;
                case POWER -> Color.ORANGE;
            });
            g.fillRect((int) x, (int) y, width, height);
        }
    }
}
