package app.model;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import javax.swing.ImageIcon;

/**
 * Represents a furniture item in the floor plan.
 */
public class Furniture implements PlanItem, Serializable {
    private static final long serialVersionUID = 1L;
    private int x, y, width, height;
    private String type;
    private transient Room room; // Transient to avoid circular serialization
    private transient Image image; // Transient as images are handled via imagePath
    private String imagePath; // Path to the image file
    private int angle; // Rotation angle in degrees (0, 90, 180, 270)

    public Furniture(int width, int height, String type, Room room) {
        this.width = width;
        this.height = height;
        this.type = type;
        this.room = room;
        this.angle = 0; // Default rotation
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

    public String getType() {
        return type;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Image getImage() {
        return image;
    }

    /**
     * Sets the image and its path.
     *
     * @param image     The image to set.
     * @param imagePath The path to the image file.
     */
    public void setImage(Image image, String imagePath) {
        this.image = image;
        this.imagePath = imagePath;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Gets the rotation angle of the furniture.
     *
     * @return The rotation angle in degrees.
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Sets the rotation angle of the furniture.
     *
     * @param angle The rotation angle in degrees (should be multiple of 90).
     */
    public void setAngle(int angle) {
        this.angle = (angle % 360 + 360) % 360; // Normalize to [0, 360)
    }

    /**
     * Rotates the furniture by 90 degrees clockwise.
     */
    public void rotateClockwise() {
        setAngle(this.angle + 90);
    }

    /**
     * Rotates the furniture by 90 degrees counter-clockwise.
     */
    public void rotateCounterClockwise() {
        setAngle(this.angle - 90);
    }

    /**
     * Custom deserialization to handle transient fields.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (imagePath != null) {
            ImageIcon icon = new ImageIcon(imagePath);
            this.image = icon.getImage();
        }
    }
}