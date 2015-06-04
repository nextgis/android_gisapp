/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import com.nextgis.maplibui.fragment.LayersListAdapter;
import com.nextgis.maplibui.fragment.ReorderedLayerView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.TypeEvaluator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.animation.AnimatorProxy;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;


public class ReorderedLayerViewAnimated
        extends ReorderedLayerView
{
    private final int MOVE_DURATION = 150;


    public ReorderedLayerViewAnimated(Context context)
    {
        super(context);
    }


    public ReorderedLayerViewAnimated(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
    }


    public ReorderedLayerViewAnimated(
            Context context,
            AttributeSet attrs,
            int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReorderedLayerViewAnimated(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    protected void handleCellSwitch()
    {
        final int deltaY = mLastEventY - mDownY;
        int deltaYTotal = mHoverCellOriginalBounds.top + mTotalOffset + deltaY;

        View belowView = getViewForID(mBelowItemId);
        View mobileView = getViewForID(mMobileItemId);
        View aboveView = getViewForID(mAboveItemId);

        boolean isBelow = (belowView != null) && (deltaYTotal > belowView.getTop());
        boolean isAbove = (aboveView != null) && (deltaYTotal < aboveView.getTop());

        if (isBelow || isAbove) {

            final long switchItemID = isBelow ? mBelowItemId : mAboveItemId;
            View switchView = isBelow ? belowView : aboveView;
            final int originalItem = getPositionForView(mobileView);

            if (switchView == null) {
                updateNeighborViewsForID(mMobileItemId);
                return;
            }

            LayersListAdapter adapter = (LayersListAdapter) getAdapter();
            if (null != adapter) {
                adapter.swapElements(originalItem, getPositionForView(switchView));
            }

            mDownY = mLastEventY;

            final int switchViewStartTop = switchView.getTop();

            mobileView.setVisibility(View.VISIBLE);
            switchView.setVisibility(View.INVISIBLE);

            updateNeighborViewsForID(mMobileItemId);

            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener()
                    {
                        public boolean onPreDraw()
                        {
                            observer.removeOnPreDrawListener(this);

                            View switchView = getViewForID(switchItemID);
                            AnimatorProxy switchViewProxy = AnimatorProxy.wrap(switchView);

                            mTotalOffset += deltaY;

                            int switchViewNewTop = switchView.getTop();
                            int delta = switchViewStartTop - switchViewNewTop;

                            switchViewProxy.setTranslationY(delta);

                            ObjectAnimator animator =
                                    ObjectAnimator.ofFloat(switchViewProxy, "translationY", 0);
                            animator.setDuration(MOVE_DURATION);
                            animator.start();

                            return true;
                        }
                    });
        }
    }


    @Override
    protected void touchEventsEnded()
    {

        final View mobileView = getViewForID(mMobileItemId);
        if (mCellIsMobile || mIsWaitingForScrollFinish) {

            LayersListAdapter adapter = (LayersListAdapter) getAdapter();
            adapter.endDrag();

            mCellIsMobile = false;
            mIsWaitingForScrollFinish = false;
            mIsMobileScrolling = false;
            mActivePointerId = NOT_FOUND;

            // If the autoscroller has not completed scrolling, we need to wait for it to
            // finish in order to determine the final location of where the hover cell
            // should be animated to.
            if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mIsWaitingForScrollFinish = true;
                return;
            }

            mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left, mobileView.getTop());

            ObjectAnimator hoverViewAnimator = ObjectAnimator.ofObject(
                    mHoverCell, "bounds", sBoundEvaluator, mHoverCellCurrentBounds);
            hoverViewAnimator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener()
                    {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator)
                        {
                            invalidate();
                        }
                    });
            hoverViewAnimator.addListener(
                    new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationStart(Animator animation)
                        {
                            setEnabled(false);
                        }


                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            mAboveItemId = NOT_FOUND;
                            mMobileItemId = NOT_FOUND;
                            mBelowItemId = NOT_FOUND;
                            mobileView.setVisibility(VISIBLE);
                            mHoverCell = null;
                            setEnabled(true);
                            invalidate();
                        }
                    });
            hoverViewAnimator.start();
        } else {
            touchEventsCancelled();
        }
    }


    /**
     * This TypeEvaluator is used to animate the BitmapDrawable back to its final location when the
     * user lifts his finger by modifying the BitmapDrawable's bounds.
     */
    private final static TypeEvaluator<Rect> sBoundEvaluator = new TypeEvaluator<Rect>()
    {
        public Rect evaluate(
                float fraction,
                Rect startValue,
                Rect endValue)
        {
            return new Rect(
                    interpolate(startValue.left, endValue.left, fraction),
                    interpolate(startValue.top, endValue.top, fraction),
                    interpolate(startValue.right, endValue.right, fraction),
                    interpolate(startValue.bottom, endValue.bottom, fraction));
        }


        public int interpolate(
                int start,
                int end,
                float fraction)
        {
            return (int) (start + fraction * (end - start));
        }
    };
}
