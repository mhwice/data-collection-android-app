package mcmaster.ilos.datacollectionapp.DataModel;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;

/* Responsible for scanning for Cellular data and recording it. Note: on some OS's the mTelephonyManager.getAllCellInfo()
 * is not available to the developer, and thus we cannot scan for the information that we need. */
public class GSMListener {

    private BufferedWriter cellBF = null;
    private TelephonyManager mTelephonyManager;
    private volatile static GSMListener gsmListener = null;
    private MapsActivity activity;
    private final int SCAN_FREQUENCY = 500;
    private final String FILENAME = "cellCheckpoint.txt";
    public int scanNum = 0;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            checkCellInfos();
            timerHandler.postDelayed(this, SCAN_FREQUENCY);
        }
    };

    private void createCheckpointFiles() {

        /* Creates the directory to hold the files */
        File dataFolder = new File(activity.getFilesDir(),"checkpoints/");
        try {
            if (!dataFolder.exists() && !dataFolder.mkdir()) {
                Log.e("CRASH", "Failed to make directory to hold checkpoint files");
            } else {
                String internalFolderPath = dataFolder.getAbsolutePath();

                File cellOutputFile = new File(internalFolderPath, FILENAME);
                FileWriter cellFileWriter = new FileWriter(cellOutputFile.getAbsolutePath(), true);
                cellBF = new BufferedWriter(cellFileWriter);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to create directory to hold checkpoint files", e);
        }
    }

    private  void closeCheckpointFiles() {
        try {
            if (cellBF != null) {
                cellBF.close();
                cellBF = null;
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
        File cellFile = getCheckpointFileWithName();
        if (cellFile != null) {
            if (!cellFile.delete()) {
                Log.e("CRASH", "Failed to delete cell checkpoint file");
            }
        }
    }

    private GSMListener() {

    }

    /* Returns current GSMManager instance if it exists, returns new GSMManager otherwise */
    public static GSMListener getInstance() {
        if (gsmListener == null) {
            /* Makes sure that no other thread is modifying this object */
            synchronized (GSMListener.class) {
                if (gsmListener == null) {
                    gsmListener = new GSMListener();
                }
            }
        }
        return gsmListener;
    }

    public void init(final MapsActivity activity) {
        this.activity = activity;
        this.mTelephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void startListening() {
        if (cellBF == null) {
            createCheckpointFiles();
        }

        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void stopListening() {
        timerHandler.removeCallbacks(timerRunnable);
        closeCheckpointFiles();
    }

    public void clearCellData() {
        scanNum = 0;
    }

    private void checkCellInfos() {

        if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<CellInfo> infos = mTelephonyManager.getAllCellInfo();

        if (infos == null || infos.size() == 0) {
            return;
        }

        scanNum+=1;

        for (CellInfo i : infos) {

            int sigDbm;
            String idString;

            if (i instanceof CellInfoGsm) {
                sigDbm = ((CellInfoGsm) i).getCellSignalStrength().getDbm();
                int cid = ((CellInfoGsm) i).getCellIdentity().getCid();
                int lac = ((CellInfoGsm) i).getCellIdentity().getLac();
                int mcc = ((CellInfoGsm) i).getCellIdentity().getMcc();
                int mnc = ((CellInfoGsm) i).getCellIdentity().getMnc();
                idString = mcc + "," +  mnc + "," + lac + "," + cid;

                try {
                    String data = SystemClock.elapsedRealtimeNanos() + "," + sigDbm + "," + "GSM" + "," + idString;
                    if (cellBF != null) {
                        cellBF.write(data);
                        cellBF.newLine();
                    }
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to write cell data", e);
                }
            } else if (i instanceof CellInfoCdma) {
                sigDbm = ((CellInfoCdma) i).getCellSignalStrength().getDbm();
                int cid = ((CellInfoCdma) i).getCellIdentity().getBasestationId();
                int nid = ((CellInfoCdma) i).getCellIdentity().getNetworkId();
                int sid = ((CellInfoCdma) i).getCellIdentity().getSystemId();
                idString = sid + "," + nid + "," + cid;

                try {
                    String data = SystemClock.elapsedRealtimeNanos() + "," + sigDbm + "," + "CDMA" + "," + idString;
                    if (cellBF != null) {
                        cellBF.write(data);
                        cellBF.newLine();
                    }
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to write cell data", e);
                }
            } else if (i instanceof CellInfoLte) {
                sigDbm = ((CellInfoLte) i).getCellSignalStrength().getDbm();
                int cid = ((CellInfoLte) i).getCellIdentity().getCi();
                int lac = ((CellInfoLte) i).getCellIdentity().getTac();
                int mcc = ((CellInfoLte) i).getCellIdentity().getMcc();
                int mnc = ((CellInfoLte) i).getCellIdentity().getMnc();
                idString = mcc + "," +  mnc + "," + lac + "," + cid;

                try {
                    String data = SystemClock.elapsedRealtimeNanos() + "," + sigDbm + "," + "LTE" + "," + idString;
                    if (cellBF != null) {
                        cellBF.write(data);
                        cellBF.newLine();
                    }
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to write cell data", e);
                }
            } else if (i instanceof CellInfoWcdma) {
                sigDbm = ((CellInfoWcdma) i).getCellSignalStrength().getDbm();
                int cid = ((CellInfoWcdma) i).getCellIdentity().getCid();
                int lac = ((CellInfoWcdma) i).getCellIdentity().getLac();
                int mcc = ((CellInfoWcdma) i).getCellIdentity().getMcc();
                int mnc = ((CellInfoWcdma) i).getCellIdentity().getMnc();
                idString = mcc + "," +  mnc + "," + lac + "," + cid;

                try {
                    String data = SystemClock.elapsedRealtimeNanos() + "," + sigDbm + "," + "WCDMA" + "," + idString;
                    if (cellBF != null) {
                        cellBF.write(data);
                        cellBF.newLine();
                    }
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to write cell data", e);
                }
            } else {
                Log.i("Location", "Base Station(CellInfoGsm) Info Nearby: UNKNOWN" + "\n");
            }
        }
    }

}
