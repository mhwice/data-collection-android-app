package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

/* Represents a single cellular item */
public class CellItem {

    public long timeStamp; // Timestamp in nanoseconds since last boot
    public String type;
    public int rssi;
    public String id;

    public CellItem(long timeStamp, int rssi, String type, String id) {
        this.timeStamp = timeStamp;
        this.type = type;
        this.rssi = rssi;
        this.id = id;
    }

    @Override
    @NonNull
    public String toString() {
        return "Timestamp: " + timeStamp + ", Type: " + type + ", Rssi: " + rssi + ", Id: " + id;
    }
}
