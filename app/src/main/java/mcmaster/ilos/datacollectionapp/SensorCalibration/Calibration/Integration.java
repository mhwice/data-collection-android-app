package mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration;

import org.apache.commons.math4.linear.LUDecomposition;
import org.apache.commons.math4.linear.MatrixUtils;
import org.apache.commons.math4.linear.RealMatrix;
import org.apache.commons.math4.linear.RealVector;

import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.fromOmegaToQ;
import static mcmaster.ilos.datacollectionapp.SensorCalibration.Calibration.CalibrationUtils.fromQtoR;

public class Integration {

    // 7ms
    public static RealMatrix rotationRK4(RealMatrix omega) {

        double dt = 0.01;

        RealVector omega_x = MatrixUtils.createRealVector(omega.getRow(0));
        RealVector omega_y = MatrixUtils.createRealVector(omega.getRow(1));
        RealVector omega_z = MatrixUtils.createRealVector(omega.getRow(2));

        int num_samples = omega_x.getDimension();

        RealMatrix param_one = MatrixUtils.createRealMatrix(new double[][]{{omega_x.getEntry(0)}, {omega_y.getEntry(0)}, {omega_z.getEntry(0)}});
        CalibrationUtils.FromOmegaToQReturnType fromOmegaToQReturnType = fromOmegaToQ(param_one, MatrixUtils.createRealVector(new double[] {0.01}));
        RealVector q_k = fromOmegaToQReturnType.q;
        RealVector q_next_k = MatrixUtils.createRealVector(new double[]{0, 0, 0, 0});

        for (int i = 0; i < (num_samples-1); i++) {
            RealVector q_i_1 = q_k;
            RealMatrix OMEGA_omega_t_k = MatrixUtils.createRealMatrix(new double[][]{{0, -omega_x.getEntry(i), -omega_y.getEntry(i), -omega_z.getEntry(i)},
                    {omega_x.getEntry(i), 0, omega_z.getEntry(i), -omega_y.getEntry(i)},
                    {omega_y.getEntry(i), -omega_z.getEntry(i), 0, omega_x.getEntry(i)},
                    {omega_z.getEntry(i), omega_y.getEntry(i), -omega_x.getEntry(i), 0}});

            RealVector k_1 = OMEGA_omega_t_k.operate(q_i_1).mapMultiply(0.5);

            RealVector q_i_2 = q_k.add(k_1.mapMultiply(0.5).mapMultiply(dt));
            RealMatrix OMEGA_omega_t_k_plus_half_dt = MatrixUtils.createRealMatrix(new double[][]{{0, -(omega_x.getEntry(i) + omega_x.getEntry(i+1))/2, -(omega_y.getEntry(i) + omega_y.getEntry(i+1))/2, -(omega_z.getEntry(i) + omega_z.getEntry(i+1))/2},
                    {(omega_x.getEntry(i) + omega_x.getEntry(i+1))/2, 0, (omega_z.getEntry(i) + omega_z.getEntry(i+1))/2, -(omega_y.getEntry(i) + omega_y.getEntry(i+1))/2},
                    {(omega_y.getEntry(i) + omega_y.getEntry(i+1))/2, -(omega_z.getEntry(i) + omega_z.getEntry(i+1))/2, 0, (omega_x.getEntry(i) + omega_x.getEntry(i+1))/2},
                    {(omega_z.getEntry(i) + omega_z.getEntry(i+1))/2, (omega_y.getEntry(i) + omega_y.getEntry(i+1))/2, -(omega_x.getEntry(i) + omega_x.getEntry(i+1))/2, 0}});
            RealVector k_2 = OMEGA_omega_t_k_plus_half_dt.operate(q_i_2).mapMultiply(0.5);

            RealVector q_i_3 = q_k.add(k_2.mapMultiply(0.5).mapMultiply(dt));
            OMEGA_omega_t_k_plus_half_dt = MatrixUtils.createRealMatrix(new double[][]{{0, -(omega_x.getEntry(i) + omega_x.getEntry(i+1))/2, -(omega_y.getEntry(i) + omega_y.getEntry(i+1))/2, -(omega_z.getEntry(i) + omega_z.getEntry(i+1))/2},
                    {(omega_x.getEntry(i) + omega_x.getEntry(i+1))/2, 0, (omega_z.getEntry(i) + omega_z.getEntry(i+1))/2, -(omega_y.getEntry(i) + omega_y.getEntry(i+1))/2},
                    {(omega_y.getEntry(i) + omega_y.getEntry(i+1))/2, -(omega_z.getEntry(i) + omega_z.getEntry(i+1))/2, 0, (omega_x.getEntry(i) + omega_x.getEntry(i+1))/2},
                    {(omega_z.getEntry(i) + omega_z.getEntry(i+1))/2, (omega_y.getEntry(i) + omega_y.getEntry(i+1))/2, -(omega_x.getEntry(i) + omega_x.getEntry(i+1))/2, 0}});
            RealVector k_3 = OMEGA_omega_t_k_plus_half_dt.operate(q_i_3).mapMultiply(0.5);

            RealVector q_i_4 = q_k.add(k_3.mapMultiply(1).mapMultiply(dt));
            RealMatrix OMEGA_omega_t_k_plus_dt = MatrixUtils.createRealMatrix(new double[][]{{0, -omega_x.getEntry(i+1), -omega_y.getEntry(i+1), -omega_z.getEntry(i+1)},
                    {omega_x.getEntry(i+1), 0, omega_z.getEntry(i+1), -omega_y.getEntry(i+1)},
                    {omega_y.getEntry(i+1), -omega_z.getEntry(i+1), 0, omega_x.getEntry(i+1)},
                    {omega_z.getEntry(i+1), omega_y.getEntry(i+1), -omega_x.getEntry(i+1), 0}});
            RealVector k_4 = OMEGA_omega_t_k_plus_dt.operate(q_i_4).mapMultiply(0.5);

            RealVector parenthesis = k_1.mapMultiply((1d/6d)).add(k_2.mapMultiply((1d/3d))).add(k_3.mapMultiply((1d/3d))).add(k_4.mapMultiply((1d/6d)));
            q_next_k = q_k.add(parenthesis.mapMultiply(dt));
            q_next_k = q_next_k.mapMultiply((1d/q_next_k.getNorm()));
            q_k = q_next_k;
        }

        RealMatrix R = new LUDecomposition(fromQtoR(q_next_k)).getSolver().getInverse();

        return R;
    }
}
