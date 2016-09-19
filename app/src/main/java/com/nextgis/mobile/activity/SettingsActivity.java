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

package com.nextgis.mobile.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.activity.NGPreferenceActivity;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.MainApplication;
import com.nextgis.mobile.R;
import com.nextgis.mobile.dialog.SelectMapPathDialogPreference;
import com.nextgis.mobile.fragment.SettingsFragment;
import com.nextgis.mobile.util.IntEditTextPreference;

import java.io.File;
import java.util.List;

import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;
import static com.nextgis.maplibui.service.TrackerService.isTrackerServiceRunning;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_COMPASS;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_MEASURING;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_SCALE_RULER;
import static com.nextgis.mobile.util.SettingsConstants.KEY_PREF_SHOW_ZOOM_CONTROLS;


public class SettingsActivity
        extends NGPreferenceActivity
{

    public BackgroundMoveTask mBkTask;
    protected boolean mIsPaused = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action != null) {
            switch (action) {
                case SettingsConstantsUI.ACTION_PREFS_GENERAL:
                    addPreferencesFromResource(R.xml.preferences_general);

                    final ListPreference theme = (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_THEME);
                    initializeTheme(this, theme);
                    final Preference reset = findPreference(SettingsConstantsUI.KEY_PREF_RESET_SETTINGS);
                    initializeReset(this, reset);
                    break;
                case SettingsConstantsUI.ACTION_PREFS_MAP:
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
                    initializeMapPath(this, mapPath);

                    final ListPreference showCurrentLocation = (ListPreference) findPreference(
                            SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC);
                    initializeShowCurrentLocation(showCurrentLocation);

                    final ListPreference changeMapBG = (ListPreference) findPreference(
                            SettingsConstantsUI.KEY_PREF_MAP_BG);
                    initializeMapBG(this, changeMapBG);
                    break;
                case SettingsConstantsUI.ACTION_PREFS_LOCATION:
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
                case SettingsConstantsUI.ACTION_PREFS_TRACKING:
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
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.preference_headers_legacy);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsPaused) {
            startActivity(getIntent());
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsPaused = true;
    }

    public static void initializeMapBG(final Activity activity, final ListPreference mapBG) {
        mapBG.setSummary(mapBG.getEntry());

        mapBG.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(mapBG.getEntries()[mapBG.findIndexOfValue((String) newValue)]);
                return true;
            }
        });
    }

    public static void initializeAccurateTaking(EditTextPreference accurateMaxCount) {
        accurateMaxCount.setSummary(accurateMaxCount.getText());

        accurateMaxCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((CharSequence) newValue);
                return true;
            }
        });
    }

    public static void initializeCoordinates(ListPreference lpCoordinateFormat, IntEditTextPreference etCoordinateFraction)
    {
        if (etCoordinateFraction != null) {
            etCoordinateFraction.setSummary(etCoordinateFraction.getPersistedString("6"));

            etCoordinateFraction.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference,
                                Object newValue) {
                            preference.setSummary(newValue.toString());
                            return true;
                        }
                    });
        }

        if (null != lpCoordinateFormat) {
            lpCoordinateFormat.setSummary(lpCoordinateFormat.getEntry());
            lpCoordinateFormat.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference,
                                Object newValue)
                        {
                            int value = Integer.parseInt(newValue.toString());
                            CharSequence summary =
                                    ((ListPreference) preference).getEntries()[value];
                            preference.setSummary(summary);

                            return true;
                        }
                    });
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return SettingsFragment.class.getName().equals(fragmentName);
        //return super.isValidFragment(fragmentName);
    }


    public static void initializeLocationAccuracy(
            final ListPreference listPreference,
            final boolean isTracks)
    {
        if (listPreference != null) {
            Context ctx = listPreference.getContext();
            CharSequence[] entries = new CharSequence[3];
            entries[0] = ctx.getString(R.string.pref_location_accuracy_gps);
            entries[1] = ctx.getString(R.string.pref_location_accuracy_cell);
            entries[2] = ctx.getString(R.string.pref_location_accuracy_gps) +
                         " & " +
                         ctx.getString(R.string.pref_location_accuracy_cell);
            listPreference.setEntries(entries);
            listPreference.setSummary(listPreference.getEntry());

            listPreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference,
                                Object newValue)
                        {
                            int value = Integer.parseInt(newValue.toString());
                            CharSequence summary =
                                    ((ListPreference) preference).getEntries()[value - 1];
                            preference.setSummary(summary);

                            sectionWork(preference.getContext(), isTracks);

                            return true;
                        }
                    });
        }
    }


    public static void initializeLocationMins(
            ListPreference minTime,
            final ListPreference minDistance,
            final boolean isTracks)
    {
        final Context context = minDistance.getContext();
        minTime.setSummary(getMinSummary(context, minTime.getEntry(), minTime.getValue()));
        minDistance.setSummary(
                getMinSummary(context, minDistance.getEntry(), minDistance.getValue()));

        minTime.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener()
                {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue)
                    {
                        int id = ((ListPreference) preference).findIndexOfValue((String) newValue);
                        preference.setSummary(
                                getMinSummary(
                                        context, ((ListPreference) preference).getEntries()[id],
                                        (String) newValue));

                        String preferenceKey = isTracks
                                               ? SettingsConstants.KEY_PREF_TRACKS_MIN_TIME
                                               : SettingsConstants.KEY_PREF_LOCATION_MIN_TIME;
                        preference.getSharedPreferences()
                                .edit()
                                .putString(preferenceKey, (String) newValue)
                                .commit();

                        sectionWork(preference.getContext(), isTracks);

                        return true;
                    }
                });

        minDistance.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener()
                {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue)
                    {
                        int id = ((ListPreference) preference).findIndexOfValue((String) newValue);
                        preference.setSummary(
                                getMinSummary(
                                        context, ((ListPreference) preference).getEntries()[id],
                                        (String) newValue));

                        String preferenceKey = isTracks
                                               ? SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE
                                               : SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE;
                        preference.getSharedPreferences()
                                .edit()
                                .putString(preferenceKey, (String) newValue)
                                .commit();

                        sectionWork(preference.getContext(), isTracks);

                        return true;
                    }
                });
    }


    protected static void sectionWork(
            Context context,
            boolean isTracks)
    {
        if (!isTracks) {
            Activity parent = (Activity) context;
            MainApplication application = (MainApplication) parent.getApplication();
            application.getGpsEventSource().updateActiveListeners();
        } else {
            if (isTrackerServiceRunning(context)) {
                Toast.makeText(
                        context, context.getString(R.string.tracks_reload), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    private static String getMinSummary(
            Context context,
            CharSequence newEntry,
            String newValue)
    {
        int value = 0;

        try {
            value = Integer.parseInt(newValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        String addition = newEntry + "";
        addition += value == 0 ? context.getString(R.string.frequentest) : "";

        return addition;
    }


    public static void initializeMapPath(
            final Context context,
            final SelectMapPathDialogPreference mapPath)
    {
        if (null != mapPath) {
            mapPath.setSummary(mapPath.getText());

            mapPath.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference,
                                Object o)
                        {
                            final SettingsActivity parent = (SettingsActivity) context;
                            if (null == parent) {
                                return false;
                            }

                            File newPath = new File((String) o);
                            if (newPath.listFiles().length != 0) {
                                Toast.makeText(
                                        context, context.getString(
                                                R.string.warning_folder_should_be_empty),
                                        Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            parent.moveMap(newPath);

                            return true;
                        }
                    });
        }
    }


    public static void initializeTheme(final SettingsActivity activity, final ListPreference theme) {
        if (null != theme) {
            theme.setSummary(theme.getEntry());

            theme.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference,
                                Object newValue) {
                            activity.startActivity(activity.getIntent());
                            activity.finish();
                            return true;
                        }
                    });
        }
    }


    public static void initializeReset(final SettingsActivity activity, final Preference preference) {
        if (null != preference) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder confirm = new AlertDialog.Builder(activity);
                    confirm.setTitle(R.string.reset_settings_title).setMessage(R.string.reset_settings_message)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resetSettings(activity);
                                    deleteLayers(activity);
                                    ((MainApplication) activity.getApplication()).initBaseLayers();
                                }
                            }).show();
                    return false;
                }
            });
        }
    }


    protected static void resetSettings(SettingsActivity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(SettingsConstantsUI.KEY_PREF_THEME);
        editor.remove(SettingsConstantsUI.KEY_PREF_COMPASS_TRUE_NORTH);
        editor.remove(SettingsConstantsUI.KEY_PREF_COMPASS_MAGNETIC);
        editor.remove(SettingsConstantsUI.KEY_PREF_COMPASS_KEEP_SCREEN);
        editor.remove(SettingsConstantsUI.KEY_PREF_COMPASS_VIBRATE);
        editor.remove(SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL);
        editor.remove(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC);
        editor.remove(KEY_PREF_SHOW_COMPASS);
        editor.remove(SettingsConstantsUI.KEY_PREF_KEEPSCREENON);
        editor.remove(SettingsConstantsUI.KEY_PREF_COORD_FORMAT);
        editor.remove(KEY_PREF_SHOW_ZOOM_CONTROLS);
        editor.remove(SettingsConstants.KEY_PREF_LOCATION_SOURCE);
        editor.remove(SettingsConstants.KEY_PREF_LOCATION_MIN_TIME);
        editor.remove(SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE);
        editor.remove(SettingsConstants.KEY_PREF_LOCATION_ACCURATE_COUNT);
        editor.remove(SettingsConstants.KEY_PREF_TRACKS_SOURCE);
        editor.remove(SettingsConstants.KEY_PREF_TRACKS_MIN_TIME);
        editor.remove(SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE);
        editor.remove(SettingsConstants.KEY_PREF_TRACK_RESTORE);
        editor.remove(KEY_PREF_SHOW_MEASURING);
        editor.remove(KEY_PREF_SHOW_SCALE_RULER);
        editor.remove(SettingsConstantsUI.KEY_PREF_SHOW_GEO_DIALOG);

        File defaultPath = activity.getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath == null)
            defaultPath = new File(activity.getFilesDir(), KEY_PREF_MAP);

        editor.putString(SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());
        editor.commit();

        PreferenceManager.setDefaultValues(activity, R.xml.preferences_general, true);
        PreferenceManager.setDefaultValues(activity, R.xml.preferences_map, true);
        PreferenceManager.setDefaultValues(activity, R.xml.preferences_location, true);
        PreferenceManager.setDefaultValues(activity, R.xml.preferences_tracks, true);

        activity.moveMap(defaultPath);
    }


    protected static void deleteLayers(Activity activity) {
        MainApplication app = (MainApplication) activity.getApplication();
        for (int i = app.getMap().getLayerCount() - 1; i >= 0; i--) {
            ILayer layer = app.getMap().getLayer(i);
            if (!layer.getPath().getName().equals(MainApplication.LAYER_OSM)
                    && !layer.getPath().getName().equals(MainApplication.LAYER_A)
                    && !layer.getPath().getName().equals(MainApplication.LAYER_B)
                    && !layer.getPath().getName().equals(MainApplication.LAYER_C)
                    && !layer.getPath().getName().equals(MainApplication.LAYER_TRACKS))
                layer.delete();
        }
    }


    public void moveMap(File path)
    {
        MainApplication application = (MainApplication) getApplication();
        if (null == application) {
            return;
        }

        ContentResolver.cancelSync(null, application.getAuthority());

        mBkTask = new BackgroundMoveTask(this, application.getMap(), path);
        mBkTask.execute();
    }

    public static void initializeShowStatusPanel(final ListPreference listPreference) {
        listPreference.setSummary(listPreference.getEntry());

        listPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener()
                {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue)
                    {
                        preference.setSummary(
                                listPreference.getEntries()[listPreference.findIndexOfValue(
                                        (String) newValue)]);

                        return true;
                    }
                });
    }

    public static void initializeShowCurrentLocation(
            final ListPreference listPreference)
    {
        listPreference.setSummary(listPreference.getEntry());

        listPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener()
                {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference,
                            Object newValue)
                    {
                        preference.setSummary(
                                listPreference.getEntries()[listPreference.findIndexOfValue(
                                        (String) newValue)]);

                        return true;
                    }
                });
    }


    protected static class BackgroundMoveTask
            extends AsyncTask<Void, Void, Void>
    {
        protected ProgressDialog mProgressDialog;
        protected Activity       mActivity;
        protected MapBase        mMap;
        protected File           mPath;


        public BackgroundMoveTask(
                Activity activity,
                MapBase map,
                File path)
        {
            mActivity = activity;
            mMap = map;
            mPath = path;
        }


        @Override
        protected Void doInBackground(Void... voids)
        {
            mMap.moveTo(mPath);
            return null;
        }


        @Override
        protected void onPreExecute()
        {
            //not good solution but rare used so let it be
            ControlHelper.lockScreenOrientation(mActivity);
            mProgressDialog = ProgressDialog.show(
                    mActivity, mActivity.getString(R.string.moving),
                    mActivity.getString(R.string.warning_map_moving), true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIcon(
                    mActivity.getResources().getDrawable(R.drawable.ic_action_warning_light));
        }


        @Override
        protected void onPostExecute(Void aVoid)
        {
            mProgressDialog.dismiss();
            ControlHelper.unlockScreenOrientation(mActivity);
        }
    }
}
