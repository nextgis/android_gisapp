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

import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_SD_CARD_NAME;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Toast;

import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.dialog.LocalResourceSelectDialog;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.mobile.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


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

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        File defaultPath = getContext().getExternalFilesDir(SettingsConstants.KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(getContext().getFilesDir(), SettingsConstants.KEY_PREF_MAP);
        }

        String mapPath = mSharedPreferences.getString(SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());

        File[] files = ContextCompat.getExternalFilesDirs(context, null);

        if (files != null && files.length > 0) {
            int itemCount = -1;

            final List<File>  externalFilesPaths = new ArrayList<>();
            final List<String> externalFilesStrings = new ArrayList<>();
            final List<String> cardnames = new ArrayList<>();
            // два носителя или больше - есть что выбирать
            int selectedItem = -1;

            for (File file : files){
                if (file != null) {

                    String cardNamePart = "";
                    String parts[] = file.getAbsolutePath().split("/");
                    if (parts.length>2)
                        cardNamePart = parts[2];
                    cardnames.add(cardNamePart);


                    itemCount++;
                    String path = file.getAbsolutePath();
                    if (mapPath.contains(path))
                        selectedItem = itemCount;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        externalFilesPaths.add(file);
                        if (Environment.isExternalStorageRemovable(file))
                            externalFilesStrings.add(context.getString(R.string.sd_card) + ": " + cardNamePart);
                        else
                            externalFilesStrings.add(context.getString(R.string.internal_storage) +": " + cardNamePart);
                    } else {
                        externalFilesPaths.add(file);
                        if (!path.contains("emulated"))
                            externalFilesStrings.add(context.getString(R.string.sd_card) + ": " + cardNamePart);
                        else
                            externalFilesStrings.add(context.getString(R.string.internal_storage) + ": " + cardNamePart);
                    }
                }
            }

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle(R.string.choose_storage);
            final int selectedItemFinal = selectedItem;
            String[] array = externalFilesStrings.toArray(new String[externalFilesPaths.size()]);
            alertDialog.setSingleChoiceItems(array, selectedItemFinal, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    int d = 0;
                    if(i == selectedItemFinal) {
                        dialogInterface.dismiss();
                        return;
                    }
                    String value = externalFilesPaths.get(i).getAbsolutePath()+ "/map";

                    final String finalValue = value;
                    final String cardcardnameFinal = cardnames.get(i);

                    // check path exists
                    File newPath = new File(value);
                    if (newPath.listFiles() !=null && newPath.listFiles().length != 0) {
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                        builder.setMessage(R.string.warning_folder_should_be_empty_clear)
                                .setPositiveButton(R.string.clean, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (callChangeListener(finalValue)) {
                                            setText(finalValue);
                                            setSummary(finalValue);
                                            saveStoredCardNameToSettings(cardcardnameFinal);

                                        }
                                    }
                                })
                                .setNegativeButton(R.string.cancel,null)
                                .setTitle("");
                        builder.create().show();
                        dialogInterface.dismiss();
                        return;
                    }

                    if (callChangeListener(finalValue)) {
                        setText(finalValue);
                        setSummary(finalValue);
                        saveStoredCardNameToSettings(cardnames.get(i));
                    }
                    dialogInterface.dismiss();
                }
            });

            AlertDialog customAlertDialog = alertDialog.create();
            customAlertDialog.show();

        } else {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.no_storages_error)
                    .setPositiveButton(R.string.ok, null)
                    .create()
                    .show();
        }

//         old change path
//        File path = context.getExternalFilesDir(null);
//        if (null == path) {
//            path = context.getFilesDir();
//        }
//
//        if (mFragmentManager != null) {
//            LocalResourceSelectDialog dialog = new LocalResourceSelectDialog();
//            dialog.setPath(path);
//            dialog.setTypeMask(Constants.FILETYPE_FOLDER);
//            dialog.setCanSelectMultiple(false);
//            dialog.setOnSelectionListener(this);
//            dialog.show(mFragmentManager, ConstantsUI.FRAGMENT_SELECT_RESOURCE);
//        }
    }

    public void saveStoredCardNameToSettings(final String sdCardName){
        if (!TextUtils.isEmpty(sdCardName)) {
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            mSharedPreferences.edit().putString(KEY_PREF_SD_CARD_NAME, sdCardName).apply();
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
