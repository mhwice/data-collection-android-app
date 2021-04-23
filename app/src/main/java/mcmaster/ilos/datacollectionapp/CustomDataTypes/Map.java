package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

/* Represents a map object - holds metadata relevant to the map */
public class Map {

    private String floorNum;
    private String size;
    private String name;
    private Boolean downloaded;

    public Map(String name, String floorNum, String size) {
        this.name = name;
        this.floorNum = floorNum;
        this.size = size;
    }

    public String getFloorNum() {
        return floorNum;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDownloaded(Boolean downloaded) {
        this.downloaded = downloaded;
    }

    public Boolean getDownloaded() {
        return downloaded;
    }

    @Override
    @NonNull
    public String toString() {
        return "Name: " + name + ", FloorNum: " + floorNum + ", Size: " + size + ", Downloaded: " + downloaded;
    }
}