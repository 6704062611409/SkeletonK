package game;

import javax.swing.*;
import java.awt.*;

public class Boss {
    public double x, y;
    public double dy = 0;
    public int width, height;
    public int health = 1000;
    public int maxHealth = 1000;
    public boolean hit = false;
    public int damage = 30;
    public boolean isAttacking = false;
    public boolean isGrounded = false;
    public boolean facingRight = true;

    // sprites
    private Image idleSheet, walkSheet, attackSheet, currentSheet;
    private int frameWidth = 96;
    private int frameHeight = 64;
    private int frameCount;
    private int currentFrame = 0;
    private int animTimer = 0;
    private int animSpeed = 6;
    private static final int SCALE = 3;

    private enum State { IDLE, WALK, ATTACK }
    private State state = State.IDLE;

    public double knockbackX = 0;
    private int attackCooldown = 0;
    private int hitCooldown = 0;
    private int movePattern = 0;
    private boolean enraged = false;

    // --- Damage-timing control ---
    public boolean canDealDamage = false;     // only true in the short damage window
    private boolean hasDealtDamage = false;   // prevent multiple hits per attack
    private final int ATTACK_START_FRAME = 3; // frame (0-based) where damage starts
    private final int ATTACK_END_FRAME = 4;   // frame where damage ends

    public Boss(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.width = frameWidth * SCALE;
        this.height = frameHeight * SCALE;

        idleSheet = loadImage("/assets/enemy_idle.png");
        walkSheet = loadImage("/assets/enemy_walk.png");
        attackSheet = loadImage("/assets/enemy_attack.png");

        setState(State.IDLE);
    }

    private Image loadImage(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) return new ImageIcon(url).getImage();
        } catch (Exception ignored) {}
        try {
            return new ImageIcon(path.replaceFirst("/", "")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load boss image: " + path);
            return null;
        }
    }

    public Rectangle getBounds() {
        // Damage hitbox - กลางตัว
        int hitW = (int)(width * 0.4);
        int hitH = (int)(height * 0.6);
        int hitX = (int)x + (width - hitW) / 2;
        int hitY = (int)y + (int)(height * 0.4);
        return new Rectangle(hitX, hitY, hitW, hitH);
    }

    public void setState(State s) {
        if (state == s) return;
        state = s;
        switch (s) {
            case IDLE: currentSheet = idleSheet; frameCount = 8; animSpeed = 10; break;
            case WALK: currentSheet = walkSheet; frameCount = 10; animSpeed = 8; break;
            case ATTACK: currentSheet = attackSheet; frameCount = 10; animSpeed = 6; break;
        }
        currentFrame = 0;
        animTimer = 0;
    }

    public void update(Player player, java.util.List<Platform> platforms) {
        // Enrage mode when health is low
        if (health < maxHealth / 2) {
            enraged = true;
        }

        // AI behavior
        int centerX = (int)x + width/2;
        int pCenter = (int)player.x + player.width/2;
        int distance = Math.abs(centerX - pCenter);

        attackCooldown--;
        hitCooldown--;

        // Boss AI - more aggressive than normal enemies
        double speed = enraged ? 5 : 3.5;
        int attackRange = enraged ? 200 : 150;
        int attackDelay = enraged ? 30 : 40;

        if (!isAttacking) {
            if (distance > 300) {
                // Far distance - aggressive chase
                setState(State.WALK);
                if (centerX < pCenter) {
                    x += speed;
                    facingRight = true;
                } else {
                    x -= speed;
                    facingRight = false;
                }
                movePattern++;
                if (movePattern > 60) movePattern = 0;
            } else if (distance > attackRange) {
                // Medium distance - approach carefully
                setState(State.WALK);
                if (centerX < pCenter) {
                    x += speed * 0.7;
                    facingRight = true;
                } else {
                    x -= speed * 0.7;
                    facingRight = false;
                }
            } else {
                // Close distance - attempt to start attack
                if (attackCooldown <= 0) {
                    // start attack
                    setState(State.ATTACK);
                    isAttacking = true;
                    canDealDamage = false;
                    hasDealtDamage = false;
                    attackCooldown = attackDelay;
                    // face the player
                    facingRight = centerX < pCenter;
                    // reset animation counters so we start from frame 0
                    currentFrame = 0;
                    animTimer = 0;
                } else {
                    setState(State.IDLE);
                    isAttacking = false;
                }
            }
        }
        // If already attacking, we keep running the attack animation and timing below

        // Apply gravity
        dy += 0.6;

        // Limit fall speed
        if (dy > 20) dy = 20;

        y += dy;

        // Apply knockback (Boss has more resistance)
        if (Math.abs(knockbackX) > 0.01) {
            x += knockbackX / 3.0;
            knockbackX *= 0.6;
            if (Math.abs(knockbackX) < 0.1) knockbackX = 0;
        }

        isGrounded = false;

        // Collision detection with platforms
        for (Platform p : platforms) {
            Rectangle bossRect = getBounds();
            Rectangle platRect = p.getBounds();

            if (bossRect.intersects(platRect)) {
                // Calculate overlap in each direction
                double overlapLeft = (x + width) - platRect.x;
                double overlapRight = (platRect.x + platRect.width) - x;
                double overlapTop = (y + height) - platRect.y;
                double overlapBottom = (platRect.y + platRect.height) - y;

                // Find minimum overlap
                double minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                        Math.min(overlapTop, overlapBottom));

                // Resolve collision based on minimum overlap
                if (minOverlap == overlapTop && dy > 0) {
                    // Collision from top (landing)
                    y = platRect.y - height;
                    dy = 0;
                    isGrounded = true;
                }
                else if (minOverlap == overlapBottom && dy < 0) {
                    // Collision from bottom
                    y = platRect.y + platRect.height;
                    dy = 0;
                }
                else if (minOverlap == overlapLeft) {
                    // Collision from left
                    x = platRect.x - width;
                }
                else if (minOverlap == overlapRight) {
                    // Collision from right
                    x = platRect.x + platRect.width;
                }
            }
        }

        // Animation
        animTimer++;
        if (animTimer >= animSpeed) {
            animTimer = 0;
            currentFrame++;

            // --- control damage window only during attack animation ---
            if (state == State.ATTACK) {
                // open the damage window on specific frames
                if (currentFrame == ATTACK_START_FRAME) {
                    canDealDamage = true;
                } else if (currentFrame > ATTACK_END_FRAME) {
                    canDealDamage = false;
                }
            } else {
                canDealDamage = false;
            }

            if (currentFrame >= frameCount) {
                currentFrame = 0;
                if (state == State.ATTACK) {
                    // attack animation finished
                    isAttacking = false;
                    canDealDamage = false;
                    hasDealtDamage = false;
                    setState(State.IDLE);
                }
            }
        }

        // Reset hit flag
        if (hit && hitCooldown <= 0) {
            hit = false;
        }
    }

    public Rectangle getAttackBox() {
        // Attack box - bigger than damage box
        int w = (int)(width * 0.8);
        int h = (int)(height * 0.8);
        // shift attack box a little forward based on facing direction
        int forwardOffset = (int)(width * 0.1);
        int ax = (int)x + (width - w) / 2 + (facingRight ? forwardOffset : -forwardOffset);
        int ay = (int)y + (int)(height * 0.1);
        return new Rectangle(ax, ay, w, h);
    }

    public void draw(Graphics2D g) {
        drawHealthBar(g);

        if (currentSheet == null) {
            g.setColor(new Color(150, 0, 0));
            g.fillRect((int)x, (int)y, width, height);
            return;
        }

        int sx = currentFrame * frameWidth;
        int sy = 0;
        int sw = frameWidth;
        int sh = frameHeight;

        // Hit flash effect
        Composite oldComposite = g.getComposite();
        if (hit) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g.setColor(new Color(255, 0, 0, 100));
            g.fillRect((int)x, (int)y, width, height);
        }

        // Enrage visual effect
        if (enraged) {
            g.setColor(new Color(255, 0, 0, 50));
            g.fillOval((int)x - 20, (int)y - 20, width + 40, height + 40);
        }

        // draw with facing
        if (facingRight) {
            g.drawImage(currentSheet, (int)x, (int)y, (int)x + width, (int)y + height,
                    sx, sy, sx + sw, sy + sh, null);
        } else {
            // Flip sprite when facing left
            g.drawImage(currentSheet, (int)x + width, (int)y, (int)x, (int)y + height,
                    sx, sy, sx + sw, sy + sh, null);
        }

        g.setComposite(oldComposite);

        if (hit) {
            g.setColor(new Color(255, 100, 100, 100));
            g.drawOval((int)x - 10, (int)y - 10, width + 20, height + 20);
        }

        // Debug: draw damage window visibility (optional)
        if (false) {
            if (canDealDamage) {
                g.setColor(new Color(255, 0, 0, 120));
                Rectangle ab = getAttackBox();
                g.fillRect(ab.x, ab.y, ab.width, ab.height);
            } else {
                // optional: draw attack box lightly
                g.setColor(new Color(255, 0, 0, 40));
                Rectangle ab = getAttackBox();
                g.drawRect(ab.x, ab.y, ab.width, ab.height);
            }
        }
    }

    private void drawHealthBar(Graphics2D g) {
        int barWidth = width;
        int barHeight = 12;
        int barX = (int)x;
        int barY = (int)y - 25;

        // Background
        g.setColor(new Color(50, 50, 50, 200));
        g.fillRect(barX, barY, barWidth, barHeight);

        // Health bar
        int currentBarWidth = (int)((double)health / maxHealth * barWidth);

        Color healthColor = enraged ? new Color(255, 0, 0) : new Color(200, 0, 0);
        Color healthColor2 = enraged ? new Color(255, 100, 0) : new Color(255, 100, 0);

        GradientPaint gp = new GradientPaint(
                barX, barY, healthColor,
                barX + currentBarWidth, barY, healthColor2);
        g.setPaint(gp);
        g.fillRect(barX, barY, currentBarWidth, barHeight);

        // Border
        g.setColor(new Color(255, 215, 0));
        g.drawRect(barX, barY, barWidth, barHeight);

        // Boss label
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(enraged ? new Color(255, 50, 50) : new Color(255, 215, 0));
        String bossText = enraged ? "BOSS - ENRAGED!" : "BOSS";
        int textWidth = g.getFontMetrics().stringWidth(bossText);
        g.drawString(bossText, barX + barWidth/2 - textWidth/2, barY - 5);
    }

    // Called by player when hitting the boss
    public void applyKnockback(double kb) {
        this.knockbackX += kb;
    }
}
