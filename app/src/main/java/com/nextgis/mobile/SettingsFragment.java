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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.nextgis.maplib.util.SettingsConstants;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment
{
    //protected SettingsSupport support;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String settings = getArguments().getString("settings");
        if ("general".equals(settings)) {
            addPreferencesFromResource(R.xml.preferences_general);
        } else if ("map".equals(settings)) {
            addPreferencesFromResource(R.xml.preferences_map);

            final ListPreference lpLocationAccuracy = (ListPreference) findPreference(
                    SettingsConstants.KEY_PREF_LOCATION_SOURCE+"_str");
            SettingsActivity.initializeLocationAccuracy(lpLocationAccuracy);
        } /*else if ("user".equals(settings)) {
            addPreferencesFromResource(R.xml.preferences_user);
        }
        support = new SettingsSupport(getActivity(), this.getPreferenceScreen());
        */
    }

    @Override
    public void onResume() {
        super.onResume();
    //    if(support != null)
    //        support.registerListener();
    }

    @Override
    public void onPause() {
        super.onPause();
    //    if(support != null)
    //        support.unregisterListener();
    }
}
