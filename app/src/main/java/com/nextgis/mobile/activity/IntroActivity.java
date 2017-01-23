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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.mobile.R;
import com.nextgis.mobile.util.SettingsConstants;
import com.tech.freak.wizardpager.model.AbstractWizardModel;
import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.PageList;
import com.tech.freak.wizardpager.model.ReviewItem;
import com.tech.freak.wizardpager.ui.PageFragmentCallbacks;
import com.tech.freak.wizardpager.ui.StepPagerStrip;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class IntroActivity extends NGActivity implements PageFragmentCallbacks, ViewPager.OnPageChangeListener, View.OnClickListener {
    private static final String IMAGE_ID = "IMAGE";
    private static final String TEXT_ID = "TEXT";

    private AbstractWizardModel mWizardModel = new IntroWizardModel(this);

    private ViewPager mPager;
    private StepPagerStrip mStepPagerStrip;
    private IntroPagerAdapter mPagerAdapter;
    private List<Page> mCurrentPageSequence;
    private Button mNextButton, mPrevButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        setToolbar(R.id.main_toolbar);

        mCurrentPageSequence = mWizardModel.getCurrentPageSequence();
        mPagerAdapter = new IntroPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mStepPagerStrip = (StepPagerStrip) findViewById(R.id.strip);
        mStepPagerStrip.setOnPageSelectedListener(new StepPagerStrip.OnPageSelectedListener() {
            @Override
            public void onPageStripSelected(int position) {
                position = Math.min(mPagerAdapter.getCount() - 1, position);
                if (mPager.getCurrentItem() != position)
                    mPager.setCurrentItem(position);
            }
        });

        mPager.addOnPageChangeListener(this);
        mNextButton = (Button) findViewById(R.id.next_button);
        mPrevButton = (Button) findViewById(R.id.prev_button);
        mNextButton.setOnClickListener(this);
        mPrevButton.setOnClickListener(this);
        mStepPagerStrip.setPageCount(mCurrentPageSequence.size());
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
        mStepPagerStrip.setCurrentPage(position);
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
            return 2;
        }
    }

    private class IntroWizardModel extends AbstractWizardModel {

        IntroWizardModel(Context context) {
            super(context);
        }

        @Override
        protected PageList onNewRootPageList() {
            return new PageList(new IntroPage(this, "1").setText(R.string.intro1).setImage(R.drawable.intro_logo),
                                new IntroPage(this, "2").setText(R.string.intro2));
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

        public IntroPage setImage(int imageId) {
            mData.putInt(IMAGE_ID, imageId);
            return this;
        }

        public IntroPage setText(int stringId) {
            mData.putInt(TEXT_ID, stringId);
            return this;
        }
    }

    public static class IntroFragment extends Fragment {
        private static final String ARG_KEY = "key";

        private PageFragmentCallbacks mCallbacks;
        private String mKey;
        private Page mPage;

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
            mPage = mCallbacks.onGetPage(mKey);
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mCallbacks = (PageFragmentCallbacks) context;
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mCallbacks = null;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView;
            if (mKey.equals("2")) {
                rootView = inflater.inflate(R.layout.fragment_intro_login, container, false);
            } else {
                rootView = inflater.inflate(R.layout.fragment_intro, container, false);

                final int image = mPage.getData().getInt(IMAGE_ID);
                final ImageView imageView = (ImageView) rootView.findViewById(R.id.imageView);
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        InputStream is = getResources().openRawResource(image);
                        BitmapFactory.Options options = ControlHelper.getOptions(is, imageView.getMeasuredWidth(), imageView.getMeasuredWidth());
                        is = getResources().openRawResource(image);
                        Bitmap bitmap = ControlHelper.getBitmap(is, options);
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }

            int text = mPage.getData().getInt(TEXT_ID);
            TextView textView = (TextView) rootView.findViewById(R.id.textView);
            textView.setText(text);

            return rootView;
        }
    }
}