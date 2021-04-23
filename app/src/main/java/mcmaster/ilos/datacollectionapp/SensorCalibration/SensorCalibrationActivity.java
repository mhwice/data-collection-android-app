package mcmaster.ilos.datacollectionapp.SensorCalibration;

import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;

import org.apache.commons.math4.linear.MatrixUtils;
import org.apache.commons.math4.linear.RealMatrix;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData3D;
import mcmaster.ilos.datacollectionapp.R;
import mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationManager;
import mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.SensorCollector;


/*
*
*
*   Unfinished activity, and package. Please do not touch for the time being.
*
*
*
*
* */



public class SensorCalibrationActivity extends AppCompatActivity {

    Handler timerHandler = new Handler();
    long startTime = 15;
    long collectTime = 60;
    TextView calibrationMessageTextView;
    TextView textPercent;
    DecoView arcView;
    int series1Index;
    Boolean collecting = false;
    public SensorCollector sensorCollector;

    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            if (!collecting) {
                collecting = true;
                sensorCollector.clearSensorData();
                sensorCollector.startScanning();
            }

            if (startTime > 0) {
                textPercent.setText(String.format("%02d", startTime));
                startTime--;
                timerHandler.postDelayed(this, 1000);
            } else {
                calibrationMessageTextView.setText(getString(R.string.calibrationMessageOne));
                timerHandler.removeCallbacks(timerRunnable);
                timerHandler.postDelayed(collectRunnable, 0);
            }
        }
    };

    Runnable collectRunnable = new Runnable() {
        @Override
        public void run() {

            if (collectTime > 0) {
                textPercent.setText(String.format("%02d", collectTime));
                collectTime--;
                timerHandler.postDelayed(this, 1000);
            } else {
                sensorCollector.stopScanning();
                calibrationMessageTextView.setText(getString(R.string.calibratonMessageTwo));
                timerHandler.removeCallbacks(collectRunnable);
                textPercent.setText("");
                arcView.deleteAll();
                arcView.setVisibility(View.GONE);

                startCalibrationAlgo();
            }
        }
    };

    public RealMatrix formatAccData() {
        double minTime = ((double) sensorCollector.accData.get(0).timestamp) / 1000000;
        double[][] matData = new double[sensorCollector.accData.size()][4];
        for (int i = 0; i < sensorCollector.accData.size(); i++) {
            SensorData3D accData = sensorCollector.accData.get(i);
            double time = (((double) accData.timestamp) / 1000000) - minTime;
            double[] el = {time, accData.x, accData.y, accData.z};
            matData[i] = el;
        }
        return MatrixUtils.createRealMatrix(matData);
    }

    public RealMatrix formatGyroData() {
        double minTime = ((double) sensorCollector.gyroData.get(0).timestamp) / 1000000;
        double[][] matData = new double[sensorCollector.gyroData.size()][4];
        for (int i = 0; i < sensorCollector.gyroData.size(); i++) {
            SensorData3D accData = sensorCollector.gyroData.get(i);
            double time = (((double) accData.timestamp) / 1000000) - minTime;
            double[] el = {time, accData.x, accData.y, accData.z};
            matData[i] = el;
        }
        return MatrixUtils.createRealMatrix(matData);
    }

    public void startCalibrationAlgo() {
//        RealMatrix IMU0x2Dalpha1 = parseMatrixFromFileContents("IMU0x2Dalpha.txt");
//        RealMatrix IMU0x2Domega1 = parseMatrixFromFileContents("IMU0x2Domega.txt");
//
//        RealMatrix IMU0x2Dalpha2 = parseMatrixFromFileContents("acc_data.txt");
//        RealMatrix IMU0x2Domega2 = parseMatrixFromFileContents("gyro_data.txt");

//        RealMatrix IMU0x2Dalpha2 = formatAccData();
//        RealMatrix IMU0x2Domega2 = formatGyroData();
//
//        Log.i("DIM", IMU0x2Dalpha2.getRowDimension() + ", " + IMU0x2Dalpha2.getColumnDimension());
//        Log.i("DIM", IMU0x2Domega2.getRowDimension() + ", " + IMU0x2Domega2.getColumnDimension());
//
//        int diff = IMU0x2Dalpha2.getRowDimension() - IMU0x2Domega2.getRowDimension();
//        if (diff > 0) {
//            IMU0x2Dalpha2 = subtractLastNRows(IMU0x2Dalpha2, diff);
//        } else if (diff < 0) {
//            IMU0x2Domega2 = subtractLastNRows(IMU0x2Domega2, abs(diff));
//        }
//
//        Log.i("DIM2", IMU0x2Dalpha2.getRowDimension() + ", " + IMU0x2Dalpha2.getColumnDimension());
//        Log.i("DIM2", IMU0x2Domega2.getRowDimension() + ", " + IMU0x2Domega2.getColumnDimension());
//
//        try {
//            calibrationManager = new CalibrationManager(IMU0x2Dalpha2, IMU0x2Domega2);
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to set calibration manager", e);
//        }

        try {
//            CalibrationReturnType calibrationData = calibrationManager.calibrate();

            // AB(a-C)
            // DE(g)

//            prettyPrintMatrix(calibrationData.getComp_a_misal(), "A");
//            prettyPrintMatrix(calibrationData.getComp_a_scale(), "B");
//            prettyPrintVector(calibrationData.getA_bias(), "C");
//            prettyPrintMatrix(calibrationData.getComp_g_misal(), "D");
//            prettyPrintMatrix(calibrationData.getComp_g_scale(), "E");

//            saveCalibrationData(calibrationData, this);

        } catch (Exception e) {
            Log.e("CRASH", "Failed to calibrate", e);
        }
    }

//    public void saveCalibrationData(CalibrationReturnType cal) {
//        SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
//
//        SharedPreferences.Editor prefsEditor = mPrefs.edit();
//        Gson gson = new Gson();
//        prefsEditor.putString("comp_a_scale", gson.toJson(cal.getComp_a_scale()));
//        prefsEditor.putString("comp_a_misal", gson.toJson(cal.getComp_a_misal()));
//        prefsEditor.putString("comp_g_scale", gson.toJson(cal.getComp_g_scale()));
//        prefsEditor.putString("comp_g_misal", gson.toJson(cal.getComp_g_misal()));
//        prefsEditor.commit();
//    }
//
//    public CalibrationReturnType loadCalibrationData() {
//        SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
//        Gson gson = new Gson();
//        RealMatrix comp_a_scale = gson.fromJson(mPrefs.getString("comp_a_scale", ""), RealMatrix.class);
//        RealMatrix comp_a_misal = gson.fromJson(mPrefs.getString("comp_a_misal", ""), RealMatrix.class);
//        RealMatrix comp_g_scale = gson.fromJson(mPrefs.getString("comp_g_scale", ""), RealMatrix.class);
//        RealMatrix comp_g_misal = gson.fromJson(mPrefs.getString("comp_g_misal", ""), RealMatrix.class);
//        return new CalibrationReturnType(comp_a_scale, comp_a_misal, comp_g_scale, comp_g_misal);
//    }

//    private void saveToken(String token) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
//        sharedPreferences.edit().putString("token", token).apply();
//    }
//
//    public String loadToken() {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
//        return sharedPreferences.getString("token", "");
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_calibration);

        sensorCollector = SensorCollector.getInstance();
        sensorCollector.init(SensorCalibrationActivity.this);
//        calibrationManager = new CalibrationManager();

        arcView = findViewById(R.id.dynamicArcView);

        // Create background track
        arcView.addSeries(new SeriesItem.Builder(Color.argb(255, 218, 218, 218))
                .setRange(0, startTime, startTime)
                .setInitialVisibility(false)
                .setLineWidth(32f)
                .build());

        // Create data series track
        SeriesItem seriesItem1 = new SeriesItem.Builder(getColor(R.color.turq))
                .setInitialVisibility(false)
                .setRange(0, startTime, startTime)
                .setLineWidth(32f)
                .setInterpolator(new LinearInterpolator())
                .build();

        textPercent = findViewById(R.id.textPercentage);
        textPercent.setText("");
        seriesItem1.addArcSeriesItemListener(new SeriesItem.SeriesItemListener() {
            @Override
            public void onSeriesItemAnimationProgress(float percentComplete, float currentPosition) {

            }

            @Override
            public void onSeriesItemDisplayProgress(float percentComplete) {

            }
        });

        series1Index = arcView.addSeries(seriesItem1);

//        startCalibrationAlgo();
    }

    public void startCalibration(View view) {

        calibrationMessageTextView = findViewById(R.id.calibrationMessage);

        calibrationMessageTextView.setText("Keep your device as steady as possible until the timer expires");

        Button calibrationButton = findViewById(R.id.calibrationButton);
        calibrationButton.setVisibility(View.GONE);

        timerHandler.postDelayed(timerRunnable, 1500);
        arcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true).setDelay(500).setDuration(1000).build());
        arcView.addEvent(new DecoEvent.Builder(0).setIndex(series1Index).setDelay(1500).setDuration(startTime*1000).build());
        arcView.addEvent(new DecoEvent.Builder(startTime).setIndex(series1Index).setDelay(1500+startTime*1000).setDuration(collectTime*1000).build());

        timerHandler.postDelayed(timerRunnable, 1500+startTime*1000+collectTime*1000);
    }

    public String loadFile(String nameOfLocalFile) throws IOException {
        InputStream is = this.getAssets().open(nameOfLocalFile);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }

    public RealMatrix parseMatrixFromFileContents(String fname) {
        try {
            ArrayList<double[]> contents = new ArrayList<>();
            String fileContents = loadFile(fname);
            List<String> fileContentsToLines = new ArrayList<>(Arrays.asList(fileContents.split("\n")));
            for (int i=0; i<fileContentsToLines.size(); i++) {
                List<String> lines = new ArrayList<>(Arrays.asList(fileContentsToLines.get(i).split(",")));
                double[] doubleNums = new double[lines.size()];
                for (int n=0; n<lines.size(); n++) {
                    doubleNums[n] = Double.parseDouble(lines.get(n));
                }
                contents.add(doubleNums);
            }
            double[][] res = new double[contents.size()][];
            for (int r=0; r<contents.size(); r++) {
                res[r] = contents.get(r);
            }
            return MatrixUtils.createRealMatrix(res);
        } catch (IOException e) {
            e.printStackTrace();
            return MatrixUtils.createRealMatrix(new double[][]{{}});
        }
    }

    // ---- Activity Lifecycle ---- //

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
