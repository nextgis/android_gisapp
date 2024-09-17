package com.nextgis.mobile.util;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;

import com.nextgis.mobile.activity.MainActivity;

public class SDCardReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        String nameSD = intent.getData().getPath();
        if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Карточку вытащили")
                    .setPositiveButton("ok", null)
                    .setTitle("SD Card");
            AlertDialog alertDialog=builder.create();
            alertDialog.show();
            // SD card has been removed
        } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            // SD card has been mounted
        }
    }

//    public void registerReceiver(Context context) {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_MEDIA_EJECT);
//        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
//        filter.addDataScheme("file");
//        context.registerReceiver(this, filter );
//    }
}
