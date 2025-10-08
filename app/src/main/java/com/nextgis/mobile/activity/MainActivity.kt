/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2021 NextGIS, info@nextgis.com
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
package com.nextgis.mobile.activity

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SyncResult
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.snackbar.Snackbar
import com.hypertrack.hyperlog.HyperLog
import com.nextgis.maplib.api.GpsEventListener
import com.nextgis.maplib.api.IGISApplication
import com.nextgis.maplib.api.ILayer
import com.nextgis.maplib.datasource.GeoMultiPoint
import com.nextgis.maplib.datasource.GeoPoint
import com.nextgis.maplib.map.MapDrawable
import com.nextgis.maplib.map.NGWVectorLayer
import com.nextgis.maplib.map.VectorLayer
import com.nextgis.maplib.util.AccountUtil
import com.nextgis.maplib.util.Constants
import com.nextgis.maplib.util.FileUtil
import com.nextgis.maplib.util.GeoConstants
import com.nextgis.maplib.util.MapUtil
import com.nextgis.maplib.util.NGWUtil
import com.nextgis.maplib.util.NetworkUtil
import com.nextgis.maplibui.activity.NGActivity
import com.nextgis.maplibui.api.IChooseLayerResult
import com.nextgis.maplibui.api.IVectorLayerUI
import com.nextgis.maplibui.fragment.BottomToolbar
import com.nextgis.maplibui.fragment.LayerFillProgressDialogFragment
import com.nextgis.maplibui.overlay.EditLayerOverlay
import com.nextgis.maplibui.service.TrackerService
import com.nextgis.maplibui.service.TrackerService.BackgroundPermissionCallback
import com.nextgis.maplibui.util.ConstantsUI
import com.nextgis.maplibui.util.ControlHelper
import com.nextgis.maplibui.util.NGIDUtils
import com.nextgis.maplibui.util.SettingsConstantsUI
import com.nextgis.maplibui.util.UiUtil
import com.nextgis.mobile.MainApplication
import com.nextgis.mobile.R
import com.nextgis.mobile.fragment.LayersFragment
import com.nextgis.mobile.fragment.MapFragment
import com.nextgis.mobile.util.AppSettingsConstants
import com.nextgis.mobile.util.SDCardUtils
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Main activity. Map and drawer with layers list created here
 */
class MainActivity : NGActivity(), GpsEventListener, IChooseLayerResult {
    var mapFragment: MapFragment? = null
        protected set
    protected var mLayersFragment: LayersFragment? = null
    protected var mMessageReceiver: MessageReceiver? = null
    protected var mToolbar: Toolbar? = null

    protected var mBackPressed: Long = 0
    protected var mTrackItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences_general, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_map, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_location, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_tracks, false)

        if (!mPreferences.getBoolean(AppSettingsConstants.KEY_PREF_INTRO, false)) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        mMessageReceiver = MessageReceiver()

        mToolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(mToolbar)
        if (null != supportActionBar) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawerLayout.setStatusBarBackgroundColor(
            ControlHelper.getColor(
                this,
                android.R.attr.colorPrimaryDark
            )
        )

        val fm = supportFragmentManager
        mapFragment = fm.findFragmentById(R.id.map) as MapFragment?
        mapFragment!!.undoRedoOverlay?.setTopToolbar(mToolbar)
        mapFragment!!.editLayerOverlay?.setTopToolbar(mToolbar)
        mapFragment!!.editLayerOverlay?.setBottomToolbar(bottomToolbar)

        val app = application as MainApplication
        mLayersFragment = fm.findFragmentById(R.id.layers) as LayersFragment?

        if (mLayersFragment != null && null != mLayersFragment!!.view) {
            mLayersFragment?.view?.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        com.nextgis.maplibui.R.color.color_grey_050
                    )
                )
            // Set up the drawer.
            mLayersFragment!!.setUp(R.id.layers, drawerLayout, app.map as MapDrawable)
        }

        var progressFragment =
            fm.findFragmentByTag(TAG_FRAGMENT_PROGRESS) as LayerFillProgressDialogFragment?
        if (progressFragment == null) {
            progressFragment = LayerFillProgressDialogFragment()
            fm.beginTransaction().add(progressFragment, TAG_FRAGMENT_PROGRESS).commit()
        }


        if (!hasLocationPermissions()) {
            Handler().postDelayed({ processAllPermisions(PERMISSIONS_REQUEST_ZERO) }, 1500)
        }

        //        if (!hasLocationPermissions()) {
//            List<String> permslist = new ArrayList<>();
//            permslist.add(Manifest.permission.ACCESS_COARSE_LOCATION);
//            permslist.add(Manifest.permission.ACCESS_FINE_LOCATION);
        /*            permslist.add(Manifest.permission.GET_ACCOUNTS);
        * /            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        * /                permslist.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        * /
        * /            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2)
        * /                permslist.add(Manifest.permission.POST_NOTIFICATIONS); */
//
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    requestPermissions(R.string.permissions, R.string.location_permissions, PERMISSIONS_REQUEST_LOC, permslist.toArray(new String[permslist.size()])); // list.toArray(new Foo[list.size()])
//                }
//            }, 5000);
//
//        }
        NGIDUtils.get(this) { response ->
            if (response.isOk) {
                var support = getExternalFilesDir(null)
                support = if (support == null) File(filesDir, Constants.SUPPORT)
                else File(support, Constants.SUPPORT)

                try {
                    val jsonString = FileUtil.readFromFile(support)
                    val json = JSONObject(jsonString)
                    if (json.optBoolean(Constants.JSON_SUPPORTED_KEY)) {
                        val id = json.getString(Constants.JSON_USER_ID_KEY)
                        NetworkUtil.setUserNGUID(id)
                    }
                } catch (exception: Exception) {
                }

                try {
                    FileUtil.writeToFile(support, response.responseBody)
                } catch (ignored: IOException) {
                }

                NetworkUtil.setIsPro(AccountUtil.isProUser(baseContext))
            }
            if (mapFragment!!.editLayerOverlay?.mode == EditLayerOverlay.MODE_NONE)
                mToolbar?.setTitle(appName)
            //                boolean isLoggedIn = !TextUtils.isEmpty(mPreferences.getString(NGIDUtils.PREF_ACCESS_TOKEN, ""));
            //                if (!isLoggedIn)
            //                    showSnack();
        }
    }

    private fun showSnack() {
        val snackbar = Snackbar.make(
            findViewById(R.id.mainview),
            getString(R.string.support_available),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.more) {
                val pricing =
                    Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.pricing)))
                startActivity(pricing)
            }

        val view = snackbar.view
        val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(
            ContextCompat.getColor(
                view.context,
                com.nextgis.maplibui.R.color.color_white
            )
        )
        snackbar.show()
    }

    fun processAllPermisions(startlevel: Int) {
        if (startlevel == PERMISSIONS_REQUEST_ZERO) {
            if (!hasLocationPermissions()) {
                val permslist: MutableList<String> = ArrayList()
                permslist.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                permslist.add(Manifest.permission.ACCESS_FINE_LOCATION)
                requestPermissions(
                    this,
                    R.string.permissions,
                    R.string.location_permissions,
                    PERMISSIONS_REQUEST_LOC,
                    PERMISSIONS_REQUEST_LOC,
                    *permslist.toTypedArray<String>()
                ) // list.toArray(new Foo[list.size()])
                return
            } else processAllPermisions(PERMISSIONS_REQUEST_LOC)
            return
        }
        if (startlevel == PERMISSIONS_REQUEST_LOC) {
            if (!hasAccountCreatePermissions()) {
                val permslist: MutableList<String> = ArrayList()
                permslist.add(Manifest.permission.GET_ACCOUNTS)
                requestPermissions(
                    this,
                    R.string.permissions,
                    R.string.account_permissions,
                    PERMISSIONS_REQUEST_ACCOUNT,
                    -1,
                    *permslist.toTypedArray<String>()
                ) // list.toArray(new Foo[list.size()])
            } else processAllPermisions(PERMISSIONS_REQUEST_ACCOUNT)
            return
        }
        if (startlevel == PERMISSIONS_REQUEST_ACCOUNT) {
            if (!hasSDCARDWritePermissions()) {
                val permslist: MutableList<String> = ArrayList()
                permslist.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(
                    this,
                    R.string.permissions,
                    R.string.memory_permissions,
                    PERMISSIONS_REQUEST_MEMORY,
                    PERMISSIONS_REQUEST_MEMORY,
                    *permslist.toTypedArray<String>()
                ) // list.toArray(new Foo[list.size()])
            } else processAllPermisions(PERMISSIONS_REQUEST_MEMORY)
            return
        }
        if (startlevel == PERMISSIONS_REQUEST_MEMORY) {
            if (!hasNotifyPermissions()) {
                val permslist: MutableList<String> = ArrayList()
                permslist.add(Manifest.permission.POST_NOTIFICATIONS)
                requestPermissions(
                    this, R.string.permissions, R.string.push_permissions, PERMISSIONS_REQUEST_PUSH,
                    -1,
                    *permslist.toTypedArray<String>()
                ) // list.toArray(new Foo[list.size()])
            }
        }
    }

    protected fun hasLocationPermissions(): Boolean {
        val permissions =
            isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        return permissions
    }

    protected fun hasAccountCreatePermissions(): Boolean {
        return isPermissionGranted(Manifest.permission.GET_ACCOUNTS)
    }

    protected fun hasSDCARDWritePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else true
    }

    protected fun hasNotifyPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        else true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            TrackerService.PERMISSIONS_REQUEST_ZERO_LOCATION_POSPONDED -> if (hasLocationPermissions()) {
                var item: MenuItem? = null
                try {
                    item = mToolbar!!.menu.findItem(R.id.menu_track)
                } catch (ex: Exception) {
                }
                askBackgroundPerm(item)
            }

            PERMISSIONS_REQUEST_LOC -> {
                mapFragment!!.restartGpsListener()
                processAllPermisions(PERMISSIONS_REQUEST_LOC)
            }

            PERMISSIONS_REQUEST_ACCOUNT -> processAllPermisions(PERMISSIONS_REQUEST_ACCOUNT)
            PERMISSIONS_REQUEST_MEMORY -> processAllPermisions(PERMISSIONS_REQUEST_MEMORY)
            PERMISSIONS_REQUEST_PUSH ->                 // turn on sync notify
                if (permissions.size > 0) {
                    var i = 0
                    while (i < permissions.size) {
                        if (permissions[i] == Manifest.permission.POST_NOTIFICATIONS
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED
                        ) {
                            PreferenceManager.getDefaultSharedPreferences(
                                applicationContext
                            ).edit()
                                .putBoolean(AppSettingsConstants.KEY_PREF_SHOW_SYNC, true)
                                .commit()
                        }
                        i++
                    }
                }

            LOCATION_BACKGROUND_REQUEST -> if (mTrackItem != null) {
                controlTrack(mTrackItem)
                mTrackItem = null
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun showEditToolbar() {
        stopRefresh(mToolbar!!.menu.findItem(R.id.menu_refresh))
        mToolbar!!.menu.clear()
        mToolbar!!.inflateMenu(com.nextgis.maplibui.R.menu.edit_geometry)

        var item = mToolbar!!.menu.findItem(com.nextgis.maplibui.R.id.menu_edit_redo)
        val visible = mapFragment!!.mode != MapFragment.MODE_EDIT_BY_WALK
        item.setVisible(visible)
        item = mToolbar!!.menu.findItem(com.nextgis.maplibui.R.id.menu_edit_undo)
        item.setVisible(visible)

        mLayersFragment!!.isDrawerToggleEnabled = false
        mToolbar!!.setNavigationIcon(com.nextgis.maplibui.R.drawable.ic_action_cancel_dark)
    }


    fun showDefaultToolbar() {
        mToolbar?.title = appName
        mToolbar?.setSubtitle(null)
        mToolbar?.menu?.clear()
        mToolbar?.inflateMenu(R.menu.main)
        mLayersFragment!!.isDrawerToggleEnabled = true
        mLayersFragment!!.syncState()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (null != mLayersFragment && !mLayersFragment!!.isDrawerOpen) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            menuInflater.inflate(R.menu.main, menu)

            //restoreActionBar();
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }


    val bottomToolbar: BottomToolbar
        get() = findViewById<View>(com.nextgis.maplibui.R.id.bottom_toolbar) as BottomToolbar


    private fun controlTrack(item: MenuItem?) {
        if (item != null) {
            val iconAndTitle = TrackerService.controlAndGetIconWithTitle(this)
            setTrackItem(item, iconAndTitle.second, iconAndTitle.first)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val app = application as IGISApplication

        when (item.itemId) {
            android.R.id.home -> if (hasFragments()) return finishFragment()
            else if (mapFragment!!.isEditMode)
                return mapFragment!!.onOptionsItemSelected(item.itemId)
            else {
                mLayersFragment!!.toggle()
                return true
            }

            R.id.menu_settings -> {
                app.showSettings(
                    SettingsConstantsUI.ACTION_PREFS_GENERAL, RELOAD_ACTIVITY_DATA,
                    this
                )
                return true
            }

            R.id.menu_about -> {
                val intentAbout = Intent(this, AboutActivity::class.java)
                startActivity(intentAbout)
                return true
            }

            R.id.menu_locate -> {
                locateCurrentPosition()
                return true
            }

            R.id.menu_track -> {
                askBackgroundPerm(item)
                return true
            }

            R.id.menu_refresh -> {
                if (null != mapFragment) {
                    mapFragment!!.refresh()
                }
                return true
            }

            com.nextgis.maplibui.R.id.menu_edit_save -> return mapFragment!!.saveEdits()
            com.nextgis.maplibui.R.id.menu_edit_undo, com.nextgis.maplibui.R.id.menu_edit_redo -> return mapFragment!!.onOptionsItemSelected(
                item.itemId
            )

            R.id.menu_share_log -> {
                shareLog()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun askBackgroundPerm(item: MenuItem?) {
        TrackerService.showBackgroundDialog(this, object : BackgroundPermissionCallback {
            override fun beforeAndroid10(hasBackgroundPermission: Boolean) {
                if (!hasBackgroundPermission) {
                    mTrackItem = item
                    val permissions = arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    requestPermissions(
                        this@MainActivity,
                        R.string.permissions,
                        R.string.location_permissions,
                        LOCATION_BACKGROUND_REQUEST,
                        -1,
                        *permissions
                    )
                } else {
                    controlTrack(item)
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            override fun onAndroid10(hasBackgroundPermission: Boolean) {
                if (!hasBackgroundPermission) {
                    mTrackItem = item
                    requestbackgroundLocationPermissions()
                } else {
                    controlTrack(item)
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            override fun afterAndroid10(hasBackgroundPermission: Boolean) {
                if (!hasBackgroundPermission) {
                    mTrackItem = item
                    requestbackgroundLocationPermissions()
                } else {
                    controlTrack(item)
                }
            }
        })
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun requestbackgroundLocationPermissions() {
        val permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ActivityCompat.requestPermissions(this, permissions, LOCATION_BACKGROUND_REQUEST)
    }


    private fun shareLog() {
        HyperLog.getDeviceLogsInFile(this)
        val dir = File(getExternalFilesDir(null), "LogFiles")
        val size = FileUtil.getDirectorySize(dir)
        if (size == 0L) {
            Toast.makeText(this, com.nextgis.maplib.R.string.error_empty_dataset, Toast.LENGTH_LONG)
                .show()
            return
        }

        val files = zipLogs(dir)
        val type = "text/plain"
        UiUtil.share(files, type, this, true)
    }

    private fun zipLogs(dir: File): File? {
        var temp = MapUtil.prepareTempDir(this, "shared_layers", false)
        val outdated = ArrayList<File>()
        try {
            val fileName = "ng-logs.zip"
            if (temp == null) {
                AlertDialog.Builder(this)
                    .setMessage(com.nextgis.maplibui.R.string.error_file_create)
                    .setPositiveButton(com.nextgis.maplibui.R.string.ok, null)
                    .create()
                    .show()
                //Toast.makeText(this, R.string.error_file_create, Toast.LENGTH_LONG).show();
            }

            temp = File(temp, fileName)
            temp.createNewFile()
            val fos = FileOutputStream(temp, false)
            val zos = ZipOutputStream(BufferedOutputStream(fos))

            val buffer = ByteArray(1024)
            var length: Int

            for (file in dir.listFiles()) {
                if (System.currentTimeMillis() - file.lastModified() > 60 * 60 * 1000) outdated.add(
                    file
                )
                try {
                    val fis = FileInputStream(file)
                    zos.putNextEntry(ZipEntry(file.name))

                    while ((fis.read(buffer).also { length = it }) > 0) zos.write(buffer, 0, length)

                    zos.closeEntry()
                    fis.close()
                } catch (ignored: Exception) {
                }
            }

            zos.close()
            fos.close()
        } catch (ignored: IOException) {
            temp = null
        }
        for (file in outdated) {
            file.delete()
        }
        return temp
    }

    private fun setTrackItem(item: MenuItem?, title: Int, icon: Int) {
        if (null != item) {
            item.setTitle(title)
            item.setIcon(icon)
        }
    }


    fun hasFragments(): Boolean {
        return supportFragmentManager.backStackEntryCount > 0
    }


    fun finishFragment(): Boolean {
        if (hasFragments()) {
            supportFragmentManager.popBackStack()
            setActionBarState(true)
            return true
        }

        return false
    }


    override fun isHomeEnabled(): Boolean {
        return false
    }


    @Synchronized
    fun onRefresh(isRefresh: Boolean) {
        val refreshItem = mToolbar!!.menu.findItem(R.id.menu_refresh)
        if (null != refreshItem) {
            if (isRefresh) {
                if (refreshItem.actionView == null) {
                    refreshItem.setActionView(R.layout.layout_refresh)
                    val progress =
                        refreshItem.actionView!!.findViewById<ProgressBar>(R.id.refreshingProgress)
                    progress?.indeterminateDrawable?.setColorFilter(
                        ContextCompat.getColor(
                            this,
                            com.nextgis.maplibui.R.color.color_grey_200
                        ), PorterDuff.Mode.SRC_IN
                    )
                }
            } else stopRefresh(refreshItem)
        }
    }

    protected fun stopRefresh(refreshItem: MenuItem?) {
        val handler = Handler(Looper.getMainLooper())
        val r = Runnable {
            if (refreshItem != null && refreshItem.actionView != null) {
                refreshItem.actionView!!.clearAnimation()
                refreshItem.setActionView(null)
            }
        }
        handler.post(r)
    }


    fun addLocalLayer() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        // https://developer.android.com/guide/topics/providers/document-provider.html#client
        val intent = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Intent(Intent.ACTION_GET_CONTENT)
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        }
        intent.setType("*/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_file)),
                FILE_SELECT_CODE
            )
        } catch (ex: ActivityNotFoundException) {
            //TODO: open select local resource dialog
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(
                this, getString(R.string.warning_install_file_manager), Toast.LENGTH_SHORT
            )
                .show()
        }
    }



    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?) {
        //http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
        //http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult/18345899
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            FILE_SELECT_CODE -> if (resultCode == RESULT_OK) {
                // Get the Uri of the selected file
                val uri = data?.data
                if (Constants.DEBUG_MODE) Log.d(
                    Constants.TAG, "File Uri: " + (uri?.toString()
                        ?: "")
                )
                //check the file type from extension
                val fileName = FileUtil.getFileNameByUri(this, uri, "")
                if (fileName.lowercase(Locale.getDefault()).endsWith("ngrc") ||
                    fileName.lowercase(Locale.getDefault()).endsWith("zip")
                ) { //create local tile layer
                    if (null != mapFragment) {
                        mapFragment!!.addLocalTMSLayer(uri)
                    }
                } else if (fileName.lowercase(Locale.getDefault())
                        .endsWith("geojson")
                ) { //create local vector layer
                    if (null != mapFragment) {
                        mapFragment!!.addLocalVectorLayer(uri)
                    }
                } else if (fileName.lowercase(Locale.getDefault())
                        .endsWith("ngfp")
                ) { //create local vector layer with form
                    if (null != mapFragment) {
                        mapFragment!!.addLocalVectorLayerWithForm(uri)
                    }
                } else {
                    Toast.makeText(
                        this, getString(R.string.error_file_unsupported),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            IVectorLayerUI.MODIFY_REQUEST -> mapFragment!!.onActivityResult(
                requestCode,
                resultCode,
                data
            )

            RELOAD_ACTIVITY_DATA -> if (resultCode == RESULT_OK) {
                finish()
                val intent = Intent(this, this.javaClass)
                startActivity(intent)
            }
        }
    }


    protected fun locateCurrentPosition() {
        if (!hasLocationPermissions()) {
            val permslist: MutableList<String> = ArrayList()
            permslist.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permslist.add(Manifest.permission.ACCESS_FINE_LOCATION)
            requestPermissions(
                this,
                R.string.permissions,
                R.string.location_permissions,
                PERMISSIONS_REQUEST_LOC_SILENT,
                PERMISSIONS_REQUEST_LOC_SILENT,
                *permslist.toTypedArray<String>()
            ) //
        }

        if (null != mapFragment) {
            mapFragment!!.locateCurrentPosition()
        }
    }


    fun testSync() {
        val application = application as IGISApplication
        val map = application.map
        var ngwVectorLayer: NGWVectorLayer
        for (i in 0..<map.layerCount) {
            val layer = map.getLayer(i)
            if (layer is NGWVectorLayer) {
                ngwVectorLayer = layer
                val ver = NGWUtil.getNgwVersion(this, ngwVectorLayer.accountName)
                ngwVectorLayer.sync(application.authority, ver, SyncResult())
            }
        }
    }


    fun testUpdate() {
        //test sync
        val application = application as IGISApplication
        val map = application.map
        var ngwVectorLayer: NGWVectorLayer? = null
        for (i in 0..<map.layerCount) {
            val layer = map.getLayer(i)
            if (layer is NGWVectorLayer) {
                ngwVectorLayer = layer
            }
        }
        if (null != ngwVectorLayer) {
            val uri = Uri.parse(
                "content://" + AppSettingsConstants.AUTHORITY + "/" +
                        ngwVectorLayer.path.name
            )
            val updateUri = ContentUris.withAppendedId(uri, 29)
            val values = ContentValues()
            values.put("width", 4)
            values.put("azimuth", 8.0)
            values.put("status", "test4")
            values.put("temperatur", -10)
            values.put("name", "xxx")

            val calendar: Calendar = GregorianCalendar(2014, Calendar.JANUARY, 23)
            values.put("datetime", calendar.timeInMillis)
            try {
                val pt = GeoPoint(67.0, 65.0)
                pt.crs = GeoConstants.CRS_WGS84
                pt.project(GeoConstants.CRS_WEB_MERCATOR)
                val mpt = GeoMultiPoint()
                mpt.add(pt)
                values.put(Constants.FIELD_GEOM, mpt.toBlob())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val result = contentResolver.update(updateUri, values, null, null)
            if (Constants.DEBUG_MODE) {
                if (result == 0) {
                    Log.d(Constants.TAG, "update failed")
                } else {
                    Log.d(Constants.TAG, "" + result)
                }
            }
        }
    }


    fun testAttachUpdate() {
        val application = application as IGISApplication
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri updateUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" +
                                      ngwVectorLayer.getPath().getName() + "/36/attach/1000");
        */
        val updateUri = Uri.parse(
            "content://" + AppSettingsConstants.AUTHORITY +
                    "/layer_20150210140455993/36/attach/2"
        )

        val values = ContentValues()
        values.put(VectorLayer.ATTACH_DISPLAY_NAME, "no_image.jpg")
        values.put(VectorLayer.ATTACH_DESCRIPTION, "simple update description")
        //    values.put(VectorLayer.ATTACH_ID, 999);
        val result = contentResolver.update(updateUri, values, null, null)
        if (Constants.DEBUG_MODE) {
            if (result == 0) {
                Log.d(Constants.TAG, "update failed")
            } else {
                Log.d(Constants.TAG, "" + result)
            }
        }
        //}
    }


    fun testAttachDelete() {
        val application = application as IGISApplication
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri deleteUri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" +
                                ngwVectorLayer.getPath().getName() + "/36/attach/1000");
        */
        val deleteUri = Uri.parse(
            "content://" + AppSettingsConstants.AUTHORITY +
                    "/layer_20150210140455993/36/attach/1"
        )
        val result = contentResolver.delete(deleteUri, null, null)
        if (Constants.DEBUG_MODE) {
            if (result == 0) {
                Log.d(Constants.TAG, "delete failed")
            } else {
                Log.d(Constants.TAG, "" + result)
            }
        }
        //}
    }


    fun testAttachInsert() {
        val application = application as IGISApplication
        /*MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer = null;
        for(int i = 0; i < map.getLayerCount(); i++){
            ILayer layer = map.getLayer(i);
            if(layer instanceof NGWVectorLayer)
            {
                ngwVectorLayer = (NGWVectorLayer)layer;
            }
        }
        if(null != ngwVectorLayer) {
            Uri uri = Uri.parse("content://" + SettingsConstants.AUTHORITY + "/" + ngwVectorLayer.getPath().getName() + "/36/attach");
        */
        val uri = Uri.parse(
            "content://" + AppSettingsConstants.AUTHORITY + "/layer_20150210140455993/36/attach"
        )
        val values = ContentValues()
        values.put(VectorLayer.ATTACH_DISPLAY_NAME, "test_image.jpg")
        values.put(VectorLayer.ATTACH_MIME_TYPE, "image/jpeg")
        values.put(VectorLayer.ATTACH_DESCRIPTION, "test image description")

        val result = contentResolver.insert(uri, values)
        if (result == null) {
            Log.d(Constants.TAG, "insert failed")
        } else {
            try {
                val outStream = contentResolver.openOutputStream(result)
                val sourceBitmap = BitmapFactory.decodeResource(
                    resources, com.nextgis.maplibui.R.drawable.bk_tile
                )
                sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream!!)
                outStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (Constants.DEBUG_MODE) Log.d(Constants.TAG, result.toString())
        }
        //}
    }


    fun testInsert() {
        //test sync
        val application = application as IGISApplication
        val map = application.map
        var ngwVectorLayer: NGWVectorLayer? = null
        for (i in 0..<map.layerCount) {
            val layer = map.getLayer(i)
            if (layer is NGWVectorLayer) {
                ngwVectorLayer = layer
            }
        }
        if (null != ngwVectorLayer) {
            val uri = Uri.parse(
                "content://" + AppSettingsConstants.AUTHORITY + "/" +
                        ngwVectorLayer.path.name
            )
            val values = ContentValues()
            //values.put(VectorLayer.FIELD_ID, 26);
            values.put("width", 1)
            values.put("azimuth", 2.0)
            values.put("status", "grot")
            values.put("temperatur", -13)
            values.put("name", "get")

            val calendar: Calendar = GregorianCalendar(2015, Calendar.JANUARY, 23)
            values.put("datetime", calendar.timeInMillis)

            try {
                val pt = GeoPoint(37.0, 55.0)
                pt.crs = GeoConstants.CRS_WGS84
                pt.project(GeoConstants.CRS_WEB_MERCATOR)
                val mpt = GeoMultiPoint()
                mpt.add(pt)
                values.put(Constants.FIELD_GEOM, mpt.toBlob())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val result = contentResolver.insert(uri, values)
            if (Constants.DEBUG_MODE) {
                if (result == null) {
                    Log.d(Constants.TAG, "insert failed")
                } else {
                    Log.d(Constants.TAG, result.toString())
                }
            }
        }
    }


    fun testDelete() {
        val application = application as IGISApplication
        val map = application.map
        var ngwVectorLayer: NGWVectorLayer? = null
        for (i in 0..<map.layerCount) {
            val layer = map.getLayer(i)
            if (layer is NGWVectorLayer) {
                ngwVectorLayer = layer
            }
        }
        if (null != ngwVectorLayer) {
            val uri = Uri.parse(
                "content://" + AppSettingsConstants.AUTHORITY + "/" +
                        ngwVectorLayer.path.name
            )
            val deleteUri = ContentUris.withAppendedId(uri, 27)
            val result = contentResolver.delete(deleteUri, null, null)
            if (Constants.DEBUG_MODE) {
                if (result == 0) {
                    Log.d(Constants.TAG, "delete failed")
                } else {
                    Log.d(Constants.TAG, "" + result)
                }
            }
        }
    }


    fun addNGWLayer() {
        if (null != mapFragment) {
            mapFragment!!.addNGWLayer()
        }
    }


    fun addRemoteLayer() {
        if (null != mapFragment) {
            mapFragment!!.addRemoteLayer()
        }
    }


    override fun onFinishChooseLayerDialog(
        code: Int,
        layer: ILayer
    ) {
        if (null != mapFragment) {
            mapFragment!!.onFinishChooseLayerDialog(code, layer)
        }
    }


    protected inner class MessageReceiver

        : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (intent.action == ConstantsUI.MESSAGE_INTENT) {
                Log.e("ZZXX", "intent.getAction().equals(ConstantsUI.MESSAGE_INTENT)")
                Toast.makeText(
                    this@MainActivity, intent.extras!!.getString(
                        ConstantsUI.KEY_MESSAGE
                    ), Toast.LENGTH_SHORT
                ).show()
            }

            if (intent.action == Constants.MESSAGE_ALERT_INTENT) {
                Log.e("ZZXX", "intent.getAction().equals(MESSAGE_ALERT_INTENT")
                val message = intent.extras!!.getString(Constants.MESSAGE_EXTRA)
                val title = intent.extras!!.getString(Constants.MESSAGE_TITLE_EXTRA)

                val s = SpannableString(message) // msg should have url to enable clicking
                Linkify.addLinks(s, Linkify.ALL)

                val builder = android.app.AlertDialog.Builder(this@MainActivity)
                builder.setMessage(s)
                    .setPositiveButton("ok", null)
                    .setTitle(title)
                val alertDialog = builder.create()
                alertDialog.show()

                (alertDialog.findViewById<View>(android.R.id.message) as TextView).movementMethod =
                    LinkMovementMethod.getInstance()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        mToolbar!!.background.alpha = 128
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConstantsUI.MESSAGE_INTENT)
        intentFilter.addAction(Constants.MESSAGE_ALERT_INTENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mMessageReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(mMessageReceiver, intentFilter)
        }


        if (SDCardUtils.isSDCardUsedAndExtracted(this)) {
            val builder = android.app.AlertDialog.Builder(this@MainActivity)
            builder.setMessage(com.nextgis.maplibui.R.string.no_sd_card_attention)
                .setPositiveButton(com.nextgis.maplibui.R.string.ok, null)
                .setTitle(com.nextgis.maplibui.R.string.sd_card)
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (null != mLayersFragment && !mLayersFragment!!.isDrawerOpen) {
            val hasUnfinishedTracks = TrackerService.hasUnfinishedTracks(this)
            val title =
                if (hasUnfinishedTracks) com.nextgis.maplibui.R.string.track_stop else com.nextgis.maplibui.R.string.track_start
            val icon =
                if (hasUnfinishedTracks) com.nextgis.maplibui.R.drawable.ic_action_maps_directions_walk_rec else com.nextgis.maplibui.R.drawable.ic_action_maps_directions_walk
            setTrackItem(menu.findItem(R.id.menu_track), title, icon)
        }

        if (mapFragment!!.isEditMode) showEditToolbar()

        val log = menu.findItem(R.id.menu_share_log)
        log?.setVisible(mPreferences.getBoolean("save_log", false))

        return super.onPrepareOptionsMenu(menu)
    }


    override fun onPause() {
        try {
            if (mMessageReceiver != null) {
                unregisterReceiver(mMessageReceiver)
                mMessageReceiver = null
            }
        } catch (ignored: Exception) {
        }

        super.onPause()
    }

    override fun onBackPressed() {
        if (finishFragment()) return

        if (mBackPressed + 2000 > System.currentTimeMillis()) super.onBackPressed()
        else Toast.makeText(this, R.string.press_aback_again, Toast.LENGTH_SHORT).show()

        mBackPressed = System.currentTimeMillis()
    }

    override fun onLocationChanged(location: Location) {
    }

    override fun onBestLocationChanged(location: Location) {
    }


    override fun onGpsStatusChanged(event: Int) {
    }


    fun setActionBarState(state: Boolean) {
        mLayersFragment!!.isDrawerToggleEnabled = state

        if (state) {
            mToolbar!!.background.alpha = 128
            bottomToolbar.background.alpha = 128
        } else {
            mToolbar!!.background.alpha = 255
            bottomToolbar.background.alpha = 255
        }
    }


    fun hideBottomBar() {
        mapFragment!!.hideBottomBar()
    }


    fun restoreBottomBar(mode: Int) {
        if (mapFragment!!.isAdded) mapFragment!!.restoreBottomBar(mode)
    }

    fun setSubtitle(subtitle: String?) {
        mToolbar!!.subtitle = subtitle
    }

    fun requestPermissions(
        activity1: Activity, title: Int, message: Int, requestCode: Int,
        nextLevelOndeny: Int,
        vararg permissions: String?
    ) {
        val activity = activity1
        if (true) {
            val builder = AlertDialog.Builder(activity).setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                    com.nextgis.maplibui.R.string.allow
                ) { dialog: DialogInterface?, which: Int ->
                    ActivityCompat.requestPermissions(
                        activity,
                        permissions,
                        requestCode
                    )
                }
                .setNegativeButton(
                    com.nextgis.maplibui.R.string.deny
                ) { dialog: DialogInterface?, which: Int ->
                    if (nextLevelOndeny > 0) processAllPermisions(nextLevelOndeny)
                }
                .create()
            builder.setCanceledOnTouchOutside(false)
            builder.show()
        }
    }

    companion object {
        protected const val PERMISSIONS_REQUEST_ZERO: Int = 0
        protected const val PERMISSIONS_REQUEST_LOC: Int = 1
        protected const val PERMISSIONS_REQUEST_ACCOUNT: Int = 2
        protected const val PERMISSIONS_REQUEST_MEMORY: Int = 3
        protected const val PERMISSIONS_REQUEST_PUSH: Int = 4

        protected const val PERMISSIONS_REQUEST_LOC_SILENT: Int = 6
        const val LOCATION_BACKGROUND_REQUEST: Int = 5
        protected const val TAG_FRAGMENT_PROGRESS: String = "layer_fill_dialog_fragment"

        protected const val FILE_SELECT_CODE: Int = 555
        protected const val RELOAD_ACTIVITY_DATA: Int = 777
    }
}
