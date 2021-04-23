package mcmaster.ilos.datacollectionapp.StepCounter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;


public class FancyStepCounter implements SensorEventListener {

    private float[] oriValues = new float[3];

    // Whether is increasing
    private boolean isUp = false;

    // The count of continuous up
    private int continueUpCount = 0;
    private int lastContinueUpCount = 0;
    private float peakValue = 0;
    private float valleyValue = 0;

    // The timestamp of this peak
    private long timeOfThisPeak = 0;

    // The timestamp of last peak
    private long timeOfLastPeak = 0;
    private float lastSensorValue = 0;
    private float lastFilteredSensorValue = 0;

    // Dynamic peak-valley value threshold, init value is 2.0
    // Private float thresholdValue = (float) 2.0;
    private float thresholdValue = (float) 0.6;    // todo

    // Second threshold, the peak value will be compared with the above threshold only when it is
    // Greater than this threshold
    private final float judgeValue = (float) 1.5;
    private final int arrayNum = 4;
    private float[] thresholdArray = new float[arrayNum];
    private int arrayCount = 0;

    // Step counts for meaningless steps
    private int stepCount = 0;

    // Total step counts
    private int totalCount = 0;

    // Callbacks
    private StepEventListener stepListener;

    public FancyStepCounter() {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        float alpha = 0.75f;   // todo

        // event.values is a float[3] containing the accelerometer values
        System.arraycopy(event.values, 0, oriValues, 0, 3);

        float sensorValue = (float) Math.sqrt(oriValues[0] * oriValues[0] + oriValues[1] * oriValues[1] + oriValues[2] * oriValues[2]);

        if (lastFilteredSensorValue == 0) {
            lastFilteredSensorValue = sensorValue;
        } else {
            // native low pass filter
            sensorValue = alpha * lastFilteredSensorValue + (1 - alpha) * sensorValue;
            lastFilteredSensorValue = sensorValue;
        }

        detectNewStep(sensorValue);
    }

    /*
     * Start to detect step
     * 1. feed with acc data
     * 2. If a peak is detected, the time inverval and peak-valley conditions are satisified, then a step
     * 3. If the time interval condition is satisified, then update the threshold
     */
    private void detectNewStep(float sensorValue) {
        if (lastSensorValue == 0) { // initial
            lastSensorValue = sensorValue;
        } else {
            if (detectPeak(sensorValue, lastSensorValue)) { //if a peak is detected
                timeOfLastPeak = timeOfThisPeak;
                long timeOfNow = System.currentTimeMillis();

                if (timeOfNow - timeOfLastPeak >= 300 && (peakValue - valleyValue >= thresholdValue)) {
                    timeOfThisPeak = timeOfNow;

                    if (stepListener != null) {
                        totalCount++;
                        stepListener.onStepEvent(totalCount, SystemClock.elapsedRealtimeNanos());
                    }
                }

                if (timeOfNow - timeOfLastPeak >= 300 //original 250
                        && (peakValue - valleyValue >= judgeValue)) {
                    timeOfThisPeak = timeOfNow;
                    thresholdValue = calculateThreshold(peakValue - valleyValue);
                }
            }
            lastSensorValue = sensorValue;
        }
    }

    /*
     * Peak detection
     * Four conditions:
     * 1.isDirectionUP = false
     * 2.lastStatus = true
     * 3.continuUpCount >= 2
     * 4.peak value >= 20
     */
    private boolean detectPeak(float newValue, float oldValue) {
        boolean lastIsUp = isUp;
        if (newValue >= oldValue) { // up
            isUp = true;
            continueUpCount++;
        } else { // down
            lastContinueUpCount = continueUpCount;
            isUp = false;
            continueUpCount = 0;
        }

        //prevent FP，continuous up or a significant up
        if (!isUp && lastIsUp && (lastContinueUpCount >= 2 || oldValue >= 20)) {
            peakValue = oldValue;
            return true;
        } else if (!lastIsUp && isUp) {
            valleyValue = oldValue;
            return false;
        } else {
            return false;
        }
    }

    /*
     * 1.Start when step count >= 5
     */
    private int detectValidStep() {
        boolean valid = false;
        if (timeOfThisPeak - timeOfLastPeak < 3000) {
            stepCount++;
            if (stepCount > 5) return 1;
            else if (stepCount == 5) return 5;
            else return 0;
        } else {
            stepCount = 0;
            return 0;
        }
    }

    /*
     * Threshold calibration
     * 1.Adjust the threshold based on legel peak-valley difference
     * 2.Based on past four values
     */
    private float calculateThreshold(float value) {
        if (arrayCount < arrayNum) {
            thresholdArray[arrayCount++] = value;
            return thresholdValue;
        } else {
            float newThreshold;
            newThreshold = averageValue(thresholdArray, arrayNum);
            // simulate a queue
            System.arraycopy(thresholdArray, 1, thresholdArray, 0, arrayNum - 1);
            thresholdArray[arrayNum - 1] = value;
            return newThreshold;
        }
    }

    /*
     * gradient the threshold
     * 1.average the threshold
     * 2.set the threshold into a range, according to the average threshold
     */
    private float averageValue(float[] value, int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / n;

        if (ave >= 8)
            ave = (float) 4.3;
        else if (ave >= 7 && ave < 8)
            ave = (float) 3.3;
        else if (ave >= 4 && ave < 7)
            ave = (float) 2.3;
        else if (ave >= 3 && ave < 4)
            ave = (float) 2.0;
        else {
            ave = (float) 1.3;
        }
        return ave;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setStepListener(StepEventListener stepListener) {
        this.stepListener = stepListener;
    }

    public void clearStepCounter() {
        totalCount = 0;
    }
}
