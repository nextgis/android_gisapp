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

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.keenfin.easypicker.PhotoPicker;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.control.PhotoGallery;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.overlay.EditLayerOverlay;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.MainActivity;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
import static com.nextgis.maplib.util.GeoConstants.FTDate;
import static com.nextgis.maplib.util.GeoConstants.FTDateTime;
import static com.nextgis.maplib.util.GeoConstants.FTTime;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;


public class AttributesFragment
        extends Fragment
{
    protected static final String KEY_ITEM_ID       = "item_id";
    protected static final String KEY_ITEM_POSITION = "item_pos";

    private LinearLayout          mAttributes;
    private VectorLayer           mLayer;
    private List<Long> mFeatureIDs;

    private long                  mItemId;
    private int                   mItemPosition;
    private boolean mIsTablet;

    protected EditLayerOverlay mEditLayerOverlay;
    protected Menu mBottomMenu;


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        if (mLayer == null)
            getActivity().getSupportFragmentManager().popBackStack();

        getActivity().setTitle(mLayer.getName());
        setHasOptionsMenu(!isTablet());

        int resId = isTablet() ? R.layout.fragment_attributes_tab : R.layout.fragment_attributes;
        View view = inflater.inflate(resId, container, false);

        if (isTablet()) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(view.getLayoutParams());
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            lp.width = metrics.widthPixels / 2;

            int[] attrs = {R.attr.actionBarSize};
            TypedArray ta = getActivity().obtainStyledAttributes(attrs);
            lp.bottomMargin = ta.getDimensionPixelSize(0, 0);
            ta.recycle();

            view.setLayoutParams(lp);
        }

        mAttributes = (LinearLayout) view.findViewById(R.id.ll_attributes);
        setAttributes();

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).setActionBarState(isTablet());
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.menu_about || item.getItemId() == R.id.menu_settings) {
                continue;
            }
            item.setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }


    @Override
    public void onDestroyView()
    {
        ((MainActivity) getActivity()).restoreBottomBar(MapFragment.MODE_SELECT_ACTION);
        ((MainActivity) getActivity()).setSubtitle(null);
        getActivity().setTitle(R.string.app_name);
        super.onDestroyView();
    }


    public void setSelectedFeature(
            VectorLayer selectedLayer,
            long selectedItemId)
    {
        mItemId = selectedItemId;
        mLayer = selectedLayer;

        if (mLayer == null)
            return;

        mFeatureIDs = mLayer.query(null); // get all feature IDs

        for (int i = 0; i < mFeatureIDs.size(); i++) {
            if (mFeatureIDs.get(i) == mItemId) {
                mItemPosition = i;
                break;
            }
        }

        setAttributes();
    }


    private void setAttributes()
    {
        if (mAttributes == null) {
            return;
        }

        mAttributes.removeAllViews();

        FragmentActivity activity = getActivity();
        if (null == activity)
            return;

        ((MainActivity) activity).setSubtitle(String.format(getString(R.string.features_count_attributes), mItemPosition + 1, mFeatureIDs.size()));

        String selection = Constants.FIELD_ID + " = ?";
        Cursor attributes = mLayer.query(null, selection, new String[]{mItemId + ""}, null, null);

        if (attributes.moveToFirst()) {
            for (int i = 0; i < attributes.getColumnCount(); i++) {
                String column = attributes.getColumnName(i);
                String text;

                if (column.startsWith(Constants.FIELD_GEOM_))
                    continue;

                if (column.equals(Constants.FIELD_GEOM)) {
                    switch (mLayer.getGeometryType()) {
                        case GTPoint:
                            try {
                                GeoPoint pt = (GeoPoint) GeoGeometryFactory.fromBlob(attributes.getBlob(i));
                                pt.setCRS(CRS_WEB_MERCATOR);
                                pt.project(CRS_WGS84);

                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                                int format = prefs.getInt(SettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);

                                String lat = getString(com.nextgis.maplibui.R.string.latitude_caption_short) + ": " +
                                        LocationUtil.formatLatitude(pt.getY(), format, getResources());
                                String lon = getString(com.nextgis.maplibui.R.string.longitude_caption_short) + ": " +
                                        LocationUtil.formatLongitude(pt.getX(), format, getResources());

                                text = lat + "\r\n" + lon;
                                addRow(getString(R.string.coordinates), text);
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        default:
                            continue;
                    }
                }

                Field field = mLayer.getFieldByName(column);
                int fieldType = field != null ? field.getType() : Constants.NOT_FOUND;
                switch (fieldType) {
                    case GeoConstants.FTInteger:
                        text = attributes.getInt(i) + "";
                        break;
                    case GeoConstants.FTReal:
                        NumberFormat nf = NumberFormat.getInstance();
                        nf.setMaximumFractionDigits(4);
                        nf.setGroupingUsed(false);
                        text = nf.format(attributes.getDouble(i));
                        break;
                    case GeoConstants.FTDate:
                    case GeoConstants.FTTime:
                    case GeoConstants.FTDateTime:
                        text = formatDateTime(attributes.getLong(i), fieldType);
                        break;
                    default:
                        text = attributes.getString(i);
                        break;
                }

            addRow(column, text);
            }

            IGISApplication app = (GISApplication) getActivity().getApplication();
            final Map<String, Integer> mAttaches = new HashMap<>();
        PhotoGallery.getAttaches(app, mLayer, mItemId, mAttaches);

            if (mAttaches.size() > 0) {
                final PhotoPicker gallery = new PhotoPicker(getActivity(), true);
                gallery.post(new Runnable() {
                    @Override
                    public void run() {
                        gallery.restoreImages(new ArrayList<>(mAttaches.keySet()));
                    }
                });

                mAttributes.addView(gallery);
            }
        }

        attributes.close();
        checkNearbyItems();
    }


    protected void addRow(String column, String text) {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

        TextView columnName = new TextView(getActivity());
        columnName.setLayoutParams(params);
        columnName.setText(column);
        TextView data = new TextView(getActivity());
        data.setLayoutParams(params);

        try {
            data.setText(text);
        } catch (Exception ignored) {

        }

        row.addView(columnName);
        row.addView(data);
        mAttributes.addView(row);
    }


    protected String formatDateTime(long millis, int type) {
        String result = millis + "";
        SimpleDateFormat sdf = null;

        switch (type) {
            case FTDate:
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                break;
            case FTTime:
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                break;
            case FTDateTime:
                sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                break;
        }

        if (sdf != null)
            try {
                result = sdf.format(new Date(millis));
            } catch (Exception e) {
                e.printStackTrace();
            }

        return result;
    }


    private void checkNearbyItems() {
        boolean hasNext = mItemPosition + 1 <= mFeatureIDs.size() - 1;
        boolean hasPrevious = mItemPosition - 1 >= 0;

        if (mBottomMenu != null) {
            mBottomMenu.getItem(0).setVisible(hasPrevious);
            mBottomMenu.getItem(1).setVisible(hasNext);
        }
    }


    public void selectItem(boolean isNext)
    {
        boolean hasItem = false;

        if (isNext) {
            if (mItemPosition < mFeatureIDs.size() - 1) {
                mItemPosition++;
                hasItem = true;
            }
        } else {
            if (mItemPosition > 0) {
                mItemPosition--;
                hasItem = true;
            }
        }

        if (hasItem) {
            mItemId = mFeatureIDs.get(mItemPosition);
            setAttributes();
            if (null != mEditLayerOverlay) {
                mEditLayerOverlay.setFeature(mLayer, mItemId);
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_ITEM_ID, mItemId);
        outState.putInt(KEY_ITEM_POSITION, mItemPosition);
    }


    @Override
    public void onViewStateRestored(
            @Nullable
            Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            mItemId = savedInstanceState.getLong(KEY_ITEM_ID);
            mItemPosition = savedInstanceState.getInt(KEY_ITEM_POSITION);
        }

        setAttributes();
    }


    public void setTablet(boolean tablet)
    {
        mIsTablet = tablet;
    }


    public boolean isTablet()
    {
        return mIsTablet;
    }


    public void setToolbar(
            final BottomToolbar toolbar,
            EditLayerOverlay overlay)
    {
        if (null == toolbar || null == mLayer) {
            return;
        }

        mEditLayerOverlay = overlay;

        if (mEditLayerOverlay != null) {
            mEditLayerOverlay.setMode(EditLayerOverlay.MODE_HIGHLIGHT);
        }

        toolbar.setNavigationIcon(R.drawable.ic_action_cancel_dark);
        toolbar.setNavigationOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((MainActivity) getActivity()).finishFragment();

                        if (isTablet())
                            getActivity().getSupportFragmentManager().beginTransaction().remove(AttributesFragment.this).commit();
                    }
                });

        mBottomMenu = toolbar.getMenu();
        if (mBottomMenu != null) {
            mBottomMenu.clear();
        }

        toolbar.inflateMenu(R.menu.attributes);
        toolbar.setOnMenuItemClickListener(
                new BottomToolbar.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        if (null == mLayer) {
                            return false;
                        }
                        if (menuItem.getItemId() == R.id.menu_next) {
                            selectItem(true);
                            return true;
                        } else if (menuItem.getItemId() == R.id.menu_prev) {
                            selectItem(false);
                            return true;
                        }

                        return true;
                    }
                });
        checkNearbyItems();
    }
}