package au.com.schmick.game.cxn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.swing.JPanel;

/**
 * Custom component that handles the following tasks:
 * <p>
 * <ul>
 * <li>Manages the current list of balls and displays them when painting
 * <li>Animates each ball (frame) on request
 * <li>Checks a given TODO
 * </ul>
 * 
 * @author mesketh
 */
@SuppressWarnings("serial")
public class GameCanvas extends JPanel {

	// private static final int INITIAL_BALLOON_COUNT = 10;
	public static final Dimension PREFERRED_SIZE = new Dimension(640, 480); // 4:3
	private final static long FRAME_DELAY = 60; //

	private BufferedImage imgBuf;

	private int reactionCount;

	private GameState gameState;

	public GameCanvas(GameState gameState) {
		this.gameState = gameState;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#getMaximumSize()
	 */
	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {
		return PREFERRED_SIZE;
	}

	/**
	 * Canvas has been added to a parent component.
	 */
	public void addNotify() {

		super.addNotify();

		// create the db image
		this.imgBuf = new BufferedImage(getPreferredSize().width,
				getPreferredSize().height, BufferedImage.TYPE_INT_RGB);

		setFocusable(true);
		requestFocus();
	}

	private void clearBackground(Graphics2D g) {
		g.setBackground(Color.BLACK);
		g.clearRect(0, 0, getSize().width, getSize().height);
	}
	
	private void paintBackground(Graphics2D g) {
	   // TODO Paint a nice starry background here... 	
	}

	// engine logic //
	public void moveBalls() {
		Rectangle bounds = getBounds();
		for (Ball nextB : this.gameState.getBalls()) {
			nextB.move(bounds);
		}
	}

	private void paintFrame() {
		long renderStartTime = 0L;
		try {

			// blit to the screen
			renderStartTime = System.currentTimeMillis();
			Graphics2D g = (Graphics2D) getGraphics();
			if (g != null) {
				g.drawImage(this.imgBuf, 0, 0, null);
			}
			// TODO Linux only** May not hold anymore - check and remove
			Toolkit.getDefaultToolkit().sync();
			g.dispose();
		} finally {
			// wait up to the frame period if we're running ahead of the frame
			// rate
			try {
				long currTime = System.currentTimeMillis();
				long renderTime = currTime - renderStartTime;
				if (renderTime < FRAME_DELAY) {
					Thread.sleep(FRAME_DELAY - renderTime);
				} else {
					Thread.sleep(5); // TODO sleep for 5ms??
				}
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * TODO
	 */
	public void renderGameState() {
		renderFrame();
		paintFrame();
	}

	private void renderFrame() {
		Graphics2D gbuf = (Graphics2D) this.imgBuf.getGraphics();
		clearBackground(gbuf);
		paintBalls(gbuf);
		if (!gameState.isPaused()) {
			paintReactors(gbuf);
		}
		paintMessages(gbuf);
	}

	private void paintMessages(Graphics2D g) {

		if (this.gameState.isPaused()) {
			Color currColor = g.getColor();
			Font currFont = g.getFont();
			g.setColor(Color.WHITE);
			Font f = new Font("Courier", Font.BOLD, 14);
			g.setFont(f);
			FontMetrics fm = g.getFontMetrics(f);
			g.drawString("<<Paused>>", getWidth() - 150, (getHeight() - 30)
					- (fm.getMaxAscent() + fm.getMaxDescent()));
			g.drawString("Press P again to resume", getWidth() - 200,
					(getHeight() - 30));
			g.setColor(currColor);
			g.setFont(currFont);
		} else {
			if (this.gameState.getBallsLeft() > 0) {
				Color currColr = g.getColor();
				g.setColor(Color.WHITE.brighter().brighter());
				g.drawString(
						String.format("Balls left = %d",
								this.gameState.getBallsLeft()), 10, 10);
				g.setColor(currColr);
			}
		}
	}

	private void paintReactors(Graphics2D g) {
		if (this.gameState.getReactors() != null) {
			for (Reactor nextReactor : this.gameState.getReactors()) {
				if (nextReactor.isAlive()) {
					nextReactor.paintNextFrame(g);
				}
			}
		}
	}

	/**
	 * @param g
	 */
	private void paintBalls(Graphics2D g) {
		Iterator<Ball> iter = this.gameState.getBalls().iterator();
		for (; iter.hasNext();) {
			Ball nextB = iter.next();
			nextB.paintBall(g);
		}
	}

}
