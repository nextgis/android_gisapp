/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2019 NextGIS, info@nextgis.com
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
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.ILayerView;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoLineString;
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
import com.nextgis.maplibui.overlay.UndoRedoOverlay;
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

import static android.content.Context.MODE_MULTI_PROCESS;
import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.FIELD_ID;
import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.MapUtil.isGeometryIntersects;
import static com.nextgis.maplibui.util.ConstantsUI.GA_EDIT;
import static com.nextgis.maplibui.util.ConstantsUI.GA_FAB;
import static com.nextgis.maplibui.util.ConstantsUI.GA_LAYER;
import static com.nextgis.mobile.util.AppConstants.DEFAULT_COORDINATES_FRACTION_DIGITS;
import static com.nextgis.mobile.util.AppSettingsConstants.KEY_PREF_SHOW_COMPASS;
import static com.nextgis.mobile.util.AppSettingsConstants.KEY_PREF_SHOW_MEASURING;
import static com.nextgis.mobile.util.AppSettingsConstants.KEY_PREF_SHOW_SCALE_RULER;
import static com.nextgis.mobile.util.AppSettingsConstants.KEY_PREF_SHOW_ZOOM;
import static com.nextgis.mobile.util.AppSettingsConstants.KEY_PREF_SHOW_ZOOM_CONTROLS;

/**
 * Main map fragment
 */
public class MapFragment
        extends Fragment
        implements MapViewEventListener, GpsEventListener, EditEventListener, OnClickListener, RulerOverlay.OnRulerChanged {
    protected float mTolerancePX;

    protected SharedPreferences    mPreferences;
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
    protected TextView mScaleRulerText, mZoomLevel;
    protected ImageView mScaleRuler;

    protected RelativeLayout         mMapRelativeLayout;
    protected GpsEventSource         mGpsEventSource;
    protected View                   mMainButton;
    protected int                    mMode;
    protected CurrentLocationOverlay mCurrentLocationOverlay;
    protected CurrentTrackOverlay    mCurrentTrackOverlay;
    protected EditLayerOverlay       mEditLayerOverlay;
    protected UndoRedoOverlay        mUndoRedoOverlay;
    protected RulerOverlay           mRulerOverlay;
    protected GeoPoint               mCurrentCenter;
    protected VectorLayer            mSelectedLayer;

    protected int mCoordinatesFormat, mCoordinatesFraction;
    protected ChooseLayerDialog mChooseLayerDialog;
    protected AlertDialog mGPSDialog;
    protected Vibrator mVibrator;

    public static final int MODE_NORMAL        = 0;
    public static final int MODE_SELECT_ACTION = 1;
    public static final int MODE_EDIT          = 2;
    public static final int MODE_INFO          = 3;
    public static final int MODE_EDIT_BY_WALK  = 4;
    public static final int MODE_EDIT_BY_TOUCH = 5;

    protected static final String KEY_MODE = "mode";
    protected static final String BUNDLE_KEY_LAYER = "layer";
    protected static final String BUNDLE_KEY_FEATURE_ID = "feature";
    protected static final String BUNDLE_KEY_SAVED_FEATURE = "feature_blob";
    protected static final String BUNDLE_KEY_IS_MEASURING = "is_measuring";
    protected boolean mIsCompassDragging;
    protected int mStatusPanelMode;
    protected onModeChange mModeListener;
    protected OnClickListener mFinishListener;

    protected final int ADD_CURRENT_LOC         = 1;
    public static final int EDIT_LAYER          = 2;
    protected final int ADD_GEOMETRY_BY_WALK    = 3;
    protected final int ADD_POINT_BY_TAP        = 4;
    private boolean mNeedSave = false;

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
        setHasOptionsMenu(true);
        mActivity = (MainActivity) getActivity();
        mTolerancePX = mActivity.getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mApp = (MainApplication) mActivity.getApplication();
        mVibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        mGpsEventSource = mApp.getGpsEventSource();

        mMap = new MapViewOverlays(mActivity, (MapDrawable) mApp.getMap());
        mMap.setId(R.id.map_view);

        mEditLayerOverlay = new EditLayerOverlay(mActivity, mMap);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mUndoRedoOverlay != null)
            mUndoRedoOverlay.defineUndoRedo();

        if (mEditLayerOverlay != null)
            mEditLayerOverlay.setHasEdits(mEditLayerOverlay.hasEdits());
    }

    public void restartGpsListener() {
        mGpsEventSource.removeListener(this);
        mGpsEventSource.addListener(this);
    }

    public EditLayerOverlay getEditLayerOverlay() {
        return mEditLayerOverlay;
    }

    public UndoRedoOverlay getUndoRedoOverlay() {
        return mUndoRedoOverlay;
    }

    public boolean isEditMode() {
        return mMode == MODE_EDIT || mMode == MODE_EDIT_BY_WALK || mMode == MODE_EDIT_BY_TOUCH;
    }

    public int getMode() {
        return mMode;
    }

    public boolean onOptionsItemSelected(int id) {
        boolean result;
        switch (id) {
            case android.R.id.home:
                cancelEdits();
                return true;
            case 0:
                mMap.setLockMap(false);
                setMode(MODE_EDIT);
                return true;
            case R.id.menu_edit_by_touch:
                setMode(MODE_EDIT_BY_TOUCH);
                result = mEditLayerOverlay.onOptionsItemSelected(id);
                return result;
            case R.id.menu_edit_undo:
            case R.id.menu_edit_redo:
                result = mUndoRedoOverlay.onOptionsItemSelected(id);
                if (result) {
                    Feature undoRedoFeature = mUndoRedoOverlay.getFeature();
                    Feature feature = mEditLayerOverlay.getSelectedFeature();
                    feature.setGeometry(undoRedoFeature.getGeometry());
                    mEditLayerOverlay.fillDrawItems(undoRedoFeature.getGeometry());

                    GeoGeometry original = mSelectedLayer.getGeometryForId(feature.getId());
                    boolean hasEdits = original != null && undoRedoFeature.getGeometry().equals(original);

                    mEditLayerOverlay.setHasEdits(!hasEdits);

                    mMap.buffer();
                    mMap.postInvalidate();
                }

                return result;
            case R.id.menu_edit_by_walk:
                setMode(MODE_EDIT_BY_WALK);
            default:
                result = mEditLayerOverlay.onOptionsItemSelected(id);
                if (result)
                    mUndoRedoOverlay.saveToHistory(mEditLayerOverlay.getSelectedFeature());
                return result;
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

        if (geometry == null || !geometry.isValid()) {
            Toast.makeText(getContext(), R.string.not_enough_points, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (isGeometryIntersects(getContext(), geometry))
            return false;

        mMap.setLockMap(false);
        mEditLayerOverlay.setHasEdits(false);
        if (mMode == MODE_EDIT_BY_WALK) {
            mEditLayerOverlay.stopGeometryByWalk();
            setMode(MODE_EDIT);
            mUndoRedoOverlay.clearHistory();
            mUndoRedoOverlay.defineUndoRedo();
        }

        if (mMode == MODE_EDIT_BY_TOUCH) {
            setMode(MODE_EDIT);
            mUndoRedoOverlay.clearHistory();
            mUndoRedoOverlay.defineUndoRedo();
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

        if (mMode == MODE_INFO || resultCode != Activity.RESULT_OK) {
            mEditLayerOverlay.setHasEdits(true);
            return;
        }

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
            mUndoRedoOverlay.clearHistory();
            mUndoRedoOverlay.defineUndoRedo();
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
                mUndoRedoOverlay.clearHistory();
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
                mUndoRedoOverlay.clearHistory();
                break;
            case MODE_EDIT_BY_TOUCH:
                mSelectedLayer.setLocked(true);
                mActivity.showEditToolbar();
                mEditLayerOverlay.setMode(EditLayerOverlay.MODE_EDIT_BY_TOUCH);
                toolbar.setOnMenuItemClickListener(
                        new BottomToolbar.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                return onOptionsItemSelected(menuItem.getItemId());
                            }
                        });
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

                mFinishListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setMode(MODE_NORMAL);
                    }
                };
                toolbar.setNavigationOnClickListener(mFinishListener);

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
                                        mUndoRedoOverlay.clearHistory();
                                        setMode(MODE_EDIT);
                                        mUndoRedoOverlay.saveToHistory(mEditLayerOverlay.getSelectedFeature());
                                        mEditLayerOverlay.setHasEdits(true);
                                        break;
                                    case R.id.menu_feature_edit:
                                        setMode(MODE_EDIT);
                                        mUndoRedoOverlay.saveToHistory(mEditLayerOverlay.getSelectedFeature());
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
                mUndoRedoOverlay.clearHistory();
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
                final AttributesFragment attributesFragment = getAttributesFragment(fragmentManager);
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

                attributesFragment.setSelectedFeature(mSelectedLayer, mEditLayerOverlay.getSelectedFeatureId());
                attributesFragment.setToolbar(toolbar, mEditLayerOverlay);

                mFinishListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((MainActivity) getActivity()).finishFragment();

                        if (attributesFragment.isTablet())
                            getActivity().getSupportFragmentManager().beginTransaction().remove(attributesFragment).commit();

                        if (view == null)
                            setMode(MODE_NORMAL);
                    }
                };

                toolbar.setNavigationIcon(R.drawable.ic_action_cancel_dark);
                toolbar.setNavigationOnClickListener(mFinishListener);

                break;
        }

        if (mModeListener != null)
            mModeListener.onModeChangeListener();

        setMarginsToPanel();
        defineMenuItems();
    }

    private static AttributesFragment getAttributesFragment(FragmentManager fragmentManager) {
		AttributesFragment attributesFragment = (AttributesFragment) fragmentManager.findFragmentByTag("ATTRIBUTES");
		if (null == attributesFragment)
			attributesFragment = new AttributesFragment();
		return attributesFragment;
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
                .addCallback(new Snackbar.Callback() {
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
        TextView textView = view.findViewById(R.id.snackbar_text);
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
        mUndoRedoOverlay = new UndoRedoOverlay(mActivity, mMap);

        mMap.addOverlay(mCurrentTrackOverlay);
        mMap.addOverlay(mCurrentLocationOverlay);
        mMap.addOverlay(mEditLayerOverlay);
        mMap.addOverlay(mUndoRedoOverlay);
        mMap.addOverlay(mRulerOverlay);

        //search relative view of map, if not found - add it
        mMapRelativeLayout = view.findViewById(R.id.maprl);
        if (mMapRelativeLayout != null) {
            mMapRelativeLayout.addView(
                    mMap, 0, new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT));
        }

        float mapZoom;
        try {
            mapZoom = mPreferences.getFloat(SettingsConstantsUI.KEY_PREF_ZOOM_LEVEL, mMap.getMinZoom());
        } catch (ClassCastException e) {
            mapZoom = mMap.getMinZoom();
        }

        double mapScrollX;
        double mapScrollY;
        try {
            mapScrollX = Double.longBitsToDouble(mPreferences.getLong(SettingsConstantsUI.KEY_PREF_SCROLL_X, 0));
            mapScrollY = Double.longBitsToDouble(mPreferences.getLong(SettingsConstantsUI.KEY_PREF_SCROLL_Y, 0));
        } catch (ClassCastException e) {
            mapScrollX = 0;
            mapScrollY = 0;
        }
        mMap.setZoomAndCenter(mapZoom, new GeoPoint(mapScrollX, mapScrollY));

        mMainButton = view.findViewById(R.id.multiple_actions);
        mAddPointButton = view.findViewById(R.id.add_point_by_tap);
        mAddPointButton.setOnClickListener(this);

        View addCurrentLocation = view.findViewById(R.id.add_current_location);
        addCurrentLocation.setOnClickListener(this);

        mAddNewGeometry = view.findViewById(R.id.add_new_geometry);
        mAddNewGeometry.setOnClickListener(this);
        mRuler = view.findViewById(R.id.action_ruler);
        mRuler.setOnClickListener(this);

        View addGeometryByWalk = view.findViewById(R.id.add_geometry_by_walk);
        addGeometryByWalk.setOnClickListener(this);

        mivZoomIn = view.findViewById(R.id.action_zoom_in);
        mivZoomIn.setOnClickListener(this);

        mivZoomOut = view.findViewById(R.id.action_zoom_out);
        mivZoomOut.setOnClickListener(this);

        mStatusPanel = view.findViewById(R.id.fl_status_panel);
        mScaleRuler = view.findViewById(R.id.iv_ruler);
        mScaleRulerText = view.findViewById(R.id.tv_ruler);
        mScaleRulerText.setText(getRulerText());
        mZoomLevel = view.findViewById(R.id.tv_zoom_level);
        mZoomLevel.setText(getZoomText());
        mScaleRulerLayout = view.findViewById(R.id.ll_ruler);
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
        mZoomLevel.setText(getZoomText());
    }


    protected String getZoomText() {
        return String.format("%.0fz", mMap.getZoomLevel());
    }


    protected String getRulerText() {
        GeoPoint p1 = new GeoPoint(mScaleRuler.getLeft(), mScaleRuler.getBottom());
        GeoPoint p2 = new GeoPoint(mScaleRuler.getRight(), mScaleRuler.getBottom());
        p1 = mMap.getMap().screenToMap(p1);
        p2 = mMap.getMap().screenToMap(p2);
        p1.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        p2.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        GeoLineString s = new GeoLineString();
        s.add(p1);
        s.add(p2);

        return LocationUtil.formatLength(getContext(), s.getLength(), 1);
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
                    } catch (IOException e) {
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
            SharedPreferences preferences = getContext().getSharedPreferences(WalkEditService.TEMP_PREFERENCES, MODE_MULTI_PROCESS);
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
            mEditLayerOverlay.onPause();
        }

        final SharedPreferences.Editor edit = mPreferences.edit();
        if (null != mMap) {
            edit.putFloat(SettingsConstantsUI.KEY_PREF_ZOOM_LEVEL, mMap.getZoomLevel());
            GeoPoint point = mMap.getMapCenter();
            edit.putLong(SettingsConstantsUI.KEY_PREF_SCROLL_X, Double.doubleToRawLongBits(point.getX()));
            edit.putLong(SettingsConstantsUI.KEY_PREF_SCROLL_Y, Double.doubleToRawLongBits(point.getY()));

            mMap.removeListener(this);
        }
        edit.apply();

        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();

        boolean showControls = mPreferences.getBoolean(KEY_PREF_SHOW_ZOOM_CONTROLS, false);
        showMapButtons(showControls, mMapRelativeLayout);

        if(Constants.DEBUG_MODE)
            Log.d(Constants.TAG, "KEY_PREF_SHOW_ZOOM_CONTROLS: " + (showControls ? "ON" : "OFF"));

        showControls = mPreferences.getBoolean(KEY_PREF_SHOW_SCALE_RULER, true);
        if (showControls)
            mScaleRulerLayout.setVisibility(View.VISIBLE);
        else
            mScaleRulerLayout.setVisibility(View.GONE);

        showControls = mPreferences.getBoolean(KEY_PREF_SHOW_ZOOM, false);
        if (showControls)
            mZoomLevel.setVisibility(View.VISIBLE);
        else
            mZoomLevel.setVisibility(View.GONE);

        showControls = mPreferences.getBoolean(KEY_PREF_SHOW_MEASURING, false);
        if (showControls)
            mRuler.setVisibility(View.VISIBLE);
        else
            mRuler.setVisibility(View.GONE);

        if (null != mMap) {
            mMap.getMap().setBackground(mApp.getMapBackground());
            mMap.addListener(this);
        }

        String coordinatesFormat = mPreferences.getString(SettingsConstantsUI.KEY_PREF_COORD_FORMAT, Location.FORMAT_DEGREES + "");
        if (FileUtil.isIntegerParseInt(coordinatesFormat))
            mCoordinatesFormat = Integer.parseInt(coordinatesFormat);
        else
            mCoordinatesFormat = Location.FORMAT_DEGREES;
        mCoordinatesFraction = mPreferences.getInt(SettingsConstantsUI.KEY_PREF_COORD_FRACTION, DEFAULT_COORDINATES_FRACTION_DIGITS);

        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay.updateMode(mPreferences.getString(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC, "3"));
            mCurrentLocationOverlay.startShowingCurrentLocation();
        }
        if (null != mGpsEventSource) {
            mGpsEventSource.addListener(this);
            if (mGPSDialog == null || !mGPSDialog.isShowing())
                mGPSDialog = NotificationHelper.showLocationInfo(getActivity());
        }

        if (null != mEditLayerOverlay) {
            mEditLayerOverlay.addListener(this);
            mEditLayerOverlay.onResume();
        }

        try {
            String statusPanelModeStr = mPreferences.getString(SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL, "1");
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

        boolean showCompass = mPreferences.getBoolean(KEY_PREF_SHOW_COMPASS, true);
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
        final FrameLayout compass = mMapRelativeLayout.findViewById(compassContainer);

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
                final int X = (int) event.getRawX();
                final int Y = (int) event.getRawY();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                        _xDelta = X - lParams.leftMargin;
                        _yDelta = Y - lParams.topMargin;
                        return false;
                    case MotionEvent.ACTION_UP:
                        mIsCompassDragging = false;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        if (!mIsCompassDragging)
                            return false;

                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                        int width = v.getWidth();
                        int height = v.getHeight();
                        int toolbarHeight = 0;
                        if (mActivity.getSupportActionBar() != null)
                            toolbarHeight = mActivity.getSupportActionBar().getHeight();
                        if (X > width / 3 && X < v.getRootView().getWidth() - width / 3)
                            layoutParams.leftMargin = X - _xDelta;
                        if (Y > height / 2 + toolbarHeight && Y < v.getRootView().getHeight() - height / 2)
                            layoutParams.topMargin = Y - _yDelta;

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
        mApp.sendEvent(GA_LAYER, GA_EDIT, GA_FAB);

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
            createPointFromOverlay();

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

    protected void createPointFromOverlay() {
        mEditLayerOverlay.setSelectedFeature(new Feature());
        mEditLayerOverlay.getSelectedFeature().setGeometry(new GeoPoint());
        setMode(MODE_EDIT);
        mUndoRedoOverlay.clearHistory();
        mEditLayerOverlay.createPointFromOverlay();
        mEditLayerOverlay.setHasEdits(true);
        mUndoRedoOverlay.saveToHistory(mEditLayerOverlay.getSelectedFeature());
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
            createPointFromOverlay();
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
        if (null == mapEnv)
            return;

        GeoEnvelope exactEnv = new GeoEnvelope(event.getX(), event.getX(), event.getY(), event.getY());
        exactEnv = mMap.screenToMap(exactEnv);
        if (null == exactEnv)
            return;
        GeoPoint point = new GeoPoint(exactEnv.getMaxX(), exactEnv.getMinY());
        point.setCRS(GeoConstants.CRS_WEB_MERCATOR);

        //show actions dialog
        List<ILayer> layers = mMap.getVectorLayersByType(GeoConstants.GTAnyCheck);
        List<Long> items;
        VectorLayer vectorLayer;
        layersLoop:
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
            for (int i = 0; i < items.size(); i++) {    // FIXME hack for bad RTree cache
                long featureId = items.get(i);
                GeoGeometry geometry = vectorLayer.getGeometryForId(featureId);
                if (mEditLayerOverlay.notContains(geometry, point))
                    continue;

                if (mSelectedLayer != null)
                    mSelectedLayer.setLocked(false);

                mSelectedLayer = vectorLayer;
                mEditLayerOverlay.setSelectedLayer(vectorLayer);

                if (geometry != null)
                    mEditLayerOverlay.setSelectedFeature(featureId);

                setMode(MODE_SELECT_ACTION);
                break layersLoop;
            }
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
        if (mPreferences.getBoolean(KEY_PREF_SHOW_MEASURING, false))
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
                if (mEditLayerOverlay.selectGeometryInScreenCoordinates(event.getX(), event.getY()))
                    mUndoRedoOverlay.saveToHistory(mEditLayerOverlay.getSelectedFeature());
                defineMenuItems();
                break;
            case MODE_SELECT_ACTION:
                mEditLayerOverlay.selectGeometryInScreenCoordinates(event.getX(), event.getY());
                defineMenuItems();
                break;
            case MODE_INFO:
                mEditLayerOverlay.selectGeometryInScreenCoordinates(event.getX(), event.getY());

                if (null != mEditLayerOverlay) {
                    AttributesFragment attributesFragment = (AttributesFragment) mActivity.getSupportFragmentManager().findFragmentByTag("ATTRIBUTES");
                    if (attributesFragment != null) {
                        attributesFragment.setSelectedFeature(mSelectedLayer, mEditLayerOverlay.getSelectedFeatureId());
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
        if (mEditLayerOverlay.getMode() == EditLayerOverlay.MODE_CHANGE)
            mNeedSave = true;
    }


    @Override
    public void panMoveTo(MotionEvent e)
    {

    }


    @Override
    public void panStop()
    {
        if (mMode == MODE_EDIT_BY_TOUCH || mNeedSave) {
            mNeedSave = false;
            mUndoRedoOverlay.saveToHistory(mEditLayerOverlay.getSelectedFeature());
        }
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
        mStatusSource = panel.findViewById(R.id.tv_source);
        mStatusAccuracy = panel.findViewById(R.id.tv_accuracy);
        mStatusSpeed = panel.findViewById(R.id.tv_speed);
        mStatusAltitude = panel.findViewById(R.id.tv_altitude);
        mStatusLatitude = panel.findViewById(R.id.tv_latitude);
        mStatusLongitude = panel.findViewById(R.id.tv_longitude);
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
                    mActivity.setTitle(mActivity.getAppName());
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
        mRulerOverlay.startMeasuring(this, mCurrentCenter);
        hideOverlayPoint();
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
