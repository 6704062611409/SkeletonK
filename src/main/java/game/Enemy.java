package game;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Enemy {
    public double x, y;
    public double dy = 0;
    public int width, height;
    public int health = 100;
    public boolean hit = false;
    public int damage = 10;
    public boolean isAttacking = false;
    public boolean isGrounded = false;
    public boolean facingRight = true;
    public boolean isActive = false; // ✅ สำหรับระบบโหลดเฉพาะเมื่อผู้เล่นเข้าใกล้

    // Sprite animation
    private Image idleSheet, walkSheet, attackSheet, currentSheet;
    private int frameWidth = 96;
    private int frameHeight = 64;
    private int frameCount;
    private int currentFrame = 0;
    private int animTimer = 0;
    private int animSpeed = 6;
    private static final int SCALE = 2;

    private enum State { IDLE, WALK, ATTACK }
    private State state = State.IDLE;

    public double knockbackX = 0;
    private int attackCooldown = 0;
    private int hitCooldown = 0;
    private final double moveSpeed = 2.0;
    private final double jumpPower = -12; // ✅ ความแรงกระโดด

    public Enemy(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.width = frameWidth * SCALE;
        this.height = frameHeight * SCALE;

        idleSheet = loadImage("/assets/enemy_idle.png");
        walkSheet = loadImage("/assets/enemy_walk.png");
        attackSheet = loadImage("/assets/enemy_attack.png");

        setState(State.IDLE);
    }

    private Image loadImage(String resourcePath) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url != null) return new ImageIcon(url).getImage();
        } catch (Exception ignored) {}
        try {
            return new ImageIcon(resourcePath.replaceFirst("/", "")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load enemy image: " + resourcePath);
            return null;
        }
    }

    public Rectangle getBounds() {
        int hitW = (int)(width * 0.5);
        int hitH = (int)(height * 0.55);
        int hitX = (int)x + (width - hitW) / 2;
        int hitY = (int)y + (int)(height * 0.45);
        return new Rectangle(hitX, hitY, hitW, hitH);
    }

    public Rectangle getAttackBox() {
        int w = (int)(width * 0.85);
        int h = (int)(height * 0.65);
        int ax = (int)x + (width - w) / 2;
        int ay = (int)y + (int)(height * 0.5);
        return new Rectangle(ax, ay, w, h);
    }

    private void setState(State s) {
        if (state == s) return;
        state = s;
        switch (s) {
            case IDLE -> { currentSheet = idleSheet; frameCount = 8; animSpeed = 8; }
            case WALK -> { currentSheet = walkSheet; frameCount = 10; animSpeed = 6; }
            case ATTACK -> { currentSheet = attackSheet; frameCount = 10; animSpeed = 2; }
        }
        currentFrame = 0;
        animTimer = 0;
    }

    public void update(Player player, List<Platform> platforms) {
        int centerX = (int)x + width / 2;
        int pCenter = (int)player.x + player.width / 2;
        int distance = Math.abs(centerX - pCenter);
        attackCooldown--;
        hitCooldown--;

        // ✅ ระบบ Active เฉพาะเมื่อผู้เล่นอยู่ในระยะ
        int activationRange = 900;
        isActive = distance < activationRange;

        if (!isActive) return; // ❌ อยู่นอกระยะ ไม่ต้องอัปเดต

        // ✅ ตรวจด้านหน้า
        int detectionRange = 2000;
        boolean playerInFront =
                (facingRight && player.x > x && distance < detectionRange) ||
                        (!facingRight && player.x < x && distance < detectionRange);

        if (!isAttacking) {
            if (playerInFront) {
                // เดินเข้าหาผู้เล่น
                if (distance > 120) {
                    setState(State.WALK);
                    x += facingRight ? moveSpeed : -moveSpeed;
                } else {
                    // ระยะโจมตี
                    if (attackCooldown <= 0) {
                        setState(State.ATTACK);
                        isAttacking = true;
                        attackCooldown = 150;
                    } else {
                        setState(State.IDLE);
                    }
                }
            } else {
                // ไม่เห็นผู้เล่น → หันหาผู้เล่น
                facingRight = player.x > x;
                setState(State.IDLE);
            }
        }

        // ✅ ตรวจขอบ platform เพื่อกระโดด
        if (isGrounded && willFallOffEdge(platforms)) {
            dy = jumpPower;
            isGrounded = false;
        }

        // ✅ Gravity
        dy += 0.6;
        if (dy > 20) dy = 20;
        y += dy;

        // ✅ Knockback
        if (Math.abs(knockbackX) > 0.1) {
            x += knockbackX;
            knockbackX *= 0.7;
        } else {
            knockbackX = 0;
        }

        // ✅ Collision ตรวจพื้น
        isGrounded = false;
        for (Platform p : platforms) {
            Rectangle enemyRect = getBounds();
            Rectangle platRect = p.getBounds();

            if (enemyRect.intersects(platRect)) {
                double overlapTop = (y + height) - platRect.y;
                double overlapBottom = (platRect.y + platRect.height) - y;

                if (overlapTop < overlapBottom && dy > 0) {
                    y = platRect.y - height;
                    dy = 0;
                    isGrounded = true;
                }
            }
        }

        // ✅ Animation update
        animTimer++;
        if (animTimer >= animSpeed) {
            animTimer = 0;
            currentFrame++;
            if (currentFrame >= frameCount) {
                currentFrame = 0;
                if (state == State.ATTACK) {
                    isAttacking = false;
                    setState(State.IDLE);
                }
            }
        }

        if (hit && hitCooldown <= 0) hit = false;
    }

    // ✅ ตรวจว่ากำลังจะตก platform หรือมี platform ข้างหน้าไหม
    private boolean willFallOffEdge(List<Platform> platforms) {
        double frontX = facingRight ? x + width + 5 : x - 5;
        double checkY = y + height + 5;
        Rectangle footCheck = new Rectangle((int)frontX, (int)checkY, 4, 4);

        for (Platform p : platforms) {
            if (footCheck.intersects(p.getBounds())) return false;
        }
        return true;
    }

    public void applyKnockback(double value) {
        this.knockbackX = value;
    }

    public void draw(Graphics2D g) {
        if (!isActive) return; // ❌ ไม่อยู่ในระยะ = ไม่วาด

        if (currentSheet == null) {
            g.setColor(Color.RED);
            g.fillRect((int)x, (int)y, width, height);
            return;
        }

        int sx = currentFrame * frameWidth;
        int sy = 0;
        int sw = frameWidth;
        int sh = frameHeight;

        if (!facingRight) {
            g.drawImage(currentSheet,
                    (int)x + width, (int)y, (int)x, (int)y + height,
                    sx, sy, sx + sw, sy + sh, null);
        } else {
            g.drawImage(currentSheet,
                    (int)x, (int)y, (int)x + width, (int)y + height,
                    sx, sy, sx + sw, sy + sh, null);
        }

        // ✅ Debug hitboxes
        if (false) {
            g.setColor(new Color(0, 255, 0, 150));
            Rectangle bounds = getBounds();
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

            if (isAttacking) {
                g.setColor(new Color(255, 0, 0, 150));
                Rectangle attackBox = getAttackBox();
                g.drawRect(attackBox.x, attackBox.y, attackBox.width, attackBox.height);
            }

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString("HP:" + health, (int)x, (int)y - 5);
        }
    }
}
