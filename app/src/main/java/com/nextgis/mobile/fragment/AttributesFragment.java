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

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.keenfin.easypicker.PhotoPicker;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.control.PhotoGallery;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.overlay.EditLayerOverlay;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    private boolean mIsFinished;
    private boolean mIsReorient = false;

    protected EditLayerOverlay mEditLayerOverlay;


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    protected void finishFragment()
    {
        MainActivity activity = (MainActivity) getActivity();
        if (null != activity) {
            activity.getSupportFragmentManager().popBackStack();
            mIsFinished = true;
        }
    }


    @Override
    public void onDestroyView()
    {
        if (!mIsReorient) {
            ((MainActivity) getActivity()).setActionBarState(true);
            ((MainActivity) getActivity()).restoreBottomBar();
        }

        super.onDestroyView();
    }


    public void setSelectedFeature(
            VectorLayer selectedLayer,
            long selectedItemId)
    {
        mItemId = selectedItemId;
        mLayer = selectedLayer;

        if (mLayer == null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }

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
        if (null == activity) {
            return;
        }
        TextView title = new TextView(activity);
        title.setText(mLayer.getName());
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24, r.getDisplayMetrics());

        title.setPadding(0, 0, 0, px);
        mAttributes.addView(title);

        String selection = Constants.FIELD_ID + " = ?";
        Cursor attributes = mLayer.query(null, selection, new String[]{mItemId + ""}, null, null);

        if (attributes.moveToFirst()) {
            for (int i = 0; i < attributes.getColumnCount(); i++) {
                String column = attributes.getColumnName(i);
                if (column.startsWith(Constants.FIELD_GEOM))
                    continue;

                LinearLayout row = new LinearLayout(getActivity());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

                TextView columnName = new TextView(getActivity());
                columnName.setLayoutParams(params);



                if (column.equals(Constants.FIELD_GEOM)) {
                    continue;
                }

                columnName.setText(column);
                TextView data = new TextView(getActivity());
                data.setLayoutParams(params);

                try {
                    data.setText(attributes.getString(i));
                } catch (Exception ignored) {

                }

                row.addView(columnName);
                row.addView(data);
                mAttributes.addView(row);
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
        } else {
            Toast.makeText(getActivity(), R.string.attributes_last_item, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_ITEM_ID, mItemId);
        outState.putInt(KEY_ITEM_POSITION, mItemPosition);
        mIsReorient = true;
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


    public boolean isFinished() {
        return mIsFinished;
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
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        finishFragment();
                    }
                });

        if (toolbar.getMenu() != null) {
            toolbar.getMenu().clear();
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
    }
}