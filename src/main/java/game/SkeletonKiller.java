package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SkeletonKiller extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;
    private static final int BOSS_SPAWN_SCORE = 3000;

    private Timer timer;
    private Player player;
    private ArrayList<Platform> platforms;
    private ArrayList<Enemy> enemies;
    private ArrayList<Particle> particles;
    private ArrayList<Potion> potions;
    private Boss boss;
    private int score;
    private boolean gameOver;
    private boolean gameWon;
    private boolean bossSpawned;
    private Random random;
    private int cameraX;
    private int enemyKillCount = 0;
    private long lastPotionDropTime = 0;

    private double[] parallaxSpeeds;
    private Image[] backgroundLayers;

    public SkeletonKiller() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(20, 20, 30));
        setFocusable(true);
        addKeyListener(this);

        loadBackgrounds(); // โหลดพื้นหลังจาก resource path
        initGame();

        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    // ✅ โหลดพื้นหลังจาก resourcePath
    private void loadBackgrounds() {
        backgroundLayers = new Image[] {
                loadImage("/assets/bg1.png"),
                loadImage("/assets/bg2.png"),
                loadImage("/assets/bg3.png")
        };
        parallaxSpeeds = new double[] { 0.2, 0.4, 0.6 };

        for (int i = 0; i < backgroundLayers.length; i++) {
            if (backgroundLayers[i] == null) {
                System.err.println("⚠️ Background " + i + " failed to load!");
            } else {
                System.out.println("✅ Background " + i + " loaded successfully.");
            }
        }
    }

    // ✅ โหลดภาพแบบ resource path
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

    private void initGame() {
        random = new Random();
        platforms = new ArrayList<>();
        enemies = new ArrayList<>();
        particles = new ArrayList<>();
        potions = new ArrayList<>();
        boss = null;
        score = 0;
        gameOver = false;
        gameWon = false;
        bossSpawned = false;
        cameraX = 0;
        enemyKillCount = 0;
        lastPotionDropTime = System.currentTimeMillis();

        createLevel();
        player = new Player(100, 100);
        spawnEnemies();
    }

    private void createLevel() {
        // พื้นล่างสุด
        platforms.add(new Platform(0, HEIGHT - 90, WIDTH * 3, 60, true, 0));

        // ขอบซ้ายและขวา
        platforms.add(new Platform(-200, 0, 200, HEIGHT, true, 0));
        platforms.add(new Platform(WIDTH * 3, 0, 200, HEIGHT, true, 0));

        // แพลตฟอร์มระดับกลาง
        int baseY = HEIGHT - 300;
        for (int i = 0; i < 20; i++) {
            int x = 500 + i * 250 + random.nextInt(60);
            int y = baseY - random.nextInt(100);
            int w = 100 + random.nextInt(100);
            platforms.add(new Platform(x, y, w, 20));
        }
    }

    private void spawnEnemies() {
        for (int i = 0; i < 5; i++) {
            enemies.add(new Enemy(300 + i * 350, 50));
        }
    }

    private void spawnBoss() {
        if (!bossSpawned) {
            bossSpawned = true;
            enemies.clear();
            boss = new Boss(cameraX + WIDTH / 2 + 200, 50);
            lastPotionDropTime = System.currentTimeMillis();
            System.out.println("⚠️ BOSS SPAWNED!");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !gameWon) update();
        repaint();
    }

    private void update() {
        player.update(platforms);
        player.x = Math.max(0, Math.min(player.x, WIDTH * 3 - player.width));

        if (score >= BOSS_SPAWN_SCORE && !bossSpawned) spawnBoss();

        if (boss != null) {
            boss.update(player, platforms);
            handleBossCombat();
            handleBossPotionDrops();
        } else {
            updateEnemies();
        }

        updatePotions();

        particles.removeIf(p -> {
            p.update();
            return p.life <= 0;
        });

        cameraX = (int) (player.x - WIDTH / 2);
        cameraX = Math.max(0, cameraX);
    }

    private void handleBossCombat() {
        if (player.isAttacking && player.canDealDamage && !player.hasDealtDamage &&
                player.getAttackBox().intersects(boss.getBounds())) {

            int actualDamage = player.powerBuff ? player.damage * 2 : player.damage;
            boss.health -= actualDamage;
            boss.hit = true;
            boss.knockbackX = player.facingRight ? 20 : -20;
            createHitParticles(boss.x + boss.width / 2, boss.y + boss.height / 2);

            player.hasDealtDamage = true;
            player.canDealDamage = false;

            if (boss.health <= 0) {
                boss = null;
                score += 1000;
                gameWon = true;
            }
        }

        if (boss != null && boss.isAttacking && boss.getAttackBox().intersects(player.getBoundsWorld())) {
            if (!player.isHit) {
                player.health -= boss.damage;
                player.isHit = true;
                player.hitTimer = 30;
                player.knockbackX = boss.x < player.x ? 25 : -25;

                if (player.health <= 0) gameOver = true;
            }
        }
    }

    private void updateEnemies() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(player, platforms);
            enemy.x = Math.max(0, Math.min(enemy.x, WIDTH * 3 - enemy.width));

            if (player.isAttacking && player.canDealDamage && !player.hasDealtDamage &&
                    player.getAttackBox().intersects(enemy.getBounds())) {

                int actualDamage = player.powerBuff ? player.damage * 2 : player.damage;
                enemy.health -= actualDamage;
                enemy.hit = true;
                enemy.knockbackX = player.facingRight ? 16 : -16;
                createHitParticles(enemy.x, enemy.y);

                player.hasDealtDamage = true;
                player.canDealDamage = false;

                if (enemy.health <= 0) {
                    enemies.remove(i);
                    score += 100;
                    enemyKillCount++;

                    if (enemyKillCount % 5 == 0) dropPotion(enemy.x, enemy.y);

                    if (score < BOSS_SPAWN_SCORE)
                        enemies.add(new Enemy(cameraX + WIDTH + 200, 50));
                }
            }

            if (i < enemies.size() && enemy.isAttacking &&
                    enemy.getAttackBox().intersects(player.getBoundsWorld())) {
                if (!player.isHit) {
                    player.health -= enemy.damage;
                    player.isHit = true;
                    player.hitTimer = 30;
                    player.knockbackX = enemy.x < player.x ? 20 : -20;
                    if (player.health <= 0) gameOver = true;
                }
            }
        }
    }

    private void handleBossPotionDrops() {
        long now = System.currentTimeMillis();
        if (now - lastPotionDropTime >= 5000) {
            double x = cameraX + random.nextInt(WIDTH - 100) + 50;
            potions.add(new Potion(x, 0, Potion.Type.values()[random.nextInt(3)]));
            lastPotionDropTime = now;
        }
    }

    private void updatePotions() {
        for (Potion p : potions) {
            if (!p.collected) {
                p.y += 4;
                for (Platform plat : platforms) {
                    if (p.getBounds().intersects(plat.getBounds())) {
                        p.y = plat.y - p.height;
                    }
                }

                if (p.getBounds().intersects(player.getBoundsWorld())) {
                    p.collected = true;
                    switch (p.type) {
                        case HEALTH -> player.health = Math.min(100, player.health + 50);
                        case SPEED -> {
                            player.speedBuff = true;
                            player.buffTimer = 8 * 60;
                        }
                        case POWER -> {
                            player.powerBuff = true;
                            player.buffTimer = 8 * 60;
                        }
                    }
                }
            }
        }
    }

    private void dropPotion(double x, double y) {
        Potion.Type type = Potion.Type.values()[random.nextInt(3)];
        potions.add(new Potion(x, y, type));
    }

    private void createHitParticles(double x, double y) {
        for (int i = 0; i < 12; i++) particles.add(new Particle(x, y));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 🌄 วาดพื้นหลังแบบ parallax
        for (int i = 0; i < backgroundLayers.length; i++) {
            Image bg = backgroundLayers[i];
            if (bg == null) continue;
            double offset = -(cameraX * parallaxSpeeds[i]) % WIDTH;
            g2d.drawImage(bg, (int) offset, 0, WIDTH, HEIGHT, this);
            g2d.drawImage(bg, (int) offset + WIDTH, 0, WIDTH, HEIGHT, this);
        }

        if (!gameOver && !gameWon) {
            g2d.translate(-cameraX, 0);

            for (Platform p : platforms) p.draw(g2d);
            for (Potion p : potions) p.draw(g2d);
            for (Particle p : particles) p.draw(g2d);
            if (boss != null) boss.draw(g2d);
            for (Enemy e : enemies) e.draw(g2d);
            player.draw(g2d);

            g2d.translate(cameraX, 0);
            drawUI(g2d);
        } else if (gameOver) {
            drawGameOver(g2d);
        } else if (gameWon) {
            drawGameWon(g2d);
        }
    }

    // ============================ UI ============================
    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(200, 50, 50));
        g.setFont(new Font("Arial", Font.BOLD, 60));
        String text = "YOU DIED";
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, WIDTH / 2 - w / 2, HEIGHT / 2 - 30);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        text = "Final Score: " + score;
        w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, WIDTH / 2 - w / 2, HEIGHT / 2 + 20);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        text = "Press R to Restart";
        w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, WIDTH / 2 - w / 2, HEIGHT / 2 + 60);
    }

    private void drawGameWon(Graphics2D g) {
        g.setColor(new Color(255, 215, 0, 70));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("Arial", Font.BOLD, 70));
        String text = "VICTORY!";
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, WIDTH / 2 - w / 2, HEIGHT / 2 - 50);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        text = "Boss Defeated!";
        w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, WIDTH / 2 - w / 2, HEIGHT / 2 + 10);

        g.setFont(new Font("Arial", Font.BOLD, 30));
        text = "Final Score: " + score;
        w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, WIDTH / 2 - w / 2, HEIGHT / 2 + 60);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(new Color(255, 215, 0));
        text = "Press R to Play Again";
        w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, WIDTH / 2 - w / 2, HEIGHT / 2 + 100);
    }

    private void drawUI(Graphics2D g) {
        g.setColor(new Color(50, 50, 60, 200));
        g.fillRoundRect(15, 15, 214, 34, 10, 10);
        g.setColor(Color.RED);
        g.fillRoundRect(20, 20, (int) (player.health * 2), 24, 8, 8);
        g.setColor(Color.WHITE);
        g.drawRoundRect(20, 20, 204, 24, 8, 8);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.YELLOW);
        g.drawString("Score: " + score, 20, 70);

        if (player.speedBuff || player.powerBuff) {
            int y = 110;
            g.setFont(new Font("Arial", Font.BOLD, 16));
            if (player.speedBuff) {
                g.setColor(Color.CYAN);
                g.drawString("⚡ Speed Up (" + (player.buffTimer / 60) + "s)", 20, y);
                y += 20;
            }
            if (player.powerBuff) {
                g.setColor(Color.ORANGE);
                g.drawString("💪 Power Up (" + (player.buffTimer / 60) + "s)", 20, y);
            }
        }
    }

    // ============================ Key Controls ============================
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if ((gameOver || gameWon) && key == KeyEvent.VK_R) {
            initGame();
            return;
        }
        if (!gameOver && !gameWon) {
            switch (key) {
                case KeyEvent.VK_A -> player.moveLeft = true;
                case KeyEvent.VK_D -> player.moveRight = true;
                case KeyEvent.VK_W -> player.jump();
                case KeyEvent.VK_J -> player.startAttack();
                case KeyEvent.VK_K -> player.dash();
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) player.moveLeft = false;
        if (e.getKeyCode() == KeyEvent.VK_D) player.moveRight = false;
    }
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Skeleton Killer");
            frame.add(new SkeletonKiller());
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
