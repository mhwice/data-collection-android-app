package mcmaster.ilos.datacollectionapp.DownloadMaps;

import android.content.Context;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import mcmaster.ilos.datacollectionapp.R;

public class Download_Toolbar_ActionMode_Callback implements ActionMode.Callback {

    private Context context;
    private MapDownloadActivity activity;
    private Download_RecyclerView_Adapter recyclerView_adapter;
    private ArrayList<Download_Item_Model> message_models;

    Download_Toolbar_ActionMode_Callback(Context context, Download_RecyclerView_Adapter recyclerView_adapter, ArrayList<Download_Item_Model> message_models, MapDownloadActivity activity) {
        this.context = context;
        this.recyclerView_adapter = recyclerView_adapter;
        this.message_models = message_models;
        this.activity = activity;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

        menu.findItem(R.id.action_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.action_selectall).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                activity.removeDownloads();
                mode.finish();
                break;
            case R.id.action_selectall:
                activity.selectAllRows();
                break;
            case R.id.action_stoarge:
                activity.showStorageDialog();
                break;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        recyclerView_adapter.removeSelection();
        activity.setNullToActionMode();
    }
}
