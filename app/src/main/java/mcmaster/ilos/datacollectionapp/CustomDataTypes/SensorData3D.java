package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

/* Used to represent sensor data with values along each axis */
public class SensorData3D {

    public float x;
    public float y;
    public float z;
    public long timestamp; // Timestamp in nanoseconds since last boot

    public SensorData3D(float x, float y, float z, long timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    @Override
    @NonNull
    public String toString() {
        return "Timestamp: " + timestamp + ", x: " + x + ", y: " + y + ", z: " + z;
    }
}