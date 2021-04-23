package mcmaster.ilos.datacollectionapp.Utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.BTItem;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.CellItem;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.GPSItem;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.MapMarker;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.RSSItem;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SaveFile;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData1D;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SensorData3D;
import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;
import mcmaster.ilos.datacollectionapp.protobuf.ProtoData;

import static mcmaster.ilos.datacollectionapp.BuildingsScreen.BuildingManager.removeFileRecord;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.formatSize;

/* Responsible for packing data into a PBF file */
public class ProtobufManager {

    private MapsActivity activity;

    public ProtobufManager(MapsActivity activity) {
        this.activity = activity;
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

    public ProtoData.DataPack packDataFromCheckpoints(int STATE, String buildingName, int realStepNum, ArrayList<Long> stepTimestamps, int SENSOR_STATE) throws Exception {
        ProtoData.DataPack.Builder packBuilder  = ProtoData.DataPack.newBuilder();

        packBuilder.setState(STATE);
        packBuilder.setDeviceInfo(Build.MANUFACTURER + "," + Build.MODEL + "," + Build.VERSION.SDK_INT + "," + Build.BRAND);
        packBuilder.setBuildingName(buildingName);
        packBuilder.setSensorState(SENSOR_STATE);
        packBuilder.setSensorFrequency(frequencyFlagToFrequency(activity.sensorDataManager.SCAN_FREQUENCY_FLAG));

        PackageManager manager = activity.getPackageManager();
        boolean hasBarometer = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);

		if (activity.markerList.size() < 1) {
			throw new Exception("No markers found");
		}

        if (hasBarometer) {
            for (MapMarker marker : activity.markerList) {
                packBuilder.addMarkers(ProtoData.MarkerEvent.newBuilder()
                        .setCoordinate(ProtoData.Coordinate.newBuilder()
                                .setLatitude(marker.getCircle().getLatLng().getLatitude())
                                .setLongitude(marker.getCircle().getLatLng().getLongitude())
                                .setAltitude(marker.getAltitude()))
                        .setFloorNumber(marker.getFloor())
                        .setTimestamp(marker.getTimestamp()));
            }
        } else {
            for (MapMarker marker : activity.markerList) {
                packBuilder.addMarkers(ProtoData.MarkerEvent.newBuilder()
                        .setCoordinate(ProtoData.Coordinate.newBuilder()
                                .setLatitude(marker.getCircle().getLatLng().getLatitude())
                                .setLongitude(marker.getCircle().getLatLng().getLongitude()))
                        .setFloorNumber(marker.getFloor())
                        .setTimestamp(marker.getTimestamp()));
            }
        }

        /* Wifi Data */
        if (SENSOR_STATE == 0 || SENSOR_STATE == 1) {
            try {

            	List<RSSItem> rssItems = parseWifiCheckpointFile();

				if (rssItems.size() < 1) {
					throw new Exception("No Wifi scans found");
				}

                for (RSSItem rssItem : rssItems) {
                    packBuilder.addRssItems(ProtoData.RSSItem.newBuilder()
                            .setScanNum(rssItem.scanNum)
                            .setTimestamp(rssItem.timeStamp)
                            .setBssid(rssItem.bssid)
                            .setSsid(rssItem.ssid)
                            .setRssi(rssItem.level)
                            .setFrequency(rssItem.frequency));
                }
            } catch (IOException e) {
                Log.e("CRASH", "Failed to parse wifi file", e);
            }
        }

        /* Bluetooth Data */
        if (SENSOR_STATE == 0 || SENSOR_STATE == 2) {
            try {

				List<BTItem> btItems = parseBTCheckpointFile();

				if (btItems.size() < 1) {
					throw new Exception("No Bluetooth scans found");
				}

				for (BTItem btItem : btItems) {
                    packBuilder.addBtItems(ProtoData.BTItem.newBuilder()
                            .setAddress(btItem.address)
                            .setRssi(btItem.rssi)
                            .setTimestamp(btItem.timeStamp));
                }
            } catch (IOException e) {
                Log.e("CRASH", "Failed to parse bt file", e);
            }
        }

        /* Cellular Data */
        try {

            for (CellItem cellSignal : parseCellCheckpointFile()) {
                packBuilder.addCellItems(ProtoData.CellItem.newBuilder()
                        .setRssi(cellSignal.rssi)
                        .setType(cellSignal.type)
                        .setId(cellSignal.id)
                        .setTimestamp(cellSignal.timeStamp));
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to parse cell file", e);
        }

        /* GPS Data */
        try {
            for (GPSItem gpsData : parseGPSCheckpointFile()) {
                packBuilder.addGpsItems(ProtoData.GPSItem.newBuilder()
                        .setTimestamp(gpsData.getTimestamp())
                        .setCount(gpsData.getCount()));
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to parse gps file", e);
        }

        /* Light Data */
        try {
            for (SensorData1D light: parse1DSensorDataWithFilename("lightCheckpoint.txt")) {
                packBuilder.addLight(ProtoData.SensorData1D.newBuilder()
                        .setValue(light.value)
                        .setTimestamp(light.timestamp));
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to parse light file", e);
        }

        /* Magnetic Data */
        try {
            for (SensorData3D mag: parse3DSensorDataWithFilename("magCheckpoint.txt")) {
                packBuilder.addMagnetic(ProtoData.SensorData3D.newBuilder()
                        .setX(mag.x)
                        .setY(mag.y)
                        .setZ(mag.z)
                        .setTimestamp(mag.timestamp));
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to parse mag file", e);
        }

        /* Gyroscope Data */
        try {
            for (SensorData3D gyro: parse3DSensorDataWithFilename("gyroCheckpoint.txt")) {
                packBuilder.addGyroscope(ProtoData.SensorData3D.newBuilder()
                        .setX(gyro.x)
                        .setY(gyro.y)
                        .setZ(gyro.z)
                        .setTimestamp(gyro.timestamp));
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to parse gyro file", e);
        }

        /* Accelerometer Data */
        try {
            for (SensorData3D acceleration: parse3DSensorDataWithFilename("accCheckpoint.txt")) {
                packBuilder.addAcceleration(ProtoData.SensorData3D.newBuilder()
                        .setX(acceleration.x)
                        .setY(acceleration.y)
                        .setZ(acceleration.z)
                        .setTimestamp(acceleration.timestamp));
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to parse acc file", e);
        }

        /* Linear Acceleration Data */
        try {
            for (SensorData3D linearAcc: parse3DSensorDataWithFilename("linearAccCheckpoint.txt")) {
                packBuilder.addLinearAcc(ProtoData.SensorData3D.newBuilder()
                        .setX(linearAcc.x)
                        .setY(linearAcc.y)
                        .setZ(linearAcc.z)
                        .setTimestamp(linearAcc.timestamp));
            }
        } catch (IOException e) {
            Log.e("CRASH", "Failed to parse linearAcc file", e);
        }

        /* StepEvents */
        for (long stepTime: stepTimestamps) {
            packBuilder.addStepEvents(stepTime);
        }

        /* Calibration Data */
        if (realStepNum != -1) {
            packBuilder.setRealStepNum(realStepNum);
        }

        return packBuilder.build();
    }

    private ArrayList<BTItem> parseBTCheckpointFile() throws IOException {
        ArrayList<BTItem> btData = new ArrayList<>();
        File btFile = getCheckpointFileWithName("btCheckpoint.txt");
        if (btFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(btFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(",");
                btData.add(new BTItem(Long.valueOf(elements[0]), elements[1], Integer.valueOf(elements[2])));
            }
            br.close();
        }
        return btData;
    }

    private static void removeAllSubList(List<?> list, List<?> subList) {
        int i = Collections.indexOfSubList(list, subList);
        if (i != -1) {
            list.subList(i, i + subList.size()).clear();
            removeAllSubList(list.subList(i, list.size()), subList);
        }
    }

    private ArrayList<RSSItem> parseWifiCheckpointFile() throws IOException {
        ArrayList<RSSItem> wifiData = new ArrayList<>();
        File wifiFile = getCheckpointFileWithName("wifiCheckpoint.txt");
        if (wifiFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(wifiFile));
            String line;

            while ((line = br.readLine()) != null) {
                ArrayList<String> elements = new ArrayList<>(Arrays.asList(line.split(",")));

                // this means that the name has comma's
                if (elements.size() > 6) {

					Log.i("RANGE", elements.toString());

                    // extract all of the strings which make up the bssid
                    List<String> bssidArray = elements.subList(3, elements.size()-2);

                    // merge
                    String bssid = TextUtils.join(",", bssidArray);

                    // replace back in
					List<Integer> range = new ArrayList<>();
					for (int i = 3; i <= elements.size()-3; i++) {
						range.add(i);
					}

					// Doesn't work with Android OS 23
//                    List<Integer> range = IntStream.rangeClosed(3, elements.size()-3).boxed().collect(Collectors.toList());

                    Collections.sort(range, Collections.reverseOrder());
                    for (int i : range) {
                        elements.remove(i);
                    }
                    elements.add(3, bssid);
                }

                wifiData.add(new RSSItem(Integer.valueOf(elements.get(0)), Long.valueOf(elements.get(1)), elements.get(2), elements.get(3), Integer.valueOf(elements.get(4)), Integer.valueOf(elements.get(5))));
            }
            br.close();
        }
        return wifiData;
    }

    private ArrayList<CellItem> parseCellCheckpointFile() throws IOException {
        ArrayList<CellItem> cellData = new ArrayList<>();
        File cellFile = getCheckpointFileWithName("cellCheckpoint.txt");
        if (cellFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(cellFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(",");
                cellData.add(new CellItem(Long.valueOf(elements[0]), Integer.valueOf(elements[1]), elements[2], elements[3]));
            }
            br.close();
        }
        return cellData;
    }

    private ArrayList<GPSItem> parseGPSCheckpointFile() throws IOException {
        ArrayList<GPSItem> gpsData = new ArrayList<>();
        File gpsFile = getCheckpointFileWithName("gpsCheckpoint.txt");
        if (gpsFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(gpsFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(",");
                gpsData.add(new GPSItem(Integer.valueOf(elements[0]), Long.valueOf(elements[1])));
            }
            br.close();
        }
        return gpsData;
    }

    private ArrayList<SensorData1D> parse1DSensorDataWithFilename(String filename) throws IOException {
        ArrayList<SensorData1D> oneData = new ArrayList<>();
        File oneFile = getCheckpointFileWithName(filename);
        if (oneFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(oneFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(",");
                oneData.add(new SensorData1D(Float.valueOf(elements[0]), Long.valueOf(elements[1])));
            }
            br.close();
        }
        return oneData;
    }

    private ArrayList<SensorData3D> parse3DSensorDataWithFilename(String filename) throws IOException {
        ArrayList<SensorData3D> threeData = new ArrayList<>();
        File threeFile = getCheckpointFileWithName(filename);
        if (threeFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(threeFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(",");
                threeData.add(new SensorData3D(Float.valueOf(elements[0]), Float.valueOf(elements[1]), Float.valueOf(elements[2]), Long.valueOf(elements[3])));
            }
            br.close();
        }
        return threeData;
    }

//    public ProtoData.DataPack packData(int STATE, String buildingName, int realStepNum, ArrayList<Long> stepTimestamps, int SENSOR_STATE) {
//
//        ProtoData.DataPack.Builder packBuilder  = ProtoData.DataPack.newBuilder();
//
//        packBuilder.setState(STATE);
//        packBuilder.setDeviceInfo(Build.MANUFACTURER + "," + Build.MODEL + "," + Build.VERSION.SDK_INT + "," + Build.BRAND);
//        packBuilder.setBuildingName(buildingName);
//        packBuilder.setSensorState(SENSOR_STATE);
//        packBuilder.setSensorFrequency(frequencyFlagToFrequency(activity.sensorDataManager.SCAN_FREQUENCY_FLAG));
//
//        PackageManager manager = activity.getPackageManager();
//        boolean hasBarometer = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
//
//        if (hasBarometer) {
//            for (MapMarker marker : activity.markerList) {
//                packBuilder.addMarkers(ProtoData.MarkerEvent.newBuilder()
//                        .setCoordinate(ProtoData.Coordinate.newBuilder()
//                                .setLatitude(marker.getCircle().getLatLng().getLatitude())
//                                .setLongitude(marker.getCircle().getLatLng().getLongitude())
//                                .setAltitude(marker.getAltitude()))
//                        .setFloorNumber(marker.getFloor())
//                        .setTimestamp(marker.getTimestamp()));
//            }
//        } else {
//            for (MapMarker marker : activity.markerList) {
//                packBuilder.addMarkers(ProtoData.MarkerEvent.newBuilder()
//                        .setCoordinate(ProtoData.Coordinate.newBuilder()
//                                .setLatitude(marker.getCircle().getLatLng().getLatitude())
//                                .setLongitude(marker.getCircle().getLatLng().getLongitude()))
//                        .setFloorNumber(marker.getFloor())
//                        .setTimestamp(marker.getTimestamp()));
//            }
//        }
//
//        /* Wifi Data */
//        if (SENSOR_STATE == 0 || SENSOR_STATE == 1) {
//            for (RSSItem rssItem : activity.wifiDataManager.rssItems) {
//                packBuilder.addRssItems(ProtoData.RSSItem.newBuilder()
//                        .setScanNum(rssItem.scanNum)
//                        .setTimestamp(rssItem.timeStamp)
//                        .setBssid(rssItem.bssid)
//                        .setSsid(rssItem.ssid)
//                        .setRssi(rssItem.level)
//                        .setFrequency(rssItem.frequency));
//            }
//        }
//
//        /* Bluetooth Data */
//        if (SENSOR_STATE == 0 || SENSOR_STATE == 2) {
//            for (BTItem btItem : activity.btManager.btItems) {
//                packBuilder.addBtItems(ProtoData.BTItem.newBuilder()
//                        .setAddress(btItem.address)
//                        .setRssi(btItem.rssi)
//                        .setTimestamp(btItem.timeStamp));
//            }
//        }
//
//        /* Cellular Data */
//        for (CellItem cellSignal : activity.gsmListener.cellSignals) {
//            packBuilder.addCellItems(ProtoData.CellItem.newBuilder()
//                    .setRssi(cellSignal.rssi)
//                    .setType(cellSignal.type)
//                    .setId(cellSignal.id)
//                    .setTimestamp(cellSignal.timeStamp));
//        }
//
//        /* GPS Data */
//        for (GPSItem gpsData : activity.gpsListener.gpsData) {
//            packBuilder.addGpsItems(ProtoData.GPSItem.newBuilder()
//                    .setTimestamp(gpsData.getTimestamp())
//                    .setCount(gpsData.getCount()));
//        }
//
//        /* Light Data */
//        for (SensorData1D light: activity.sensorDataManager.lightData) {
//            packBuilder.addLight(ProtoData.SensorData1D.newBuilder()
//                    .setValue(light.value)
//                    .setTimestamp(light.timestamp));
//        }
//
//        /* Magnetic Data */
//        for (SensorData3D mag: activity.sensorDataManager.magData) {
//            packBuilder.addMagnetic(ProtoData.SensorData3D.newBuilder()
//                    .setX(mag.x)
//                    .setY(mag.y)
//                    .setZ(mag.z)
//                    .setTimestamp(mag.timestamp));
//        }
//
//        /* Gyroscope Data */
//        for (SensorData3D gyro: activity.sensorDataManager.gyroData) {
//            packBuilder.addGyroscope(ProtoData.SensorData3D.newBuilder()
//                    .setX(gyro.x)
//                    .setY(gyro.y)
//                    .setZ(gyro.z)
//                    .setTimestamp(gyro.timestamp));
//        }
//
//        /* Accelerometer Data */
//        for (SensorData3D acceleration: activity.sensorDataManager.accData) {
//            packBuilder.addAcceleration(ProtoData.SensorData3D.newBuilder()
//                    .setX(acceleration.x)
//                    .setY(acceleration.y)
//                    .setZ(acceleration.z)
//                    .setTimestamp(acceleration.timestamp));
//        }
//
//        /* Linear Acceleration Data */
//        for (SensorData3D linearAcc: activity.sensorDataManager.linearAccData) {
//            packBuilder.addLinearAcc(ProtoData.SensorData3D.newBuilder()
//                    .setX(linearAcc.x)
//                    .setY(linearAcc.y)
//                    .setZ(linearAcc.z)
//                    .setTimestamp(linearAcc.timestamp));
//        }
//
//        /* StepEvents */
//        for (long stepTime: stepTimestamps) {
//            packBuilder.addStepEvents(stepTime);
//        }
//
//        /* Calibration Data */
//        if (realStepNum != -1) {
//            packBuilder.setRealStepNum(realStepNum);
//        }
//
//        return packBuilder.build();
//    }

    /* Given a flag, returns the sensor scan frequency associated with that flag */
    private static int frequencyFlagToFrequency(int FLAG) {

        // SENSOR_DELAY_NORMAL  -   200,000,000 nanoseconds    =   0.2 seconds     =   3
        // SENSOR_DELAY_UI      -   60,000,000  nanoseconds    =   0.06 seconds    =   2
        // SENSOR_DELAY_GAME    -   20,000,000  nanoseconds    =   0.02 seconds    =   1
        // SENSOR_DELAY_FASTEST -   0           nanoseconds    =   0.00 seconds    =   0

        int frequency;
        switch (FLAG) {
            case 0:
                frequency = 0;
                break;
            case 1:
                frequency = 20000000;
                break;
            case 2:
                frequency = 60000000;
                break;
            case 3:
                frequency = 200000000;
                break;
            default:
                frequency = 0;
        }
        return frequency;
    }

    /* Saves a PBF file to local storage */
    public String savePackToFile(ProtoData.DataPack packedData, String fileName) {

        String size = "";
        try {
            File dataFolder = new File(activity.getFilesDir(),"data/");
            try {
                if (!dataFolder.exists() && !dataFolder.mkdir()) {
                    Log.e("CRASH", "Failed to make directory to hold datapacks");
                    return size;
                }
            } catch (Exception e) {
                Log.e("CRASH", "Failed to create directory to hold datapacks", e);
                return size;
            }
            String internalFolderPath = dataFolder.getAbsolutePath();
            File outputFile = new File(internalFolderPath, fileName);
            MediaScannerConnection.scanFile(activity, new String[]{outputFile.getAbsolutePath()}, null, null);
            FileOutputStream output = new FileOutputStream(outputFile);
            packedData.writeTo(output);
            output.close();
            return formatSize(outputFile.length());
        } catch (Exception e) {
            Log.e("CRASH", "Unable to save datapack to file", e);
            return size;
        }
    }

    public void printFiles(Context context) {
        File dataFolder = new File(context.getFilesDir(),"data/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);
        for (File f : directory.listFiles()) {
            Log.i("FILE", f.toString());
        }
    }

    /* Returns all files currently in local storage */
    public static File[] getFiles(Context context) {
        File dataFolder = new File(context.getFilesDir(),"data/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);
        return directory.listFiles();
    }

    public static int getCheckpointFileCount(Context context) {
        File dataFolder = new File(context.getFilesDir(),"checkpoints/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);

        if (directory.exists() && directory.isDirectory()) {
            return directory.listFiles().length;
        } else {
            return 0;
        }
    }

    /* Through experimentation I found that you could theoretically collect 15.34Mb in 1Km */
    public static double getSizeOfCheckpointsDuringScan(Context context) {
        File dataFolder = new File(context.getFilesDir(),"checkpoints/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);

        double totalSize = 0.0;
        if (directory.exists() && directory.isDirectory()) {
            for (File f : directory.listFiles()) {
                totalSize+=f.length();
            }
            return totalSize;
        } else {
            return totalSize;
        }
    }

    /* Returns the file given a provided filename */
    public static File getFileWithName(String filename, Context context) {
        File[] files = getFiles(context);
        for (File f : files) {
            if (f.getName().equals(filename)) {
                return f;
            }
        }
        return null;
    }

    public void deleteAllCheckpointFiles(Context context) {
        File dataFolder = new File(context.getFilesDir(),"checkpoints/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);
        if (directory.exists() && directory.isDirectory()) {
            for (File checkpointFile : directory.listFiles()) {
                boolean deleted = checkpointFile.delete();
            }
        }
    }

    /* Deletes a file from storage given a filename */
    public static boolean deleteFileFromStorage(String filename, Context context) {
        File[] files = getFiles(context);
        for (File f : files) {
            if (f.getName().equals(filename)) {
                try {
                    boolean deleted = f.delete();
                    if (deleted) {
                        return true;
                    }
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to delete_marker the file", e);
                }
            }
        }
        return false;
    }

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            activity.uploadFinished();
        }
    };

    /* Uploads a PBF file to the server */
    public void uploadFile(String token, SaveFile saveFile, String errorCode) {

        final HashMap<String, String> map = new HashMap<>();

        map.put("buildingName", saveFile.getBuilding());
        map.put("floorLevel", saveFile.getFloor());
        map.put("otherFloorLevel", saveFile.getOtherFloor());
        map.put("fileName", saveFile.getFilename());
        map.put("errorCode",errorCode);
        map.put("Authorization", "Token " + token);

        String uploadURL = Config.loadProperties(activity).getProperty("FILE_UPLOAD_URL");

        new Thread() {

            @Override
            public void run() {

                long startTime = SystemClock.elapsedRealtimeNanos();

                File dataFolder = new File(activity.getFilesDir(),"data/");
                String filePath = dataFolder.getAbsolutePath() + "/" + saveFile.getFilename();
                FileUploader.upload(uploadURL, new File(filePath), map, saveFile, new FileUploader.FileUploadListener() {

                    @Override
                    public void onFail() {
                        activity.uploadFailed();
                    }

                    @Override
                    public void onProgress(long pro, double percent) {
                        Log.i("upload", "upload percent: " + percent);
                    }

                    @Override
                    public void onFinish(int code, String res, Map<String, List<String>> headers, SaveFile file) {

                        String status;
                        try {
                            JSONObject json = new JSONObject(res);
                            status = json.getString("status");
                        } catch (JSONException e) {
                            Log.e("CRASH", "Faile to parse json response", e);
                            return;
                        }

                        Log.i("STATUS", status);

                        if (!status.equals("1")) {

                            // Logging the error sent by the server
                            try {
                                JSONObject json = new JSONObject(res);
                                String message = json.getString("message");
                                Log.e("CRASH", "Message: " + message);
                            } catch (JSONException e) {
                                Log.e("CRASH", "Faile to parse json response", e);
                                return;
                            }

                            activity.uploadFailed();
                            return;
                        }

                        boolean fileDeletedFromStorage = deleteFileFromStorage(file.getFilename(), activity);
                        if (!fileDeletedFromStorage) {
                            activity.uploadFailed();
                            return;
                        }

                        boolean fileDeletedFromDatabase = removeFileRecord(token, file, activity);
                        if (!fileDeletedFromDatabase) {
                            activity.uploadFailed();
                            return;
                        }

                        long endTime = SystemClock.elapsedRealtimeNanos();
                        double uploadTime = (endTime - startTime) / 1e9;
                        long remaining = (long) ((1 - uploadTime) * 1000);
                        if (uploadTime < 1) {
                            timerHandler.postDelayed(timerRunnable, remaining);
                        } else {
                            activity.uploadFinished();
                        }

                        Log.i("upload", "return code: " + code);
                        Log.i("upload", "res: " + res);
                        Log.i("upload", "headers " + headers.toString());

//                        if (Integer.parseInt(errorCode) == 0) {
//                            activity.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(activity, "Data has been uploaded successfully!", Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        }

                        // Delete file after uploaded (except calibration file)
//                        if (!fileName.contains("Calibration")) {
//                            Log.i("upload", "Deleting: " + fileName);
//                            activity.deleteFile(fileName);
//                        }
                    }
                });
            }
        }.start();
    }
}