/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.mobile.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LayerUtil;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.mobile.MainApplication;
import com.nextgis.mobile.R;
import com.nextgis.mobile.dialog.NewFieldDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CreateVectorLayerActivity extends NGActivity implements View.OnClickListener, NewFieldDialog.OnFieldChooseListener {
    private EditText mEtLayerName;
    private Spinner mSpLayerType;
    private FieldAdapter mFieldAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_layer);
        setToolbar(R.id.main_toolbar);

        findViewById(R.id.ib_add_field).setOnClickListener(this);

        mEtLayerName = (EditText) findViewById(R.id.et_layer_name);
        mSpLayerType = (Spinner) findViewById(R.id.sp_layer_type);
        ListView lvFields = (ListView) findViewById(R.id.lv_fields);

        mFieldAdapter = new FieldAdapter();
        lvFields.setAdapter(mFieldAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.edit_attributes, menu);
        menu.findItem(R.id.menu_settings).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_apply:
                int info = R.string.error_layer_create;

                if (TextUtils.isEmpty(mEtLayerName.getText().toString().trim()))
                    info = R.string.empty_name;
                else if (hasLayerWithSameName())
                    info = R.string.same_layer_name;
                else if (createNewLayer()) {
                    info = R.string.message_layer_created;
                    finish();
                }

                Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_add_field:
                addNewField();
                break;
        }
    }

    private boolean hasLayerWithSameName() {
        MainApplication app = (MainApplication) getApplication();
        MapBase map = app.getMap();

        for (int i = 0; i < map.getLayerCount(); i++)
            if (map.getLayer(i).getName().trim().equalsIgnoreCase(mEtLayerName.getText().toString().trim()))
                return true;

        return false;
    }

    private void addNewField() {
        NewFieldDialog nfDialog = new NewFieldDialog();
        nfDialog.setOnFieldChooseListener(this)
                .setTitle(getString(R.string.new_field)).setTheme(getThemeId())
                .show(getSupportFragmentManager(), "new_field");
    }

    @Override
    public void OnFieldChosen(String alias, int type) {
        String name = System.currentTimeMillis() + "";
        name = "field_" + name.substring(8);
        if (TextUtils.isEmpty(alias))
            Toast.makeText(this, R.string.empty_name, Toast.LENGTH_SHORT).show();
        else if (mFieldAdapter.containsField(alias))
            Toast.makeText(this, R.string.same_field_name, Toast.LENGTH_SHORT).show();
        else
            mFieldAdapter.addField(new Field(type, name, alias));
    }

    private boolean createNewLayer() {
        MainApplication app = (MainApplication) getApplication();
        int geomType = getResources().getIntArray(R.array.geom_types)[mSpLayerType.getSelectedItemPosition()];
        List<Field> fields = mFieldAdapter.getFields();
        if (fields.size() == 0)
            fields.add(new Field(GeoConstants.FTString, "description", getString(R.string.default_field_name)));

        VectorLayer layer = app.createEmptyVectorLayer(mEtLayerName.getText().toString().trim(), null, geomType, fields);

        SimpleFeatureRenderer sfr = (SimpleFeatureRenderer) layer.getRenderer();
        if (null != sfr) {
            Style style = sfr.getStyle();
            if (null != style) {
                Random rnd = new Random(System.currentTimeMillis());
                style.setColor(Color.rgb(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255)));
            }
        }

        MapBase map = app.getMap();
        map.addLayer(layer);
        return map.save();
    }

    protected class FieldAdapter extends BaseAdapter {
        private List<Field> mFields;

        public FieldAdapter() {
            mFields = new ArrayList<>();
        }

        public void addField(Field field) {
            mFields.add(field);
            notifyDataSetChanged();
        }

        public boolean containsField(String fieldName) {
            for (Field field : mFields)
                if (field.getAlias().equalsIgnoreCase(fieldName))
                    return true;

            return false;
        }

        public List<Field> getFields() {
            return mFields;
        }

        @Override
        public int getCount() {
            return mFields.size();
        }

        @Override
        public Object getItem(int position) {
            return mFields.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(CreateVectorLayerActivity.this);
                view = inflater.inflate(R.layout.item_field, parent, false);
            }

            final Field field = mFields.get(position);
            TextView fieldName = (TextView) view.findViewById(R.id.tv_field_name);
            TextView fieldType = (TextView) view.findViewById(R.id.tv_field_type);
            fieldName.setText(field.getAlias());
            fieldType.setText(LayerUtil.typeToString(CreateVectorLayerActivity.this, field.getType()));

            view.findViewById(R.id.ib_remove_field).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFields.remove(field);
                    notifyDataSetChanged();
                }
            });

            return view;
        }
    }
}
