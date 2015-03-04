/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2012-2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.mobile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.File;
import java.util.List;

import static com.nextgis.maplibui.TracksActivity.isTrackerServiceRunning;
import static com.nextgis.mobile.util.SettingsConstants.*;


public class SettingsActivity
        extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ViewGroup root = ((ViewGroup) findViewById(android.R.id.content));
        if(null != root) {
            View content = root.getChildAt(0);
            if (null != content) {
                LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_settings, null);

                root.removeAllViews();
                toolbarContainer.addView(content);
                root.addView(toolbarContainer);

                Toolbar toolbar = (Toolbar) toolbarContainer.findViewById(R.id.main_toolbar);
                toolbar.getBackground().setAlpha(255);
                toolbar.setTitle(getTitle());
                toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
                toolbar.setNavigationOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        SettingsActivity.this.finish();
                    }
                });
            }
        }

        String action = getIntent().getAction();
        if (action != null) {
            switch (action) {
                /*case ACTION_PREFS_GENERAL:
                    addPreferencesFromResource(R.xml.preferences_general);
                    break;*/
                case ACTION_PREFS_MAP:
                    addPreferencesFromResource(R.xml.preferences_map);

                    final ListPreference lpCoordinateFormat = (ListPreference) findPreference(
                            SettingsConstantsUI.KEY_PREF_COORD_FORMAT);
                    initializeCoordinateFormat(lpCoordinateFormat);

                    final SelectMapPathDialogPreference mapPath = (SelectMapPathDialogPreference) findPreference(
                            SettingsConstants.KEY_PREF_MAP_PATH);
                    initializeMapPath(this, mapPath);

                    break;
                case ACTION_PREFS_LOCATION:
                    addPreferencesFromResource(R.xml.preferences_location);

                    final ListPreference lpLocationAccuracy = (ListPreference) findPreference(
                            SettingsConstants.KEY_PREF_LOCATION_SOURCE + "_str");
                    initializeLocationAccuracy(lpLocationAccuracy, false);

                    final EditTextPreference minTimeLoc = (EditTextPreference) findPreference(
                            SettingsConstants.KEY_PREF_LOCATION_MIN_TIME);
                    final EditTextPreference minDistanceLoc = (EditTextPreference) findPreference(
                            SettingsConstants.KEY_PREF_LOCATION_MIN_DISTANCE);
                    initializeLocationMins(minTimeLoc, minDistanceLoc, false);
                    break;
                case ACTION_PREFS_TRACKING:
                    addPreferencesFromResource(R.xml.preferences_tracks);

                    final ListPreference lpTracksAccuracy = (ListPreference) findPreference(
                            SettingsConstants.KEY_PREF_TRACKS_SOURCE + "_str");
                    initializeLocationAccuracy(lpTracksAccuracy, true);

                    final EditTextPreference minTime = (EditTextPreference) findPreference(
                            SettingsConstants.KEY_PREF_TRACKS_MIN_TIME);
                    final EditTextPreference minDistance = (EditTextPreference) findPreference(
                            SettingsConstants.KEY_PREF_TRACKS_MIN_DISTANCE);
                    initializeLocationMins(minTime, minDistance, true);
                    break;
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.preference_headers_legacy);
        }
    }

    public static void initializeCoordinateFormat(ListPreference lpCoordinateFormat)
    {
        if(null != lpCoordinateFormat){
            lpCoordinateFormat.setSummary(lpCoordinateFormat.getEntry());

            lpCoordinateFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(
                        Preference preference,
                        Object newValue)
                {
                    int value = Integer.parseInt(newValue.toString());
                    CharSequence summary = ((ListPreference) preference).getEntries()[value];
                    preference.setSummary(summary);

                    String preferenceKey = preference.getKey() + "_int";
                    preference.getSharedPreferences().edit().putInt(preferenceKey, value).commit();

                    return true;
                }
            });
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

                    String preferenceKey = isTracks
                                           ? SettingsConstants.KEY_PREF_TRACKS_SOURCE
                                           : SettingsConstants.KEY_PREF_LOCATION_SOURCE;
                    preference.getSharedPreferences().edit().putInt(preferenceKey, value).commit();

                    sectionWork(preference.getContext(), isTracks);

                    return true;
                }
            });
        }
    }


    public static void initializeLocationMins(
            EditTextPreference minTime,
            final EditTextPreference minDistance,
            final boolean isTracks)
    {
        final Context context = minDistance.getContext();
        minTime.setSummary(getMinSummary(context, R.string.unit_second, minTime.getText()));
        minDistance.setSummary(getMinSummary(context, R.string.unit_meter, minDistance.getText()));

        minTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                preference.setSummary(getMinSummary(context, R.string.unit_second, (String) newValue));

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

        minDistance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            @Override
            public boolean onPreferenceChange(
                    Preference preference,
                    Object newValue)
            {
                preference.setSummary(
                        getMinSummary(context, R.string.unit_meter, (String) newValue));

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
            GISApplication application = (GISApplication) parent.getApplication();
            application.getGpsEventSource().updateActiveListeners();
        } else {
            if (isTrackerServiceRunning(context)) {
                Toast.makeText(context, context.getString(R.string.tracks_reload),
                               Toast.LENGTH_SHORT).show();
            }
        }
    }


    private static String getMinSummary(
            Context context,
            int unit,
            String newValue)
    {
        int value = 0;

        try {
            value = Integer.parseInt(newValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        String addition = context.getString(unit);
        addition += value == 0 ? context.getString(R.string.frequentest) : "";

        return value + " " + addition;
    }

    public static void initializeMapPath(final Context context, final SelectMapPathDialogPreference mapPath)
    {
        if(null != mapPath){
            mapPath.setSummary(mapPath.getText());

            mapPath.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                private ProgressDialog dialog;

                @Override
                public boolean onPreferenceChange(
                        Preference preference,
                        Object o)
                {
                    Activity parent = (Activity) context;
                    if(null == parent)
                        return false;

                    GISApplication application = (GISApplication) parent.getApplication();
                    if(null == application)
                        return false;

                    File newPath = new File((String)o);
                    if(newPath.listFiles().length != 0){
                        Toast.makeText(context, context.getString(R.string.warning_folder_should_be_empty), Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    ContentResolver.cancelSync(null, application.getAuthority());

                    new BackgroundMoveTask(parent, application.getMap(), new File((String)o)).execute();

                    return true;
                }
            });
        }
    }

    private static class BackgroundMoveTask extends AsyncTask<Void, Void, Void>
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
            lockScreenOrientation();
            mProgressDialog =
                    ProgressDialog.show(mActivity, mActivity.getString(R.string.moving), mActivity.getString(R.string.warning_map_moving), true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIcon(mActivity.getResources().getDrawable(R.drawable.ic_action_warning));
        }


        @Override
        protected void onPostExecute(Void aVoid)
        {
            mProgressDialog.dismiss();
            unlockScreenOrientation();
        }


        protected void lockScreenOrientation()
        {
            int currentOrientation = mActivity.getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }


        protected void unlockScreenOrientation()
        {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }
}
