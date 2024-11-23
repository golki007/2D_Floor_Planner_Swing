package app.gui;

import app.model.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * The canvas where rooms, doors, windows, and furniture are drawn.
 * Supports adding, displaying, and interacting with elements.
 */
public class CanvasPanel extends JPanel {
    private List<Room> rooms;
    private List<Furniture> furnitures;
    private Room roomToHighlight = null; // For highlighting selected room
    private Furniture selectedFurniture = null; // Currently selected furniture

    // Grid settings
    private final int GRID_SIZE = 20;
    private boolean showGrid = true;

    // Drag-and-Drop Variables
    private Room draggingRoom = null;
    private Furniture draggingFurniture = null;
    private Point dragOffset = null;
    private Point furnitureDragOffset = null;
    private Point originalPosition = null;

    // Action Modes
    public enum ActionMode { NONE, ADD_ROOM, ADD_RELATIVE_ROOM, ADD_DOOR, ADD_WINDOW, ADD_CUSTOM_FURNITURE }
    private ActionMode currentAction = ActionMode.NONE;

    // Selected Room Variable
    private Room selectedRoom = null;

    // Room Selection Listeners
    private List<RoomSelectionListener> roomSelectionListeners = new ArrayList<>();

    // Next Room Position for Row-Major Placement
    private int nextRoomX = 0; // Starting x position aligned to grid
    private int nextRoomY = 0; // Starting y position aligned to grid

    // Undo and Redo Stacks (Encapsulated)
    private Deque<Command> undoStack = new ArrayDeque<>();
    private Deque<Command> redoStack = new ArrayDeque<>();

    public CanvasPanel() {
        rooms = new ArrayList<>();
        furnitures = new ArrayList<>();

        setBackground(new Color(245, 245, 245)); // Softer background color
        setPreferredSize(new Dimension(1200, 800));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow(); // To capture key events
                Point p = snapToGrid(e.getPoint());
                // Check if clicking on furniture first
                for (Furniture furniture : furnitures) {
                    if (furniture.contains(p)) {
                        selectedRoom = furniture.getRoom();
                        selectedFurniture = furniture;
                        furnitureDragOffset = new Point(p.x - furniture.getX(), p.y - furniture.getY());
                        draggingFurniture = furniture;
                        notifyRoomSelected(selectedRoom);
                        originalPosition = furniture.getPosition();
                        return;
                    }
                }
                // Check if clicking on a room
                for (Room room : rooms) {
                    if (room.contains(p)) {
                        selectedRoom = room;
                        selectedFurniture = null;
                        draggingRoom = room;
                        dragOffset = new Point(p.x - room.getX(), p.y - room.getY());
                        notifyRoomSelected(room);
                        originalPosition = new Point(room.getX(), room.getY());
                        return;
                    }
                }
                // Clicked on empty space
                selectedRoom = null;
                selectedFurniture = null;
                notifyRoomSelected(null);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = snapToGrid(e.getPoint());
                if (draggingRoom != null) {
                    int newX = p.x - dragOffset.x;
                    int newY = p.y - dragOffset.y;
                    Point newPos = new Point(newX, newY);
                    // Temporarily set new position for overlap checking
                    Room oldRoom = draggingRoom;
                    draggingRoom.setPosition(newPos);
                    boolean overlap = checkRoomOverlap(draggingRoom);
                    if (overlap) {
                        roomToHighlight = draggingRoom;
                    } else {
                        roomToHighlight = null;
                    }
                    repaint();
                } else if (draggingFurniture != null) {
                    int newX = p.x - furnitureDragOffset.x;
                    int newY = p.y - furnitureDragOffset.y;
                    Point newPos = new Point(newX, newY);
                    draggingFurniture.setPosition(newPos);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingRoom != null) {
                    Point p = snapToGrid(e.getPoint());
                    int newX = p.x - dragOffset.x;
                    int newY = p.y - dragOffset.y;
                    Point newPos = new Point(newX, newY);
                    // Check for overlap before finalizing
                    draggingRoom.setPosition(newPos);
                    boolean overlap = checkRoomOverlap(draggingRoom);
                    if (overlap) {
                        showMessage("Rooms cannot overlap!", "Overlap Error", JOptionPane.ERROR_MESSAGE);
                        draggingRoom.setPosition(originalPosition);
                    } else {
                        // Record the move
                        pushUndo(new MoveRoomCommand(draggingRoom, originalPosition, newPos));
                        redoStack.clear();
                    }
                    roomToHighlight = null;
                    draggingRoom = null;
                    repaint();
                } else if (draggingFurniture != null) {
                    Point p = snapToGrid(e.getPoint());
                    int newX = p.x - furnitureDragOffset.x;
                    int newY = p.y - furnitureDragOffset.y;
                    Point newPos = new Point(newX, newY);
                    // Optionally, add overlap checks for furniture
                    pushUndo(new MoveFurnitureCommand(draggingFurniture, originalPosition, newPos));
                    redoStack.clear();
                    draggingFurniture = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click to rotate furniture
                    Point p = snapToGrid(e.getPoint());
                    for (Furniture furniture : furnitures) {
                        if (furniture.contains(p)) {
                            rotateFurniture(furniture, true); // Rotate clockwise
                            repaint();
                            break;
                        }
                    }
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // Key Bindings for Undo and Redo
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Z"), "undo");
        getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performUndo();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Y"), "redo");
        getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performRedo();
            }
        });
    }

    /**
     * Rotates the specified furniture.
     *
     * @param furniture The furniture to rotate.
     * @param clockwise True to rotate clockwise, false for counter-clockwise.
     */
    public void rotateFurniture(Furniture furniture, boolean clockwise) {
        Point oldPosition = furniture.getPosition();
        if (clockwise) {
            furniture.rotateClockwise();
        } else {
            furniture.rotateCounterClockwise();
        }
        Point newPosition = furniture.getPosition();
        pushUndo(new RotateFurnitureCommand(furniture, oldPosition, newPosition));
        redoStack.clear();
    }

    /**
     * Snaps a point to the nearest grid intersection.
     *
     * @param p The original point.
     * @return The snapped point.
     */
    private Point snapToGrid(Point p) {
        int x = (p.x / GRID_SIZE) * GRID_SIZE;
        int y = (p.y / GRID_SIZE) * GRID_SIZE;
        return new Point(x, y);
    }

    /**
     * Checks if the newRoom overlaps with any existing rooms.
     *
     * @param newRoom The room to check for overlaps.
     * @return True if there is an overlap, false otherwise.
     */
    private boolean checkRoomOverlap(Room newRoom) {
        for (Room room : rooms) {
            if (room != newRoom && room.intersects(newRoom)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if a wall is external based on its direction and room position.
     *
     * @param room      The room containing the wall.
     * @param direction The direction of the wall.
     * @return True if the wall is external, false otherwise.
     */
    private boolean isExternalWall(Room room, String direction) {
        switch(direction) {
            case "North":
                return room.getY() == 0;
            case "South":
                return (room.getY() + room.getHeight()) == getHeight();
            case "East":
                return (room.getX() + room.getWidth()) == getWidth();
            case "West":
                return room.getX() == 0;
            default:
                return false;
        }
    }

    /**
     * Checks if two PlanItems are intersecting.
     *
     * @param item1 The first PlanItem.
     * @param item2 The second PlanItem.
     * @return True if they intersect, false otherwise.
     */
    public boolean areItemsIntersecting(PlanItem item1, PlanItem item2) {
        return item1.intersects(item2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Draw grid if enabled
        if (showGrid) {
            g2.setColor(new Color(220, 220, 220)); // Light gray grid lines
            for (int x = 0; x < getWidth(); x += GRID_SIZE) {
                g2.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += GRID_SIZE) {
                g2.drawLine(0, y, getWidth(), y);
            }
        }

        // Draw all rooms
        for (Room room : rooms) {
            // Fill room color
            g2.setColor(room.getColor());
            g2.fillRect(room.getX(), room.getY(), room.getWidth(), room.getHeight());

            // Draw room border
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(room.getX(), room.getY(), room.getWidth(), room.getHeight());

            // Draw room name if available
            if (room.getName() != null) {
                Font originalFont = g2.getFont();
                Font smallFont = originalFont.deriveFont(Font.BOLD, 14f);
                g2.setFont(smallFont);
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(room.getName());
                g2.drawString(room.getName(), room.getX() + (room.getWidth() - textWidth) / 2, room.getY() + 20);
                g2.setFont(originalFont);
            }

            // Draw walls with doors and windows
            drawRoomWalls(g2, room);

            // Draw furniture
            for (Furniture furniture : room.getFurnitures()) {
                if (furniture.getImage() != null) {
                    Graphics2D backup = (Graphics2D) g2.create();
                    int centerX = furniture.getX() + furniture.getWidth() / 2;
                    int centerY = furniture.getY() + furniture.getHeight() / 2;
                    backup.rotate(Math.toRadians(furniture.getAngle()), centerX, centerY);
                    backup.drawImage(furniture.getImage(), furniture.getX(), furniture.getY(), furniture.getWidth(), furniture.getHeight(), this);
                    backup.dispose();
                } else {
                    g2.setColor(Color.MAGENTA);
                    g2.fillRect(furniture.getX(), furniture.getY(), furniture.getWidth(), furniture.getHeight());

                    // Draw border only for non-image furnitures
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRect(furniture.getX(), furniture.getY(), furniture.getWidth(), furniture.getHeight());
                }
            }
        }

        // Draw highlight if any
        drawHighlight(g2, roomToHighlight);
        g2.dispose();
    }

    /**
     * Draws the walls of a room, incorporating doors and windows.
     *
     * @param g2   The Graphics2D object.
     * @param room The room whose walls are to be drawn.
     */
    private void drawRoomWalls(Graphics2D g2, Room room) {
        // Define walls as lines
        int x = room.getX();
        int y = room.getY();
        int w = room.getWidth();
        int h = room.getHeight();

        // Set basic wall properties
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(3));

        // North Wall
        drawWallWithItems(g2, room, "North", x, y, x + w, y);

        // South Wall
        drawWallWithItems(g2, room, "South", x, y + h, x + w, y + h);

        // East Wall
        drawWallWithItems(g2, room, "East", x + w, y, x + w, y + h);

        // West Wall
        drawWallWithItems(g2, room, "West", x, y, x, y + h);
    }

    /**
     * Draws a single wall with its doors and windows.
     *
     * @param g2        The Graphics2D object.
     * @param room      The room containing the wall.
     * @param direction The direction of the wall.
     * @param x1        Starting x-coordinate.
     * @param y1        Starting y-coordinate.
     * @param x2        Ending x-coordinate.
     * @param y2        Ending y-coordinate.
     */
    private void drawWallWithItems(Graphics2D g2, Room room, String direction, int x1, int y1, int x2, int y2) {
        List<PlanItem> itemsOnWall = new ArrayList<>();

        // Collect doors and windows on this wall
        for (Door door : room.getDoors()) {
            if (door.getDirection().equals(direction)) {
                itemsOnWall.add(door);
            }
        }
        for (PlanWindow window : room.getWindows()) {
            if (window.getDirection().equals(direction)) {
                itemsOnWall.add(window);
            }
        }

        // Sort items based on their position along the wall
        itemsOnWall.sort(Comparator.comparingInt(item -> {
            if (direction.equals("North") || direction.equals("South")) {
                return item.getX();
            } else {
                return item.getY();
            }
        }));

        // Draw the wall segments between items
        int startX = x1;
        int startY = y1;
        for (PlanItem item : itemsOnWall) {
            if (direction.equals("North") || direction.equals("South")) {
                g2.drawLine(startX, startY, item.getX(), startY);
                if (item instanceof Door) {
                    // Draw door as a gap
                    startX = item.getX() + ((Door) item).getWidth();
                } else if (item instanceof PlanWindow) {
                    // Draw window as a gap and represent it with a dashed line
                    PlanWindow window = (PlanWindow) item;
                    startX = window.getX() + window.getWidth();

                    // Draw the window representation
                    Stroke originalStroke = g2.getStroke();
                    float[] dashPattern = {5, 5};
                    Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0);
                    g2.setStroke(dashed);
                    g2.setColor(Color.WHITE); // Color for windows
                    g2.drawLine(window.getX(), window.getY(), window.getX() + window.getWidth(), window.getY());
                    g2.setStroke(originalStroke);
                    g2.setColor(Color.BLACK); // Reset color
                }
            } else { // East or West
                g2.drawLine(startX, startY, startX, item.getY());
                if (item instanceof Door) {
                    // Draw door as a gap
                    startY = item.getY() + ((Door) item).getHeight();
                } else if (item instanceof PlanWindow) {
                    // Draw window as a gap and represent it with a dashed line
                    PlanWindow window = (PlanWindow) item;
                    startY = window.getY() + window.getHeight();

                    // Draw the window representation
                    Stroke originalStroke = g2.getStroke();
                    float[] dashPattern = {5, 5};
                    Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0);
                    g2.setStroke(dashed);
                    g2.setColor(Color.WHITE); // Color for windows
                    g2.drawLine(window.getX(), window.getY(), window.getX(), window.getY() + window.getHeight());
                    g2.setStroke(originalStroke);
                    g2.setColor(Color.BLACK); // Reset color
                }
            }
        }

        // Draw the remaining wall
        if (direction.equals("North") || direction.equals("South")) {
            g2.drawLine(startX, startY, x2, y2);
        } else {
            g2.drawLine(startX, startY, x2, y2);
        }
    }

    /**
     * Draws the highlight around a room.
     *
     * @param g    The Graphics object.
     * @param room The room to highlight.
     */
    private void drawHighlight(Graphics g, Room room) {
        if (room != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.MAGENTA);
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0));
            g2.drawRect(room.getX() - 5, room.getY() - 5, room.getWidth() + 10, room.getHeight() + 10);
        }
    }

    // Additional getters for rooms and furnitures
    public List<Room> getRooms() {
        return rooms;
    }

    public List<Furniture> getFurnitures() {
        return furnitures;
    }

    /**
     * Removes any room highlight.
     */
    public void removeHighlight() {
        this.roomToHighlight = null;
        repaint();
    }

    // Methods to set and reset action mode
    public void setCurrentAction(ActionMode mode) {
        currentAction = mode;
    }

    public void resetActionMode() {
        currentAction = ActionMode.NONE;
    }

    /**
     * Adds a room selection listener.
     *
     * @param listener The listener to add.
     */
    public void addRoomSelectionListener(RoomSelectionListener listener) {
        roomSelectionListeners.add(listener);
    }

    /**
     * Notifies all listeners about room or furniture selection.
     *
     * @param room The selected Room, or null.
     */
    private void notifyRoomSelected(Room room) {
        for (RoomSelectionListener listener : roomSelectionListeners) {
            listener.onRoomSelected(room);
        }
    }

    /**
     * Shows a message dialog.
     *
     * @param message The message to display.
     * @param title   The title of the dialog.
     * @param type    The message type.
     */
    private void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    /**
     * Adds a room to the canvas.
     *
     * @param type  The type of the room.
     * @param color The color of the room.
     * @param width The width of the room.
     * @param height The height of the room.
     * @param name The optional name of the room.
     */
    public void addRoom(String type, Color color, int width, int height, String name) {
        // Position based on nextRoomX and nextRoomY
        Room newRoom = new Room(nextRoomX, nextRoomY, width, height, type, color, name);
        // Check for overlap
        if (checkRoomOverlap(newRoom)) {
            showMessage("Cannot place room here. It overlaps with an existing room.", "Placement Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        rooms.add(newRoom);
        pushUndo(new AddRoomCommand(newRoom));
        redoStack.clear();
        // Update next position (row-major order)
        nextRoomX += width; // Removed GRID_SIZE for spacing between rooms
        if (nextRoomX + width > getWidth()) {
            nextRoomX = 0;
            nextRoomY += height; // Removed GRID_SIZE for spacing between rooms
        }
        repaint();
    }

    /**
     * Adds a relative room to an existing room with specified dimensions, alignment, and optional name.
     *
     * @param baseRoom  The base room to which the new room is relative.
     * @param type      The type of the new room.
     * @param direction The direction relative to the base room.
     * @param alignment The alignment relative to the wall (Left, Center, Right).
     * @param color     The color representing the room type.
     * @param width     The width of the new room.
     * @param height    The height of the new room.
     * @param name      The optional name of the new room.
     */
    public void addRelativeRoom(Room baseRoom, String type, String direction, String alignment, Color color, int width, int height, String name) {
        int newX = baseRoom.getX();
        int newY = baseRoom.getY();

        switch (direction) {
            case "North":
                newY = baseRoom.getY() - height; // Removed GRID_SIZE for spacing
                break;
            case "South":
                newY = baseRoom.getY() + baseRoom.getHeight(); // Removed GRID_SIZE for spacing
                break;
            case "East":
                newX = baseRoom.getX() + baseRoom.getWidth(); // Removed GRID_SIZE for spacing
                break;
            case "West":
                newX = baseRoom.getX() - width; // Removed GRID_SIZE for spacing
                break;
            default:
                showMessage("Invalid direction for relative room.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
        }

        // Adjust alignment
        switch (alignment) {
            case "Left":
                if (direction.equals("North") || direction.equals("South")) {
                    newX = baseRoom.getX();
                } else {
                    newY = baseRoom.getY();
                }
                break;
            case "Center":
                if (direction.equals("North") || direction.equals("South")) {
                    newX = baseRoom.getX() + (baseRoom.getWidth() - width) / 2;
                } else {
                    newY = baseRoom.getY() + (baseRoom.getHeight() - height) / 2;
                }
                break;
            case "Right":
                if (direction.equals("North") || direction.equals("South")) {
                    newX = baseRoom.getX() + baseRoom.getWidth() - width;
                } else {
                    newY = baseRoom.getY() + baseRoom.getHeight() - height;
                }
                break;
            default:
                showMessage("Invalid alignment option.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
        }

        Room newRoom = new Room(newX, newY, width, height, type, color, name);
        // Check for overlap
        if (checkRoomOverlap(newRoom)) {
            showMessage("Cannot place room here. It overlaps with an existing room.", "Placement Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        rooms.add(newRoom);
        pushUndo(new AddRelativeRoomCommand(newRoom));
        redoStack.clear();
        repaint();
    }

    /**
     * Adds a door to a specified room.
     *
     * @param room      The room to which the door will be added.
     * @param direction The direction ("North", "South", "East", "West") where the door will be placed.
     */
    public void addDoorToRoom(Room room, String direction) {
        if (room == null) {
            showMessage("No room selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prevent Bedroom/Bathroom doors from leading outside
        if (room.getType().equalsIgnoreCase("Bedroom") || room.getType().equalsIgnoreCase("Bathroom")) {
            if (isExternalWall(room, direction)) {
                showMessage("ERROR: Bedroom/Bathroom doors cannot lead directly outside.", "Placement Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Prevent doors on the same wall overlapping with existing items
        // Prompt for door length
        String lengthStr = JOptionPane.showInputDialog(this, "Enter Door Length (pixels):", "Door Dimensions", JOptionPane.PLAIN_MESSAGE);
        if (lengthStr == null) return; // User canceled
        int doorLength;
        try {
            doorLength = Integer.parseInt(lengthStr);
            if (doorLength <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showMessage("Invalid door length entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Determine door dimensions based on direction
        int doorWidth, doorHeight;
        if (direction.equals("North") || direction.equals("South")) {
            doorWidth = doorLength;
            doorHeight = 20; // Fixed height for horizontal doors
        } else { // East or West
            doorWidth = 20; // Fixed width for vertical doors
            doorHeight = doorLength;
        }

        // Check if the wall is shared with another room
        boolean isWallShared = isWallShared(room, direction);

        if (room.getType().equalsIgnoreCase("Bedroom") || room.getType().equalsIgnoreCase("Bathroom")) {
            if (!isWallShared) {
                showMessage("ERROR: Bedroom/Bathroom doors must be placed on walls connecting to another room.", "Placement Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Determine door position on the specified wall
        Door newDoor = new Door(doorWidth, doorHeight, direction);

        // Positioning door on the specified wall
        switch (direction) {
            case "North":
                newDoor.setPosition(new Point(
                        room.getX() + (room.getWidth() - newDoor.getWidth()) / 2,
                        room.getY()
                ));
                break;
            case "South":
                newDoor.setPosition(new Point(
                        room.getX() + (room.getWidth() - newDoor.getWidth()) / 2,
                        room.getY() + room.getHeight()
                ));
                break;
            case "East":
                newDoor.setPosition(new Point(
                        room.getX() + room.getWidth(),
                        room.getY() + (room.getHeight() - newDoor.getHeight()) / 2
                ));
                break;
            case "West":
                newDoor.setPosition(new Point(
                        room.getX() - newDoor.getWidth(),
                        room.getY() + (room.getHeight() - newDoor.getHeight()) / 2
                ));
                break;
            default:
                showMessage("Invalid direction for door.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
        }

        // Snap door position to grid
        newDoor.setPosition(snapToGrid(newDoor.getPosition()));

        // Check if door is within wall bounds
        if (!isWithinWallBounds(room, newDoor)) {
            showMessage("Door positioning is out of wall bounds!", "Placement Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for overlapping with existing doors and windows
        if (isOverlappingDoorOrWindow(room, newDoor)) {
            showMessage("Cannot place door here. It overlaps with an existing door or window!", "Overlap Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        room.addDoor(newDoor);
        repaint();

        // Push to undo stack
        pushUndo(new AddDoorCommand(room, newDoor));
        redoStack.clear();
    }

    /**
     * Adds a window to a specified room.
     *
     * @param room      The room to which the window will be added.
     * @param direction The direction ("North", "South", "East", "West") where the window will be placed.
     */
    public void addWindowToRoom(Room room, String direction) {
        if (room == null) {
            showMessage("No room selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prevent windows on the same wall overlapping with existing doors
        boolean hasDoorOnWall = false;
        for (Door door : room.getDoors()) {
            if (door.getDirection().equals(direction)) {
                hasDoorOnWall = true;
                break;
            }
        }
        if (hasDoorOnWall) {
            showMessage("Cannot place window on a wall that has a door!", "Placement Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if the wall is shared with another room
        if (isWallShared(room, direction)) {
            showMessage("Cannot place window on a wall shared with another room.", "Placement Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prompt for window length
        String lengthStr = JOptionPane.showInputDialog(this, "Enter Window Length (pixels):", "Window Dimensions", JOptionPane.PLAIN_MESSAGE);
        if (lengthStr == null) return; // User canceled
        int windowLength;
        try {
            windowLength = Integer.parseInt(lengthStr);
            if (windowLength <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showMessage("Invalid window length entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PlanWindow newWindow = new PlanWindow(
                direction.equals("North") || direction.equals("South") ? windowLength : 10,
                direction.equals("North") || direction.equals("South") ? 10 : windowLength,
                direction
        );

        // Positioning window on the specified wall, centered
        switch (direction) {
            case "North":
                newWindow.setPosition(new Point(
                        room.getX() + (room.getWidth() - newWindow.getWidth()) / 2,
                        room.getY()
                ));
                break;
            case "South":
                newWindow.setPosition(new Point(
                        room.getX() + (room.getWidth() - newWindow.getWidth()) / 2,
                        room.getY() + room.getHeight()
                ));
                break;
            case "East":
                newWindow.setPosition(new Point(
                        room.getX() + room.getWidth(),
                        room.getY() + (room.getHeight() - newWindow.getHeight()) / 2
                ));
                break;
            case "West":
                newWindow.setPosition(new Point(
                        room.getX() - newWindow.getWidth(),
                        room.getY() + (room.getHeight() - newWindow.getHeight()) / 2
                ));
                break;
            default:
                showMessage("Invalid direction for window.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
        }

        // Snap window position to grid
        newWindow.setPosition(snapToGrid(newWindow.getPosition()));

        // Check if window is within wall bounds
        if (!isWithinWallBounds(room, newWindow)) {
            showMessage("Window positioning is out of wall bounds!", "Placement Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for overlap with existing windows and doors
        if (isOverlappingDoorOrWindow(room, newWindow)) {
            showMessage("Cannot place window here. Overlaps with existing window or door!", "Overlap Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        room.addWindow(newWindow);
        repaint();

        // Push to undo stack
        pushUndo(new AddWindowCommand(room, newWindow));
        redoStack.clear();
    }

    /**
     * Checks if a door/window overlaps with existing doors or windows on the same wall.
     *
     * @param room The room where the door/window is being added.
     * @param item The door/window being added.
     * @return True if there is an overlap, false otherwise.
     */
    private boolean isOverlappingDoorOrWindow(Room room, PlanItem item) {
        List<PlanItem> existingItems = new ArrayList<>();
        if (item instanceof Door) {
            existingItems.addAll(room.getDoors());
            existingItems.addAll(room.getWindows());
        } else if (item instanceof PlanWindow) {
            existingItems.addAll(room.getWindows());
            existingItems.addAll(room.getDoors());
        }

        for (PlanItem existingItem : existingItems) {
            if (existingItem != item && existingItem.intersects(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the item is within the bounds of the room's wall.
     *
     * @param room The room containing the wall.
     * @param item The item to check.
     * @return True if within bounds, false otherwise.
     */
    private boolean isWithinWallBounds(Room room, PlanItem item) {
        Rectangle roomBounds = room.getBounds();
        Rectangle itemBounds = item.getBounds();

        String direction = "";
        if (item instanceof Door) {
            direction = ((Door)item).getDirection();
        } else if (item instanceof PlanWindow) {
            direction = ((PlanWindow)item).getDirection();
        }

        switch(direction) {
            case "North":
            case "South":
                return roomBounds.x <= itemBounds.x && (itemBounds.x + item.getWidth()) <= (roomBounds.x + roomBounds.width);
            case "East":
            case "West":
                return roomBounds.y <= itemBounds.y && (itemBounds.y + item.getHeight()) <= (roomBounds.y + roomBounds.height);
            default:
                return false;
        }
    }

    /**
     * Pushes a command onto the undo stack.
     *
     * @param command The command to push.
     */
    public void pushUndo(Command command) {
        undoStack.push(command);
    }

    /**
     * Pushes a command onto the redo stack.
     *
     * @param command The command to push.
     */
    public void pushRedo(Command command) {
        redoStack.push(command);
    }

    /**
     * Clears the redo stack.
     */
    public void clearRedo() {
        redoStack.clear();
    }

    /**
     * Performs an undo operation.
     */
    public void performUndo() {
        if (!undoStack.isEmpty()) {
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
            repaint();
        }
    }

    /**
     * Performs a redo operation.
     */
    public void performRedo() {
        if (!redoStack.isEmpty()) {
            Command cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
            repaint();
        }
    }

    /**
     * Checks if a wall is shared with another room.
     *
     * @param room The room to check.
     * @param direction The direction of the wall.
     * @return True if the wall is shared with another room, false otherwise.
     */
    private boolean isWallShared(Room room, String direction) {
        for (Room otherRoom : rooms) {
            if (otherRoom == room) continue;
            switch (direction) {
                case "North":
                    if (otherRoom.getY() + otherRoom.getHeight() == room.getY() &&
                        roomsOverlap(room.getX(), room.getX() + room.getWidth(), otherRoom.getX(), otherRoom.getX() + otherRoom.getWidth())) {
                        return true;
                    }
                    break;
                case "South":
                    if (otherRoom.getY() == room.getY() + room.getHeight() &&
                        roomsOverlap(room.getX(), room.getX() + room.getWidth(), otherRoom.getX(), otherRoom.getX() + otherRoom.getWidth())) {
                        return true;
                    }
                    break;
                case "East":
                    if (otherRoom.getX() == room.getX() + room.getWidth() &&
                        roomsOverlap(room.getY(), room.getY() + room.getHeight(), otherRoom.getY(), otherRoom.getY() + otherRoom.getHeight())) {
                        return true;
                    }
                    break;
                case "West":
                    if (otherRoom.getX() + otherRoom.getWidth() == room.getX() &&
                        roomsOverlap(room.getY(), room.getY() + room.getHeight(), otherRoom.getY(), otherRoom.getY() + otherRoom.getHeight())) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    /**
     * Helper method to determine if two ranges overlap.
     *
     * @param start1 Start of first range.
     * @param end1 End of first range.
     * @param start2 Start of second range.
     * @param end2 End of second range.
     * @return True if ranges overlap, false otherwise.
     */
    private boolean roomsOverlap(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }

    /**
     * Interface for undoable actions.
     */
    public interface Command {
        void execute();
        void undo();
    }

    /**
     * Command to add a room.
     */
    public class AddRoomCommand implements Command {
        private Room room;

        public AddRoomCommand(Room room) {
            this.room = room;
        }

        @Override
        public void execute() {
            rooms.add(room);
        }

        @Override
        public void undo() {
            rooms.remove(room);
            repaint();
        }
    }

    /**
     * Command to add a relative room.
     */
    public class AddRelativeRoomCommand implements Command {
        private Room room;

        public AddRelativeRoomCommand(Room room) {
            this.room = room;
        }

        @Override
        public void execute() {
            rooms.add(room);
        }

        @Override
        public void undo() {
            rooms.remove(room);
            repaint();
        }
    }

    /**
     * Command to delete a room.
     */
    public class DeleteRoomCommand implements Command {
        private Room room;
        private List<Door> doorsBackup;
        private List<PlanWindow> windowsBackup;
        private List<Furniture> furnituresBackup;

        public DeleteRoomCommand(Room room) {
            this.room = room;
            this.doorsBackup = new ArrayList<>(room.getDoors());
            this.windowsBackup = new ArrayList<>(room.getWindows());
            this.furnituresBackup = new ArrayList<>(room.getFurnitures());
        }

        @Override
        public void execute() {
            // Remove furnitures from global list
            for (Furniture furniture : furnituresBackup) {
                furnitures.remove(furniture);
            }

            // Remove room
            rooms.remove(room);
            repaint();
        }

        @Override
        public void undo() {
            // Add room back
            rooms.add(room);

            // Restore doors, windows, and furnitures
            for (Door door : doorsBackup) {
                room.addDoor(door);
            }
            for (PlanWindow window : windowsBackup) {
                room.addWindow(window);
            }
            for (Furniture furniture : furnituresBackup) {
                room.addFurniture(furniture);
                furnitures.add(furniture);
            }
            repaint();
        }
    }

    /**
     * Command to move a room.
     */
    public class MoveRoomCommand implements Command {
        private Room room;
        private Point oldPosition;
        private Point newPosition;

        public MoveRoomCommand(Room room, Point oldPosition, Point newPosition) {
            this.room = room;
            this.oldPosition = oldPosition;
            this.newPosition = newPosition;
        }

        @Override
        public void execute() {
            room.setPosition(newPosition);
        }

        @Override
        public void undo() {
            room.setPosition(oldPosition);
        }
    }

    /**
     * Command to add a door.
     */
    public class AddDoorCommand implements Command {
        private Room room;
        private Door door;

        public AddDoorCommand(Room room, Door door) {
            this.room = room;
            this.door = door;
        }

        @Override
        public void execute() {
            room.addDoor(door);
        }

        @Override
        public void undo() {
            room.getDoors().remove(door);
            repaint();
        }
    }

    /**
     * Command to add a window.
     */
    public class AddWindowCommand implements Command {
        private Room room;
        private PlanWindow window;

        public AddWindowCommand(Room room, PlanWindow window) {
            this.room = room;
            this.window = window;
        }

        @Override
        public void execute() {
            room.addWindow(window);
        }

        @Override
        public void undo() {
            room.getWindows().remove(window);
            repaint();
        }
    }

    /**
     * Command to add furniture.
     */
    public class AddFurnitureCommand implements Command {
        private Room room;
        private Furniture furniture;

        public AddFurnitureCommand(Room room, Furniture furniture) {
            this.room = room;
            this.furniture = furniture;
        }

        @Override
        public void execute() {
            room.addFurniture(furniture);
            furnitures.add(furniture);
        }

        @Override
        public void undo() {
            room.getFurnitures().remove(furniture);
            furnitures.remove(furniture);
            repaint();
        }
    }

    /**
     * Command to move furniture.
     */
    public class MoveFurnitureCommand implements Command {
        private Furniture furniture;
        private Point oldPosition;
        private Point newPosition;

        public MoveFurnitureCommand(Furniture furniture, Point oldPosition, Point newPosition) {
            this.furniture = furniture;
            this.oldPosition = oldPosition;
            this.newPosition = newPosition;
        }

        @Override
        public void execute() {
            furniture.setPosition(newPosition);
        }

        @Override
        public void undo() {
            furniture.setPosition(oldPosition);
        }
    }

    /**
     * Command to rotate furniture.
     */
    public class RotateFurnitureCommand implements Command {
        private Furniture furniture;
        private Point oldPosition;
        private Point newPosition;

        public RotateFurnitureCommand(Furniture furniture, Point oldPosition, Point newPosition) {
            this.furniture = furniture;
            this.oldPosition = oldPosition;
            this.newPosition = newPosition;
        }

        @Override
        public void execute() {
            // Rotation is already handled before pushing the command
        }

        @Override
        public void undo() {
            furniture.setPosition(oldPosition);
            repaint();
        }
    }

    /**
     * Adds furniture to a room.
     *
     * @param room      The room to which the furniture will be added.
     * @param furniture The furniture to add.
     */
    public void addFurnitureToRoom(Room room, Furniture furniture) {
        room.addFurniture(furniture);
        furnitures.add(furniture);
        repaint();
        pushUndo(new AddFurnitureCommand(room, furniture));
        redoStack.clear();
    }

    /**
     * Adds a relative room and furniture placements as needed.
     */
    // Additional methods can be added here as needed.
}