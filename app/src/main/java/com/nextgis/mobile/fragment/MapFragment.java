/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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


import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
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

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoLinearRing;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.api.EditEventListener;
import com.nextgis.maplibui.api.ILayerUI;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.dialog.ChooseLayerDialog;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.fragment.CompassFragment;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.overlay.CurrentLocationOverlay;
import com.nextgis.maplibui.overlay.CurrentTrackOverlay;
import com.nextgis.maplibui.overlay.EditLayerOverlay;
import com.nextgis.maplibui.overlay.RulerOverlay;
import com.nextgis.maplibui.service.WalkEditService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.NotificationHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.MainApplication;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.MainActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_COMPASS;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_MEASURING;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_SCALE_RULER;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_ZOOM_CONTROLS;

/**
 * Main map fragment
 */
public class MapFragment
        extends Fragment
        implements MapViewEventListener, GpsEventListener, EditEventListener, OnClickListener, RulerOverlay.OnRulerChanged {
    protected float mTolerancePX;

    protected MainApplication      mApp;
    protected MainActivity         mActivity;
    protected MapViewOverlays      mMap;
    protected FloatingActionButton mivZoomIn;
    protected FloatingActionButton mivZoomOut;
    protected FloatingActionButton mRuler;
    protected FloatingActionButton mAddNewGeometry;
    protected FloatingActionButton mAddPointButton;

    protected TextView mStatusSource, mStatusAccuracy, mStatusSpeed, mStatusAltitude,
            mStatusLatitude, mStatusLongitude;
    protected FrameLayout mStatusPanel;
    protected LinearLayout mScaleRulerLayout;
    protected TextView mScaleRulerText;
    protected ImageView mScaleRuler;

    protected RelativeLayout         mMapRelativeLayout;
    protected GpsEventSource         mGpsEventSource;
    protected View                   mMainButton;
    protected int                    mMode;
    protected CurrentLocationOverlay mCurrentLocationOverlay;
    protected CurrentTrackOverlay    mCurrentTrackOverlay;
    protected EditLayerOverlay       mEditLayerOverlay;
    protected RulerOverlay           mRulerOverlay;
    protected GeoPoint               mCurrentCenter;
    protected VectorLayer            mSelectedLayer;

    protected int mCoordinatesFormat, mCoordinatesFraction;
    protected ChooseLayerDialog mChooseLayerDialog;
    protected Vibrator mVibrator;

    public static final int MODE_NORMAL        = 0;
    public static final int MODE_SELECT_ACTION = 1;
    public static final int MODE_EDIT          = 2;
    public static final int MODE_INFO          = 3;
    public static final int MODE_EDIT_BY_WALK  = 4;

    protected static final String KEY_MODE = "mode";
    protected static final String BUNDLE_KEY_LAYER = "layer";
    protected static final String BUNDLE_KEY_FEATURE_ID = "feature";
    protected static final String BUNDLE_KEY_SAVED_FEATURE = "feature_blob";
    protected static final String BUNDLE_KEY_IS_MEASURING = "is_measuring";
    protected boolean mIsCompassDragging;
    protected int mStatusPanelMode;
    protected onModeChange mModeListener;

    protected final int ADD_CURRENT_LOC         = 1;
    public static final int EDIT_LAYER          = 2;
    protected final int ADD_GEOMETRY_BY_WALK    = 3;
    protected final int ADD_POINT_BY_TAP        = 4;

    public interface onModeChange {
        void onModeChangeListener();
    }

    public void setOnModeChangeListener(onModeChange listener) {
        mModeListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mActivity = (MainActivity) getActivity();
        mTolerancePX = mActivity.getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;

        mApp = (MainApplication) mActivity.getApplication();
        mVibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        mGpsEventSource = mApp.getGpsEventSource();

        mMap = new MapViewOverlays(mActivity, (MapDrawable) mApp.getMap());
        mMap.setId(R.id.map_view);

        mEditLayerOverlay = new EditLayerOverlay(mActivity, mMap);
    }

    public void restartGpsListener() {
        mGpsEventSource.removeListener(this);
        mGpsEventSource.addListener(this);
    }

    public EditLayerOverlay getEditLayerOverlay() {
        return mEditLayerOverlay;
    }

    public boolean isEditMode() {
        return mMode == MODE_EDIT || mMode == MODE_EDIT_BY_WALK;
    }

    public int getMode() {
        return mMode;
    }

    public boolean onOptionsItemSelected(int id) {
        switch (id) {
            case android.R.id.home:
                cancelEdits();
                return true;
            case R.id.menu_edit_by_walk:
                setMode(MODE_EDIT_BY_WALK);
            default:
                return mEditLayerOverlay.onOptionsItemSelected(id);
        }
    }


    public boolean saveEdits() {
        Feature feature = mEditLayerOverlay.getSelectedFeature();
        long featureId = NOT_FOUND;
        GeoGeometry geometry = null;

        if (feature != null) {
            geometry = feature.getGeometry();
            featureId = feature.getId();
        }

        if (!isGeometryValid(geometry))
            return false;

        mEditLayerOverlay.setHasEdits(false);
        if (mMode == MODE_EDIT_BY_WALK) {
            mEditLayerOverlay.stopGeometryByWalk();
            setMode(MODE_EDIT);
            mEditLayerOverlay.clearHistory();
            mEditLayerOverlay.defineUndoRedo();
        }

        if (mSelectedLayer != null) {
            if (featureId == NOT_FOUND) {
                //show attributes edit activity
                IVectorLayerUI vectorLayerUI = (IVectorLayerUI) mSelectedLayer;
                vectorLayerUI.showEditForm(mActivity, featureId, geometry);
            } else {
                Uri uri = Uri.parse("content://" + mApp.getAuthority() + "/" + mSelectedLayer.getPath().getName());
                uri = ContentUris.withAppendedId(uri, featureId);
                ContentValues values = new ContentValues();

                try {
                    values.put(FIELD_GEOM, geometry.toBlob());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mActivity.getContentResolver().update(uri, values, null, null);
                setMode(MODE_SELECT_ACTION);
            }
        }

        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mMode == MODE_INFO || resultCode != Activity.RESULT_OK)
            return;

        if (requestCode == IVectorLayerUI.MODIFY_REQUEST && data != null) {
            long id = data.getLongExtra(ConstantsUI.KEY_FEATURE_ID, NOT_FOUND);

            if (id != NOT_FOUND) {
                mEditLayerOverlay.setSelectedFeature(id);
                if (mSelectedLayer != null)
                    mSelectedLayer.showFeature(id);
                setMode(MODE_SELECT_ACTION);
            }
        } else if (mEditLayerOverlay.getSelectedFeatureGeometry() != null)
            mEditLayerOverlay.setHasEdits(true);
    }


    protected boolean isGeometryValid(GeoGeometry geometry) {
        if (!hasMinimumPoints(geometry)) {
            Toast.makeText(getContext(), R.string.not_enough_points, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (geometry instanceof GeoPolygon) {
            if (((GeoPolygon) geometry).intersects()) {
                Toast.makeText(getContext(), R.string.self_intersection, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (!((GeoPolygon) geometry).isHolesInside()) {
                Toast.makeText(getContext(), R.string.ring_outside, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (((GeoPolygon) geometry).isHolesIntersect()) {
                Toast.makeText(getContext(), R.string.rings_intersection, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (geometry instanceof GeoMultiPolygon) {
            if (((GeoMultiPolygon) geometry).isSelfIntersects()) {
                Toast.makeText(getContext(), R.string.self_intersection, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (!((GeoMultiPolygon) geometry).isHolesInside()) {
                Toast.makeText(getContext(), R.string.ring_outside, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (((GeoMultiPolygon) geometry).isHolesIntersect()) {
                Toast.makeText(getContext(), R.string.rings_intersection, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }


    protected boolean hasMinimumPoints(GeoGeometry geometry) {
        if (null == geometry)
            return false;

        switch (geometry.getType()) {
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                return true;
            case GeoConstants.GTLineString:
                GeoLineString line = (GeoLineString) geometry;
                return line.getPointCount() > 1;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLine = (GeoMultiLineString) geometry;
                for (int i = 0; i < multiLine.size(); i++) {
                    GeoLineString subLine = multiLine.get(i);
                    if (subLine.getPointCount() > 1) {
                        return true;
                    }
                }
                return false;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon) geometry;
                GeoLinearRing ring = polygon.getOuterRing();
                return ring.getPointCount() > 2;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon) geometry;
                for (int i = 0; i < multiPolygon.size(); i++) {
                    GeoPolygon subPolygon = multiPolygon.get(i);
                    if (subPolygon.getOuterRing().getPointCount() > 2) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }


    public boolean hasEdits() {
        return mEditLayerOverlay != null && mEditLayerOverlay.hasEdits();
    }


    public void cancelEdits() {
//        if (mEditLayerOverlay.hasEdits()) TODO prompt dialog
//            return;

        // restore
        mEditLayerOverlay.setHasEdits(false);
        if (mMode == MODE_EDIT_BY_WALK) {
            mEditLayerOverlay.stopGeometryByWalk(); // TODO toast?
            setMode(MODE_EDIT);
            mEditLayerOverlay.clearHistory();
            mEditLayerOverlay.defineUndoRedo();
        }

        long featureId = mEditLayerOverlay.getSelectedFeatureId();
        mEditLayerOverlay.setSelectedFeature(featureId);
        setMode(MODE_SELECT_ACTION);
    }


    protected void setMode(int mode) {
        mMode = mode;

        hideMainButton();
        hideAddByTapButton();
        hideRulerButton();

        final BottomToolbar toolbar = mActivity.getBottomToolbar();
        toolbar.getBackground().setAlpha(128);
        toolbar.setVisibility(View.VISIBLE);
        mActivity.showDefaultToolbar();

        if (mStatusPanelMode != 3)
            mStatusPanel.setVisibility(View.INVISIBLE);

        switch (mode) {
            case MODE_NORMAL:
                if (mSelectedLayer != null)
                    mSelectedLayer.setLocked(false);

                mSelectedLayer = null;
                toolbar.setVisibility(View.GONE);
                showMainButton();
                showRulerButton();
                if (mStatusPanelMode != 0)
                    mStatusPanel.setVisibility(View.VISIBLE);

                mEditLayerOverlay.showAllFeatures();
                mEditLayerOverlay.setMode(EditLayerOverlay.MODE_NONE);
                break;
            case MODE_EDIT:
                if (mSelectedLayer == null) {
                    setMode(MODE_NORMAL);
                    return;
                }

                mSelectedLayer.setLocked(true);
                mActivity.showEditToolbar();
                mEditLayerOverlay.setMode(EditLayerOverlay.MODE_EDIT);
                toolbar.setOnMenuItemClickListener(
                        new BottomToolbar.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                return onOptionsItemSelected(menuItem.getItemId());
                            }
                        });
                break;
            case MODE_EDIT_BY_WALK:
                mSelectedLayer.setLocked(true);
                mActivity.showEditToolbar();
                mEditLayerOverlay.setMode(EditLayerOverlay.MODE_EDIT_BY_WALK);
                break;
            case MODE_SELECT_ACTION:
                if (mSelectedLayer == null) {
                    setMode(MODE_NORMAL);
                    return;
                }

                mSelectedLayer.setLocked(true);
                toolbar.setTitle(null);
                toolbar.getMenu().clear();
                toolbar.inflateMenu(R.menu.select_action);
                toolbar.setNavigationIcon(R.drawable.ic_action_cancel_dark);

                toolbar.setNavigationOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                setMode(MODE_NORMAL);
                            }
                        });

                toolbar.setOnMenuItemClickListener(
                        new BottomToolbar.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (mSelectedLayer == null)
                                    return false;

                                switch (item.getItemId()) {
                                    case R.id.menu_feature_add:
                                        mEditLayerOverlay.setSelectedFeature(new Feature());
                                        mEditLayerOverlay.createNewGeometry();
                                        setMode(MODE_EDIT);
                                        mEditLayerOverlay.saveToHistory();
                                        mEditLayerOverlay.setHasEdits(true);
                                        break;
                                    case R.id.menu_feature_edit:
                                        setMode(MODE_EDIT);
                                        mEditLayerOverlay.saveToHistory();
                                        mEditLayerOverlay.setHasEdits(false);
                                        break;
                                    case R.id.menu_feature_delete:
                                        deleteFeature();
                                        break;
                                    case R.id.menu_feature_attributes:
                                        setMode(MODE_INFO);
                                        break;
                                }

                                return true;
                            }
                        });

                mEditLayerOverlay.setMode(EditLayerOverlay.MODE_HIGHLIGHT);
                break;
            case MODE_INFO:
                if (mSelectedLayer == null) {
                    setMode(MODE_NORMAL);
                    return;
                }

                mSelectedLayer.setLocked(true);
                boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
                FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
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

                attributesFragment.setSelectedFeature(mSelectedLayer,
                        mEditLayerOverlay.getSelectedFeatureId());

                attributesFragment.setToolbar(toolbar, mEditLayerOverlay);
                break;
        }

        if (mModeListener != null)
            mModeListener.onModeChangeListener();

        setMarginsToPanel();
        defineMenuItems();
    }

    protected void defineMenuItems() {
        if (mMode == MODE_NORMAL || mMode == MODE_INFO)
            return;

        if (mSelectedLayer == null) {
            setMode(MODE_NORMAL);
            return;
        }

        boolean noFeature = mEditLayerOverlay.getSelectedFeatureGeometry() == null;
        long featureId = mEditLayerOverlay.getSelectedFeatureId();

        String featureName = String.format(getString(R.string.feature_n), featureId);
        String labelField = mSelectedLayer.getPreferences().getString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, FIELD_ID);
        if (!labelField.equals(FIELD_ID) && !noFeature && featureId != NOT_FOUND) {
            Feature feature = mSelectedLayer.getFeature(featureId);
            if (feature != null)
                featureName = feature.getFieldValueAsString(labelField);
        }

        featureName = noFeature ? getString(R.string.nothing_selected) :
                featureId == Constants.NOT_FOUND ? getString(R.string.new_feature) : featureName;
        mActivity.setTitle(featureName);
        mActivity.setSubtitle(mSelectedLayer.getName());

        boolean hasSelectedFeature = mEditLayerOverlay.getSelectedFeature() != null && !noFeature;
        BottomToolbar toolbar = mActivity.getBottomToolbar();
        for (int i = 0; i < toolbar.getMenu().size(); i++) {
            MenuItem item = toolbar.getMenu().findItem(R.id.menu_feature_delete);
            if (item != null)
                ControlHelper.setEnabled(item, hasSelectedFeature);

            item = toolbar.getMenu().findItem(R.id.menu_feature_edit);
            if (item != null)
                ControlHelper.setEnabled(item, hasSelectedFeature);

            item = toolbar.getMenu().findItem(R.id.menu_feature_attributes);
            if (item != null)
                ControlHelper.setEnabled(item, hasSelectedFeature);
        }
    }


    public void deleteFeature() {
        final long selectedFeatureId = mEditLayerOverlay.getSelectedFeatureId();
        mSelectedLayer.hideFeature(selectedFeatureId);
        mEditLayerOverlay.setSelectedFeature(null);
        defineMenuItems();
        final VectorLayer layer = mSelectedLayer;

        Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.mainview), getActivity().getString(R.string.delete_item_done), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        layer.showFeature(selectedFeatureId);
                        mEditLayerOverlay.setSelectedFeature(selectedFeatureId);
                        defineMenuItems();
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_MANUAL)
                            return;
                        if (event != DISMISS_EVENT_ACTION)
                            layer.deleteAddChanges(selectedFeatureId);
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                        super.onShown(snackbar);
                    }
                });

        View view = snackbar.getView();
        TextView textView = (TextView) view.findViewById(R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(mActivity, com.nextgis.maplibui.R.color.color_white));
        snackbar.show();
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mCurrentLocationOverlay = new CurrentLocationOverlay(mActivity, mMap);
        mCurrentLocationOverlay.setStandingMarker(R.mipmap.ic_location_standing);
        mCurrentLocationOverlay.setMovingMarker(R.mipmap.ic_location_moving);
        mCurrentLocationOverlay.setAutopanningEnabled(true);

        mCurrentTrackOverlay = new CurrentTrackOverlay(mActivity, mMap);
        mRulerOverlay = new RulerOverlay(mActivity, mMap);

        mMap.addOverlay(mCurrentTrackOverlay);
        mMap.addOverlay(mCurrentLocationOverlay);
        mMap.addOverlay(mEditLayerOverlay);
        mMap.addOverlay(mRulerOverlay);

        //search relative view of map, if not found - add it
        mMapRelativeLayout = (RelativeLayout) view.findViewById(R.id.maprl);
        if (mMapRelativeLayout != null) {
            mMapRelativeLayout.addView(
                    mMap, 0, new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT));
        }
        mMap.invalidate();

        mMainButton = view.findViewById(R.id.multiple_actions);
        mAddPointButton = (FloatingActionButton) view.findViewById(R.id.add_point_by_tap);
        mAddPointButton.setOnClickListener(this);

        View addCurrentLocation = view.findViewById(R.id.add_current_location);
        addCurrentLocation.setOnClickListener(this);

        mAddNewGeometry = (FloatingActionButton) view.findViewById(R.id.add_new_geometry);
        mAddNewGeometry.setOnClickListener(this);
        mRuler = (FloatingActionButton) view.findViewById(R.id.action_ruler);
        mRuler.setOnClickListener(this);

        View addGeometryByWalk = view.findViewById(R.id.add_geometry_by_walk);
        addGeometryByWalk.setOnClickListener(this);

        mivZoomIn = (FloatingActionButton) view.findViewById(R.id.action_zoom_in);
        mivZoomIn.setOnClickListener(this);

        mivZoomOut = (FloatingActionButton) view.findViewById(R.id.action_zoom_out);
        mivZoomOut.setOnClickListener(this);

        mStatusPanel = (FrameLayout) view.findViewById(R.id.fl_status_panel);
        mScaleRuler = (ImageView) view.findViewById(R.id.iv_ruler);
        mScaleRulerText = (TextView) view.findViewById(R.id.tv_ruler);
        mScaleRulerLayout = (LinearLayout) view.findViewById(R.id.ll_ruler);
        drawScaleRuler();

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


    protected void drawScaleRuler() {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 10, getResources().getDisplayMetrics());
        int notch = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, getResources().getDisplayMetrics());
        Bitmap ruler = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(ruler);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ContextCompat.getColor(getActivity(), R.color.primary_dark));
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(0, px, px, px, paint);
        canvas.drawLine(0, px, 0, 0, paint);
        canvas.drawLine(0, 0, notch, 0, paint);
        canvas.drawLine(px, px, px, px - notch, paint);
        mScaleRuler.setImageBitmap(ruler);
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
        mScaleRulerText.setText(getRulerText());
    }


    protected String getRulerText() {
        GeoEnvelope left = new GeoEnvelope(mScaleRuler.getLeft(), mScaleRuler.getLeft(), mScaleRuler.getTop(), mScaleRuler.getTop());
        left = mMap.screenToMap(left);
        GeoEnvelope right = new GeoEnvelope(mScaleRuler.getRight(), mScaleRuler.getRight(), mScaleRuler.getTop(), mScaleRuler.getTop());
        right = mMap.screenToMap(right);

        GeoPoint p1 = new GeoPoint(left.getMaxX(), left.getMaxY());
        GeoPoint p2 = new GeoPoint(right.getMaxX(), right.getMaxY());
        p1.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        p2.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        GeoLineString s = new GeoLineString();
        s.add(p1);
        s.add(p2);

        return LocationUtil.formatLength(getContext(), s.getLength(), 0);
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
        MainActivity activity = (MainActivity) mActivity;
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
            if (null != mActivity) {
                mActivity.onRefresh(false);
            }
        }
    }


    @Override
    public void onLayerDrawStarted()
    {
        if (null != mActivity) {
            mActivity.onRefresh(true);
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
        outState.putBoolean(BUNDLE_KEY_IS_MEASURING, mRulerOverlay.isMeasuring());
        outState.putInt(KEY_MODE, mMode);
        outState.putInt(BUNDLE_KEY_LAYER, null == mSelectedLayer ? Constants.NOT_FOUND : mSelectedLayer.getId());

        Feature feature = mEditLayerOverlay.getSelectedFeature();
        outState.putLong(BUNDLE_KEY_FEATURE_ID, null == feature ? Constants.NOT_FOUND : feature.getId());

        if (null != feature && feature.getGeometry() != null) {
            try {
                outState.putByteArray(BUNDLE_KEY_SAVED_FEATURE, feature.getGeometry().toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

            int layerId = savedInstanceState.getInt(BUNDLE_KEY_LAYER);
            ILayer layer = mMap.getLayerById(layerId);
            Feature feature = null;

            if (null != layer && layer instanceof VectorLayer) {
                mSelectedLayer = (VectorLayer) layer;

                if (savedInstanceState.containsKey(BUNDLE_KEY_SAVED_FEATURE)) {
                    GeoGeometry geometry = null;

                    try {
                        geometry = GeoGeometryFactory.fromBlob(savedInstanceState.getByteArray(BUNDLE_KEY_SAVED_FEATURE));
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    feature = new Feature();
                    feature.setId(savedInstanceState.getLong(BUNDLE_KEY_FEATURE_ID));
                    feature.setGeometry(geometry);
                }
            }

            mEditLayerOverlay.setSelectedLayer(mSelectedLayer);
            mEditLayerOverlay.setSelectedFeature(feature);
        }

        if (WalkEditService.isServiceRunning(getContext())) {
            SharedPreferences preferences = getContext().getSharedPreferences(WalkEditService.TEMP_PREFERENCES, Constants.MODE_MULTI_PROCESS);
            int layerId = preferences.getInt(ConstantsUI.KEY_LAYER_ID, NOT_FOUND);
            long featureId = preferences.getLong(ConstantsUI.KEY_FEATURE_ID, NOT_FOUND);
            ILayer layer = mMap.getMap().getLayerById(layerId);
            if (layer != null && layer instanceof VectorLayer) {
                mSelectedLayer = (VectorLayer) layer;
                mEditLayerOverlay.setSelectedLayer(mSelectedLayer);

                if (featureId > NOT_FOUND)
                    mEditLayerOverlay.setSelectedFeature(featureId);
                else
                    mEditLayerOverlay.newGeometryByWalk();

                GeoGeometry geometry = GeoGeometryFactory.fromWKT(preferences.getString(ConstantsUI.KEY_GEOMETRY, ""), GeoConstants.CRS_WEB_MERCATOR);
                if (geometry != null)
                    mEditLayerOverlay.setGeometryFromWalkEdit(geometry);

                mMode = MODE_EDIT_BY_WALK;
            }
        }

        setMode(mMode);

        if (savedInstanceState != null && savedInstanceState.getBoolean(BUNDLE_KEY_IS_MEASURING, false))
            startMeasuring();
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
                PreferenceManager.getDefaultSharedPreferences(mActivity).edit();
        if (null != mMap) {
            edit.putFloat(SettingsConstantsUI.KEY_PREF_ZOOM_LEVEL, mMap.getZoomLevel());
            GeoPoint point = mMap.getMapCenter();
            edit.putLong(SettingsConstantsUI.KEY_PREF_SCROLL_X, Double.doubleToRawLongBits(point.getX()));
            edit.putLong(SettingsConstantsUI.KEY_PREF_SCROLL_Y, Double.doubleToRawLongBits(point.getY()));

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
                PreferenceManager.getDefaultSharedPreferences(mActivity);

        boolean showControls = prefs.getBoolean(KEY_PREF_SHOW_ZOOM_CONTROLS, false);
        showMapButtons(showControls, mMapRelativeLayout);

        if(Constants.DEBUG_MODE)
            Log.d(Constants.TAG, "KEY_PREF_SHOW_ZOOM_CONTROLS: " + (showControls ? "ON" : "OFF"));

        showControls = prefs.getBoolean(KEY_PREF_SHOW_SCALE_RULER, true);
        if (showControls)
            mScaleRulerLayout.setVisibility(View.VISIBLE);
        else
            mScaleRulerLayout.setVisibility(View.GONE);

        showControls = prefs.getBoolean(KEY_PREF_SHOW_MEASURING, false);
        if (showControls)
            mRuler.setVisibility(View.VISIBLE);
        else
            mRuler.setVisibility(View.GONE);

        if (null != mMap) {
            mMap.getMap().setBackground(mApp.getMapBackground());
            float mMapZoom;
            try {
                mMapZoom = prefs.getFloat(SettingsConstantsUI.KEY_PREF_ZOOM_LEVEL, mMap.getMinZoom());
            } catch (ClassCastException e) {
                mMapZoom = mMap.getMinZoom();
            }

            double mMapScrollX;
            double mMapScrollY;
            try {
                mMapScrollX = Double.longBitsToDouble(prefs.getLong(SettingsConstantsUI.KEY_PREF_SCROLL_X, 0));
                mMapScrollY = Double.longBitsToDouble(prefs.getLong(SettingsConstantsUI.KEY_PREF_SCROLL_Y, 0));
            } catch (ClassCastException e) {
                mMapScrollX = 0;
                mMapScrollY = 0;
            }
            mMap.setZoomAndCenter(mMapZoom, new GeoPoint(mMapScrollX, mMapScrollY));
            mMap.addListener(this);
            mMap.post(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
        }

        String coordinatesFormat = prefs.getString(SettingsConstantsUI.KEY_PREF_COORD_FORMAT, Location.FORMAT_DEGREES + "");
        if (FileUtil.isIntegerParseInt(coordinatesFormat))
            mCoordinatesFormat = Integer.parseInt(coordinatesFormat);
        else
            mCoordinatesFormat = Location.FORMAT_DEGREES;
        mCoordinatesFraction = prefs.getInt(SettingsConstantsUI.KEY_PREF_COORD_FRACTION, 6);

        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay.updateMode(
                    PreferenceManager.getDefaultSharedPreferences(mActivity)
                            .getString(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC, "3"));
            mCurrentLocationOverlay.startShowingCurrentLocation();
        }
        if (null != mGpsEventSource) {
            mGpsEventSource.addListener(this);
            NotificationHelper.showLocationInfo(getActivity());
        }
        if (null != mEditLayerOverlay) {
            mEditLayerOverlay.addListener(this);
        }

        try {
            String statusPanelModeStr = prefs.getString(SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL, "0");
            if (FileUtil.isIntegerParseInt(statusPanelModeStr))
                mStatusPanelMode = Integer.parseInt(statusPanelModeStr);
            else
                mStatusPanelMode = 0;
        } catch (ClassCastException e){
            mStatusPanelMode = 0;
            if(Constants.DEBUG_MODE)
                Log.d(Constants.TAG, "Previous version of KEY_PREF_SHOW_STATUS_PANEL of bool type. Let set it to 0");
        }

        if (null != mStatusPanel) {
            if (mStatusPanelMode != 0) {
                mStatusPanel.setVisibility(View.VISIBLE);
                fillStatusPanel(null);

                if (mMode != MODE_NORMAL && mStatusPanelMode != 3)
                    mStatusPanel.setVisibility(View.INVISIBLE);
            } else {
                mStatusPanel.removeAllViews();
            }

            setMarginsToPanel();
        }

        boolean showCompass = prefs.getBoolean(KEY_PREF_SHOW_COMPASS, true);
        checkCompass(showCompass);

        mCurrentCenter = null;
    }




    protected void setMarginsToPanel() {
        final BottomToolbar toolbar = mActivity.getBottomToolbar();

        toolbar.post(new Runnable() {
            @Override
            public void run() {
                boolean isToolbarVisible = toolbar.getVisibility() == View.VISIBLE;
                boolean isPanelVisible = mStatusPanel.getVisibility() == View.VISIBLE;
                int toolbarHeight = toolbar.getMeasuredHeight();

                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mStatusPanel.getLayoutParams();

                int bottom;
                if (isToolbarVisible && isPanelVisible)
                    bottom = toolbarHeight;
                else
                    bottom = 0;

                lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, bottom);
                mStatusPanel.setLayoutParams(lp);

                if (isToolbarVisible && !isPanelVisible)
                    bottom = toolbarHeight;
                else
                    bottom = 0;

                mStatusPanel.setMinimumHeight(bottom);
                mStatusPanel.requestLayout();
            }
        });
    }


    protected void checkCompass(boolean showCompass) {
        int compassContainer = R.id.fl_compass;
        FrameLayout compass = (FrameLayout) mMapRelativeLayout.findViewById(compassContainer);

        if (!showCompass) {
            compass.setVisibility(View.GONE);
            return;
        }

        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //get or create fragment
        CompassFragment compassFragment = (CompassFragment) fragmentManager.findFragmentByTag("NEEDLE_COMPASS");
        if (null == compassFragment)
            compassFragment = new CompassFragment();

        compass.setClickable(false);
        compassFragment.setStyle(true);
        if (!compassFragment.isAdded())
            fragmentTransaction.add(compassContainer, compassFragment, "NEEDLE_COMPASS")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        if (!compassFragment.isVisible()) {
            fragmentTransaction.show(compassFragment);
        }

        fragmentTransaction.commit();

        compass.setVisibility(View.VISIBLE);
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
    }


    protected void addNewGeometry()
    {
        //show select layer dialog if several layers, else start default or custom form
        List<ILayer> layers = mMap.getVectorLayersByType(
                GeoConstants.GTPointCheck | GeoConstants.GTMultiPointCheck |
                GeoConstants.GTLineStringCheck | GeoConstants.GTMultiLineStringCheck |
                GeoConstants.GTPolygonCheck | GeoConstants.GTMultiPolygonCheck);
        layers = removeHideLayers(layers);
        if (layers.isEmpty()) {
            Toast.makeText(mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG).show();
        } else if (layers.size() == 1) {
            //open form
            VectorLayer layer = (VectorLayer) layers.get(0);

            mSelectedLayer = layer;
            mEditLayerOverlay.setSelectedLayer(layer);
            setMode(MODE_SELECT_ACTION);

            Toast.makeText(mActivity, String.format(getString(R.string.edit_layer), layer.getName()), Toast.LENGTH_SHORT).show();
        } else {
            if (isDialogShown())
                return;
            //open choose edit layer dialog
            mChooseLayerDialog = new ChooseLayerDialog();
            mChooseLayerDialog.setLayerList(layers)
                    .setCode(EDIT_LAYER)
                    .setTitle(getString(R.string.choose_layers))
                    .setTheme(mActivity.getThemeId())
                    .show(mActivity.getSupportFragmentManager(), "choose_layer");
        }
    }


    protected void addPointByTap()
    {
        if (mSelectedLayer != null)
            mSelectedLayer.setLocked(false);

        //show select layer dialog if several layers, else start default or custom form
        List<ILayer> layers = mMap.getVectorLayersByType(GeoConstants.GTPointCheck | GeoConstants.GTMultiPointCheck);
        layers = removeHideLayers(layers);
        if (layers.isEmpty()) {
            Toast.makeText(
                    mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG)
                    .show();
        } else if (layers.size() == 1) {
            //open form
            VectorLayer layer = (VectorLayer) layers.get(0);

            mSelectedLayer = layer;
            mEditLayerOverlay.setSelectedLayer(layer);
            mEditLayerOverlay.setSelectedFeature(new Feature());
            mEditLayerOverlay.getSelectedFeature().setGeometry(new GeoPoint());
            setMode(MODE_EDIT);
            mEditLayerOverlay.clearHistory();
            mEditLayerOverlay.createPointFromOverlay();
            mEditLayerOverlay.setHasEdits(true);

            Toast.makeText(
                    mActivity,
                    String.format(getString(R.string.edit_layer), layer.getName()),
                    Toast.LENGTH_SHORT).show();
        } else {
            if (isDialogShown())
                return;
            //open choose edit layer dialog
            mChooseLayerDialog = new ChooseLayerDialog();
            mChooseLayerDialog.setLayerList(layers)
                    .setCode(ADD_POINT_BY_TAP)
                    .setTitle(getString(R.string.choose_layers))
                    .setTheme(mActivity.getThemeId())
                    .show(mActivity.getSupportFragmentManager(), "choose_layer");
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
                    mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG)
                    .show();
        } else if (layers.size() == 1) {
            //open form
            ILayer vectorLayer = layers.get(0);
            if (vectorLayer instanceof ILayerUI) {
                mSelectedLayer = (VectorLayer) vectorLayer;
                mEditLayerOverlay.setSelectedLayer(mSelectedLayer);
                IVectorLayerUI vectorLayerUI = (IVectorLayerUI) vectorLayer;
                vectorLayerUI.showEditForm(mActivity, Constants.NOT_FOUND, null);

                Toast.makeText(
                        mActivity,
                        String.format(getString(R.string.edit_layer), vectorLayer.getName()),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(
                        mActivity, getString(R.string.warning_no_edit_layers),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            if (isDialogShown())
                return;
            //open choose dialog
            mChooseLayerDialog = new ChooseLayerDialog();
            mChooseLayerDialog.setLayerList(layers)
                    .setCode(ADD_CURRENT_LOC)
                    .setTitle(getString(R.string.choose_layers))
                    .setTheme(mActivity.getThemeId())
                    .show(mActivity.getSupportFragmentManager(), "choose_layer");
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
        List<ILayer> layers = mMap.getVectorLayersByType(GeoConstants.GTLineStringCheck | GeoConstants.GTPolygonCheck
                | GeoConstants.GTMultiLineStringCheck | GeoConstants.GTMultiPolygonCheck);
        layers = removeHideLayers(layers);

        if (layers.isEmpty()) {
            Toast.makeText(mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG).show();
        } else if (layers.size() == 1) {
            //open form
            VectorLayer layer = (VectorLayer) layers.get(0);
            mSelectedLayer = layer;
            mEditLayerOverlay.setSelectedLayer(layer);
            mEditLayerOverlay.newGeometryByWalk();
            setMode(MODE_EDIT_BY_WALK);

            Toast.makeText(mActivity, String.format(getString(R.string.edit_layer), layer.getName()), Toast.LENGTH_SHORT).show();
        } else {
            if (isDialogShown())
                return;
            //open choose edit layer dialog
            mChooseLayerDialog = new ChooseLayerDialog();
            mChooseLayerDialog.setLayerList(layers)
                    .setCode(ADD_GEOMETRY_BY_WALK)
                    .setTitle(getString(R.string.choose_layers))
                    .setTheme(mActivity.getThemeId())
                    .show(mActivity.getSupportFragmentManager(), "choose_layer");
        }
    }


    public void onFinishChooseLayerDialog(
            int code,
            ILayer layer)
    {
        VectorLayer vectorLayer = (VectorLayer) layer;
        if (layer == null)
            return; // TODO toast?

        if (mSelectedLayer != null)
            mSelectedLayer.setLocked(false);

        mSelectedLayer = vectorLayer;
        mEditLayerOverlay.setSelectedLayer(vectorLayer);

        if (code == ADD_CURRENT_LOC) {
            if (layer instanceof ILayerUI) {
                IVectorLayerUI layerUI = (IVectorLayerUI) layer;
                layerUI.showEditForm(mActivity, Constants.NOT_FOUND, null);
            }
        } else if (code == EDIT_LAYER) {
            setMode(MODE_SELECT_ACTION);
        } else if (code == ADD_GEOMETRY_BY_WALK) {
            mEditLayerOverlay.newGeometryByWalk();
            setMode(MODE_EDIT_BY_WALK);
        } else if (code == ADD_POINT_BY_TAP) {
            mEditLayerOverlay.setSelectedFeature(new Feature());
            mEditLayerOverlay.getSelectedFeature().setGeometry(new GeoPoint());
            setMode(MODE_EDIT);
            mEditLayerOverlay.clearHistory();
            mEditLayerOverlay.createPointFromOverlay();
            mEditLayerOverlay.setHasEdits(true);
        }
    }


    @Override
    public void onLongPress(MotionEvent event)
    {
        if (!(mMode == MODE_NORMAL || mMode == MODE_SELECT_ACTION) || mRulerOverlay.isMeasuring()) {
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
            if (mSelectedLayer != null)
                mSelectedLayer.setLocked(false);

            mSelectedLayer = vectorLayer;
            mEditLayerOverlay.setSelectedLayer(vectorLayer);
            for (int i = 0; i < items.size(); i++) {    // FIXME hack for bad RTree cache
                long featureId = items.get(i);
                GeoGeometry geometry = mSelectedLayer.getGeometryForId(featureId);
                if (geometry != null)
                    mEditLayerOverlay.setSelectedFeature(featureId);
            }
            setMode(MODE_SELECT_ACTION);
        }

        showOverlayPoint(event);
        //set select action mode
        mMap.postInvalidate();
    }


    public void showAddByTapButton() {
        mAddPointButton.setVisibility(View.VISIBLE);
    }


    public void hideAddByTapButton() {
        mAddPointButton.setVisibility(View.GONE);
    }


    public void showRulerButton() {
        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean(KEY_PREF_SHOW_MEASURING, false))
            mRuler.setVisibility(View.VISIBLE);
    }


    public void hideRulerButton() {
        mRuler.setVisibility(View.GONE);
    }


    public void showMainButton() {
        if (mMode == MODE_EDIT_BY_WALK)
            return;

        mAddNewGeometry.getIconDrawable().setAlpha(255);
        mMainButton.setVisibility(View.VISIBLE);
    }


    public void hideMainButton() {
        mMainButton.setVisibility(View.GONE);
    }


    public void hideOverlayPoint() {
        mEditLayerOverlay.hideOverlayPoint();
        mMap.postInvalidate();

        hideAddByTapButton();
        showMainButton();
    }


    public void showOverlayPoint(MotionEvent event) {
        hideMainButton();
        showAddByTapButton();
        mEditLayerOverlay.setOverlayPoint(event);
    }


    @Override
    public void onSingleTapUp(MotionEvent event)
    {
        switch (mMode) {
            case MODE_EDIT:
            case MODE_SELECT_ACTION:
                mEditLayerOverlay.selectGeometryInScreenCoordinates(event.getX(), event.getY());
                defineMenuItems();
                break;
            case MODE_INFO:
                mEditLayerOverlay.selectGeometryInScreenCoordinates(event.getX(), event.getY());

                if (null != mEditLayerOverlay) {
                    AttributesFragment attributesFragment =
                            (AttributesFragment) mActivity.getSupportFragmentManager()
                                    .findFragmentByTag("ATTRIBUTES");

                    if (attributesFragment != null) {
                        attributesFragment.setSelectedFeature(mSelectedLayer,
                                mEditLayerOverlay.getSelectedFeatureId());

                        mMap.postInvalidate();
                    }
                }

                break;
            default:
                if (!mRulerOverlay.isMeasuring())
                    hideOverlayPoint();
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


    private void fillStatusPanel(Location location){
        if (mStatusPanelMode == 0)
            return;

        boolean needViewUpdate = true;
        boolean isCurrentOrientationOneLine = mStatusPanel.getChildCount() > 0 &&
                mStatusPanel.getChildAt(0).getId() == R.id.status_container_land;

        View panel;
        if (!isCurrentOrientationOneLine) {
            panel = mActivity.getLayoutInflater().inflate(R.layout.status_panel_land, mStatusPanel, false);
            defineTextViews(panel);
        } else {
            panel = mStatusPanel.getChildAt(0);
            needViewUpdate = false;
        }

        fillTextViews(location);

        if (!isFitOneLine()) {
            panel = mActivity.getLayoutInflater().inflate(R.layout.status_panel, mStatusPanel, false);
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
                int satellites = location.getExtras() != null ? location.getExtras().getInt("satellites") : 0;
                if (satellites > 0)
                    text += satellites;

                mStatusSource.setText(text);
                mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_location), null, null, null);
            } else {
                mStatusSource.setText("");
                mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_signal_wifi), null, null, null);
            }

            mStatusAccuracy.setText(
                    String.format(Locale.getDefault(),
                            "%.1f %s", location.getAccuracy(), getString(R.string.unit_meter)));
            mStatusAltitude.setText(
                    String.format(Locale.getDefault(),
                            "%.1f %s", location.getAltitude(), getString(R.string.unit_meter)));
            mStatusSpeed.setText(
                    String.format(Locale.getDefault(),
                            "%.1f %s/%s", location.getSpeed() * 3600 / 1000,
                            getString(R.string.unit_kilometer), getString(R.string.unit_hour)));
            mStatusLatitude.setText(
                    formatCoordinate(location.getLatitude(), R.string.latitude_caption_short));
            mStatusLongitude.setText(
                    formatCoordinate(location.getLongitude(), R.string.longitude_caption_short));
        }
    }


    private String formatCoordinate(double value, int appendix) {
        return LocationUtil.formatCoordinate(value, mCoordinatesFormat, mCoordinatesFraction) + " " + getString(appendix);
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
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

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

    @Override
    public void onFinishEditByWalkSession() {

    }


    public void hideBottomBar() {
        mActivity.getBottomToolbar().setVisibility(View.GONE);
    }


    public void restoreBottomBar(int mode) {
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
            Toast.makeText(mActivity, R.string.error_no_location, Toast.LENGTH_SHORT).show();
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
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        FullCompassFragment compassFragment = new FullCompassFragment();
        compassFragment.setClickable(true);

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
            case R.id.add_point_by_tap:
                if (mRulerOverlay.isMeasuring()) {
                    mRulerOverlay.stopMeasuring();
                    showMainButton();
                    showRulerButton();
                    hideAddByTapButton();
                    mAddPointButton.setIcon(R.drawable.ic_action_add_point);
                    mActivity.setTitle(R.string.app_name);
                    mActivity.setSubtitle(null);
                } else
                    addPointByTap();
                break;
            case R.id.action_ruler:
                startMeasuring();
                Toast.makeText(getContext(), R.string.tap_to_measure, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    protected void startMeasuring() {
        mRulerOverlay.startMeasuring(this);
        hideMainButton();
        hideRulerButton();
        showAddByTapButton();
        mAddPointButton.setIcon(R.drawable.ic_action_apply_dark);
    }

    @Override
    public void onLengthChanged(double length) {
        mActivity.setTitle(LocationUtil.formatLength(getContext(), length, 3));
    }

    @Override
    public void onAreaChanged(double area) {
        mActivity.setSubtitle(LocationUtil.formatArea(getContext(), area));
    }
}
