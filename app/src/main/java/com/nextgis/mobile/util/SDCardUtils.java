package com.nextgis.mobile.util;

import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_SD_CARD_NAME;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import java.io.File;

public class SDCardUtils{
    public static boolean isSDCardUsedAndExtracted(Context context){

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String sdCardName = mSharedPreferences.getString(KEY_PREF_SD_CARD_NAME, "");

        if (!TextUtils.isEmpty(sdCardName)){
            // sd card used
            boolean sdStillExists = false;
            File[] files = ContextCompat.getExternalFilesDirs(context, null);
            for (File file : files){
                if (file != null && file.getAbsolutePath().contains(sdCardName))
                    sdStillExists = true;

            }
            if (!sdStillExists)
                return true;
        }
        return false;
    }
}
