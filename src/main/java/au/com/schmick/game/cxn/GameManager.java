package au.com.schmick.game.cxn;

import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.SplashScreen;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.io.IOUtils;

/**
 * @author mesketh
 */
public class GameManager implements Runnable, GameState {

    private final static String REACTION_SOUND = "beep-02.wav";
    private final static String LEVEL_SOUND = "todo";
    private byte[] reactorEffectRawData;

    private volatile boolean gameOver;
    private volatile boolean isRunning;
    private volatile boolean isPaused;
    protected volatile boolean isReactorAdded;
    protected volatile boolean isBetweenLevels;
    protected volatile boolean isGameInProgress;
    protected volatile boolean isOnFirstLevel = true;

    private volatile Object lock = new Object();

    private volatile int score;
    private volatile int levelScore;

    private JFrame gameFrame;

    private GameCanvas gameCanvas;

    private GameMessager gameMessager;

    private CardLayout cardManager;

    private volatile List<Ball> ballList = new ArrayList<Ball>();

    private List<Reactor> reactorList;

    private Thread animatorThread;

    private enum SplashScreenStages {

        INIT_UI_START("Initialising UI...", 0), INIT_EFFECTS_START(
                "Loading effects...", 0);

        private String desc;
        private int progress;

        /**
         * Splash screen msg details to display - progress == %progress.
         *
         * @param desc     Msg on LHS of the progress
         * @param progress (0-100)
         */
        SplashScreenStages(String desc, int progress) {
            this.desc = desc;
            this.progress = progress;
        }

        public String getDesc() {
            return desc;
        }

        public int getProgress() {
            return progress;
        }

        public String getMessage() {
            return getDesc();
        }

    }

    // Game layout tags
    private enum GameScreen {

        GAME_LEVEL, INFO
    }

    private void quitGame() {
        LevelManager.resetLevels();
        this.score = 0;
        this.levelScore = 0;
    }

    private void initaliseEffects() {
        try {
            this.reactorEffectRawData = IOUtils.toByteArray(GameManager.class
                    .getResourceAsStream("sounds/" + REACTION_SOUND));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void run() {

        this.isRunning = true;
        do {
            if (!isBetweenLevels) {

                // System.out.println("Playing");
                updateGameState();
                this.gameCanvas.renderGameState();

                purgeReactors();
                purgeBalls();
            }

            if (!isBetweenLevels && isLevelOver()) {
                this.isBetweenLevels = true;
                this.isReactorAdded = false;
                updateScore();
                changeScreen(GameScreen.INFO);
                if (isGameFinished()) {
                    try {
                        changeScreen(GameScreen.INFO);
                        synchronized (this.lock) {
                            this.lock.wait();
                        }
                        isBetweenLevels = false;
                        changeScreen(GameScreen.GAME_LEVEL);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                } else {
                    try {
                        synchronized (this.lock) {
                            this.lock.wait();
                        }
                        changeScreen(GameScreen.GAME_LEVEL);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } while (isRunning);

    }

    private void updateScore() {
        this.score += this.levelScore;
    }

    private void changeScreen(GameScreen targetScreen) {
        this.cardManager.show(this.gameFrame.getContentPane(),
                targetScreen.name());

        switch (targetScreen) {

            case GAME_LEVEL:
                this.gameCanvas.setFocusable(true);
                this.gameCanvas.requestFocus();
                break;
            case INFO:
                this.gameMessager.setFocusable(true);
                this.gameMessager.requestFocus();
                break;
        }
    }

    public boolean isLevelOver() {

        boolean levelDone = false;

        if (this.isReactorAdded) { // level has begun (and may have finished)
            if (LevelManager.isLevelPassed(this)) {
                if (this.reactorList != null && this.reactorList.isEmpty()) {
                    levelDone = true; // level complete - advance to next level
                }
            } else if (this.reactorList != null && this.reactorList.isEmpty()) {
                levelDone = true; // level is done but, will have to be repeated
            }
        }

        return levelDone;
    }

    private void updateGameState() {
        if (!isBetweenLevels && !isPaused) {
            this.gameCanvas.moveBalls();
            this.checkCollisions();
        }
    }

    public boolean isGameStarted() {
        return isGameInProgress;
    }

    private void purgeReactors() {

        if (this.getReactors() != null) {

            Iterator<Reactor> ri = this.getReactors().iterator();

            while (ri.hasNext()) {
                Reactor nextReactor = ri.next();
                if (!nextReactor.isAlive()) {
                    ri.remove();
                }
            }
        }
    }

    private void purgeBalls() {

        if (this.getBalls() != null) {

            Iterator<Ball> bi = this.getBalls().iterator();

            while (bi.hasNext()) {
                Ball nextBall = bi.next();
                if (!nextBall.isAlive()) {
                    bi.remove();
                }
            }
        }
    }

    public void initialiseUI() {

        this.gameFrame = new JFrame(
                "Chain Rxn v1.0.0b - Copyright \u00a9 Schmick Software 2012");

        intialiseScreens(this.gameFrame.getContentPane());
        // TODO: Log point for splash

        gameFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        changeScreen(GameScreen.INFO);

        this.gameFrame.pack();
        this.gameFrame.setVisible(true);
    }

    private void startGame() {

        if (this.animatorThread == null) {
            this.animatorThread = new Thread(this, "CHAINRXN-THREAD");
            this.animatorThread.start();
        } else {
            synchronized (this.lock) {
                lock.notify();
            }
        }

        changeScreen(GameScreen.GAME_LEVEL);
    }

    private void intialiseScreens(Container parentContainer) {
        this.cardManager = new CardLayout(10, 10);
        parentContainer.setLayout(this.cardManager);

        this.gameMessager = new GameMessager(this);
        this.gameMessager.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                // System.out.println("**** GameMessager has lost focus ****");
            }

        });

        parentContainer.add(this.gameMessager, GameScreen.INFO.name());
        parentContainer.add(this.gameCanvas = new GameCanvas(this),
                GameScreen.GAME_LEVEL.name());

        this.gameCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                GameManager.this.gameCanvas.requestFocusInWindow();
                if (!isReactorAdded && !isPaused) {
                    addReactor(e.getX(), e.getY());
                    GameManager.this.isReactorAdded = true;
                }
            }
        });

        this.gameCanvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    if (GameManager.this.isGameInProgress
                            && !GameManager.this.isBetweenLevels) {
                        GameManager.this.isPaused = !isPaused;
                    }
                }
            }
        });

        this.gameMessager.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_N) {
                    if (GameManager.this.isBetweenLevels
                            && LevelManager.isLevelPassed(GameManager.this)
                            && !GameManager.this.isGameFinished()) {
                        GameManager.this.loadNextLevel();
                        changeScreen(GameScreen.GAME_LEVEL);
                        GameManager.this.isBetweenLevels = false;
                        GameManager.this.isReactorAdded = false;
                        GameManager.this.levelScore = 0;
                        // TODO - should ask for confirmation here
                        synchronized (GameManager.this.lock) {
                            GameManager.this.lock.notify();
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_X) {
                    stopGame();
                    quitGame();
                    prepareGame();
                    synchronized (GameManager.this.lock) {
                        GameManager.this.lock.notify();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_S) {
                    if (!GameManager.this.isGameInProgress) {
                        prepareGame();
                        startGame();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_A) {
                    if (GameManager.this.isGameInProgress
                            && GameManager.this.isBetweenLevels
                            && !LevelManager.isLevelPassed(GameManager.this)) {
                        GameManager.this.restartLevel();
                        changeScreen(GameScreen.GAME_LEVEL);
                        GameManager.this.isBetweenLevels = false;
                        GameManager.this.isReactorAdded = false;
                        synchronized (GameManager.this.lock) {
                            GameManager.this.lock.notify();
                        }
                    }
                }
            }

        });

    }

    /**
     * TODO
     */
    public void checkCollisions() {

        if (this.reactorList != null && !this.reactorList.isEmpty()) {

            List<Reactor> newReactorList = new ArrayList<Reactor>();
            for (Ball nextBall : this.ballList) {
                Iterator<Reactor> reactorIter = this.reactorList.iterator();
                while (reactorIter.hasNext()) {
                    Reactor nextReactor = reactorIter.next();
                    if (nextReactor.isCollidingWith(nextBall)) {
                        Point2D intersection = calculatePointOfIntersection(
                                nextReactor, nextBall);
                        newReactorList.add(new Reactor(intersection,
                                calculateScore()));
                        nextBall.setAlive(false);

                        playSound();
                    }
                }
            }

            if (!newReactorList.isEmpty()) {
                this.reactorList.addAll(newReactorList);
            }
        }
    }

    private int calculateScore() {
        if (this.reactorList.isEmpty())
            return levelScore;

        if (this.reactorList.size() == 1) {
            levelScore += LevelManager.getCurrentLevel().getBallScore();
        } else {
            // peg the score to the length of the chain (chain length * level
            // ball score)
            levelScore += (this.reactorList.size())
                    * LevelManager.getCurrentLevel().getBallScore();
        }
        return levelScore;
    }

    private final void addReactor(int x, int y) {

        if (this.reactorList == null) {
            this.reactorList = new ArrayList<Reactor>();
        }

        this.reactorList.add(new Reactor(x, y));

    }

    private Point2D calculatePointOfIntersection(Reactor reactor, Ball ball) {

        Point2D reactorCentre = reactor.getCentre();
        Point2D ballCentre = ball.getCentre();

        double ballToReactorRatio = ((double) ball.getRadius() / (double) reactor
                .getRadius());
        double dx = (Math.abs(reactorCentre.getX() - ball.getX()))
                - ballToReactorRatio
                * Math.abs(reactorCentre.getX() - ball.getX());
        double dy = (Math.abs(reactorCentre.getY() - ball.getY()))
                - ballToReactorRatio
                * Math.abs(reactorCentre.getY() - ball.getY());

        if (ballCentre.getX() <= reactorCentre.getX()) {
            dx = -dx;
        }

        if (ballCentre.getY() <= reactorCentre.getY()) {
            dy = -dy;
        }

        Point intersection = new Point((Point) reactorCentre);
        intersection.translate(BigDecimal.valueOf(dx).intValue(), BigDecimal
                .valueOf(dy).intValue());
        return intersection;
    }

    public boolean isPaused() {
        return this.isPaused;
    }

    public List<Reactor> getReactors() {
        return this.reactorList;
    }

    public List<Ball> getBalls() {
        return this.ballList;
    }

    // game lifecycle //

    private void prepareGame() {

        loadNextLevel();
        this.isGameInProgress = true;
        // startGame();
    }

    void loadNextLevel() {
        LevelManager.nextLevel();
        loadLevel();
    }

    private void loadLevel() {
        this.ballList.clear();
        for (int i = 0; i < LevelManager.getCurrentLevel().getMaxBalls(); i++) {
            this.ballList.add(new Ball());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void restartLevel() {
        this.reactorList.clear();
        this.loadLevel();
        this.score -= this.levelScore;
        this.levelScore = 0;
        // this.loadNextLevel();
    }

    private void stopGame() {
        // this.isRunning = false;
        this.isGameInProgress = false;
    }

    public int getCurrentScore() {
        return this.score;
    }

    public int getBallsLeft() {
        return LevelManager.getCurrentLevel().getMinBallsToPass()
                - (LevelManager.getCurrentLevel().getMaxBalls() - this.ballList
                .size());
    }

    private void init() {

        updateProgress(SplashScreenStages.INIT_EFFECTS_START);
        initaliseEffects();

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        updateProgress(SplashScreenStages.INIT_UI_START);
        initialiseUI();
    }

    public static void main(String[] args) {
        GameManager mgr = new GameManager();
        mgr.init();
    }

    public boolean isAdvancing() {
        boolean advancing = false;

        int ballsToAdvance = getBallsLeft();
        if (ballsToAdvance > 0) {
            advancing = false;
        } else {
            advancing = true;
        }

        return advancing;
    }

    public boolean isGameFinished() {
        return LevelManager.isLastLevel() && LevelManager.isLevelPassed(this);
    }

    public synchronized void playSound() {
      if (reactorEffectRawData != null) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Clip clip = AudioSystem.getClip();
                    AudioInputStream effectStream = AudioSystem
                            .getAudioInputStream(new ByteArrayInputStream(
                                    GameManager.this.reactorEffectRawData));
                    clip.open(effectStream);
                    clip.start();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
      }
    }

    private void updateProgress(SplashScreenStages stage) {
        SplashScreen ss = SplashScreen.getSplashScreen();
        if (ss != null) {
            Graphics2D ssg = ss.createGraphics();
            ssg.setComposite(AlphaComposite.Clear);
            ssg.fillRect(450, GameCanvas.PREFERRED_SIZE.height - 70,
                    GameCanvas.PREFERRED_SIZE.width,
                    GameCanvas.PREFERRED_SIZE.height);
            ssg.setPaintMode();

            Font f = new Font("Courier", Font.BOLD, 14);
            ssg.setFont(f);
            FontMetrics fm = ssg.getFontMetrics(f);

            ssg.drawString(stage.getMessage(), 450,
                    GameCanvas.PREFERRED_SIZE.height - 40
                            - (fm.getMaxAscent() + fm.getMaxDescent()));
            ss.update();
        }
    }
}

@SuppressWarnings("serial")
class GameMessager extends JPanel {

    GameState gameState;

    GameMessager(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public void paintComponent(Graphics g) {
        clearBackground((Graphics2D) g);
        renderMessage((Graphics2D) g);
    }

    private void clearBackground(Graphics2D g) {
        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, getSize().width, getSize().height);
    }

    private String getLevelMessage() {

        // display the splash screen before game starts
        if (!this.gameState.isGameStarted()) {
            // TODO: Check
            return String.format("Welcome - Press S to start");
        } else if (this.gameState.isAdvancing()) {
            if (!this.gameState.isGameFinished()) {
                return String
                        .format("Current Score: %d\n\n Press N to proceed to next level",
                                this.gameState.getCurrentScore());
            } else {
                return String
                        .format("Game finished - your score was: %d - Press S to play again",
                                this.gameState.getCurrentScore());
            }
        } else {
            return String
                    .format("Failed to complete level - Press A to try again");
        }
    }

    public void renderMessage(Graphics2D g) {
        Color currColor = g.getColor();
        g.setColor(Color.WHITE);
        g.drawString(getLevelMessage(), 50, 50);
        g.setColor(currColor);
    }

}
