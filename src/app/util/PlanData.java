package app.util;

import app.model.Furniture;
import app.model.Room;
import java.io.Serializable;
import java.util.List;

/**
 * Serializable class to hold plan data.
 */
public class PlanData implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Room> rooms;
    private List<Furniture> furnitures;

    public PlanData(List<Room> rooms, List<Furniture> furnitures) {
        this.rooms = rooms;
        this.furnitures = furnitures;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Furniture> getFurnitures() {
        return furnitures;
    }
}