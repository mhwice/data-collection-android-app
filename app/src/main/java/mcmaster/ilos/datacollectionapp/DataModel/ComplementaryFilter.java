package mcmaster.ilos.datacollectionapp.DataModel;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData1D;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData3D;

//sensor data thread ref. https://stackoverflow.com/questions/3286815/sensoreventlistener-in-separate-thread

// A Java-based Complimentary filter used only for experimentation. Please DO NOT DELETE for now.
public class ComplementaryFilter implements SensorEventListener {

    /* Activity this is being called in */
    private MapsActivity activity;

    /* Sensor thread management */
    private Handler mSensorHandler;
    private SensorManager sensorManager;

    /* Volatile - The value of this variable will never be cached thread-locally: all reads and writes will go straight to "main memory" */
    private volatile static ComplementaryFilter complementaryFilter = null;

    /* Sensors to collect */
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor lightSensor;
    private Sensor linearAccelerometer;

    /* ArrayLists to hold sensor data */
    private ArrayList<SensorData3D> gyroData = new ArrayList<>();
    private ArrayList<SensorData3D> accData = new ArrayList<>();
    private ArrayList<SensorData3D> magData = new ArrayList<>();
    private ArrayList<SensorData1D> lightData = new ArrayList<>();
    private ArrayList<SensorData3D> linearAccData = new ArrayList<>();

    /* Complementary Filter */
    private SensorManager mSensorManager = null;

    private float[] gyro = new float[3];
    private float[] gyroMatrix = new float[9];
    private float[] gyroOrientation = new float[3];
    private ArrayList<SensorData3D> gyroOrientationArray = new ArrayList<>();
    private float[] magnet = new float[3];
    private float[] accel = new float[3];
    private float[] accMagOrientation = new float[3];
    private float[] fusedOrientation = new float[3];
    private ArrayList<SensorData3D> fusedOrientationArray = new ArrayList<>();
    private float[] rotationMatrix = new float[9];

    private static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private long timestamp;
    private boolean initState = true;

    private static final int TIME_CONSTANT = 30;
    private static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();

    private ComplementaryFilter() {}

    /* Returns current SensorDataManager instance if it exists, returns new SensorDataManager otherwise */
    public static ComplementaryFilter getInstance() {
        if (complementaryFilter == null) {
            /* Makes sure that no other thread is modifying this object */
            synchronized (ComplementaryFilter.class) {
                if (complementaryFilter == null) {
                    complementaryFilter = new ComplementaryFilter();
                }
            }
        }
        return complementaryFilter;
    }

    /* Declare the sensors we want to access, then create a new thread */
    public void init(MapsActivity activity) {
        this.activity = activity;

        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        /* This assumes that the devices indeed does have these sensors which might not be true */
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        HandlerThread mSensorThread = new HandlerThread("SensorThread");
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper());
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        /* If there is no data collection happening, ignore changes in sensor data */
        if (!activity.isCollecting) {
            return;
        }

        /* Write sensor data to ArrayLists */
        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                gyroData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
                gyroFunction(sensorEvent);
                break;
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                accData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
                System.arraycopy(sensorEvent.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
//                Log.i("MAG", "X:" + sensorEvent.values[0] + ", Y:" + sensorEvent.values[1] + ", Z:" + sensorEvent.values[2]);
                System.arraycopy(sensorEvent.values, 0, magnet, 0, 3);
                break;
            case Sensor.TYPE_LIGHT:
                lightData.add(new SensorData1D(sensorEvent.values[0],sensorEvent.timestamp/1000));
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                linearAccData.add(new SensorData3D(sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2],sensorEvent.timestamp/1000));
                break;
        }
    }

    /* Called when accuracy of sensor changes, if sensor accuracy is low, this is a problem. */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        switch(sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                switch(i) {
                    case SensorManager.SENSOR_STATUS_ACCURACY_LOW :
                        Log.i("TYPE_MAGNETIC_FIELD", "LOW");
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM :
                        Log.i("TYPE_MAGNETIC_FIELD", "MEDIUM");
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_HIGH :
                        Log.i("TYPE_MAGNETIC_FIELD", "HIGH");
                        break;
                    case SensorManager.SENSOR_STATUS_UNRELIABLE :
                        Log.i("TYPE_MAGNETIC_FIELD", "NONE");
                        break;
                }
                break;

            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                switch(i) {
                    case SensorManager.SENSOR_STATUS_ACCURACY_LOW :
                        Log.i("TYPE_GYROSCOPE", "LOW");
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM :
                        Log.i("TYPE_GYROSCOPE", "MEDIUM");
                        break;
                    case SensorManager.SENSOR_STATUS_ACCURACY_HIGH :
                        Log.i("TYPE_GYROSCOPE", "HIGH");
                        break;
                    case SensorManager.SENSOR_STATUS_UNRELIABLE :
                        Log.i("TYPE_GYROSCOPE", "NONE");
                        break;
                }
                break;
            default:
                break;
        }
    }

    /* Start a listener for changes in sensor data */
    public void startScanning() {
        Log.i("RUN", "START");
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);
        }
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);
        }
        if (linearAccelerometer != null) {
            sensorManager.registerListener(this, linearAccelerometer, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);
        }

        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(), 1000, TIME_CONSTANT);

        // SENSOR_DELAY_NORMAL - 200,000 microseconds = 0.2 seconds
        // SENSOR_DELAY_GAME - 20,000 microseconds = 0.02 seconds
        // SENSOR_DELAY_UI - 60,000 microseconds = 0.06 seconds
        // SENSOR_DELAY_FASTEST - 0 microseconds = 0.00 seconds
        // Reference - https://developer.android.com/guide/topics/sensors/sensors_overview#java
    }

    /* Unregisters the sensor listeners */
    public void stopScanning() {
        Log.i("RUN", "STOP");
        fuseTimer.cancel();
        sensorManager.unregisterListener(this);
    }

    /* Removes all sensor data from the ArrayLists */
    public void clearSensorData() {
        accData.clear();
        gyroData.clear();
        magData.clear();
        lightData.clear();
        linearAccData.clear();
    }

    // calculates orientation angles from accelerometer and magnetometer output
    public void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    // This function is borrowed from the Android reference
    // at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    // It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues, float[] deltaRotationVector, float timeFactor) {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude = (float) Math.sqrt(gyroValues[0] * gyroValues[0] + gyroValues[1] * gyroValues[1] + gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if (initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
        gyroOrientationArray.add(new SensorData3D(gyroOrientation[0],gyroOrientation[1],gyroOrientation[2],timestamp));
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            /*
             * Fix for 179� <--> -179� transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360� (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360� from the result
             * if it is greater than 180�. This stabilizes the output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
//            Log.i("OUTPUT", "----------------------------------------------------");
//            Log.i("OUTPUT", "accMag[0]" + accMagOrientation[0] * 180/Math.PI);
//            Log.i("OUTPUT", "accMag[1]" + accMagOrientation[1] * 180/Math.PI);
//            Log.i("OUTPUT", "accMag[2]" + accMagOrientation[2] * 180/Math.PI);
//            Log.i("OUTPUT", "gyroOr[0]" + gyroOrientation[0] * 180/Math.PI);
//            Log.i("OUTPUT", "gyroOr[1]" + gyroOrientation[1] * 180/Math.PI);
//            Log.i("OUTPUT", "gyroOr[2]" + gyroOrientation[2] * 180/Math.PI);
//            Log.i("OUTPUT", "fusedO[0]" + fusedOrientation[0] * 180/Math.PI);
//            Log.i("OUTPUT", "fusedO[1]" + fusedOrientation[1] * 180/Math.PI);
//            Log.i("OUTPUT", "fusedO[2]" + fusedOrientation[2] * 180/Math.PI);
//            Log.i("OUTPUT", "----------------------------------------------------");
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
            fusedOrientationArray.add(new SensorData3D(convertToFloat(fusedOrientation[0]), convertToFloat(fusedOrientation[1]), convertToFloat(fusedOrientation[2]),timestamp/1000));
        }
    }

    public static Float convertToFloat(double doubleValue) {
        return (float) doubleValue;
    }
}