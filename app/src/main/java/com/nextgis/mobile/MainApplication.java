/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.mapui.LayerFactoryUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.mapui.TrackLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.maplibui.service.TileDownloadService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.activity.SettingsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.MAP_EXT;
import static com.nextgis.maplib.util.GeoConstants.TMSTYPE_OSM;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;
import static com.nextgis.mobile.util.SettingsConstants.AUTHORITY;
import static com.nextgis.mobile.util.SettingsConstants.FIRSTSTART_DOWNLOADZOOM;


public class MainApplication extends GISApplication
{
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

        String mapPath = sharedPreferences.getString(SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());
        String mapName = sharedPreferences.getString(SettingsConstantsUI.KEY_PREF_MAP_NAME, "default");

        File mapFullPath = new File(mapPath, mapName + MAP_EXT);

        final Bitmap bkBitmap = BitmapFactory.decodeResource(
                getResources(), com.nextgis.maplibui.R.drawable.bk_tile);
        mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactoryUI());
        mMap.setName(mapName);
        mMap.load();

        checkTracksLayerExist();

        return mMap;
    }


    protected void checkTracksLayerExist()
    {
        List<ILayer> tracks = new ArrayList<>();
        LayerGroup.getLayersByType(mMap, Constants.LAYERTYPE_TRACKS, tracks);
        if (tracks.isEmpty()) {
            String trackLayerName = getString(R.string.tracks);
            TrackLayerUI trackLayer =
                    new TrackLayerUI(getApplicationContext(), mMap.createLayerStorage("tracks"));
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
    public void showSettings()
    {
        Intent intentSet = new Intent(this, SettingsActivity.class);
        intentSet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentSet);
    }

    @Override
    protected void onFirstRun()
    {
        //add OpenStreetMap layer on application first run
        String layerName = getString(R.string.osm);
        String layerURL = SettingsConstantsUI.OSM_URL;
        RemoteTMSLayerUI layer =
                new RemoteTMSLayerUI(getApplicationContext(), mMap.createLayerStorage("osm"));
        layer.setName(layerName);
        layer.setURL(layerURL);
        layer.setTMSType(TMSTYPE_OSM);
        layer.setVisible(true);
        layer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
        layer.setMaxZoom(18);

        mMap.addLayer(layer);
        mMap.moveLayer(0, layer);

        //start tiles download
        final int zoomFrom = (int) mMap.getMinZoom();
        final int zoomTo = FIRSTSTART_DOWNLOADZOOM > zoomFrom ? FIRSTSTART_DOWNLOADZOOM : zoomFrom;
        final int layerId = layer.getId();
        final GeoEnvelope env = mMap.getFullBounds();

        //start download service
        Intent intent = new Intent(this, TileDownloadService.class);
        intent.setAction(TileDownloadService.ACTION_ADD_TASK);
        intent.putExtra(ConstantsUI.KEY_LAYER_ID, layerId);
        intent.putExtra(TileDownloadService.KEY_ZOOM_FROM, zoomFrom);
        intent.putExtra(TileDownloadService.KEY_ZOOM_TO, zoomTo);
        intent.putExtra(TileDownloadService.KEY_MINX, env.getMinX());
        intent.putExtra(TileDownloadService.KEY_MAXX, env.getMaxX());
        intent.putExtra(TileDownloadService.KEY_MINY, env.getMinY());
        intent.putExtra(TileDownloadService.KEY_MAXY, env.getMaxY());

        startService(intent);

        // create empty layers for first experimental editing
        mMap.addLayer(createEmptyVectorLayer(getString(R.string.points_for_edit),   "vector_a", GeoConstants.GTPoint));
        mMap.addLayer(createEmptyVectorLayer(getString(R.string.lines_for_edit),    "vector_b", GeoConstants.GTLineString));
        mMap.addLayer(createEmptyVectorLayer(getString(R.string.polygons_for_edit), "vector_c", GeoConstants.GTPolygon));

        mMap.save();
    }


    protected ILayer createEmptyVectorLayer(
            String layerName,
            String layerPath,
            int layerType)
    {
        VectorLayerUI vectorLayer = new VectorLayerUI(this, mMap.createLayerStorage(layerPath));
        vectorLayer.setName(layerName);
        vectorLayer.setVisible(true);
        vectorLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
        vectorLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

        List<Field> fields = new ArrayList<>(2);
        fields.add(new Field(GeoConstants.FTInteger, "FID", "FID"));
        fields.add(new Field(GeoConstants.FTString, "TEXT", "TEXT"));

        vectorLayer.create(layerType, fields);
        return vectorLayer;
    }
}
