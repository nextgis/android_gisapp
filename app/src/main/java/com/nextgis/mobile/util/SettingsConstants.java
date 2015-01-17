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

package com.nextgis.mobile.util;

public interface SettingsConstants
{
    public final static String AUTHORITY = "com.nextgis.mobile.provider";
    /**
     * preference pages
     */
    public final static String ACTION_PREFS_GENERAL  = "com.nextgis.mobile.PREFS_GENERAL";
    public final static String ACTION_PREFS_MAP      = "com.nextgis.mobile.PREFS_MAP";
    public final static String ACTION_PREFS_NGW      = "com.nextgis.mobile.PREFS_NGW";
    public final static String ACTION_PREFS_COMPASS  = "com.nextgis.mobile.PREFS_COMPASS";
    public final static String ACTION_PREFS_TRACKING = "com.nextgis.mobile.PREFS_TRACKING";
    public final static String ACTION_PREFS_EDIT     = "com.nextgis.mobile.PREFS_EDIT";

    /**
     * map preference
     */
    public static final String KEY_PREF_COORD_FORMAT = "coordinates_format";

    /**
     * Preference key - not UI
     */
    public static final String KEY_PREF_SCROLL_X      = "map_scroll_x";
    public static final String KEY_PREF_SCROLL_Y      = "map_scroll_y";
    public static final String KEY_PREF_ZOOM_LEVEL    = "map_zoom_level";
    public static final String KEY_PREF_SHOW_LOCATION = "map_show_loc";
    public static final String KEY_PREF_SHOW_COMPASS  = "map_show_compass";
    public static final String KEY_PREF_SHOW_INFO     = "map_show_info";
    public static final String KEY_PREF_APP_FIRST_RUN = "app_first_run";

    /**
     * Preference keys - in UI
     */
    public static final String KEY_PREF_STORAGE_SITE         = "storage_site";
    public static final String KEY_PREF_USER_ID              = "user_id";
    public static final String KEY_PREF_MIN_DIST_CHNG_UPD    = "min_dist_change_for_update";
    public static final String KEY_PREF_MIN_TIME_UPD         = "min_time_beetwen_updates";
    public static final String KEY_PREF_SW_TRACK_SRV         = "sw_track_service";
    public static final String KEY_PREF_SW_TRACKGPX_SRV      = "sw_trackgpx_service";
    public static final String KEY_PREF_SHOW_LAYES_LIST      = "show_layers_list";
    public static final String KEY_PREF_SW_SENDPOS_SRV       = "sw_sendpos_service";
    public static final String KEY_PREF_SW_ENERGY_ECO        = "sw_energy_economy";
    public static final String KEY_PREF_TIME_DATASEND        = "time_between_datasend";
    public static final String KEY_PREF_ACCURATE_LOC         = "accurate_coordinates_pick";
    public static final String KEY_PREF_ACCURATE_GPSCOUNT    = "accurate_coordinates_pick_count";
    public static final String KEY_PREF_ACCURATE_CE          = "accurate_type";
    public static final String KEY_PREF_TILE_SIZE            = "map_tile_size";
    public static final String KEY_PREF_COMPASS_VIBRO        = "compass_vibration";
    public static final String KEY_PREF_COMPASS_TRUE_NORTH   = "compass_true_north";
    public static final String KEY_PREF_COMPASS_SHOW_MAGNET  = "compass_show_magnetic";
    public static final String KEY_PREF_COMPASS_WAKE_LOCK    = "compass_wake_lock";
    public static final String KEY_PREF_MAP_NAME             = "map_name";
    public static final String KEY_PREF_SHOW_ZOOM_CONTROLS   = "show_zoom_controls";
}
