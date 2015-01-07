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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.api.MapEventListener;
import com.nextgis.maplibui.MapView;

import static com.nextgis.mobile.util.SettingsConstants.*;

public class MapFragment
        extends Fragment
        implements MapEventListener
{

    protected final static int mMargings = 10;

    protected MapView     mMap;
    protected ImageView   mivZoomIn;
    protected ImageView   mivZoomOut;

    protected RelativeLayout mMapRelativeLayout;
    protected boolean        mShowZoomControl;


    public MapFragment()
    {
        mShowZoomControl = true;
    }


    public boolean isShowZoomControl()
    {
        return mShowZoomControl;
    }


    public void setShowZoomControl(boolean showZoomControl)
    {
        mShowZoomControl = showZoomControl;
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        FrameLayout layout = (FrameLayout) view.findViewById(R.id.mapholder);

        //search relative view of map, if not found - add it
        if (mMap != null) {
            mMapRelativeLayout = (RelativeLayout) layout.findViewById(R.id.maprl);
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout.addView(mMap, new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if (sharedPreferences.getBoolean(KEY_PREF_SHOW_ZOOM_CONTROLS, false)) {
                    addMapButtons(view.getContext(), mMapRelativeLayout);
                }
                //TODO: The idea is to have one fab (new in Android L v5) to add new geometry to layer.
                //TODO: The zoom should be the same as scale bar: user have to choose meters, foots or zoom to seen over the map. The bar/rech shold shown only while zoom/scale is changed
                //TODO: The zoomin/zoomout buttons should be at left center or top of display and has alpha about 25%. First tap on screen make them non transparent
                //http://stackoverflow.com/questions/26740107/is-there-a-library-for-floating-action-buttons-with-labels
                //https://github.com/futuresimple/android-floating-action-button
                //http://stackoverflow.com/questions/26928976/android-floating-action-button-api-19-kitkat
                //http://stackoverflow.com/questions/24464017/android-landroid-material-circular-button
                //http://stackoverflow.com/questions/24451026/android-l-fab-button
            }
        }

        return view;
    }


    @Override
    public void onDestroyView()
    {
        if (mMap != null) {
            mMap.removeListener(this);
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout.removeView(mMap);
            }
        }

        super.onDestroyView();
    }

    protected void removeMapButtons(Context context, RelativeLayout rl)
    {
        rl.removeAllViewsInLayout();
        mivZoomIn = null;
        mivZoomOut = null;
    }

    protected void addMapButtons(Context context, RelativeLayout rl)
    {
        mivZoomIn = new ImageView(context);
        mivZoomIn.setImageResource(R.drawable.ic_plus);
        mivZoomIn.setId(R.drawable.ic_plus);

        mivZoomOut = new ImageView(context);
        mivZoomOut.setImageResource(R.drawable.ic_minus);
        //mivZoomOut.setId(R.drawable.ic_minus);

        mivZoomIn.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mMap.zoomIn();
            }
        });

        mivZoomOut.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mMap.zoomOut();
            }
        });

        final RelativeLayout.LayoutParams RightParams4 =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams4.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams4.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        RightParams4.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
        rl.addView(mivZoomIn, RightParams4);

        final RelativeLayout.LayoutParams RightParams2 =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams2.setMargins(mMargings + 5, mMargings - 5, mMargings + 5, mMargings - 5);
        RightParams2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        RightParams2.addRule(RelativeLayout.BELOW, R.drawable.ic_plus);
        rl.addView(mivZoomOut, RightParams2);

        setZoomInEnabled(mMap.canZoomIn());
        setZoomOutEnabled(mMap.canZoomOut());
    }


    @Override
    public void onLayerAdded(int id)
    {

    }


    @Override
    public void onLayerDeleted(int id)
    {

    }


    @Override
    public void onLayerChanged(int id)
    {

    }


    @Override
    public void onExtentChanged(
            float zoom,
            GeoPoint center)
    {
        setZoomInEnabled(mMap.canZoomIn());
        setZoomOutEnabled(mMap.canZoomOut());
    }


    @Override
    public void onLayersReordered()
    {

    }


    @Override
    public void onLayerDrawFinished(
            int id,
            float percent)
    {
        //TODO: invalidate map or listen event in map?
    }


    protected void setZoomInEnabled(boolean bEnabled)
    {
        if (mivZoomIn == null) {
            return;
        }
        if (bEnabled) {
            mivZoomIn.getDrawable().setAlpha(255);
        } else {
            mivZoomIn.getDrawable().setAlpha(50);
        }
    }


    protected void setZoomOutEnabled(boolean bEnabled)
    {
        if (mivZoomOut == null) {
            return;
        }
        if (bEnabled) {
            mivZoomOut.getDrawable().setAlpha(255);
        } else {
            mivZoomOut.getDrawable().setAlpha(50);
        }
    }


    public boolean onInit(MapView map)
    {
        mMap = map;
        mMap.addListener(this);
        return true;
    }


    @Override
    public void onPause()
    {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        if(null != mMap) {
            edit.putFloat(KEY_PREF_ZOOM_LEVEL, mMap.getZoomLevel());
            GeoPoint point = mMap.getMapCenter();
            edit.putLong(KEY_PREF_SCROLL_X, Double.doubleToRawLongBits(point.getX()));
            edit.putLong(KEY_PREF_SCROLL_Y, Double.doubleToRawLongBits(point.getY()));
        }
        edit.commit();

        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if(null != mMap) {
            float mMapZoom = prefs.getFloat(KEY_PREF_ZOOM_LEVEL, mMap.getMinZoom());
            double mMapScrollX = Double.longBitsToDouble(prefs.getLong(KEY_PREF_SCROLL_X, 0));
            double mMapScrollY = Double.longBitsToDouble(prefs.getLong(KEY_PREF_SCROLL_Y, 0));
            mMap.setZoomAndCenter(mMapZoom, new GeoPoint(mMapScrollX, mMapScrollY));
        }
    }
}
