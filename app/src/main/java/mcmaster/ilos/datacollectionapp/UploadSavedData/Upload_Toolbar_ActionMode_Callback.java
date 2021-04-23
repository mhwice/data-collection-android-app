package mcmaster.ilos.datacollectionapp.UploadSavedData;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import mcmaster.ilos.datacollectionapp.R;

/* Defines what happens when the actionmode items are pressed */
public class Upload_Toolbar_ActionMode_Callback implements ActionMode.Callback {

    private UploadActivity activity;
    private Upload_RecyclerView_Adapter recyclerView_adapter;

    Upload_Toolbar_ActionMode_Callback(Upload_RecyclerView_Adapter recyclerView_adapter, UploadActivity activity) {
        this.recyclerView_adapter = recyclerView_adapter;
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
                activity.showConfirmationDialog();
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
