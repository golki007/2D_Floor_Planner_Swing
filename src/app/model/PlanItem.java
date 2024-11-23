package app.model;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface representing an item on the floor plan.
 */
public interface PlanItem {
    Rectangle getBounds();
    boolean intersects(PlanItem other);
    boolean contains(Point p);
    void setPosition(Point newPosition);
    Point getPosition();
    int getX();
    int getY();
    int getWidth();
    int getHeight();
}