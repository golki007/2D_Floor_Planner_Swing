package app.model;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;

/**
 * Represents a window in the floor plan.
 */
public class PlanWindow implements PlanItem, Serializable {
    private static final long serialVersionUID = 1L;
    private int x, y, width, height;
    private String direction; // North, South, East, West
    private transient Room room; // Transient to avoid circular serialization

    public PlanWindow(int width, int height, String direction) {
        this.width = width;
        this.height = height;
        this.direction = direction;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public boolean intersects(PlanItem other) {
        return this.getBounds().intersects(other.getBounds());
    }

    @Override
    public boolean contains(Point p) {
        return this.getBounds().contains(p);
    }

    @Override
    public void setPosition(Point newPosition) {
        this.x = newPosition.x;
        this.y = newPosition.y;
    }

    @Override
    public Point getPosition() {
        return new Point(x, y);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public String getDirection() {
        return direction;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }

    /**
     * Updates the window's position relative to the room's new position.
     *
     * @param room The room to which the window belongs.
     */
    public void updatePositionRelativeToRoom(Room room) {
        this.room = room;
        switch (direction) {
            case "North":
                setPosition(new Point(
                        room.getX() + (room.getWidth() - width) / 2,
                        room.getY()
                ));
                break;
            case "South":
                setPosition(new Point(
                        room.getX() + (room.getWidth() - width) / 2,
                        room.getY() + room.getHeight()
                ));
                break;
            case "East":
                setPosition(new Point(
                        room.getX() + room.getWidth(),
                        room.getY() + (room.getHeight() - height) / 2
                ));
                break;
            case "West":
                // Adjusted to position the window inside the room's western boundary
                setPosition(new Point(
                        room.getX(),
                        room.getY() + (room.getHeight() - height) / 2
                ));
                break;
            default:
                break;
        }
    }
}