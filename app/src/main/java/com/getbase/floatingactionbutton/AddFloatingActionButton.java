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

package com.getbase.floatingactionbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import com.nextgis.mobile.R;


public class AddFloatingActionButton
        extends FloatingActionButton
{
    int mPlusColor;


    public AddFloatingActionButton(Context context)
    {
        this(context, null);
    }


    public AddFloatingActionButton(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
    }


    public AddFloatingActionButton(
            Context context,
            AttributeSet attrs,
            int defStyle)
    {
        super(context, attrs, defStyle);
    }


    @Override
    void init(
            Context context,
            AttributeSet attributeSet)
    {
        TypedArray attr = context.obtainStyledAttributes(
                attributeSet, R.styleable.AddFloatingActionButton, 0, 0);
        mPlusColor = attr.getColor(
                R.styleable.AddFloatingActionButton_fab_plusIconColor,
                getColor(android.R.color.white));
        attr.recycle();

        super.init(context, attributeSet);
    }


    @Override
    public Drawable getIconDrawable()
    {
        final float iconSize = getDimension(R.dimen.fab_icon_size);
        final float iconHalfSize = iconSize / 2f;

        final float plusSize = getDimension(R.dimen.fab_plus_icon_size);
        final float plusHalfStroke = getDimension(R.dimen.fab_plus_icon_stroke) / 2f;
        final float plusOffset = (iconSize - plusSize) / 2f;

        final Shape shape = new Shape()
        {
            @Override
            public void draw(
                    Canvas canvas,
                    Paint paint)
            {
                canvas.drawRect(
                        plusOffset, iconHalfSize - plusHalfStroke, iconSize - plusOffset,
                        iconHalfSize + plusHalfStroke, paint);
                canvas.drawRect(
                        iconHalfSize - plusHalfStroke, plusOffset, iconHalfSize + plusHalfStroke,
                        iconSize - plusOffset, paint);
            }
        };

        ShapeDrawable drawable = new ShapeDrawable(shape);

        final Paint paint = drawable.getPaint();
        paint.setColor(mPlusColor);
        paint.setStyle(Style.FILL);
        paint.setAntiAlias(true);

        return drawable;
    }


    @Override
    public void setIcon(
            @DrawableRes
            int icon)
    {
        throw new UnsupportedOperationException(
                "Use FloatingActionButton if you want to use custom icon");
    }


    /**
     * @return the current Color of plus icon.
     */
    public int getPlusColor()
    {
        return mPlusColor;
    }


    public void setPlusColor(int color)
    {
        if (mPlusColor != color) {
            mPlusColor = color;
            updateBackground();
        }
    }


    public void setPlusColorResId(
            @ColorRes
            int plusColor)
    {
        setPlusColor(getColor(plusColor));
    }
}
