/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2014 NextGIS
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

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nextgis.maplibui.LayersListAdapter;
import com.nextgis.maplibui.MapView;

/**
 * A layers fragment class
 */
public class LayersFragment extends Fragment {
	protected ActionBarDrawerToggle mDrawerToggle;
    protected DrawerLayout mDrawerLayout;
    protected ListView mLayersListView;
    protected View mFragmentContainerView;
    protected LayersListAdapter mListAdapter;
	protected MapView mMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.layersfragment, container, false);
	}


    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);

        Display display = getActivity().getWindowManager().getDefaultDisplay();

        int displayWidth;
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            displayWidth = size.x;
        } else {
            displayWidth = display.getWidth();
        }

        ViewGroup.LayoutParams params = mFragmentContainerView.getLayoutParams();
        if(params.width >= displayWidth)
            params.width = (int) (displayWidth * 0.8);
        mFragmentContainerView.setLayoutParams(params);

        mLayersListView = (ListView) mFragmentContainerView.findViewById(R.id.layer_list);
        mListAdapter = new LayersListAdapter(getActivity(), mMap.getMap());
        mLayersListView.setAdapter(mListAdapter);

        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    // host Activity
                mDrawerLayout,                    // DrawerLayout object
//                R.drawable.ic_drawer,             // nav drawer image to replace 'Up' caret
                R.string.layers_drawer_open,  // "open drawer" description for accessibility
                R.string.layers_drawer_close  // "close drawer" description for accessibility
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onInit(MapView map) {
        mMap = map;
        return true;
    }
}
