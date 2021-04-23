package mcmaster.ilos.datacollectionapp.CustomDataTypes;

/* Represents the metadata of a marker */
public class MarkerData {

    private Double latitude;
    private Double longitude;
    private String floor;

    public MarkerData(Double latitude, Double longitude, String floor) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.floor = floor;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getFloor() {
        return floor;
    }
}
