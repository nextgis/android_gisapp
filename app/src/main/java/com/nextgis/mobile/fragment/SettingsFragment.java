/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.fragment.NGPreferenceSettingsFragment;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.MainApplication;
import com.nextgis.mobile.R;
import com.nextgis.mobile.util.SelectMapPathPreference;
import com.nextgis.mobile.util.ApkDownloader;
import com.nextgis.mobile.util.ConstantsApp;
import com.nextgis.mobile.util.IntEditTextPreference;

import java.io.File;

import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;
import static com.nextgis.maplibui.service.TrackerService.isTrackerServiceRunning;
import static com.nextgis.mobile.util.SettingsConstants.*;


public class SettingsFragment
        extends NGPreferenceSettingsFragment
        implements SelectMapPathPreference.OnAttachedListener
{
    @Override
    protected void createSettings(PreferenceScreen screen)
    {
        switch (mAction) {
            case SettingsConstantsUI.ACTION_PREFS_GENERAL:
                addPreferencesFromResource(R.xml.preferences_general);

                final ListPreference theme =
                        (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_THEME);
                initializeTheme(getActivity(), theme);
                final Preference reset =
                        findPreference(SettingsConstantsUI.KEY_PREF_RESET_SETTINGS);
                initializeReset(getActivity(), reset);
                break;
            case SettingsConstantsUI.ACTION_PREFS_MAP:
                addPreferencesFromResource(R.xml.preferences_map);

                final ListPreference showInfoPanel = (ListPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL);
                initializeShowStatusPanel(showInfoPanel);

                final ListPreference lpCoordinateFormat =
                        (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_COORD_FORMAT);
                final IntEditTextPreference etCoordinateFraction =
                        (IntEditTextPreference) findPreference(
                                SettingsConstantsUI.KEY_PREF_COORD_FRACTION);
                initializeCoordinates(lpCoordinateFormat, etCoordinateFraction);

                final SelectMapPathPreference mapPath = (SelectMapPathPreference) findPreference(
                        SettingsConstants.KEY_PREF_MAP_PATH);
                mapPath.setOnAttachedListener(this);
                initializeMapPath(getActivity(), mapPath);

                final ListPreference showCurrentLocation = (ListPreference) findPreference(
                        SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC);
                initializeShowCurrentLocation(showCurrentLocation);

                final ListPreference changeMapBG =
                        (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_MAP_BG);
                initializeMapBG(changeMapBG);
                break;
            case SettingsConstantsUI.ACTION_PREFS_LOCATION:
                addPreferencesFromResource(R.xml.preferences_location);

                final ListPreference lpLocationAccuracy =
                        (ListPreference) findPreference(SettingsConstants.KEY_PREF_LOCATION_SOURCE);
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

                final ListPreference lpTracksAccuracy =
                        (ListPreference) findPreference(SettingsConstants.KEY_PREF_TRACKS_SOURCE);
                initializeLocationAccuracy(lpTracksAccuracy, true);

                final ListPreference minTime =
                        (ListPreference) findPreference(SettingsConstants.KEY_PREF_TRACKS_MIN_TIME);
                final ListPreference minDistance = (ListPreference) findPreference(
                        SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE);
                initializeLocationMins(minTime, minDistance, true);
                break;
            case SettingsConstantsUI.ACTION_PREFS_UPDATE:
                ApkDownloader.check(getActivity(), true);
                break;
        }
    }


    @Override
    public FragmentManager getFragmentManagerFromParentFragment()
    {
        return getFragmentManager();
    }


    public static void initializeTheme(
            final Activity activity,
            final ListPreference theme)
    {
        if (null != theme) {
            theme.setSummary(theme.getEntry());

            theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(
                        Preference preference,
                        Object newValue)
                {
                    activity.startActivity(activity.getIntent());
                    activity.finish();
                    return true;
                }
            });
        }
    }


    public static void initializeReset(
            final Activity activity,
            final Preference preference)
    {
        if (null != preference) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    AlertDialog.Builder confirm = new AlertDialog.Builder(activity);
                    confirm.setTitle(R.string.reset_settings_title)
                            .setMessage(R.string.reset_settings_message)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which)
                                        {
                                            resetSettings(activity);
                                            deleteLayers(activity);
                                            ((MainApplication) activity.getApplication()).initBaseLayers();
                                        }
                                    })
                            .show();
                    return false;
                }
            });
        }
    }


    public static void initializeShowStatusPanel(final ListPreference listPreference)
    {
        listPreference.setSummary(listPreference.getEntry());

        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                preference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(
                        (String) newValue)]);

                return true;
            }
        });
    }


    public static void initializeCoordinates(
            ListPreference lpCoordinateFormat,
            IntEditTextPreference etCoordinateFraction)
    {
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

        if (etCoordinateFraction != null) {
            etCoordinateFraction.setSummary(etCoordinateFraction.getPersistedString(
                    "" + ConstantsApp.DEFAULT_COORDINATES_FRACTION_DIGITS));

            etCoordinateFraction.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference,
                                Object newValue)
                        {
                            preference.setSummary(newValue.toString());
                            return true;
                        }
                    });
        }
    }


    public static void initializeMapPath(
            final Context context,
            final SelectMapPathPreference mapPath)
    {
        if (null != mapPath) {
            mapPath.setSummary(mapPath.getText());

            mapPath.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(
                        Preference preference,
                        Object o)
                {
                    final Activity parent = (Activity) context;
                    if (null == parent) {
                        return false;
                    }

                    File newPath = new File((String) o);
                    if (newPath.listFiles().length != 0) {
                        Toast.makeText(context,
                                context.getString(R.string.warning_folder_should_be_empty),
                                Toast.LENGTH_LONG).show();
                        return false;
                    }

                    moveMap(parent, newPath);

                    return true;
                }
            });
        }
    }


    public static void initializeShowCurrentLocation(
            final ListPreference listPreference)
    {
        listPreference.setSummary(listPreference.getEntry());

        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                preference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(
                        (String) newValue)]);

                return true;
            }
        });
    }


    public static void initializeMapBG(final ListPreference mapBG)
    {
        mapBG.setSummary(mapBG.getEntry());

        mapBG.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                preference.setSummary(
                        mapBG.getEntries()[mapBG.findIndexOfValue((String) newValue)]);
                return true;
            }
        });
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

            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(
                        Preference preference,
                        Object newValue)
                {
                    int value = Integer.parseInt(newValue.toString());
                    CharSequence summary = ((ListPreference) preference).getEntries()[value - 1];
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

        minTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                int id = ((ListPreference) preference).findIndexOfValue((String) newValue);
                preference.setSummary(
                        getMinSummary(context, ((ListPreference) preference).getEntries()[id],
                                (String) newValue));

                String preferenceKey = isTracks
                                       ? SettingsConstants.KEY_PREF_TRACKS_MIN_TIME
                                       : SettingsConstants.KEY_PREF_LOCATION_MIN_TIME;
                preference.getSharedPreferences()
                        .edit()
                        .putString(preferenceKey, (String) newValue)
                        .apply();

                sectionWork(preference.getContext(), isTracks);

                return true;
            }
        });

        minDistance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                int id = ((ListPreference) preference).findIndexOfValue((String) newValue);
                preference.setSummary(
                        getMinSummary(context, ((ListPreference) preference).getEntries()[id],
                                (String) newValue));

                String preferenceKey = isTracks
                                       ? SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE
                                       : SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE;
                preference.getSharedPreferences()
                        .edit()
                        .putString(preferenceKey, (String) newValue)
                        .apply();

                sectionWork(preference.getContext(), isTracks);

                return true;
            }
        });
    }


    public static void initializeAccurateTaking(EditTextPreference accurateMaxCount)
    {
        accurateMaxCount.setSummary(accurateMaxCount.getText());

        accurateMaxCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                preference.setSummary((CharSequence) newValue);
                return true;
            }
        });
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


    protected static void sectionWork(
            Context context,
            boolean isTracks)
    {
        if (!isTracks) {
            MainApplication application = (MainApplication) context.getApplicationContext();
            application.getGpsEventSource().updateActiveListeners();
        } else {
            if (isTrackerServiceRunning(context)) {
                Toast.makeText(
                        context, context.getString(R.string.tracks_reload), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    protected static void resetSettings(Activity activity)
    {
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
        editor.remove(KEY_PREF_GA);

        File defaultPath = activity.getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(activity.getFilesDir(), KEY_PREF_MAP);
        }

        editor.putString(SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());
        editor.apply();

        PreferenceManager.setDefaultValues(activity, R.xml.preferences_general, true);
        PreferenceManager.setDefaultValues(activity, R.xml.preferences_map, true);
        PreferenceManager.setDefaultValues(activity, R.xml.preferences_location, true);
        PreferenceManager.setDefaultValues(activity, R.xml.preferences_tracks, true);

        moveMap(activity, defaultPath);
    }


    protected static void deleteLayers(Activity activity)
    {
        MainApplication app = (MainApplication) activity.getApplication();
        for (int i = app.getMap().getLayerCount() - 1; i >= 0; i--) {
            ILayer layer = app.getMap().getLayer(i);
            if (!layer.getPath().getName().equals(MainApplication.LAYER_OSM) && !layer.getPath()
                    .getName()
                    .equals(MainApplication.LAYER_A) && !layer.getPath()
                    .getName()
                    .equals(MainApplication.LAYER_B) && !layer.getPath()
                    .getName()
                    .equals(MainApplication.LAYER_C) && !layer.getPath()
                    .getName()
                    .equals(MainApplication.LAYER_TRACKS)) {
                layer.delete();
            }
        }

        try {
            ((MapContentProviderHelper) MapBase.getInstance()).getDatabase(false).execSQL("VACUUM");
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }


    public static void moveMap(
            Activity activity,
            File path)
    {
        MainApplication application = (MainApplication) activity.getApplication();
        if (null == application) {
            return;
        }

        ContentResolver.cancelSync(null, application.getAuthority());

        BackgroundMoveTask moveTask = new BackgroundMoveTask(activity, application.getMap(), path);
        moveTask.execute();
    }


    protected static class BackgroundMoveTask
            extends AsyncTask<Void, Void, Void>
    {
        private ProgressDialog mProgressDialog;
        private Activity       mActivity;
        private MapBase        mMap;
        private File           mPath;


        BackgroundMoveTask(
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
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setTitle(R.string.moving);
            mProgressDialog.setMessage(mActivity.getString(R.string.warning_map_moving));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIcon(R.drawable.ic_action_warning_light);
            mProgressDialog.show();
        }


        @Override
        protected void onPostExecute(Void aVoid)
        {
            mProgressDialog.dismiss();
            ControlHelper.unlockScreenOrientation(mActivity);
        }
    }
}
