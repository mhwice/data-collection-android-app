package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.plugins.annotation.Circle;

/* Represents a marker on the map */
public class MapMarker {

    private Circle circle;
    private String floor;
    private boolean visited;
    private long timestamp; // Timestamp in nanoseconds since last boot
    private float altitude = 0.0f;

    public MapMarker(Circle circle, boolean visited, String floor) {
        this.circle = circle;
        this.floor = floor;
        this.visited = visited;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    public boolean getVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public String getFloor() {
        return floor;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public float getAltitude() {
        return altitude;
    }

    @Override
    @NonNull
    public String toString() {
        return "LAT: " + circle.getLatLng().getLatitude() + ", LON: " + circle.getLatLng().getLongitude() + ", ALT: " + altitude + ", TIME: " + timestamp + ", VIS: " + visited;
    }
}
