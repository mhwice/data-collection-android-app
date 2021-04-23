package mcmaster.ilos.datacollectionapp.DataModel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.RSSItem;
import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;

/* Responsible for scanning and recording wifi data */
public class WifiDataManager {

    /* Writes wifi scan data to the disk */
    private BufferedWriter wifiBF = null;
    private final String FILENAME = "wifiCheckpoint.txt";

    /* Activity this is being called in */
    private MapsActivity activity;

    /* Volatile - The value of this variable will never be cached thread-locally: all reads and writes will go straight to "main memory" */
    private volatile static WifiDataManager wifiDataManager = null;

    /* Wifi Management */
    private WifiManager wifiManager;
    private WifiManager.WifiLock wifiLock;

    /* Number of scans performed */
    public int scanNum = 0;

    /* Map and ArrayList to hold wifi data */
    private Map<String, Long> lastRssTime = new HashMap<>();

    /* Thread management */
    private Handler mWifiHandler;
    public boolean isRegistered;

    // Callbacks
    private WifiScanListener wifiListener;

    private WifiDataManager() {

    }

    /* Returns current WifiDataManager instance if it exists, returns new WifiDataManager otherwise */
    public static WifiDataManager getInstance() {
        if (wifiDataManager == null) {
            /* Makes sure that no other thread is modifying this object */
            synchronized (WifiDataManager.class) {
                if (wifiDataManager == null) {
                    wifiDataManager = new WifiDataManager();
                }
            }
        }
        return wifiDataManager;
    }

    /* Acquires a Wifi Lock (a wifi lock prevents the scanner from falling asleep), starts a wifi scanning thread, and begins scanning for rssi's */
    public void init(final MapsActivity activity) {
        this.activity = activity;

        try {
            wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "WiFiLock");
            getWifiLock();

            HandlerThread mWiFiThread = new HandlerThread("WiFiThread");
            mWiFiThread.start();
            mWifiHandler = new Handler(mWiFiThread.getLooper());
        } catch (Exception e) {
            Log.e("CRASH", "Failed to intialize wifi manager");
        }
    }

    private void createCheckpointFiles() {

        /* Creates the directory to hold the files */
        File dataFolder = new File(activity.getFilesDir(),"checkpoints/");
        try {
            if (!dataFolder.exists() && !dataFolder.mkdir()) {
                Log.e("CRASH", "Failed to make directory to hold checkpoint files");
            } else {
                String internalFolderPath = dataFolder.getAbsolutePath();

                File wifiOutputFile = new File(internalFolderPath, FILENAME);
                FileWriter wifiFileWriter = new FileWriter(wifiOutputFile.getAbsolutePath(), true);
                wifiBF = new BufferedWriter(wifiFileWriter);
                Log.i("DEBUGGER", "creating wifiBF");
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to create directory to hold checkpoint files", e);
        }
    }

    private void closeCheckpointFiles() {
        try {
            if (wifiBF != null) {
                wifiBF.close();
                wifiBF = null;
                Log.i("DEBUGGER", "close wifiBF");
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to close checkpoint files", e);
        }
    }

    private File getCheckpointFileWithName() {
        File dataFolder = new File(activity.getFilesDir(),"checkpoints/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.getName().equals(FILENAME)) {
                return file;
            }
        }
        return null;
    }

    public void deleteCheckpointFiles() {
        File wifiFile = getCheckpointFileWithName();
        if (wifiFile != null) {
            if (!wifiFile.delete()) {
                Log.e("CRASH", "Failed to delete wifi checkpoint file");
            }
        }
    }

    private final BroadcastReceiver cycleWiFiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            /* Re-start scan first if it is collecting */
            if (activity.isCollecting) {
                startScanning();
            }

            boolean updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (!updated) {
                return;
            }

            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults == null) {
                return;
            }

            /* Update successful scanned number */
            scanNum++;

            if (wifiListener != null) {
                wifiListener.onScanEvent(scanNum);
            }

            /* Record WiFi Data */
            for (ScanResult r : scanResults) {
                if (lastRssTime.containsKey(r.BSSID) && lastRssTime.get(r.BSSID) >= r.timestamp) {
                } else {
                    lastRssTime.put(r.BSSID, r.timestamp);
                    if (activity.isCollecting) {

                        try {

                            if (r.SSID.equals("")) {
                                continue;
                            }

                            String data = scanNum + "," + r.timestamp*1000 + "," + r.BSSID + "," + r.SSID + "," + r.frequency + "," + r.level;

                            if (wifiBF != null) {
                                wifiBF.write(data);
                                wifiBF.newLine();
                                Log.i("DEBUGGER", "writting wifiBF");
                            }

                        } catch (Exception e) {
                            Log.e("CRASH", "Failed to write wifi data", e);
                        }
                    }
                }
            }
        }
    };

    /* Checks for Wifi Connection */

    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo !=null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /* Wifi Lock */

    private void getWifiLock() {
        try {
            if (wifiLock != null && !wifiLock.isHeld()) {
                wifiLock.acquire();
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to get wifi lock");
        }
    }

    private void releaseWifiLock() {
        try {
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to release wifi lock");
        }
    }

    /* Wifi Receiver */

    private void unregisterWifiReceiver() {
        try {
            activity.unregisterReceiver(cycleWiFiReceiver);
        } catch (Exception e) {
            Log.e("CRASH", "Could not unregister wifi receiver");
        }
    }

    private void registerWifiReceiver() {
        try {
            activity.registerReceiver(cycleWiFiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), null, mWifiHandler);
        } catch (Exception e) {
            Log.e("CRASH", "Could not register wifi receiver");
        }
    }

    /* Start/Stop Scanning */

    public void startScanning() {
        try {
            Log.i("DEBUGGER", "start scanning");
            if (wifiBF == null) {
                createCheckpointFiles();
            }
            getWifiLock();
            isRegistered = true;
            registerWifiReceiver();
            boolean scan_start = wifiManager.startScan();
            while (!scan_start) {
                scan_start = wifiManager.startScan();
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to start scanning wifi");
        }
    }

    public void stopScanning() {
        try {
            Log.i("DEBUGGER", "stop scanning");
            isRegistered = false;
            unregisterWifiReceiver();
            closeCheckpointFiles();
        } catch (Exception e) {
            Log.e("CRASH", "Failed to stop scanning wifi");
        }
    }

    /* Clear Wifi Data */

    public void clearWiFiData() {
        scanNum = 0;
    }

    /* Wifi Lifecycle */

    public void destroy() {
        try {
            isRegistered = false;
            unregisterWifiReceiver();
            releaseWifiLock();
        } catch (Exception e) {
            Log.e("CRASH", "Failed to destroy wifi");
        }
    }

    /* Set Callback */

    public void setScanListener(WifiScanListener wifiListener) {
        this.wifiListener = wifiListener;
    }
}