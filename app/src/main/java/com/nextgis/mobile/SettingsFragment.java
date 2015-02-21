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
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.util.SettingsConstantsUI;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment
{
    //protected SettingsSupport support;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String settings = getArguments().getString("settings");
        switch (settings) {
            /*case "general":
                addPreferencesFromResource(R.xml.preferences_general);
                break;*/
            case "map":
                addPreferencesFromResource(R.xml.preferences_map);

                final ListPreference lpCoordinateFormat = (ListPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_COORD_FORMAT);
                SettingsActivity.initializeCoordinateFormat(lpCoordinateFormat);

                final SelectMapPathDialogPreference mapPath = (SelectMapPathDialogPreference) findPreference(
                        SettingsConstants.KEY_PREF_MAP_PATH);
                SettingsActivity.initializeMapPath(getActivity(), mapPath);

                break;
            case "location":
                addPreferencesFromResource(R.xml.preferences_location);

                final ListPreference lpLocationAccuracy = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_LOCATION_SOURCE + "_str");
                SettingsActivity.initializeLocationAccuracy(lpLocationAccuracy, false);

                final EditTextPreference minTimeLoc = (EditTextPreference) findPreference(
                        SettingsConstants.KEY_PREF_LOCATION_MIN_TIME);
                final EditTextPreference minDistanceLoc = (EditTextPreference) findPreference(
                        SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE);
                SettingsActivity.initializeLocationMins(minTimeLoc, minDistanceLoc, false);
                break;
            case "tracks":
                addPreferencesFromResource(R.xml.preferences_tracks);

                final ListPreference lpTracksAccuracy = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_TRACKS_SOURCE + "_str");
                SettingsActivity.initializeLocationAccuracy(lpTracksAccuracy, true);

                final EditTextPreference minTime = (EditTextPreference) findPreference(
                        SettingsConstants.KEY_PREF_TRACKS_MIN_TIME);
                final EditTextPreference minDistance = (EditTextPreference) findPreference(
                        SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE);
                SettingsActivity.initializeLocationMins(minTime, minDistance, true);
                break;
        }
    }
}
