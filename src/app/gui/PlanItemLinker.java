package app.gui;

import app.model.*;

/**
 * Helper class to link PlanItems to their associated Rooms.
 */
public class PlanItemLinker {

    /**
     * Retrieves the associated Room for a given PlanItem.
     *
     * @param item The PlanItem whose Room is to be found.
     * @return The associated Room, or null if not found.
     */
    public Room getAssociatedRoom(PlanItem item) {
        if (item instanceof Furniture) {
            return ((Furniture) item).getRoom();
        } else if (item instanceof Door) {
            return ((Door) item).getRoom();
        } else if (item instanceof PlanWindow) {
            return ((PlanWindow) item).getRoom();
        }
        return null;
    }

    /**
     * Checks if two PlanItems are in the same Room.
     *
     * @param item1 The first PlanItem.
     * @param item2 The second PlanItem.
     * @return True if both items are in the same Room, false otherwise.
     */
    public boolean areInSameRoom(PlanItem item1, PlanItem item2) {
        Room room1 = getAssociatedRoom(item1);
        Room room2 = getAssociatedRoom(item2);
        if (room1 != null && room2 != null && room1.equals(room2)) {
            return true;
        }
        return false;
    }
}