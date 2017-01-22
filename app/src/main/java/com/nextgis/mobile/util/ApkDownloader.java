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

package com.nextgis.mobile.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.mobile.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class ApkDownloader extends AsyncTask<String, Integer, String> {
    private Activity mActivity;
    private ProgressDialog mProgress;
    private String mApkPath;

    public ApkDownloader(Activity activity) {
        mActivity = activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        ControlHelper.lockScreenOrientation(mActivity);
        mProgress = new ProgressDialog(mActivity);
        mProgress.setMessage(mActivity.getString(R.string.message_loading));
        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgress.setIndeterminate(true);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancel(true);
                Toast.makeText(mActivity, mActivity.getString(R.string.cancel_download), Toast.LENGTH_SHORT).show();
            }
        });
        mProgress.show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            mApkPath = Environment.getExternalStorageDirectory() + "/download/" + Uri.parse(params[0]).getLastPathSegment();
            URLConnection connection = url.openConnection();
            if (url.getProtocol().equalsIgnoreCase("https"))
                connection.setRequestProperty("Authorization", params[1]);

            connection.connect();
            int fileLength = connection.getContentLength() / 1024;
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(mApkPath);

            byte data[] = new byte[1024];
            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                publishProgress((int) total / 1024, fileLength);
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            return null;
        } catch (MalformedURLException e) {
            return mActivity.getString(R.string.error_invalid_url);
        } catch (IOException e) {
            return mActivity.getString(R.string.error_network_unavailable);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        mProgress.dismiss();

        if (result == null) {
            Intent install = new Intent();
            install.setAction(Intent.ACTION_VIEW);
            File apk = new File(mApkPath);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(mActivity, "com.keenfin.easypicker.provider", apk);
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else
                uri = Uri.fromFile(apk);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            mActivity.startActivity(install);
        } else
            Toast.makeText(mActivity, result, Toast.LENGTH_LONG).show();    // show some info

        ControlHelper.unlockScreenOrientation(mActivity);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        if (mProgress.isIndeterminate()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                mProgress.setProgressNumberFormat(String.format(Locale.getDefault(), "%.2f Mb", values[1] / 1024f));

            mProgress.setIndeterminate(false);
            mProgress.setMax(values[1]);
        }

        mProgress.setProgress(values[0]);
    }
}
