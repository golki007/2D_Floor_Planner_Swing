package app.util;

import java.io.*;

/**
 * Utility class for serializing and deserializing plan data.
 */
public class PlanSerializer {

    /**
     * Saves the plan data to a file.
     *
     * @param data The plan data to save.
     * @param file The file to save the data to.
     * @throws IOException If an I/O error occurs.
     */
    public static void savePlan(PlanData data, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
        }
    }

    /**
     * Loads the plan data from a file.
     *
     * @param file The file to load the data from.
     * @return The loaded plan data.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    public static PlanData loadPlan(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (PlanData) ois.readObject();
        }
    }
}