package mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData1D;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData3D;
import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;
import mcmaster.ilos.datacollectionapp.SensorCalibration.SensorCalibrationActivity;

public class SensorCollector implements SensorEventListener {

    /* Activity this is being called in */
    private SensorCalibrationActivity activity;

    /* Sensor thread management */
    private Handler mSensorHandler;
    private SensorManager sensorManager;

    /* Volatile - The value of this variable will never be cached thread-locally: all reads and writes will go straight to "main memory" */
    private volatile static SensorCollector sensorDataManager = null;

    /* Sensors to collect */
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor lightSensor;
    private Sensor linearAccelerometer;

    /* ArrayLists to hold sensor data */
    public ArrayList<SensorData3D> gyroData = new ArrayList<>();
    public ArrayList<SensorData3D> accData = new ArrayList<>();
    public ArrayList<SensorData3D> magData = new ArrayList<>();
    public ArrayList<SensorData1D> lightData = new ArrayList<>();
    public ArrayList<SensorData3D> linearAccData = new ArrayList<>();

    /* Complementary Filter */


    private SensorCollector() {}

    /* Returns current SensorDataManager instance if it exists, returns new SensorDataManager otherwise */
    public static SensorCollector getInstance() {
        if (sensorDataManager == null) {
            /* Makes sure that no other thread is modifying this object */
            synchronized (SensorCollector.class) {
                if (sensorDataManager == null) {
                    sensorDataManager = new SensorCollector();
                }
            }
        }
        return sensorDataManager;
    }

    /* Declare the sensors we want to access, then create a new thread */
    public void init(SensorCalibrationActivity activity) {
        try {
            sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

            /* This assumes that the devices indeed does have these sensors which might not be true */
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

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
//        if (!activity.isCollecting) {
//            return;
//        }

        try {
            /* Write sensor data to ArrayLists */
            int sensorType = sensorEvent.sensor.getType();
            switch (sensorType) {
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    gyroData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
                    break;
                case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                    accData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
                    break;
                case Sensor.TYPE_LIGHT:
                    lightData.add(new SensorData1D(sensorEvent.values[0],sensorEvent.timestamp/1000));
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    linearAccData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
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

    /* Start a listener for changes in sensor data */
    public void startScanning() {
        try {
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, mSensorHandler);
            }
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, mSensorHandler);
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME, mSensorHandler);
            }
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME, mSensorHandler);
            }
            if (linearAccelerometer != null) {
                sensorManager.registerListener(this, linearAccelerometer, SensorManager.SENSOR_DELAY_GAME, mSensorHandler);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to register sensors");
        }

        // SENSOR_DELAY_NORMAL - 200,000 microseconds = 0.2 seconds
        // SENSOR_DELAY_GAME - 20,000 microseconds = 0.02 seconds
        // SENSOR_DELAY_UI - 60,000 microseconds = 0.06 seconds
        // SENSOR_DELAY_FASTEST - 0 microseconds = 0.00 seconds
        // Reference - https://developer.android.com/guide/topics/sensors/sensors_overview#java
    }

    /* Unregisters the sensor listeners */
    public void stopScanning() {
        try {
            sensorManager.unregisterListener(this);
        } catch (Exception e) {
            Log.e("CRASH", "Failed to unregister sensor manager");
        }
    }

    /* Removes all sensor data from the ArrayLists */
    public void clearSensorData() {
        accData.clear();
        gyroData.clear();
        magData.clear();
        lightData.clear();
        linearAccData.clear();
    }
}