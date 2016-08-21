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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
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

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.INGWLayer;
import com.nextgis.maplib.datasource.ngw.SyncAdapter;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.fragment.LayersListAdapter;
import com.nextgis.maplibui.fragment.ReorderedLayerView;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.CreateVectorLayerActivity;
import com.nextgis.mobile.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

import static com.nextgis.maplib.util.Constants.NGW_ACCOUNT_TYPE;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.mobile.util.SettingsConstants.AUTHORITY;


/**
 * A layers fragment class
 */
public class LayersFragment
        extends Fragment implements View.OnClickListener {
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

        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.action_space);
        if (null != linearLayout) {
            linearLayout.setBackgroundColor(ControlHelper.getColor(view.getContext(), R.attr.colorPrimary));
        }

        mSyncButton = (ImageButton) view.findViewById(R.id.sync);
        mNewLayer = (ImageButton) view.findViewById(R.id.new_layer);
        mNewLayer.setOnClickListener(this);
        mInfoText = (TextView) view.findViewById(R.id.info);

        setupSyncOptions();

        updateInfo();
        return view;
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.add_layer, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new:
                Intent intentNewLayer = new Intent(getActivity(), CreateVectorLayerActivity.class);
                startActivity(intentNewLayer);
                return true;
            case R.id.menu_add_local:
                ((MainActivity) getActivity()).addLocalLayer();
                return true;
            case R.id.menu_add_remote:
                ((MainActivity) getActivity()).addRemoteLayer();
                return true;
            case R.id.menu_add_ngw:
                ((MainActivity) getActivity()).addNGWLayer();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    protected void setupSyncOptions()
    {
        mAccounts.clear();
        final AccountManager accountManager = AccountManager.get(getActivity().getApplicationContext());
        Log.d(TAG, "LayersFragment: AccountManager.get(" + getActivity().getApplicationContext() + ")");
        final IGISApplication application = (IGISApplication) getActivity().getApplication();
        List<INGWLayer> layers = new ArrayList<>();

        for (Account account : accountManager.getAccountsByType(NGW_ACCOUNT_TYPE)) {
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


    protected void updateInfo()
    {
        if (null == mInfoText) {
            return;
        }

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                Constants.PREFERENCES, Constants.MODE_MULTI_PROCESS);
        long timeStamp =
                sharedPreferences.getLong(SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP, 0);
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
        registerForContextMenu(mNewLayer);
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
        MainActivity activity = (MainActivity) getActivity();
        mFragmentContainerView = activity.findViewById(fragmentId);

        Display display = activity.getWindowManager().getDefaultDisplay();

        int displayWidth;
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            displayWidth = size.x;
        } else {
            displayWidth = display.getWidth();
        }

        ViewGroup.LayoutParams params = mFragmentContainerView.getLayoutParams();
        if (params.width >= displayWidth) {
            params.width = (int) (displayWidth * 0.8);
        }
        mFragmentContainerView.setLayoutParams(params);

        final MapFragment mapFragment = activity.getMapFragment();
        mListAdapter = new LayersListAdapter(activity, map);
        mListAdapter.setDrawer(drawerLayout);
        mListAdapter.setOnPencilClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mapFragment.hasEdits()) {
                    mapFragment.setMode(MapFragment.MODE_NORMAL);
                    return;
                }

                AlertDialog builder = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.save)
                        .setMessage(R.string.has_edits)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mapFragment.saveEdits();
                                mapFragment.setMode(MapFragment.MODE_NORMAL);
                            }
                        })
                        .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
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

        mLayersListView = (ReorderedLayerView) mFragmentContainerView.findViewById(R.id.layer_list);
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
                R.string.layers_drawer_open,
                // "open drawer" description for accessibility
                R.string.layers_drawer_close
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
        getActivity().registerReceiver(mSyncReceiver, intentFilter);
    }


    @Override
    public void onPause()
    {
        getActivity().unregisterReceiver(mSyncReceiver);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sync:
                for (Account account : mAccounts) {
                    Bundle settingsBundle = new Bundle();
                    settingsBundle.putBoolean(
                            ContentResolver.SYNC_EXTRAS_MANUAL, true);
                    settingsBundle.putBoolean(
                            ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                    ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
                }

                updateInfo();
                break;
            case R.id.new_layer:
                mNewLayer.showContextMenu();
                break;
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
            } else if (intent.getAction().equals(SyncAdapter.SYNC_FINISH) ||
                    intent.getAction().equals(SyncAdapter.SYNC_CANCELED)) {
                refresh(false);
                updateInfo();
            }
        }
    }
}
