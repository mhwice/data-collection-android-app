package mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration;

import android.util.Log;

import org.apache.commons.math4.linear.LUDecomposition;
import org.apache.commons.math4.linear.MatrixUtils;
import org.apache.commons.math4.linear.RealMatrix;
import org.apache.commons.math4.linear.RealVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class CalibrationUtils {

    // 1ms
    public static List<RealMatrix> obtainComparableMatrix(RealMatrix acc_scale_matrix, RealMatrix acc_misal_matrix, RealMatrix gyro_scale_matrix, RealMatrix gyro_misal_matrix) {

        double alpha_xz_6 = 0.01;
        double alpha_xy_6 = -0.02;
        double alpha_yx_6 = 0.01;

        RealMatrix R_xz = getRPart(alpha_xz_6, new double[] {0,0,1});
        RealMatrix R_xy = getRPart(alpha_xy_6, new double[] {0,1,0});
        RealMatrix R_yx = getRPart(alpha_yx_6, new double[] {1,0,0});

        RealMatrix comp_a_scale_matrix = new LUDecomposition(acc_scale_matrix).getSolver().getInverse();
        RealMatrix comp_a_misal_matrix = new LUDecomposition(R_xz.multiply(R_xy).multiply(R_yx).multiply(acc_misal_matrix)).getSolver().getInverse();
        RealMatrix comp_g_scale_matrix = new LUDecomposition(gyro_scale_matrix).getSolver().getInverse();
        RealMatrix comp_g_misal_matrix = new LUDecomposition(R_xz.multiply(R_xy).multiply(R_yx).multiply(gyro_misal_matrix)).getSolver().getInverse();

        List<RealMatrix> matrices = new ArrayList<>();
        matrices.add(comp_a_scale_matrix);
        matrices.add(comp_a_misal_matrix);
        matrices.add(comp_g_scale_matrix);
        matrices.add(comp_g_misal_matrix);
        return matrices;
    }

    // 0ms
    public static RealMatrix getRPart(double alpha, double[] wdata) {
        double theta = -1 * alpha;
        RealVector w = MatrixUtils.createRealVector(wdata);

        double q0 = cos(theta/2);
        double[] qrest = w.mapMultiply(sin(theta/2)).toArray();
        RealVector q = MatrixUtils.createRealVector(new double[]{q0, qrest[0], qrest[1], qrest[2]});

        return fromQtoR(q);
    }

    // 0ms
    public static RealMatrix createOnesMatrix(int i, int j) {
        double[][] zMat = new double[i][j];
        for (int x=0; x < zMat.length; x++) {
            for (int y=0; y < zMat[x].length; y++) {
                zMat[x][y] = 1;
            }
        }
        return MatrixUtils.createRealMatrix(zMat);
    }

    // 0ms
    public static RealMatrix createZerosMatrix(int i, int j) {
        double[][] zMat = new double[i][j];
        for (int x=0; x < zMat.length; x++) {
            for (int y=0; y < zMat[x].length; y++) {
                zMat[x][y] = 0;
            }
        }
        return MatrixUtils.createRealMatrix(zMat);
    }

    // 0ms
    public static RealMatrix fromQtoR(RealVector q) {
        double r_11 = Math.pow(q.getEntry(0), 2) + Math.pow(q.getEntry(1), 2) - Math.pow(q.getEntry(2), 2) - Math.pow(q.getEntry(3), 2);
        double r_12 = 2 * (q.getEntry(1)*q.getEntry(2) - q.getEntry(0)*q.getEntry(3));
        double r_13 = 2 * (q.getEntry(1)*q.getEntry(3) + q.getEntry(0)*q.getEntry(2));
        double r_21 = 2 * (q.getEntry(1)*q.getEntry(2) + q.getEntry(0)*q.getEntry(3));
        double r_22 = Math.pow(q.getEntry(0), 2) - Math.pow(q.getEntry(1), 2) + Math.pow(q.getEntry(2), 2) - Math.pow(q.getEntry(3), 2);
        double r_23 = 2 * (q.getEntry(2)*q.getEntry(3) - q.getEntry(0)*q.getEntry(1));
        double r_31 = 2 * (q.getEntry(1)*q.getEntry(3) - q.getEntry(0)*q.getEntry(2));
        double r_32 = 2 * (q.getEntry(2)*q.getEntry(3) + q.getEntry(0)*q.getEntry(1));
        double r_33 = Math.pow(q.getEntry(0), 2) - Math.pow(q.getEntry(1), 2) - Math.pow(q.getEntry(2), 2) + Math.pow(q.getEntry(3), 2);
        double[][] R = {{r_11, r_12, r_13}, {r_21, r_22, r_23}, {r_31, r_32, r_33}};
        return MatrixUtils.createRealMatrix(R);
    }

    public static double mean(RealVector v) {
        double m = 0;
        for (double d : v.toArray()) {
            m += d;
        }
        return m / v.getDimension();
    }

    public static double max(RealVector v) {
        double m = 0;
        for (double d : v.toArray()) {
            if (d > m) {
                m = d;
            }
        }
        return m;
    }

    public static RealVector createZerosVector(int i) {
        double[] zVec = new double[i];
        Arrays.fill(zVec, 0);
        return MatrixUtils.createRealVector(zVec);
    }

    public static RealVector createOnesVector(int i) {
        double[] zVec = new double[i];
        Arrays.fill(zVec, 1);
        return MatrixUtils.createRealVector(zVec);
    }

    public static RealMatrix appendRow(RealMatrix m, double[] rowData) {

        double[][] mData = m.getData();
        double[][] newData = new double[m.getRowDimension()+1][m.getColumnDimension()];

        if (m.getRowDimension() >= 0) {
            System.arraycopy(mData, 0, newData, 0, m.getRowDimension());
        }

        newData[m.getRowDimension()] = rowData;

        return MatrixUtils.createRealMatrix(newData);
    }

    public static RealMatrix appendColumn(RealMatrix m, double[] colData) {
        return appendRow(m.transpose(), colData).transpose();
    }

    public static RealMatrix subtractLastNRows(RealMatrix m, int n) {
        double[][] mData = m.getData();
        double[][] newData = new double[m.getRowDimension()-n][m.getColumnDimension()];

        if (m.getRowDimension() >= 0) {
            System.arraycopy(mData, 0, newData, 0, m.getRowDimension()-n);
        }

        return MatrixUtils.createRealMatrix(newData);
    }

    public static void prettyPrintMatrix(RealMatrix m, String tag) {
        double[][] mData = m.getData();

        ArrayList<ArrayList<Double>> mat = new ArrayList<>();
        for (int i = 0; i < mData.length; i++) {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < mData[i].length; j++) {
                row.add(mData[i][j]);
            }
            Log.i(tag, row.toString());
            mat.add(row);
        }
    }

    public static void prettyPrintVector(RealVector m, String tag) {
        double[] mData = m.toArray();

        ArrayList<Double> row = new ArrayList<>();
        for (int j = 0; j < mData.length; j++) {
            row.add(mData[j]);
        }
        Log.i(tag, row.toString());
    }

    // 0ms
    public static FromOmegaToQReturnType fromOmegaToQ(RealMatrix omega, RealVector intervals) {

        int s = omega.getColumnDimension();

        double angularRotation = 0;
        RealVector direction = createZerosVector(3);
        RealVector q = createZerosVector(4);

        for (int i = 0; i < s; i++) {

            angularRotation = Math.pow(Math.pow(omega.getEntry(0,i), 2) + Math.pow(omega.getEntry(1,i), 2) + Math.pow(omega.getEntry(2,i), 2), 0.5) * intervals.getEntry(i);
            direction.setSubVector(0, omega.getColumnVector(i).mapMultiply((intervals.getEntry(i) / angularRotation)));
            RealVector dir = direction;

            RealVector qvals = MatrixUtils.createRealVector(new double[4]);
            qvals.setEntry(0,cos(angularRotation/2));
            RealVector q123 = dir.mapMultiply(sin(angularRotation/2));
            for (int qi = 0; qi < q123.getDimension(); qi++) {
                qvals.setEntry(qi+1,q123.getEntry(qi));
            }
            q.setSubVector(i,qvals);
        }

        return new FromOmegaToQReturnType(q,angularRotation,direction);
    }

    public static class FromOmegaToQReturnType {
        RealVector direction;
        double angularRotation;
        RealVector q;

        FromOmegaToQReturnType(RealVector q, double angularRotation, RealVector direction) {
            this.direction = direction;
            this.angularRotation = angularRotation;
            this.q = q;
        }
    }
}
