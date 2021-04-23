package mcmaster.ilos.datacollectionapp.BuildingsScreen;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.Building;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.Map;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.MapDownloadReturnType;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SaveFile;
import mcmaster.ilos.datacollectionapp.Utils.Config;

import static android.content.Context.MODE_PRIVATE;

/* Used to manage (save/read to local storage, download) building and map objects */
public class BuildingManager {

    private Context context;

    public BuildingManager(Context context) {
        this.context = context;
    }

    /* Saves map metadata to local storage */
    public boolean saveDownloadedMapRecord(String token, Map map) {
        boolean success = false;
        try (SQLiteDatabase sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null)) {
            sqLiteDatabase.execSQL(createMapsTable(token));
            sqLiteDatabase.execSQL(addMap(token, map));
            success = true;
        } catch (Exception e) {
            Log.e("CRASH", "Failed to save map", e);
        }
        return success;
    }

    public boolean isMapAlreadyDownloaded(String token, Map map) {
        Cursor c = null;
        SQLiteDatabase sqLiteDatabase = null;
        boolean exists = false;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createMapsTable(token));
            c = sqLiteDatabase.rawQuery(getMap(token, map), null);
            c.moveToFirst();
            if (c.getInt(0) == 1) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to load maps", e);
        } finally {
            if (c != null) {
                c.close();
            }
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
        return exists;
    }

    /* Save a file to local storage */
    public static boolean saveFileRecord(String token, SaveFile file, Context context) {
        boolean success = false;
        try (SQLiteDatabase sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null)) {
            sqLiteDatabase.execSQL(createFilesTable(token));
            sqLiteDatabase.execSQL(addFile(token, file));
            success = true;
        } catch (Exception e) {
            Log.e("CRASH", "Failed to save file record", e);
        }
        return success;
    }

    /* Get all files from local storage */
    public static ArrayList<SaveFile> getFileRecords(String token, Context context) {
        ArrayList<SaveFile> files = new ArrayList<>();
        Cursor c = null;
        SQLiteDatabase sqLiteDatabase = null;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createFilesTable(token));
            c = sqLiteDatabase.rawQuery(getFileList(token), null);
            int buildingIndex = c.getColumnIndex("building");
            int floorNumIndex = c.getColumnIndex("floor");
            int otherFloorNumIndex = c.getColumnIndex("otherfloor");
            int filenameIndex = c.getColumnIndex("filename");
            int sizeIndex = c.getColumnIndex("size");
            while (c.moveToNext()) {
                files.add(new SaveFile(c.getString(buildingIndex), c.getString(floorNumIndex), c.getString(otherFloorNumIndex), c.getString(filenameIndex), c.getString(sizeIndex)));
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to load files", e);
        } finally {
            if (c != null) {
                c.close();
            }
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
        return files;
    }

    /* Delete file from local storage */
    public static boolean removeFileRecord(String token, SaveFile file, Context context) {
        boolean success = false;
        try (SQLiteDatabase sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null)) {
            sqLiteDatabase.execSQL(createFilesTable(token));
            sqLiteDatabase.execSQL(removeFile(token, file));
            success = true;
        } catch (Exception e) {
            Log.e("CRASH", "Failed to remove file record", e);
        }
        return success;
    }

    /* Get a list of all unique building names from a list of maps */
    public ArrayList<String> bnameList(ArrayList<Map> maps) {
        ArrayList<String> bnames = new ArrayList<>();
        for (Map m : maps) {
            if (!bnames.contains(m.getName())) {
                bnames.add(m.getName());
            }
        }
        return bnames;
    }

    /* Delete map metadata from local storage */
    public boolean removeDownloadedMapRecord(String token, Map map) {
        boolean success = false;
        try (SQLiteDatabase sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null)) {
            sqLiteDatabase.execSQL(createMapsTable(token));
            sqLiteDatabase.execSQL(removeMap(token, map));
            success = true;
        } catch (Exception e) {
            Log.e("CRASH", "Failed to remove map", e);
        }
        return success;
    }

    /* Returns the file metadata from local storage given a filename */
    public static SaveFile getFileWithFilename(String token, String filename, Context context) {
        Log.i("FILE1", filename);
        ArrayList<SaveFile> files = getFileRecords(token, context);
        for (SaveFile f : files) {
            Log.i("FILE1", f.getFilename());
            if (f.getFilename().equals(filename)) {
                return f;
            }
        }
        return null;
    }


    /* Returns a list of all map metadata that have been downloaded and saved locally */
    public ArrayList<Map> getDownloadedMapList(String token) {
        ArrayList<Map> maps = new ArrayList<>();
        Cursor c = null;
        SQLiteDatabase sqLiteDatabase = null;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createMapsTable(token));
            c = sqLiteDatabase.rawQuery(getMapList(token), null);
            int nameIndex = c.getColumnIndex("name");
            int floorNumIndex = c.getColumnIndex("floor");
            int sizeIndex = c.getColumnIndex("size");
            while (c.moveToNext()) {
                Map map = new Map(c.getString(nameIndex), c.getString(floorNumIndex), c.getString(sizeIndex));
                maps.add(map);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to load maps", e);
        } finally {
            if (c != null) {
                c.close();
            }
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
        return maps;
    }

    /* SQLite command to create a building table */
    private static String createbuildingTable(String token) {
        return "CREATE TABLE IF NOT EXISTS buildings_" + token + " (name VARCHAR, address VARCHAR, description VARCHAR, numfloors INT, UNIQUE(name))";
    }

    /* SQLite command to create a files table */
    private static String createFilesTable(String token) {
        return "CREATE TABLE IF NOT EXISTS files_" + token + " (building VARCHAR, floor VARCHAR, otherfloor VARCHAR, filename VARCHAR, size VARCHAR, UNIQUE(filename))";
    }

    /* SQLite command to create a maps table */
    private static String createMapsTable(String token) {
        return "CREATE TABLE IF NOT EXISTS maps_" + token + " (name VARCHAR, floor VARCHAR, size VARCHAR, UNIQUE(name, floor))";
    }

    /* SQLite command to save a file */
    private static String addFile(String token, SaveFile file) {
        return "INSERT OR IGNORE INTO files_" + token + " (building, floor, otherfloor, filename, size) VALUES ('" + file.getBuilding() + "', '" + file.getFloor() + "', '" + file.getOtherFloor() + "', '" + file.getFilename() + "', '" + file.getSize() + "')";
    }

    /* SQLite command to remove a file */
    private static String removeFile(String token, SaveFile file) {
        return "DELETE FROM files_" + token + " WHERE building = '" + file.getBuilding() + "' AND floor = '" + file.getFloor() + "' AND otherfloor = '" + file.getOtherFloor() + "' AND filename = '" + file.getFilename() + "'";
    }

    /* SQLite command save a map */
    private static String addMap(String token, Map map) {
        return "INSERT OR IGNORE INTO maps_" + token + " (name, floor, size) VALUES ('" + map.getName() + "', '" + map.getFloorNum() + "', '" + map.getSize() + "')";
    }

    /* SQLite command to remove a map */
    private static String removeMap(String token, Map map) {
        return "DELETE FROM maps_" + token + " WHERE name = '" + map.getName() + "' AND floor = '" + map.getFloorNum() + "'";
    }

    private static String getMap(String token, Map map) {
        return "SELECT EXISTS ( SELECT 1 FROM maps_" + token + " WHERE name = '" + map.getName() + "' AND floor = '" + map.getFloorNum() + "' LIMIT 1 )";
    }

    private static String getMap2(String token, Map map) {
        return "SELECT FROM maps_" + token + " WHERE name = '" + map.getName() + "' AND floor = '" + map.getFloorNum() + "'";
    }

//    SELECT EXISTS(SELECT 1 FROM myTbl WHERE u_tag="tag");

    /* SQLite command to get all maps */
    private static String getMaps(String token, String buildingName) {
        return "SELECT * FROM maps_" + token + " WHERE name = '" + buildingName + "'";
    }

    /* SQLite command to save a building */
    private static String addBuilding(String token, Building building) {
        return "INSERT OR IGNORE INTO buildings_" + token + " (name, address, description, numfloors) VALUES ('" + building.getName() + "', '" + building.getAddress() + "', '" + building.getDescription() + "', '" + building.getNumFloors() +  "')";
    }

    /* SQLite command to remove a marker */
    private static String removeMarker(String tableName, LatLng point) {
        return "DELETE FROM " + tableName + " WHERE latitude = " + point.getLatitude() + " AND longitude = " + point.getLongitude();
    }

    /* SQLite command to update the marker attributes */
    private static String updateMarker(String tableName, Double oldLat, Double oldLng, Double newLat, Double newLng) {
        return "UPDATE " + tableName + " SET latitude = " + newLat + ", longitude = " + newLng + " WHERE latitude = " + oldLat + " AND longitude = " + oldLng;
    }

    /* SQLite command to get all buildings */
    private static String getbuildingsList(String token) {
        return "SELECT * FROM buildings_" + token;
    }

    /* SQLite command to get all maps */
    private static String getMapList(String token) {
        return "SELECT * FROM maps_" + token;
    }

    /* SQLite command to get all files */
    private static String getFileList(String token) {
        return "SELECT * FROM files_" + token;
    }

    /* For debugging purposes only! */
    public void printFiles() {
        File dataFolder = new File(context.getFilesDir(),"maps/");
        String internalFolderPath = dataFolder.getAbsolutePath();
        File directory = new File(internalFolderPath);
        File[] files = directory.listFiles();
        try{
            Log.i("Files", "Size: "+ files.length);
            for (File file : files) {
                Log.i("Files", "FileName:" + file.getName());
            }
        } catch (Exception e) {
            Log.i("Files", "No Files Yet");
        }
    }



    /* Callback interface for map downloading */
    public interface AsyncResponse {
        void processFinish(MapDownloadReturnType output);
    }

    /* Returns a list of all map metadata */
    public ArrayList<Map> getAllMaps(String token) {
        ArrayList<Map> allMaps = new ArrayList<>();
        ArrayList<Building> buildings = dummyDownloadBuildings(token);
        for (Building b : buildings) {
            allMaps.addAll(b.getUploadedMaps());
        }
        return allMaps;
    }

    /* DEBUG ONLY - simulates the downloading of buildings */
    private ArrayList<Building> dummyDownloadBuildings(String token) {

        String fakeResponse = "{\"buildings\":[{\"name\":\"ITB\",\"description\":\"ITB\",\"address\": \"Main Street\",\"num_floors\":4,\"uploaded_maps\":[{\"floor_num\":\"2\", \"size\":\"13.2Mb\"},{\"floor_num\":\"1\", \"size\":\"7.9Kb\"}]},{\"name\":\"ETB\",\"description\":\"Some Description\",\"address\":\"Some Address\",\"num_floors\":7,\"uploaded_maps\":[{\"floor_num\":\"1\", \"size\":\"1Gb\"}]}]}";

        ArrayList<Building> buildings = new ArrayList<>();
        DowloadBuildings task = new DowloadBuildings();
        String urlEndpoint = Config.loadProperties(context).getProperty("BUILDING_DOWNLOAD_URL");

        String result = null;
        try {
            result = task.execute(urlEndpoint, token).get();
            result = fakeResponse;
        } catch (InterruptedException | ExecutionException e) {
            Log.e("CRASH", "Interrupted while downloading buildings", e);
        } catch (Exception e) {
            Log.e("CRASH", "Failed to download buildings", e);
        }

        if (result != null) {
            try {
                JSONObject json = new JSONObject(result);
                JSONArray buildingsArray = json.getJSONArray("buildings");
                for (int i = 0; i < buildingsArray.length(); i++) {
                    JSONObject building = buildingsArray.getJSONObject(i);
                    String building_name = building.getString("name");
                    String building_description = building.getString("description");
                    String building_address = building.getString("address");
                    int building_num_floors = building.getInt("num_floors");
                    JSONArray uploadedMaps = building.getJSONArray("uploaded_maps");
                    ArrayList<Map> uploadedMapList = new ArrayList<>();
                    for (int j = 0; j < uploadedMaps.length(); j++) {
                        JSONObject uploadedMap = uploadedMaps.getJSONObject(j);
                        String floorNumber = uploadedMap.getString("floor_num");
                        String size = uploadedMap.getString("size");
                        uploadedMapList.add(new Map(building_name, floorNumber, size));
                    }
                    buildings.add(new Building(building_name, building_address, building_description, building_num_floors, uploadedMapList));
                }
            } catch (Exception e) {
                Log.e("CRASH", "Failed to parse downloaded buildings JSON response", e);
            }
        }
        return buildings;
    }

    /* Returns a list of map metadata that lives on the server */
    public ArrayList<Map> getServerMapList(String token) {
        ArrayList<Building> serverBuildingsList = downloadBuildings(token);
        for (Building b : serverBuildingsList) {
        }
        ArrayList<Map> serverMapList = new ArrayList<>();
        for (Building bld : serverBuildingsList) {
            serverMapList.addAll(bld.getUploadedMaps());
        }
        return serverMapList;
    }

    /* Downloads all buildings and returns them */
    private ArrayList<Building> downloadBuildings(String token) {

        ArrayList<Building> buildings = new ArrayList<>();
        DowloadBuildings task = new DowloadBuildings();
        String urlEndpoint = Config.loadProperties(context).getProperty("BUILDING_DOWNLOAD_URL");

        String result = null;
        try {
            result = task.execute(urlEndpoint, token).get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e("CRASH", "Interrupted while downloading buildings", e);
        } catch (Exception e) {
            Log.e("CRASH", "Failed to download buildings", e);
        }

        if (result != null) {

            try {
                JSONObject json = new JSONObject(result);
                JSONArray buildingsArray = json.getJSONArray("buildings");
                for (int i = 0; i < buildingsArray.length(); i++) {
                    JSONObject building = buildingsArray.getJSONObject(i);
                    String building_name = building.getString("name");
                    String building_description = building.getString("description");
                    String building_address = building.getString("address");
                    int building_num_floors = building.getInt("num_floors");
                    JSONArray uploadedMaps = building.getJSONArray("uploaded_maps");
                    ArrayList<Map> uploadedMapList = new ArrayList<>();
                    for (int j = 0; j < uploadedMaps.length(); j++) {
                        JSONObject uploadedMap = uploadedMaps.getJSONObject(j);
                        String floorNumber = uploadedMap.getString("floor_num");
                        String size = uploadedMap.getString("size");
                        uploadedMapList.add(new Map(building_name, floorNumber, size));
                    }
                    buildings.add(new Building(building_name, building_address, building_description, building_num_floors, uploadedMapList));
                }
            } catch (Exception e) {
                Log.e("CRASH", "Failed to parse downloaded buildings JSON response", e);
            }
        }
        return buildings;
    }

    /* An asynchronous class used to download buildings in the background */
    private static class DowloadBuildings extends AsyncTask<String, Void, String> {

        private static final String REQUEST_METHOD = "GET";

        /* Amount of time available to perform the download once connection is established */
        private static final int READ_TIMEOUT = 10000;

        /* Amount of time available to establish a connection */
        private static final int CONNECTION_TIMEOUT = 3000;

        @Override
        protected String doInBackground(String... params) {

            String stringUrl;
            String token;

            try {
                stringUrl = params[0];
                token = params[1];
            } catch (Exception e) {
                Log.e("CRASH", "Failed to set initial params", e);
                return null;
            }

            HttpURLConnection connection = null;
            InputStreamReader streamReader = null;
            BufferedReader reader = null;
            try {
                URL myUrl = new URL(stringUrl);
                connection = (HttpURLConnection) myUrl.openConnection();
                connection.setRequestMethod(REQUEST_METHOD);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setConnectTimeout(CONNECTION_TIMEOUT);

                /* Add headers */
                connection.setRequestProperty("Authorization", "Token " + token);
                connection.connect();

                streamReader = new InputStreamReader(connection.getInputStream());
                reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                return stringBuilder.toString();
            } catch (IOException e) {
                Log.e("CRASH", "Failed to get buildings", e);
                return null;
            } catch (Exception e) {
                Log.e("CRASH", "Failed to get buildings", e);
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (streamReader != null) {
                        streamReader.close();
                    }
                } catch (IOException e) {
                    Log.e("CRASH", "Failed to close readers after buildings download failed", e);
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to close readers after buildings download failed", e);
                }
            }
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
}


/* Takes a list of buildings, and removes any buildings with no maps */
//    public ArrayList<Building> cleanBuildingList(ArrayList<Building> list) {
//        ArrayList<Building> listCopy = new ArrayList<>();
//        for (Building building : list) {
//            if (!building.getUploadedMaps().isEmpty()) {
//                listCopy.add(building);
//            }
//        }
//        return listCopy;
//    }

//    public void saveBuildings(String token, ArrayList<Building> buildings) {
//        String buildingsTableName = "buildings";
//        String mapTableName = "maps";
//        SQLiteDatabase sqLiteDatabase = null;
//        try {
//            sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null);
//            sqLiteDatabase.execSQL(createbuildingTable(buildingsTableName));
//            sqLiteDatabase.execSQL(createMapsTable(mapTableName));
//            for (Building building : buildings) {
//                sqLiteDatabase.execSQL(addBuilding(buildingsTableName, building));
//                for (Map map : building.getUploadedMaps()) {
//                    sqLiteDatabase.execSQL(addMap(mapTableName, map));
//                }
//            }
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to save buildings", e);
//        } finally {
//            if (sqLiteDatabase != null) {
//                sqLiteDatabase.close();
//            }
//        }
//    }

// ------------------------------------------------------


//    public void saveBuildingRecord(String token, Building building) {
//        SQLiteDatabase sqLiteDatabase = null;
//        try {
//            sqLiteDatabase = context.openOrCreateDatabase(token, MODE_PRIVATE, null);
//            sqLiteDatabase.execSQL(createbuildingTable());
//            sqLiteDatabase.execSQL(addBuilding(building));
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to save building", e);
//        } finally {
//            if (sqLiteDatabase != null) {
//                sqLiteDatabase.close();
//            }
//        }
//    }

//    public void loadBuildingRecords(String token) {
//        ArrayList<Building> buildings = new ArrayList<>();
//        Cursor c = null;
//        SQLiteDatabase sqLiteDatabase = null;
//        try {
//            sqLiteDatabase = context.openOrCreateDatabase(token+"buildings", MODE_PRIVATE, null);
//            sqLiteDatabase.execSQL(createbuildingTable());
//            c = sqLiteDatabase.rawQuery(getbuildingsList(), null);
//            int nameIndex = c.getColumnIndex("name");
//            int addressIndex = c.getColumnIndex("address");
//            int descriptionIndex = c.getColumnIndex("description");
//            int numfloorsIndex = c.getColumnIndex("numfloors");
//            while (c.moveToNext()) {
//                String buildingName = c.getString(nameIndex);
//                Building building = new Building(c.getString(nameIndex), c.getString(addressIndex), c.getString(descriptionIndex), c.getInt(numfloorsIndex));
//                buildings.add(building);
//            }
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to load buildings", e);
//        } finally {
//            if (c != null) {
//                c.close();
//            }
//            if (sqLiteDatabase != null) {
//                sqLiteDatabase.close();
//            }
//        }
//        return buildings;
//    }

// ------------------------------------------------------

//    public void saveMaps(String token, ArrayList<Map> maps) {
//        String mapTableName = "maps";
//        SQLiteDatabase sqLiteDatabase = null;
//        try {
//            sqLiteDatabase = context.openOrCreateDatabase(token+"_maps", MODE_PRIVATE, null);
//            sqLiteDatabase.execSQL(createMapsTable(mapTableName));
//            for (Map map : maps) {
//                sqLiteDatabase.execSQL(addMap(mapTableName, map));
//            }
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to save maps", e);
//        } finally {
//            if (sqLiteDatabase != null) {
//                sqLiteDatabase.close();
//            }
//        }
//    }

    /*

    Buildings Table

    Name, #Floors, Description, Address

    Maps Table

    Name, Floor#, Size

     */


//    public ArrayList<Building> mapsToBuildings(ArrayList<Map> maps) {
//
//    }

//    public ArrayList<Building> loadBuildingsRecord(String token) {
//        String buildingsTableName = "buildings";
//        String mapTableName = "maps";
//        ArrayList<Building> buildings = new ArrayList<>();
//        Cursor c = null;
//        Cursor inc = null;
//        SQLiteDatabase sqLiteDatabase = null;
//        try {
//            sqLiteDatabase = context.openOrCreateDatabase(token+"buildings", MODE_PRIVATE, null);
//            sqLiteDatabase.execSQL(createbuildingTable(buildingsTableName));
//            sqLiteDatabase.execSQL(createMapsTable(mapTableName));
//            c = sqLiteDatabase.rawQuery(getbuildingsList(buildingsTableName), null);
//            int nameIndex = c.getColumnIndex("name");
//            int addressIndex = c.getColumnIndex("address");
//            int descriptionIndex = c.getColumnIndex("description");
//            int numfloorsIndex = c.getColumnIndex("numfloors");
//            while (c.moveToNext()) {
//                String buildingName = c.getString(nameIndex);
//                inc = sqLiteDatabase.rawQuery(getMaps(mapTableName, buildingName), null);
//                int mapsFloorIndex = inc.getColumnIndex("floor");
//                int mapsSizeIndex = inc.getColumnIndex("size");
//                ArrayList<Map> uploadedMaps = new ArrayList<>();
//                while (inc.moveToNext()) {
//                    uploadedMaps.add(new Map(buildingName, inc.getString(mapsFloorIndex), inc.getString(mapsSizeIndex)));
//                }
//                Building building = new Building(c.getString(nameIndex), c.getString(addressIndex), c.getString(descriptionIndex), c.getInt(numfloorsIndex), uploadedMaps);
//                buildings.add(building);
//            }
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to load buildings", e);
//        } finally {
//            if (c != null) {
//                c.close();
//            }
//            if (inc != null) {
//                inc.close();
//            }
//            if (sqLiteDatabase != null) {
//                sqLiteDatabase.close();
//            }
//        }
//    }

//    public void downloadMap(String token, String buildingName, String floorNumber) {
//        File dataFolder = new File(context.getFilesDir(),"maps/");
//        try {
//            if (!dataFolder.exists() && !dataFolder.mkdir()) {
//                Log.e("CRASH", "Failed to make directory to hold downloaded maps");
//                return;
//            }
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to create folder to hold maps");
//            return;
//        }
//        String folderPath = dataFolder.getAbsolutePath();
////        DownloadMapFile task = new DownloadMapFile();
//        String urlEndpoint = Config.loadProperties(context).getProperty("MAP_DOWNLOAD_URL");
//        try {
//            /* result will either be null if this fails, or "success" if it succeeds */
//            Log.i("Result", "Start");
//            String result = task.execute(urlEndpoint, token, buildingName, floorNumber, folderPath).get();
//            Log.i("Result", result);
//        } catch (InterruptedException | ExecutionException e) {
//            Log.e("CRASH", "Interrupted while downloading map file", e);
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to download map file", e);
//        }
//    }


//    public ArrayList<Building> downloadBuildings(String token) {
//
//        ArrayList<Building> buildings = new ArrayList<>();
//        DowloadBuildings task = new DowloadBuildings();
//        String urlEndpoint = Config.loadProperties(context).getProperty("BUILDING_DOWNLOAD_URL");
//
//        String result = null;
//        try {
//            result = task.execute(urlEndpoint, token).get();
//        } catch (InterruptedException | ExecutionException e) {
//            Log.e("CRASH", "Interrupted while downloading buildings", e);
//        } catch (Exception e) {
//            Log.e("CRASH", "Failed to download buildings", e);
//        }
//
//        if (result != null) {
//            try {
//                JSONObject json = new JSONObject(result);
//                JSONArray buildingsArray = json.getJSONArray("buildings");
//                for (int i = 0; i < buildingsArray.length(); i++) {
//                    JSONObject building = buildingsArray.getJSONObject(i);
//                    String building_name = building.getString("name");
//                    String building_description = building.getString("description");
//                    String building_address = building.getString("address");
//                    int building_num_floors = building.getInt("num_floors");
//                    JSONArray uploadedMaps = building.getJSONArray("uploaded_maps");
//                    ArrayList<String> uploadedMapList = new ArrayList<>();
//                    for (int j = 0; j < uploadedMaps.length(); j++) {
//                        JSONObject uploadedMap = uploadedMaps.getJSONObject(j);
//                        String floorNumber = uploadedMap.getString("floor_num");
//                        uploadedMapList.add(floorNumber);
//                    }
//                    buildings.add(new Building(building_name, building_address, building_description, building_num_floors, uploadedMapList));
//                }
//            } catch (Exception e) {
//                Log.e("CRASH", "Failed to parse downloaded buildings JSON response", e);
//            }
//        }
//        return buildings;
//    }

//    public ArrayList<Map> getDownloadedMapsList(String token) {
//        ArrayList<Map> downloadedMapList = new ArrayList<>();
//        return downloadedMapList;
//    }