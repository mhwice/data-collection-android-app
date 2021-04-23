package mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.commons.math4.exception.DimensionMismatchException;
import org.apache.commons.math4.exception.NotStrictlyPositiveException;
import org.apache.commons.math4.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math4.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math4.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math4.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math4.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math4.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.linear.ArrayRealVector;
import org.apache.commons.math4.linear.MatrixUtils;
import org.apache.commons.math4.linear.RealMatrix;
import org.apache.commons.math4.linear.RealVector;
import org.apache.commons.math4.linear.SingularMatrixException;
import org.apache.commons.math4.stat.StatUtils;
import org.apache.commons.math4.stat.descriptive.moment.Variance;
import org.apache.commons.math4.util.Pair;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;

import java.util.Arrays;
import java.util.List;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.CalibrationReturnType;

import static java.lang.Math.floor;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.appendColumn;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.appendRow;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.createOnesMatrix;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.createOnesVector;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.createZerosMatrix;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.createZerosVector;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.mean;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.obtainComparableMatrix;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.Integration.rotationRK4;

/**
 * This library is used to perform calibration on the Accelerometer and Gyroscope following the
 * procedure outlined in https://www.researchgate.net/publication/273383944_A_robust_and_easy_to_implement_method_for_IMU_calibration_without_external_equipments
 *
 * Author of this library is Marshall Wice (in 2019)
 */

public class CalibrationManager {

    private RealMatrix selectedAccData;
    private RealMatrix QS_time_interval_calib_info_matrix;
    private RealVector omega_x;
    private RealVector omega_y;
    private RealVector omega_z;

    // Static time
    private int QS_TIME = 1;

    // Sampling frequency of sensors (both acc and gyro)
    private double SAMPLE_PERIOD = 0.02;
    
    // Number of samples per second
    private final double NUM_SAMPLES = QS_TIME / SAMPLE_PERIOD;

    // Offsets of the sensors
    private int OFFSET_ACC_X = 0; // 33123
    private int OFFSET_ACC_Y = 0; // 33276
    private int OFFSET_ACC_Z = 0; // 32360
    private int OFFSET_GYRO_X = 0; // 32768
    private int OFFSET_GYRO_Y = 0; // 32466
    private int OFFSET_GYRO_Z = 0; // 32485

    // Local gravity value
    private double LOCAL_GRAVITY = 9.81744;
    
    // Window size
    private int WINDOW = 51; //101;
    private final int HALF_WINDOW = (int) floor(WINDOW / 2.0);

    // Loops
    private final int MAX_TIMES_THE_VAR = 10;
    
    // Total Number of Samples
    private int TOTAL_SAMPLES;

    // Length of time IMU is initally staitc (in seconds)
    private int STATIC_PERIOD = 10;

    // Authors used 3000
    private final int STATIC_SAMPLES = (int) floor(STATIC_PERIOD / SAMPLE_PERIOD);

    // Acc and Gyro data
    private RealMatrix IMU0x2Dalpha;
    private RealMatrix IMU0x2Domega;


    /*

        I NEED TO MAKE A CHECK TO ENSURE THAT THE MATRICES HAVE THE CORRECT DIMENSIONS!

        STATIC SAMPLES MUST BE POSITIVE!

        MOST THINGS MUST BE

     */
    public CalibrationManager(RealMatrix accelerometerData, RealMatrix gyroscopeData) throws DimensionMismatchException {

        if (accelerometerData.getColumnDimension() != 4) {
            throw new DimensionMismatchException(accelerometerData.getColumnDimension(),4);
        }

        if (gyroscopeData.getColumnDimension() != 4) {
            throw new DimensionMismatchException(gyroscopeData.getColumnDimension(),4);
        }

        this.IMU0x2Dalpha = accelerometerData;
        this.IMU0x2Domega = gyroscopeData;
    }

    public CalibrationManager setWindowSize(int w) throws NotStrictlyPositiveException {

        if (w <= 0) {
            throw new NotStrictlyPositiveException(w);
        }

        this.WINDOW = w;
        return this;
    }

    public CalibrationManager setGravity(double gravity) throws NotStrictlyPositiveException {

        if (gravity <= 0) {
            throw new NotStrictlyPositiveException(gravity);
        }

        this.LOCAL_GRAVITY = gravity;
        return this;
    }

    public CalibrationManager setAccelerometerOffsets(int x, int y, int z) {
        this.OFFSET_ACC_X = x;
        this.OFFSET_ACC_Y = y;
        this.OFFSET_ACC_Z = z;
        return this;
    }

    public CalibrationManager setGyroscopeOffsets(int x, int y, int z) {
        this.OFFSET_GYRO_X = x;
        this.OFFSET_GYRO_Y = y;
        this.OFFSET_GYRO_Z = z;
        return this;
    }

    public CalibrationManager setSamplePeriod(double samplePeriod) throws NotStrictlyPositiveException {

        if (samplePeriod <= 0) {
            throw new NotStrictlyPositiveException(samplePeriod);
        }

        this.SAMPLE_PERIOD = samplePeriod;
        return this;
    }

    public CalibrationManager setStaticPeriod(int staticPeriod) throws NotStrictlyPositiveException {

        if (staticPeriod <= 0) {
            throw new NotStrictlyPositiveException(staticPeriod);
        }

        this.STATIC_PERIOD = staticPeriod;
        return this;
    }

    public CalibrationManager setQsTime(int qsTime) throws NotStrictlyPositiveException {

        if (qsTime <= 0) {
            throw new NotStrictlyPositiveException(qsTime);
        }

        this.QS_TIME = qsTime;
        return this;
    }

    public CalibrationReturnType calibrate() throws Exception {

        if (IMU0x2Dalpha.getData().length == 0 || IMU0x2Domega.getData().length == 0) {
            return null;
        }

        TOTAL_SAMPLES = IMU0x2Domega.getRowDimension();

        RealVector a_xp = IMU0x2Dalpha.getColumnVector(1).subtract(createOnesVector(TOTAL_SAMPLES).mapMultiply(OFFSET_ACC_X));
        RealVector a_yp = IMU0x2Dalpha.getColumnVector(2).subtract(createOnesVector(TOTAL_SAMPLES).mapMultiply(OFFSET_ACC_Y));
        RealVector a_zp = IMU0x2Dalpha.getColumnVector(3).subtract(createOnesVector(TOTAL_SAMPLES).mapMultiply(OFFSET_ACC_Z));

        omega_x = IMU0x2Domega.getColumnVector(1).subtract(createOnesVector(TOTAL_SAMPLES).mapMultiply(OFFSET_GYRO_X));
        omega_y = IMU0x2Domega.getColumnVector(2).subtract(createOnesVector(TOTAL_SAMPLES).mapMultiply(OFFSET_GYRO_Y));
        omega_z = IMU0x2Domega.getColumnVector(3).subtract(createOnesVector(TOTAL_SAMPLES).mapMultiply(OFFSET_GYRO_Z));

        /*

            Static State Statistical Filter

         */

        Variance v = new Variance();
        // set the last Math.pow(..., 0.5) to get the root, or leave as 1 for the papers implementation
        double var_3D = Math.pow(Math.pow(v.evaluate(a_xp.getSubVector(0,STATIC_SAMPLES).toArray()), 2) + Math.pow(v.evaluate(a_yp.getSubVector(0,STATIC_SAMPLES).toArray()), 2) + Math.pow(v.evaluate(a_zp.getSubVector(0,STATIC_SAMPLES).toArray()), 2), 1);

        /*

            ISSUE

            Variance during static inteval is too small to compare it to the data. It needs to be bigger!

         */


        Log.i("VAR_3D", "" + var_3D);

        RealVector normal_x = createZerosVector(TOTAL_SAMPLES);
        RealVector normal_y = createZerosVector(TOTAL_SAMPLES);
        RealVector normal_z = createZerosVector(TOTAL_SAMPLES);

        if (TOTAL_SAMPLES < 2 * WINDOW) {
            throw new Exception("Too few samples or too large a window");
        }

        for (int i = HALF_WINDOW; i < TOTAL_SAMPLES - (HALF_WINDOW + 1); i++) {
            normal_x.setEntry(i, v.evaluate(a_xp.getSubVector(i - HALF_WINDOW, 2*HALF_WINDOW+1).toArray()));
            normal_y.setEntry(i, v.evaluate(a_yp.getSubVector(i - HALF_WINDOW, 2*HALF_WINDOW+1).toArray()));
            normal_z.setEntry(i, v.evaluate(a_zp.getSubVector(i - HALF_WINDOW, 2*HALF_WINDOW+1).toArray()));
        }

        RealVector s_square = normal_x.ebeMultiply(normal_x).add(normal_y.ebeMultiply(normal_y)).add(normal_z.ebeMultiply(normal_z));
        RealVector s_filter = createZerosVector(TOTAL_SAMPLES);

        /*

            Cycle used to individuate the optimal threshold

         */

        RealMatrix signal = MatrixUtils.createRealMatrix(new double[][]{a_xp.toArray(), a_yp.toArray(), a_zp.toArray()});

        RealMatrix res_norm_vector = createZerosMatrix(11, MAX_TIMES_THE_VAR);
        for (int times_the_var = 0; times_the_var < MAX_TIMES_THE_VAR; times_the_var++) {

            for (int i = HALF_WINDOW-1; i < TOTAL_SAMPLES - (HALF_WINDOW + 1); i++) {
                if (s_square.getEntry(i) < (times_the_var+1) * var_3D) {
                    s_filter.setEntry(i, 1);
                }
            }

            int l = 0;
            RealMatrix QS_time_interval_info_matrix = createZerosMatrix(1,3);
            int samples = 0;
            int start = 0;

            int flag;
            if (s_filter.getEntry(0) == 0) {
                flag = 0;
            } else {
                flag = 1;
                start = 1;
            }

        /*

            Cycle to determine the QS_time_interval_info_matrix

         */

            for (int i = 0; i < s_filter.getDimension(); i++) {
                if (flag == 1 && s_filter.getEntry(i) == 1) {
                    samples++;
                } else if (flag == 1 && s_filter.getEntry(i) == 0) {
                    if (QS_time_interval_info_matrix.getRowDimension() == l) {
                        QS_time_interval_info_matrix = appendRow(QS_time_interval_info_matrix, new double[]{start, i-1, samples});
                    } else {
                        QS_time_interval_info_matrix.setRowVector(l, MatrixUtils.createRealVector(new double[]{start, i-1, samples}));
                    }
                    l++;
                    flag = 0;
                } else if (flag == 0 && s_filter.getEntry(i) == 1) {
                    start = i;
                    samples = 1;
                    flag = 1;
                }
            }

        /*

            Data selection - accelerometer

         */

            RealMatrix selected_data = createZerosMatrix(3,1);
            l = 0;

            for (int j = 0; j < QS_time_interval_info_matrix.getRowDimension(); j++) {
                if (QS_time_interval_info_matrix.getEntry(j,2) >= NUM_SAMPLES) {
                    int selection_step = (int) floor(QS_time_interval_info_matrix.getEntry(j,2) / NUM_SAMPLES);

                    for (int i = 0; i < (NUM_SAMPLES-1); i++) {
                        if (selected_data.getColumnDimension() == l) {
                            selected_data = appendColumn(selected_data, signal.getColumn((int) QS_time_interval_info_matrix.getEntry(j,0) + (i - 1 + 1) * selection_step));
                        } else {
                            selected_data.setColumnVector(l, signal.getColumnVector((int) QS_time_interval_info_matrix.getEntry(j,0) + (i - 1 + 1) * selection_step));
                        }

                        l++;
                    }

                    if (selected_data.getColumnDimension() == l) {
                        selected_data = appendColumn(selected_data, signal.getColumn((int) QS_time_interval_info_matrix.getEntry(j, 1)));
                    } else {
                        selected_data.setColumnVector(l, signal.getColumnVector((int) QS_time_interval_info_matrix.getEntry(j, 1)));
                    }
                    l++;
                }
            }

            /*

                Minimization

             */

            selectedAccData = selected_data;

//            Log.i("SELECT", selectedAccData.getRowDimension() + ", " + selectedAccData.getColumnDimension());

            RealVector theta_pr = createZerosVector(9);
            double[] gravity = new double[selectedAccData.getColumnDimension()];
            Arrays.fill(gravity, Math.pow(LOCAL_GRAVITY, 2));

            LeastSquaresProblem problem = new LeastSquaresBuilder()
                    .start(new double[]{0,0,0,1,1,1,0,0,0})
                    .model(costFunc)
                    .target(gravity)
                    .lazyEvaluation(false)
                    .maxEvaluations(150000)
                    .maxIterations(6000)
                    .build();

//            LeastSquaresOptimizer.Optimum optimum; // = new LevenbergMarquardtOptimizer().withParameterRelativeTolerance(1.0e-10).withCostRelativeTolerance(1.0e-10).optimize(problem);
//            try {
//                optimum = new LevenbergMarquardtOptimizer().withCostRelativeTolerance(1.0e-10).optimize(problem);
//
//            } catch (Exception e) {
//                throw new Exception(e);
//            }
            // Apache optimizer is approximately 6x faster than DDog optimizer


            FunctionNtoM accFunc = new FunctionAcc();
            UnconstrainedLeastSquares<DMatrixRMaj> accoptimizer = FactoryOptimization.levenbergMarquardt(null, true);
            accoptimizer.setFunction(accFunc,null);
            accoptimizer.initialize(new double[]{0,0,0,1,1,1,0,0,0},1.0e-10,1.0e-10);
            UtilOptimize.process(accoptimizer,6000);
            RealVector theta_pr_acc = MatrixUtils.createRealVector(accoptimizer.getParameters());

            Log.i("THETA_2", theta_pr_acc.toString());

            double[] res_norm_column = new double[11];
            for (int t = 0; t < theta_pr.getDimension(); t++) {
//                res_norm_column[t] =  optimum.getPoint().getEntry(t);
                res_norm_column[t] =  theta_pr_acc.getEntry(t);
            }

//            Log.i("OPT_RES", "" + optimum.getResiduals());
            Log.i("RESIDUALS", "" + problem.evaluate(MatrixUtils.createRealVector(accoptimizer.getParameters())).getResiduals());

//            RealVector res = optimum.getResiduals();
            RealVector res = problem.evaluate(MatrixUtils.createRealVector(accoptimizer.getParameters())).getResiduals();
            res_norm_column[9] = StatUtils.sum(res.ebeMultiply(res).toArray());
            res_norm_column[10] = (times_the_var+1) * var_3D;

            res_norm_vector.setColumnVector(times_the_var, MatrixUtils.createRealVector(res_norm_column));
        }

        RealVector vec = res_norm_vector.getRowVector(9);
        int z = vec.getMinIndex();
        double threshold_opt = res_norm_vector.getEntry(10,z);
        RealVector theta_pr_opt = res_norm_vector.getColumnVector(z).getSubVector(0,9);
        Log.i("theta_pr_opt", "" + theta_pr_opt.toString());
        RealMatrix estimated_misalignmentMatrix = MatrixUtils.createRealMatrix(new double[][]{{1, -theta_pr_opt.getEntry(0), theta_pr_opt.getEntry(1)}, {0, 1, -theta_pr_opt.getEntry(2)}, {0, 0, 1}});
        RealMatrix estimated_scalingMatrix = MatrixUtils.createRealDiagonalMatrix(new double[]{theta_pr_opt.getEntry(3), theta_pr_opt.getEntry(4), theta_pr_opt.getEntry(5)});
        RealVector estimated_biasVector = MatrixUtils.createRealVector(new double[]{theta_pr_opt.getEntry(6), theta_pr_opt.getEntry(7), theta_pr_opt.getEntry(8)});

        s_filter = createZerosVector(TOTAL_SAMPLES);

        for (int i = HALF_WINDOW-1; i < TOTAL_SAMPLES - (HALF_WINDOW + 1); i++) {
            if (s_square.getEntry(i) < threshold_opt) {
                s_filter.setEntry(i, 1);
            }
        }

        /*

            GYROSCOPE BIAS REMOVAL

         */

        /*

            QS filter for the first static region individuation

         */

        int init_long_qs_interval_start = 0;
        int init_long_qs_interval_end = 0;
        int flag_is_first_long_static_interval = 1;

//        for (int i = 0; i < TOTAL_SAMPLES; i++) {
//            if (s_filter.getEntry(i) == 0 && flag_is_first_long_static_interval == 1) {
//
//            } else if (s_filter.getEntry(i) == 1 && flag_is_first_long_static_interval == 1) {
//                init_long_qs_interval_start = i;
//                flag_is_first_long_static_interval = 2;
//            } else if (s_filter.getEntry(i) == 1 && flag_is_first_long_static_interval == 2) {
//
//            } else if (s_filter.getEntry(i) == 0 && flag_is_first_long_static_interval == 2) {
//                init_long_qs_interval_end = i;
//                break;
//            }
//        }

        for (int i = 0; i < TOTAL_SAMPLES; i++) {
            if (s_filter.getEntry(i) == 1 && flag_is_first_long_static_interval == 1) {
                init_long_qs_interval_start = i;
                flag_is_first_long_static_interval = 2;
            } else if (s_filter.getEntry(i) == 0 && flag_is_first_long_static_interval == 2) {
                init_long_qs_interval_end = i;
                break;
            }
        }

        double estimate_bias_x = mean(omega_x.getSubVector(init_long_qs_interval_start, init_long_qs_interval_end-init_long_qs_interval_start+1));
        double estimate_bias_y = mean(omega_y.getSubVector(init_long_qs_interval_start, init_long_qs_interval_end-init_long_qs_interval_start+1));
        double estimate_bias_z = mean(omega_z.getSubVector(init_long_qs_interval_start, init_long_qs_interval_end-init_long_qs_interval_start+1));

        omega_x = omega_x.mapSubtract(estimate_bias_x);
        omega_y = omega_y.mapSubtract(estimate_bias_y);
        omega_z = omega_z.mapSubtract(estimate_bias_z);

        /*

            GYROSCOPE MINIMIZATION

         */

        RealMatrix calib_acc = estimated_misalignmentMatrix
                .multiply(estimated_scalingMatrix)
                .multiply(signal)
                .subtract(MatrixUtils.createRealDiagonalMatrix(estimated_biasVector.toArray()).multiply(createOnesMatrix(3, a_xp.getDimension())));

        RealVector filter = s_filter;
        int l = 0;
        RealMatrix QS_time_interval_info_matrix = createZerosMatrix(6,1);

        int samples = 0;
        int start = 0;
        int flag;
        if (filter.getEntry(0) == 0) {
            flag = 0;
        } else {
            flag = 1;
            start = 1;
        }

//        for (int i = 0; i < filter.getDimension(); i++) {
//            if (flag == 0 && filter.getEntry(i) == 0) {
//
//            } else if (flag == 1 && filter.getEntry(i) == 1) {
//                samples++;
//            } else if (flag == 1 && filter.getEntry(i) == 0) {
//                if (QS_time_interval_info_matrix.getColumnDimension() == l) {
//                    QS_time_interval_info_matrix = appendColumn(QS_time_interval_info_matrix, new double[]{start, i-1, samples, 0, 0, 0});
//                } else {
//                    QS_time_interval_info_matrix.setColumnVector(l, MatrixUtils.createRealVector(new double[]{start, i-1, samples, 0, 0, 0}));
//                }
//                l++;
//                flag = 0;
//            } else if (flag == 0 && filter.getEntry(i) == 1) {
//                start = i;
//                samples = 1;
//                flag = 1;
//            }
//        }

        for (int i = 0; i < filter.getDimension(); i++) {
            if (flag == 1 && filter.getEntry(i) == 1) {
                samples++;
            } else if (flag == 1 && filter.getEntry(i) == 0) {
                if (QS_time_interval_info_matrix.getColumnDimension() == l) {
                    QS_time_interval_info_matrix = appendColumn(QS_time_interval_info_matrix, new double[]{start, i-1, samples, 0, 0, 0});
                } else {
                    QS_time_interval_info_matrix.setColumnVector(l, MatrixUtils.createRealVector(new double[]{start, i-1, samples, 0, 0, 0}));
                }
                l++;
                flag = 0;
            } else if (flag == 0 && filter.getEntry(i) == 1) {
                start = i;
                samples = 1;
                flag = 1;
            }
        }


        signal = MatrixUtils.createRealMatrix(new double[][]{calib_acc.getRowVector(0).toArray(), calib_acc.getRowVector(1).toArray(), calib_acc.getRowVector(2).toArray()});
        RealMatrix selected_data = createZerosMatrix(3,(int) NUM_SAMPLES);
        l = 0;

        for (int g = 0; g < QS_time_interval_info_matrix.getColumnDimension(); g++) {
            RealMatrix selected_acc_data = createZerosMatrix(3,1);
            double selection_step = floor(QS_time_interval_info_matrix.getEntry(2,g) / NUM_SAMPLES);

            for (int i = 0; i < (NUM_SAMPLES-1); i++) {
                if (selected_acc_data.getColumnDimension() == i) {
                    selected_acc_data = appendColumn(selected_acc_data, signal.getColumn((int) (QS_time_interval_info_matrix.getEntry(0,g) + i * selection_step)));
                } else  {
                    selected_acc_data.setColumnVector(i, signal.getColumnVector((int) (QS_time_interval_info_matrix.getEntry(0,g) + (i - 1 + 1) * selection_step)));
                }
            }

            selected_data.setColumnVector((int) (NUM_SAMPLES-1), signal.getColumnVector((int) QS_time_interval_info_matrix.getEntry(1, g)));

            for (int rowNum = 0; rowNum < selected_acc_data.getRowDimension(); rowNum++) {

                double rowMean = mean(selected_acc_data.getRowVector(rowNum));
                QS_time_interval_info_matrix.setEntry(rowNum+3, l, rowMean);
            }
            l++;
        }

        QS_time_interval_calib_info_matrix = QS_time_interval_info_matrix;

        for (int i = 0; i < QS_time_interval_calib_info_matrix.getColumnDimension(); i++) {
            QS_time_interval_calib_info_matrix.setEntry(0, i, QS_time_interval_calib_info_matrix.getEntry(0,i)+1);
            QS_time_interval_calib_info_matrix.setEntry(1, i, QS_time_interval_calib_info_matrix.getEntry(1,i)+1);
        }

        /*

            Minimizing LSQNONLIN

         */

        FunctionNtoM func = new FunctionGyro();
        UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);
        optimizer.setFunction(func,null);
//        optimizer.initialize(new double[]{0.9232,-1.1104,0.7791,1.171,0.9458,-0.4122,-2.5887,0.2421,1.4486},1.0e-6,1.0e-7);
        optimizer.initialize(new double[]{1,0,0,0,1,0,0,0,1},1.0e-6,1.0e-7);
        try {
            UtilOptimize.process(optimizer,1000);
        } catch (Exception e) {
            throw new Exception(e);
        }

        RealVector theta_pr_gyro = MatrixUtils.createRealVector(optimizer.getParameters());

        RealMatrix misal_matrix = MatrixUtils.createRealMatrix(new double[][]{{1, theta_pr_gyro.getEntry(1), theta_pr_gyro.getEntry(2)}, {theta_pr_gyro.getEntry(3), 1, theta_pr_gyro.getEntry(5)}, {theta_pr_gyro.getEntry(6), theta_pr_gyro.getEntry(7), 1}});
        RealMatrix scale_matrix = MatrixUtils.createRealMatrix(new double[][]{{theta_pr_gyro.getEntry(0), 0, 0}, {0, theta_pr_gyro.getEntry(4), 0}, {0, 0, theta_pr_gyro.getEntry(8)}});

        List<RealMatrix> compMatrices;
        try {
            compMatrices = obtainComparableMatrix(estimated_scalingMatrix, estimated_misalignmentMatrix, scale_matrix, misal_matrix);
        } catch (SingularMatrixException e) {
            throw new Exception("Singular matrix", e);
        } catch (Exception e) {
            throw new Exception(e);
        }

//        List<RealMatrix> compMatrices = obtainComparableMatrix(estimated_scalingMatrix, estimated_misalignmentMatrix, scale_matrix, misal_matrix);
        RealMatrix comp_a_scale = compMatrices.get(0);
        RealMatrix comp_a_misal = compMatrices.get(1);
        RealMatrix comp_g_scale = compMatrices.get(2);
        RealMatrix comp_g_misal = compMatrices.get(3);

        Log.i("INFO", "Accelerometer Estimated Scaling Matrix: " + comp_a_scale.toString());
        Log.i("INFO", "Accelerometer Estimated Misalignment Matrix: " + comp_a_misal.toString());
        Log.i("INFO", "Gyroscope Estimated Scaling Matrix: " + comp_g_scale.toString());
        Log.i("INFO", "Gyroscope Estimated Misalignment Matrix: " + comp_g_misal.toString());

        /*

            Need to return these values, which then need to be save in the phones memory.

         */

        return new CalibrationReturnType(comp_a_scale, comp_a_misal, estimated_biasVector, comp_g_scale, comp_g_misal);
    }

    private MultivariateJacobianFunction costFunc = new MultivariateJacobianFunction() {
        @Override
        public Pair<RealVector, RealMatrix> value(RealVector point) {

            double E1 = point.getEntry(0);
            double E2 = point.getEntry(1);
            double E3 = point.getEntry(2);
            double E4 = point.getEntry(3);
            double E5 = point.getEntry(4);
            double E6 = point.getEntry(5);
            double E7 = point.getEntry(6);
            double E8 = point.getEntry(7);
            double E9 = point.getEntry(8);

            RealVector value = new ArrayRealVector(selectedAccData.getColumnDimension());
            RealMatrix jacobian = new Array2DRowRealMatrix(selectedAccData.getColumnDimension(), 9);

            RealMatrix misalignmentMatrix = MatrixUtils.createRealMatrix(new double[][]{{1, -E1, E2}, {0, 1, -E3}, {0 ,0, 1}});
            RealMatrix scalingMatrix = MatrixUtils.createRealDiagonalMatrix(new double[]{E4, E5, E6});
            RealMatrix dMatrix = MatrixUtils.createRealDiagonalMatrix(new double[]{E7, E8, E9});
            RealMatrix oMatrix = createOnesMatrix(3,selectedAccData.getColumnDimension());
            RealMatrix a_bar = misalignmentMatrix.multiply(scalingMatrix).multiply(selectedAccData).subtract(dMatrix.multiply(oMatrix));

            for (int i = 0; i < selectedAccData.getColumnDimension(); ++i) {

                double modelI = (Math.pow(a_bar.getEntry(0,i),2)+Math.pow(a_bar.getEntry(1,i),2)+Math.pow(a_bar.getEntry(2,i),2));
                value.setEntry(i, modelI);

                double a1 = selectedAccData.getEntry(0,i);
                double a2 = selectedAccData.getEntry(1,i);
                double a3 = selectedAccData.getEntry(2,i);

                jacobian.setEntry(i, 0, 2*E5*a2*(E7 - E4*a1 + E1*E5*a2 - E2*E6*a3));
                jacobian.setEntry(i, 1, -2*E6*a3*(E7 - E4*a1 + E1*E5*a2 - E2*E6*a3));
                jacobian.setEntry(i, 2, 2*E6*a3*(E8 - E5*a2 + E3*E6*a3));
                jacobian.setEntry(i, 3, -2*a1*(E7 - E4*a1 + E1*E5*a2 - E2*E6*a3));
                jacobian.setEntry(i, 4, 2*E1*a2*(E7 - E4*a1 + E1*E5*a2 - E2*E6*a3) - 2*a2*(E8 - E5*a2 + E3*E6*a3));
                jacobian.setEntry(i, 5, 2*E3*a3*(E8 - E5*a2 + E3*E6*a3) - 2*E2*a3*(E7 - E4*a1 + E1*E5*a2 - E2*E6*a3) - 2*a3*(E9 - E6*a3));
                jacobian.setEntry(i, 6, 2*E7 - 2*E4*a1 + 2*E1*E5*a2 - 2*E2*E6*a3);
                jacobian.setEntry(i, 7, 2*E8 - 2*E5*a2 + 2*E3*E6*a3);
                jacobian.setEntry(i, 8, 2*E9 - 2*E6*a3);
            }

            return new Pair<>(value, jacobian);
        }
    };

    private class FunctionGyro implements FunctionNtoM {

        @Override
        public void process(double[] input, double[] output) {

            double E1 = input[0];
            double E2 = input[1];
            double E3 = input[2];
            double E4 = input[3];
            double E5 = input[4];
            double E6 = input[5];
            double E7 = input[6];
            double E8 = input[7];
            double E9 = input[8];

            RealMatrix omega_hat = MatrixUtils.createRealMatrix(new double[][]{omega_x.toArray(), omega_y.toArray(), omega_z.toArray()});
            RealMatrix misalignmentMatrix = MatrixUtils.createRealMatrix(new double[][]{{1, E2, E3}, {E4, 1, E6}, {E7, E8, 1}});
            RealMatrix scalingMatrix = MatrixUtils.createRealDiagonalMatrix(new double[]{E1, E5, E9});
            RealMatrix omega_bar = misalignmentMatrix.multiply(scalingMatrix).multiply(omega_hat);

            RealMatrix vector = createZerosMatrix(3,5);

            for (int pr = 0; pr < (QS_time_interval_calib_info_matrix.getColumnDimension()-1); pr++) {
                RealMatrix sub1 = QS_time_interval_calib_info_matrix.getSubMatrix(3,5,pr,pr);
                RealMatrix sub2 = QS_time_interval_calib_info_matrix.getSubMatrix(3,5,pr+1,pr+1);

                if (vector.getRowDimension() <= (pr*3)) {
                    for (int i = 0; i < 3; i++) {
                        double[] theRow = new double[]{sub1.getRow(i)[0], 0, 0, 0, sub2.getRow(i)[0]};
                        vector = appendRow(vector, theRow);
                    }
                } else {
                    vector.setSubMatrix(sub1.getData(), (pr*3), 0);
                    vector.setSubMatrix(sub2.getData(), (pr*3), 4);
                }

                int x_part_one = (int) (QS_time_interval_calib_info_matrix.getEntry(1,pr) + 1);
                int x_part_two = (int) (QS_time_interval_calib_info_matrix.getEntry(0,pr + 1) - 1);
                RealVector x_part = omega_bar.getRowVector(0).getSubVector(x_part_one-1, x_part_two-x_part_one+1);

                int y_part_one = (int) (QS_time_interval_calib_info_matrix.getEntry(1,pr) + 1);
                int y_part_two = (int) (QS_time_interval_calib_info_matrix.getEntry(0,pr + 1) - 1);
                RealVector y_part = omega_bar.getRowVector(1).getSubVector(y_part_one-1, y_part_two-y_part_one+1);

                int z_part_one = (int) (QS_time_interval_calib_info_matrix.getEntry(1,pr) + 1);
                int z_part_two = (int) (QS_time_interval_calib_info_matrix.getEntry(0,pr + 1) - 1);
                RealVector z_part = omega_bar.getRowVector(2).getSubVector(z_part_one-1, z_part_two-z_part_one+1);

                RealMatrix gyroUnbiasUncalibratedValues = MatrixUtils.createRealMatrix(new double[][]{x_part.toArray(), y_part.toArray(), z_part.toArray()});
                RealMatrix R = rotationRK4(gyroUnbiasUncalibratedValues);
                vector.setSubMatrix(R.getData(), (pr*3), 1);
            }

            int residual_size = vector.getRowDimension()/3;

            for (int i = 1; i <= residual_size; i++) {

                RealMatrix v1 = vector.getSubMatrix(3*i-3, 3*i-1, 4, 4);
                double v2 = Math.pow((Math.pow(vector.getEntry(3*i-3, 4),2) + Math.pow(vector.getEntry(3*i-2, 4),2) + Math.pow(vector.getEntry(i*3-1, 4),2)), 0.5);

                RealMatrix v3 = vector.getSubMatrix(3*i-3, 3*i-1, 1, 3);
                RealMatrix v4 = vector.getSubMatrix(3*i-3, 3*i-1, 0, 0);
                double v5 = Math.pow((Math.pow(vector.getEntry(3*i-3, 4),2) + Math.pow(vector.getEntry(3*i-2, 4),2) + Math.pow(vector.getEntry(3*i-1, 4),2)), 0.5);

                RealVector v = v1.scalarMultiply(1/v2).subtract(v3.multiply(v4).scalarMultiply(1/v5)).getColumnVector(0);
                output[i-1] = Math.pow((Math.pow(v.getEntry(0),2) + Math.pow(v.getEntry(1),2) + Math.pow(v.getEntry(2),2)), 0.5);
            }
        }

        @Override
        public int getNumOfInputsN() {
            return 9;
        }

        @Override
        public int getNumOfOutputsM() {
            return QS_time_interval_calib_info_matrix.getColumnDimension()-1;
        }
    }

    private class FunctionAcc implements FunctionNtoM {

        @Override
        public void process(double[] input, double[] output) {

            double E1 = input[0];
            double E2 = input[1];
            double E3 = input[2];
            double E4 = input[3];
            double E5 = input[4];
            double E6 = input[5];
            double E7 = input[6];
            double E8 = input[7];
            double E9 = input[8];

            RealMatrix misalignmentMatrix = MatrixUtils.createRealMatrix(new double[][]{{1, -E1, E2}, {0, 1, -E3}, {0 ,0, 1}});
            RealMatrix scalingMatrix = MatrixUtils.createRealDiagonalMatrix(new double[]{E4, E5, E6});
            RealMatrix dMatrix = MatrixUtils.createRealDiagonalMatrix(new double[]{E7, E8, E9});
            RealMatrix oMatrix = createOnesMatrix(3,selectedAccData.getColumnDimension());
            RealMatrix a_bar = misalignmentMatrix.multiply(scalingMatrix).multiply(selectedAccData).subtract(dMatrix.multiply(oMatrix));

            for (int i = 0; i < selectedAccData.getColumnDimension(); ++i) {
                output[i] = Math.pow(LOCAL_GRAVITY, 2) - (Math.pow(a_bar.getEntry(0,i),2)+Math.pow(a_bar.getEntry(1,i),2)+Math.pow(a_bar.getEntry(2,i),2));
            }
        }

        @Override
        public int getNumOfInputsN() {
            return 9;
        }

        @Override
        public int getNumOfOutputsM() {
            return selectedAccData.getColumnDimension();
        }
    }

    public static void saveCalibrationData(CalibrationReturnType cal, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        prefsEditor.putString("comp_a_scale", gson.toJson(cal.getComp_a_scale().getData()));
        prefsEditor.putString("comp_a_misal", gson.toJson(cal.getComp_a_misal().getData()));
        prefsEditor.putString("a_bias", gson.toJson(cal.getA_bias().toArray()));
        prefsEditor.putString("comp_g_scale", gson.toJson(cal.getComp_g_scale().getData()));
        prefsEditor.putString("comp_g_misal", gson.toJson(cal.getComp_g_misal().getData()));
        prefsEditor.commit();
    }

    public static CalibrationReturnType loadCalibrationData(Context context) {
        SharedPreferences mPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Gson gson = new Gson();

        RealMatrix comp_a_scale = MatrixUtils.createRealMatrix(gson.fromJson(mPrefs.getString("comp_a_scale", ""), double[][].class));
        RealMatrix comp_a_misal = MatrixUtils.createRealMatrix(gson.fromJson(mPrefs.getString("comp_a_misal", ""), double[][].class));
        RealVector a_bias = MatrixUtils.createRealVector(gson.fromJson(mPrefs.getString("a_bias", ""), double[].class));
        RealMatrix comp_g_scale = MatrixUtils.createRealMatrix(gson.fromJson(mPrefs.getString("comp_g_scale", ""), double[][].class));
        RealMatrix comp_g_misal = MatrixUtils.createRealMatrix(gson.fromJson(mPrefs.getString("comp_g_misal", ""), double[][].class));
        return new CalibrationReturnType(comp_a_scale, comp_a_misal, a_bias, comp_g_scale, comp_g_misal);
    }
}
