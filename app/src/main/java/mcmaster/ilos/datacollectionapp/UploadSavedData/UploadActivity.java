package mcmaster.ilos.datacollectionapp.UploadSavedData;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.SaveFile;
import mcmaster.ilos.datacollectionapp.LoginScreen.LoginManager;
import mcmaster.ilos.datacollectionapp.R;
import mcmaster.ilos.datacollectionapp.Utils.Config;
import mcmaster.ilos.datacollectionapp.Utils.FileUploader;
import mcmaster.ilos.datacollectionapp.Utils.RecyclerTouchListener;

import static mcmaster.ilos.datacollectionapp.BuildingsScreen.BuildingManager.getFileRecords;
import static mcmaster.ilos.datacollectionapp.BuildingsScreen.BuildingManager.getFileWithFilename;
import static mcmaster.ilos.datacollectionapp.BuildingsScreen.BuildingManager.removeFileRecord;
import static mcmaster.ilos.datacollectionapp.Utils.ProtobufManager.deleteFileFromStorage;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.formatSize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getAvailableExternalMemorySize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getAvailableInternalMemorySize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getTotalExternalMemorySize;
import static mcmaster.ilos.datacollectionapp.Utils.Storage.getTotalInternalMemorySize;
import static org.apache.commons.numbers.core.Precision.round;

/* Activity used for uploading saved traces to the server */
public class UploadActivity extends AppCompatActivity implements DialogInterface.OnCancelListener {

    private RecyclerView recyclerView;
    private static ArrayList<Upload_Item_Model> uploadedItemModels;
    private Upload_RecyclerView_Adapter adapter;
    private ActionMode mActionMode;

    // A dialog to show the local storage
    Dialog storageDialog;
    TextView textPercent;

    Dialog noInternetDialog;

    // A fancy animation to show a moving ring when the storage dialog is shown
    DecoView arcView;
    Dialog confirmDeleteDialog;

    // A searchView so the user can search for traces
    SearchView mSearchView;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        loginManager = new LoginManager(this);

        confirmDeleteDialog = new Dialog(this, R.style.NewDialog);
        confirmDeleteDialog.setContentView(R.layout.delete_confirmation_dialog);
        confirmDeleteDialog.setCancelable(false);
        confirmDeleteDialog.setCanceledOnTouchOutside(false);

        Button dialogConfirmDeleteButton = confirmDeleteDialog.findViewById(R.id.dialog_delete_button);
        dialogConfirmDeleteButton.setOnClickListener(v -> {
            deleteRows();
            confirmDeleteDialog.cancel();

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.collapseActionView();
        });

        Button dialogCancelButton = confirmDeleteDialog.findViewById(R.id.dialog_cancel_button);
        dialogCancelButton.setOnClickListener(v -> confirmDeleteDialog.cancel());

        noInternetDialog = new Dialog(this, R.style.NewDialog);
        noInternetDialog.setContentView(R.layout.internet_dialog);
        noInternetDialog.setCancelable(false);
        noInternetDialog.setCanceledOnTouchOutside(false);

        Button failedDialogButton = noInternetDialog.findViewById(R.id.failed_dialog_ok);
        failedDialogButton.setOnClickListener(v -> dismissInternetDialog());

        storageDialog = new Dialog(this, R.style.NewDialog);
        storageDialog.setContentView(R.layout.storage_dialog);
        storageDialog.setCancelable(true);
        storageDialog.setCanceledOnTouchOutside(true);
        storageDialog.setOnCancelListener(this);

        arcView = storageDialog.findViewById(R.id.dynamicArcView);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Upload scans");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        populateRecyclerView();
        implementRecyclerViewClickListeners();

        final FloatingActionButton fab = findViewById(R.id.floating_action_button);
        fab.hide();
        fab.setOnClickListener(view -> uploadFiles());

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
    }

    /* No Internet Dialog */

    void showInternetDialog() {
        noInternetDialog.show();
    }

    void dismissInternetDialog() {
        noInternetDialog.dismiss();
    }

    private void populateRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Gets a list of all files listed in local storage and displays them
        uploadedItemModels = new ArrayList<>();
        String token = loginManager.loadToken();
        ArrayList<SaveFile> saveFiles = getFileRecords(token, this);
        for (SaveFile f : saveFiles) {

            String filename = f.getFilename();
            String collectType = filename.split("_")[1];
            String mainText = f.getBuilding();
            String secondText = "Fl. " + f.getFloor() + " • (" + f.getSize() + ")";

            DateFormat readFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            Date date;
            try {
                date = readFormat.parse(filename.split("_")[2]);
                DateFormat displayFormat = new SimpleDateFormat("dd/MM/yy, hh:mma", Locale.getDefault());
                String formattedDate = displayFormat.format(date);
                secondText = "Fl. " + f.getFloor() + " • (" + f.getSize() + ") • (" + formattedDate + ")";
            } catch (ParseException e) {
                Log.e("CRASH", "failed to set text", e);
            }

            uploadedItemModels.add(new Upload_Item_Model(mainText, secondText, false, collectType, filename));
        }

        adapter = new Upload_RecyclerView_Adapter(this, uploadedItemModels);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void implementRecyclerViewClickListeners() {
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, (view, position) -> onListItemSelect(position)));
    }

    public void showConfirmationDialog() {
        confirmDeleteDialog.show();
    }

    private void onListItemSelect(int position) {
        adapter.toggleSelection(position);
        boolean hasCheckedItems = adapter.getSelectedCount() > 0;

        if (hasCheckedItems && mActionMode == null) {
            FloatingActionButton fab = findViewById(R.id.floating_action_button);
            fab.show();
            mActionMode = this.startSupportActionMode(new Upload_Toolbar_ActionMode_Callback(adapter, this));
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
                Upload_Item_Model selectedItem = adapter.getFilteredData().get(selected.keyAt(i));
                String title = selectedItem.getFilename();
                boolean deleted = deleteFileFromStorage(title, this);
                if (deleted) {
                    uploadedItemModels.remove(selectedItem);
                }
                adapter.notifyDataSetChanged();
            }
        }
        mActionMode.finish();
        adapter.updateDataset(uploadedItemModels);
        mSearchView.setQuery("", true);
    }

    public void selectAllRows() {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            adapter.selectView(i, true);
            adapter.notifyDataSetChanged();
        }
        mActionMode.setTitle(adapter.getSelectedCount() + " selected");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // close this activity and return to preview activity (if there is any)
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // Displays animated dialog and tells user how much available storage space they have
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
            public void onSeriesItemDisplayProgress(float percentComplete) {}
        });

        int series1Index = arcView.addSeries(seriesItem1);
        arcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true).setDelay(400).setDuration(1000).build());
        arcView.addEvent(new DecoEvent.Builder(totalSize-availableSize).setIndex(series1Index).setDelay(1800).build());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        textPercent.setText("");
        arcView.deleteAll();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        final MenuItem mStorage = menu.findItem(R.id.action_stoarge);
        MenuItem mSearch = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearch.getActionView();
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

    /* Creates a background thread, and loops through all of the selected files, and uploads them one at a time */
    public void uploadFiles() {

        String uploadURL = Config.loadProperties(this).getProperty("FILE_UPLOAD_URL");
        String token = loginManager.loadToken();

        ArrayList<Upload_Item_Model> selectedItems = new ArrayList<>();
        for (int i = 0; i < adapter.getSelectedCount(); i++) {
            Upload_Item_Model selectedItem = adapter.getFilteredData().get(adapter.getSelectedIds().keyAt(i));
            runOnUiThread(() -> selectedItem.setUploading(true));
            selectedItems.add(selectedItem);
        }

        mActionMode.finish();

        new Thread() {

            @Override
            public void run() {

                for (int i = 0; i < selectedItems.size(); i++) {
                    Upload_Item_Model selectedItem = selectedItems.get(i);

                    String title = selectedItem.getFilename();
                    SaveFile file = getFileWithFilename(token, title, UploadActivity.this);

                    final HashMap<String, String> map = new HashMap<>();

                    map.put("buildingName", file.getBuilding());
                    map.put("floorLevel", file.getFloor());
                    map.put("otherFloorLevel", file.getOtherFloor());
                    map.put("fileName", file.getFilename());
                    map.put("errorCode", "0");
                    map.put("Authorization", "Token " + token);

                    String filePath = new File(getFilesDir(),"data/").getAbsolutePath() + "/" + file.getFilename();
                    FileUploader.upload(uploadURL, new File(filePath), map, file, new FileUploader.FileUploadListener() {

                        @Override
                        public void onFail() {
                            runOnUiThread(() -> {
                                selectedItem.setUploading(false);
                                selectedItem.setUploaded(false);
                                adapter.notifyDataSetChanged();
                                UploadActivity.this.showInternetDialog();
                            });
                        }

                        @Override
                        public void onProgress(long pro, double percent) {
                            Log.i("upload", "upload percent: " + percent);
                        }

                        @Override
                        public void onFinish(int code, String res, Map<String, List<String>> headers, SaveFile saveFile) {
                            Log.i("upload", "return code: " + code);
                            Log.i("upload", "res: " + res);
                            Log.i("upload", "headers " + headers.toString());

                            runOnUiThread(() -> {
                                selectedItem.setUploading(false);
                                selectedItem.setUploaded(true);

                                Boolean fileDeletedFromStorage = deleteFileFromStorage(saveFile.getFilename(), UploadActivity.this);
                                Boolean fileDeletedFromDatabase = removeFileRecord(token, saveFile, UploadActivity.this);
                                if (fileDeletedFromStorage && fileDeletedFromDatabase) {
                                    adapter.removeItem(selectedItem);
                                }
                            });
                        }
                    });
                }
            }
        }.start();
    }
}
