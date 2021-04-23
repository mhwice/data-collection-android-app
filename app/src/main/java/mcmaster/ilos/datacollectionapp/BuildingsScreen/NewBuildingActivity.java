package mcmaster.ilos.datacollectionapp.BuildingsScreen;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.Map;
import mcmaster.ilos.datacollectionapp.DownloadMaps.Download_RecyclerView_Adapter;
import mcmaster.ilos.datacollectionapp.LoginScreen.LoginManager;
import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;
import mcmaster.ilos.datacollectionapp.R;
import mcmaster.ilos.datacollectionapp.DownloadMaps.Download_Item_Model;
import mcmaster.ilos.datacollectionapp.Utils.RecyclerClick_Listener;
import mcmaster.ilos.datacollectionapp.Utils.RecyclerTouchListener;

/* The buildings activity. Called "NewBuildingActivity" because there used to be an activity called "BuildingsActivity" */
public class NewBuildingActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Download_RecyclerView_Adapter adapter;
    private BuildingManager buildingManager;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_building);

        buildingManager = new BuildingManager(this);
        loginManager = new LoginManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Buildings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        populateRecyclerView();
        implementRecyclerViewClickListeners();
    }

    private void implementRecyclerViewClickListeners() {
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, (view, position) -> onListItemSelect(position)));
    }

    private void onListItemSelect(int position) {
        Intent mapsActivity = new Intent(this, MapsActivity.class);

        String bname = adapter.getFilteredData().get(position).getTitle();
        LoginManager loginManager = new LoginManager(this);
        String token = loginManager.loadToken();
        mapsActivity.putExtra("buildingName", bname);
        mapsActivity.putExtra("token", token);

        ArrayList<Map> dMaps = buildingManager.getDownloadedMapList(token);

		// Not OS 23 Compatible
        // Finds a list of downloaded maps with building name equal to whatever was selected
//		List<Map> mapsInBuilding = dMaps.stream().filter(c -> bname.equals(c.getName())).collect(Collectors.toList());

		List<Map> mapsInBuilding = new ArrayList<>();
        for (Map m : dMaps) {
        	if (m.getName().equals(bname)) {
        		mapsInBuilding.add(m);
			}
		}

        ArrayList<String> floors = new ArrayList<>();
        for (Map m : mapsInBuilding) {
            floors.add(m.getFloorNum());
        }

        mapsActivity.putStringArrayListExtra("floors", floors);
        startActivity(mapsActivity);
    }

    private void populateRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String token = loginManager.loadToken();
        ArrayList<Map> downloadedMapList = buildingManager.getDownloadedMapList(token);

        ArrayList<Download_Item_Model> downloadItemModels = new ArrayList<>();

        // Not OS 23 Compatible
		// This finds all of the uniquely named buildings
//        List<Map> someList = downloadedMapList.stream().filter(distinctByKey(Map::getName)).collect(Collectors.toList());

        List<String> someList2 = new ArrayList<>();
        for (Map m : downloadedMapList) {
        	if (!someList2.contains(m.getName())) {
				someList2.add(m.getName());
			}
		}

        for (String bname : someList2) {
        	long numMatches2 = 0;
        	for (Map m : downloadedMapList) {
        		if (bname.equals(m.getName())) {
        			numMatches2+=1;
				}
			}
			String pred = " map";
			if (numMatches2 > 1) {
				pred = " maps";
			}
			downloadItemModels.add(new Download_Item_Model(bname, numMatches2 + pred, false));
		}

		//			// Not OS 23 Compatible
//        for (Map m : someList) {
//            long numMatches = downloadedMapList.stream().filter(c -> m.getName().equals(c.getName())).count();
//            String pred = " map";
//            if (numMatches > 1) {
//                pred = " maps";
//            }
//            downloadItemModels.add(new Download_Item_Model(m.getName(), numMatches + pred, false));
//        }

        adapter = new Download_RecyclerView_Adapter(this, downloadItemModels);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

	// Not OS 23 Compatible
//    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
//        Set<Object> seen = ConcurrentHashMap.newKeySet();
//        return t -> seen.add(keyExtractor.apply(t));
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_only, menu);

        MenuItem mSearch = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) mSearch.getActionView();
        mSearchView.setQueryHint("");

        View v = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundColor(Color.TRANSPARENT);

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

        return super.onCreateOptionsMenu(menu);
    }
}
