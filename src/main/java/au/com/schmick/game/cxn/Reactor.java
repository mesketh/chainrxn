package au.com.schmick.game.cxn;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.math.BigDecimal;

import javax.swing.JComponent;

public class Reactor extends JComponent implements CollisionDetectable {

	private static final long serialVersionUID = -4671969855350238598L;

	private final static int MAX_RADIUS = 30;
	private final static int INFLATED_FRAMES_LIMIT = 16;
	private Font SCORE_FONT = new Font("Courier", Font.PLAIN, 9);

	private final static AlphaComposite alphaComposite = AlphaComposite
			.getInstance(AlphaComposite.SRC_OVER, 0.25f);
	private int inflatedFramesCounter;
	private int currentRadius = 1;
	private Point centre;
	private Color theColor;
	private boolean finishedAnimating = false;
	private boolean inflating = true;

	private int score;

	public Reactor(Point2D centre, int score) {
		this(BigDecimal.valueOf(centre.getX()).intValue(), BigDecimal.valueOf(
				centre.getY()).intValue());
		this.theColor = this.theColor.brighter();
		this.score = score;
	}

	public Reactor(int x, int y) {
		this.centre = new Point(x, y);
		this.theColor = generateColor();
	}

	private Color generateColor() {
		int r = (int) (Math.random() * 220) + 10;
		int g = (int) (Math.random() * 220) + 10;
		int b = (int) (Math.random() * 220) + 10;
		return new Color(r, g, b);
	}

	protected boolean isAlive() {
		return !this.finishedAnimating;
	}

	/**
	 * Render a frame to the given buffer. Paints a circle in a particular
	 * colour and size.
	 * 
	 * @param g
	 */
	public void paintNextFrame(Graphics2D g) {

		if (!finishedAnimating) {
			Color currColor = g.getColor();
			Composite currComposite = g.getComposite();
			g.setColor(theColor);
			g.setComposite(this.alphaComposite);
			g.drawOval(centre.x - this.currentRadius, centre.y
					- this.currentRadius, this.currentRadius * 2,
					this.currentRadius * 2);
			g.fillOval(centre.x - this.currentRadius, centre.y
					- this.currentRadius, this.currentRadius * 2,
					this.currentRadius * 2);

			if (inflating && currentRadius <= MAX_RADIUS) {
				this.currentRadius += 2;
				if (this.score > 0) {
					drawScore(g);
				}
			} else {
				if (inflating
						&& this.inflatedFramesCounter++ <= INFLATED_FRAMES_LIMIT) {
					if (this.score > 0) {
						drawScore(g);
					}
				} else if (inflating) {
					inflating = !inflating;
				} else {
					this.currentRadius -= 2;
					if (currentRadius <= 2)
						this.finishedAnimating = true;
				}
			}
			g.setComposite(currComposite);
			g.setColor(currColor);
		}

	}

	private void drawScore(Graphics2D g) {
		Color currColor = g.getColor();
		Font currFont = g.getFont();
		g.setColor(Color.PINK);
		g.setFont(SCORE_FONT);
		g.drawString("+" + this.score, centre.x, centre.y);
		g.setFont(currFont);
		g.setColor(currColor);
	}

	public Point2D getCentre() {
		return centre;
	}

	public boolean isCollidingWith(CollisionDetectable otherObj) {

		Point2D otherCentre = otherObj.getCentre();
		double distanceBetweenCentres = otherCentre.distance(getCentre());

		double totalRadii = getRadius() + otherObj.getRadius();

		return (distanceBetweenCentres < totalRadii);
	}

	public int getRadius() {
		return this.currentRadius;
	}

	public int getScore() {
		return score;
	}

}
