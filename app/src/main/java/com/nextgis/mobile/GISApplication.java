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

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.mapui.LayerFactoryUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;

import java.io.File;

import static com.nextgis.maplib.util.GeoConstants.*;
import static com.nextgis.mobile.util.SettingsConstants.*;
import static com.nextgis.maplib.util.Constants.*;


public class GISApplication
        extends Application
{
    protected MapDrawable mMap;


    @Override
    public void onCreate()
    {
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        File defaultPath = getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath != null) {
            String mapPath = sharedPreferences.getString(KEY_PREF_MAP_PATH, defaultPath.getPath());
            String mapName = sharedPreferences.getString(KEY_PREF_MAP_NAME, "default");

            File mapFullPath = new File(mapPath, mapName + MAP_EXT);

            final Bitmap bkBitmap = BitmapFactory.decodeResource(getResources(),
                                                                 com.nextgis.maplibui.R.drawable.bk_tile);
            mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactoryUI(mapFullPath));
            mMap.setName(mapName);
            mMap.load();

            if(sharedPreferences.getBoolean(KEY_PREF_APP_FIRST_RUN, true))
            {
                onFirstRun();
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putBoolean(KEY_PREF_APP_FIRST_RUN, false);
                edit.commit();
            }
        }
    }


    public MapDrawable getMap()
    {
        return mMap;
    }

    protected void onFirstRun()
    {
        //add OpenStreetMap layer on application first run
        String layerName = getString(R.string.osm);
        String layerURL = getString(R.string.osm_url);
        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(getApplicationContext(), mMap.cretateLayerStorage());
        layer.setName(layerName);
        layer.setURL(layerURL);
        layer.setTMSType(TMSTYPE_OSM);
        layer.setVisible(true);

        mMap.addLayer(layer);
        mMap.save();
    }
}
