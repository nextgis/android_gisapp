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

package com.nextgis.mobile.fragment;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.internal.view.menu.ActionMenuItemView;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.api.EditEventListener;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.fragment.CompassFragment;
import com.nextgis.maplibui.dialog.ChooseLayerDialog;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.overlay.CurrentLocationOverlay;
import com.nextgis.maplibui.overlay.CurrentTrackOverlay;
import com.nextgis.maplibui.overlay.EditLayerOverlay;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.MainApplication;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.MainActivity;
import com.nextgis.mobile.activity.SettingsActivity;

import java.util.List;

import static com.nextgis.mobile.util.SettingsConstants.ACTION_PREFS_LOCATION;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_COORD_FORMAT;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SCROLL_X;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SCROLL_Y;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_ZOOM_CONTROLS;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_ZOOM_LEVEL;


public class MapFragment
        extends Fragment
        implements MapViewEventListener, GpsEventListener, EditEventListener, OnClickListener
{
    protected final static int mMargins = 10;
    protected float mTolerancePX;

    protected MapViewOverlays      mMap;
    protected FloatingActionButton mivZoomIn;
    protected FloatingActionButton mivZoomOut;

    protected TextView mStatusSource, mStatusAccuracy, mStatusSpeed, mStatusAltitude,
            mStatusLatitude, mStatusLongitude;
    protected FrameLayout mStatusPanel;

    protected RelativeLayout         mMapRelativeLayout;
    protected GpsEventSource         mGpsEventSource;
    protected View                   mMainButton;
    protected int                    mMode;
    protected CurrentLocationOverlay mCurrentLocationOverlay;
    protected CurrentTrackOverlay    mCurrentTrackOverlay;
    protected EditLayerOverlay       mEditLayerOverlay;
    protected GeoPoint               mCurrentCenter;

    protected int mCoordinatesFormat;
    protected ChooseLayerDialog mChooseLayerDialog;
    protected Vibrator mVibrator;

    protected static final int MODE_NORMAL        = 0;
    protected static final int MODE_SELECT_ACTION = 1;
    protected static final int MODE_EDIT          = 2;
    protected static final int MODE_HIGHLIGHT     = 3;
    protected static final int MODE_INFO          = 4;
    protected static final int MODE_EDIT_BY_WALK  = 5;

    protected static final String KEY_MODE = "mode";
    protected boolean mShowStatusPanel, mIsCompassDragging;

    protected final int ADD_CURRENT_LOC      = 1;
    protected final int ADD_NEW_GEOMETRY     = 2;
    protected final int ADD_GEOMETRY_BY_WALK = 3;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mTolerancePX =
                getActivity().getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;
    }


    protected void setMode(int mode)
    {
        MainActivity activity = (MainActivity) getActivity();
        if (null == activity) {
            return;
        }

        final BottomToolbar toolbar = activity.getBottomToolbar();
        switch (mode) {
            case MODE_NORMAL:
                if (null != toolbar) {
                    toolbar.setVisibility(View.GONE);
                }
                if (null != mMainButton) {
                    mMainButton.setVisibility(View.VISIBLE);
                }

                if (mShowStatusPanel) {
                    mStatusPanel.setVisibility(View.VISIBLE);
                }
                break;
            case MODE_EDIT:
                if (null != toolbar) {
                    if (null != mEditLayerOverlay) {
                        if(mEditLayerOverlay.setMode(EditLayerOverlay.MODE_EDIT)) {

                            if (null != mMainButton) {
                                mMainButton.setVisibility(View.GONE);
                            }
                            mStatusPanel.setVisibility(View.INVISIBLE);
                            toolbar.setVisibility(View.VISIBLE);
                            toolbar.getBackground().setAlpha(128);
                            Menu menu = toolbar.getMenu();
                            if (null != menu) {
                                menu.clear();
                            }

                            mEditLayerOverlay.setToolbar(toolbar);
                        }
                        else{
                            Toast.makeText(getActivity(), R.string.error_unsupported_layer_type,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }
                break;
            case MODE_EDIT_BY_WALK:
                if (null != toolbar) {
                    if (null != mEditLayerOverlay) {
                        if(mEditLayerOverlay.setMode(EditLayerOverlay.MODE_EDIT_BY_WALK)) {
                            if (null != mMainButton) {
                                mMainButton.setVisibility(View.GONE);
                            }
                            mStatusPanel.setVisibility(View.INVISIBLE);
                            toolbar.setVisibility(View.VISIBLE);
                            toolbar.getBackground().setAlpha(128);
                            Menu menu = toolbar.getMenu();
                            if (null != menu) {
                                menu.clear();
                            }

                            toolbar.inflateMenu(R.menu.edit_by_walk);

                            toolbar.setOnMenuItemClickListener(
                                    new Toolbar.OnMenuItemClickListener()
                                    {
                                        @Override
                                        public boolean onMenuItemClick(MenuItem menuItem)
                                        {
                                            if (menuItem.getItemId() == R.id.menu_cancel) {
                                                mEditLayerOverlay.stopGeometryByWalk();
                                                mEditLayerOverlay.setFeature(
                                                        mEditLayerOverlay.getSelectedLayer(), Constants.NOT_FOUND);
                                                setMode(MODE_EDIT);
                                                return true;
                                            } else if (menuItem.getItemId() == R.id.menu_settings) {
                                                Intent locationSettings =
                                                        new Intent(getActivity(), SettingsActivity.class);

                                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                                                    locationSettings.setAction(ACTION_PREFS_LOCATION);
                                                } else {
                                                    locationSettings.putExtra("settings", "location");
                                                    locationSettings.putExtra(
                                                            PreferenceActivity.EXTRA_NO_HEADERS, true);
                                                    locationSettings.putExtra(
                                                            PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                                                            SettingsFragment.class.getName());
                                                    locationSettings.putExtra(
                                                            PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS,
                                                            locationSettings.getExtras());
                                                }

                                                locationSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(locationSettings);
                                                return true;
                                            }
                                            return false;
                                        }
                                    });

                            mEditLayerOverlay.setToolbar(toolbar);
                        }
                        else{
                            Toast.makeText(getActivity(), R.string.error_unsupported_layer_type,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }
                break;
            case MODE_SELECT_ACTION:
                //hide FAB, show bottom toolbar
                if (null != toolbar) {
                    if (null != mMainButton) {
                        mMainButton.setVisibility(View.GONE);
                    }
                    mStatusPanel.setVisibility(View.INVISIBLE);
                    toolbar.setNavigationIcon(R.drawable.ic_action_cancel_dark);
                    toolbar.setNavigationOnClickListener(
                            new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View view)
                                {
                                    if (null != mEditLayerOverlay) {
                                        mEditLayerOverlay.setMode(EditLayerOverlay.MODE_NONE);
                                    }
                                    setMode(MODE_NORMAL);
                                }
                            });
                    toolbar.setVisibility(View.VISIBLE);
                    toolbar.getBackground().setAlpha(128);
                    toolbar.setOnMenuItemClickListener(
                            new BottomToolbar.OnMenuItemClickListener()
                            {
                                @Override
                                public boolean onMenuItemClick(MenuItem item)
                                {
                                    switch (item.getItemId()) {
                                        case R.id.menu_edit:
                                            setMode(MODE_EDIT);
                                            break;
                                        case R.id.menu_delete:
                                            if (null != mEditLayerOverlay) {
                                                mEditLayerOverlay.deleteItem();
                                            }

                                            setMode(MODE_NORMAL);
                                            break;
                                        case R.id.menu_info:
                                            setMode(MODE_INFO);
                                            break;
                                    }
                                    return true;
                                }
                            });
                    // Inflate a menu to be displayed in the toolbar
                    Menu menu = toolbar.getMenu();
                    if (null != menu) {
                        menu.clear();
                    }
                    toolbar.inflateMenu(R.menu.select_action);
                    //distributeToolbarItems(toolbar);
                }
                break;
            case MODE_HIGHLIGHT:
                if (null != toolbar) {
                    toolbar.setVisibility(View.VISIBLE);
                    toolbar.getBackground().setAlpha(128);
                    if (null != mMainButton) {
                        mMainButton.setVisibility(View.GONE);
                    }
                    mStatusPanel.setVisibility(View.INVISIBLE);

                    if (mEditLayerOverlay != null) {
                        mEditLayerOverlay.setToolbar(toolbar);
                    }
                }
                break;
            case MODE_INFO:
                boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                //get or create fragment
                AttributesFragment attributesFragment =
                        (AttributesFragment) fragmentManager.findFragmentByTag("ATTRIBUTES");

                if (null == attributesFragment)
                    attributesFragment = new AttributesFragment();

                attributesFragment.setTablet(tabletSize);
                int container = R.id.mainview;

                if (attributesFragment.isTablet()) {
                    container = R.id.fl_attributes;
                } else {
                    Fragment hide = fragmentManager.findFragmentById(R.id.map);
                    fragmentTransaction.hide(hide);
                }

                if (!attributesFragment.isAdded()) {
                    fragmentTransaction.add(container, attributesFragment, "ATTRIBUTES")
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

                    if (!attributesFragment.isTablet())
                        fragmentTransaction.addToBackStack(null);
                }

                if (!attributesFragment.isVisible()) {
                    fragmentTransaction.show(attributesFragment);
                }

                fragmentTransaction.commit();

                attributesFragment.setSelectedFeature(
                        mEditLayerOverlay.getSelectedLayer(),
                        mEditLayerOverlay.getSelectedItemId());

                if (null != toolbar) {
                    toolbar.setVisibility(View.VISIBLE);
                    if (null != mMainButton) {
                        mMainButton.setVisibility(View.GONE);
                    }
                    mStatusPanel.setVisibility(View.INVISIBLE);

                    attributesFragment.setToolbar(toolbar, mEditLayerOverlay);
                }
                break;
        }
        mMode = mode;
    }


    protected void distributeToolbarItems(Toolbar toolbar)
    {
        toolbar.setContentInsetsAbsolute(0, 0);
        // Get the ChildCount of your Toolbar, this should only be 1
        int childCount = toolbar.getChildCount();
        // Get the Screen Width in pixels
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        // Create the Toolbar Params based on the screenWidth
        Toolbar.LayoutParams toolbarParams =
                new Toolbar.LayoutParams(screenWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Loop through the child Items
        for (int i = 0; i < childCount; i++) {
            // Get the item at the current index
            View childView = toolbar.getChildAt(i);
            // If its a ViewGroup
            if (childView instanceof ViewGroup) {
                // Set its layout params
                childView.setLayoutParams(toolbarParams);
                // Get the child count of this view group, and compute the item widths based on this count & screen size
                int innerChildCount = ((ViewGroup) childView).getChildCount();
                int itemWidth = (screenWidth / innerChildCount);
                // Create layout params for the ActionMenuView
                ActionMenuView.LayoutParams params = new ActionMenuView.LayoutParams(
                        itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                // Loop through the children
                for (int j = 0; j < innerChildCount; j++) {
                    View grandChild = ((ViewGroup) childView).getChildAt(j);
                    if (grandChild instanceof ActionMenuItemView) {
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
        MainApplication app = (MainApplication) getActivity().getApplication();

        mMap = new MapViewOverlays(getActivity(), (MapDrawable) app.getMap());
        mMap.setId(777);

        mGpsEventSource = app.getGpsEventSource();
        mCurrentLocationOverlay = new CurrentLocationOverlay(getActivity(), mMap);
        mCurrentLocationOverlay.setStandingMarker(R.mipmap.ic_location_standing);
        mCurrentLocationOverlay.setMovingMarker(R.mipmap.ic_location_moving);
        mCurrentLocationOverlay.setAutopanningEnabled(true);

        mCurrentTrackOverlay = new CurrentTrackOverlay(getActivity(), mMap);

        //add edit_point layer overlay
        mEditLayerOverlay = new EditLayerOverlay(getActivity(), mMap);

        mMap.addOverlay(mCurrentTrackOverlay);
        mMap.addOverlay(mCurrentLocationOverlay);
        mMap.addOverlay(mEditLayerOverlay);


        //search relative view of map, if not found - add it
        mMapRelativeLayout = (RelativeLayout) view.findViewById(R.id.maprl);
        if (mMapRelativeLayout != null) {
            mMapRelativeLayout.addView(
                    mMap, 0, new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT));
        }
        mMap.invalidate();

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //get or create fragment
        CompassFragment compassFragment = (CompassFragment) fragmentManager.findFragmentByTag("NEEDLE_COMPASS");
        if (null == compassFragment) {
            compassFragment = new CompassFragment();
            compassFragment.setStyle(true);
        }

        int compassContainer = R.id.fl_compass;
        if (!compassFragment.isAdded())
            fragmentTransaction.add(compassContainer, compassFragment, "NEEDLE_COMPASS")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        if (!compassFragment.isVisible()) {
            fragmentTransaction.show(compassFragment);
        }

        fragmentTransaction.commit();

        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        FrameLayout compass = (FrameLayout) view.findViewById(compassContainer);
        compass.setOnClickListener(this);
        compass.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mIsCompassDragging = true;
                mVibrator.vibrate(5);
                return true;
            }
        });
        // Thanks to http://javatechig.com/android/how-to-drag-a-view-in-android
        compass.setOnTouchListener(new View.OnTouchListener() {
            private int _xDelta;
            private int _yDelta;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mIsCompassDragging)
                    return false;

                final int X = (int) event.getRawX();
                final int Y = (int) event.getRawY();
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                        _xDelta = X - lParams.leftMargin;
                        _yDelta = Y - lParams.topMargin;
                        break;
                    case MotionEvent.ACTION_UP:
                        mIsCompassDragging = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                        layoutParams.leftMargin = X - _xDelta;
                        layoutParams.topMargin = Y - _yDelta;
                        layoutParams.rightMargin = -250;
                        layoutParams.bottomMargin = -250;
                        v.setLayoutParams(layoutParams);
                        break;
                }
                mMapRelativeLayout.invalidate();
                return true;
            }
        });

        mMainButton = view.findViewById(R.id.multiple_actions);

        View addCurrentLocation = view.findViewById(R.id.add_current_location);
        if (null != addCurrentLocation)
            addCurrentLocation.setOnClickListener(this);

        View addNewGeometry = view.findViewById(R.id.add_new_geometry);
        if (null != addNewGeometry)
            addNewGeometry.setOnClickListener(this);

        View addGeometryByWalk = view.findViewById(R.id.add_geometry_by_walk);
        if (null != addGeometryByWalk)
            addGeometryByWalk.setOnClickListener(this);

        mivZoomIn = (FloatingActionButton) view.findViewById(R.id.action_zoom_in);
        if (null != mivZoomIn)
            mivZoomIn.setOnClickListener(this);

        mivZoomOut = (FloatingActionButton) view.findViewById(R.id.action_zoom_out);
        if (null != mivZoomOut)
            mivZoomOut.setOnClickListener(this);

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


    protected void showMapButtons(
            boolean show,
            RelativeLayout rl)
    {
        if (null == rl) {
            return;
        }
        View v = rl.findViewById(R.id.action_zoom_out);
        if (null != v) {
            if (show) {
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
        }

        v = rl.findViewById(R.id.action_zoom_in);
        if (null != v) {
            if (show) {
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
        }

        rl.invalidate();
    }


    @Override
    public void onLayerAdded(int id)
    {

    }


    @Override
    public void onLayerDeleted(int id)
    {
        setMode(MODE_NORMAL);
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
        //Log.d(Constants.TAG, "onLayerDrawFinished: " + id + " percent " + percent);
        /*if (percent >= 1.0)
            mLayerDrawn++;
        MainActivity activity = (MainActivity) getActivity();
        if (null != activity){
            if (percent >= 1.0) {
                if (id == mMap.getTopVisibleLayerId()) {
                    activity.onRefresh(false, 0);
                } else {
                    activity.onRefresh(true, (mLayerDrawn * 100) / mMap.getVisibleLayerCount());
                }
            }
        }*/
        if (percent >= 1.0 && id == mMap.getMap().getId()) {
            MainActivity activity = (MainActivity) getActivity();
            if (null != activity) {
                activity.onRefresh(false, 1);
            }
        }
    }


    @Override
    public void onLayerDrawStarted()
    {
        MainActivity activity = (MainActivity) getActivity();
        if (null != activity) {
            activity.onRefresh(true, 0);
        }
    }


    protected void setZoomInEnabled(boolean bEnabled)
    {
        if (mivZoomIn == null) {
            return;
        }

        mivZoomIn.setEnabled(bEnabled);
    }


    protected void setZoomOutEnabled(boolean bEnabled)
    {
        if (mivZoomOut == null) {
            return;
        }
        mivZoomOut.setEnabled(bEnabled);
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
        if (null == savedInstanceState) {
            mMode = MODE_NORMAL;
        } else {
            mMode = savedInstanceState.getInt(KEY_MODE);
        }

        setMode(mMode);
    }


    @Override
    public void onPause()
    {
        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay.stopShowingCurrentLocation();
        }
        if (null != mGpsEventSource) {
            mGpsEventSource.removeListener(this);
        }
        if (null != mEditLayerOverlay) {
            mEditLayerOverlay.removeListener(this);
        }

        final SharedPreferences.Editor edit =
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        if (null != mMap) {
            edit.putFloat(KEY_PREF_ZOOM_LEVEL, mMap.getZoomLevel());
            GeoPoint point = mMap.getMapCenter();
            edit.putLong(KEY_PREF_SCROLL_X, Double.doubleToRawLongBits(point.getX()));
            edit.putLong(KEY_PREF_SCROLL_Y, Double.doubleToRawLongBits(point.getY()));

            mMap.removeListener(this);
        }
        edit.commit();

        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();

        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());

        boolean showControls = prefs.getBoolean(KEY_PREF_SHOW_ZOOM_CONTROLS, false);
        showMapButtons(showControls, mMapRelativeLayout);

        Log.d(Constants.TAG, "KEY_PREF_SHOW_ZOOM_CONTROLS: " + (showControls ? "ON" : "OFF"));

        if (null != mMap) {
            float mMapZoom;
            try {
                mMapZoom = prefs.getFloat(KEY_PREF_ZOOM_LEVEL, mMap.getMinZoom());
            } catch (ClassCastException e) {
                mMapZoom = mMap.getMinZoom();
            }

            double mMapScrollX;
            double mMapScrollY;
            try {
                mMapScrollX = Double.longBitsToDouble(prefs.getLong(KEY_PREF_SCROLL_X, 0));
                mMapScrollY = Double.longBitsToDouble(prefs.getLong(KEY_PREF_SCROLL_Y, 0));
            } catch (ClassCastException e) {
                mMapScrollX = 0;
                mMapScrollY = 0;
            }
            mMap.setZoomAndCenter(mMapZoom, new GeoPoint(mMapScrollX, mMapScrollY));

            mMap.addListener(this);
        }

        mCoordinatesFormat = prefs.getInt(KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_DEGREES);

        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay.updateMode(
                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .getString(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC, "3"));
            mCurrentLocationOverlay.startShowingCurrentLocation();
        }
        if (null != mGpsEventSource) {
            mGpsEventSource.addListener(this);
        }
        if (null != mEditLayerOverlay) {
            mEditLayerOverlay.addListener(this);
        }

        mShowStatusPanel = prefs.getBoolean(SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL, true);

        if (null != mStatusPanel) {
            if (mShowStatusPanel) {
                mStatusPanel.setVisibility(View.VISIBLE);
                fillStatusPanel(null);

                if (mMode != MODE_NORMAL) {
                    mStatusPanel.setVisibility(View.INVISIBLE);
                }
            } else {
                mStatusPanel.removeAllViews();
            }
        }

        mCurrentCenter = null;
    }


    protected void addNewGeometry()
    {
        //show select layer dialog if several layers, else start default or custom form
        List<ILayer> layers = mMap.getVectorLayersByType(
                GeoConstants.GTPointCheck |
                GeoConstants.GTMultiPointCheck | GeoConstants.GTLineStringCheck |
                GeoConstants.GTPolygonCheck);
        layers = removeHideLayers(layers);
        if (layers.isEmpty()) {
            Toast.makeText(
                    getActivity(), getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG)
                    .show();
        } else if (layers.size() == 1) {
            //open form
            ILayer vectorLayer = layers.get(0);
            mEditLayerOverlay.setFeature((VectorLayer) vectorLayer, Constants.NOT_FOUND);
            setMode(MODE_EDIT);

            Toast.makeText(
                    getActivity(),
                    String.format(getString(R.string.edit_layer), vectorLayer.getName()),
                    Toast.LENGTH_SHORT).show();
        } else {
            if (isDialogShown())
                return;
            //open choose edit layer dialog
            mChooseLayerDialog = new ChooseLayerDialog();
            mChooseLayerDialog.setTitle(getString(R.string.select_layer))
                    .setLayerList(layers)
                    .setCode(ADD_NEW_GEOMETRY)
                    .show(getActivity().getSupportFragmentManager(), "choose_layer");
        }
    }


    protected void addCurrentLocation()
    {
        //show select layer dialog if several layers, else start default or custom form
        List<ILayer> layers = mMap.getVectorLayersByType(
                GeoConstants.GTMultiPointCheck | GeoConstants.GTPointCheck);
        layers = removeHideLayers(layers);
        if (layers.isEmpty()) {
            Toast.makeText(
                    getActivity(), getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG)
                    .show();
        } else if (layers.size() == 1) {
            //open form
            ILayer vectorLayer = layers.get(0);
            if (vectorLayer instanceof ILayerUI) {
                IVectorLayerUI vectorLayerUI = (IVectorLayerUI) vectorLayer;
                vectorLayerUI.showEditForm(getActivity(), Constants.NOT_FOUND, null);

                Toast.makeText(
                        getActivity(),
                        String.format(getString(R.string.edit_layer), vectorLayer.getName()),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(
                        getActivity(), getString(R.string.warning_no_edit_layers),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            if (isDialogShown())
                return;
            //open choose dialog
            mChooseLayerDialog = new ChooseLayerDialog();
            mChooseLayerDialog.setTitle(getString(R.string.select_layer))
                    .setLayerList(layers)
                    .setCode(ADD_CURRENT_LOC)
                    .show(getActivity().getSupportFragmentManager(), "choose_layer");
        }
    }


    protected List<ILayer> removeHideLayers(List<ILayer> layerList)
    {
        for (int i = 0; i < layerList.size(); i++) {
            ILayerView layerView = (ILayerView) layerList.get(i);
            if (null != layerView) {
                if (!layerView.isVisible()) {
                    layerList.remove(i);
                    i--;
                }
            }
        }

        return layerList;
    }


    protected void addGeometryByWalk()
    {
        //show select layer dialog if several layers, else start default or custom form
        List<ILayer> layers = mMap.getVectorLayersByType(
                GeoConstants.GTLineStringCheck | GeoConstants.GTPolygonCheck);

        layers = removeHideLayers(layers);

        if (layers.isEmpty()) {
            Toast.makeText(
                    getActivity(), getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG)
                    .show();
        } else if (layers.size() == 1) {
            //open form
            ILayer vectorLayer = layers.get(0);
            mEditLayerOverlay.setFeature((VectorLayer) vectorLayer, Constants.NOT_FOUND);
            setMode(MODE_EDIT_BY_WALK);

            Toast.makeText(
                    getActivity(),
                    String.format(getString(R.string.edit_layer), vectorLayer.getName()),
                    Toast.LENGTH_SHORT).show();
        } else {
            if (isDialogShown())
                return;
            //open choose edit layer dialog
            mChooseLayerDialog = new ChooseLayerDialog();
            mChooseLayerDialog.setTitle(getString(R.string.select_layer))
                    .setLayerList(layers)
                    .setCode(ADD_GEOMETRY_BY_WALK)
                    .show(getActivity().getSupportFragmentManager(), "choose_layer");
        }
    }


    public void onFinishChooseLayerDialog(
            int code,
            ILayer layer)
    {
        if (code == ADD_CURRENT_LOC) {
            if (layer instanceof ILayerUI) {
                IVectorLayerUI layerUI = (IVectorLayerUI) layer;
                layerUI.showEditForm(getActivity(), Constants.NOT_FOUND, null);
            }
        } else if (code == ADD_NEW_GEOMETRY) {
            VectorLayer vectorLayer = (VectorLayer) layer;
            if(null != vectorLayer) {
                mEditLayerOverlay.setFeature(vectorLayer, Constants.NOT_FOUND);
                setMode(MODE_EDIT);
            }
        } else if (code == ADD_GEOMETRY_BY_WALK) {
            VectorLayer vectorLayer = (VectorLayer) layer;
            if(null != vectorLayer) {
                mEditLayerOverlay.setFeature(vectorLayer, Constants.NOT_FOUND);
                setMode(MODE_EDIT_BY_WALK);
            }
        }
    }


    @Override
    public void onLongPress(MotionEvent event)
    {
        if (!(mMode == MODE_NORMAL || mMode == MODE_SELECT_ACTION)) {
            return;
        }

        double dMinX = event.getX() - mTolerancePX;
        double dMaxX = event.getX() + mTolerancePX;
        double dMinY = event.getY() - mTolerancePX;
        double dMaxY = event.getY() + mTolerancePX;

        GeoEnvelope mapEnv = mMap.screenToMap(new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY));
        if (null == mapEnv) {
            return;
        }

        //show actions dialog
        List<ILayer> layers = mMap.getVectorLayersByType(GeoConstants.GTAnyCheck);
        List<Long> items = null;
        VectorLayer vectorLayer = null;
        boolean intersects = false;
        for (ILayer layer : layers) {
            if (!layer.isValid()) {
                continue;
            }
            ILayerView layerView = (ILayerView) layer;
            if (!layerView.isVisible()) {
                continue;
            }

            vectorLayer = (VectorLayer) layer;
            items = vectorLayer.query(mapEnv);
            if (!items.isEmpty()) {
                intersects = true;
                break;
            }
        }

        if (intersects) {
            //add geometry to overlay

            if (null != mEditLayerOverlay) {
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
        switch (mMode) {
            /*case MODE_SELECT_ACTION:
                setMode(MODE_NORMAL);
                if (null != mEditLayerOverlay) {
                    mEditLayerOverlay.setFeature(null, Constants.NOT_FOUND);
                    mEditLayerOverlay.setMode(EditLayerOverlay.MODE_NONE);
                }
                break;*/
            case MODE_INFO:
                if (null != mEditLayerOverlay) {
                    AttributesFragment attributesFragment =
                            (AttributesFragment) getActivity().getSupportFragmentManager()
                                    .findFragmentByTag("ATTRIBUTES");

                    if (attributesFragment != null) {
                        attributesFragment.setSelectedFeature(
                                mEditLayerOverlay.getSelectedLayer(),
                                mEditLayerOverlay.getSelectedItemId());

                        mMap.postInvalidate();
                    }
                }

                break;
        }
    }


    @Override
    public void panStart(MotionEvent e)
    {

    }


    @Override
    public void panMoveTo(MotionEvent e)
    {

    }


    @Override
    public void panStop()
    {

    }


    @Override
    public void onLocationChanged(Location location)
    {
        if (location != null) {
            if (mCurrentCenter == null) {
                mCurrentCenter = new GeoPoint();
            }

            mCurrentCenter.setCoordinates(location.getLongitude(), location.getLatitude());
            mCurrentCenter.setCRS(GeoConstants.CRS_WGS84);

            if (!mCurrentCenter.project(GeoConstants.CRS_WEB_MERCATOR)) {
                mCurrentCenter = null;
            }
        }

        fillStatusPanel(location);
    }

    @Override
    public void onBestLocationChanged(Location location) {

    }


    private void fillStatusPanel(Location location)
    {
        if (!mShowStatusPanel) //mStatusPanel.getVisibility() == FrameLayout.INVISIBLE)
        {
            return;
        }

        boolean needViewUpdate = true;
        boolean isCurrentOrientationOneLine =
                mStatusPanel.getChildCount() > 0 && ((LinearLayout) mStatusPanel.getChildAt(
                        0)).getOrientation() == LinearLayout.HORIZONTAL;

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
            panel = getActivity().getLayoutInflater()
                    .inflate(R.layout.status_panel, mStatusPanel, false);
            defineTextViews(panel);
            fillTextViews(location);
            needViewUpdate = true;
        }

        if (needViewUpdate) {
            mStatusPanel.removeAllViews();
            panel.getBackground().setAlpha(128);
            mStatusPanel.addView(panel);
        }
    }


    private void fillTextViews(Location location)
    {
        if (null == location) {
            setDefaultTextViews();
        } else {
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                String text = "";
                int satellites = location.getExtras().getInt("satellites");
                if (satellites > 0)
                    text += satellites;

                mStatusSource.setText(text);
                mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.ic_location), null, null, null);
            } else {
                mStatusSource.setText("");
                mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.ic_signal_wifi), null, null, null);
            }

            mStatusAccuracy.setText(
                    String.format(
                            "%.1f %s", location.getAccuracy(), getString(R.string.unit_meter)));
            mStatusAltitude.setText(
                    String.format(
                            "%.1f %s", location.getAltitude(), getString(R.string.unit_meter)));
            mStatusSpeed.setText(
                    String.format(
                            "%.1f %s/%s", location.getSpeed() * 3600 / 1000,
                            getString(R.string.unit_kilometer), getString(R.string.unit_hour)));
            mStatusLatitude.setText(
                    LocationUtil.formatCoordinate(location.getLatitude(), mCoordinatesFormat) +
                            " " +
                            getString(R.string.latitude_caption_short));
            mStatusLongitude.setText(
                    LocationUtil.formatCoordinate(location.getLongitude(), mCoordinatesFormat) +
                            " " +
                            getString(R.string.longitude_caption_short));
        }
    }


    private void setDefaultTextViews()
    {
        mStatusSource.setCompoundDrawables(null, null, null, null);
        mStatusSource.setText("");
        mStatusAccuracy.setText(getString(R.string.n_a));
        mStatusAltitude.setText(getString(R.string.n_a));
        mStatusSpeed.setText(getString(R.string.n_a));
        mStatusLatitude.setText(getString(R.string.n_a));
        mStatusLongitude.setText(getString(R.string.n_a));
    }


    private boolean isFitOneLine()
    {
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


    private void defineTextViews(View panel)
    {
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
    }


    @Override
    public void onFinishEditSession()
    {
        setMode(MODE_NORMAL);
    }


    public void hideBottomBar() {
        ((MainActivity) getActivity()).getBottomToolbar().setVisibility(View.GONE);
    }


    public void restoreBottomBar(int mode)
    {
        setMode(mode != -1 ? mode : mMode);
    }


    public void addLocalTMSLayer(Uri uri)
    {
        if (null != mMap) {
            mMap.addLocalTMSLayer(uri);
        }
    }


    public void addLocalVectorLayer(Uri uri)
    {
        if (null != mMap) {
            mMap.addLocalVectorLayer(uri);
        }
    }


    public void addLocalVectorLayerWithForm(Uri uri)
    {
        if (null != mMap) {
            mMap.addLocalVectorLayerWithForm(uri);
        }
    }


    public void locateCurrentPosition()
    {
        if (mCurrentCenter != null) {
            mMap.panTo(mCurrentCenter);
        } else {
            Toast.makeText(getActivity(), R.string.error_no_location, Toast.LENGTH_SHORT).show();
        }
    }


    public void addNGWLayer()
    {
        if (null != mMap) {
            mMap.addNGWLayer();
        }
    }


    public void addRemoteLayer()
    {
        if (null != mMap) {
            mMap.addRemoteLayer();
        }
    }


    public void refresh()
    {
        if (null != mMap) {
            mMap.drawMapDrawable();
        }
    }

    public boolean isDialogShown() {
        return mChooseLayerDialog != null && mChooseLayerDialog.isResumed();
    }

    protected void showFullCompass() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        FullCompassFragment compassFragment = new FullCompassFragment();

        int container = R.id.mainview;
        fragmentTransaction.add(container, compassFragment, "COMPASS_FULL")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fl_compass:
                showFullCompass();
                break;
            case R.id.add_current_location:
                if (v.isEnabled())
                    addCurrentLocation();
                break;
            case R.id.add_new_geometry:
                if (v.isEnabled())
                    addNewGeometry();
                break;
            case R.id.add_geometry_by_walk:
                if (v.isEnabled())
                    addGeometryByWalk();
                break;
            case R.id.action_zoom_in:
                if (v.isEnabled())
                    mMap.zoomIn();
                break;
            case R.id.action_zoom_out:
                if (v.isEnabled())
                    mMap.zoomOut();
                break;
        }
    }
}
