package mcmaster.ilos.datacollectionapp.MainMenu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import mcmaster.ilos.datacollectionapp.BuildingsScreen.NewBuildingActivity;
import mcmaster.ilos.datacollectionapp.DownloadMaps.MapDownloadActivity;
import mcmaster.ilos.datacollectionapp.LoginScreen.LoginActivity;
import mcmaster.ilos.datacollectionapp.LoginScreen.LoginManager;
import mcmaster.ilos.datacollectionapp.R;
import mcmaster.ilos.datacollectionapp.UploadSavedData.UploadActivity;

import static mcmaster.ilos.datacollectionapp.Utils.ProtobufManager.getFiles;

/* The activity which displays the main menu and allows the user to navigate to new activities */
public class MainMenuActivity extends AppCompatActivity {

    String[] listviewTitle = new String[]{"Collect data", "Upload traces", "Download maps", "Logout"};

    int[] listviewImage = new int[]{
            R.drawable.building_icon,
            R.drawable.upload_icon_new,
            R.drawable.map_icon,
            R.drawable.logout_icon
    };

    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        loginManager = new LoginManager(this);

        Toolbar mActionBarToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("iLOS Site Survey Tool");

        final List<HashMap<String, String>> aList = new ArrayList<>();

        for (int i = 0; i < listviewTitle.length; i++) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put("listview_title", listviewTitle[i]);
            hm.put("listview_image", Integer.toString(listviewImage[i]));
            aList.add(hm);
        }

        String[] from = {"listview_image", "listview_title"};
        int[] to = {R.id.listview_image, R.id.listview_item_title};

        SimpleAdapter simpleAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.custom_list_view, from, to);
        ListView androidListView = findViewById(R.id.mainMenuListView);
        androidListView.setAdapter(simpleAdapter);

        androidListView.setOnItemClickListener((parent, view, position, id) -> {

            Intent newActivity = null;
            switch (position) {
                case 0:
                    newActivity = new Intent(MainMenuActivity.this, NewBuildingActivity.class);
                    break;
                case 1:
                    newActivity = new Intent(MainMenuActivity.this, UploadActivity.class);
                    break;
                case 2:
                    newActivity = new Intent(MainMenuActivity.this, MapDownloadActivity.class);
                    break;
                case 3:
                    loginManager.logout();
                    newActivity = new Intent(MainMenuActivity.this, LoginActivity.class);
                    newActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    break;
                default:
                    break;
            }
            if (newActivity != null) {
                startActivity(newActivity);
            }
        });
    }
}
