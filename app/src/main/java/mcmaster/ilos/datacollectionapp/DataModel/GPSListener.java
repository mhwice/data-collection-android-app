package mcmaster.ilos.datacollectionapp.DataModel;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.GPSItem;
import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;

import static android.content.Context.LOCATION_SERVICE;

/* This class is responsible for scanning for GPS scans and recording that information. Note: GPS scanning is a heavy operation. */
public class GPSListener implements LocationListener {

    private BufferedWriter gpsBF = null;
    private LocationManager mLocationManager;
    private GnssStatus.Callback mGnssStatusCallback;
    private MapsActivity activity;
    private volatile static GPSListener gpsListener = null;
    private final int SCAN_FREQUENCY = 500;
    private final String FILENAME = "gpsCheckpoint.txt";
    public int numScans = 0;

    private void createCheckpointFiles() {

        /* Creates the directory to hold the files */
        File dataFolder = new File(activity.getFilesDir(),"checkpoints/");
        try {
            if (!dataFolder.exists() && !dataFolder.mkdir()) {
                Log.e("CRASH", "Failed to make directory to hold checkpoint files");
            } else {
                String internalFolderPath = dataFolder.getAbsolutePath();

                File gpsOutputFile = new File(internalFolderPath, FILENAME);
                FileWriter gpsFileWriter = new FileWriter(gpsOutputFile.getAbsolutePath(), true);
                gpsBF = new BufferedWriter(gpsFileWriter);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to create directory to hold checkpoint files", e);
        }
    }

    private  void closeCheckpointFiles() {
        try {
            if (gpsBF != null) {
                gpsBF.close();
                gpsBF = null;
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
        File gpsFile = getCheckpointFileWithName();
        if (gpsFile != null) {
            if (!gpsFile.delete()) {
                Log.e("CRASH", "Failed to delete gps checkpoint file");
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private GPSListener() {

    }

    /* Returns current GPSListener instance if it exists, returns new GPSListener otherwise */
    public static GPSListener getInstance() {
        if (gpsListener == null) {
            /* Makes sure that no other thread is modifying this object */
            synchronized (GPSListener.class) {
                if (gpsListener == null) {
                    gpsListener = new GPSListener();
                }
            }
        }
        return gpsListener;
    }

    public void init(final MapsActivity activity) {
        this.activity = activity;
        mLocationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mGnssStatusCallback = new GnssStatus.Callback() {
				@Override
				public void onSatelliteStatusChanged(GnssStatus status) {
					numScans+=1;
					try {
						String data = status.getSatelliteCount() + "," + SystemClock.elapsedRealtimeNanos();
						if (gpsBF != null) {
							gpsBF.write(data);
							gpsBF.newLine();
						}
					} catch (Exception e) {
						Log.e("CRASH", "Failed to write gps data", e);
					}
				}
			};
		}
	}

    public void clearGPSData() {
        numScans = 0;
    }

    public void start() {

        if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            if (gpsBF == null) {
                createCheckpointFiles();
            }

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
			}
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, SCAN_FREQUENCY, 0, this);
        } catch (SecurityException e) {
            Log.e("CRASH", "GPS Fail", e);
        }
    }

    public void stop() {
        mLocationManager.removeUpdates(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
		}
		closeCheckpointFiles();
    }
}
