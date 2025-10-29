package game;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Player {
    // position & physics
    public double x, y;
    public double dx, dy;
    public int width, height;

    // input / state
    public boolean moveLeft, moveRight;
    public boolean facingRight = true;
    public boolean isGrounded = false;
    public boolean hasDealtDamage = false;

    // combat/state
    public boolean isAttacking = false;
    public boolean isHit = false;
    public boolean canDealDamage = false;
    public int health = 100;
    public int damage = 25;
    public int hitTimer = 0;
    public double knockbackX = 0;
    public int attackCooldown = 0;

    // jump buffering
    private boolean jumpQueued = false;
    private int jumpBufferTime = 8;
    private int jumpBufferTimer = 0;

    // sprite/animation
    private Image playerIdle, playerRun, playerDash, playerAttack, playerAttack2;
    private Image currentSheet;

    private int frameWidth, frameHeight;
    private int frameCount;
    private int currentFrame = 0;
    private int animTimer = 0;
    private int animSpeed = 6;

    private static final int IDLE_FRAMES = 10;
    private static final int RUN_FRAMES = 10;
    private static final int DASH_FRAMES = 2;
    private static final int ATTACK_FRAMES = 6;
    private static final int ATTACK2_FRAMES = 6;
    private static final int SCALE = 2;

    private enum State { IDLE, RUN, DASH, ATTACK, ATTACK2 }
    private State state = State.IDLE;

    // dash system
    private boolean isDashing = false;
    private int dashTimer = 0;
    private int dashCooldown = 0;
    private double dashSpeed = 20;

    // trail effect
    private static class Trail {
        double x, y;
        int life;
        boolean facingRight;

        Trail(double x, double y, boolean facingRight) {
            this.x = x;
            this.y = y;
            this.life = 10; // 10 เฟรมแล้วหาย
            this.facingRight = facingRight;
        }
    }
    private final List<Trail> trails = new ArrayList<>();

    // potion buffs
    public boolean speedBuff = false;
    public boolean powerBuff = false;
    public int buffTimer = 0;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.frameWidth = 120;
        this.frameHeight = 80;
        this.width = frameWidth * SCALE;
        this.height = frameHeight * SCALE;

        playerIdle = loadImage("/assets/player_idle.png");
        playerRun = loadImage("/assets/player_run.png");
        playerDash = loadImage("/assets/player_dash.png");
        playerAttack = loadImage("/assets/player_attack.png");
        playerAttack2 = loadImage("/assets/player_attack2.png");

        currentSheet = playerIdle;
        frameCount = IDLE_FRAMES;
    }

    private Image loadImage(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) return new ImageIcon(url).getImage();
        } catch (Exception ignored) {}
        try {
            return new ImageIcon(path.replaceFirst("/", "")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path);
            return null;
        }
    }

    // Hitbox
    public Rectangle getBounds() {
        int hitW = (int)(width * 0.25);
        int hitH = (int)(height * 0.6);
        int hitX = (int)x + (width - hitW) / 2;
        int hitY = (int)y + (int)(height * 0.4);
        return new Rectangle(hitX, hitY, hitW, hitH);
    }

    public Rectangle getBoundsWorld() {
        return getBounds();
    }

    // Attack Box
    public Rectangle getAttackBox() {
        int w = (int)(width * 0.5);
        int h = (int)(height * 0.7);
        int ax = facingRight ? (int)(x + width * 0.4) : (int)(x + width * 0.4 - w);
        int ay = (int)(y + height * 0.35);
        return new Rectangle(ax, ay, w, h);
    }

    // Attack
    public void startAttack() {
        if (!isAttacking && attackCooldown <= 0 && !isDashing) {
            isAttacking = true;
            canDealDamage = false;
            hasDealtDamage = false;
            attackCooldown = 10;
            if (Math.random() < 0.5) setState(State.ATTACK);
            else setState(State.ATTACK2);
            currentFrame = 0;
            animTimer = 0;
            animSpeed = 4;
        }
    }

    // Dash
    public void dash() {
        if (!isDashing && dashCooldown <= 0 && !isAttacking) {
            isDashing = true;
            dashTimer = 14;
            dashCooldown = 40;
            dy = 0;
            dx = facingRight ? dashSpeed : -dashSpeed;
            setState(State.DASH);
            // สร้าง trail ชุดแรก
            trails.clear();
            trails.add(new Trail(x, y, facingRight));
        }
    }

    // Jump
    public void jump() {
        if (isGrounded) {
            dy = -18;
            isGrounded = false;
            jumpQueued = false;
        } else {
            jumpQueued = true;
            jumpBufferTimer = jumpBufferTime;
        }
    }

    private void setState(State s) {
        if (state == s) return;
        state = s;
        switch (s) {
            case IDLE -> { currentSheet = playerIdle; frameCount = IDLE_FRAMES; animSpeed = 8; }
            case RUN -> { currentSheet = playerRun; frameCount = RUN_FRAMES; animSpeed = 6; }
            case DASH -> { currentSheet = playerDash; frameCount = DASH_FRAMES; animSpeed = 4; }
            case ATTACK -> { currentSheet = playerAttack; frameCount = ATTACK_FRAMES; animSpeed = 4; }
            case ATTACK2 -> { currentSheet = playerAttack2; frameCount = ATTACK2_FRAMES; animSpeed = 4; }
        }
        currentFrame = 0;
        animTimer = 0;
    }

    public void update(java.util.List<Platform> platforms) {
        double baseSpeed = 5.0;
        double speed = speedBuff ? baseSpeed * 1.5 : baseSpeed;

        // Movement
        if (!isDashing) {
            if (moveLeft && !isAttacking) {
                dx = -speed;
                facingRight = false;
                setState(State.RUN);
            } else if (moveRight && !isAttacking) {
                dx = speed;
                facingRight = true;
                setState(State.RUN);
            } else {
                dx = 0;
                if (!isAttacking) setState(State.IDLE);
            }
        }

        // Gravity
        if (!isDashing) {
            dy += 0.6;
            if (dy > 20) dy = 20;
        }

        // Dash update
        if (isDashing) {
            dashTimer--;
            x += dx;
            dy = 0;

            // เพิ่ม trail ทุก 2 เฟรม
            if (dashTimer % 2 == 0) {
                trails.add(new Trail(x, y, facingRight));
            }

            if (dashTimer <= 0) {
                isDashing = false;
                dx = 0;
                setState(State.IDLE);
            }
        } else {
            x += dx;
            y += dy;
        }

        // Collision
        isGrounded = false;
        for (Platform p : platforms) {
            Rectangle playerRect = getBounds();
            Rectangle platRect = p.getBounds();

            if (playerRect.intersects(platRect)) {
                double overlapTop = (y + height) - platRect.y;
                double overlapBottom = (platRect.y + platRect.height) - y;
                if (overlapTop < overlapBottom && dy > 0) {
                    y = platRect.y - height;
                    dy = 0;
                    isGrounded = true;
                }
            }
        }

        // Jump buffer
        if (jumpQueued && isGrounded) {
            dy = -15;
            isGrounded = false;
            jumpQueued = false;
        }

        if (jumpQueued) {
            jumpBufferTimer--;
            if (jumpBufferTimer <= 0) jumpQueued = false;
        }

        // Knockback
        if (Math.abs(knockbackX) > 0.1) {
            x += knockbackX;
            knockbackX *= 0.8;
        }

        // Trail fade
        trails.removeIf(t -> --t.life <= 0);

        // Animation
        animTimer++;
// Animation
        animTimer++;
        if (animTimer >= animSpeed) {
            animTimer = 0;
            currentFrame++;

            // ✅ ถ้าเป็นการโจมตี ให้เปิด/ปิด damage window
            if (isAttacking) {
                if (currentFrame == 1 && !canDealDamage) {
                    canDealDamage = true;
                    hasDealtDamage = false;
                } else if (currentFrame > 1 && canDealDamage) {
                    canDealDamage = false;
                }
            }

            // ✅ ถ้า animation จบ
            if (currentFrame >= frameCount) {
                currentFrame = 0;

                // ถ้าเป็นการโจมตี → กลับสู่ Idle
                if (state == State.ATTACK || state == State.ATTACK2) {
                    isAttacking = false;
                    canDealDamage = false;

                    // ถ้ายังถือปุ่มเดินอยู่ → กลับไป RUN
                    if (moveLeft || moveRight) setState(State.RUN);
                    else setState(State.IDLE);
                }

                // ถ้า Dash จบ → กลับเป็น Idle
                if (state == State.DASH && !isDashing) {
                    setState(State.IDLE);
                }
            }
        }


        // Timers
        if (hitTimer > 0) hitTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (dashCooldown > 0) dashCooldown--;
        if (buffTimer > 0) {
            buffTimer--;
            if (buffTimer <= 0) {
                speedBuff = false;
                powerBuff = false;
            }
        }
    }

    // Draw player
    public void draw(Graphics2D g) {
        // draw dash trails
        for (Trail t : trails) {
            float alpha = t.life / 10f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.4f));
            g.setColor(new Color(255, 240, 100));
            int offset = t.facingRight ? 20 : -20;
            g.fillRoundRect((int)t.x + offset, (int)t.y + height / 3, width / 2, 10, 10, 10);
        }

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // draw player sprite
        int sx = currentFrame * frameWidth;
        int sy = 0;
        int sw = frameWidth;
        int sh = frameHeight;

        if (facingRight)
            g.drawImage(currentSheet, (int)x, (int)y, (int)x + width, (int)y + height,
                    sx, sy, sx + sw, sy + sh, null);
        else
            g.drawImage(currentSheet, (int)x + width, (int)y, (int)x, (int)y + height,
                    sx, sy, sx + sw, sy + sh, null);

//        // debug hitboxes
//        g.setColor(new Color(0, 255, 0, 150));
//        Rectangle b = getBounds();
//        g.drawRect(b.x, b.y, b.width, b.height);
//
//        if (isAttacking) {
//            g.setColor(canDealDamage ? new Color(255, 0, 0, 200)
//                    : new Color(255, 0, 0, 80));
//            Rectangle atk = getAttackBox();
//            g.drawRect(atk.x, atk.y, atk.width, atk.height);
//        }
    }
}
