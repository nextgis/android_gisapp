package com.nextgis.mobile;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import com.nextgis.maplibui.fragments.LayersFragment;
import com.nextgis.maplibui.fragments.MapFragment;
import com.nextgis.maplibui.mapui.MapView;


public class MainActivity extends ActionBarActivity {

    protected MapFragment mMapFragment;
    protected LayersFragment mLayersFragment;
    protected MapView mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMap = new MapView(this);

        setContentView(R.layout.activity_main);

        //restoreActionBar();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mMapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag("MAP");

        if (mMapFragment == null) {
            mMapFragment = new MapFragment();
            if(mMapFragment.onInit((String) getTitle(), mMap)) {
                fragmentTransaction.add(R.id.map, mMapFragment, "MAP").commit();
            }
        }

        getSupportFragmentManager().executePendingTransactions();

        mLayersFragment = (LayersFragment) getSupportFragmentManager().findFragmentById(R.id.layers);
        if(mLayersFragment != null && mLayersFragment.onInit((String) getTitle(), mMap)) {
            mLayersFragment.getView().setBackgroundColor(getResources().getColor(R.color.background_material_light));
            // Set up the drawer.
            mLayersFragment.setUp(R.id.layers, (DrawerLayout) findViewById(R.id.drawer_layout));
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mLayersFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
