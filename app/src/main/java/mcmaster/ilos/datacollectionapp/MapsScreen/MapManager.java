package mcmaster.ilos.datacollectionapp.MapsScreen;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.MarkerData;

import static android.content.Context.MODE_PRIVATE;

class MapManager {

    private Context context;
    MapboxMap mapboxMap;
    private String currentFloor;
    private int MAX_MARKER_COUNT_PER_FLOOR = 100;
    private String authtoken;

    MapManager(Context context, String floor, String authtoken) {
        this.context = context;
        currentFloor = floor;
        this.authtoken = authtoken;
    }

    /* Used for downloaded map files */
    String loadJsonFromDownloadedFile(String nameOfLocalFile) throws IOException {
        File mapsFolder = new File(context.getFilesDir(),"maps/");
        String mapsFolderPath = mapsFolder.getAbsolutePath();
        File fileToRead = new File(mapsFolderPath, nameOfLocalFile);
        InputStream is = new FileInputStream(fileToRead.getAbsolutePath());
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }

    /* Used for statically added map files */
    public String loadJsonFromAsset(String nameOfLocalFile) throws IOException {
        InputStream is = context.getAssets().open(nameOfLocalFile);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }

    private LatLngBounds calculateBounds(String geojsonString) throws Exception {
        ArrayList<Double> latList = new ArrayList<>();
        ArrayList<Double> lngList = new ArrayList<>();
        JSONObject json = new JSONObject(geojsonString);
        JSONArray features = json.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject geometry = feature.getJSONObject("geometry");
            if (geometry != null) {
                String type = geometry.getString("type");
                JSONArray coords = geometry.getJSONArray("coordinates");
                if (!TextUtils.isEmpty(type) && (type.equalsIgnoreCase("LineString"))) {
                    for (int coordIndex = 0; coordIndex < coords.length(); coordIndex++) {
                        JSONArray coord = coords.getJSONArray(coordIndex);
                        latList.add(coord.getDouble(1));
                        lngList.add(coord.getDouble(0));
                    }
                } else if (!TextUtils.isEmpty(type) && (type.equalsIgnoreCase("Polygon"))) {
                    JSONArray innerCoords = coords.getJSONArray(0);
                    for (int coordIndex = 0; coordIndex < innerCoords.length(); coordIndex++) {
                        JSONArray coord = innerCoords.getJSONArray(coordIndex);
                        latList.add(coord.getDouble(1));
                        lngList.add(coord.getDouble(0));
                    }
                } else {
                    latList.add(coords.getDouble(1));
                    lngList.add(coords.getDouble(0));
                }
            }
        }
        Double minLat = latList.get(latList.indexOf(Collections.min(latList)));
        Double maxLat = latList.get(latList.indexOf(Collections.max(latList)));
        Double minLng = lngList.get(lngList.indexOf(Collections.min(lngList)));
        Double maxLng = lngList.get(lngList.indexOf(Collections.max(lngList)));

        return new LatLngBounds.Builder()
                .include(new LatLng(minLat, minLng))
                .include(new LatLng(maxLat, maxLng))
                .build();
    }

//    public void saveCurrentFloor(String floor) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
//        sharedPreferences.edit().putString("currentFloor", floor).apply();
//    }
//
//    public void loadCurrentFloor() {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
//        String floor = sharedPreferences.getString("currentFloor", "1");
//        this.currentFloor = floor;
//    }

    void alignMap(String geojsonString) throws Exception {
        LatLngBounds mapBounds = calculateBounds(geojsonString);
        mapboxMap.setLatLngBoundsForCameraTarget(mapBounds);
        mapboxMap.setMinZoomPreference(0);
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBounds, 200));
        Double currentZoom = mapboxMap.getCameraPosition().zoom;
        mapboxMap.setMinZoomPreference(15);
//        mapboxMap.setMinZoomPreference(currentZoom);
    }

    String getCurrentFloor() {
        return this.currentFloor;
    }

    void setCurrentFloor(String currentFloor) {
        this.currentFloor = currentFloor;
    }

    void saveCurrentFloorForBuilding(String databaseName, String currentFloor, String buildigName) {
        String tableName = "floors";
        SQLiteDatabase sqLiteDatabase = null;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(authtoken+"_"+databaseName, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createCurrentFloorTable(tableName));
            sqLiteDatabase.execSQL(deleteFloor(tableName));
            sqLiteDatabase.execSQL(saveFloor(tableName, buildigName, currentFloor));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(" CATCH", "Failed to save current floor for building", e);
        } finally {
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
    }

    String loadCurrentFloorForBuilding(String databaseName, String buildingName) {
        String tableName = "floors";
        String floorNum = this.currentFloor;
        Cursor c = null;
        SQLiteDatabase sqLiteDatabase = null;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(authtoken+"_"+databaseName, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createCurrentFloorTable(tableName));
            c = sqLiteDatabase.rawQuery(getFloor(tableName, buildingName), null);
            int floorIndex = c.getColumnIndex("floor");
            while (c.moveToNext()) {
                floorNum = c.getString(floorIndex);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to load current floor from database", e);
        } finally {
            if (c != null) {
                c.close();
            }
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
        this.currentFloor = floorNum;
        return floorNum;
    }

    void saveMarker(String databaseName, LatLng point) {
        String tableName = "markers";
        SQLiteDatabase sqLiteDatabase = null;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(authtoken+"_"+databaseName, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createMarkerTable(tableName));
            sqLiteDatabase.execSQL(addMarker(tableName, point, getCurrentFloor()));
        } catch (Exception e) {
            Log.e("CRASH", "Failed to save marker to database", e);
        } finally {
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
    }

    void deleteMarker(String databaseName, LatLng point) {
        String tableName = "markers";
        SQLiteDatabase sqLiteDatabase = null;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(authtoken+"_"+databaseName, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createMarkerTable(tableName));
            sqLiteDatabase.execSQL(removeMarker(tableName, point));
        } catch (Exception e) {
            Log.e("CRASH", "Failed to remove marker from database", e);
        } finally {
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
    }

    ArrayList<MarkerData> loadMarkers(String databaseName, String floor) {
        String tableName = "markers";
        ArrayList<MarkerData> markers = new ArrayList<>();
        Cursor c = null;
        SQLiteDatabase sqLiteDatabase = null;
        try {
            sqLiteDatabase = context.openOrCreateDatabase(authtoken+"_"+databaseName, MODE_PRIVATE, null);
            sqLiteDatabase.execSQL(createMarkerTable(tableName));
            c = sqLiteDatabase.rawQuery(getMarkersOnFloor(tableName, floor), null);
            int latIndex = c.getColumnIndex("latitude");
            int lngIndex = c.getColumnIndex("longitude");
            int floorIndex = c.getColumnIndex("floor");
            while (c.moveToNext()) {
                MarkerData markerData = new MarkerData(c.getDouble(latIndex), c.getDouble(lngIndex), c.getString(floorIndex));
                markers.add(markerData);
            }
        } catch (Exception e) {
            Log.e("CRASH", "Failed to load markers from database", e);
        } finally {
            if (c != null) {
                c.close();
            }
            if (sqLiteDatabase != null) {
                sqLiteDatabase.close();
            }
        }
        return markers;
    }

    int getMaxMarkerCountPerFloor() {
        return MAX_MARKER_COUNT_PER_FLOOR;
    }

    /* SQL MARKERS */

    private String createMarkerTable(String tableName) {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (latitude REAL, longitude REAL, floor VARCHAR, UNIQUE(latitude, longitude, floor))";
    }

    private String addMarker(String tableName, LatLng point, String floor) {
        return "INSERT OR IGNORE INTO " + tableName + " (latitude, longitude, floor) VALUES (" + point.getLatitude() + ", " + point.getLongitude() + ", '" + floor + "')";
    }

    private String removeMarker(String tableName, LatLng point) {
        return "DELETE FROM " + tableName + " WHERE latitude = " + point.getLatitude() + " AND longitude = " + point.getLongitude();
    }

    private String updateMarker(String tableName, Double oldLat, Double oldLng, Double newLat, Double newLng) {
        return "UPDATE " + tableName + " SET latitude = " + newLat + ", longitude = " + newLng + " WHERE latitude = " + oldLat + " AND longitude = " + oldLng;
    }

    private String getMarkersOnFloor(String tableName, String floor) {
        return "SELECT * FROM " + tableName + " WHERE floor = '" + floor + "'";
    }

    /* Current Floor SQL Commands */

    private String createCurrentFloorTable(String tableName) {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (floor VARCHAR, buildingName VARCHAR, UNIQUE(floor, buildingName))";
    }

    private String saveFloor(String tableName, String buildingName, String currentFloor) {
        return "INSERT OR IGNORE INTO " + tableName + " (floor, buildingName) VALUES ('" + currentFloor + "', '" + buildingName + "')";
    }

    private String deleteFloor(String tableName) {
        return "DELETE FROM " + tableName;
    }

    private String getFloor(String tableName, String buildingName) {
        return "SELECT * FROM " + tableName + " WHERE buildingName = '" + buildingName + "'";
    }
}