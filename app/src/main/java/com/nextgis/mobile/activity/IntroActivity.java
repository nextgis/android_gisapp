/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2017 NextGIS, info@nextgis.com
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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.util.SettingsConstants;
import com.tech.freak.wizardpager.model.AbstractWizardModel;
import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.PageList;
import com.tech.freak.wizardpager.model.ReviewItem;
import com.tech.freak.wizardpager.ui.PageFragmentCallbacks;

import java.util.ArrayList;
import java.util.List;

public class IntroActivity extends NGActivity implements PageFragmentCallbacks, ViewPager.OnPageChangeListener, View.OnClickListener {
    private AbstractWizardModel mWizardModel = new IntroWizardModel(this);

    private ViewPager mPager;
    private List<Page> mCurrentPageSequence;
    private Button mNextButton, mPrevButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        mCurrentPageSequence = mWizardModel.getCurrentPageSequence();
        IntroPagerAdapter pagerAdapter = new IntroPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(pagerAdapter);

        mPager.addOnPageChangeListener(this);
        mNextButton = (Button) findViewById(R.id.next_button);
        mPrevButton = (Button) findViewById(R.id.prev_button);
        mNextButton.setOnClickListener(this);
        mPrevButton.setOnClickListener(this);
        updateBottomBar();
    }

    @Override
    public void finish() {
        super.finish();
        mPreferences.edit().putBoolean(SettingsConstants.KEY_PREF_INTRO, true).apply();
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPager.removeOnPageChangeListener(this);
    }

    @Override
    protected void setToolbar(int toolbarId) {
        super.setToolbar(toolbarId);

        if (null != getSupportActionBar()) {
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void updateBottomBar() {
        int position = mPager.getCurrentItem();
        mNextButton.setText(position == mCurrentPageSequence.size() - 1 ? R.string.skip : R.string.attributes_next);
        mPrevButton.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public Page onGetPage(String key) {
        return mWizardModel.findByKey(key);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        updateBottomBar();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.prev_button:
                mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                break;
            case R.id.next_button:
                if (mPager.getCurrentItem() == mCurrentPageSequence.size() - 1)
                    finish();
                else
                    mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                break;
        }
    }

    public class IntroPagerAdapter extends FragmentStatePagerAdapter {

        IntroPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return mCurrentPageSequence.get(i).createFragment();
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    private class IntroWizardModel extends AbstractWizardModel {

        IntroWizardModel(Context context) {
            super(context);
        }

        @Override
        protected PageList onNewRootPageList() {
            return new PageList(new IntroPage(this, "1"), new IntroPage(this, "2"), new IntroPage(this, "3"));
        }
    }

    private class IntroPage extends Page {

        IntroPage(ModelCallbacks callbacks, String title) {
            super(callbacks, title);
        }

        @Override
        public Fragment createFragment() {
            return IntroFragment.newInstance(getKey());
        }

        @Override
        public void getReviewItems(ArrayList<ReviewItem> dest) {

        }
    }

    public static class IntroFragment extends Fragment {
        private static final String ARG_KEY = "key";

        private String mKey;

        public static IntroFragment newInstance(String key) {
            Bundle args = new Bundle();
            args.putString(ARG_KEY, key);

            IntroFragment fragment = new IntroFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            mKey = args.getString(ARG_KEY);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            switch (mKey) {
                case "1":
                    return inflater.inflate(R.layout.fragment_intro1, container, false);
                case "2":
                    View v = inflater.inflate(R.layout.fragment_intro2, container, false);
                    v.findViewById(R.id.get_pro).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent pricing = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.pricing)));
                            startActivity(pricing);
                        }
                    });
                    return v;
                case "3":
                    return inflater.inflate(R.layout.fragment_intro_login, container, false);
                default:
                    return null;
            }
        }
    }
}