/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2014-2019 NextGIS, info@nextgis.com
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.fragment.LayersListAdapter;
import com.nextgis.maplibui.fragment.ReorderedLayerView;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.NGIDUtils;
import com.nextgis.maplibui.util.UiUtil;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.CreateVectorLayerActivity;
import com.nextgis.mobile.activity.MainActivity;
import com.nextgis.mobile.util.OfflineSyncIntentService;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_MULTI_PROCESS;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplibui.util.ConstantsUI.GA_CREATE;
import static com.nextgis.maplibui.util.ConstantsUI.GA_EDIT;
import static com.nextgis.maplibui.util.ConstantsUI.GA_GEOSERVICE;
import static com.nextgis.maplibui.util.ConstantsUI.GA_IMPORT;
import static com.nextgis.maplibui.util.ConstantsUI.GA_LAYER;
import static com.nextgis.maplibui.util.ConstantsUI.GA_LOCAL;
import static com.nextgis.maplibui.util.ConstantsUI.GA_MENU;
import static com.nextgis.maplibui.util.ConstantsUI.GA_NGW;
import static com.nextgis.maplibui.util.SettingsConstantsUI.KEY_PREF_OFFLINE_SYNC_ON;
import static com.nextgis.mobile.util.AppSettingsConstants.AUTHORITY;

/**
 * A layers fragment class
 */
public class LayersFragment
        extends Fragment implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    protected ActionBarDrawerToggle mDrawerToggle;
    protected DrawerLayout          mDrawerLayout;
    protected ReorderedLayerView    mLayersListView;
    protected View                  mFragmentContainerView;
    protected LayersListAdapter     mListAdapter;
    protected TextView              mInfoText;
    protected SyncReceiver          mSyncReceiver;
    protected ImageButton           mSyncButton;
    protected ImageButton           mNewLayer;
    protected List<Account>         mAccounts;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mSyncReceiver = new SyncReceiver();
        mAccounts = new ArrayList<>();
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_layers, container, false);

        LinearLayout linearLayout = view.findViewById(R.id.action_space);
        if (null != linearLayout) {
            linearLayout.setBackgroundColor(ControlHelper.getColor(view.getContext(), android.R.attr.colorPrimary));
        }

        mSyncButton = view.findViewById(R.id.sync);
        mNewLayer = view.findViewById(R.id.new_layer);
        mNewLayer.setOnClickListener(this);
        mInfoText = view.findViewById(R.id.info);

        setupSyncOptions();

        updateInfo();
        return view;
    }

    protected void setupSyncOptions()
    {
        mAccounts.clear();
        final AccountManager accountManager = AccountManager.get(getActivity().getApplicationContext());
        Log.d(TAG, "LayersFragment: AccountManager.get(" + getActivity().getApplicationContext() + ")");
        final IGISApplication application = (IGISApplication) getActivity().getApplication();
        List<INGWLayer> layers = new ArrayList<>();

        for (Account account : accountManager.getAccountsByType(application.getAccountsType())) {
            layers.clear();
            MapContentProviderHelper.getLayersByAccount(application.getMap(), account.name, layers);

            if (layers.size() > 0)
                mAccounts.add(account);
        }

        if (mAccounts.isEmpty()) {
            if (null != mSyncButton) {
                mSyncButton.setEnabled(false);
                mSyncButton.setVisibility(View.GONE);
            }
            if (null != mInfoText) {
                mInfoText.setVisibility(View.INVISIBLE);
            }
        } else {
            if (null != mSyncButton) {
                mSyncButton.setVisibility(View.VISIBLE);
                mSyncButton.setEnabled(true);
                mSyncButton.setOnClickListener(this);
            }
            if (null != mInfoText) {
                mInfoText.setVisibility(View.VISIBLE);
            }
        }
    }


    protected void updateInfo() {
        if (null == mInfoText || getContext() == null) {
            return;
        }

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(Constants.PREFERENCES, MODE_MULTI_PROCESS);
        long timeStamp = sharedPreferences.getLong(SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP, 0);
        if (timeStamp > 0) {
            mInfoText.setText(ControlHelper.getSyncTime(getContext(), timeStamp));
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }


    public boolean isDrawerOpen()
    {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }


    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId
     *         The android:id of this fragment in its activity's layout.
     * @param drawerLayout
     *         The DrawerLayout containing this fragment's UI.
     */
    public void setUp(
            int fragmentId,
            DrawerLayout drawerLayout,
            final MapDrawable map)
    {
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null)
            return;
        mFragmentContainerView = activity.findViewById(fragmentId);

        Display display = activity.getWindowManager().getDefaultDisplay();

        int displayWidth;
        Point size = new Point();
        display.getSize(size);
        displayWidth = size.x;

        ViewGroup.LayoutParams params = mFragmentContainerView.getLayoutParams();
        if (params.width >= displayWidth) {
            params.width = (int) (displayWidth * 0.8);
        }
        mFragmentContainerView.setLayoutParams(params);

        final MapFragment mapFragment = activity.getMapFragment();
        mListAdapter = new LayersListAdapter(activity, mapFragment.mMap);
        mListAdapter.setDrawer(drawerLayout);
        mListAdapter.setOnPencilClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mapFragment.hasEdits()) {
                    if (mapFragment.mFinishListener != null)
                        mapFragment.mFinishListener.onClick(null);
                    return;
                }

                AlertDialog builder = new AlertDialog.Builder(activity)
                        .setTitle(com.nextgis.maplibui.R.string.save)
                        .setMessage(com.nextgis.maplibui.R.string.has_edits)
                        .setPositiveButton(com.nextgis.maplibui.R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mapFragment.saveEdits();
                                mapFragment.setMode(MapFragment.MODE_NORMAL);
                            }
                        })
                        .setNegativeButton(com.nextgis.maplibui.R.string.discard, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mapFragment.cancelEdits();
                                mapFragment.setMode(MapFragment.MODE_NORMAL);
                            }
                        }).create();
                builder.show();
            }
        });
        mListAdapter.setOnLayerEditListener(new LayersListAdapter.onEdit() {
            @Override
            public void onLayerEdit(ILayer layer) {
                IGISApplication application = (IGISApplication) activity.getApplication();
                application.sendEvent(GA_LAYER, GA_EDIT, GA_MENU);
                mapFragment.onFinishChooseLayerDialog(MapFragment.EDIT_LAYER, layer);
                toggle();
            }
        });
        mapFragment.setOnModeChangeListener(new MapFragment.onModeChange() {
            @Override
            public void onModeChangeListener() {
                mListAdapter.notifyDataSetChanged();
            }
        });

        mLayersListView = mFragmentContainerView.findViewById(R.id.layer_list);
        mLayersListView.setAdapter(mListAdapter);
        mLayersListView.setDrawer(drawerLayout);

        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    // host Activity
                mDrawerLayout,// DrawerLayout object
//                R.drawable.ic_drawer,             // nav drawer image to replace 'Up' caret
                com.nextgis.maplibui.R.string.layers_drawer_open,
                // "open drawer" description for accessibility
                com.nextgis.maplibui.R.string.layers_drawer_close
                // "close drawer" description for accessibility
        )
        {
            @Override
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }


            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                setupSyncOptions();
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        syncState();
    }

    public void syncState() {
        mDrawerLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerToggle.syncState();
                        mDrawerLayout.setDrawerListener(mDrawerToggle);
                    }
                });
    }

    public void toggle() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else
            mDrawerLayout.openDrawer(GravityCompat.START);
    }

    public boolean isDrawerToggleEnabled() {
        return mDrawerToggle.isDrawerIndicatorEnabled();
    }

    public void setDrawerToggleEnabled(boolean state)
    {
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(state);

            if (state) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    public void refresh(boolean start)
    {
        if (mSyncButton == null) {
            return;
        }
        if (start) {
            RotateAnimation rotateAnimation = new RotateAnimation(
                    0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setFillAfter(true);
            rotateAnimation.setDuration(700);
            rotateAnimation.setRepeatCount(500);

            mSyncButton.startAnimation(rotateAnimation);
        } else {
            mSyncButton.clearAnimation();
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mListAdapter.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SyncAdapter.SYNC_START);
        intentFilter.addAction(SyncAdapter.SYNC_FINISH);
        intentFilter.addAction(SyncAdapter.SYNC_CANCELED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getActivity().registerReceiver(mSyncReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            getActivity().registerReceiver(mSyncReceiver, intentFilter);
        }
    }


    @Override
    public void onPause()
    {
        getActivity().unregisterReceiver(mSyncReceiver);
        super.onPause();
    }

    private void checkAccountForSync(final Context context, final Account account){
        boolean isYourAccountSyncEnabled = ContentResolver.getSyncAutomatically(account, "com.nextgis.mobile.provider");
        if (!isYourAccountSyncEnabled){
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ContentResolver.setSyncAutomatically(account, "com.nextgis.mobile.provider", true);

                }
            };
                new AlertDialog.Builder(context).setTitle(R.string.alert_sync_title)
                        .setMessage(R.string.alert_sync_turned_off)
                        .setPositiveButton(com.nextgis.maplibui.R.string.yes, onClickListener)
                        .setNegativeButton(com.nextgis.maplibui.R.string.cancel, null)
                        .create()
                        .show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sync:
                HyperLog.v(Constants.TAG, "onClick sync cliked!");

                final SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                String base = mPreferences.getString("ngid_url", NGIDUtils.NGID_MY);
                boolean offlineSync = mPreferences.getBoolean(KEY_PREF_OFFLINE_SYNC_ON, false);

                if (offlineSync || !NGIDUtils.NGID_MY.equals(base)){
                    HyperLog.v(Constants.TAG, "onClick start on-premise sync");
                    OfflineSyncIntentService.startActionFoo(v.getContext());
                } else {

                    final Runnable switchRunnable = new Runnable() {
                        @Override
                        public void run() {
                            final SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                            boolean offlineSync = mPreferences.getBoolean(KEY_PREF_OFFLINE_SYNC_ON, false);
                            if (!offlineSync)
                                mPreferences.edit().
                                        putBoolean(KEY_PREF_OFFLINE_SYNC_ON, true).
                                        apply();
                            OfflineSyncIntentService.startActionFoo(v.getContext());
                        }
                    };

                    GISApplication.getInstance().startRunnable(switchRunnable);

                    for (Account account : mAccounts) {
                        HyperLog.v(Constants.TAG, "onClick add sync to queue for " + account.name +" account");
                        // тут надо предупредить что нет включенной синхронизации
                        checkAccountForSync(v.getContext(), account);

                        Bundle settingsBundle = new Bundle();
                        settingsBundle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        settingsBundle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                        ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
                    }
                }

                updateInfo();
                break;
            case R.id.new_layer:
                if (getActivity() != null) {
//                    View view = getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
                    View view = getActivity().findViewById(R.id.new_layer);
                    PopupMenu popup = new PopupMenu(getActivity(), view);
                    UiUtil.setForceShowIcon(popup);
                    popup.getMenuInflater().inflate(R.menu.add_layer, popup.getMenu());
                    popup.setOnMenuItemClickListener(this);
                    if (!AccountUtil.isProUser(getActivity())) {
                        popup.getMenu().findItem(R.id.menu_add_ngw).setIcon(com.nextgis.maplibui.R.drawable.ic_lock_black_24dp);
                    }
                    popup.show();
                }

                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        IGISApplication application = (IGISApplication) getActivity().getApplication();
        switch (menuItem.getItemId()) {
            case R.id.menu_new:
                application.sendEvent(GA_LAYER, GA_CREATE, GA_LOCAL);
                Intent intentNewLayer = new Intent(getActivity(), CreateVectorLayerActivity.class);
                startActivity(intentNewLayer);
                return true;
            case R.id.menu_add_local:
                application.sendEvent(GA_LAYER, GA_CREATE, GA_IMPORT);
                ((MainActivity) getActivity()).addLocalLayer();
                return true;
            case R.id.menu_add_remote:
                application.sendEvent(GA_LAYER, GA_CREATE, GA_GEOSERVICE);
                ((MainActivity) getActivity()).addRemoteLayer();
                return true;
            case R.id.menu_add_ngw:
                if (!AccountUtil.isProUser(getActivity())) {
                    ControlHelper.showProDialog(getActivity());
                } else {
                    application.sendEvent(GA_LAYER, GA_CREATE, GA_NGW);
                    ((MainActivity) getActivity()).addNGWLayer();
                }
                return true;
            default:
                return super.onContextItemSelected(menuItem);
        }
    }

    protected class SyncReceiver
            extends BroadcastReceiver
    {

        @Override
        public void onReceive(
                Context context,
                Intent intent)
        {
            if (intent.getAction().equals(SyncAdapter.SYNC_START)) {
                refresh(true);
            } else if (intent.getAction().equals(SyncAdapter.SYNC_FINISH) || intent.getAction().equals(SyncAdapter.SYNC_CANCELED)) {
                if (intent.hasExtra(SyncAdapter.EXCEPTION))
                    Toast.makeText(getContext(), intent.getStringExtra(SyncAdapter.EXCEPTION), Toast.LENGTH_LONG).show();

                refresh(false);
                updateInfo();
            }
        }
    }
}
