package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

import java.util.ArrayList;

/* Represents building the user has created */
public class Building {

    private String name;
    private String address;
    private String description;
    private int numFloors;
    private ArrayList<Map> uploadedMaps;

    public Building(String name, String address, String description, int numFloors, ArrayList<Map> uploadedMaps) {
        this.name = name;
        this.address = address;
        this.description = description;
        this.numFloors = numFloors;
        this.uploadedMaps = uploadedMaps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public int getNumFloors() {
        return numFloors;
    }

    public ArrayList<Map> getUploadedMaps() {
        return uploadedMaps;
    }

    @Override
    @NonNull
    public String toString() {
        return "Name: " + name + ", Address: " + address + ", Description: " + description + ", NumFloors: " + numFloors + ", uploadedMaps: " + uploadedMaps.toString();
    }
}

/*

[Name: itb, FloorNum: 1, Size: 342.8 Kb, Downloaded: null,
 Name: itb, FloorNum: 2, Size: 572.1 Kb, Downloaded: null,
 Name: itb, FloorNum: 1, Size: 342.8 Kb, Downloaded: null,
 Name: itb, FloorNum: 2, Size: 572.1 Kb, Downloaded: null

So the problem is, that the uploadedMaps array is incorrect...

*/


