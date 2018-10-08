/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2018 NextGIS, info@nextgis.com
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

import android.support.v7.preference.PreferenceScreen;

import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplibui.fragment.NGPreferenceHeaderFragment;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.R;


public class SettingsHeaderFragment
        extends NGPreferenceHeaderFragment
{
    @Override
    protected void createPreferences(PreferenceScreen screen)
    {
        addPreferencesFromResource(R.xml.preference_headers);
        if (getActivity() != null && !AccountUtil.isProUser(getActivity()))
            screen.findPreference(SettingsConstantsUI.ACTION_PREFS_NGW).setIcon(R.drawable.ic_lock_black_24dp);
    }
}
