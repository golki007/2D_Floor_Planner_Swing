package app.gui;

import app.model.*;
import app.util.PlanData;
import app.util.PlanSerializer;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

/**
 * Control panel for managing the floor plan.
 */
public class ControlPanel extends JPanel implements RoomSelectionListener {
    private CanvasPanel canvas;
    private Room selectedRoom = null;
    private File currentFile;

    // UI Components that might need to be enabled/disabled based on selection
    private JButton deleteRoomButton;

    public ControlPanel(CanvasPanel canvas) {
        this.canvas = canvas;
        canvas.addRoomSelectionListener(this); // Correctly adding as a listener

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(250, 800));
        setBackground(new Color(240, 248, 255)); // Lighter lavender background

        // Create buttons and panels
        createAddRoomButton();
        createAddRelativeRoomButton();
        createAddDoorButton();
        createAddWindowButton();
        createFurniturePanel();
        createFixturePanel();
        createSaveLoadButtons();
        createDeleteRoomButton();
        createUndoRedoButtons();

        add(Box.createVerticalGlue());
    }

    /**
     * Creates the "Add Room" button.
     */
    private void createAddRoomButton() {
        JButton addRoomButton = new JButton("Add Room");
        addRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addRoomButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addRoomButton.getMinimumSize().height));
        addRoomButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addRoomButton.setBackground(new Color(173, 216, 230));
        addRoomButton.setFocusPainted(false);
        addRoomButton.addActionListener(e -> {
            String[] roomTypes = {"Bedroom", "Bathroom", "Kitchen", "Dining Room"};
            String type = (String) JOptionPane.showInputDialog(this, "Select Room Type:", "Add Room",
                    JOptionPane.PLAIN_MESSAGE, null, roomTypes, roomTypes[0]);
            if (type != null) {
                // Prompt for room width
                String widthStr = JOptionPane.showInputDialog(this, "Enter Room Width (pixels):", "Room Dimensions",
                        JOptionPane.PLAIN_MESSAGE);
                if (widthStr == null) return; // User canceled
                int width;
                try {
                    width = Integer.parseInt(widthStr);
                    if (width <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Prompt for room height
                String heightStr = JOptionPane.showInputDialog(this, "Enter Room Height (pixels):", "Room Dimensions",
                        JOptionPane.PLAIN_MESSAGE);
                if (heightStr == null) return; // User canceled
                int height;
                try {
                    height = Integer.parseInt(heightStr);
                    if (height <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Ask if user wants to name the room
                int option = JOptionPane.showConfirmDialog(this, "Do you want to name the room?", "Name Room", JOptionPane.YES_NO_OPTION);
                String name = null;
                if (option == JOptionPane.YES_OPTION) {
                    name = JOptionPane.showInputDialog(this, "Enter Room Name:", "Room Name", JOptionPane.PLAIN_MESSAGE);
                    if (name != null && name.trim().isEmpty()) {
                        name = null; // Treat empty input as no name
                    }
                }

                Color color = getColorForRoomType(type);
                canvas.setCurrentAction(CanvasPanel.ActionMode.ADD_ROOM);
                canvas.addRoom(type, color, width, height, name);
                canvas.resetActionMode();
            }
        });
        add(addRoomButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the "Add Relative Room" button.
     */
    private void createAddRelativeRoomButton() {
        JButton addRelativeRoomButton = new JButton("Add Relative Room");
        addRelativeRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addRelativeRoomButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addRelativeRoomButton.getMinimumSize().height));
        addRelativeRoomButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addRelativeRoomButton.setBackground(new Color(144, 238, 144));
        addRelativeRoomButton.setFocusPainted(false);
        addRelativeRoomButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] directions = {"North", "South", "East", "West"};
            String direction = (String) JOptionPane.showInputDialog(this, "Select Direction:", "Add Relative Room",
                    JOptionPane.PLAIN_MESSAGE, null, directions, directions[0]);
            if (direction == null) {
                return;
            }

            String[] alignments = {"Left", "Center", "Right"};
            String alignment = (String) JOptionPane.showInputDialog(this, "Select Alignment:", "Add Relative Room",
                    JOptionPane.PLAIN_MESSAGE, null, alignments, alignments[1]);
            if (alignment == null) {
                return;
            }

            String[] roomTypes = {"Bedroom", "Bathroom", "Kitchen", "Dining Room"};
            String type = (String) JOptionPane.showInputDialog(this, "Select Room Type:", "Add Relative Room",
                    JOptionPane.PLAIN_MESSAGE, null, roomTypes, roomTypes[0]);
            if (type == null) {
                return;
            }

            // Prompt for room width
            String widthStr = JOptionPane.showInputDialog(this, "Enter Room Width (pixels):", "Room Dimensions",
                    JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for room height
            String heightStr = JOptionPane.showInputDialog(this, "Enter Room Height (pixels):", "Room Dimensions",
                    JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Ask if user wants to name the room
            int option = JOptionPane.showConfirmDialog(this, "Do you want to name the room?", "Name Room", JOptionPane.YES_NO_OPTION);
            String name = null;
            if (option == JOptionPane.YES_OPTION) {
                name = JOptionPane.showInputDialog(this, "Enter Room Name:", "Room Name", JOptionPane.PLAIN_MESSAGE);
                if (name != null && name.trim().isEmpty()) {
                    name = null; // Treat empty input as no name
                }
            }

            Color color = getColorForRoomType(type);
            canvas.setCurrentAction(CanvasPanel.ActionMode.ADD_RELATIVE_ROOM);
            canvas.addRelativeRoom(selectedRoom, type, direction, alignment, color, width, height, name);
            canvas.resetActionMode();
        });
        add(addRelativeRoomButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the "Add Door" button.
     */
    private void createAddDoorButton() {
        JButton addDoorButton = new JButton("Add Door");
        addDoorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addDoorButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addDoorButton.getMinimumSize().height));
        addDoorButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addDoorButton.setBackground(new Color(255, 182, 193));
        addDoorButton.setFocusPainted(false);
        addDoorButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] directions = {"North", "South", "East", "West"};
            String direction = (String) JOptionPane.showInputDialog(this, "Select Door Direction:", "Add Door",
                    JOptionPane.PLAIN_MESSAGE, null, directions, directions[0]);
            if (direction == null) {
                return;
            }

            canvas.setCurrentAction(CanvasPanel.ActionMode.ADD_DOOR);
            canvas.addDoorToRoom(selectedRoom, direction);
            canvas.resetActionMode();
        });
        add(addDoorButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the "Add Window" button.
     */
    private void createAddWindowButton() {
        JButton addWindowButton = new JButton("Add Window");
        addWindowButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addWindowButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addWindowButton.getMinimumSize().height));
        addWindowButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addWindowButton.setBackground(new Color(135, 206, 250));
        addWindowButton.setFocusPainted(false);
        addWindowButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] directions = {"North", "South", "East", "West"};
            String direction = (String) JOptionPane.showInputDialog(this, "Select Window Direction:", "Add Window",
                    JOptionPane.PLAIN_MESSAGE, null, directions, directions[0]);
            if (direction == null) {
                return;
            }

            canvas.setCurrentAction(CanvasPanel.ActionMode.ADD_WINDOW);
            canvas.addWindowToRoom(selectedRoom, direction);
            canvas.resetActionMode();
        });
        add(addWindowButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the furniture panel with options to add furniture.
     */
    private void createFurniturePanel() {
        JPanel furniturePanel = new JPanel();
        furniturePanel.setLayout(new BoxLayout(furniturePanel, BoxLayout.Y_AXIS));
        furniturePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        furniturePanel.setBorder(BorderFactory.createTitledBorder("Furniture"));

        // Add existing Chair and Table buttons
        JButton addChairButton = new JButton("Add Chair");
        addChairButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addChairButton.getMinimumSize().height));
        addChairButton.setBackground(new Color(255, 228, 196));
        addChairButton.setFocusPainted(false);
        addChairButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for chair dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Chair Width (pixels):", "Chair Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Chair Height (pixels):", "Chair Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture chair = new Furniture(width, height, "Chair", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/chair.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            chair.setImage(scaledImage, "/resources/chair.png");

            canvas.addFurnitureToRoom(selectedRoom, chair);
        });

        JButton addTableButton = new JButton("Add Table");
        addTableButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addTableButton.getMinimumSize().height));
        addTableButton.setBackground(new Color(255, 182, 193));
        addTableButton.setFocusPainted(false);
        addTableButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for table dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Table Width (pixels):", "Table Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Table Height (pixels):", "Table Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture table = new Furniture(width, height, "Table", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/table.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            table.setImage(scaledImage, "/resources/table.png");

            canvas.addFurnitureToRoom(selectedRoom, table);
        });

        // Add new furniture buttons
        JButton addBedButton = new JButton("Add Bed");
        addBedButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addBedButton.getMinimumSize().height));
        addBedButton.setBackground(new Color(175, 238, 238));
        addBedButton.setFocusPainted(false);
        addBedButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for bed dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Bed Width (pixels):", "Bed Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Bed Height (pixels):", "Bed Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture bed = new Furniture(width, height, "Bed", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/bed.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            bed.setImage(scaledImage, "/resources/bed.png");

            canvas.addFurnitureToRoom(selectedRoom, bed);
        });

        JButton addSofaButton = new JButton("Add Sofa");
        addSofaButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addSofaButton.getMinimumSize().height));
        addSofaButton.setBackground(new Color(221, 160, 221));
        addSofaButton.setFocusPainted(false);
        addSofaButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for sofa dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Sofa Width (pixels):", "Sofa Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Sofa Height (pixels):", "Sofa Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture sofa = new Furniture(width, height, "Sofa", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/sofa.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            sofa.setImage(scaledImage, "/resources/sofa.png");

            canvas.addFurnitureToRoom(selectedRoom, sofa);
        });

        JButton addDiningSetButton = new JButton("Add Dining Set");
        addDiningSetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addDiningSetButton.getMinimumSize().height));
        addDiningSetButton.setBackground(new Color(255, 255, 224));
        addDiningSetButton.setFocusPainted(false);
        addDiningSetButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for dining set dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Dining Set Width (pixels):", "Dining Set Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Dining Set Height (pixels):", "Dining Set Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture diningSet = new Furniture(width, height, "Dining Set", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/dining_set.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            diningSet.setImage(scaledImage, "/resources/dining_set.png");

            canvas.addFurnitureToRoom(selectedRoom, diningSet);
        });

        furniturePanel.add(addChairButton);
        furniturePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        furniturePanel.add(addTableButton);
        furniturePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        furniturePanel.add(addBedButton);
        furniturePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        furniturePanel.add(addSofaButton);
        furniturePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        furniturePanel.add(addDiningSetButton);

        add(furniturePanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the fixture panel with options to add fixtures.
     */
    private void createFixturePanel() {
        JPanel fixturePanel = new JPanel();
        fixturePanel.setLayout(new BoxLayout(fixturePanel, BoxLayout.Y_AXIS));
        fixturePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fixturePanel.setBorder(BorderFactory.createTitledBorder("Fixtures"));

        JButton addCommodeButton = new JButton("Add Commode");
        addCommodeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addCommodeButton.getMinimumSize().height));
        addCommodeButton.setBackground(new Color(255, 222, 173));
        addCommodeButton.setFocusPainted(false);
        addCommodeButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for commode dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Commode Width (pixels):", "Commode Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Commode Height (pixels):", "Commode Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture commode = new Furniture(width, height, "Commode", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/commode.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            commode.setImage(scaledImage, "/resources/commode.png");

            canvas.addFurnitureToRoom(selectedRoom, commode);
        });

        JButton addWashbasinButton = new JButton("Add Washbasin");
        addWashbasinButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addWashbasinButton.getMinimumSize().height));
        addWashbasinButton.setBackground(new Color(152, 251, 152));
        addWashbasinButton.setFocusPainted(false);
        addWashbasinButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for washbasin dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Washbasin Width (pixels):", "Washbasin Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Washbasin Height (pixels):", "Washbasin Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture washbasin = new Furniture(width, height, "Washbasin", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/washbasin.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            washbasin.setImage(scaledImage, "/resources/washbasin.png");

            canvas.addFurnitureToRoom(selectedRoom, washbasin);
        });

        JButton addShowerButton = new JButton("Add Shower");
        addShowerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addShowerButton.getMinimumSize().height));
        addShowerButton.setBackground(new Color(224, 255, 255));
        addShowerButton.setFocusPainted(false);
        addShowerButton.addActionListener(e -> {
            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(this, "Please select a room first.", "No Room Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prompt for shower dimensions
            String widthStr = JOptionPane.showInputDialog(this, "Enter Shower Width (pixels):", "Shower Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (widthStr == null) return; // User canceled
            int width;
            try {
                width = Integer.parseInt(widthStr);
                if (width <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid width entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String heightStr = JOptionPane.showInputDialog(this, "Enter Shower Height (pixels):", "Shower Dimensions", JOptionPane.PLAIN_MESSAGE);
            if (heightStr == null) return; // User canceled
            int height;
            try {
                height = Integer.parseInt(heightStr);
                if (height <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid height entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Furniture shower = new Furniture(width, height, "Shower", selectedRoom);
            // Set image from resources
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/shower.png"));
            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            shower.setImage(scaledImage, "/resources/shower.png");

            canvas.addFurnitureToRoom(selectedRoom, shower);
        });

        fixturePanel.add(addCommodeButton);
        fixturePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        fixturePanel.add(addWashbasinButton);
        fixturePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        fixturePanel.add(addShowerButton);

        add(fixturePanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the "Save" and "Load" buttons.
     */
    private void createSaveLoadButtons() {
        JPanel saveLoadPanel = new JPanel();
        saveLoadPanel.setLayout(new BoxLayout(saveLoadPanel, BoxLayout.X_AXIS));
        saveLoadPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveLoadPanel.setBorder(BorderFactory.createTitledBorder("Save/Load"));

        JButton saveButton = new JButton("Save Plan");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveButton.setBackground(new Color(144, 238, 144));
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    PlanData data = new PlanData(canvas.getRooms(), canvas.getFurnitures());
                    PlanSerializer.savePlan(data, file);
                    JOptionPane.showMessageDialog(this, "Plan saved successfully.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving plan: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton loadButton = new JButton("Load Plan");
        loadButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        loadButton.setBackground(new Color(255, 160, 122));
        loadButton.setFocusPainted(false);
        loadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    PlanData data = PlanSerializer.loadPlan(file);
                    canvas.getRooms().clear();
                    canvas.getFurnitures().clear();

                    // Add loaded rooms and furnitures
                    canvas.getRooms().addAll(data.getRooms());
                    canvas.getFurnitures().addAll(data.getFurnitures());

                    // Restore room references in furnitures and load images
                    for (Furniture furniture : canvas.getFurnitures()) {
                        furniture.setRoom(findRoomForFurniture(furniture));
                        if (furniture.getImagePath() != null && !furniture.getImagePath().trim().isEmpty()) {
                            ImageIcon icon = new ImageIcon(getClass().getResource(furniture.getImagePath()));
                            // Optionally, scale the image to fit the furniture dimensions
                            Image scaledImage = icon.getImage().getScaledInstance(furniture.getWidth(), furniture.getHeight(), Image.SCALE_SMOOTH);
                            furniture.setImage(scaledImage, furniture.getImagePath());
                        }
                    }

                    // Restore room references in doors and windows
                    for (Room room : canvas.getRooms()) {
                        for (Door door : room.getDoors()) {
                            door.setRoom(room);
                        }
                        for (PlanWindow window : room.getWindows()) {
                            window.setRoom(room);
                        }
                    }

                    canvas.repaint();
                    JOptionPane.showMessageDialog(this, "Plan loaded successfully.", "Load Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException | ClassNotFoundException ex) {
                    JOptionPane.showMessageDialog(this, "Error loading plan: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        saveLoadPanel.add(saveButton);
        saveLoadPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        saveLoadPanel.add(loadButton);

        add(saveLoadPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the "Delete Room" button.
     */
    private void createDeleteRoomButton() {
        deleteRoomButton = new JButton("Delete Room");
        deleteRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteRoomButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, deleteRoomButton.getMinimumSize().height));
        deleteRoomButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        deleteRoomButton.setBackground(new Color(255, 99, 71));
        deleteRoomButton.setForeground(Color.WHITE);
        deleteRoomButton.setFocusPainted(false);
        deleteRoomButton.setEnabled(false);
        deleteRoomButton.addActionListener(e -> {
            if (selectedRoom != null) {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the selected room?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    // Create and execute delete command
                    CanvasPanel.DeleteRoomCommand cmd = canvas.new DeleteRoomCommand(selectedRoom);
                    cmd.execute();
                    canvas.pushUndo(cmd);
                    canvas.clearRedo();
                    selectedRoom = null;
                    onRoomSelected(null);
                }
            }
        });
        add(deleteRoomButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Creates the "Undo" and "Redo" buttons.
     */
    private void createUndoRedoButtons() {
        JPanel undoRedoPanel = new JPanel();
        undoRedoPanel.setLayout(new BoxLayout(undoRedoPanel, BoxLayout.X_AXIS));
        undoRedoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        undoRedoPanel.setBorder(BorderFactory.createTitledBorder("Undo/Redo"));

        JButton undoButton = new JButton("Undo (Ctrl+Z)");
        undoButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        undoButton.setBackground(new Color(255, 215, 0));
        undoButton.setFocusPainted(false);
        undoButton.addActionListener(e -> canvas.performUndo());

        JButton redoButton = new JButton("Redo (Ctrl+Y)");
        redoButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        redoButton.setBackground(new Color(100, 149, 237));
        redoButton.setForeground(Color.WHITE);
        redoButton.setFocusPainted(false);
        redoButton.addActionListener(e -> canvas.performRedo());

        undoRedoPanel.add(undoButton);
        undoRedoPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        undoRedoPanel.add(redoButton);

        add(undoRedoPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * Finds the room that a furniture belongs to.
     *
     * @param furniture The furniture to find the room for.
     * @return The Room to which the furniture belongs, or null if not found.
     */
    private Room findRoomForFurniture(Furniture furniture) {
        for (Room room : canvas.getRooms()) {
            if (room.getFurnitures().contains(furniture)) {
                return room;
            }
        }
        return null;
    }

    /**
     * Handles room selection events from the canvas.
     *
     * @param room The Room that was selected.
     */
    @Override
    public void onRoomSelected(Room room) {
        selectedRoom = room;
        deleteRoomButton.setEnabled(room != null);
    }

    /**
     * Provides color coding based on room type.
     *
     * @param type The type of the room.
     * @return The corresponding Color.
     */
    private Color getColorForRoomType(String type) {
        switch (type) {
            case "Bedroom":
                return new Color(152, 251, 152); // Light green
            case "Bathroom":
                return new Color(104, 181, 244); // Steel blue
            case "Kitchen":
                return new Color(251, 75, 0); // Light salmon
            case "Dining Room":
                return new Color(251, 201, 73); // Bisque
            default:
                return Color.LIGHT_GRAY;
        }
    }
}