/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

package com.nextgis.mobile.datasource;

import static com.nextgis.maplib.util.Constants.MESSAGE_EXTRA;
import static com.nextgis.maplib.util.Constants.MESSAGE_NOTIFY_INTENT;
import static com.nextgis.maplib.util.Constants.MESSAGE_TITLE_EXTRA;
import static com.nextgis.maplibui.util.NotificationHelper.createBuilder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;

import androidx.core.app.NotificationCompat;

import com.nextgis.maplib.service.NGWSyncService;
import com.nextgis.maplibui.util.NotificationHelper;
import com.nextgis.mobile.R;
import com.nextgis.mobile.activity.MainActivity;

public class SyncService extends NGWSyncService {

    private static final int NOTIFICATION_ID = 518;
    protected MessageReceiver mMessageReceiver;

    @Override
    public void onCreate(){
        super.onCreate();
        mMessageReceiver = new MessageReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MESSAGE_NOTIFY_INTENT);
        registerReceiver(mMessageReceiver, intentFilter);
    }

    @Override
    protected SyncAdapter createSyncAdapter(Context context, boolean autoInitialize) {
        return new SyncAdapter(context, autoInitialize);
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    protected class MessageReceiver
            extends BroadcastReceiver    {
        @Override
        public void onReceive(Context context, Intent intent)        {
            if (intent.getAction().equals(MESSAGE_NOTIFY_INTENT)) {
                // show notification about it
                showNotificationErrorLayer(context, intent.getStringExtra(MESSAGE_EXTRA),
                        intent.getStringExtra(MESSAGE_TITLE_EXTRA));
            }
       }
    }

    // show notification
    public void showNotificationErrorLayer(
            Context context,
            String message,
            String title ){

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = createBuilder(context, com.nextgis.maplibui.R.string.sync);
        builder.setSmallIcon(R.drawable.ic_action_information_light)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setOngoing(false);

        Bitmap largeIcon = NotificationHelper.getLargeIcon(R.drawable.ic_action_information_light, context.getResources());
                builder.setProgress(0, 0, false)
                        .setTicker(context.getString(com.nextgis.maplib.R.string.sync_finished))
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message));
                        //.setContentTitle(context.getString(com.nextgis.maplib.R.string.synchronization)).setContentText(context.getString(com.nextgis.maplib.R.string.sync_finished));

        builder.setLargeIcon(largeIcon);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
