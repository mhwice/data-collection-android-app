package mcmaster.ilos.datacollectionapp.CustomDataTypes;

/* Represents a single GPS item */
public class GPSItem {

    private long timestamp; // Timestamp in nanoseconds since last boot
    private int count;

    public GPSItem(int count, long timestamp) {
        this.count = count;
        this.timestamp = timestamp;
    }

    public int getCount() {
        return count;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
