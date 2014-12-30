package com.nextgis.mobile;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import com.nextgis.maplibui.MapView;


public class MainActivity extends ActionBarActivity {

    protected MapFragment mMapFragment;
    protected LayersFragment mLayersFragment;
    protected MapView mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMap = new MapView(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(128);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mMapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag("MAP");

        if (mMapFragment == null) {
            mMapFragment = new MapFragment();
            if(mMapFragment.onInit(mMap)) {
                fragmentTransaction.add(R.id.map, mMapFragment, "MAP").commit();
            }
        }

        getSupportFragmentManager().executePendingTransactions();

        mLayersFragment = (LayersFragment) getSupportFragmentManager().findFragmentById(R.id.layers);
        if(mLayersFragment != null && mLayersFragment.onInit(mMap)) {
            mLayersFragment.getView().setBackgroundColor(getResources().getColor(R.color.background_material_light));
            // Set up the drawer.
            mLayersFragment.setUp(R.id.layers, (DrawerLayout) findViewById(R.id.drawer_layout));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mLayersFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            //restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_settings:
                return true;
            case R.id.menu_add_local:

                return true;
            case R.id.menu_add_remote:
                addRemoteLayer();
                return true;
            case R.id.menu_add_ngw:

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void addRemoteLayer(){

    }
}
