/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2018, 2021 NextGIS, info@nextgis.com
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

package com.nextgis.mobile.activity;

import static com.nextgis.mobile.fragment.SettingsFragment.REQUEST_NOTIFICATION_PERMISSION;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplibui.activity.NGIDSettingsActivity;
import com.nextgis.maplibui.activity.NGPreferenceActivity;
import com.nextgis.maplibui.activity.NGWSettingsActivity;
import com.nextgis.maplibui.fragment.NGIDSettingsFragment;
import com.nextgis.maplibui.fragment.NGPreferenceSettingsFragment;
import com.nextgis.maplibui.fragment.NGPreferenceHeaderFragment;
import com.nextgis.maplibui.fragment.NGWSettingsFragment;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.R;
import com.nextgis.mobile.fragment.SettingsFragment;
import com.nextgis.mobile.fragment.SettingsHeaderFragment;
import com.nextgis.mobile.util.AppConstants;
import com.nextgis.mobile.util.Logger;

/**
 * Application preference
 */
public class SettingsActivity
        extends NGPreferenceActivity
{
    @Override
    protected String getPreferenceHeaderFragmentTag()
    {
        return AppConstants.FRAGMENT_SETTINGS_HEADER_FRAGMENT;
    }


    @Override
    protected NGPreferenceHeaderFragment getNewPreferenceHeaderFragment()
    {
        return new SettingsHeaderFragment();
    }


    @Override
    protected String getPreferenceSettingsFragmentTag()
    {
        return AppConstants.FRAGMENT_SETTINGS_FRAGMENT;
    }


    @Override
    protected NGPreferenceSettingsFragment getNewPreferenceSettingsFragment(String subScreenKey)
    {
        NGPreferenceSettingsFragment fragment;
        switch (subScreenKey) {
            default:
                fragment = new SettingsFragment();
                break;
            case SettingsConstantsUI.ACTION_PREFS_NGW:
                fragment = new NGWSettingsFragment();
                break;
            case SettingsConstantsUI.ACTION_PREFS_NGID:
                fragment = new NGIDSettingsFragment();
                break;
        }
        return fragment;
    }


    @Override
    public String getTitleString()
    {
        return getString(R.string.action_settings);
    }


    @Override
    public void setTitle(PreferenceScreen preferenceScreen)
    {
        switch (preferenceScreen.getKey()) {
            default:
                super.setTitle(preferenceScreen);
                return;
            case SettingsConstantsUI.ACTION_PREFS_NGW:
            case SettingsConstantsUI.ACTION_PREFS_NGID:
                break;
        }
    }


    @Override
    protected void onStartSubScreen(PreferenceScreen preferenceScreen)
    {
        Intent intent;
        switch (preferenceScreen.getKey()) {
            default:
                super.onStartSubScreen(preferenceScreen);
                return;
            case SettingsConstantsUI.ACTION_PREFS_NGW:
                if (!AccountUtil.isProUser(this)) {
                    ControlHelper.showProDialog(this);
                    return;
                }
                intent = new Intent(this, NGWSettingsActivity.class);
                break;
            case SettingsConstantsUI.ACTION_PREFS_NGID:
                intent = new Intent(this, NGIDSettingsActivity.class);
                break;
        }
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("save_log", false)) {
            Logger.initialize(this);
        }
    }



    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data)    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_NOTIFICATION_PERMISSION:
                for (Fragment fragment : getSupportFragmentManager ().getFragments()){
                    if (fragment instanceof SettingsFragment)
                        ((SettingsFragment)fragment).processPermission(requestCode,resultCode, data);
                }
                break;
            default:
                super.onActivityResult(requestCode,  resultCode, data);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION){
            if (grantResults.length > 0  ) {
                if (grantResults[0] == PackageManager.PERMISSION_DENIED)
                    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                        if (fragment instanceof SettingsFragment)
                            ((SettingsFragment) fragment).processPermission(requestCode, RESULT_CANCELED, null);
                    }
                else if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                        if (fragment instanceof SettingsFragment)
                            ((SettingsFragment) fragment).processPermission(requestCode, RESULT_OK, null);
                    }
            }

        }



    }
}
