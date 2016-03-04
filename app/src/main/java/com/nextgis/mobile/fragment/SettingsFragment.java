/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.SettingsActivity;
import com.nextgis.mobile.dialog.SelectMapPathDialogPreference;
import com.nextgis.mobile.util.IntEditTextPreference;

import static com.nextgis.mobile.activity.SettingsActivity.*;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment
        extends PreferenceFragment
{
    //protected SettingsSupport support;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String settings = getArguments().getString("settings");
        if (settings == null)
            return;

        switch (settings) {
            case "general":
                addPreferencesFromResource(R.xml.preferences_general);

                final ListPreference theme = (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_THEME);
                initializeTheme((SettingsActivity) getActivity(), theme);
                final Preference reset = findPreference(SettingsConstantsUI.KEY_PREF_RESET_SETTINGS);
                initializeReset((SettingsActivity) getActivity(), reset);
                break;
            case "map":
                addPreferencesFromResource(R.xml.preferences_map);

                final ListPreference showInfoPanel = (ListPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL);
                initializeShowStatusPanel(showInfoPanel);

                final ListPreference lpCoordinateFormat = (ListPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_COORD_FORMAT);
                final IntEditTextPreference etCoordinateFraction = (IntEditTextPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_COORD_FRACTION);
                initializeCoordinates(lpCoordinateFormat, etCoordinateFraction);

                final SelectMapPathDialogPreference mapPath =
                        (SelectMapPathDialogPreference) findPreference(
                                SettingsConstants.KEY_PREF_MAP_PATH);
                initializeMapPath(getActivity(), mapPath);

                final ListPreference showCurrentLocation = (ListPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC);
                initializeShowCurrentLocation(showCurrentLocation);

                final ListPreference changeMapBG = (ListPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_MAP_BG);
                initializeMapBG(getActivity(), changeMapBG);
                break;
            case "location":
                addPreferencesFromResource(R.xml.preferences_location);

                final ListPreference lpLocationAccuracy = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_LOCATION_SOURCE);
                initializeLocationAccuracy(lpLocationAccuracy, false);

                final ListPreference minTimeLoc = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_LOCATION_MIN_TIME);
                final ListPreference minDistanceLoc = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE);
                initializeLocationMins(minTimeLoc, minDistanceLoc, false);

                final EditTextPreference accurateMaxCount = (EditTextPreference) findPreference(
                        SettingsConstants.KEY_PREF_LOCATION_ACCURATE_COUNT);
                initializeAccurateTaking(accurateMaxCount);
                break;
            case "tracks":
                addPreferencesFromResource(R.xml.preferences_tracks);

                final ListPreference lpTracksAccuracy = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_TRACKS_SOURCE);
                initializeLocationAccuracy(lpTracksAccuracy, true);

                final ListPreference minTime = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_TRACKS_MIN_TIME);
                final ListPreference minDistance = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE);
                initializeLocationMins(minTime, minDistance, true);
                break;
        }
    }
}
