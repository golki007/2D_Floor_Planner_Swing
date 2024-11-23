package app.model;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a room in the floor plan.
 */
public class Room implements PlanItem, Serializable {
    private static final long serialVersionUID = 1L;
    private int x, y, width, height;
    private String type;
    private String name; // New field for room name
    private Color color;

    private List<Door> doors;
    private List<PlanWindow> windows;
    private List<Furniture> furnitures;

    public Room(int x, int y, int width, int height, String type, Color color) {
        this(x, y, width, height, type, color, null); // Default name as null
    }

    public Room(int x, int y, int width, int height, String type, Color color, String name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.color = color;
        this.name = name;
        this.doors = new ArrayList<>();
        this.windows = new ArrayList<>();
        this.furnitures = new ArrayList<>();
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

        // Update positions of doors and windows relative to the room
        for (Door door : doors) {
            door.updatePositionRelativeToRoom(this);
        }
        for (PlanWindow window : windows) {
            window.updatePositionRelativeToRoom(this);
        }

        // Optionally, update furnitures' positions if needed
        for (Furniture furniture : furnitures) {
            // You can implement furniture position updates here if desired
        }
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

    // Additional getters and setters
    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getColor() {
        return color;
    }

    public List<Door> getDoors() {
        return doors;
    }

    public List<PlanWindow> getWindows() {
        return windows;
    }

    public List<Furniture> getFurnitures() {
        return furnitures;
    }

    // Methods to add doors, windows, and furniture
    public void addDoor(Door door) {
        doors.add(door);
        door.setRoom(this);
    }

    public void addWindow(PlanWindow window) {
        windows.add(window);
        window.setRoom(this);
    }

    public void addFurniture(Furniture furniture) {
        furnitures.add(furniture);
        furniture.setRoom(this);
    }
}