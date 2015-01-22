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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import com.nextgis.maplib.api.ILayer;

import java.util.ArrayList;
import java.util.List;


/**
 * A dialog to choose layer and start attributes edit form
 */
public class ChooseLayerDialog extends DialogFragment
{
    protected String       mTitle;
    protected List<ILayer> mLayers;

    protected AlertDialog mDialog;

    protected final static String KEY_TITLE = "title";
    protected final static String KEY_LAYERS_IDS = "ids";

    public ChooseLayerDialog setTitle(String title)
    {
        mTitle = title;
        return this;
    }

    public ChooseLayerDialog setLayerList(List<ILayer> list)
    {
        mLayers = list;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        if(null == savedInstanceState){

        }
        else{

        }

        return super.onCreateDialog(savedInstanceState);
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(KEY_TITLE, mTitle);
        ArrayList<Integer> ids = new ArrayList<>();
        for(ILayer layer : mLayers){
            ids.add((int) layer.getId());
        }
        outState.putIntegerArrayList(KEY_LAYERS_IDS, ids);
        super.onSaveInstanceState(outState);
    }
}
