/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.mobile;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.CurrentLocationOverlay;
import com.nextgis.maplibui.MapView;
import com.nextgis.maplibui.MapViewOverlays;
import com.nextgis.maplibui.util.Constants;
import com.nextgis.mobile.util.SettingsConstants;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


public class MainActivity
        extends ActionBarActivity
        implements GpsEventListener
{

    protected MapFragment     mMapFragment;
    protected LayersFragment  mLayersFragment;
    protected MapViewOverlays mMap;
    protected MessageReceiver mMessageReceiver;
    protected GpsEventSource  gpsEventSource;
    protected CurrentLocationOverlay mCurrentLocationOverlay;

    protected final static int FILE_SELECT_CODE = 555;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences_general, false);

        GISApplication app = (GISApplication) getApplication();
        mMap = new MapViewOverlays(this, (MapDrawable) app.getMap());

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
            if (mMapFragment.onInit(mMap)) {
                fragmentTransaction.add(R.id.map, mMapFragment, "MAP").commit();
            }
        } else {
            mMapFragment.onInit(mMap);
        }

        getSupportFragmentManager().executePendingTransactions();

        mLayersFragment =
                (LayersFragment) getSupportFragmentManager().findFragmentById(R.id.layers);
        if (mLayersFragment != null) {
            mLayersFragment.getView()
                           .setBackgroundColor(
                                   getResources().getColor(R.color.background_material_light));
            // Set up the drawer.
            mLayersFragment.setUp(R.id.layers, (DrawerLayout) findViewById(R.id.drawer_layout),
                                  (MapDrawable) app.getMap());
        }

        mMessageReceiver = new MessageReceiver();

        gpsEventSource = ((IGISApplication) getApplication()).getGpsEventSource();
        mCurrentLocationOverlay = new CurrentLocationOverlay(this, mMap.getMap());
        mMap.addOverlay(mCurrentLocationOverlay);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
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
    public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId()) {
            case R.id.menu_settings:
                final IGISApplication app = (IGISApplication) getApplication();
                app.showSettings();
                return true;
            case R.id.menu_about:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                return true;
            case R.id.menu_add_local:
                addLocalLayer();
                return true;
            case R.id.menu_add_remote:
                addRemoteLayer();
                return true;
            case R.id.menu_add_ngw:
                addNGWLayer();
                return true;
            case R.id.menu_locate:
                locateCurrentPosition();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void addLocalLayer()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.select_file)),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, getString(R.string.warning_install_file_manager),
                           Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
        //http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult/18345899
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    //check the file type from extension
                    String fileName = FileUtil.getFileNameByUri(this, uri, "");
                    if(fileName.endsWith("zip") || fileName.endsWith("ZIP")){ //create local tile layer
                        mMap.addLocalTMSLayer(uri);
                    }
                    else if(fileName.endsWith("geojson") || fileName.endsWith("GEOJSON")){ //create local vector layer
                        mMap.addLocalVectorLayer(uri);
                    }
                    else if(fileName.endsWith("ngfp") || fileName.endsWith("NGFP")){ //create local vector layer with form
                        mMap.addLocalVectorLayerWithForm(uri);
                    }
                    else{
                        Toast.makeText(this, getString(R.string.error_file_unsupported), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }


    private void locateCurrentPosition()
    {
        Location location = gpsEventSource.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(location != null) {
            GeoPoint center = new GeoPoint(location.getLongitude(), location.getLatitude());
            center.setCRS(GeoConstants.CRS_WGS84);
            if(center.project(GeoConstants.CRS_WEB_MERCATOR)) {
                mMap.invalidate();
                mMap.setZoomAndCenter(mMap.getZoomLevel(), center);
            }
        }
    }


    void testUpdate(){
        //test sync
        IGISApplication application = (IGISApplication)getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri uri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" + ngwVectorLayer.getPath().getName());
            Uri updateUri =
                    ContentUris.withAppendedId(uri, 29);
            ContentValues values = new ContentValues();
            values.put("width", 4);
            values.put("azimuth", 8.0);
            values.put("status", "test4");
            values.put("temperatur", -10);
            values.put("name", "xxx");

            Calendar calendar = new GregorianCalendar(2014, Calendar.JANUARY, 23);
            values.put("datetime", calendar.getTimeInMillis());
            try {
                GeoPoint pt = new GeoPoint(67, 65);
                pt.setCRS(CRS_WGS84);
                pt.project(CRS_WEB_MERCATOR);
                GeoMultiPoint mpt = new GeoMultiPoint();
                mpt.add(pt);
                values.put(VectorLayer.FIELD_GEOM, mpt.toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
            int result = getContentResolver().update(updateUri, values, null, null);
            if (result == 0) {
                Log.d(TAG, "update failed");
            }
            else{
                Log.d(TAG, "" + result);
            }
        }
    }

    void testInsert(){
        //test sync
        IGISApplication application = (IGISApplication)getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri uri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" + ngwVectorLayer.getPath().getName());
            ContentValues values = new ContentValues();
            //values.put(VectorLayer.FIELD_ID, 26);
            values.put("width", 1);
            values.put("azimuth", 2.0);
            values.put("status", "grot");
            values.put("temperatur", -13);
            values.put("name", "get");

            Calendar calendar = new GregorianCalendar(2015, Calendar.JANUARY, 23);
            values.put("datetime", calendar.getTimeInMillis());

            try {
                GeoPoint pt = new GeoPoint(37, 55);
                pt.setCRS(CRS_WGS84);
                pt.project(CRS_WEB_MERCATOR);
                GeoMultiPoint mpt = new GeoMultiPoint();
                mpt.add(pt);
                values.put(VectorLayer.FIELD_GEOM, mpt.toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Uri result = getContentResolver().insert(uri, values);
            if (result == null) {
                Log.d(TAG, "insert failed");
            }
            else{
                Log.d(TAG, result.toString());
            }
        }
    }

    void testDelete(){
        IGISApplication application = (IGISApplication)getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri uri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" +
                                ngwVectorLayer.getPath().getName());
            Uri deleteUri =
                    ContentUris.withAppendedId(uri, 27);
            int result = getContentResolver().delete(deleteUri, null, null);
            if (result == 0) {
                Log.d(TAG, "delete failed");
            } else {
                Log.d(TAG, ""+result);
            }
        }
    }


    protected void addNGWLayer()
    {
        mMap.addNGWLayer();
    }


    protected void addRemoteLayer()
    {
        mMap.addRemoteLayer();
    }

    protected class MessageReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.MESSAGE_INTENT)) {
                Toast.makeText(MainActivity.this, intent.getExtras().getString(
                        Constants.KEY_MESSAGE), Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.MESSAGE_INTENT);
        registerReceiver(mMessageReceiver, intentFilter);
        gpsEventSource.addListener(this);
        mCurrentLocationOverlay.startShowingCurrentLocation();
    }


    @Override
    protected void onPause()
    {
        mCurrentLocationOverlay.stopShowingCurrentLocation();
        gpsEventSource.removeListener(this);
        unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location)
    {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }

}
