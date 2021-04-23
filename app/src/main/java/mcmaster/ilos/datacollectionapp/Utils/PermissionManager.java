package mcmaster.ilos.datacollectionapp.Utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import mcmaster.ilos.datacollectionapp.R;

/* A class used to handle the checking of permissions */
public class PermissionManager {

    private static final int REQUEST_CODE = 101;
    private static final boolean SHOW_SYSTEM_SETTINGS = true;

    private static PermissionManager mPermission;
    private IPermissionResult mPermissionResult;

    private AlertDialog mPermissionDialog;

    private PermissionManager() {

    }

    public static PermissionManager getInstance() {
        if (mPermission == null) {
            synchronized (PermissionManager.class) {
                if (mPermission == null) {
                    mPermission = new PermissionManager();
                }
            }
        }
        return mPermission;
    }

    public void checkPermissions(Activity context, String[] permissions, @NonNull IPermissionResult permissionResult) {
        mPermissionResult = permissionResult;

        List<String> reqPermList = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                reqPermList.add(perm);
            }
        }

        if (reqPermList.size() > 0) {
            ActivityCompat.requestPermissions(context, reqPermList.toArray(new String[0]), REQUEST_CODE);
        } else {
            permissionResult.permissionSucceed();
        }
    }


    public void onRequestPermissionResult(Activity context, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        boolean hasPermissionDismiss = false;
        if (REQUEST_CODE == requestCode) {
            for (int grantResult : grantResults) {
                if (grantResult == -1) {
                    hasPermissionDismiss = true;
                }
            }

            if (hasPermissionDismiss) {
                if (SHOW_SYSTEM_SETTINGS) {
                    showSystemPermissionsSettingDialog(context);
                } else {
                    mPermissionResult.permissionFailed();
                }
            } else {
                mPermissionResult.permissionSucceed();
            }
        }
    }

    private void showSystemPermissionsSettingDialog(final Activity context) {
        final String mPackName = context.getPackageName();
        if (mPermissionDialog == null) {
            mPermissionDialog = new AlertDialog.Builder(context, R.style.PermissionsDialogTheme)
                    .setMessage("iLOS needs access to you devices location and memory in order to function properly. Please allow this in Settings to continue.")
                    .setPositiveButton("Settings", (dialog, which) -> {
                        cancelPermissionDialog();
                        Uri packageURI = Uri.parse("package:" + mPackName);
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                        context.startActivity(intent);
                        context.finish();
                    })
                    .setNegativeButton("Back", (dialog, which) -> {
                        cancelPermissionDialog();
                        mPermissionResult.permissionFailed();
                    })
                    .create();
        }
        mPermissionDialog.show();
    }


    private void cancelPermissionDialog() {
        if (mPermissionDialog != null) {
            mPermissionDialog.cancel();
            mPermissionDialog = null;
        }
    }

    public interface IPermissionResult {
        void permissionSucceed();
        void permissionFailed();
    }
}
