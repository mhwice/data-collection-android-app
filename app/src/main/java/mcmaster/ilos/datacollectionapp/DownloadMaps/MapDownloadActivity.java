package mcmaster.ilos.datacollectionapp.DownloadMaps;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import mcmaster.ilos.datacollectionapp.BuildingsScreen.BuildingManager;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.Map;
import mcmaster.ilos.datacollectionapp.LoginScreen.LoginManager;
import mcmaster.ilos.datacollectionapp.R;
import mcmaster.ilos.datacollectionapp.Utils.Config;
import mcmaster.ilos.datacollectionapp.Utils.DownloadMapFile;
import mcmaster.ilos.datacollectionapp.Utils.InternetManager;
import mcmaster.ilos.datacollectionapp.Utils.RecyclerClick_Listener;
import mcmaster.ilos.datacollectionapp.Utils.RecyclerTouchListener;

import static java.util.Collections.*;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.formatSize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getAvailableExternalMemorySize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getAvailableInternalMemorySize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getFreeDiskSpace;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getTotalExternalMemorySize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getTotalInternalMemorySize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.unformatSize;

public class MapDownloadActivity extends AppCompatActivity implements DialogInterface.OnCancelListener {

    private RecyclerView recyclerView;
    private static ArrayList<Download_Item_Model> downloadItemModels;
    private Download_RecyclerView_Adapter adapter;
    private ActionMode mActionMode;

    private BuildingManager buildingManager;
    private InternetManager internetManager;
    private LoginManager loginManager;

    Dialog noInternetDialog;
    Dialog diskSpaceDialog;
    Dialog storageDialog;
    TextView textPercent;
    DecoView arcView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_download);

        buildingManager = new BuildingManager(this);
        internetManager = new InternetManager();
        loginManager = new LoginManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        noInternetDialog = new Dialog(this, R.style.NewDialog);
        noInternetDialog.setContentView(R.layout.internet_dialog);
        noInternetDialog.setCancelable(false);
        noInternetDialog.setCanceledOnTouchOutside(false);

        Button failedDialogButton = noInternetDialog.findViewById(R.id.failed_dialog_ok);
        failedDialogButton.setOnClickListener(v -> dismissInternetDialog());

        diskSpaceDialog = new Dialog(this, R.style.NewDialog);
        diskSpaceDialog.setContentView(R.layout.disk_space_dialog);
        diskSpaceDialog.setCancelable(false);
        diskSpaceDialog.setCanceledOnTouchOutside(false);

        Button noDiskSpaceDialogButton = diskSpaceDialog.findViewById(R.id.failed_dialog_ok);
        noDiskSpaceDialogButton.setOnClickListener(v -> dismissDiskSpaceDialog());

        Objects.requireNonNull(getSupportActionBar()).setTitle("Download maps");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        pop();
        implementRecyclerViewClickListeners();

        final FloatingActionButton fab = findViewById(R.id.floating_action_button);
        fab.hide();
        fab.setOnClickListener(view -> {

            String token = loginManager.loadToken();

            ArrayList<Integer> indiciesList = new ArrayList<>();
            for (int i = 0; i < adapter.getSelectedCount(); i++) {
                indiciesList.add(adapter.getSelectedIds().keyAt(i));
            }

            mActionMode.finish();

            for (int i = 0; i < indiciesList.size(); i++) {
                int indicies = indiciesList.get(i);
                ArrayList<Download_Item_Model> filteredData = adapter.getFilteredData();

                Download_Item_Model selectedItem = filteredData.get(indicies);
                String bname = selectedItem.getTitle();
                String[] splitSubTitle = selectedItem.getSubTitle().split(" • ");
                String floor = splitSubTitle[0].substring(4);
                String size = splitSubTitle[1].replace("(", "").replace(")","");

                boolean mapAlreadyExists = buildingManager.isMapAlreadyDownloaded(token, new Map(bname, floor, size));
                Log.i("EXISTS", "" + mapAlreadyExists);
                if (mapAlreadyExists) {
                    continue;
                }

                if (getFreeDiskSpace() < unformatSize(size)) {
                    showDiskSpaceDialog();
                    continue;
                }

                selectedItem.setDownloading(true);
                adapter.notifyDataSetChanged();

                File dataFolder = new File(getFilesDir(),"maps/");
                try {
                    if (!dataFolder.exists() && !dataFolder.mkdir()) {
                        Log.e("CRASH", "Failed to make directory to hold downloaded maps");
                        return;
                    }
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to create folder to hold maps");
                    return;
                }
                String folderPath = dataFolder.getAbsolutePath();
                String urlEndpoint = Config.loadProperties(MapDownloadActivity.this).getProperty("MAP_DOWNLOAD_URL");

                DownloadMapFile asyncTask = new DownloadMapFile(output -> {

                    String rowNumber = output.getRowNumber();
                    Boolean downloadSuccess = output.getSuccess();
                    Download_Item_Model selectedItem1 = filteredData.get(Integer.parseInt(rowNumber));

                    if (downloadSuccess) {
                        Boolean saveSuccess = buildingManager.saveDownloadedMapRecord(token, new Map(bname, floor, size));
                        if (saveSuccess) {
                            selectedItem1.setDownloaded(true);
                        } else {
                            selectedItem1.setDownloaded(false);
                        }
                    } else {
                        showInternetDialog();
                        selectedItem1.setDownloaded(false);
                    }
                    selectedItem1.setDownloading(false);
                    adapter.notifyDataSetChanged();
                });

                asyncTask.execute(urlEndpoint, token, bname, floor, folderPath, String.valueOf(indicies));
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                if (adapter.getSelectedCount() > 0) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        fab.show();
                    }
                }

                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

                if (adapter.getSelectedCount() > 0) {
                    if (dy > 0 || dy < 0 && fab.isShown()) {
                        fab.hide();
                    }
                }

                super.onScrolled(recyclerView, dx, dy);
            }
        });

        storageDialog = new Dialog(this, R.style.NewDialog);
        storageDialog.setContentView(R.layout.storage_dialog);
        storageDialog.setCancelable(true);
        storageDialog.setCanceledOnTouchOutside(true);
        storageDialog.setOnCancelListener(this);

        arcView = storageDialog.findViewById(R.id.dynamicArcView);

        setInitialUI();
    }

    /* No Internet Dialog */

    void showInternetDialog() {
        noInternetDialog.show();
    }

    void dismissInternetDialog() {
        noInternetDialog.dismiss();
    }

    /* No Disk Space Dialog */

    void showDiskSpaceDialog() {
        diskSpaceDialog.show();
    }

    void dismissDiskSpaceDialog() {
        diskSpaceDialog.dismiss();
    }

    public void pop() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String token = loginManager.loadToken();
        ArrayList<Map> serverMapList = new ArrayList<>();
        ArrayList<Map> downloadedMapList = buildingManager.getDownloadedMapList(token);

        if (internetManager.isNetworkAvailable(this)) {
            serverMapList = buildingManager.getServerMapList(token);
        }

        downloadItemModels = new ArrayList<>();
        for (Map dMap : downloadedMapList) {
            dMap.setDownloaded(true);
        }

        ArrayList<Map> undownloadedMapList = new ArrayList<>();
        for (Map serverMap : serverMapList) {
            boolean unadded = true;
            for (Map downloadedMap : downloadedMapList) {

            	// Not OS 23 compatible
//                if (Comparator.comparing(Map::getName).thenComparing(Map::getFloorNum).compare(downloadedMap, serverMap) == 0) {
//                    unadded = false;
//                }

				// OS 23 Compatible
				if (downloadedMap.getName().equals(serverMap.getName()) && downloadedMap.getFloorNum().equals(serverMap.getFloorNum())) {
					unadded = false;
				}
            }
            if (unadded) {
                serverMap.setDownloaded(false);
                undownloadedMapList.add(serverMap);
            }
        }

        downloadedMapList.addAll(undownloadedMapList);

        // Not OS 23 Compatible
		// downloadedMapList.sort(Comparator.comparing(Map::getName).thenComparing(Map::getFloorNum));

		// OS 23 Compatible
		Collections.sort(downloadedMapList, new MapComparator());

        for (Map m : downloadedMapList) {
            downloadItemModels.add(new Download_Item_Model(m.getName(), "Fl. " + m.getFloorNum() + " • (" + m.getSize() + ")", m.getDownloaded()));
        }

        adapter = new Download_RecyclerView_Adapter(this, downloadItemModels);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

	public class MapComparator implements Comparator<Map> {
		public int compare(Map left, Map right) {
			if (left.getName().equals(right.getName())) {
				return left.getFloorNum().compareTo(right.getFloorNum());
			} else {
				return left.getName().compareTo(right.getName());
			}
		}
	}

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    private void implementRecyclerViewClickListeners() {
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, (view, position) -> onListItemSelect(position)));
    }

    private void onListItemSelect(int position) {
        adapter.toggleSelection(position);
        boolean hasCheckedItems = adapter.getSelectedCount() > 0;

        if (hasCheckedItems && mActionMode == null) {
            FloatingActionButton fab = findViewById(R.id.floating_action_button);
            fab.show();
            mActionMode = this.startSupportActionMode(new Download_Toolbar_ActionMode_Callback(this, adapter, downloadItemModels, this));
        } else if (!hasCheckedItems && mActionMode != null) {
            mActionMode.finish();
        }
        if (mActionMode != null) {
            mActionMode.setTitle(adapter.getSelectedCount() + " selected");
        }
    }

    public void setNullToActionMode() {
        if (mActionMode != null) {
            FloatingActionButton fab = findViewById(R.id.floating_action_button);
            fab.hide();
            mActionMode = null;
        }
    }

    public void deleteRows() {
        SparseBooleanArray selected = adapter.getSelectedIds();

        for (int i = (selected.size() - 1); i >= 0; i--) {
            if (selected.valueAt(i)) {
                downloadItemModels.remove(selected.keyAt(i));
                adapter.notifyDataSetChanged();
            }
        }
        mActionMode.finish();
        adapter.updateDataset(downloadItemModels);
    }

    public void removeDownloads() {
        SparseBooleanArray selected = adapter.getSelectedIds();
        String token = loginManager.loadToken();

        for (int i = (selected.size() - 1); i >= 0; i--) {
            if (selected.valueAt(i)) {
                Download_Item_Model selectedItem = adapter.getFilteredData().get(selected.keyAt(i));
                String bname = selectedItem.getTitle();
                String[] splitSubTitle = selectedItem.getSubTitle().split(" • ");
                String floor = splitSubTitle[0].substring(4);
                String size = splitSubTitle[1].replace("(", "").replace(")","");
                boolean removeSuccess = buildingManager.removeDownloadedMapRecord(token, new Map(bname, floor, size));
                if (removeSuccess) {
                    selectedItem.setDownloaded(false);
                    adapter.notifyDataSetChanged();
                }
            }
        }
        mActionMode.finish();
        adapter.updateDataset(downloadItemModels);
    }


    public void selectAllRows() {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            adapter.selectView(i, true);
            adapter.notifyDataSetChanged();
        }

        mActionMode.setTitle(adapter.getSelectedCount() + " selected");
    }

//    public void unselectAllRows() {
//        for (int i = 0; i < adapter.getItemCount(); i++) {
//            adapter.selectView(i, false);
//            adapter.notifyDataSetChanged();
//        }
//
////        mActionMode.setTitle(adapter.getSelectedCount() + " selected");
//    }

    public void setInitialUI() {
        Typeface avenirnext_demibold = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/AvenirNext-DemiBold.ttf");
        Typeface avenirnext_medium = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/AvenirNext-Medium.ttf");

        TextView storageHeader = storageDialog.findViewById(R.id.storage_header_text);
        TextView storageBody = storageDialog.findViewById(R.id.storage_body_text);
        TextView storageRatio = storageDialog.findViewById(R.id.storage_ratio_text);

        storageHeader.setTypeface(avenirnext_demibold);
        storageBody.setTypeface(avenirnext_medium);
        storageRatio.setTypeface(avenirnext_medium);
    }

    public void showStorageDialog() {
        long aInternal = getAvailableInternalMemorySize();
        long tInternal = getTotalInternalMemorySize();
        long aExternal = getAvailableExternalMemorySize();
        long tExternal = getTotalExternalMemorySize();

        TextView storageRatio = storageDialog.findViewById(R.id.storage_ratio_text);
        String storageRatioText = formatSize(tInternal-aInternal) + " / " + formatSize(tInternal);
        storageRatio.setText(storageRatioText);

        float totalSize;
        float availableSize;
        if (tExternal == 0) {
            totalSize = (float) tInternal;
            availableSize = (float) aInternal;
        } else {
            totalSize = (float) tExternal;
            availableSize = (float) aExternal;
        }

        storageDialog.show();

        // Create background track
        arcView.addSeries(new SeriesItem.Builder(Color.argb(255, 218, 218, 218))
                .setRange(0, totalSize, totalSize)
                .setInitialVisibility(false)
                .setLineWidth(32f)
                .build());

        // Create data series track
        SeriesItem seriesItem1 = new SeriesItem.Builder(getColor(R.color.raspberry))
                .setInitialVisibility(false)
                .setRange(0, totalSize, 0)
                .setLineWidth(32f)
                .build();

        textPercent = storageDialog.findViewById(R.id.textPercentage);
        textPercent.setText("");
        seriesItem1.addArcSeriesItemListener(new SeriesItem.SeriesItemListener() {
            @Override
            public void onSeriesItemAnimationProgress(float percentComplete, float currentPosition) {
                if (percentComplete > 0.001 && percentComplete != 1.0) {
                    float percentFilled = ((currentPosition - seriesItem1.getMinValue()) / (seriesItem1.getMaxValue() - seriesItem1.getMinValue()));
                    String textContent = (int) round(percentFilled*100, 0) + "%";
                    textPercent.setText(textContent);
                }
            }

            @Override
            public void onSeriesItemDisplayProgress(float percentComplete) {

            }
        });

        int series1Index = arcView.addSeries(seriesItem1);

        arcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true).setDelay(400).setDuration(1000).build());
        arcView.addEvent(new DecoEvent.Builder(totalSize-availableSize).setIndex(series1Index).setDelay(1800).build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        final MenuItem mStorage = menu.findItem(R.id.action_stoarge);
        MenuItem mSearch = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) mSearch.getActionView();
        mSearchView.setQueryHint("");

        View v = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundColor(Color.TRANSPARENT);

        mSearchView.setOnSearchClickListener(v1 -> {
            mStorage.setVisible(false);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        });

        mSearchView.setOnCloseListener(() -> {
            mStorage.setVisible(true);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            return false;
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        mStorage.setOnMenuItemClickListener(item -> {
            showStorageDialog();

            return false;
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        textPercent.setText("");
        arcView.deleteAll();
    }
}