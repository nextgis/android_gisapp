/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov aka Bishop, bishop.dev@gmail.com
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.internal.view.menu.ActionMenuItemView;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplib.util.VectorCacheItem;
import com.nextgis.maplibui.ChooseLayerDialog;
import com.nextgis.maplibui.EditLayerOverlay;
import com.nextgis.maplibui.MapView;
import com.nextgis.maplibui.api.EditEventListener;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nineoldandroids.view.ViewHelper;

import java.util.List;

import static com.nextgis.mobile.util.SettingsConstants.*;

public class MapFragment
        extends Fragment
        implements MapViewEventListener, GpsEventListener, EditEventListener
{

    protected final static int mMargins     = 10;
    protected static final int mToleranceDP = 20;
    protected float mTolerancePX;

    protected MapView   mMap;
    protected ImageView mivZoomIn;
    protected ImageView mivZoomOut;

    protected TextView  mStatusSource, mStatusAccuracy, mStatusSpeed,
            mStatusAltitude, mStatusLatitude, mStatusLongitude;
    protected FrameLayout mStatusPanel;

    protected RelativeLayout mMapRelativeLayout;
    protected GpsEventSource mGpsEventSource;
    protected View mMainButton;
    protected int mMode;
    protected EditLayerOverlay mEditLayerOverlay;

    protected int mCoordinatesFormat;


    protected static final int MODE_NORMAL = 0;
    protected static final int MODE_SELECT_ACTION = 1;
    protected static final int MODE_EDIT = 2;
    protected static final String KEY_MODE = "mode";

    public MapFragment()
    {
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mTolerancePX = getActivity().getResources().getDisplayMetrics().density * mToleranceDP;
    }


    protected void setMode(int mode)
    {
        MainActivity activity = (MainActivity)getActivity();
        final Toolbar toolbar = activity.getBottomToolbar();
        switch (mode){
            case MODE_NORMAL:
                if(null != toolbar){
                    toolbar.setVisibility(View.GONE);
                }
                mMainButton.setVisibility(View.VISIBLE);
                mStatusPanel.setVisibility(View.VISIBLE);
                break;
            case MODE_EDIT:
                if(null != toolbar) {
                    mMainButton.setVisibility(View.GONE);
                    mStatusPanel.setVisibility(View.INVISIBLE);
                    toolbar.setVisibility(View.VISIBLE);
                    toolbar.getBackground().setAlpha(128);
                    Menu menu = toolbar.getMenu();
                    if (null != menu)
                        menu.clear();

                    if(null != mEditLayerOverlay) {
                        mEditLayerOverlay.setMode(EditLayerOverlay.MODE_EDIT);
                        mEditLayerOverlay.setToolbar(toolbar);
                    }
                }
                break;
            case MODE_SELECT_ACTION:
                //hide FAB, show bottom toolbar
                if(null != toolbar){
                    mMainButton.setVisibility(View.GONE);
                    mStatusPanel.setVisibility(View.INVISIBLE);
                    toolbar.setVisibility(View.VISIBLE);
                    toolbar.getBackground().setAlpha(128);
                    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch(item.getItemId()){
                                case R.id.menu_edit:
                                    setMode(MODE_EDIT);
                                    break;
                                case R.id.menu_delete:
                                    if(null != mEditLayerOverlay) {
                                        mEditLayerOverlay.deleteItem();
                                    }

                                    setMode(MODE_NORMAL);
                                    break;
                                case R.id.menu_info:
                                    //TODO: show attributes fragment
                                    // in small displays on map fragment place,
                                    // in large displays - at right side of map.
                                    // Also need the next and prev buttons to navigate throw records
                                    break;
                            }
                            return true;
                        }
                    });
                    // Inflate a menu to be displayed in the toolbar
                    Menu menu = toolbar.getMenu();
                    if(null != menu)
                        menu.clear();
                    toolbar.inflateMenu(R.menu.select_action);
                    //distributeToolbarItems(toolbar);
                }
                break;
        }
        mMode = mode;
    }

    protected void distributeToolbarItems(Toolbar toolbar){
        toolbar.setContentInsetsAbsolute(0,0);
        // Get the ChildCount of your Toolbar, this should only be 1
        int childCount = toolbar.getChildCount();
        // Get the Screen Width in pixels
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        // Create the Toolbar Params based on the screenWidth
        Toolbar.LayoutParams toolbarParams = new Toolbar.LayoutParams(screenWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Loop through the child Items
        for(int i = 0; i < childCount; i++){
            // Get the item at the current index
            View childView = toolbar.getChildAt(i);
            // If its a ViewGroup
            if(childView instanceof ViewGroup){
                // Set its layout params
                childView.setLayoutParams(toolbarParams);
                // Get the child count of this view group, and compute the item widths based on this count & screen size
                int innerChildCount = ((ViewGroup) childView).getChildCount();
                int itemWidth  = (screenWidth / innerChildCount);
                // Create layout params for the ActionMenuView
                ActionMenuView.LayoutParams params = new ActionMenuView.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                // Loop through the children
                for(int j = 0; j < innerChildCount; j++){
                    View grandChild = ((ViewGroup) childView).getChildAt(j);
                    if(grandChild instanceof ActionMenuItemView){
                        // set the layout parameters on each View
                        grandChild.setLayoutParams(params);
                    }
                }
            }
        }
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
                mMapRelativeLayout.addView(mMap, 0, new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));
            }
            mMap.invalidate();
        }

        mMainButton = view.findViewById(R.id.action_add_current_location);
        if (null != mMainButton) {
            mMainButton.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    addCurrentLocation();
                }
            });
        }

        mGpsEventSource = ((IGISApplication) getActivity().getApplication()).getGpsEventSource();
        mStatusPanel = (FrameLayout) view.findViewById(R.id.fl_status_panel);

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


    protected void removeMapButtons(RelativeLayout rl)
    {
        rl.removeViewInLayout(rl.findViewById(R.drawable.ic_minus));
        rl.removeViewInLayout(rl.findViewById(R.drawable.ic_plus));
        mivZoomIn = null;
        mivZoomOut = null;
        rl.invalidate();
    }

    protected void addMapButtons(Context context, RelativeLayout rl)
    {
        mivZoomIn = new ImageView(context);
        mivZoomIn.setImageResource(R.drawable.ic_plus);
        mivZoomIn.setId(R.drawable.ic_plus);

        mivZoomOut = new ImageView(context);
        mivZoomOut.setImageResource(R.drawable.ic_minus);
        mivZoomOut.setId(R.drawable.ic_minus);

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
        RightParams4.setMargins(mMargins + 5, mMargins - 5, mMargins + 5, mMargins - 5);
        RightParams4.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        RightParams4.addRule(RelativeLayout.CENTER_IN_PARENT);//ALIGN_PARENT_TOP
        rl.addView(mivZoomIn, RightParams4);

        final RelativeLayout.LayoutParams RightParams2 =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RightParams2.setMargins(mMargins + 5, mMargins - 5, mMargins + 5, mMargins - 5);
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

            //TODO: show select dialog
            //1. edit_point geometry
            //2. delete geometry
            //3. see attributes
            //Toast.makeText(getActivity(), "cool! geometry is pick", Toast.LENGTH_LONG).show();
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
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_MODE, mMode);
    }


    @Override
    public void onViewStateRestored(
            @Nullable
            Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);
        if(null == savedInstanceState) {
            mMode = MODE_NORMAL;
        }
        else{
            mMode = savedInstanceState.getInt(KEY_MODE);
        }

        setMode(mMode);
    }


    @Override
    public void onPause()
    {
        mGpsEventSource.removeListener(this);

        if(null != mEditLayerOverlay){
            mEditLayerOverlay.removeListener(this);
        }


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
        if (null != mMap) {
            float mMapZoom = prefs.getFloat(KEY_PREF_ZOOM_LEVEL, mMap.getMinZoom());
            double mMapScrollX = Double.longBitsToDouble(prefs.getLong(KEY_PREF_SCROLL_X, 0));
            double mMapScrollY = Double.longBitsToDouble(prefs.getLong(KEY_PREF_SCROLL_Y, 0));
            mMap.setZoomAndCenter(mMapZoom, new GeoPoint(mMapScrollX, mMapScrollY));
        }

        //change zoom controls visibility
        boolean showControls = prefs.getBoolean(KEY_PREF_SHOW_ZOOM_CONTROLS, false);
        if (showControls) {
            if (mivZoomIn == null || mivZoomOut == null)
                addMapButtons(getActivity(), mMapRelativeLayout);
        }
        else {
            removeMapButtons(mMapRelativeLayout);
        }

        mCoordinatesFormat = prefs.getInt(KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_DEGREES);
        fillStatusPanel(mGpsEventSource.getLastKnownLocation());
        mGpsEventSource.addListener(this);

        MainActivity activity = (MainActivity) getActivity();
        mEditLayerOverlay = activity.getEditLayerOverlay();
        if(null != mEditLayerOverlay){
            mEditLayerOverlay.addListener(this);
        }
    }

    protected void addCurrentLocation(){
        //show select layer dialog if several layers, else start default or custom form
        List<ILayer> layers = mMap.getVectorLayersByType(GeoConstants.GTMultiPoint | GeoConstants.GTPoint);
        if(layers.isEmpty()){
            Toast.makeText(getActivity(), getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG).show();
        }
        else if(layers.size() == 1){
            //open form
            ILayer vectorLayer = layers.get(0);
            if(vectorLayer instanceof ILayerUI){
                ILayerUI vectorLayerUI = (ILayerUI)vectorLayer;
                vectorLayerUI.showEditForm(getActivity());
            }
            else{
                Toast.makeText(getActivity(), getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG).show();
            }
        }
        else{
            //open choose dialog
            ChooseLayerDialog newChooseLayerDialog = new ChooseLayerDialog();
            newChooseLayerDialog.setTitle(getString(R.string.select_layer))
                       .setLayerList(layers)
                       .show(getActivity().getSupportFragmentManager(), "choose_layer");
        }
    }


    @Override
    public void onLongPress(MotionEvent event)
    {
        double dMinX = event.getX() - mTolerancePX;
        double dMaxX = event.getX() + mTolerancePX;
        double dMinY = event.getY() - mTolerancePX;
        double dMaxY = event.getY() + mTolerancePX;

        GeoEnvelope mapEnv = mMap.screenToMap(new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY));
        if(null == mapEnv)
            return;

        //show actions dialog
        List<ILayer> layers = mMap.getVectorLayersByType(GeoConstants.GTAny);
        List<VectorCacheItem> items = null;
        VectorLayer vectorLayer = null;
        boolean intersects = false;
        for(ILayer layer : layers){
            if(!layer.isValid())
                continue;
            ILayerView layerView = (ILayerView)layer;
            if(!layerView.isVisible())
                continue;

            vectorLayer = (VectorLayer)layer;
            items = vectorLayer.query(mapEnv);
            if(!items.isEmpty()){
                intersects = true;
                break;
            }
        }

        if(intersects){
            //add geometry to overlay

            if(null != mEditLayerOverlay) {
                mEditLayerOverlay.setFeature(vectorLayer, items.get(0));
                mEditLayerOverlay.setMode(EditLayerOverlay.MODE_HIGHLIGHT);
            }
            mMap.postInvalidate();
            //set select action mode
            setMode(MODE_SELECT_ACTION);
        }
    }


    @Override
    public void onSingleTapUp(MotionEvent event)
    {
        if(mMode == MODE_SELECT_ACTION) {
            setMode(MODE_NORMAL);

            if(null != mEditLayerOverlay){
                mEditLayerOverlay.setFeature(null, null);
            }
            mMap.postInvalidate();
        }
    }


    @Override
    public void onLocationChanged(Location location)
    {
        fillStatusPanel(location);
    }


    private void fillStatusPanel(Location location)
    {
        if (location == null)
            return;

        boolean needViewUpdate = true;
        boolean isCurrentOrientationOneLine = mStatusPanel.getChildCount() > 0 &&
                ((LinearLayout) mStatusPanel.getChildAt(0)).getOrientation() == LinearLayout.HORIZONTAL;

        View panel;
        if (!isCurrentOrientationOneLine) {
            panel = getActivity().getLayoutInflater()
                                 .inflate(R.layout.status_panel_land, mStatusPanel, false);
            defineTextViews(panel);
        } else {
            panel = mStatusPanel.getChildAt(0);
            needViewUpdate = false;
        }

        fillTextViews(location);

        if (!isFitOneLine()) {
            panel = getActivity().getLayoutInflater().inflate(R.layout.status_panel, mStatusPanel, false);
            defineTextViews(panel);
            fillTextViews(location);
            needViewUpdate = true;
        }

        if (needViewUpdate) {
            mStatusPanel.removeAllViews();
            setAlpha(panel);
            mStatusPanel.addView(panel);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setAlpha(View view) {
        float alpha = ViewHelper.getAlpha(getActivity().findViewById(R.id.main_toolbar));

        if (alpha == 1.0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            alpha = getActivity().findViewById(R.id.main_toolbar).getBackground().getAlpha() / 255f;

        ViewHelper.setAlpha(view, alpha);
    }

    private void fillTextViews(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            mStatusSource.setText(location.getExtras().getInt("satellites") + "");
            mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_location),
                    null, null, null);
        } else {
            mStatusSource.setText("");
            mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_signal_wifi),
                    null, null, null);
        }

        mStatusAccuracy.setText(String.format("%.1f %s", location.getAccuracy(),
                                              getString(R.string.unit_meter)));
        mStatusAltitude.setText(String.format("%.1f %s", location.getAltitude(),
                                              getString(R.string.unit_meter)));
        mStatusSpeed.setText(String.format("%.1f %s/%s", location.getSpeed() * 3600 / 1000,
                                           getString(R.string.unit_kilometer),
                                           getString(R.string.unit_hour)));
        mStatusLatitude.setText(
                LocationUtil.formatCoordinate(location.getLatitude(), mCoordinatesFormat) + " " +
                getString(R.string.latitude_caption_short));
        mStatusLongitude.setText(
                LocationUtil.formatCoordinate(location.getLongitude(), mCoordinatesFormat) + " " +
                getString(R.string.longitude_caption_short));
    }

    private boolean isFitOneLine() {
        mStatusLongitude.measure(0, 0);
        mStatusLatitude.measure(0, 0);
        mStatusAltitude.measure(0, 0);
        mStatusSpeed.measure(0, 0);
        mStatusAccuracy.measure(0, 0);
        mStatusSource.measure(0, 0);

        int totalWidth = mStatusSource.getMeasuredWidth() + mStatusLongitude.getMeasuredWidth() +
                         mStatusLatitude.getMeasuredWidth() + mStatusAccuracy.getMeasuredWidth() +
                         mStatusSpeed.getMeasuredWidth() + mStatusAltitude.getMeasuredWidth();

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return totalWidth < metrics.widthPixels;
//        return totalWidth < mStatusPanel.getWidth();
    }

    private void defineTextViews(View panel) {
        mStatusSource = (TextView) panel.findViewById(R.id.tv_source);
        mStatusAccuracy = (TextView) panel.findViewById(R.id.tv_accuracy);
        mStatusSpeed = (TextView) panel.findViewById(R.id.tv_speed);
        mStatusAltitude = (TextView) panel.findViewById(R.id.tv_altitude);
        mStatusLatitude = (TextView) panel.findViewById(R.id.tv_latitude);
        mStatusLongitude = (TextView) panel.findViewById(R.id.tv_longitude);
    }

    @Override
    public void onGpsStatusChanged(int event)
    {

    }


    @Override
    public void onStartEditSession()
    {
        //TODO: hide my place
    }


    @Override
    public void onFinishEditSession()
    {
        //TODO: restore my place after the end edit session
        setMode(MODE_NORMAL);
    }
}
