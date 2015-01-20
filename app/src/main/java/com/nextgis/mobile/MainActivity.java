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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplibui.MapView;
import com.nextgis.mobile.util.SettingsConstants;

import java.io.IOException;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


public class MainActivity
        extends ActionBarActivity
{

    protected MapFragment    mMapFragment;
    protected LayersFragment mLayersFragment;
    protected MapView        mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences_general, false);

        GISApplication app = (GISApplication) getApplication();
        mMap = new MapView(this, (MapDrawable) app.getMap());

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
                Intent intentSet = new Intent(this, SettingsActivity.class);
                //intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentSet);
                return true;
            case R.id.menu_about:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                return true;
            case R.id.menu_add_local:
                testInsert();
                //testUpdate();
                //testDelete();
                return true;
            case R.id.menu_add_remote:
                addRemoteLayer();
                return true;
            case R.id.menu_add_ngw:
                addNGWLayer();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
                    ContentUris.withAppendedId(uri, 1022);
            ContentValues values = new ContentValues();
            values.put("width", 2);
            values.put("azimuth", 4.0);
            values.put("status", "test3");
            values.put("temperatur", -30);
            values.put("name", "None");
            try {
                GeoPoint pt = new GeoPoint(47, 65);
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
            values.put(VectorLayer.FIELD_ID, 1022);
            values.put("width", 1);
            values.put("azimuth", 2.0);
            values.put("status", "test");
            values.put("temperatur", -10);
            values.put("name", "None");
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
                    ContentUris.withAppendedId(uri, 1022);
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
}
