/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.mobile.fragment;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.nextgis.maplibui.fragment.CompassFragment;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.MainActivity;
import com.nineoldandroids.view.ViewHelper;

public class FullCompassFragment extends CompassFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        ((MainActivity) getActivity()).hideBottomBar();

        View view = super.onCreateView(inflater, container, savedInstanceState);
        assert view != null;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(view.getLayoutParams());
        int[] attrs = {R.attr.actionBarSize};
        TypedArray ta = getActivity().obtainStyledAttributes(attrs);
        lp.topMargin = ta.getDimensionPixelSize(0, 0);
        ta.recycle();
        view.setLayoutParams(lp);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mIsVibrationOn = prefs.getBoolean(SettingsConstantsUI.KEY_PREF_COMPASS_VIBRATE, true);
        mParent.setKeepScreenOn(prefs.getBoolean(SettingsConstantsUI.KEY_PREF_COMPASS_KEEP_SCREEN, true));
        mTrueNorth = prefs.getBoolean(SettingsConstantsUI.KEY_PREF_COMPASS_TRUE_NORTH, true);
        mShowMagnetic = prefs.getBoolean(SettingsConstantsUI.KEY_PREF_COMPASS_MAGNETIC, true);

        ((MainActivity) getActivity()).setActionBarState(false);
        getActivity().setTitle(R.string.compass_title);
        assert getView() != null;
        int accentColor = ControlHelper.getColor(getContext(), R.attr.colorAccent);
        getView().setBackgroundColor(accentColor);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.menu_about || item.getItemId() == R.id.menu_settings) {
                continue;
            }
            item.setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onDestroyView()
    {
        ((MainActivity) getActivity()).restoreBottomBar(-1);
        getActivity().setTitle(R.string.app_name);
        super.onDestroyView();
    }


    @Override
    public void updateCompass(float azimuth) {
        float alpha = 1f;
        if (mShowMagnetic) {
            alpha = .3f;
        }

        ViewHelper.setAlpha(mCompassNeedleMagnetic, alpha);
        ViewHelper.setAlpha(mCompassNeedle, alpha);

        super.updateCompass(azimuth);
    }
}
