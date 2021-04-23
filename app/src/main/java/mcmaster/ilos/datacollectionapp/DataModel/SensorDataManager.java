package mcmaster.ilos.datacollectionapp.DataModel;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData1D;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData3D;

import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.hardware.SensorManager.getAltitude;

//sensor data thread ref. https://stackoverflow.com/questions/3286815/sensoreventlistener-in-separate-thread

/* This class is responsible for scanning and recording sensor information */
public class SensorDataManager implements SensorEventListener {

    private BufferedWriter gyroBF = null;
    private BufferedWriter accBF = null;
    private BufferedWriter magBF = null;
    private BufferedWriter lightBF = null;
    private BufferedWriter linearAccBF = null;

    /* Activity this is being called in */
    private MapsActivity activity;

    /* Sensor thread management */
    private Handler mSensorHandler;
    private SensorManager sensorManager;

    /* Volatile - The value of this variable will never be cached thread-locally: all reads and writes will go straight to "main memory" */
    private volatile static SensorDataManager sensorDataManager = null;

    /* Sensors to collect */
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor lightSensor;
    private Sensor linearAccelerometer;

    public int numScansAcc = 0;
    public int numScansGyro = 0;
    public int numScansMag = 0;
    public int numScansLight = 0;
    public int numScansLinAcc = 0;
    public int numScansPres = 0;

    // SENSOR_DELAY_NORMAL  -   200,000 microseconds    =   0.2 seconds     =   3
    // SENSOR_DELAY_UI      -   60,000 microseconds     =   0.06 seconds    =   2
    // SENSOR_DELAY_GAME    -   20,000 microseconds     =   0.02 seconds    =   1
    // SENSOR_DELAY_FASTEST -   0 microseconds          =   0.00 seconds    =   0
    // Reference - https://developer.android.com/guide/topics/sensors/sensors_overview#java
    public final int SCAN_FREQUENCY_FLAG = 3;

    /* ALTITUDE */
    private Sensor pressure;
    public Float firstPressureValue = null;
    public Float lastPressureValue = null;

    private SensorDataManager() {}

    /* Returns current SensorDataManager instance if it exists, returns new SensorDataManager otherwise */
    public static SensorDataManager getInstance() {
        if (sensorDataManager == null) {
            /* Makes sure that no other thread is modifying this object */
            synchronized (SensorDataManager.class) {
                if (sensorDataManager == null) {
                    sensorDataManager = new SensorDataManager();
                }
            }
        }
        return sensorDataManager;
    }

    /* Declare the sensors we want to access, then create a new thread */
    public void init(MapsActivity activity) {
        try {
            this.activity = activity;
            sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

            /* This assumes that the devices indeed does have these sensors which might not be true */
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

            HandlerThread mSensorThread = new HandlerThread("SensorThread");
            mSensorThread.start();
            mSensorHandler = new Handler(mSensorThread.getLooper());
        } catch (Exception e) {
            Log.e("CRASH", "Failed to init sensordatamanager");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        /* If there is no data collection happening, ignore changes in sensor data */
        if (!activity.isCollecting) {
            return;
        }

        try {

            /* Write sensor data to ArrayLists */
            int sensorType = sensorEvent.sensor.getType();
            switch (sensorType) {
                case Sensor.TYPE_GYROSCOPE:

                    numScansGyro+=1;

                    try {
                        String data = sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2] + "," + sensorEvent.timestamp;
                        if (gyroBF != null) {
                            gyroBF.write(data);
                            gyroBF.newLine();
                        }
                    } catch (Exception e) {
                        Log.e("CRASH", "Failed to write gyro data", e);
                    }

                    break;
                case Sensor.TYPE_ACCELEROMETER:

                    numScansAcc+=1;

                    try {
                        String data = sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2] + "," + sensorEvent.timestamp;
                        if (accBF != null) {
                            accBF.write(data);
                            accBF.newLine();
                        }
                    } catch (Exception e) {
                        Log.e("CRASH", "Failed to write acc data", e);
                    }

                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:

                    numScansMag+=1;

                    try {
                        String data = sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2] + "," + sensorEvent.timestamp;
                        if (magBF != null) {
                            magBF.write(data);
                            magBF.newLine();
                        }
                    } catch (Exception e) {
                        Log.e("CRASH", "Failed to write mag data", e);
                    }

                    break;
                case Sensor.TYPE_LIGHT:

                    numScansLight+=1;

                    try {
                        String data = sensorEvent.values[0] + "," + sensorEvent.timestamp;
                        if (lightBF != null) {
                            lightBF.write(data);
                            lightBF.newLine();
                        }
                    } catch (Exception e) {
                        Log.e("CRASH", "Failed to write light data", e);
                    }

                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:

                    numScansLinAcc+=1;

                    try {
                        String data = sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2] + "," + sensorEvent.timestamp;
                        if (linearAccBF != null) {
                            linearAccBF.write(data);
                            linearAccBF.newLine();
                        }
                    } catch (Exception e) {
                        Log.e("CRASH", "Failed to write linearacc data", e);
                    }

                    break;
                case Sensor.TYPE_PRESSURE:

                    numScansPres+=1;

                    // This isn't a great way to get the pressure. Consider the case when the pressure takes 5 seconds to start/
                    // In this case, the user may have walked up stairs before you record the downstairs marker!
                    if (firstPressureValue == null) {
                        firstPressureValue = sensorEvent.values[0];
                    } else {
                        lastPressureValue = sensorEvent.values[0];
                    }

//                    Log.i("PRES", "" + getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lastPressureValue));
                    break;
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to write sensor data to array lists");
        }
    }

    /* Called when accuracy of sensor changes, if sensor accuracy is low, this is a problem. */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        try {
            switch(sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    switch(i) {
                        case SensorManager.SENSOR_STATUS_ACCURACY_LOW :
                            Log.i("MAGACC", "LOW");
                            break;
                        case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM :
                            Log.i("MAGACC", "MEDIUM");
                            break;
                        case SensorManager.SENSOR_STATUS_ACCURACY_HIGH :
                            Log.i("MAGACC", "HIGH");
                            break;
                        case SensorManager.SENSOR_STATUS_UNRELIABLE :
                            Log.i("MAGACC", "NONE");
                            break;
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to notify sensor accuracy changes");
        }
    }

    private File getCheckpointFileWithName(String name) {
        File dataFolder = new File(activity.getFilesDir(),"checkpoints/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        return null;
    }

    private  void closeCheckpointFiles() {
        try {
            if (gyroBF != null) {
                gyroBF.close();
                gyroBF = null;
            }
            if (accBF != null) {
                accBF.close();
                accBF = null;
            }
            if (magBF != null) {
                magBF.close();
                magBF = null;
            }
            if (lightBF != null) {
                lightBF.close();
                lightBF = null;
            }
            if (linearAccBF != null) {
                linearAccBF.close();
                linearAccBF = null;
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to close checkpoint files", e);
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

                File gyroOutputFile = new File(internalFolderPath, "gyroCheckpoint.txt");
                FileWriter gyroFileWriter = new FileWriter(gyroOutputFile.getAbsolutePath(), true);
                gyroBF = new BufferedWriter(gyroFileWriter);

                File accOutputFile = new File(internalFolderPath, "accCheckpoint.txt");
                FileWriter accFileWriter = new FileWriter(accOutputFile.getAbsolutePath(), true);
                accBF = new BufferedWriter(accFileWriter);

                File magOutputFile = new File(internalFolderPath, "magCheckpoint.txt");
                FileWriter magFileWriter = new FileWriter(magOutputFile.getAbsolutePath(), true);
                magBF = new BufferedWriter(magFileWriter);

                File lightOutputFile = new File(internalFolderPath, "lightCheckpoint.txt");
                FileWriter lightFileWriter = new FileWriter(lightOutputFile.getAbsolutePath(), true);
                lightBF = new BufferedWriter(lightFileWriter);

                File linearAccOutputFile = new File(internalFolderPath, "linearAccCheckpoint.txt");
                FileWriter linearAccFileWriter = new FileWriter(linearAccOutputFile.getAbsolutePath(), true);
                linearAccBF = new BufferedWriter(linearAccFileWriter);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to create directory to hold checkpoint files", e);
        }
    }

    /* Start a listener for changes in sensor data */
    public void startScanning() {

        if (gyroBF == null && accBF == null && magBF == null && lightBF == null && linearAccBF == null) {
            createCheckpointFiles();
        }

        try {
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SENSOR_DELAY_NORMAL, mSensorHandler);
            }
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SENSOR_DELAY_NORMAL, mSensorHandler);
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SENSOR_DELAY_NORMAL, mSensorHandler);
            }
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SENSOR_DELAY_NORMAL, mSensorHandler);
            }
            if (linearAccelerometer != null) {
                sensorManager.registerListener(this, linearAccelerometer, SENSOR_DELAY_NORMAL, mSensorHandler);
            }
            if (pressure != null) {
                sensorManager.registerListener(this, pressure, SENSOR_DELAY_NORMAL, mSensorHandler);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to register sensors");
        }
    }

    /* Unregisters the sensor listeners */
    public void stopScanning() {
        try {
            sensorManager.unregisterListener(this);
            closeCheckpointFiles();
        } catch (Exception e) {
            Log.e("CRASH", "Failed to unregister sensor manager");
        }
    }

    /* Removes all sensor data from the ArrayLists */
    public void clearSensorData() {
        numScansAcc = 0;
        numScansGyro = 0;
        numScansMag = 0;
        numScansLight = 0;
        numScansLinAcc = 0;
        numScansPres = 0;
        firstPressureValue = null;
        lastPressureValue = null;
    }

    public void deleteCheckpointFiles() {
        File gyroFile = getCheckpointFileWithName("gyroCheckpoint.txt");
        if (gyroFile != null) {
            if (!gyroFile.delete()) {
                Log.e("CRASH", "Failed to delete gyro checkpoint file");
            }
        }
        File accFile = getCheckpointFileWithName("accCheckpoint.txt");
        if (accFile != null) {
            if (!accFile.delete()) {
                Log.e("CRASH", "Failed to delete acc checkpoint file");
            }
        }
        File magFile = getCheckpointFileWithName("magCheckpoint.txt");
        if (magFile != null) {
            if (!magFile.delete()) {
                Log.e("CRASH", "Failed to delete mag checkpoint file");
            }
        }
        File lightFile = getCheckpointFileWithName("lightCheckpoint.txt");
        if (lightFile != null) {
            if (!lightFile.delete()) {
                Log.e("CRASH", "Failed to delete light checkpoint file");
            }
        }
        File linearAccFile = getCheckpointFileWithName("linearAccCheckpoint.txt");
        if (linearAccFile != null) {
            if (!linearAccFile.delete()) {
                Log.e("CRASH", "Failed to delete linearAcc checkpoint file");
            }
        }
    }
}