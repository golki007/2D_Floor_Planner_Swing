package app.gui;

import app.model.Room;

/**
 * Listener interface for room selection events.
 */
public interface RoomSelectionListener {
    void onRoomSelected(Room room);
}