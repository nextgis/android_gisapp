/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * Author:   Dmitry Baryshnikov aka Bishop, bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
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

import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.VectorCacheItem;
import com.nextgis.maplibui.BottomToolbar;
import com.nextgis.maplibui.overlay.EditLayerOverlay;

import java.util.List;


public class AttributesFragment
        extends Fragment
{
    protected static final String KEY_ITEM_ID       = "item_id";
    protected static final String KEY_ITEM_POSITION = "item_pos";

    private LinearLayout          mAttributes;
    private long                  mItemId;
    private int                   mItemPosition;
    private VectorLayer           mLayer;
    private List<VectorCacheItem> mVectorCacheItems;
    private boolean mIsTablet, mIsReorient = false;

    protected EditLayerOverlay mEditLayerOverlay;


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        ((MainActivity) getActivity()).setActionBarState(isTablet());
        setHasOptionsMenu(!isTablet());

        int resId = isTablet() ? R.layout.fragment_attributes_tab : R.layout.fragment_attributes;
        View view = inflater.inflate(resId, container, false);
        mAttributes = (LinearLayout) view.findViewById(R.id.ll_attributes);
        setAttributes();

        return view;
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
            activity.setActionBarState(true);
            activity.restoreBottomBar();
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

        mVectorCacheItems = mLayer.getVectorCache();

        for (int i = 0; i < mVectorCacheItems.size(); i++) {
            if (mVectorCacheItems.get(i).getId() == mItemId) {
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
        Cursor attributes = mLayer.query(null, selection, new String[] {mItemId + ""}, null);

        if (attributes.moveToFirst()) {
            for (int i = 0; i < attributes.getColumnCount(); i++) {
                LinearLayout row = new LinearLayout(getActivity());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

                TextView columnName = new TextView(getActivity());
                columnName.setLayoutParams(params);

                String column = attributes.getColumnName(i);

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
        }
    }


    public void selectItem(boolean isNext)
    {
        boolean hasItem = false;

        if (isNext) {
            if (mItemPosition < mVectorCacheItems.size() - 1) {
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
            VectorCacheItem item = mVectorCacheItems.get(mItemPosition);
            mItemId = item.getId();
            setAttributes();
            if (null != mEditLayerOverlay) {
                mEditLayerOverlay.setFeature(mLayer, item);
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

        toolbar.setNavigationIcon(R.drawable.ic_action_cancel);
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