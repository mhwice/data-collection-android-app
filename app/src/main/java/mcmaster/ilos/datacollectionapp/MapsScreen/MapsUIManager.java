package mcmaster.ilos.datacollectionapp.MapsScreen;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.design.button.MaterialButton;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions;
import com.mapbox.mapboxsdk.utils.ColorUtils;
import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.MarkerData;
import mcmaster.ilos.datacollectionapp.CustomDataTypes.Message;
import mcmaster.ilos.datacollectionapp.R;

class MapsUIManager {

    private ArrayList<String> floors;
    private Context context;
    private MapsActivity activity;
    private BottomSheetBehavior bottomSheetBehavior;
    private Message message;
    Dialog dialog;
    Dialog failedDialog;
    Dialog scanningDialog;
    Dialog uploadingDialog;
    Dialog noInternetDialog;
    Dialog diskSpaceDialog;
    Dialog longPathDialog;

    MapsUIManager(MapsActivity activity, Context context, ArrayList<String> floors) {
        this.activity = activity;
        this.context = context;
        this.floors = floors;
        this.dialog = new Dialog(context, R.style.NewDialog);
        this.failedDialog = new Dialog(context, R.style.NewDialog);
        this.scanningDialog = new Dialog(context, R.style.NewDialog);
        this.uploadingDialog = new Dialog(context, R.style.NewDialog);
        this.noInternetDialog = new Dialog(context, R.style.NewDialog);
        this.diskSpaceDialog = new Dialog(context, R.style.NewDialog);
        this.longPathDialog = new Dialog(context, R.style.NewDialog);

        dialog.setContentView(R.layout.upload_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        failedDialog.setContentView(R.layout.failed_dialog);
        failedDialog.setCancelable(false);
        failedDialog.setCanceledOnTouchOutside(false);

        scanningDialog.setContentView(R.layout.scanning_dialog);
        scanningDialog.setCancelable(false);
        scanningDialog.setCanceledOnTouchOutside(false);

        uploadingDialog.setContentView(R.layout.uploading_dialog);
        uploadingDialog.setCancelable(false);
        uploadingDialog.setCanceledOnTouchOutside(false);

        noInternetDialog.setContentView(R.layout.internet_dialog);
        noInternetDialog.setCancelable(false);
        noInternetDialog.setCanceledOnTouchOutside(false);

        diskSpaceDialog.setContentView(R.layout.internet_dialog);
        diskSpaceDialog.setCancelable(false);
        diskSpaceDialog.setCanceledOnTouchOutside(false);

        longPathDialog.setContentView(R.layout.internet_dialog);
        longPathDialog.setCancelable(false);
        longPathDialog.setCanceledOnTouchOutside(false);
    }

    /* Dialog */

    void showUploadDialog() {
        dialog.show();
    }

    void dismissUploadDialog() {
        dialog.dismiss();
    }

    /* Failed Collection Dialog */

    void showCollectionFailedDialog() {
        failedDialog.show();
    }

    void dismissCollectionFailedDialog() {
        failedDialog.dismiss();
    }

    /* Scanning Collection Dialog */

    void showScanningDialog() {
        scanningDialog.show();
    }

    void dismissScanningDialog() {
        scanningDialog.dismiss();
    }

    /* Uploading Collection Dialog */

    void showUploadingDialog() {
        uploadingDialog.show();
    }

    void dismissUploadingDialog() {
        uploadingDialog.dismiss();
    }

    /* No Internet Dialog */

    void showInternetDialog() {
        noInternetDialog.show();
    }

    void dismissInternetDialog() {
        noInternetDialog.dismiss();
    }

    /* Disk Space Dialog */

    void showDiskSpaceDialog() {
        diskSpaceDialog.show();
    }

    void dismissDiskSpaceDialog() {
        diskSpaceDialog.dismiss();
    }

    /* Long PathDialog */

    void showLongPathDialog() {
        longPathDialog.show();
    }

    void dismissLongPathDialog() {
        longPathDialog.dismiss();
    }

    /* Menu Buttons Selection */

    void unselectFloor(String floorNum) {
        LinearLayout hs = activity.findViewById(R.id.horizontal_scroll_container);
        TextView currentlySelectedTextView = (TextView) hs.getChildAt(floors.indexOf(floorNum));
        currentlySelectedTextView.setBackgroundResource(R.drawable.unselected_circle);
        currentlySelectedTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.raspberry));
    }

    void selectFloor(String floorNum) {
        LinearLayout hs = activity.findViewById(R.id.horizontal_scroll_container);
        TextView newlySelectedTextView = (TextView) hs.getChildAt(floors.indexOf(floorNum));
        newlySelectedTextView.setBackgroundResource(R.drawable.selected_circle);
        newlySelectedTextView.setTextColor(Color.WHITE);
    }

    void selectOption(int STATE) {
        View optionView = null;
        ImageView optionIconImage = null;

        switch (STATE) {
            case 1:
                optionView = activity.findViewById(R.id.add_edit_marker_view);
                optionIconImage = activity.findViewById(R.id.add_edit_icon_image);
                break;
            case 2:
                optionView = activity.findViewById(R.id.delete_edit_marker_view);
                optionIconImage = activity.findViewById(R.id.delete_edit_icon_image);
                break;
            case 3:
                optionView = activity.findViewById(R.id.point_view);
                optionIconImage = activity.findViewById(R.id.point_icon_image);
                break;
            case 4:
                optionView = activity.findViewById(R.id.path_view);
                optionIconImage = activity.findViewById(R.id.path_icon_image);
                break;
            case 5:
                optionView = activity.findViewById(R.id.multipath_view);
                optionIconImage = activity.findViewById(R.id.multipath_icon_image);
                break;
//            case 6:
//                optionView = activity.findViewById(R.id.stair_view);
//                optionIconImage = activity.findViewById(R.id.stair_icon_image);
//                break;
//            case 7:
//                optionView = activity.findViewById(R.id.io_view);
//                optionIconImage = activity.findViewById(R.id.io_icon_image);
//                break;
            default:
                break;
        }

        if (optionView != null) {
            optionView.setBackgroundResource(R.drawable.selected_circle);
        }

        if (optionIconImage != null) {
            optionIconImage.setColorFilter(Color.WHITE);
        }
    }

    void unselectOption(int STATE) {
        View optionView;
        ImageView optionIconImage;

        switch (STATE) {
            case 1:
                optionView = activity.findViewById(R.id.add_edit_marker_view);
                optionView.setBackgroundResource(R.drawable.unselected_circle);
                optionIconImage = activity.findViewById(R.id.add_edit_icon_image);
                optionIconImage.setColorFilter(ContextCompat.getColor(context, R.color.raspberry));
                break;
            case 2:
                optionView = activity.findViewById(R.id.delete_edit_marker_view);
                optionView.setBackgroundResource(R.drawable.unselected_circle);
                optionIconImage = activity.findViewById(R.id.delete_edit_icon_image);
                optionIconImage.setColorFilter(ContextCompat.getColor(context, R.color.raspberry));
                break;
            case 3:
                optionView = activity.findViewById(R.id.point_view);
                optionIconImage = activity.findViewById(R.id.point_icon_image);
                optionView.setBackgroundResource(R.drawable.collection_options_bg);
                optionIconImage.setColorFilter(Color.TRANSPARENT);
                break;
            case 4:
                optionView = activity.findViewById(R.id.path_view);
                optionIconImage = activity.findViewById(R.id.path_icon_image);
                optionView.setBackgroundResource(R.drawable.collection_options_bg);
                optionIconImage.setColorFilter(Color.TRANSPARENT);
                break;
            case 5:
                optionView = activity.findViewById(R.id.multipath_view);
                optionIconImage = activity.findViewById(R.id.multipath_icon_image);
                optionView.setBackgroundResource(R.drawable.collection_options_bg);
                optionIconImage.setColorFilter(Color.TRANSPARENT);
                break;
//            case 6:
//                optionView = activity.findViewById(R.id.stair_view);
//                optionIconImage = activity.findViewById(R.id.stair_icon_image);
//                optionView.setBackgroundResource(R.drawable.collection_options_bg);
//                optionIconImage.setColorFilter(Color.TRANSPARENT);
//                break;
//            case 7:
//                optionView = activity.findViewById(R.id.io_view);
//                optionIconImage = activity.findViewById(R.id.io_icon_image);
//                optionView.setBackgroundResource(R.drawable.collection_options_bg);
//                optionIconImage.setColorFilter(Color.TRANSPARENT);
//                break;
            default:
                break;
        }
    }

    void handleSensorState(int SENSOR_STATE) {
        View wifiView = activity.findViewById(R.id.wifi_view);
        ImageView wifiIconImage = activity.findViewById(R.id.wifi_icon_image);
        View bluetoothView = activity.findViewById(R.id.bluetooth_view);
        ImageView bluetoothIconImage = activity.findViewById(R.id.bluetooth_icon_image);

        switch (SENSOR_STATE) {
            case 0:
                wifiView.setBackgroundResource(R.drawable.selected_circle);
                wifiIconImage.setColorFilter(Color.WHITE);
                bluetoothView.setBackgroundResource(R.drawable.selected_circle);
                bluetoothIconImage.setColorFilter(Color.WHITE);
                break;
            case 1:
                wifiView.setBackgroundResource(R.drawable.selected_circle);
                wifiIconImage.setColorFilter(Color.WHITE);
                bluetoothView.setBackgroundResource(R.drawable.unselected_circle);
                bluetoothIconImage.setColorFilter(ContextCompat.getColor(context, R.color.raspberry));
                break;
            case 2:
                wifiView.setBackgroundResource(R.drawable.unselected_circle);
                wifiIconImage.setColorFilter(ContextCompat.getColor(context, R.color.raspberry));
                bluetoothView.setBackgroundResource(R.drawable.selected_circle);
                bluetoothIconImage.setColorFilter(Color.WHITE);
                break;
            default:
                break;
        }
    }

    /* Bottom Sheet */

    void openBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    void closeBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /* Draw Markers on Map */

    CircleOptions drawMarker(LatLng point) {
        return new CircleOptions()
                .withLatLng(point)
                .withCircleColor(ColorUtils.colorToRgbaString(ResourcesCompat.getColor(activity.getResources(), R.color.marker_gray, null)))
                .withCircleRadius(10f)
                .withCircleStrokeColor(ColorUtils.colorToRgbaString(Color.WHITE))
//                .withCircleStrokeColor(ColorUtils.colorToRgbaString(ResourcesCompat.getColor(activity.getResources(), R.color.link, null)))
                .withCircleStrokeWidth(3f)
                .setDraggable(false);
    }

    ArrayList<CircleOptions> drawFloorMarkers(ArrayList<MarkerData> markerData) {
        ArrayList<CircleOptions> circleOptions = new ArrayList<>();
        for (int i = 0; i < markerData.size(); i++) {
            circleOptions.add(drawMarker(new LatLng(markerData.get(i).getLatitude(), markerData.get(i).getLongitude())));
        }
        return circleOptions;
    }

    /* Draw Lines on Map */

    LineOptions drawLine(LatLng start, LatLng end) {
        List<LatLng> latLngs = new ArrayList<>();
        latLngs.add(start);
        latLngs.add(end);
        return new LineOptions()
                .withLatLngs(latLngs)
                .withLineColor(ColorUtils.colorToRgbaString(ResourcesCompat.getColor(activity.getResources(), R.color.link, null)))
                .withLineWidth(5.0f);
    }
//    .withLineColor(ColorUtils.colorToRgbaString(Color.GRAY))

    LineOptions drawDashedLine(LatLng start, LatLng end) {
        List<LatLng> latLngs = new ArrayList<>();
        latLngs.add(start);
        latLngs.add(end);
        return new LineOptions()
                .withLatLngs(latLngs)
                .withLineColor(ColorUtils.colorToRgbaString(Color.GRAY))
                .withLineWidth(5.0f);
    }

    /* Marker Colors */

    Circle updateCircleColor(Circle circle, int color) {
        circle.setCircleColor(color);
        return circle;
    }

    /* Floating Action Button (FAB) */

    void hideFloatingActionButton() {
        MaterialButton fab = activity.findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
    }

    void showFloatingActionButton() {
        MaterialButton fab = activity.findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
    }

    void setFABText(String text) {
        MaterialButton fab = activity.findViewById(R.id.fab);
        fab.setText(text);
    }

    /* Close Button */

    void showCloseButton() {
        ImageView closeButton = activity.findViewById(R.id.close_button);
        closeButton.setVisibility(View.VISIBLE);
    }

    void hideCloseButton() {
        ImageView closeButton = activity.findViewById(R.id.close_button);
        closeButton.setVisibility(View.INVISIBLE);
    }

    /* Set Message for CardView */

    private void setMessage() {
        if (activity.circleManager != null) {
            if (activity.circleManager.getAnnotations().isEmpty()) {
                message = new Message("Welcome to iLOS", "Begin mapping your location by selecting the :add: icon. Long-press on the map to place a marker. Be sure to choose areas that are easily identifiable such as corners, stairwells, doors, etc.");
            } else {
                if (activity.markerList.isEmpty()) {
                    message = new Message("Outline Your Path", "Next we want to outline the path you will follow. Select either the :point:, :path:, or :multipath: icon. Then tap on the markers that you wish to visit.");
                } else {
                    message = new Message("Start Mapping","Visit your paths physical starting location (the green marker). Once there, press start begin walking along the path. Tap the 'Check In' button every time you visit a marker along your path.");
                }
            }
        } else {
            message = new Message("Welcome to iLOS", "Begin mapping your location by selecting the :add: icon. Long-press on the map to place a marker. Be sure to choose areas that are easily identifiable such as corners, stairwells, doors, etc.");
        }
    }

    /* Responsible for displaying inline images for the CardView text */
    private void setSpannableString() {
        TextView innerInfoView = activity.findViewById(R.id.innerInfoTextView);
        String text = message.getBody();
        SpannableString spannableString = new SpannableString(text);

        Pattern pattern = Pattern.compile(context.getString(R.string.inlineAdd));
        Matcher matcher = pattern.matcher(text);
        Bitmap img = null;
        int size = (int) (-innerInfoView.getPaint().ascent());
        while (matcher.find()) {
            if (img == null) {
                Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.add_with_circle_icon);
                img = Bitmap.createScaledBitmap(bitmap, size, size, true);
                bitmap.recycle();
            }
            ImageSpan span = new ImageSpan(getApplicationContext(), img, ImageSpan.ALIGN_BASELINE);
            spannableString.setSpan(span, matcher.start(), matcher.end(), 0);
        }

        pattern = Pattern.compile(context.getString(R.string.inlinePoint));
        matcher = pattern.matcher(text);
        img = null;
        while (matcher.find()) {
            if (img == null) {
                Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.point_icon);
                img = Bitmap.createScaledBitmap(bitmap, size, size, true);
                bitmap.recycle();
            }
            ImageSpan span = new ImageSpan(getApplicationContext(), img, ImageSpan.ALIGN_BASELINE);
            spannableString.setSpan(span, matcher.start(), matcher.end(), 0);
        }

        pattern = Pattern.compile(context.getString(R.string.inlinePath));
        matcher = pattern.matcher(text);
        img = null;
        while (matcher.find()) {
            if (img == null) {
                Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.path_icon);
                img = Bitmap.createScaledBitmap(bitmap, size, size, true);
                bitmap.recycle();
            }
            ImageSpan span = new ImageSpan(getApplicationContext(), img, ImageSpan.ALIGN_BASELINE);
            spannableString.setSpan(span, matcher.start(), matcher.end(), 0);
        }

        pattern = Pattern.compile(context.getString(R.string.inlineMultipath));
        matcher = pattern.matcher(text);
        img = null;
        while (matcher.find()) {
            if (img == null) {
                Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.multipath_icon);
                img = Bitmap.createScaledBitmap(bitmap, size, size, true);
                bitmap.recycle();
            }
            ImageSpan span = new ImageSpan(getApplicationContext(), img, ImageSpan.ALIGN_BASELINE);
            spannableString.setSpan(span, matcher.start(), matcher.end(), 0);
        }

//        pattern = Pattern.compile(context.getString(R.string.inlineStair));
//        matcher = pattern.matcher(text);
//        img = null;
//        while (matcher.find()) {
//            if (img == null) {
//                Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.stair_icon);
//                img = Bitmap.createScaledBitmap(bitmap, size, size, true);
//                bitmap.recycle();
//            }
//            ImageSpan span = new ImageSpan(getApplicationContext(), img, ImageSpan.ALIGN_BASELINE);
//            spannableString.setSpan(span, matcher.start(), matcher.end(), 0);
//        }
//
//        pattern = Pattern.compile(context.getString(R.string.inlineExits));
//        matcher = pattern.matcher(text);
//        img = null;
//        while (matcher.find()) {
//            if (img == null) {
//                Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.io_icon);
//                img = Bitmap.createScaledBitmap(bitmap, size, size, true);
//                bitmap.recycle();
//            }
//            ImageSpan span = new ImageSpan(getApplicationContext(), img, ImageSpan.ALIGN_BASELINE);
//            spannableString.setSpan(span, matcher.start(), matcher.end(), 0);
//        }
        innerInfoView.setText(spannableString);
    }

    void setInitialUI(String currentFloor) {

        /* Set font for all TextViews */
        Typeface avenirnext_demibold = Typeface.createFromAsset(activity.getAssets(), "fonts/AvenirNext-DemiBold.ttf");
        Typeface avenirnext_heavy = Typeface.createFromAsset(activity.getAssets(), "fonts/AvenirNext-Heavy.ttf");
        Typeface avenirnext_medium = Typeface.createFromAsset(activity.getAssets(), "fonts/AvenirNext-Medium.ttf");

        TextView dialogHeaderTextView = dialog.findViewById(R.id.dialog_header_textview);
        dialogHeaderTextView.setTypeface(avenirnext_heavy);

        TextView dialogBodyTextView = dialog.findViewById(R.id.dialog_body_textview);
        dialogBodyTextView.setTypeface(avenirnext_medium);

        Button uploadButton = dialog.findViewById(R.id.dialog_upload_button);
        uploadButton.setTypeface(avenirnext_heavy);

        Button uploadLaterButton = dialog.findViewById(R.id.dialog_upload_later_button);
        uploadLaterButton.setTypeface(avenirnext_demibold);

        Button deleteButton = dialog.findViewById(R.id.dialog_delete_button);
        deleteButton.setTypeface(avenirnext_demibold);

        TextView headerTextView = activity.findViewById(R.id.header_textview);
        headerTextView.setTypeface(avenirnext_heavy);

        TextView floorTextView = activity.findViewById(R.id.floor_textview);
        floorTextView.setTypeface(avenirnext_medium);

        TextView editMarkerText = activity.findViewById(R.id.edit_marker_textview);
        editMarkerText.setTypeface(avenirnext_medium);

        TextView addMarkerText = activity.findViewById(R.id.add_textview);
        addMarkerText.setTypeface(avenirnext_medium);

        TextView deleteMarkerText = activity.findViewById(R.id.delete_textview);
        deleteMarkerText.setTypeface(avenirnext_medium);

        TextView collectFingerprintTextView = activity.findViewById(R.id.collect_fingerprint_textview);
        collectFingerprintTextView.setTypeface(avenirnext_medium);

        TextView callibrateTextView = activity.findViewById(R.id.sensor_options_textview);
        callibrateTextView.setTypeface(avenirnext_medium);

        TextView pointTextView = activity.findViewById(R.id.point_textview);
        pointTextView.setTypeface(avenirnext_medium);

        TextView pathTextView = activity.findViewById(R.id.path_textview);
        pathTextView.setTypeface(avenirnext_medium);

        TextView multipathTextView = activity.findViewById(R.id.multipath_textview);
        multipathTextView.setTypeface(avenirnext_medium);

        TextView wifiTextView = activity.findViewById(R.id.wifi_textview);
        wifiTextView.setTypeface(avenirnext_medium);

        TextView bluetoothTextView = activity.findViewById(R.id.bluetooth_textview);
        bluetoothTextView.setTypeface(avenirnext_medium);

        TextView outerInfoView = activity.findViewById(R.id.outerInfoTextView);
        outerInfoView.setTypeface(avenirnext_heavy);

        TextView innerInfoView = activity.findViewById(R.id.innerInfoTextView);
        innerInfoView.setTypeface(avenirnext_medium);

        /* Initialize Bottom Sheet UI */
        RelativeLayout llBottomSheet = activity.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        bottomSheetBehavior.setHideable(false);


        /* Make CardView initially hidden */
        CardView cardView = activity.findViewById(R.id.info_cardview);
        cardView.setAlpha(0f);

        /* Set CardView Message */
        setMessage();
        outerInfoView.setText(message.getHeader());
        innerInfoView.setText(message.getBody());
        setSpannableString();

        MaterialButton fab = activity.findViewById(R.id.fab);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
//                if (BottomSheetBehavior.STATE_DRAGGING == newState) {
//                    fab.animate().scaleX(0).scaleY(0).setDuration(300).start();
//                } else if (BottomSheetBehavior.STATE_COLLAPSED == newState) {
//                    fab.animate().scaleX(1).scaleY(1).setDuration(300).start();
//                }

                /* We need to change the message when the Bottom Sheet is re-opened */
                setMessage();
                outerInfoView.setText(message.getHeader());
                innerInfoView.setText(message.getBody());
                setSpannableString();

                if (fab.getScaleX() != 1) {
                    fab.setClickable(false);
                } else {
                    fab.setClickable(true);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                fab.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start();
                cardView.animate().alpha(slideOffset).setDuration(0).start();
            }
        });

        /* Hide the FAB initially */
        hideFloatingActionButton();

        /* Set the floor menu buttons */
        LinearLayout horizontal_scroll_container = activity.findViewById(R.id.horizontal_scroll_container);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10,0,10,0);
        for (String floor : floors) {
            TextView tv = new TextView(context);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(params);
            tv.setText(floor);
            tv.setTextSize(14.0f);
            tv.setTag(tv.getText().toString());
            tv.setTypeface(avenirnext_heavy);
            if (tv.getTag().toString().equals(currentFloor)) {
                tv.setBackgroundResource(R.drawable.selected_circle);
                tv.setTextColor(Color.WHITE);
            } else {
                tv.setBackgroundResource(R.drawable.unselected_circle);
                tv.setTextColor(ContextCompat.getColor(context, R.color.raspberry));
            }
            horizontal_scroll_container.addView(tv);
        }
    }
}
