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

package com.nextgis.mobile.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.api.ISelectResourceDialog;
import com.nextgis.maplibui.dialog.LocalResourcesListAdapter;
import com.nextgis.mobile.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * A dialog to select map path from settings
 */
public class SelectMapPathDialogPreference
        extends DialogPreference
        implements ISelectResourceDialog
{
    protected LocalResourcesListAdapter mListAdapter;
    protected File                      mPath;
    private   String                    mText;


    public SelectMapPathDialogPreference(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
        setDialogLayoutResource(R.layout.layout_resources);
        mListAdapter = new LocalResourcesListAdapter(this);
        mPath = context.getExternalFilesDir(null);
    }


    @Override
    protected void onBindDialogView(
            @NonNull
            View view)
    {
        ListView dialogListView = (ListView) view.findViewById(com.nextgis.maplibui.R.id.listView);
        mListAdapter.setTypeMask(Constants.FILETYPE_FOLDER);
        mListAdapter.setCurrentPath(mPath);
        mListAdapter.setCheckState(new ArrayList<String>());
        mListAdapter.setCanWrite(true);
        mListAdapter.setCanSelectMulti(false);
        dialogListView.setAdapter(mListAdapter);
        dialogListView.setOnItemClickListener(mListAdapter);

        LinearLayout pathView = (LinearLayout) view.findViewById(com.nextgis.maplibui.R.id.path);
        mListAdapter.setPathLayout(pathView);
        super.onBindDialogView(view);
    }


    @Override
    protected Parcelable onSaveInstanceState()
    {
        final Parcelable superState = super.onSaveInstanceState();
        /*if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }*/
        final SavedState myState = new SavedState(superState);

        myState.mCurrentFile = mListAdapter.getCurrentPath();
        myState.mChoices = mListAdapter.getCheckState();
        return myState;
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mListAdapter.setCheckState(myState.mChoices);
        mListAdapter.setCurrentPath(myState.mCurrentFile);
    }


    @Override
    public void updateButtons()
    {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (null != dialog) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(mListAdapter.getCheckState().size() > 0);
        }
    }


    @Override
    protected void showDialog(Bundle state)
    {
        super.showDialog(state);
        updateButtons();
    }


    private static class SavedState
            extends BaseSavedState
    {
        public File         mCurrentFile;
        public List<String> mChoices;


        public SavedState(Parcel source)
        {
            super(source);

            mCurrentFile = (File) source.readSerializable();
            mChoices = new ArrayList<>();
            source.readStringList(mChoices);
        }


        @Override
        public void writeToParcel(
                @NonNull
                Parcel dest,
                int flags)
        {
            super.writeToParcel(dest, flags);

            dest.writeSerializable(mCurrentFile);
            dest.writeStringList(mChoices);
        }


        public SavedState(Parcelable superState)
        {
            super(superState);
        }


        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>()
                {
                    public SavedState createFromParcel(Parcel in)
                    {
                        return new SavedState(in);
                    }


                    public SavedState[] newArray(int size)
                    {
                        return new SavedState[size];
                    }
                };
    }


    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);
        if (positiveResult && mListAdapter.getCheckState().size() > 0) {
            String value = mListAdapter.getCheckState().get(0);
            if (callChangeListener(value)) {
                setText(value);
                setSummary(value);
            }
        }
    }


    /**
     * Saves the text to the {@link SharedPreferences}.
     *
     * @param text
     *         The text to save
     */
    public void setText(String text)
    {
        final boolean wasBlocking = shouldDisableDependents();
        mText = text;
        persistString(text);
        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }


    /**
     * Gets the text from the {@link SharedPreferences}.
     *
     * @return The current preference value.
     */
    public String getText()
    {
        return mText;
    }


    @Override
    public boolean shouldDisableDependents()
    {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }


    @Override
    protected Object onGetDefaultValue(
            TypedArray a,
            int index)
    {
        return a.getString(index);
    }


    @Override
    protected void onSetInitialValue(
            boolean restoreValue,
            Object defaultValue)
    {
        setText(restoreValue ? getPersistedString(mText) : (String) defaultValue);
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
    {
        super.onPrepareDialogBuilder(builder);
        builder.setInverseBackgroundForced(true);
    }
}
