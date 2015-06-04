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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.mapui.LayerFactoryUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.mapui.TrackLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.mobile.activity.SettingsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.MAP_EXT;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP_PATH;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIODICALLY;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_SYNC_PERIOD_SEC_LONG;
import static com.nextgis.mobile.util.SettingsConstants.*;


public class GISApplication
        extends Application
        implements IGISApplication
{
    protected MapDrawable    mMap;
    protected GpsEventSource mGpsEventSource;


    @Override
    public void onCreate()
    {
        super.onCreate();

        mGpsEventSource = new GpsEventSource(this);

        getMap();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(KEY_PREF_APP_FIRST_RUN, true)) {
            onFirstRun();
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean(KEY_PREF_APP_FIRST_RUN, false);
            edit.commit();
        }

        //turn on periodic sync. Can be set for each layer individually, but this is simpler
        if (sharedPreferences.getBoolean(KEY_PREF_SYNC_PERIODICALLY, true)) {
            long period =
                    sharedPreferences.getLong(KEY_PREF_SYNC_PERIOD_SEC_LONG, NOT_FOUND); //10 min
            if (period != NOT_FOUND) {
                Bundle params = new Bundle();
                params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
                params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
                params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);

                SyncAdapter.setSyncPeriod(this, params, period);
            }
        }
    }


    @Override
    public MapBase getMap()
    {
        if (null != mMap) {
            return mMap;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        File defaultPath = getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(getFilesDir(), KEY_PREF_MAP);
        }
        if (defaultPath != null) {
            String mapPath = sharedPreferences.getString(KEY_PREF_MAP_PATH, defaultPath.getPath());
            String mapName = sharedPreferences.getString(KEY_PREF_MAP_NAME, "default");

            File mapFullPath = new File(mapPath, mapName + MAP_EXT);

            final Bitmap bkBitmap = BitmapFactory.decodeResource(
                    getResources(), com.nextgis.maplibui.R.drawable.bk_tile);
            mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactoryUI());
            mMap.setName(mapName);
            mMap.load();

            checkTracksLayerExist();
        }

        return mMap;
    }


    protected void checkTracksLayerExist()
    {
        List<ILayer> tracks = new ArrayList<>();
        LayerGroup.getLayersByType(mMap, Constants.LAYERTYPE_TRACKS, tracks);
        if (tracks.isEmpty()) {
            String trackLayerName = getString(R.string.tracks);
            TrackLayerUI trackLayer =
                    new TrackLayerUI(getApplicationContext(), mMap.createLayerStorage());
            trackLayer.setName(trackLayerName);
            trackLayer.setVisible(true);
            mMap.addLayer(trackLayer);

            mMap.save();
        }
    }


    @Override
    public String getAuthority()
    {
        return AUTHORITY;
    }


    @Override
    public Account getAccount(String accountName)
    {
        AccountManager accountManager = AccountManager.get(this);
        for (Account account : accountManager.getAccountsByType(Constants.NGW_ACCOUNT_TYPE)) {
            if (account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }


    @Override
    public String getAccountUrl(Account account)
    {
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getUserData(account, "url");
    }


    @Override
    public String getAccountLogin(Account account)
    {
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getUserData(account, "login");
    }


    @Override
    public String getAccountPassword(Account account)
    {
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getPassword(account);
    }


    @Override
    public GpsEventSource getGpsEventSource()
    {
        return mGpsEventSource;
    }


    @Override
    public void showSettings()
    {
        Intent intentSet = new Intent(this, SettingsActivity.class);
        intentSet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentSet);
    }


    protected void onFirstRun()
    {
        //add OpenStreetMap layer on application first run
        String layerName = getString(R.string.osm);
        String layerURL = getString(R.string.osm_url);
        RemoteTMSLayerUI layer =
                new RemoteTMSLayerUI(getApplicationContext(), mMap.createLayerStorage());
        layer.setName(layerName);
        layer.setURL(layerURL);
        layer.setTMSType(TMSTYPE_OSM);
        layer.setVisible(true);

        mMap.addLayer(layer);
        mMap.moveLayer(0, layer);

        // create empty layers for first experimental editing
        VectorLayerUI vectorLayer =
                createEmptyVectorLayerUI(getString(R.string.points_for_edit), GeoConstants.GTPoint);
        mMap.addLayer(vectorLayer);

        vectorLayer = createEmptyVectorLayerUI(
                getString(R.string.lines_for_edit), GeoConstants.GTLineString);
        mMap.addLayer(vectorLayer);

        vectorLayer = createEmptyVectorLayerUI(
                getString(R.string.polygons_for_edit), GeoConstants.GTPolygon);
        mMap.addLayer(vectorLayer);

        mMap.save();
    }


    protected VectorLayerUI createEmptyVectorLayerUI(
            String layerName,
            int layerType)
    {
        VectorLayerUI vectorLayer = new VectorLayerUI(this, mMap.createLayerStorage());
        vectorLayer.setName(layerName);
        vectorLayer.setVisible(true);

        List<Field> fields = new ArrayList<>();
        fields.add(new Field(GeoConstants.FTInteger, "ID", null));
        fields.add(new Field(GeoConstants.FTString, "TEXT", null));

        vectorLayer.initialize(fields, new ArrayList<Feature>(), layerType);
        return vectorLayer;
    }
}
