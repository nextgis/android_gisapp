/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.maplib.map.VectorLayer;


public class AttributesFragment extends Fragment
        implements View.OnClickListener
{
    private LinearLayout mAttributes;
    private long mItemId;
    private VectorLayer mLayer;


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        ((MainActivity) getActivity()).setActionBarState(false);
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_attributes, container, false);
        mAttributes = (LinearLayout) view.findViewById(R.id.ll_attributes);
        setAttributes();

        view.findViewById(R.id.btn_attr_next).setOnClickListener(this);
        view.findViewById(R.id.btn_attr_previous).setOnClickListener(this);

        return view;
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        menu.findItem(R.id.menu_add).setVisible(false);
        menu.findItem(R.id.menu_locate).setVisible(false);
        menu.findItem(R.id.menu_track).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().getSupportFragmentManager().popBackStack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onDestroy()
    {
        ((MainActivity) getActivity()).setActionBarState(true);
        super.onDestroy();
    }


    public void setSelectedFeature(
            VectorLayer selectedLayer,
            long selectedItemId) {
        mItemId = selectedItemId;
        mLayer = selectedLayer;
        setAttributes();
    }

    private void setAttributes() {
        if (mAttributes == null)
            return;

        mAttributes.removeAllViews();

        TextView title = new TextView(getActivity());
        title.setText(mLayer.getName());
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        mAttributes.addView(title);

        String selection = VectorLayer.FIELD_ID + " = ?";
        Cursor attributes = mLayer.query(null, selection, new String[] {mItemId + ""}, null);

        if (attributes.moveToFirst()) {
            for (int i = 0; i < attributes.getColumnCount(); i++) {
                LinearLayout row = new LinearLayout(getActivity());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

                TextView columnName = new TextView(getActivity());
                columnName.setLayoutParams(params);
                columnName.setText(attributes.getColumnName(i));
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


    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.btn_attr_next:
                mItemId++;
                break;
            case R.id.btn_attr_previous:
                mItemId--;
                break;
        }

        setAttributes();
    }
}
