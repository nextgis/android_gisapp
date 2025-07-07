/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2021 NextGIS, info@nextgis.com
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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Messenger;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;

import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.HttpResponse;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.api.IChooseLayerResult;
import com.nextgis.maplibui.api.IVectorLayerUI;
import com.nextgis.maplibui.fragment.BottomToolbar;
import com.nextgis.maplibui.fragment.LayerFillProgressDialogFragment;
import com.nextgis.maplibui.overlay.EditLayerOverlay;
import com.nextgis.maplibui.service.TrackerReceiver;
import com.nextgis.maplibui.service.TrackerService;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.NGIDUtils;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.maplibui.util.UiUtil;
import com.nextgis.mobile.MainApplication;
import com.nextgis.mobile.R;
import com.nextgis.mobile.fragment.LayersFragment;
import com.nextgis.mobile.fragment.MapFragment;
import com.nextgis.mobile.util.AppSettingsConstants;
import com.nextgis.mobile.util.SDCardUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.nextgis.maplib.util.Constants.JSON_SUPPORTED_KEY;
import static com.nextgis.maplib.util.Constants.JSON_USER_ID_KEY;
import static com.nextgis.maplib.util.Constants.MESSAGE_ALERT_INTENT;
import static com.nextgis.maplib.util.Constants.MESSAGE_EXTRA;
import static com.nextgis.maplib.util.Constants.MESSAGE_TITLE_EXTRA;
import static com.nextgis.maplib.util.Constants.SUPPORT;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
import static com.nextgis.maplib.util.PermissionUtil.hasLocationPermissions;
import static com.nextgis.maplibui.service.TrackerService.PERMISSIONS_REQUEST_ZERO_LOCATION_POSPONDED;
import static com.nextgis.maplibui.service.TrackerService.hasUnfinishedTracks;

import org.json.JSONObject;

/**
 * Main activity. Map and drawer with layers list created here
 */
public class MainActivity extends NGActivity
        implements GpsEventListener, IChooseLayerResult
{
    protected final static int PERMISSIONS_REQUEST_ZERO = 0;
    protected final static int PERMISSIONS_REQUEST_LOC = 1;
    protected final static int PERMISSIONS_REQUEST_ACCOUNT = 2;
    protected final static int PERMISSIONS_REQUEST_MEMORY = 3;
    protected final static int PERMISSIONS_REQUEST_PUSH = 4;

    protected final static int PERMISSIONS_REQUEST_LOC_SILENT = 6;
    public final static int LOCATION_BACKGROUND_REQUEST = 5;
    protected final static String TAG_FRAGMENT_PROGRESS = "layer_fill_dialog_fragment";

    protected MapFragment     mMapFragment;
    protected LayersFragment  mLayersFragment;
    protected MessageReceiver mMessageReceiver;
    protected TrackerReceiver mMessageReceiverTracker;

    protected Toolbar         mToolbar;

    protected final static int FILE_SELECT_CODE = 555;
    protected final static int RELOAD_ACTIVITY_DATA = 777;

    protected long mBackPressed;
    protected MenuItem mTrackItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_map, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_location, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_tracks, false);

        if (!mPreferences.getBoolean(AppSettingsConstants.KEY_PREF_INTRO, false)) {
            startActivity(new Intent(this, IntroActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        mMessageReceiver = new MessageReceiver();
        mMessageReceiverTracker = new TrackerReceiver();

        mToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackgroundColor(ControlHelper.getColor(this, android.R.attr.colorPrimaryDark));

        FragmentManager fm = getSupportFragmentManager();
        mMapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        mMapFragment.getUndoRedoOverlay().setTopToolbar(mToolbar);
        mMapFragment.getEditLayerOverlay().setTopToolbar(mToolbar);
        mMapFragment.getEditLayerOverlay().setBottomToolbar(getBottomToolbar());

        MainApplication app = (MainApplication) getApplication();
        mLayersFragment = (LayersFragment) fm.findFragmentById(R.id.layers);

        if (mLayersFragment != null && null != mLayersFragment.getView()) {
            mLayersFragment.getView().setBackgroundColor(ContextCompat.getColor(this, com.nextgis.maplibui.R.color.color_grey_050));
            // Set up the drawer.
            mLayersFragment.setUp(R.id.layers, drawerLayout, (MapDrawable) app.getMap());
        }

        LayerFillProgressDialogFragment progressFragment = (LayerFillProgressDialogFragment) fm.findFragmentByTag(TAG_FRAGMENT_PROGRESS);
        if (progressFragment == null) {
            progressFragment = new LayerFillProgressDialogFragment();
            fm.beginTransaction().add(progressFragment, TAG_FRAGMENT_PROGRESS).commit();
        }


        if (!hasLocationPermissions()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    processAllPermisions(PERMISSIONS_REQUEST_ZERO);
                }
            }, 1500);

        }
//        if (!hasLocationPermissions()) {
//            List<String> permslist = new ArrayList<>();
//            permslist.add(Manifest.permission.ACCESS_COARSE_LOCATION);
//            permslist.add(Manifest.permission.ACCESS_FINE_LOCATION);
////            permslist.add(Manifest.permission.GET_ACCOUNTS);
////            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
////                permslist.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
////
////            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2)
////                permslist.add(Manifest.permission.POST_NOTIFICATIONS);
//
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    requestPermissions(R.string.permissions, R.string.location_permissions, PERMISSIONS_REQUEST_LOC, permslist.toArray(new String[permslist.size()])); // list.toArray(new Foo[list.size()])
//                }
//            }, 5000);
//
//        }

        NGIDUtils.get(this, new NGIDUtils.OnFinish() {
            @Override
            public void onFinish(HttpResponse response) {
                if (response.isOk()) {
                    File support = getExternalFilesDir(null);
                    if (support == null)
                        support = new File(getFilesDir(), SUPPORT);
                    else
                        support = new File(support, SUPPORT);

                    try {
                        String jsonString = FileUtil.readFromFile(support);
                        JSONObject json = new JSONObject(jsonString);
                        if (json.optBoolean(JSON_SUPPORTED_KEY)) {
                            final String id = json.getString(JSON_USER_ID_KEY);
                            NetworkUtil.setUserNGUID(id);
                        }
                    }catch (Exception exception){
                    }

                    try {
                        FileUtil.writeToFile(support, response.getResponseBody());
                    } catch (IOException ignored) {}

                    NetworkUtil.setIsPro(AccountUtil.isProUser(getBaseContext()));
                }

                if (mMapFragment.getEditLayerOverlay().getMode() == EditLayerOverlay.MODE_NONE)
                    mToolbar.setTitle(getAppName());
//                boolean isLoggedIn = !TextUtils.isEmpty(mPreferences.getString(NGIDUtils.PREF_ACCESS_TOKEN, ""));
//                if (!isLoggedIn)
//                    showSnack();
            }
        });
    }

    private void showSnack() {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.mainview), getString(R.string.support_available), Snackbar.LENGTH_LONG)
                                    .setAction(R.string.more, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent pricing = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.pricing)));
                                            startActivity(pricing);
                                        }
                                    });

        View view = snackbar.getView();
        TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(view.getContext(), com.nextgis.maplibui.R.color.color_white));
        snackbar.show();
    }

    public void processAllPermisions(int startlevel){
        if (startlevel == PERMISSIONS_REQUEST_ZERO){
            if (!hasLocationPermissions()) {
                List<String> permslist = new ArrayList<>();
                permslist.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                permslist.add(Manifest.permission.ACCESS_FINE_LOCATION);
                requestPermissions(this, R.string.permissions, R.string.location_permissions, PERMISSIONS_REQUEST_LOC,
                        PERMISSIONS_REQUEST_LOC,
                        permslist.toArray(new String[permslist.size()])); // list.toArray(new Foo[list.size()])
                return;
            } else processAllPermisions(PERMISSIONS_REQUEST_LOC);
            return;
        }
        if (startlevel == PERMISSIONS_REQUEST_LOC){
            if (!hasAccountCreatePermissions()) {
                List<String> permslist = new ArrayList<>();
                permslist.add(Manifest.permission.GET_ACCOUNTS);
                requestPermissions(this, R.string.permissions, R.string.account_permissions, PERMISSIONS_REQUEST_ACCOUNT,
                        -1, permslist.toArray(new String[permslist.size()])); // list.toArray(new Foo[list.size()])
            } else
                processAllPermisions(PERMISSIONS_REQUEST_ACCOUNT);
            return;
        }
        if (startlevel == PERMISSIONS_REQUEST_ACCOUNT){
            if (!hasSDCARDWritePermissions()) {
                List<String> permslist = new ArrayList<>();
                permslist.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                requestPermissions(this, R.string.permissions, R.string.memory_permissions, PERMISSIONS_REQUEST_MEMORY,
                        PERMISSIONS_REQUEST_MEMORY, permslist.toArray(new String[permslist.size()])); // list.toArray(new Foo[list.size()])
            } else
                processAllPermisions(PERMISSIONS_REQUEST_MEMORY);
            return;
        }
        if (startlevel == PERMISSIONS_REQUEST_MEMORY){
            if (!hasNotifyPermissions()) {
                List<String> permslist = new ArrayList<>();
                permslist.add(Manifest.permission.POST_NOTIFICATIONS);
                requestPermissions(this, R.string.permissions, R.string.push_permissions, PERMISSIONS_REQUEST_PUSH,
                        -1,
                        permslist.toArray(new String[permslist.size()])); // list.toArray(new Foo[list.size()])
            }
        }
    }

    protected boolean hasLocationPermissions() {
        boolean permissions =
                isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissions;
    }

    protected boolean hasAccountCreatePermissions() {
        return isPermissionGranted(Manifest.permission.GET_ACCOUNTS);
    }

    protected boolean hasSDCARDWritePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            return isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        else
            return true;
    }

    protected boolean hasNotifyPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
            return isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS);
        else return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ZERO_LOCATION_POSPONDED:
                if (hasLocationPermissions()) {
                    MenuItem item = null;
                    try{
                        item = mToolbar.getMenu().findItem(R.id.menu_track);
                    } catch (Exception ex){
                    }
                    askBackgroundPerm(item);
                }
                break;
            case PERMISSIONS_REQUEST_LOC:
                mMapFragment.restartGpsListener();
                processAllPermisions(PERMISSIONS_REQUEST_LOC);
                break;
            case PERMISSIONS_REQUEST_ACCOUNT:
                processAllPermisions(PERMISSIONS_REQUEST_ACCOUNT);
                break;

            case PERMISSIONS_REQUEST_MEMORY:
                processAllPermisions(PERMISSIONS_REQUEST_MEMORY);
                break;

            case PERMISSIONS_REQUEST_PUSH:
                // turn on sync notify
                if (permissions.length >0)
                    for (int i = 0; i<permissions.length; i++){
                        if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)
                                && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                            android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
                                    edit()
                                    .putBoolean(AppSettingsConstants.KEY_PREF_SHOW_SYNC, true)
                                    .commit();
                        }
                    }
                break;

            case LOCATION_BACKGROUND_REQUEST:
                if (mTrackItem != null) {
                    controlTrack(mTrackItem);
                    mTrackItem = null;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void showEditToolbar() {
        stopRefresh(mToolbar.getMenu().findItem(R.id.menu_refresh));
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(com.nextgis.maplibui.R.menu.edit_geometry);

        MenuItem item = mToolbar.getMenu().findItem(com.nextgis.maplibui.R.id.menu_edit_redo);
        boolean visible = mMapFragment.getMode() != MapFragment.MODE_EDIT_BY_WALK;
        item.setVisible(visible);
        item = mToolbar.getMenu().findItem(com.nextgis.maplibui.R.id.menu_edit_undo);
        item.setVisible(visible);

        mLayersFragment.setDrawerToggleEnabled(false);
        mToolbar.setNavigationIcon(com.nextgis.maplibui.R.drawable.ic_action_cancel_dark);
    }


    public void showDefaultToolbar() {
        mToolbar.setTitle(getAppName());
        mToolbar.setSubtitle(null);
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(R.menu.main);
        mLayersFragment.setDrawerToggleEnabled(true);
        mLayersFragment.syncState();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (null != mLayersFragment && !mLayersFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            //restoreActionBar();

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    public BottomToolbar getBottomToolbar() {
        return (BottomToolbar) findViewById(com.nextgis.maplibui.R.id.bottom_toolbar);
    }


    private void controlTrack(MenuItem item) {
        if (item != null) {
            Pair<Integer, Integer> iconAndTitle = TrackerService.controlAndGetIconWithTitle(this);
            setTrackItem(item, iconAndTitle.second, iconAndTitle.first);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        final IGISApplication app = (IGISApplication) getApplication();

        switch (item.getItemId()) {
            case android.R.id.home:
                if (hasFragments())
                    return finishFragment();
                else if (mMapFragment.isEditMode())
                    return mMapFragment.onOptionsItemSelected(item.getItemId());
                else {
                    mLayersFragment.toggle();
                    return true;
                }
            case R.id.menu_settings:
                app.showSettings(SettingsConstantsUI.ACTION_PREFS_GENERAL, RELOAD_ACTIVITY_DATA, this);
                return true;
            case R.id.menu_about:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                return true;
            case R.id.menu_locate:
                locateCurrentPosition();
                return true;
            case R.id.menu_track:
               askBackgroundPerm(item);
                return true;
            case R.id.menu_refresh:
                if (null != mMapFragment) {
                    mMapFragment.refresh();
                }
                return true;
            case com.nextgis.maplibui.R.id.menu_edit_save:
                return mMapFragment.saveEdits();
            case com.nextgis.maplibui.R.id.menu_edit_undo:
            case com.nextgis.maplibui.R.id.menu_edit_redo:
                return mMapFragment.onOptionsItemSelected(item.getItemId());
            case R.id.menu_share_log:
                shareLog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
            /*case R.id.menu_test:
                //testAttachInsert();
                //testAttachUpdate();
                //testAttachDelete();
                new Thread() {
                    @Override
                    public void run() {
                        testSync();
                    }
                }.start();
                return true;*/
        }
    }

    private void askBackgroundPerm(final MenuItem item){
        TrackerService.showBackgroundDialog(this, new TrackerService.BackgroundPermissionCallback() {
            @Override
            public void beforeAndroid10(boolean hasBackgroundPermission) {
                if (!hasBackgroundPermission) {
                    mTrackItem = item;
                    String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
                    requestPermissions(MainActivity.this, R.string.permissions, R.string.location_permissions, LOCATION_BACKGROUND_REQUEST,
                            -1, permissions);
                } else {
                    controlTrack(item);
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onAndroid10(boolean hasBackgroundPermission) {
                if (!hasBackgroundPermission) {
                    mTrackItem = item;
                    requestbackgroundLocationPermissions();
                } else {
                    controlTrack(item);
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void afterAndroid10(boolean hasBackgroundPermission) {
                if (!hasBackgroundPermission) {
                    mTrackItem = item;
                    requestbackgroundLocationPermissions();
                } else {
                    controlTrack(item);
                }
            }
        });

    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void requestbackgroundLocationPermissions() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, LOCATION_BACKGROUND_REQUEST);
    }


    private void shareLog() {
        HyperLog.getDeviceLogsInFile(this);
        File dir = new File(getExternalFilesDir(null), "LogFiles");
        long size = FileUtil.getDirectorySize(dir);
        if (size == 0L) {
            Toast.makeText(this, com.nextgis.maplib.R.string.error_empty_dataset, Toast.LENGTH_LONG).show();
            return;
        }

        File files = zipLogs(dir);
        String type = "text/plain";
        UiUtil.share(files, type, this, true );
    }

    private File zipLogs(File dir) {
        File temp = MapUtil.prepareTempDir(this, "shared_layers", false);
        ArrayList<File> outdated = new ArrayList<>();
        try {
            String fileName = "ng-logs.zip";
            if (temp == null) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setMessage(com.nextgis.maplibui.R.string.error_file_create)
                        .setPositiveButton(com.nextgis.maplibui.R.string.ok, null)
                        .create()
                        .show();
                //Toast.makeText(this, R.string.error_file_create, Toast.LENGTH_LONG).show();
            }

            temp = new File(temp, fileName);
            temp.createNewFile();
            FileOutputStream fos = new FileOutputStream(temp, false);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            byte[] buffer = new byte[1024];
            int length;

            for (File file: dir.listFiles()) {
                if (System.currentTimeMillis() - file.lastModified() > 60 * 60 * 1000)
                    outdated.add(file);
                try {
                    FileInputStream fis = new FileInputStream(file);
                    zos.putNextEntry(new ZipEntry(file.getName()));

                    while ((length = fis.read(buffer)) > 0)
                        zos.write(buffer, 0, length);

                    zos.closeEntry();
                    fis.close();
                } catch (Exception ignored) {}
            }

            zos.close();
            fos.close();
        } catch (IOException ignored) {
            temp = null;
        }
        for (File file: outdated) {
            file.delete();
        }
        return temp;
    }

    private void setTrackItem(MenuItem item, int title, int icon) {
        if (null != item) {
            item.setTitle(title);
            item.setIcon(icon);
        }
    }


    public boolean hasFragments() {
        return getSupportFragmentManager().getBackStackEntryCount() > 0;
    }


    public boolean finishFragment()
    {
        if (hasFragments()) {
            getSupportFragmentManager().popBackStack();
            setActionBarState(true);
            return true;
        }

        return false;
    }


    @Override
    protected boolean isHomeEnabled()
    {
        return false;
    }


    public synchronized void onRefresh(boolean isRefresh) {
        MenuItem refreshItem = mToolbar.getMenu().findItem(R.id.menu_refresh);
        if (null != refreshItem) {
            if (isRefresh) {
                if (refreshItem.getActionView() == null) {
                    refreshItem.setActionView(R.layout.layout_refresh);
                    ProgressBar progress = refreshItem.getActionView().findViewById(R.id.refreshingProgress);
                    if (progress != null)
                        progress.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this,
                                com.nextgis.maplibui.R.color.color_grey_200), PorterDuff.Mode.SRC_IN);
                }
            } else
                stopRefresh(refreshItem);
        }
    }

    protected void stopRefresh(final MenuItem refreshItem) {
        Handler handler = new Handler(Looper.getMainLooper());
        final Runnable r = new Runnable() {
            public void run() {
                if (refreshItem != null && refreshItem.getActionView() != null) {
                    refreshItem.getActionView().clearAnimation();
                    refreshItem.setActionView(null);
                }
            }
        };
        handler.post(r);
    }


    public void addLocalLayer()
    {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        // https://developer.android.com/guide/topics/providers/document-provider.html#client
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        }
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.select_file)),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            //TODO: open select local resource dialog
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(
                    this, getString(R.string.warning_install_file_manager), Toast.LENGTH_SHORT)
                    .show();
        }
    }


    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data)
    {
        //http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
        //http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult/18345899
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    if(Constants.DEBUG_MODE)
                        Log.d(TAG, "File Uri: " + (uri != null ? uri.toString() : ""));
                    //check the file type from extension
                    String fileName = FileUtil.getFileNameByUri(this, uri, "");
                    if (fileName.toLowerCase().endsWith("ngrc") ||
                            fileName.toLowerCase().endsWith("zip")) { //create local tile layer
                        if (null != mMapFragment) {
                            mMapFragment.addLocalTMSLayer(uri);
                        }
                    } else if (fileName.toLowerCase().endsWith("geojson")) { //create local vector layer
                        if (null != mMapFragment) {
                            mMapFragment.addLocalVectorLayer(uri);
                        }
                    } else if (fileName.toLowerCase().endsWith("ngfp")) { //create local vector layer with form
                        if (null != mMapFragment) {
                            mMapFragment.addLocalVectorLayerWithForm(uri);
                        }
                    } else {
                        Toast.makeText(
                                this, getString(R.string.error_file_unsupported),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case IVectorLayerUI.MODIFY_REQUEST:
                mMapFragment.onActivityResult(requestCode, resultCode, data);
                break;
            case RELOAD_ACTIVITY_DATA:
                if (resultCode == RESULT_OK) {
                    finish();
                    Intent intent = new Intent(this, this.getClass());
                    startActivity(intent);
                }
                break;
        }
    }


    public MapFragment getMapFragment() {
        return mMapFragment;
    }

    protected void locateCurrentPosition()
    {
        if (!hasLocationPermissions()){
            List<String> permslist = new ArrayList<>();
            permslist.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permslist.add(Manifest.permission.ACCESS_FINE_LOCATION);
            requestPermissions(this, R.string.permissions, R.string.location_permissions, PERMISSIONS_REQUEST_LOC_SILENT,
                    PERMISSIONS_REQUEST_LOC_SILENT,
                    permslist.toArray(new String[permslist.size()])); //
        }

        if (null != mMapFragment) {
            mMapFragment.locateCurrentPosition();
        }
    }


    void testSync()
    {
        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer;
        for (int i = 0; i < map.getLayerCount(); i++) {
            ILayer layer = map.getLayer(i);
            if (layer instanceof NGWVectorLayer) {
                ngwVectorLayer = (NGWVectorLayer) layer;
                Pair<Integer, Integer> ver = NGWUtil.getNgwVersion(this, ngwVectorLayer.getAccountName());
                ngwVectorLayer.sync(application.getAuthority(), ver, new SyncResult());
            }
        }
    }


    void testUpdate()
    {
        //test sync
        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for (int i = 0; i < map.getLayerCount(); i++) {
            ILayer layer = map.getLayer(i);
            if (layer instanceof NGWVectorLayer) {
                ngwVectorLayer = (NGWVectorLayer) layer;
            }
        }
        if (null != ngwVectorLayer) {
            Uri uri = Uri.parse(
                    "content://" + AppSettingsConstants.AUTHORITY + "/" +
                    ngwVectorLayer.getPath().getName());
            Uri updateUri = ContentUris.withAppendedId(uri, 29);
            ContentValues values = new ContentValues();
            values.put("width", 4);
            values.put("azimuth", 8.0);
            values.put("status", "test4");
            values.put("temperatur", -10);
            values.put("name", "xxx");

            Calendar calendar = new GregorianCalendar(2014, Calendar.JANUARY, 23);
            values.put("datetime", calendar.getTimeInMillis());
            try {
                GeoPoint pt = new GeoPoint(67, 65);
                pt.setCRS(CRS_WGS84);
                pt.project(CRS_WEB_MERCATOR);
                GeoMultiPoint mpt = new GeoMultiPoint();
                mpt.add(pt);
                values.put(Constants.FIELD_GEOM, mpt.toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
            int result = getContentResolver().update(updateUri, values, null, null);
            if(Constants.DEBUG_MODE){
                if (result == 0) {
                    Log.d(TAG, "update failed");
                } else {
                    Log.d(TAG, "" + result);
                }
            }
        }
    }


    void testAttachUpdate()
    {
        IGISApplication application = (IGISApplication) getApplication();
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri updateUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" +
                                      ngwVectorLayer.getPath().getName() + "/36/attach/1000");
        */
        Uri updateUri = Uri.parse(
                "content://" + AppSettingsConstants.AUTHORITY +
                "/layer_20150210140455993/36/attach/2");

        ContentValues values = new ContentValues();
        values.put(VectorLayer.ATTACH_DISPLAY_NAME, "no_image.jpg");
        values.put(VectorLayer.ATTACH_DESCRIPTION, "simple update description");
        //    values.put(VectorLayer.ATTACH_ID, 999);
        int result = getContentResolver().update(updateUri, values, null, null);
        if(Constants.DEBUG_MODE){
            if (result == 0) {
                Log.d(TAG, "update failed");
            } else {
                Log.d(TAG, "" + result);
            }
        }
        //}
    }


    void testAttachDelete()
    {
        IGISApplication application = (IGISApplication) getApplication();
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri deleteUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" +
                                ngwVectorLayer.getPath().getName() + "/36/attach/1000");
        */
        Uri deleteUri = Uri.parse(
                "content://" + AppSettingsConstants.AUTHORITY +
                        "/layer_20150210140455993/36/attach/1");
        int result = getContentResolver().delete(deleteUri, null, null);
        if(Constants.DEBUG_MODE){
            if (result == 0) {
                Log.d(TAG, "delete failed");
            } else {
                Log.d(TAG, "" + result);
            }
        }
        //}
    }


    void testAttachInsert()
    {
        IGISApplication application = (IGISApplication) getApplication();
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri uri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" + ngwVectorLayer.getPath().getName() + "/36/attach");
        */
        Uri uri = Uri.parse(
                "content://" + AppSettingsConstants.AUTHORITY + "/layer_20150210140455993/36/attach");
        ContentValues values = new ContentValues();
        values.put(VectorLayer.ATTACH_DISPLAY_NAME, "test_image.jpg");
        values.put(VectorLayer.ATTACH_MIME_TYPE, "image/jpeg");
        values.put(VectorLayer.ATTACH_DESCRIPTION, "test image description");

        Uri result = getContentResolver().insert(uri, values);
        if (result == null) {
            Log.d(TAG, "insert failed");
        } else {
            try {
                OutputStream outStream = getContentResolver().openOutputStream(result);
                Bitmap sourceBitmap = BitmapFactory.decodeResource(
                        getResources(), com.nextgis.maplibui.R.drawable.bk_tile);
                sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream);
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(Constants.DEBUG_MODE)
                Log.d(TAG, result.toString());
        }
        //}
    }


    void testInsert()
    {
        //test sync
        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for (int i = 0; i < map.getLayerCount(); i++) {
            ILayer layer = map.getLayer(i);
            if (layer instanceof NGWVectorLayer) {
                ngwVectorLayer = (NGWVectorLayer) layer;
            }
        }
        if (null != ngwVectorLayer) {
            Uri uri = Uri.parse(
                    "content://" + AppSettingsConstants.AUTHORITY + "/" +
                            ngwVectorLayer.getPath().getName());
            ContentValues values = new ContentValues();
            //values.put(VectorLayer.FIELD_ID, 26);
            values.put("width", 1);
            values.put("azimuth", 2.0);
            values.put("status", "grot");
            values.put("temperatur", -13);
            values.put("name", "get");

            Calendar calendar = new GregorianCalendar(2015, Calendar.JANUARY, 23);
            values.put("datetime", calendar.getTimeInMillis());

            try {
                GeoPoint pt = new GeoPoint(37, 55);
                pt.setCRS(CRS_WGS84);
                pt.project(CRS_WEB_MERCATOR);
                GeoMultiPoint mpt = new GeoMultiPoint();
                mpt.add(pt);
                values.put(Constants.FIELD_GEOM, mpt.toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Uri result = getContentResolver().insert(uri, values);
            if(Constants.DEBUG_MODE){
                if (result == null) {
                    Log.d(TAG, "insert failed");
                } else {
                    Log.d(TAG, result.toString());
                }
            }
        }
    }


    void testDelete()
    {
        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for (int i = 0; i < map.getLayerCount(); i++) {
            ILayer layer = map.getLayer(i);
            if (layer instanceof NGWVectorLayer) {
                ngwVectorLayer = (NGWVectorLayer) layer;
            }
        }
        if (null != ngwVectorLayer) {
            Uri uri = Uri.parse(
                    "content://" + AppSettingsConstants.AUTHORITY + "/" +
                    ngwVectorLayer.getPath().getName());
            Uri deleteUri = ContentUris.withAppendedId(uri, 27);
            int result = getContentResolver().delete(deleteUri, null, null);
            if(Constants.DEBUG_MODE){
                if (result == 0) {
                    Log.d(TAG, "delete failed");
                } else {
                    Log.d(TAG, "" + result);
                }
            }
        }
    }


    public void addNGWLayer()
    {
        if (null != mMapFragment) {
            mMapFragment.addNGWLayer();
        }
    }


    public void addRemoteLayer()
    {
        if (null != mMapFragment) {
            mMapFragment.addRemoteLayer();
        }
    }


    @Override
    public void onFinishChooseLayerDialog(
            int code,
            ILayer layer)
    {
        if (null != mMapFragment) {
            mMapFragment.onFinishChooseLayerDialog(code, layer);
        }
    }


    protected class MessageReceiverTracker extends BroadcastReceiver
    {
        @Override
        public void onReceive(
                Context context,
                Intent intent){

            if (intent.getAction().equals(ConstantsUI.MESSAGE_INTENT_TRACKER)) {
                //Log.e("ZZXX", "intent.getAction().equals(MESSAGE_INTENT_TRACKER");
                String message = intent.getExtras().getString(MESSAGE_EXTRA);
                String title = intent.getExtras().getString(MESSAGE_TITLE_EXTRA);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(message)
                        .setPositiveButton("ok", null)
                        .setTitle(title);
                AlertDialog alertDialog=builder.create();
                alertDialog.show();
            }
        }
    }

    protected class MessageReceiver
            extends BroadcastReceiver
    {
        @Override
        public void onReceive(
                Context context,
                Intent intent)
        {
            if (intent.getAction().equals(ConstantsUI.MESSAGE_INTENT)) {
                //Log.e("ZZXX", "intent.getAction().equals(ConstantsUI.MESSAGE_INTENT)");
                Toast.makeText(
                        MainActivity.this, intent.getExtras().getString(
                                ConstantsUI.KEY_MESSAGE), Toast.LENGTH_SHORT).show();

            }

            if (intent.getAction().equals(MESSAGE_ALERT_INTENT)) {
                //Log.e("ZZXX", "intent.getAction().equals(MESSAGE_ALERT_INTENT");
                String message = intent.getExtras().getString(MESSAGE_EXTRA);
                String title = intent.getExtras().getString(MESSAGE_TITLE_EXTRA);

                final SpannableString s = new SpannableString(message); // msg should have url to enable clicking
                Linkify.addLinks(s, Linkify.ALL);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(s)
                        .setPositiveButton("ok", null)
                        .setTitle(title);
                AlertDialog alertDialog=builder.create();
                alertDialog.show();

                ((TextView)alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        mToolbar.getBackground().setAlpha(128);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConstantsUI.MESSAGE_INTENT);
        intentFilter.addAction(MESSAGE_ALERT_INTENT);

        IntentFilter intentFilterTracker = new IntentFilter();
        intentFilter.addAction("com.example.ACTION_TRACKER_MESSAGE");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mMessageReceiver, intentFilter, Context.RECEIVER_EXPORTED);
            registerReceiver(mMessageReceiverTracker, intentFilterTracker, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mMessageReceiver, intentFilter);
            registerReceiver(mMessageReceiverTracker, intentFilterTracker);
        }


        if (SDCardUtils.isSDCardUsedAndExtracted(this)){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(com.nextgis.maplibui.R.string.no_sd_card_attention)
                    .setPositiveButton(com.nextgis.maplibui.R.string.ok, null)
                    .setTitle(com.nextgis.maplibui.R.string.sd_card);
            AlertDialog alertDialog=builder.create();
            alertDialog.show();
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (null != mLayersFragment && !mLayersFragment.isDrawerOpen()) {
            boolean hasUnfinishedTracks = hasUnfinishedTracks(this);
            int title = hasUnfinishedTracks ? com.nextgis.maplibui.R.string.track_stop : com.nextgis.maplibui.R.string.track_start;
            int icon = hasUnfinishedTracks ? com.nextgis.maplibui.R.drawable.ic_action_maps_directions_walk_rec : com.nextgis.maplibui.R.drawable.ic_action_maps_directions_walk;
            setTrackItem(menu.findItem(R.id.menu_track), title, icon);
        }

        if (mMapFragment.isEditMode())
            showEditToolbar();

        MenuItem log = menu.findItem(R.id.menu_share_log);
        if (log != null) {
            log.setVisible(mPreferences.getBoolean("save_log", false));
        }

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected void onPause()
    {
        try {
            if (mMessageReceiver != null) {
                unregisterReceiver(mMessageReceiver);
                mMessageReceiver = null;
            }
            if (mMessageReceiverTracker != null) {
                unregisterReceiver(mMessageReceiverTracker);
                mMessageReceiverTracker = null;
            }
        } catch (Exception ignored) { }

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (finishFragment())
            return;

        if (mBackPressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(this, R.string.press_aback_again, Toast.LENGTH_SHORT).show();

        mBackPressed = System.currentTimeMillis();
    }

    @Override
    public void onLocationChanged(Location location)
    {

    }

    @Override
    public void onBestLocationChanged(Location location) {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }


    public void setActionBarState(boolean state)
    {
        mLayersFragment.setDrawerToggleEnabled(state);

        if (state) {
            mToolbar.getBackground().setAlpha(128);
            getBottomToolbar().getBackground().setAlpha(128);
        } else {
            mToolbar.getBackground().setAlpha(255);
            getBottomToolbar().getBackground().setAlpha(255);
        }
    }


    public void hideBottomBar() {
        mMapFragment.hideBottomBar();
    }


    public void restoreBottomBar(int mode)
    {
        if (mMapFragment.isAdded())
            mMapFragment.restoreBottomBar(mode);
    }

    public void setSubtitle(String subtitle) {
        mToolbar.setSubtitle(subtitle);
    }

    public void requestPermissions(final Activity activity1, int title, int message, final int requestCode,
                                          int nextLevelOndeny,
                                          final String... permissions) {
        final Activity activity = activity1;
        if (true) {
            androidx.appcompat.app.AlertDialog builder = new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(com.nextgis.maplibui.R.string.allow, (dialog, which) -> {
                        ActivityCompat.requestPermissions(activity, permissions, requestCode);})
                    .setNegativeButton(com.nextgis.maplibui.R.string.deny, (dialog, which) -> {
                        if (nextLevelOndeny >0)
                            processAllPermisions(nextLevelOndeny);
                    })
                    .create();
            builder.setCanceledOnTouchOutside(false);
            builder.show();
        }
    }
}
