package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

/* Represents a metadata of a file (PBF file) that is to be saved to local storage */
public class SaveFile {

    private String building;
    private String floor;
    private String otherFloor;
    private String filename;
    private String size;

    public SaveFile(String building, String floor, String otherFloor, String filename, String size) {
        this.building = building;
        this.floor = floor;
        this.otherFloor = otherFloor;
        this.filename = filename;
        this.size = size;
    }

    public SaveFile(String building, String floor, String otherFloor, String filename) {
        this.building = building;
        this.floor = floor;
        this.otherFloor = otherFloor;
        this.filename = filename;
    }

    public String getBuilding() {
        return building;
    }

    public String getFilename() {
        return filename;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getOtherFloor() {
        return otherFloor;
    }

    @Override
    @NonNull
    public String toString() {
        return "Building: " + building + ", Floor: " + floor + ", OtherFloor: " + otherFloor + ", Filename: " + filename + ", Size: " + size;
    }
}
