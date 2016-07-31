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

package com.nextgis.mobile.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.nextgis.maplibui.dialog.NGDialog;
import com.nextgis.mobile.R;

public class NewFieldDialog extends NGDialog {
    private OnFieldChooseListener mListener;

    public interface OnFieldChooseListener {
        void OnFieldChosen(String name, int type);
    }

    public NewFieldDialog setOnFieldChooseListener(OnFieldChooseListener listener) {
        mListener = listener;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        View view = View.inflate(mContext, R.layout.dialog_new_field, null);
        final EditText name = (EditText) view.findViewById(R.id.et_field_name);
        final Spinner type = (Spinner) view.findViewById(R.id.sp_field_type);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mTitle).setView(view).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mListener != null) {
                        int fieldType = getResources().getIntArray(R.array.field_types)[type.getSelectedItemPosition()];
                        String alias = name.getText().toString().trim();
                        mListener.OnFieldChosen(alias, fieldType);
                    }
                }
            });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
