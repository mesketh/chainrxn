package au.com.schmick.game.cxn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Random;

import javax.swing.JComponent;

/**
 * The ball sprite which collide with other balls through {@link au.com.schmick.game.cxn.Reactor}
 *
 * @author mesketh of Schmick Software.
 * 
 */
public class Ball extends JComponent implements CollisionDetectable {

	private final static int MAX_WIDTH = 3, MAX_HEIGHT = 3;

	private volatile Color color;
	private int speedX = 2;
	private int speedY = 1;

	private volatile boolean isAlive = true;

	public Ball() {
		setColor(generateColor());
		setLocation((int) (Math.random() * 640), (int) (Math.random() * 480));
		setSpeedX((int)(Math.random() * 4)+1);
		setSpeedX(new Random().nextBoolean() ? getSpeedX() : -getSpeedX());
		setSpeedY((int)(Math.random() * 3)+1);
		setSpeedY(new Random().nextBoolean() ? getSpeedY() : -getSpeedY());
		setSize(getPreferredSize());
	}
	
	private Color generateColor() { 
		int r = (int)(Math.random()*220)+10;
		int g = (int)(Math.random()*220)+10;
		int b = (int)(Math.random()*220)+10;
		return new Color(r,g,b);
	}
	
	// Note: relies on the parent clearing the bg
	public void paintBall(Graphics2D g) {
		Color currColor = g.getColor();
		g.setColor(getColor());
		// draw the ball at the current position
		g.drawOval(getX() + getSpeedX(), getY() + getSpeedY(), getWidth(),
				getHeight());
		g.fillOval(getX() + getSpeedX(), getY() + getSpeedY(), getWidth(),
				getHeight());
		g.setColor(currColor);
	}

	public Color getColor() {
		return this.color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public int getSpeedX() {
		return speedX;
	}

	public void setSpeedX(int speedX) {
		this.speedX = speedX;
	}

	public int getSpeedY() {
		return speedY;
	}

	public void setSpeedY(int speedY) {
		this.speedY = speedY;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(MAX_WIDTH, MAX_HEIGHT);
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	/**
	 * Move the ball in the current direction - switching when hitting the
	 * bounds
	 * 
	 * @param bounds
	 *            The bounds for the ball to move in
	 */
	public void move(Rectangle bounds) {

		if (getSpeedX() >= 0) { // forwards
			if ((getLocation().x + getWidth() + getSpeedX()) >= bounds.width
					- MAX_WIDTH / 2) {
				setSpeedX(-getSpeedX()); // reverse direction
			} else {
				// move the ball
				setLocation(getLocation().x + getSpeedX(), getLocation().y
						+ getSpeedY());
			}
		} else {
			if ((getLocation().x + getSpeedX()) <= MAX_WIDTH / 2) {
				setSpeedX(-getSpeedX()); // reverse direction
			} else {
				// move the ball
				setLocation(getLocation().x + getSpeedX(), getLocation().y
						+ getSpeedY());
			}
		}

		if (getSpeedY() >= 0) { // downwards
			if ((getLocation().y + getHeight() + getSpeedY()) >= bounds.height
					- (MAX_HEIGHT / 2)) {
				setSpeedY(-getSpeedY()); // reverse direction
			} else {
				// move the ball
				setLocation(getLocation().x + getSpeedX(), getLocation().y
						+ getSpeedY());
			}
		} else {
			if ((getLocation().y + getSpeedY()) <= MAX_HEIGHT / 2) {
				setSpeedY(-getSpeedY()); // reverse direction
			} else {
				// move the ball
				setLocation(getLocation().x + getSpeedX(), getLocation().y
						+ getSpeedY());
			}
		}

	}

	@Override
	public boolean isCollidingWith(CollisionDetectable otherObj) {

		Point2D otherCentre = otherObj.getCentre();
		double distanceBetweenCentres = otherCentre.distance(getCentre());

		double totalRadii = getRadius() + otherObj.getRadius();

		return (distanceBetweenCentres < totalRadii);
	}

	@Override
	public Point2D getCentre() {
		return new Point(getX() + getWidth() / 2, getY() + getHeight() / 2);
	}

	@Override
	public int getRadius() {
		return getWidth();
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}

}
