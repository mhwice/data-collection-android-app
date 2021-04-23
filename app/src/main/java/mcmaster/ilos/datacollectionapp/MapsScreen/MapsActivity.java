package mcmaster.ilos.datacollectionapp.MapsScreen;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.button.MaterialButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.Line;
import com.mapbox.mapboxsdk.plugins.annotation.LineManager;
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions;
import com.mapbox.mapboxsdk.plugins.annotation.OnCircleClickListener;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.MapMarker;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.RSSItem;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.SaveFile;
import mcmaster.ilos.datacollectionapp.R;
import mcmaster.ilos.datacollectionapp.DataModel.BTManager;
import mcmaster.ilos.datacollectionapp.DataModel.ComplementaryFilter;
import mcmaster.ilos.datacollectionapp.DataModel.GPSListener;
import mcmaster.ilos.datacollectionapp.DataModel.GSMListener;
import mcmaster.ilos.datacollectionapp.DataModel.SensorDataManager;
import mcmaster.ilos.datacollectionapp.DataModel.WifiDataManager;
import mcmaster.ilos.datacollectionapp.DataModel.WifiScanListener;
import mcmaster.ilos.datacollectionapp.StepCounter.FancyStepCounter;
import mcmaster.ilos.datacollectionapp.StepCounter.StepEventListener;
import mcmaster.ilos.datacollectionapp.Utils.Config;
import mcmaster.ilos.datacollectionapp.Utils.InternetManager;
import mcmaster.ilos.datacollectionapp.Utils.PermissionManager;
import mcmaster.ilos.datacollectionapp.protobuf.ProtoData;
import mcmaster.ilos.datacollectionapp.Utils.ProtobufManager;

import static android.hardware.SensorManager.getAltitude;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static mcmaster.ilos.datacollectionapp.BuildingsScreen.BuildingManager.saveFileRecord;
import static mcmaster.ilos.datacollectionapp.Utils.ProtobufManager.getCheckpointFileCount;
import static mcmaster.ilos.datacollectionapp.Utils.ProtobufManager.getFiles;
import static mcmaster.ilos.datacollectionapp.Utils.ProtobufManager.getSizeOfCheckpointsDuringScan;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getFreeDiskSpace;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.isDiskSpaceEmpty;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.unformatSize;

/* Handles the activity which displays the map and markers, allows for data collection, and for data uploading */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapLongClickListener, MapboxMap.OnMoveListener, StepEventListener, WifiScanListener {

    // A flag which states whether or not the app is currently collecting data
    public boolean isCollecting;

    // Represents the state or mode of the app as listed below.
    public int STATE;
    // STATE
    // 0 - Idle
    // 1 - Add Marker
    // 2 - Delete Marker
    // 3 - Outlining Point
    // 4 - Outlining Path
    // 5 - Outlining Multipath
    // 6 - Outlining Stair
    // 7 - Outlining exits

    // Represents the which sensors are chosen to be used when performing the next scan. Other sensors such as IMU are always enabled.
    private int SENSOR_STATE;
    // SENSOR_STATE
    // 0 - WiFi & Bluetooth
    // 1 - WiFi
    // 2 - Bluetooth

    private String buildingName;
    public ArrayList<MapMarker> markerList = new ArrayList<>();
    private ArrayList<Line> lineList = new ArrayList<>();

    // Android sensors (ie. IMU)
    public SensorDataManager sensorDataManager;
    public SensorManager mSensorManager;

    // Wifi
    public WifiDataManager wifiDataManager;

    // Handles map logic (like adding/removing markers)
    private MapManager mapManager;

    // Handles map UI (draws things on maps)
    private MapsUIManager mapsUIManager;

    // Step counter
    private FancyStepCounter fancyStepCounter;
    private ArrayList<Long> stepTimestamps = new ArrayList<>();

    // The class responsible for creating the PBF files and uploading them
    private ProtobufManager protobufManager;

    // Bluetooth
    public BTManager btManager;

    // Cellular
    public GSMListener gsmListener;

    // GPS/Satellite
    public GPSListener gpsListener;

    // Mapbox managers
    private MapView mapView;
    public CircleManager circleManager;
    private LineManager lineManager;
    private MapboxMap mapboxMap;
    private GeoJsonSource source;

    // Holds the colors of the map markers
    private int MARKER_GREEN;
    private int MARKER_YELLOW;
    private int MARKER_RED;
    private int MARKER_GRAY;

    /* Number of scans performed for point-based collection */
    private static final int PREFERRED_SCANS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_maps);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        Intent intent = getIntent();
        buildingName = intent.getStringExtra("buildingName");
        String authtoken = intent.getStringExtra("token");

        // Gets the floors that are available to the building and sort them
        ArrayList<String> floors = intent.getStringArrayListExtra("floors");
        Collections.sort(floors, new Comparator<String>() {
            private boolean isThereAnyNumber(String a, String b) {
                return isNumber(a) || isNumber(b);
            }
            private boolean isNumber(String s) {
                return s.matches("[-+]?\\d*\\.?\\d+");
            }
            @Override
            public int compare(String a, String b) {
                return isThereAnyNumber(a, b)
                        ? isNumber(a) ? 1 : -1
                        : a.compareTo(b);
            }
        });

        MARKER_GREEN = ResourcesCompat.getColor(getResources(), R.color.marker_green, null);
        MARKER_YELLOW = ResourcesCompat.getColor(getResources(), R.color.marker_yellow, null);
        MARKER_RED = ResourcesCompat.getColor(getResources(), R.color.marker_red, null);
        MARKER_GRAY = ResourcesCompat.getColor(getResources(), R.color.marker_gray, null);

        STATE = 0;
        SENSOR_STATE = 0;
        isCollecting = false;

        // ----- Step Counter ----- //
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        fancyStepCounter = new FancyStepCounter();
        fancyStepCounter.setStepListener(this);

        // ----- Load Current Floor ----- //
        mapManager = new MapManager(this, floors.get(0), authtoken);
        mapManager.loadCurrentFloorForBuilding(buildingName, buildingName);

        // ----- Initialize Map ----- //
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // ----- Set Initial UI ----- //
        mapsUIManager = new MapsUIManager(this, MapsActivity.this, floors);
        mapsUIManager.setInitialUI(mapManager.getCurrentFloor());
        mapsUIManager.handleSensorState(SENSOR_STATE);

        // ----- Set Permissions ----- //
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
        PermissionManager.getInstance().checkPermissions(MapsActivity.this, permissions, permissionResult);

        // ----- Init Sensor Data Manager ----- //
        sensorDataManager = SensorDataManager.getInstance();
        sensorDataManager.init(this);

        // ----- Init Wifi Data Manager ----- //
        wifiDataManager = WifiDataManager.getInstance();
        wifiDataManager.init(this);
        wifiDataManager.setScanListener(this);

        // ----- Init Bluetooth Manager ----- //
        btManager = BTManager.getInstance();
        btManager.init(this);

        // ----- Init GSM Manager ----- //
        gsmListener = GSMListener.getInstance();
        gsmListener.init(this);

        // ----- Init GPS Manager ----- //
        gpsListener = GPSListener.getInstance();
        gpsListener.init(this);

        // ----- Init Protobuf Manager ----- //
        protobufManager = new ProtobufManager(this);

        // ----- Set Click Events ----- //

        ImageView closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> {

            mapsUIManager.hideCloseButton();
            isCollecting = false;

            /* WiFi */
            if (wifiDataManager != null) {
                wifiDataManager.stopScanning();
            }

            /* BT */
            if (btManager != null) {
                btManager.stopScanning();
            }

            /* Sensors */
            if (sensorDataManager != null) {
                sensorDataManager.stopScanning();
            }

            /* Step Counting */
            stopStepCounting();

            mapsUIManager.showFloatingActionButton();
            mapsUIManager.setFABText("Start");
        });

        MaterialButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {

            if (!isCollecting) {
                if (STATE == 3 && markerList.size() == 1) {
                    startDataCollection();
                } else if (STATE == 4 && markerList.size() > 1 ) {
                    startDataCollection();
                    checkForStopCriteria();
                } else if (STATE == 5 && markerList.size() > 1) {
                    startDataCollection();
                    checkForStopCriteria();
                } else if (STATE == 6 && markerList.size() > 1) {
                    startDataCollection();
                    checkForStopCriteria();
                } else if (STATE == 7 && markerList.size() > 1) {
                    startDataCollection();
                    checkForStopCriteria();
                }
            } else {
                if (STATE == 4 && markerList.size() > 1) {
                    checkForStopCriteria();
                } else if (STATE == 5 && markerList.size() > 1) {
                    checkForStopCriteria();
                } else if (STATE == 6 && markerList.size() > 1) {
                    checkForStopCriteria();
                } else if (STATE == 7 && markerList.size() > 1) {
                    checkForStopCriteria();
                }
            }

            mapsUIManager.showFloatingActionButton();
        });

        Button dialogUploadButton = mapsUIManager.dialog.findViewById(R.id.dialog_upload_button);
        dialogUploadButton.setOnClickListener(v -> {

        	try {

				/* Save file */
				ProtoData.DataPack pack = protobufManager.packDataFromCheckpoints(STATE, buildingName, -1, stepTimestamps, SENSOR_STATE);

				SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE);
				String token = sharedPreferences.getString("token", "");
				DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
				Date date = new Date();

				String fileName;
				if (STATE == 3 || STATE == 4 || STATE == 5 || STATE == 6 || STATE == 7) {
					fileName = token + "_" + STATE + "_" + dateFormat.format(date) + ".pbf";
				} else {
					fileName = token + "_Calibration_" + dateFormat.format(date) + ".pbf";
				}
				protobufManager.savePackToFile(pack, fileName);

				/* Delete checkpoints */
				protobufManager.deleteAllCheckpointFiles(this);

				protobufManager.printFiles(this);

				/* Upload file */
				protobufManager.uploadFile(token, new SaveFile(buildingName, markerList.get(0).getFloor(), markerList.get(markerList.size()-1).getFloor(), fileName), "0");

				mapsUIManager.dismissUploadDialog();
				mapsUIManager.showUploadingDialog();

			} catch (Exception e) {
				Log.e("CRASH", "Failed to upload datapack", e);

				mapsUIManager.dismissUploadDialog();
				mapsUIManager.showCollectionFailedDialog();
			}
        });

        Button dialogUploadLaterButton = mapsUIManager.dialog.findViewById(R.id.dialog_upload_later_button);
        dialogUploadLaterButton.setOnClickListener(v -> {

        	try {

				/* Save file */
				ProtoData.DataPack pack = protobufManager.packDataFromCheckpoints(STATE, buildingName, -1, stepTimestamps, SENSOR_STATE);

				SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE);
				String token = sharedPreferences.getString("token", "");
				DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
				Date date = new Date();

				String fileName;
				if (STATE == 3 || STATE == 4 || STATE == 5 || STATE == 6 || STATE == 7) {
					fileName = token + "_" + STATE + "_" + dateFormat.format(date) + ".pbf";
				} else {
					fileName = token + "_Calibration_" + dateFormat.format(date) + ".pbf";
				}

				String size = protobufManager.savePackToFile(pack, fileName);
				boolean saveSuccess = saveFileRecord(token, new SaveFile(buildingName,markerList.get(0).getFloor(), markerList.get(markerList.size()-1).getFloor(), fileName, size), MapsActivity.this);
				if (saveSuccess) {
//                 failed to save file
				} else {
					Log.e("CRASH", "Failed to save pack");
				}

				/* Delete checkpoints */
				protobufManager.deleteAllCheckpointFiles(this);

				mapsUIManager.dismissUploadDialog();

			} catch (Exception e) {
				Log.e("CRASH", "Failed to upload datapack", e);

				mapsUIManager.dismissUploadDialog();
				mapsUIManager.showCollectionFailedDialog();
			}
        });

        Button dialogDeleteButton = mapsUIManager.dialog.findViewById(R.id.dialog_delete_button);
        dialogDeleteButton.setOnClickListener(v -> mapsUIManager.dismissUploadDialog());

        Button diskSpaceButton = mapsUIManager.diskSpaceDialog.findViewById(R.id.failed_dialog_ok);
        diskSpaceButton.setOnClickListener(v -> mapsUIManager.dismissDiskSpaceDialog());

        Button longPathButton = mapsUIManager.longPathDialog.findViewById(R.id.failed_dialog_ok);
        longPathButton.setOnClickListener(v -> mapsUIManager.dismissLongPathDialog());

        Button failedDialogButton = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_ok);
        failedDialogButton.setOnClickListener(v -> mapsUIManager.dismissCollectionFailedDialog());

        Button failedInternetDialogButton = mapsUIManager.noInternetDialog.findViewById(R.id.failed_dialog_ok);
        failedInternetDialogButton.setOnClickListener(v -> mapsUIManager.dismissInternetDialog());

        View addIcon = findViewById(R.id.add_edit_icon);
        addIcon.setOnClickListener(v -> handleOptionSelection(1));

        View deleteIcon = findViewById(R.id.delete_edit_icon);
        deleteIcon.setOnClickListener(v -> handleOptionSelection(2));

        View pointIcon = findViewById(R.id.point_icon);
        pointIcon.setOnClickListener(v -> handleOptionSelection(3));

        View pathIcon = findViewById(R.id.path_icon);
        pathIcon.setOnClickListener(v -> handleOptionSelection(4));

        View multipathIcon = findViewById(R.id.multipath_icon);
        multipathIcon.setOnClickListener(v -> handleOptionSelection(5));

        // Commented out for now ...
//        View stairIcon = findViewById(R.id.stair_icon);
//        stairIcon.setOnClickListener(v -> handleOptionSelection(6));
//
//        View ioIcon = findViewById(R.id.io_icon);
//        ioIcon.setOnClickListener(v -> handleOptionSelection(7));

        View wifiIcon = findViewById(R.id.wifi_icon);
        wifiIcon.setOnClickListener(v -> {

            if (isCollecting) {
                stopDataCollection();
            }
            unselectAllMarkers();
            removeAllLines();
            markerList.clear();
            mapsUIManager.hideFloatingActionButton();
            mapsUIManager.hideCloseButton();

            if (SENSOR_STATE == 0) {
                SENSOR_STATE = 2;
            } else if (SENSOR_STATE == 1) {
                SENSOR_STATE = 1;
            } else {
                SENSOR_STATE = 0;
            }

            mapsUIManager.handleSensorState(SENSOR_STATE);
        });

        View bluetoothIcon = findViewById(R.id.bluetooth_icon);
        bluetoothIcon.setOnClickListener(v -> {
            if (isCollecting) {
                stopDataCollection();
            }
            unselectAllMarkers();
            removeAllLines();
            markerList.clear();
            mapsUIManager.hideFloatingActionButton();
            mapsUIManager.hideCloseButton();

            if (SENSOR_STATE == 0) {
                SENSOR_STATE = 1;
            } else if (SENSOR_STATE == 1) {
                SENSOR_STATE = 0;
            } else {
                SENSOR_STATE = 2;
            }

            mapsUIManager.handleSensorState(SENSOR_STATE);
        });

        // This is the click listener for the floors. When a new floor is selected, the new map needs to be loaded and displayed
        LinearLayout hs = findViewById(R.id.horizontal_scroll_container);
        for (int i = 0; i < hs.getChildCount(); i++) {
            TextView tv = (TextView) hs.getChildAt(i);
            tv.setOnClickListener(v -> {
                if (!v.getTag().toString().equals(mapManager.getCurrentFloor())) {
                    mapsUIManager.unselectFloor(mapManager.getCurrentFloor());
                    mapManager.setCurrentFloor(v.getTag().toString());
                    mapsUIManager.selectFloor(mapManager.getCurrentFloor());
                    mapManager.saveCurrentFloorForBuilding(buildingName, mapManager.getCurrentFloor(), buildingName);

                    hideAllMarkers();
                    hideAllLines();
                    for (CircleOptions circleOption: mapsUIManager.drawFloorMarkers(mapManager.loadMarkers(buildingName, mapManager.getCurrentFloor()))) {
                        circleManager.create(circleOption);
                    }

                    redrawMarkersOnFloor();
                    redrawLinesOnFloor();

                    try {
                        String geojsonString = mapManager.loadJsonFromDownloadedFile(buildingName + "_" + mapManager.getCurrentFloor() + ".geojson");
                        source.setGeoJson(geojsonString);
                        mapManager.alignMap(geojsonString);
                    } catch (Exception e) {
                        Log.e("CRASH", "Failed to set add map layer", e);
                    }
                    mapsUIManager.closeBottomSheet();
                }
            });
        }

        View peekView = findViewById(R.id.peekView);
        peekView.setOnClickListener(v -> mapsUIManager.openBottomSheet());
    }

    public void uploadFinished() {
        runOnUiThread(() -> mapsUIManager.dismissUploadingDialog());
    }

    public void uploadFailed() {
        runOnUiThread(() -> {
            mapsUIManager.dismissUploadingDialog();
            mapsUIManager.showInternetDialog();
        });
    }

    private int visitedIndex() {
        for (MapMarker mapMarker : markerList) {
            if (!mapMarker.getVisited()) {
                mapMarker.setVisited(true);
                if (sensorDataManager.lastPressureValue != null) {
                    mapMarker.setAltitude(getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sensorDataManager.lastPressureValue));
                }
                mapMarker.setTimestamp(SystemClock.elapsedRealtimeNanos());
                return markerList.indexOf(mapMarker)+1;
            }
        }
        return markerList.size();
    }

    private void clearVisited() {
        for (MapMarker mapMarker : markerList) {
            mapMarker.setVisited(false);
            mapMarker.setTimestamp(-1);
        }
    }

    private void handleOptionSelection(int newState) {

        if (isCollecting) {
            stopDataCollection();
        }

        mapsUIManager.unselectOption(STATE);
        if (STATE != newState) {
            STATE = newState;
            mapsUIManager.selectOption(STATE);
        } else {
            STATE = 0;
        }

        unselectAllMarkers();
        removeAllLines();
        markerList.clear();
        mapsUIManager.hideFloatingActionButton();
        mapsUIManager.closeBottomSheet();
        mapsUIManager.hideCloseButton();
    }

    // For permission check
    PermissionManager.IPermissionResult permissionResult = new PermissionManager.IPermissionResult() {

        @Override
        public void permissionSucceed() {}

        @Override
        public void permissionFailed() {
            Toast.makeText(MapsActivity.this, "Permission check failed!", Toast.LENGTH_SHORT).show();
            finish();
            System.exit(0);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance().onRequestPermissionResult(this, requestCode, permissions, grantResults);
    }

    // ----- Map Functions ----- //

    private boolean hasReachedMarkerLimit() {
        return circleManager.getAnnotations().size() >= mapManager.getMaxMarkerCountPerFloor();
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng point) {
        if (STATE == 1 && !hasReachedMarkerLimit()) {
            CircleOptions circleOptions = mapsUIManager.drawMarker(point);
            circleManager.create(circleOptions);
            mapManager.saveMarker(buildingName, point);
        }
        return true;
    }

    @Override
    public void onMoveBegin(@NonNull MoveGestureDetector detector) {
        mapsUIManager.closeBottomSheet();
    }

    @Override
    public void onMove(@NonNull MoveGestureDetector detector) {}

    @Override
    public void onMoveEnd(@NonNull MoveGestureDetector detector) {}

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {

        MapsActivity.this.mapboxMap = mapboxMap;
        mapboxMap.addOnMapLongClickListener(this);
        mapboxMap.addOnMoveListener(this);
        mapManager.mapboxMap = mapboxMap;

//        mapboxMap.setStyle(Style.DARK, style -> {
        mapboxMap.setStyle(new Style.Builder().fromUrl("mapbox://styles/espr3sso/cirz0ct1k003wgvnpx9hijiq2"), style -> {

            // Defines the GeoJSON styling
            try {
                String geojsonString = mapManager.loadJsonFromDownloadedFile(buildingName + "_" + mapManager.getCurrentFloor() + ".geojson");
                source = new GeoJsonSource("source-id", geojsonString);

                FillLayer fillLayer = new FillLayer("layer-id", "source-id");
                fillLayer.setProperties(PropertyFactory.fillColor(Color.WHITE));
                style.addSource(source);
                style.addLayer(fillLayer);

                // Adds the black outline
                LineLayer lineLayer = new LineLayer("line-layer-id", "source-id");
                lineLayer.setProperties(
                        PropertyFactory.lineColor(Color.BLACK),
                        PropertyFactory.lineWidth(1f)
                );
                style.addLayer(lineLayer);

                // Adds the room numbers
                SymbolLayer symbolLayer = new SymbolLayer("symbol-layer-id", "source-id");
                symbolLayer.setProperties(
                        PropertyFactory.textField(get("name"))
                );
                style.addLayer(symbolLayer);

                mapManager.alignMap(geojsonString);

            } catch (Exception e) {
                Log.e("CRASH", "Failed to set add map layer", e);
            }

            lineManager = new LineManager(mapView, mapboxMap, style);
            circleManager = new CircleManager(mapView, mapboxMap, style);

            // Determine what action to perform when a marker is pressed (depends on the state)
            circleManager.addClickListener(circle -> {
                int circleIndex = indexOfCircle(circle);
                switch (STATE) {
                    case 2:
                        handleDeleteMarkerSelection(circle);
                        break;
                    case 3:
                        handlePointSelection(circleIndex, circle);
                        break;
                    case 4:
                        handlePathSelection(circleIndex, circle);
                        break;
                    case 5:
                        handleMultipathSelection(circleIndex, circle);
                        break;
                    case 6:
                        handleMultipathSelection(circleIndex, circle);
                        break;
                    case 7:
                        handlePathSelection(circleIndex, circle);
                        break;
                    default:
                        break;
                }

                if (STATE == 3 && markerList.size() == 1 || STATE == 4 && markerList.size() > 1 || STATE == 5 && markerList.size() > 1 || STATE == 6 && markerList.size() > 1 || STATE == 7 && markerList.size() > 1) {
                    mapsUIManager.showFloatingActionButton();
                    mapsUIManager.setFABText("Start");
                } else {
                    mapsUIManager.hideFloatingActionButton();
                }
            });
            for (CircleOptions circleOption: mapsUIManager.drawFloorMarkers(mapManager.loadMarkers(buildingName, mapManager.getCurrentFloor()))) {
                circleManager.create(circleOption);
            }
        });
    }

    private void handleDeleteMarkerSelection(Circle circle) {
        for (Circle markerCircle : asList(circleManager.getAnnotations())) {
            if (markerCircle.getLatLng().equals(circle.getLatLng())) {
                circleManager.delete(markerCircle);
                mapManager.deleteMarker(buildingName, markerCircle.getLatLng());
                break;
            }
        }
    }

    private void handlePointSelection(int circleIndex, Circle circle) {
        if (markerList.size() == 0) {
            addMarker(circle, MARKER_GREEN);
        } else {
            unselectAllMarkers();
            if (circleIndex != 0) {
                addMarker(circle, MARKER_GREEN);
            }
        }
    }

    private void handlePathSelection(int circleIndex, Circle circle) {
        if (markerList.size() == 0) {
            addMarker(circle, MARKER_GREEN);
        } else if (markerList.size() == 1) {
            if (circleIndex == 0) {
                unselectAllMarkers();
                removeAllLines();
            } else {
                addMarker(circle, MARKER_RED);
                addLine(markerList.get(0), markerList.get(1));
            }
        } else {
            if (circleIndex == 0) {
                unselectAllMarkers();
                removeAllLines();
            } else if (circleIndex == 1) {
                removeMarkerAtIndex(1);
                removeAllLines();
            } else {
                removeAllLines();
                removeMarkerAtIndex(1);
                addMarker(circle, MARKER_RED);
                addLine(markerList.get(0), markerList.get(1));
            }
        }
    }

    private void handleMultipathSelection(int circleIndex, Circle circle) {
        if (markerList.size() == 0) {
            addMarker(circle, MARKER_GREEN);
        } else if (markerList.size() == 1) {
            if (circleIndex == 0) {
                unselectAllMarkers();
                removeAllLines();
            } else {
                addMarker(circle, MARKER_RED);
                addLine(markerList.get(0), markerList.get(1));
            }
        } else if (markerList.size() == 2) {
            if (circleIndex != -1) {
                if (circleIndex == 0) {
                    unselectAllMarkers();
                    removeAllLines();
                } else {
                    removeMarkerAtIndex(1);
                    removeAllLines();
                }
            } else {
                changeMarkerColorAtIndex(markerList.size()-1, MARKER_YELLOW);
                addMarker(circle, MARKER_RED);
                addLine(markerList.get(markerList.size()-2), markerList.get(markerList.size()-1));
            }
        } else {
            if (circleIndex != -1) {
                if (circleIndex == 0) {
                    unselectAllMarkers();
                    removeAllLines();
                } else if (circleIndex == markerList.size()-1) {
                    removeLineAtIndex(lineList.size()-1);
                    removeMarkerAtIndex(circleIndex);
                    changeMarkerColorAtIndex(markerList.size()-1, MARKER_RED);
                } else {
                    changeMarkerColorAtIndex(circleIndex, MARKER_RED);
                    for (int i = markerList.size()-1; i > circleIndex; i--) {
                        removeMarkerAtIndex(i);
                        removeLineAtIndex(i-1);
                    }
                }
            } else {
                if (onSameFloorAs(markerList.size()-1)) {
                    changeMarkerColorAtIndex(markerList.size()-1, MARKER_YELLOW);
                }
                addMarker(circle, MARKER_RED);
                addLine(markerList.get(markerList.size()-2), markerList.get(markerList.size()-1));
            }
        }
    }

    private void unselectAllMarkers() {
        for (MapMarker marker : markerList) {
            if (marker.getFloor().equals(mapManager.getCurrentFloor())) {
                Circle updatedCircle = mapsUIManager.updateCircleColor(marker.getCircle(), MARKER_GRAY);
                circleManager.update(updatedCircle);
            }
        }
        markerList.clear();
    }

    private int indexOfCircle(Circle circle) {
        int circleIndex = -1;
        for (int i = 0; i < markerList.size(); i++) {
            if (markerList.get(i).getCircle().getLatLng().equals(circle.getLatLng())) {
                circleIndex = i;
            }
        }
        return circleIndex;
    }

    private void addMarker(Circle circle, int color) {
        markerList.add(new MapMarker(circle, false, mapManager.getCurrentFloor()));
        Circle updatedCircle = mapsUIManager.updateCircleColor(circle, color);
        circleManager.update(updatedCircle);
    }

    private void removeMarkerAtIndex(int index) {
        if (markerList.get(index).getFloor().equals(mapManager.getCurrentFloor())) {
            Circle updatedCircle = mapsUIManager.updateCircleColor(markerList.get(index).getCircle(), MARKER_GRAY);
            circleManager.update(updatedCircle);
        }
        markerList.remove(index);
    }

    public void removeAllMarkers() {
        circleManager.delete(asList(circleManager.getAnnotations()));
        markerList.clear();
    }

    public void hideAllMarkers() {
        circleManager.delete(asList(circleManager.getAnnotations()));
    }

    private static <Circle> List<Circle> asList(LongSparseArray<Circle> sparseArray) {
        if (sparseArray == null) return null;
        List<Circle> arrayList = new ArrayList<>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++)
            arrayList.add(sparseArray.valueAt(i));
        return arrayList;
    }

    private void changeMarkerColorAtIndex(int index, int color) {
        if (markerList.get(index).getFloor().equals(mapManager.getCurrentFloor())) {
            Circle updatedCircle = mapsUIManager.updateCircleColor(markerList.get(index).getCircle(), color);
            circleManager.update(updatedCircle);
        }
    }

    private void addLine(MapMarker startMarker, MapMarker endMarker) {
        LineOptions lineOptions = mapsUIManager.drawLine(startMarker.getCircle().getLatLng(), endMarker.getCircle().getLatLng());
        Line line = lineManager.create(lineOptions);
        lineList.add(line);
    }

    private void removeAllLines() {
        lineManager.delete(lineList);
        lineList.clear();
    }

    private void hideAllLines() {
        lineManager.delete(asList(lineManager.getAnnotations()));
    }

    private void removeLineAtIndex(int index) {
        lineManager.delete(lineList.get(index));
        lineList.remove(index);
    }

    private boolean onSameFloorAs(int index) {
        return markerList.get(index).getFloor().equals(mapManager.getCurrentFloor());
    }

    private void redrawMarkersOnFloor() {
        for (Circle circle : asList(circleManager.getAnnotations())) {
            for (MapMarker marker : markerList) {
                if (circle.getLatLng().equals(marker.getCircle().getLatLng())) {
                    marker.setCircle(circle);
                }
            }
        }

        for (int i = 0; i < markerList.size(); i++) {
            if (markerList.get(i).getFloor().equals(mapManager.getCurrentFloor())) {
                if (STATE == 4 || STATE == 7) {
                    if (i == 0) {
                        mapsUIManager.updateCircleColor(markerList.get(i).getCircle(), MARKER_GREEN);
                    } else {
                        mapsUIManager.updateCircleColor(markerList.get(i).getCircle(), MARKER_RED);
                    }
                } else if (STATE == 5 || STATE == 6) {
                    if (i == 0) {
                        mapsUIManager.updateCircleColor(markerList.get(i).getCircle(), MARKER_GREEN);
                    } else if (i == markerList.size()-1) {
                        mapsUIManager.updateCircleColor(markerList.get(i).getCircle(), MARKER_RED);
                    } else {
                        mapsUIManager.updateCircleColor(markerList.get(i).getCircle(), MARKER_YELLOW);
                    }
                }
            }
        }
    }

    private void redrawLinesOnFloor() {
        for (int i = 1; i < markerList.size(); i++) {
            if (markerList.get(i).getFloor().equals(markerList.get(i-1).getFloor())) {
                if (markerList.get(i).getFloor().equals(mapManager.getCurrentFloor())) {
                    lineManager.update(lineList.get(i-1));
                }
            } else {
                lineManager.update(lineList.get(i-1));
            }
        }
    }

    /* Activity Lifecycle */

    private void revive() {
        if (STATE == 3 && markerList.size() == 1 || STATE == 4 && markerList.size() > 1 || STATE == 5 && markerList.size() > 1 || STATE == 6 && markerList.size() > 1 || STATE == 7 && markerList.size() > 1) {
            mapsUIManager.showFloatingActionButton();
            mapsUIManager.setFABText("Start");
        }
    }

    private void kill() {
        /* Stop Sensors */
        if (sensorDataManager != null) {
            sensorDataManager.stopScanning();
        }

        /* Stop Wifi */
        if (wifiDataManager != null && wifiDataManager.isRegistered) {
            wifiDataManager.destroy();
        }

        /* Stop BT */
        if (btManager != null) {
            btManager.stopScanning();
        }

        /* Stop Step Counting */
        stopStepCounting();

        isCollecting = false;
        mapsUIManager.hideCloseButton();

        protobufManager.deleteAllCheckpointFiles(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();

        revive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        revive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        kill();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();

        kill();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapboxMap != null) {
            mapboxMap.removeOnMapLongClickListener(this);
        }
        mapView.onDestroy();

        kill();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    // ----- Step Counting ----- //

    @Override
    public void onStepEvent(int totalSteps, long timestamp) {
        stepTimestamps.add(timestamp);
    }

    private void startStepCounting() {
        fancyStepCounter.clearStepCounter();
        stepTimestamps.clear();
        mSensorManager.registerListener(fancyStepCounter, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    private void stopStepCounting() {
        mSensorManager.unregisterListener(fancyStepCounter);
    }

    /* Wifi Scan Callback */
    @Override
    public void onScanEvent(int scanNum) {

        // called when point mode and scan# = 5
        if (STATE == 3 && scanNum >= PREFERRED_SCANS) {
            mapsUIManager.dismissScanningDialog();
            stopDataCollection();

            runOnUiThread(() -> {
                if (isValidCollection()) {
                    mapsUIManager.setFABText("Start");
                    mapsUIManager.showUploadDialog();
                } else {
                    mapsUIManager.setFABText("Start");
                    mapsUIManager.showCollectionFailedDialog();
                }
            });
        }
    }

    private double getLengthOfHighlightedPath() {

        double totalDistance = 0.0;

        if (markerList.size() < 1) {
            return totalDistance;
        }

        for (int i = 1; i < markerList.size(); i++) {
            LatLng markerA = markerList.get(i-1).getCircle().getLatLng();
            LatLng markerB = markerList.get(i).getCircle().getLatLng();
            totalDistance += markerA.distanceTo(markerB);
        }

        return totalDistance;
    }


    private void startDataCollection() {

        // Check for available disk space
        if (isDiskSpaceEmpty()) {
            mapsUIManager.showDiskSpaceDialog();
            return;
        }

        // Make sure the highlighted path isn't longer than 1Km
        if (getLengthOfHighlightedPath() > 1000) {
            mapsUIManager.showLongPathDialog();
            return;
        }

        isCollecting = true;

        // SENSOR_STATE
        // 0 - WiFi & Bluetooth
        // 1 - WiFi
        // 2 - Bluetooth
        if (SENSOR_STATE == 0) {
            /* WiFi */
            if (!WifiDataManager.isWiFiConnected(getApplicationContext())) {
                Log.i("WiFi", "Wifi is not connected");
            } else {
                Log.i("WiFi", "Wifi is connected");
                if (wifiDataManager != null) {
                    wifiDataManager.clearWiFiData();
                    wifiDataManager.startScanning();
                }
            }

            if (btManager != null) {
                btManager.clearData();
                btManager.startScan();
            }
            if (STATE == 3) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    mapsUIManager.dismissScanningDialog();
                    stopDataCollection();
                    if (isValidCollection()) {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showUploadDialog();
                    } else {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showCollectionFailedDialog();
                    }
                }, 7000);
                mapsUIManager.showScanningDialog();
            }
            else {
                mapsUIManager.showCloseButton();
            }
        } else if (SENSOR_STATE == 1) {
            if (!WifiDataManager.isWiFiConnected(getApplicationContext())) {
                Log.i("WiFi", "Wifi is not connected");
            } else {
                Log.i("WiFi", "Wifi is connected");
                if (wifiDataManager != null) {
                    wifiDataManager.clearWiFiData();
                    wifiDataManager.startScanning();
                }
            }
            if (STATE == 3) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    mapsUIManager.dismissScanningDialog();
                    stopDataCollection();
                    if (isValidCollection()) {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showUploadDialog();
                    } else {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showCollectionFailedDialog();
                    }
                }, 7000);
                mapsUIManager.showScanningDialog();
            }
            else {
                mapsUIManager.showCloseButton();
            }
        } else {
            if (btManager != null) {
                btManager.clearData();
                btManager.startScan();
            }
            if (STATE == 3) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    mapsUIManager.dismissScanningDialog();
                    stopDataCollection();
                    if (isValidCollection()) {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showUploadDialog();
                    } else {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showCollectionFailedDialog();
                    }
                }, 7000);
                mapsUIManager.showScanningDialog();
            }
            else {
                mapsUIManager.showCloseButton();
            }
        }

        /* Sensors */
        if (sensorDataManager != null) {
            sensorDataManager.clearSensorData();
            sensorDataManager.startScanning();
        }

        /* Cell Signals */
        if (gsmListener != null) {
            gsmListener.clearCellData();
            gsmListener.startListening();
        }

        /* GPS Satellites */
        if (gpsListener != null) {
            gpsListener.clearGPSData();
            gpsListener.start();
        }

        /* Step Counting */
        startStepCounting();

        /* Clear visited markers */
        clearVisited();

        /* Stop Timer */
        long START_TIME = SystemClock.elapsedRealtimeNanos();

        /* If point mode > set the timestamp to the start time */
        if (STATE == 3) {
            markerList.get(0).setTimestamp(START_TIME);
        }
    }

    private void stopDataCollection() {
        isCollecting = false;
        mapsUIManager.hideCloseButton();

        // SENSOR_STATE
        // 0 - WiFi & Bluetooth
        // 1 - WiFi
        // 2 - Bluetooth
        if (SENSOR_STATE == 0) {
            /* WiFi */
            if (wifiDataManager != null) {
                wifiDataManager.stopScanning();
            }

            if (btManager != null) {
                btManager.stopScanning();
            }
        } else if (SENSOR_STATE == 1) {
            if (wifiDataManager != null) {
                wifiDataManager.stopScanning();
            }
        } else {
            if (btManager != null) {
                btManager.stopScanning();
            }
        }

        /* Sensors */
        if (sensorDataManager != null) {
            sensorDataManager.stopScanning();
        }

        /* Cell Signals */
        if (gsmListener != null) {
            gsmListener.stopListening();
        }

        /* GPS Satellites */
        if (gpsListener != null) {
            gpsListener.stop();
        }

        /* Step Counting */
        stopStepCounting();

        /* Set the first markers altitude */
        if (sensorDataManager.firstPressureValue != null) {
            markerList.get(0).setAltitude(getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sensorDataManager.firstPressureValue));
        } else {
            // errror! no altitude!
        }
    }

    private void checkForStopCriteria() {

        int visitedIndexReturnValue = markerList.size();
        boolean hasQuit = false;
        for (MapMarker mapMarker : markerList) {
            if (!mapMarker.getVisited()) {
                int curr_ind = markerList.indexOf(mapMarker);
                mapMarker.setVisited(true);
                if (sensorDataManager.lastPressureValue != null) {
                    mapMarker.setAltitude(getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sensorDataManager.lastPressureValue));
                }
                mapMarker.setTimestamp(SystemClock.elapsedRealtimeNanos());
                visitedIndexReturnValue = curr_ind + 1;
                if (curr_ind > 0) {
                    MapMarker previousMapMarker = markerList.get(curr_ind-1);
                    double dist = mapMarker.getCircle().getLatLng().distanceTo(previousMapMarker.getCircle().getLatLng());
                    double velocity = dist / ((SystemClock.elapsedRealtimeNanos() - previousMapMarker.getTimestamp()) / 1000000000.0);
                    if (velocity < 0.5 || velocity > 2.5) {
                        TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
                        if (velocity < 0.5) {
                            dialog_body.setText("You are walking too slow. Please try again.");
                        } else {
                            dialog_body.setText("You are walking too fast. Please try again.");
                        }
                        mapsUIManager.setFABText("Start");
                        stopDataCollection();
                        mapsUIManager.showCollectionFailedDialog();
                        hasQuit = true;
                    }
                }
                break;
            }
        }

        if (!hasQuit) {
            int remaining = markerList.size() - visitedIndexReturnValue;
            switch (remaining) {
                case 0:
                    stopDataCollection();
                    if (isValidCollection()) {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showUploadDialog();
                    } else {
                        mapsUIManager.setFABText("Start");
                        mapsUIManager.showCollectionFailedDialog();
                    }
                    break;
                case 1:
                    mapsUIManager.setFABText("Finish");
                    break;
                default:
                    mapsUIManager.setFABText("Check In");
                    break;
            }
        }
    }

    private Boolean isValidCollection() {

        boolean validCollection = true;

        // SENSOR_STATE
        // 0 - WiFi & Bluetooth
        // 1 - WiFi
        // 2 - Bluetooth

        // STATE
        // 0 - Idle
        // 1 - Add Marker
        // 2 - Delete Marker
        // 3 - Outlining Point
        // 4 - Outlining Path
        // 5 - Outlining Multipath

        Log.i("COLLECTED", "ACC" + sensorDataManager.numScansAcc);
        Log.i("COLLECTED", "GYRO" + sensorDataManager.numScansGyro);
        Log.i("COLLECTED", "MAG" + sensorDataManager.numScansMag);
        Log.i("COLLECTED", "WIFI" + wifiDataManager.scanNum);
        Log.i("COLLECTED", "BT" + btManager.numScans);
        Log.i("COLLECTED", "CELL" + gsmListener.scanNum);

        if (STATE == 3) {

            if (SENSOR_STATE == 0) {
                if ((wifiDataManager.scanNum > 0) || (btManager.numScans > 0)) {
                    return true;
                } else {
                    TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
                    dialog_body.setText("No Wifi/Bluetooth data collected");
                    return false;
                }
            } else if (SENSOR_STATE == 1) {
                if (wifiDataManager.scanNum > 0) {
                    return true;
                } else {
                    TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
                    dialog_body.setText("No Wifi/Bluetooth data collected");
                    return false;
                }
            } else if (SENSOR_STATE == 2) {
                if (btManager.numScans > 0) {
                    return true;
                } else {
                    TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
                    dialog_body.setText("No Wifi/Bluetooth data collected");
                    return false;
                }
            } else {
                TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
                dialog_body.setText("Something went wrong");
                return false;
            }
        }

        if (STATE == 4 || STATE == 5 || STATE == 6 || STATE == 7) {
            if (stepTimestamps.isEmpty()) {
                validCollection = false;
            }
        }
        double total_travelled_distance = 0;
        for (int i = 1; i<markerList.size(); i++) {

            MapMarker previousMapMarker = markerList.get(i-1);
            MapMarker currentMapMarker = markerList.get(i);

            double dist = currentMapMarker.getCircle().getLatLng().distanceTo(previousMapMarker.getCircle().getLatLng());
            total_travelled_distance+=dist;
        }
        double stepsPerMeter = stepTimestamps.size() / total_travelled_distance;
        if (stepsPerMeter < 0.7 || stepsPerMeter > 3) {
            validCollection = false;
            TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
            dialog_body.setText("Not enough Steps were collected. Please try again.");
        }

        // UNCOMMENT TO PREVENT EMPTY SCANS
//        if (SENSOR_STATE == 0) {
//            if (wifiDataManager.rssItems.isEmpty() && btManager.btItems.isEmpty()) {
//                validCollection = false;
//            }
//
//        } else if (SENSOR_STATE == 1) {
//            if (wifiDataManager.rssItems.isEmpty()) {
//                validCollection = false;
//            }
//        } else {
//            if (btManager.btItems.isEmpty()) {
//                validCollection = false;
//            }
//        }

//        Log.i("INFOYO", "" + sensorDataManager.SCAN_FREQUENCY_FLAG);
//        Log.i("INFOYO", "" + frequencyFlagToFrequency(sensorDataManager.SCAN_FREQUENCY_FLAG));
//        double scans_per_second = frequencyFlagToFrequency(sensorDataManager.SCAN_FREQUENCY_FLAG) / 1000000000.0;
//        double seconds = (markerList.get(markerList.size()-1).getTimestamp() - markerList.get(0).getTimestamp()) / 1000000000.0;
//        double expected_number_of_scans = seconds / scans_per_second;
//        Log.i("INFOYO", "" + scans_per_second);
//        Log.i("INFOYO", "" + seconds);
//        Log.i("INFOYO", "" + expected_number_of_scans);
//        double scan_thres = 0.9;
//
//        for (int i=0; i<sensorDataManager.accData.size(); i++) {
//            SensorData3D currentData = sensorDataManager.accData.get(i);
//            if (i > 0) {
//                SensorData3D previousData = sensorDataManager.accData.get(i-1);
//                if (currentData.timestamp - previousData.timestamp > 1000000000.0) {
//                    // more than 1s between scans
//                }
//            }
//        }
//
//        if (sensorDataManager.accData.size() < expected_number_of_scans * scan_thres || sensorDataManager.accData.size() > expected_number_of_scans * (2.0 - scan_thres)) {
//            validCollection = false;
//            TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
//            dialog_body.setText("ACC - Exp: " + expected_number_of_scans + ", REC: " + sensorDataManager.accData.size());
//        } else if (sensorDataManager.gyroData.size() < expected_number_of_scans * scan_thres || sensorDataManager.gyroData.size() > expected_number_of_scans * (2.0 - scan_thres)) {
//            validCollection = false;
//            TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
//            dialog_body.setText("GYRO - Exp: " + expected_number_of_scans + ", REC: " + sensorDataManager.gyroData.size());
//        } else if (sensorDataManager.magData.size() < expected_number_of_scans * scan_thres || sensorDataManager.magData.size() > expected_number_of_scans * (2.0 - scan_thres)) {
//            validCollection = false;
//            TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
//            dialog_body.setText("MAG - Exp: " + expected_number_of_scans + ", REC: " + sensorDataManager.magData.size());
//        } else if (sensorDataManager.lightData.size() < expected_number_of_scans * scan_thres || sensorDataManager.lightData.size() > expected_number_of_scans * (2.0 - scan_thres)) {
//            validCollection = false;
//            TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
//            dialog_body.setText("LGHT - Exp: " + expected_number_of_scans + ", REC: " + sensorDataManager.lightData.size());
//        } else if (sensorDataManager.linearAccData.size() < expected_number_of_scans * scan_thres || sensorDataManager.linearAccData.size() > expected_number_of_scans * (2.0 - scan_thres)) {
//            validCollection = false;
//            TextView dialog_body = mapsUIManager.failedDialog.findViewById(R.id.failed_dialog_body_textview);
//            dialog_body.setText("LACC - Exp: " + expected_number_of_scans + ", REC: " + sensorDataManager.linearAccData.size());
//        }

        return validCollection;
    }
}