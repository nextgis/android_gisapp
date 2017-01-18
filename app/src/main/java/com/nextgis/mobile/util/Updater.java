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
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.nextgis.mobile.BuildConfig;
import com.nextgis.mobile.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class Updater extends AsyncTask<Activity, Void, Object[]> {
    private Activity mActivity;

    @Override
    protected Object[] doInBackground(Activity... params) {
        mActivity = params[0];
        try {
            URL url = new URL(SettingsConstants.APK_VERSION_UPDATE);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String data = "", line;
            while ((line = in.readLine()) != null)
                data += line;
            in.close();

            JSONObject json = new JSONObject(data);
            return new Object[]{json.getInt("versionCode"), json.getString("versionName"), json.getString("path")};
        } catch (JSONException | IOException ignored) { }

        return new Object[]{};
    }

    @Override
    protected void onPostExecute(final Object[] result) {
        super.onPostExecute(result);

        if (result.length > 0 && (int) result[0] > BuildConfig.VERSION_CODE) {
            // there is new version, create download dialog
            DialogInterface.OnClickListener dcl = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int button) {
                    new ApkDownloader().execute((String) result[2]);
                }
            };

            AlertDialog.Builder adb = new AlertDialog.Builder(mActivity);
            String update = String.format(mActivity.getString(R.string.new_update), result[1]);
            adb.setMessage(update).setTitle(R.string.update_title)
               .setPositiveButton(R.string.yes, dcl).setNegativeButton(R.string.no, null).show();
        }
    }

    private class ApkDownloader extends AsyncTask<String, Integer, String> {
        private ProgressDialog mProgress;
        private String mApkPath;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgress = new ProgressDialog(mActivity);
            mProgress.setMessage(mActivity.getString(R.string.message_loading));
            mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgress.setIndeterminate(true);
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
        protected String doInBackground(String... path) {
            try {
                URL url = new URL(path[0]);
                mApkPath = Environment.getExternalStorageDirectory() + "/download/" + Uri.parse(path[0]).getLastPathSegment();
                URLConnection connection = url.openConnection();
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
                Toast.makeText(mActivity, result, Toast.LENGTH_LONG).show();	// show some info
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            if (mProgress.isIndeterminate()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    mProgress.setProgressNumberFormat(String.format(Locale.getDefault(), "%.2f Mb", values[1]/ 1024f));

                mProgress.setIndeterminate(false);
                mProgress.setMax(values[1]);
            }

            mProgress.setProgress(values[0]);
        }
    }
}
