package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import android.support.annotation.NonNull;

/* Represents a single Wifi item */
public class RSSItem {

    public int scanNum;
    public long timeStamp; // Timestamp in nanoseconds since last boot
    public String bssid;
    public String ssid;
    public int frequency;
    public int level;

    public RSSItem(int scanNum, long timeStamp, String bssid, String ssid, int frequency, int level) {
        this.scanNum = scanNum;
        this.timeStamp = timeStamp;
        this.frequency = frequency;
        this.bssid = bssid;
        this.ssid = ssid;
        this.level = level;
    }

    @Override
    @NonNull
    public String toString() {
        return "ScanNum: " + scanNum + ", Timestamp: " + timeStamp + ", Bssid: " + bssid + ", Ssid: " + ssid + ", Frequency: " + frequency + ", Level: " + level;
    }
}