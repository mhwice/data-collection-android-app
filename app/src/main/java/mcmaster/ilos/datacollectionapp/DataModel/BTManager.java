package mcmaster.ilos.datacollectionapp.DataModel;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collections;

import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;

import static org.apache.commons.numbers.core.Precision.round;

/* Bluetooth manager class. Responsible for scanning for BT beacons */
public class BTManager {

    private BufferedWriter btBF = null;
    private BluetoothAdapter adapter;
    private MapsActivity activity;
    private volatile static BTManager mBluetoothManager = null;
    private BluetoothLeScanner bleScanner;
    private final String FILENAME = "btCheckpoint.txt";
    public int numScans = 0;

    private BTManager() {}

    private void createCheckpointFiles() {

        /* Creates the directory to hold the files */
        File dataFolder = new File(activity.getFilesDir(),"checkpoints/");
        try {
            if (!dataFolder.exists() && !dataFolder.mkdir()) {
                Log.e("CRASH", "Failed to make directory to hold checkpoint files");
            } else {
                String internalFolderPath = dataFolder.getAbsolutePath();

                File btOutputFile = new File(internalFolderPath, FILENAME);
                FileWriter btFileWriter = new FileWriter(btOutputFile.getAbsolutePath(), true);
                btBF = new BufferedWriter(btFileWriter);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to create directory to hold checkpoint files", e);
        }
    }

    private  void closeCheckpointFiles() {
        try {
            if (btBF != null) {
                btBF.close();
                btBF = null;
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
        File btFile = getCheckpointFileWithName();
        if (btFile != null) {
            if (!btFile.delete()) {
                Log.e("CRASH", "Failed to delete bt checkpoint file");
            }
        }
    }

    /* Get instance if one exists */
    public static BTManager getInstance() {
        if (mBluetoothManager == null) {
            /* Makes sure that no other thread is modifying this object */
            synchronized (BTManager.class) {
                if (mBluetoothManager == null) {
                    mBluetoothManager = new BTManager();
                }
            }
        }
        return mBluetoothManager;
    }

    public void init(MapsActivity activity) {
        try {
            this.adapter = BluetoothAdapter.getDefaultAdapter();
            this.activity = activity;

            BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            adapter = bluetoothManager.getAdapter();

        } catch (Exception e) {
            Log.e("CRASH", "Failed to inti BT manager");
        }
    }

    public void startScan() {
        try {
            if (btBF == null) {
                createCheckpointFiles();
            }

            if (adapter == null || !adapter.isEnabled()) {
                BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
                adapter = bluetoothManager.getAdapter();
            }
            else {
                bleScanner = adapter.getBluetoothLeScanner();
                if (bleScanner != null) {
                    final ScanFilter scanFilter = new ScanFilter.Builder().build();
                    ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                    bleScanner.startScan(Collections.singletonList(scanFilter), settings, scanCallback);
                }
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to start scanning bluetooth");
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            numScans+=1;

            try {
                String data = result.getTimestampNanos() + "," + result.getDevice().getAddress() + "," + result.getRssi();
                if (btBF != null) {
                    btBF.write(data);
                    btBF.newLine();
                }
            } catch (Exception e) {
                Log.e("CRASH", "Failed to write bt data", e);
            }

            super.onScanResult(callbackType, result);
        }
    };

    public void stopScanning() {
        try {
            if (adapter == null || !adapter.isEnabled()) {
                BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
                adapter = bluetoothManager.getAdapter();
            } else {
                bleScanner = adapter.getBluetoothLeScanner();
                if (bleScanner != null) {
                    bleScanner.stopScan(scanCallback);
                }
            }
            closeCheckpointFiles();
        } catch (Exception e) {
            Log.e("CRASH", "Failed to stop scanning of bluetooth");
        }
    }

    public void clearData() {
        numScans = 0;
    }
}
