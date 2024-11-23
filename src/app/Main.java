package app;

import app.gui.CanvasPanel;
import app.gui.ControlPanel;
import java.awt.*;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2D Floor Planner");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen mode
            frame.setUndecorated(false); // Set to true if you want no window borders

            CanvasPanel canvas = new CanvasPanel();
            ControlPanel controlPanel = new ControlPanel(canvas);
            canvas.addRoomSelectionListener(controlPanel); // Ensure control panel listens to room selection

            frame.setLayout(new BorderLayout());
            frame.add(controlPanel, BorderLayout.WEST);
            frame.add(new JScrollPane(canvas), BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }
}