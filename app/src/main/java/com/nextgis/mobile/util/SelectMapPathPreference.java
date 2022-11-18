/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2017, 2019 NextGIS, info@nextgis.com
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

package com.nextgis.mobile.util;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.dialog.LocalResourceSelectDialog;
import com.nextgis.maplibui.util.ConstantsUI;

import java.io.File;


/**
 * A dialog to select map path from settings
 */
public class SelectMapPathPreference
        extends Preference
        implements LocalResourceSelectDialog.OnSelectionListener
{
    protected String mText;

    protected FragmentManager    mFragmentManager;
    protected OnAttachedListener mOnAttachedListener;


    public SelectMapPathPreference(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
    }


    @Override
    public void onAttached()
    {
        super.onAttached();

        if (null != mOnAttachedListener) {
            mFragmentManager = mOnAttachedListener.getFragmentManagerFromParentFragment();
        }
    }


    @Override
    protected void onClick()
    {
        super.onClick();

        Context context = getContext();
        File path = context.getExternalFilesDir(null);
        if (null == path) {
            path = context.getFilesDir();
        }

        if (mFragmentManager != null) {
            LocalResourceSelectDialog dialog = new LocalResourceSelectDialog();
            dialog.setPath(path);
            dialog.setTypeMask(Constants.FILETYPE_FOLDER);
            dialog.setCanSelectMultiple(false);
            dialog.setOnSelectionListener(this);
            dialog.show(mFragmentManager, ConstantsUI.FRAGMENT_SELECT_RESOURCE);
        }
    }


    @Override
    public void onSelection(File file)
    {
        String value = file.getAbsolutePath();
        if (callChangeListener(value)) {
            setText(value);
            setSummary(value);
        }
    }


    /**
     * Saves the text to the {@link android.content.SharedPreferences}.
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
     * Gets the text from the {@link android.content.SharedPreferences}.
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


    public void setOnAttachedListener(OnAttachedListener onAttachedListener)
    {
        mOnAttachedListener = onAttachedListener;
    }


    public interface OnAttachedListener
    {
        FragmentManager getFragmentManagerFromParentFragment();
    }
}
