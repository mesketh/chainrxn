package au.com.schmick.game.cxn;

import java.awt.geom.Point2D;

public interface CollisionDetectable {

	Point2D getCentre();

	int getRadius();

	boolean isCollidingWith(CollisionDetectable otherObj);

}
