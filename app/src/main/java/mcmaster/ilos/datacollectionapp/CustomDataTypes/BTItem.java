package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

/* Represents a single bluetooth item */
public class BTItem {

    public long timeStamp; // Timestamp in nanoseconds since last boot
    public String address;
    public int rssi;

    public BTItem(long timeStamp, String address, int rssi) {
        this.timeStamp = timeStamp;
        this.address = address;
        this.rssi = rssi;
    }

    @Override
    @NonNull
    public String toString() {
        return "Timestamp: " + timeStamp + ", Address: " + address + ", Rssi: " + rssi;
    }
}