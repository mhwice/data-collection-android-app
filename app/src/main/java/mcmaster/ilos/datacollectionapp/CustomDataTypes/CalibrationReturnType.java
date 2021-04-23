package mcmaster.ilos.datacollectionapp.CustomDataTypes;

import org.apache.commons.math4.linear.RealMatrix;
import org.apache.commons.math4.linear.RealVector;

/* Used to return a series of matricies from the calibration algorithm outlined in the paper
 * "A Robust and Easy to Implement Method for IMU Calibration without External Equipments" by
 * David Tedaldi, Alberto Pretto, and Emanuele Menegatti */
public class CalibrationReturnType {

    private RealMatrix comp_a_scale;
    private RealMatrix comp_a_misal;
    private RealMatrix comp_g_scale;
    private RealMatrix comp_g_misal;
    private RealVector a_bias;

    public CalibrationReturnType(RealMatrix comp_a_scale, RealMatrix comp_a_misal, RealVector a_bias, RealMatrix comp_g_scale, RealMatrix comp_g_misal) {
        this.comp_a_scale = comp_a_scale;
        this.comp_a_misal = comp_a_misal;
        this.comp_g_scale = comp_g_scale;
        this.comp_g_misal = comp_g_misal;
        this.a_bias = a_bias;
    }

    public RealMatrix getComp_a_misal() {
        return comp_a_misal;
    }

    public RealMatrix getComp_a_scale() {
        return comp_a_scale;
    }

    public RealMatrix getComp_g_misal() {
        return comp_g_misal;
    }

    public RealMatrix getComp_g_scale() {
        return comp_g_scale;
    }

    public RealVector getA_bias() {
        return a_bias;
    }
}