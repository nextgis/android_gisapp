/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
import android.content.SyncResult;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.service.TrackerService;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplibui.BottomToolbar;
import com.nextgis.maplibui.api.IChooseLayerResult;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.mobile.util.SettingsConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
import static com.nextgis.maplibui.TracksActivity.isTrackerServiceRunning;


public class MainActivity
        extends ActionBarActivity
        implements GpsEventListener, IChooseLayerResult
{

    protected MapFragment     mMapFragment;
    protected LayersFragment  mLayersFragment;
    protected MessageReceiver mMessageReceiver;
    protected Toolbar mToolbar;
    protected MenuItem mRefreshItem;

    protected final static int FILE_SELECT_CODE = 555;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences_general, false);

        setContentView(R.layout.activity_main);


        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mToolbar.getBackground().setAlpha(128);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        mMapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        GISApplication app = (GISApplication) getApplication();

        mLayersFragment =
                (LayersFragment) getSupportFragmentManager().findFragmentById(R.id.layers);
        if (mLayersFragment != null) {
            mLayersFragment.getView()
                           .setBackgroundColor(
                                   getResources().getColor(R.color.background_material_light));
            // Set up the drawer.
            mLayersFragment.setUp(R.id.layers, drawerLayout,
                                  (MapDrawable) app.getMap());
        }

        mMessageReceiver = new MessageReceiver();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (null != mLayersFragment && !mLayersFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            //restoreActionBar();

            mRefreshItem = menu.findItem(R.id.menu_refresh);

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    public BottomToolbar getBottomToolbar(){
        return (BottomToolbar) findViewById(R.id.bottom_toolbar);
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
            /*case R.id.menu_add_ngw:
                addNGWLayer();
                return true;*/
            case R.id.menu_locate:
                locateCurrentPosition();
                return true;
            case R.id.menu_track:
                Intent trackerService = new Intent(this, TrackerService.class);
                trackerService.putExtra(TrackerService.TARGET_CLASS, this.getClass().getName());

                if (isTrackerServiceRunning(this)) {
                    stopService(trackerService);
                    item.setTitle(R.string.track_start);
                } else {
                    startService(trackerService);
                    item.setTitle(R.string.track_stop);
                }
                return true;
            case R.id.menu_refresh:
                if(null != mMapFragment) {
                    mMapFragment.refresh();
                }
                return true;
            /*case R.id.menu_test:
                //testAttachInsert();
                //testAttachUpdate();
                testAttachDelete();
                new Thread() {
                    @Override
                    public void run() {
                        testSync();
                    }
                }.start();
                return true;*/
        }

        return super.onOptionsItemSelected(item);
    }

    public void onRefresh(boolean isRefresh, int progress){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (null != mRefreshItem) {
                if (isRefresh) {
                    if(mRefreshItem.getActionView() == null) {
                        RotateAnimation rotateAnimation =
                                new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                                                    Animation.RELATIVE_TO_SELF, 0.5f);
                        rotateAnimation.setDuration(1500);
                        rotateAnimation.setRepeatCount(60);
                        LayoutInflater inflater = LayoutInflater.from(this);
                        ImageView iv = (ImageView) inflater.inflate(R.layout.layout_refresh, null);
                        iv.startAnimation(rotateAnimation);
                        mRefreshItem.setActionView(iv);
                    }
                } else {
                    if(mRefreshItem.getActionView() != null) {
                        mRefreshItem.getActionView().clearAnimation();
                        mRefreshItem.setActionView(null);
                    }
                }
            }
        }
    }

    protected void addLocalLayer()
    {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        // https://developer.android.com/guide/topics/providers/document-provider.html#client
        Intent intent;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        else
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.select_file)),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            //TODO: open select local resource dialog
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
                        if(null != mMapFragment)
                            mMapFragment.addLocalTMSLayer(uri);
                    }
                    else if(fileName.endsWith("geojson") || fileName.endsWith("GEOJSON")){ //create local vector layer
                        if(null != mMapFragment)
                            mMapFragment.addLocalVectorLayer(uri);
                    }
                    else if(fileName.endsWith("ngfp") || fileName.endsWith("NGFP")){ //create local vector layer with form
                        if(null != mMapFragment)
                            mMapFragment.addLocalVectorLayerWithForm(uri);
                    }
                    else{
                        Toast.makeText(this, getString(R.string.error_file_unsupported), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }


    protected void locateCurrentPosition()
    {
        if(null != mMapFragment)
            mMapFragment.locateCurrentPosition();
    }

    void testSync(){
        IGISApplication application = (IGISApplication)getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
                ngwVectorLayer.sync(application.getAuthority(), new SyncResult());
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

    void testAttachUpdate(){
        IGISApplication application = (IGISApplication)getApplication();
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri updateUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" +
                                      ngwVectorLayer.getPath().getName() + "/36/attach/1000");
        */
            Uri updateUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/layer_20150210140455993/36/attach/2");

            ContentValues values = new ContentValues();
            values.put(VectorLayer.ATTACH_DISPLAY_NAME, "no_image.jpg");
            values.put(VectorLayer.ATTACH_DESCRIPTION, "simple update description");
        //    values.put(VectorLayer.ATTACH_ID, 999);
            int result = getContentResolver().update(updateUri, values, null, null);
            if (result == 0) {
                Log.d(TAG, "update failed");
            } else {
                Log.d(TAG, "" + result);
            }
        //}
    }

    void testAttachDelete(){
        IGISApplication application = (IGISApplication)getApplication();
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri deleteUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" +
                                ngwVectorLayer.getPath().getName() + "/36/attach/1000");
        */
            Uri deleteUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/layer_20150210140455993/36/attach/1");
            int result = getContentResolver().delete(deleteUri, null, null);
            if (result == 0) {
                Log.d(TAG, "delete failed");
            } else {
                Log.d(TAG, "" + result);
            }
        //}
    }

    void testAttachInsert(){
        IGISApplication application = (IGISApplication)getApplication();
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri uri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" + ngwVectorLayer.getPath().getName() + "/36/attach");
        */
            Uri uri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/layer_20150210140455993/36/attach");
            ContentValues values = new ContentValues();
            values.put(VectorLayer.ATTACH_DISPLAY_NAME, "test_image.jpg");
            values.put(VectorLayer.ATTACH_MIME_TYPE, "image/jpeg");
            values.put(VectorLayer.ATTACH_DESCRIPTION, "test image description");

            Uri result = getContentResolver().insert(uri, values);
            if (result == null) {
                Log.d(TAG, "insert failed");
            }
            else{
                try {
                    OutputStream outStream = getContentResolver().openOutputStream(result);
                    Bitmap sourceBitmap = BitmapFactory.decodeResource(getResources(), com.nextgis.maplibui.R.drawable.bk_tile);
                    sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream);
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, result.toString());
            }
        //}
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
        if(null != mMapFragment)
            mMapFragment.addNGWLayer();
    }


    protected void addRemoteLayer()
    {
        if(null != mMapFragment)
            mMapFragment.addRemoteLayer();
    }


    @Override
    public void onFinishChooseLayerDialog(
            int code,
            ILayer layer)
    {
        if(null != mMapFragment)
            mMapFragment.onFinishChooseLayerDialog(code, layer);
    }


    protected class MessageReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConstantsUI.MESSAGE_INTENT)) {
                Toast.makeText(MainActivity.this, intent.getExtras().getString(
                        ConstantsUI.KEY_MESSAGE), Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConstantsUI.MESSAGE_INTENT);
        registerReceiver(mMessageReceiver, intentFilter);

        setActionBarState(true);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (null != mLayersFragment && !mLayersFragment.isDrawerOpen()) {
            int title = isTrackerServiceRunning(this) ? R.string.track_stop : R.string.track_start;
            MenuItem item = menu.findItem(R.id.menu_track);
            if(null != item)
                item.setTitle(title);
        }

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected void onPause()
    {
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

    public void setActionBarState(boolean state) {
        mLayersFragment.setDrawerToggleEnabled(state);

        if (state) {
            mToolbar.getBackground().setAlpha(128);
            getBottomToolbar().getBackground().setAlpha(128);
            mToolbar.setTitle(R.string.app_name);
        } else {
            mToolbar.getBackground().setAlpha(255);
            getBottomToolbar().getBackground().setAlpha(255);
            mToolbar.setTitle(R.string.action_attributes);
        }
    }


    public void restoreBottomBar() {
        mMapFragment.restoreBottomBar();
    }
}
