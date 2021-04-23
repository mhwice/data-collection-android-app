package mcmaster.ilos.datacollectionapp.CustomDataTypes;

/* Used to represent linear acceleration data */
public class SensorData1D {

    public float value;
    public long timestamp; // Timestamp in nanoseconds since last boot

    public SensorData1D(float value,long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }
}