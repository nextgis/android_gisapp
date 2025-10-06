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
package com.nextgis.mobile.fragment

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.getbase.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nextgis.maplib.api.GpsEventListener
import com.nextgis.maplib.api.ILayer
import com.nextgis.maplib.api.ILayerView
import com.nextgis.maplib.datasource.Feature
import com.nextgis.maplib.datasource.GeoEnvelope
import com.nextgis.maplib.datasource.GeoGeometry
import com.nextgis.maplib.datasource.GeoGeometryFactory
import com.nextgis.maplib.datasource.GeoLineString
import com.nextgis.maplib.datasource.GeoLinearRing
import com.nextgis.maplib.datasource.GeoMultiLineString
import com.nextgis.maplib.datasource.GeoMultiPoint
import com.nextgis.maplib.datasource.GeoMultiPolygon
import com.nextgis.maplib.datasource.GeoPoint
import com.nextgis.maplib.datasource.GeoPolygon
import com.nextgis.maplib.display.SimpleFeatureRenderer
import com.nextgis.maplib.location.GpsEventSource
import com.nextgis.maplib.map.MLP.MLGeometryEditClass
import com.nextgis.maplib.map.MapDrawable
import com.nextgis.maplib.map.MaplibreMapInteraction
import com.nextgis.maplib.map.VectorLayer
import com.nextgis.maplib.util.Constants
import com.nextgis.maplib.util.FileUtil
import com.nextgis.maplib.util.GeoConstants
import com.nextgis.maplib.util.LocationUtil
import com.nextgis.maplib.util.MapUtil
import com.nextgis.maplibui.api.EditEventListener
import com.nextgis.maplibui.api.ILayerUI
import com.nextgis.maplibui.api.IVectorLayerUI
import com.nextgis.maplibui.api.MapViewEventListener
import com.nextgis.maplibui.dialog.ChooseLayerDialog
import com.nextgis.maplibui.fragment.CompassFragment
import com.nextgis.maplibui.mapui.MapViewOverlays
import com.nextgis.maplibui.overlay.CurrentLocationOverlay
import com.nextgis.maplibui.overlay.CurrentTrackOverlay
import com.nextgis.maplibui.overlay.EditLayerOverlay
import com.nextgis.maplibui.overlay.RulerOverlay
import com.nextgis.maplibui.overlay.RulerOverlay.OnRulerChanged
import com.nextgis.maplibui.overlay.UndoRedoOverlay
import com.nextgis.maplibui.service.WalkEditService
import com.nextgis.maplibui.util.ConstantsUI
import com.nextgis.maplibui.util.ControlHelper
import com.nextgis.maplibui.util.NotificationHelper
import com.nextgis.maplibui.util.SettingsConstantsUI
import com.nextgis.mobile.MainApplication
import com.nextgis.mobile.R
import com.nextgis.mobile.activity.MainActivity
import com.nextgis.mobile.util.AppConstants
import com.nextgis.mobile.util.AppSettingsConstants
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.MultiPoint
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.io.IOException
import java.util.Locale
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Main map fragment
 */
class MapFragment

    : Fragment(), MapViewEventListener, GpsEventListener, EditEventListener,
    View.OnClickListener, OnRulerChanged, OnMapReadyCallback , MaplibreMapInteraction {
    protected var mTolerancePX: Float = 0f

    protected var mPreferences: SharedPreferences? = null
    protected var mApp: MainApplication? = null
    protected var mActivity: MainActivity? = null

    var mMap: MapViewOverlays? = null


    protected var mivZoomIn: FloatingActionButton? = null
    protected var mivZoomOut: FloatingActionButton? = null
    protected var mRuler: FloatingActionButton? = null
    protected var mAddNewGeometry: FloatingActionButton? = null
    protected var mAddPointButton: FloatingActionButton? = null

    protected var mStatusSource: TextView? = null
    protected var mStatusAccuracy: TextView? = null
    protected var mStatusSpeed: TextView? = null
    protected var mStatusAltitude: TextView? = null
    protected var mStatusLatitude: TextView? = null
    protected var mStatusLongitude: TextView? = null
    protected var mZoom: TextView? = null
    protected var mStatusPanel: FrameLayout? = null
    protected var mScaleRulerLayout: LinearLayout? = null
    protected var mScaleRulerText: TextView? = null

    //, mZoomLevel;
    protected var mScaleRuler: ImageView? = null

    protected var mMapRelativeLayout: RelativeLayout? = null
    protected var mGpsEventSource: GpsEventSource? = null
    protected var mMainButton: View? = null
    var mode: Int = 0
        protected set
    protected var mCurrentLocationOverlay: CurrentLocationOverlay? = null
    protected var mCurrentTrackOverlay: CurrentTrackOverlay? = null
    var editLayerOverlay: EditLayerOverlay? = null
        protected set
    var undoRedoOverlay: UndoRedoOverlay? = null
        protected set
    protected var mRulerOverlay: RulerOverlay? = null
    protected var mCurrentCenter: GeoPoint? = null
    protected var mSelectedLayer: VectorLayer? = null

    protected var mCoordinatesFormat: Int = 0
    protected var mCoordinatesFraction: Int = 0
    protected var mChooseLayerDialog: ChooseLayerDialog? = null
    protected var mGPSDialog: AlertDialog? = null
    protected var mVibrator: Vibrator? = null

    protected var mIsCompassDragging: Boolean = false
    protected var mStatusPanelMode: Int = 0
    protected var mModeListener: onModeChange? = null
    public var mFinishListener: View.OnClickListener? = null

    protected val ADD_CURRENT_LOC: Int = 1
    protected val ADD_GEOMETRY_BY_WALK: Int = 3
    protected val ADD_POINT_BY_TAP: Int = 4
    private var mNeedSave = false

    interface onModeChange {
        fun onModeChangeListener()
    }

    fun setOnModeChangeListener(listener: onModeChange?) {
        mModeListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mActivity = activity as MainActivity?
        mTolerancePX = mActivity!!.resources.displayMetrics.density * ConstantsUI.TOLERANCE_DP

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity)
        mApp = mActivity!!.application as MainApplication
        mVibrator = mActivity!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mGpsEventSource = mApp!!.gpsEventSource

        mMap = MapViewOverlays(mActivity, mApp!!.map as MapDrawable)
        mMap!!.id = R.id.map_view

        editLayerOverlay = EditLayerOverlay(mActivity, mMap)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        mCurrentLocationOverlay = CurrentLocationOverlay(mActivity, mMap)
        mCurrentLocationOverlay!!.setStandingMarker(R.mipmap.ic_location_standing)
        mCurrentLocationOverlay!!.setMovingMarker(R.mipmap.ic_location_moving)
        mCurrentLocationOverlay!!.setAutopanningEnabled(true)

        mCurrentTrackOverlay = CurrentTrackOverlay(mActivity, mMap)
        mRulerOverlay = RulerOverlay(mActivity, mMap)
        undoRedoOverlay = UndoRedoOverlay(mActivity, mMap)

        mMap!!.addOverlay(mCurrentTrackOverlay)
        mMap!!.addOverlay(mCurrentLocationOverlay)
        mMap!!.addOverlay(editLayerOverlay)
        mMap!!.addOverlay(undoRedoOverlay)
        mMap!!.addOverlay(mRulerOverlay)

        //search relative view of map, if not found - add it
        mMapRelativeLayout = view.findViewById(R.id.maprl)
        if (mMapRelativeLayout != null) {
            mMapRelativeLayout!!.addView(
                mMap, 0, RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
            )
        }


        var mapZoom = try {
            mPreferences!!.getFloat(SettingsConstantsUI.KEY_PREF_ZOOM_LEVEL, mMap!!.minZoom)
        } catch (e: ClassCastException) {
            mMap!!.minZoom
        }

        var mapScrollX: Double
        var mapScrollY: Double
        try {
            mapScrollX = java.lang.Double.longBitsToDouble(
                mPreferences!!.getLong(
                    SettingsConstantsUI.KEY_PREF_SCROLL_X,
                    0
                )
            )
            mapScrollY = java.lang.Double.longBitsToDouble(
                mPreferences!!.getLong(
                    SettingsConstantsUI.KEY_PREF_SCROLL_Y,
                    0
                )
            )
        } catch (e: ClassCastException) {
            mapScrollX = 0.0
            mapScrollY = 0.0
        }
        mMap!!.setZoomAndCenter(mapZoom, GeoPoint(mapScrollX, mapScrollY))

        mMainButton = view.findViewById(R.id.multiple_actions)
        mAddPointButton = view.findViewById(R.id.add_point_by_tap)
        mAddPointButton?.setOnClickListener(this)

        val addCurrentLocation = view.findViewById<View>(R.id.add_current_location)
        addCurrentLocation.setOnClickListener(this)

        mAddNewGeometry = view.findViewById(R.id.add_new_geometry)
        mAddNewGeometry?.setOnClickListener(this)
        mRuler = view.findViewById(R.id.action_ruler)
        mRuler?.setOnClickListener(this)

        val addGeometryByWalk = view.findViewById<View>(R.id.add_geometry_by_walk)
        addGeometryByWalk.setOnClickListener(this)

        mivZoomIn = view.findViewById(R.id.action_zoom_in)
        mivZoomIn?.setOnClickListener(this)

        mivZoomOut = view.findViewById(R.id.action_zoom_out)
        mivZoomOut?.setOnClickListener(this)

        mStatusPanel = view.findViewById(R.id.fl_status_panel)
        mScaleRuler = view.findViewById(R.id.iv_ruler)
        mScaleRulerText = view.findViewById(R.id.tv_ruler)
        mScaleRulerText?.setText(rulerText)

        //        mZoomLevel = view.findViewById(R.id.tv_zoom_level);
//        mZoomLevel.setText(getZoomText());
        if (mZoom != null) mZoom!!.text = zoomText

        mScaleRulerLayout = view.findViewById(R.id.ll_ruler)
        drawScaleRuler()

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapViewMaplibre = view.findViewById(R.id.mapViewMaplibre) as org.maplibre.android.maps.MapView
        mMap!!.map!!.maplibreMapView = mapViewMaplibre
        mapViewMaplibre.onCreate(savedInstanceState)

        mapViewMaplibre.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapLibreMap) {

        mMap!!.map!!.setMapFragment(this)

        val mapboxMaplibre = mapboxMap
        mMap!!.map!!.maplibreMap = mapboxMaplibre

        val styleJson = loadJsonFromAssets(requireContext(), "ngwstyle.json")
        val layers = mMap!!.getVectorLayersByType(GeoConstants.GTAnyCheck)

        mMap!!.map!!.loadLayersToMaplibreMap(styleJson, layers)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        if (undoRedoOverlay != null) undoRedoOverlay!!.defineUndoRedo()

        if (editLayerOverlay != null) editLayerOverlay!!.setHasEdits(editLayerOverlay!!.hasEdits())
    }

    fun restartGpsListener() {
        mGpsEventSource!!.removeListener(this)
        mGpsEventSource!!.addListener(this)
    }

    val isEditMode: Boolean
        get() = mode == MODE_EDIT || mode == MODE_EDIT_BY_WALK || mode == MODE_EDIT_BY_TOUCH

    fun onOptionsItemSelected(id: Int): Boolean {
        val result: Boolean
        when (id) {
            android.R.id.home -> {
                cancelEdits()
                return true
            }

            0 -> {
                mMap!!.isLockMap = false
                setMode(MODE_EDIT)
                return true
            }

            com.nextgis.maplibui.R.id.menu_edit_by_touch -> {
                setMode(MODE_EDIT_BY_TOUCH)
                result = editLayerOverlay!!.onOptionsItemSelected(id)
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_undo, com.nextgis.maplibui.R.id.menu_edit_redo -> {
                result = undoRedoOverlay!!.onOptionsItemSelected(id)
                if (result) {
                    val undoRedoFeature = undoRedoOverlay!!.feature
                    val feature = editLayerOverlay!!.selectedFeature
                    feature.geometry = undoRedoFeature.geometry
                    editLayerOverlay!!.fillDrawItems(undoRedoFeature.geometry)

                    val original = mSelectedLayer!!.getGeometryForId(feature.id)
                    val hasEdits = original != null && undoRedoFeature.geometry == original

                    editLayerOverlay!!.setHasEdits(!hasEdits)
                    mMap!!.map!!.replaceGeometryFromHistoryChanges(feature.geometry)
                    mMap!!.map!!.updateMarkerByEditObject();
                    mMap!!.buffer()
                    mMap!!.postInvalidate()
                }
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_by_walk -> {
                setMode(MODE_EDIT_BY_WALK)
                result = editLayerOverlay!!.onOptionsItemSelected(id)
                if (result) undoRedoOverlay!!.saveToHistory(editLayerOverlay!!.selectedFeature)
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_delete_point  ->{
                val result = mMap!!.map!!.deleteCurrentPoint();
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_delete_line  ->{
                val result = mMap!!.map!!.deleteCurrentLine();
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_add_new_line  ->{
                val center = mMap!!.map!!.maplibreMap.cameraPosition.target
                val result = mMap!!.map!!.addNewLine(center, mMap!!.map!!.maplibreMap.getProjection());
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_add_new_point  ->{
                val center = mMap!!.map!!.maplibreMap.cameraPosition.target
                val result = mMap!!.map!!.addNewPoint(center);
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_add_new_inner_ring  ->{
                val center = mMap!!.map!!.maplibreMap.cameraPosition.target
                val result = mMap!!.map!!.addHole( center, mMap!!.map!!.maplibreMap.getProjection());
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_delete_inner_ring  ->{
                val result = mMap!!.map!!.deleteCurrentHole();
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_delete_polygon  ->{
                val result = mMap!!.map!!.deleteCurrentPolygon();
                return result
            }

            com.nextgis.maplibui.R.id.menu_edit_add_new_polygon  ->{
                val center = mMap!!.map!!.maplibreMap.cameraPosition.target
                val result = mMap!!.map!!.addNewPolygon(center, mMap!!.map!!.maplibreMap.getProjection());
                return result
            }

//            com.nextgis.maplibui.R.id.menu_edit_  ->{
//                val result = mMap!!.map!!.deleteCurrentPoin();
//                return result
//            }

            com.nextgis.maplibui.R.id.menu_edit_move_point_to_center  ->{
                val center = mMap!!.map!!.maplibreMap.cameraPosition.target
                return mMap!!.map!!.moveToPoint(center);
            }

            com.nextgis.maplibui.R.id.menu_edit_move_point_to_current_location  ->{

                if (mCurrentCenter != null) {
                    val latlng = convert3857To4326(mCurrentCenter!!.x, mCurrentCenter!!.y)
                    return mMap!!.map!!.moveToPoint(LatLng(latlng[1], latlng[0]));
                }
//                if (mCurrentLocationOverlay != null && mCurrentLocationOverlay!!.currentLocation  != null) {
//                    val latLng =  LatLng(mCurrentLocationOverlay!!.currentLocation.latitude,mCurrentLocationOverlay!!.currentLocation.longitude);
//                    return mMap!!.map!!.moveToPoint(latLng);
//                }
                return false;
            }


            else -> {
                result = editLayerOverlay!!.onOptionsItemSelected(id)
                if (result) undoRedoOverlay!!.saveToHistory(editLayerOverlay!!.selectedFeature)
                return result
            }
        }
        return false
    }


    fun saveEdits(): Boolean {
        val feature = editLayerOverlay!!.selectedFeature
        var featureId = Constants.NOT_FOUND.toLong()
        var geometry: GeoGeometry? = null

        if (mode == MODE_EDIT_BY_WALK) {
            editLayerOverlay!!.stopGeometryByWalk()
            setMode(MODE_EDIT)
            undoRedoOverlay!!.clearHistory()
            undoRedoOverlay!!.defineUndoRedo()
            return true
        }

        if (feature != null) {
            geometry = feature.geometry
            featureId = feature.id
        }

        if (geometry == null || !geometry.isValid) {
            Toast.makeText(
                context,
                com.nextgis.maplibui.R.string.not_enough_points,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (MapUtil.isGeometryIntersects(context, geometry)) return false

        mMap!!.isLockMap = false
        editLayerOverlay!!.setHasEdits(false)

        if (mode == MODE_EDIT_BY_TOUCH) {
            setMode(MODE_EDIT)
            undoRedoOverlay!!.clearHistory()
            undoRedoOverlay!!.defineUndoRedo()
        }

        if (mSelectedLayer != null) {
            if (featureId == Constants.NOT_FOUND.toLong()) {
                //show attributes edit activity
                val vectorLayerUI = mSelectedLayer as IVectorLayerUI
                vectorLayerUI.showEditForm(mActivity, featureId, geometry)
            } else {
                var uri =  Uri.parse("content://" + mApp!!.authority + "/" + mSelectedLayer!!.path.name)
                uri = ContentUris.withAppendedId(uri!!, featureId)
                val values = ContentValues()

                try {
                    values.put(Constants.FIELD_GEOM, geometry.toBlob())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                mActivity!!.contentResolver.update(uri, values, null, null)

                mMap!!.map!!.cancelFeatureEdit(false)
                setMode(MODE_SELECT_ACTION)

            }
        }

        return true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (mode == MODE_INFO || resultCode != Activity.RESULT_OK) {
            editLayerOverlay!!.setHasEdits(true)
            return
        }

        if (requestCode == IVectorLayerUI.MODIFY_REQUEST && data != null) {
            val id = data.getLongExtra(ConstantsUI.KEY_FEATURE_ID, Constants.NOT_FOUND.toLong())

            if (id != Constants.NOT_FOUND.toLong()) {
                editLayerOverlay!!.setSelectedFeature(id)
                if (mSelectedLayer != null) mSelectedLayer!!.showFeature(id)
                setMode(MODE_SELECT_ACTION)

                mMap!!.map!!.finishCreateNewFeature(id)




            }
        } else if (editLayerOverlay!!.selectedFeatureGeometry != null) editLayerOverlay!!.setHasEdits(
            true
        )
    }

    fun hasEdits(): Boolean {
        return editLayerOverlay != null && editLayerOverlay!!.hasEdits()
    }


    fun cancelEdits() {
//        if (mEditLayerOverlay.hasEdits()) TODO prompt dialog
//            return;

        // restore

        editLayerOverlay!!.panStop()
        panStop()

        editLayerOverlay!!.setHasEdits(false)
        if (mode == MODE_EDIT_BY_WALK) {
            editLayerOverlay!!.stopGeometryByWalk() // TODO toast?
            setMode(MODE_EDIT)
            undoRedoOverlay!!.clearHistory()
            undoRedoOverlay!!.defineUndoRedo()
        }
        val featureId = editLayerOverlay!!.selectedFeatureId
        editLayerOverlay!!.setSelectedFeature(featureId)
        mMap!!.map!!.cancelFeatureEdit(featureId != -1L)
        setMode(MODE_SELECT_ACTION)
    }

    fun setMode(mode: Int, vararg readOnly: Boolean) {
        var promt = ""
        when(mode){
            0 ->  promt=  "MODE_NORMAL"
            1 ->  promt=  "MODE_SELECT_ACTION"
            2 ->  promt=  "MODE_EDIT"
            3 ->  promt=  "MODE_INFO"
            4 ->  promt=  "MODE_EDIT_BY_WALK"
            5 ->  promt=  "MODE_EDIT_BY_TOUCH"
            6 ->  promt=  "MODE_SELECT_FOR_VIEW"
            else -> promt=  "MODE_ELSE"
        }
        Log.e("MMOODDEE", "mode set to " + promt);

        this.mode = mode

        hideMainButton()
        hideAddByTapButton()
        hideRulerButton()

        val toolbar = mActivity!!.bottomToolbar
        toolbar.background.alpha = 128
        toolbar.visibility = View.VISIBLE
        mActivity!!.showDefaultToolbar()

        if (mStatusPanelMode != 3) mStatusPanel!!.visibility = View.INVISIBLE

        when (mode) {
            MODE_NORMAL -> {
                if (mSelectedLayer != null) mSelectedLayer!!.isLocked = false

                mSelectedLayer = null
                toolbar.visibility = View.GONE
                showMainButton()
                showRulerButton()
                if (mStatusPanelMode != 0) mStatusPanel!!.visibility = View.VISIBLE
                editLayerOverlay!!.showAllFeatures()
                editLayerOverlay!!.mode = EditLayerOverlay.MODE_NONE
                undoRedoOverlay!!.clearHistory()
                mMap!!.map!!.unselectFeatureFromView()
            }

            MODE_EDIT -> {
                if (mSelectedLayer == null) {
                    setMode(MODE_NORMAL)
                    return
                }

                mSelectedLayer!!.isLocked = true
                mActivity!!.showEditToolbar()
                editLayerOverlay!!.mode = EditLayerOverlay.MODE_EDIT
                toolbar.setOnMenuItemClickListener { menuItem ->
                    onOptionsItemSelected(
                        menuItem.itemId
                    )
                }
            }

            MODE_EDIT_BY_WALK -> {
                mSelectedLayer!!.isLocked = true
                mActivity!!.showEditToolbar()
                editLayerOverlay!!.mode = EditLayerOverlay.MODE_EDIT_BY_WALK
                undoRedoOverlay!!.clearHistory()
            }

            MODE_EDIT_BY_TOUCH -> {
                mSelectedLayer!!.isLocked = true
                mActivity!!.showEditToolbar()
                editLayerOverlay!!.mode = EditLayerOverlay.MODE_EDIT_BY_TOUCH
                toolbar.setOnMenuItemClickListener { menuItem ->
                    onOptionsItemSelected(
                        menuItem.itemId
                    )
                }
            }

            MODE_SELECT_ACTION -> {
                if (mSelectedLayer == null) {
                    setMode(MODE_NORMAL)
                    return
                }

                mSelectedLayer!!.isLocked = true
                toolbar.title = null
                toolbar.menu.clear()
                toolbar.inflateMenu(R.menu.select_action)
                toolbar.menu.findItem(R.id.menu_feature_edit).setEnabled(false)
                toolbar.setNavigationIcon(com.nextgis.maplibui.R.drawable.ic_action_cancel_dark)

                mFinishListener = View.OnClickListener {
                    setMode(MODE_NORMAL)
                }
                toolbar.setNavigationOnClickListener(mFinishListener)

                toolbar.setOnMenuItemClickListener(
                    Toolbar.OnMenuItemClickListener { item ->
                        if (mSelectedLayer == null) return@OnMenuItemClickListener false
                        when (item.itemId) {
                            R.id.menu_feature_add -> {

                                editLayerOverlay!!.selectedFeature = Feature()
                                editLayerOverlay!!.createNewGeometry()
                                undoRedoOverlay!!.clearHistory()
                                setMode(MODE_EDIT)
                                undoRedoOverlay!!.saveToHistory(editLayerOverlay!!.selectedFeature)
                                editLayerOverlay!!.setHasEdits(true)

                                mMap!!.map!!.startFeatureSelectionForEdit(mSelectedLayer!!.id, mSelectedLayer!!.geometryType,
                                    editLayerOverlay!!.selectedFeature, true)



                            }

                            R.id.menu_feature_edit -> {
                                setMode(MODE_EDIT)
                                undoRedoOverlay!!.saveToHistory(editLayerOverlay!!.selectedFeature)
                                editLayerOverlay!!.setHasEdits(false)
                                if(mSelectedLayer!= null)
                                    mMap!!.map!!.startFeatureSelectionForEdit(mSelectedLayer!!.id, mSelectedLayer!!.geometryType,
                                        editLayerOverlay!!.selectedFeature, false)
                            }

                            R.id.menu_feature_delete -> deleteFeature()
                            R.id.menu_feature_attributes -> setMode(MODE_INFO)
                        }
                        true
                    })

                editLayerOverlay!!.mode = EditLayerOverlay.MODE_HIGHLIGHT
                undoRedoOverlay!!.clearHistory()
            }

            MODE_SELECT_FOR_VIEW -> {
                if (mSelectedLayer == null) {
                    setMode(MODE_NORMAL)
                    return
                }

                //mSelectedLayer.setLocked(true);
                toolbar.title = null
                toolbar.menu.clear()
                toolbar.inflateMenu(R.menu.select_action)
                toolbar.menu.findItem(R.id.menu_feature_edit).setEnabled(false)
                toolbar.setNavigationIcon(com.nextgis.maplibui.R.drawable.ic_action_cancel_dark)

                mFinishListener = View.OnClickListener { setMode(MODE_NORMAL) }
                toolbar.setNavigationOnClickListener(mFinishListener)

                toolbar.setOnMenuItemClickListener(
                    Toolbar.OnMenuItemClickListener { item ->
                        if (mSelectedLayer == null) return@OnMenuItemClickListener false
                        when (item.itemId) {
                            R.id.menu_feature_attributes -> setMode(
                                MODE_INFO,
                                true
                            )
                        }
                        true
                    })

                editLayerOverlay!!.mode = EditLayerOverlay.MODE_HIGHLIGHT
                undoRedoOverlay!!.clearHistory()
            }

            MODE_INFO -> {
                if (mSelectedLayer == null) {
                    setMode(MODE_NORMAL)
                    return
                }
                var readOnlyModeValue = false
                if (readOnly.size > 0) readOnlyModeValue = readOnly[0]

                mSelectedLayer!!.isLocked = if (readOnlyModeValue) false else true
                val tabletSize = resources.getBoolean(R.bool.isTablet)
                val fragmentManager = mActivity!!.supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                //get or create fragment
                val attributesFragment = getAttributesFragment(fragmentManager)

                val attrBundle = Bundle()
                attrBundle.putBoolean(AttributesFragment.KEY_READ_ONLY, readOnlyModeValue)
                attributesFragment.arguments = attrBundle
                attributesFragment.isTablet = tabletSize
                var container = R.id.mainview

                if (attributesFragment.isTablet) {
                    container = R.id.fl_attributes
                } else {
                    val hide = fragmentManager.findFragmentById(R.id.map)
                    fragmentTransaction.hide(hide!!)
                }

                if (!attributesFragment.isAdded) {
                    fragmentTransaction.add(container, attributesFragment, "ATTRIBUTES")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

                    if (!attributesFragment.isTablet) fragmentTransaction.addToBackStack(null)
                }

                if (!attributesFragment.isVisible) {
                    fragmentTransaction.show(attributesFragment)
                }

                fragmentTransaction.commit()

                attributesFragment.setSelectedFeature(
                    mSelectedLayer,
                    editLayerOverlay!!.selectedFeatureId
                )
                attributesFragment.setToolbar(toolbar, editLayerOverlay, readOnlyModeValue)

                mFinishListener = View.OnClickListener { view ->
                    (activity as MainActivity).finishFragment()
                    if (attributesFragment.isTablet) activity!!.supportFragmentManager.beginTransaction()
                        .remove(attributesFragment).commit()
                    if (view == null) setMode(MODE_NORMAL)
                }

                toolbar.setNavigationIcon(com.nextgis.maplibui.R.drawable.ic_action_cancel_dark)
                toolbar.setNavigationOnClickListener(mFinishListener)
            }
        }

        if (mModeListener != null) mModeListener!!.onModeChangeListener()

        setMarginsToPanel()
        defineMenuItems()
    }

    private fun getAttributesFragment(fragmentManager: FragmentManager): AttributesFragment {
        var attributesFragment =
            fragmentManager.findFragmentByTag("ATTRIBUTES") as AttributesFragment?
        if (null == attributesFragment) attributesFragment = AttributesFragment()

        return attributesFragment
    }

    protected fun defineMenuItems() {
        if (mode == MODE_NORMAL || mode == MODE_INFO) return

        if (mSelectedLayer == null) {
            setMode(MODE_NORMAL)
            return
        }


        val noFeature = editLayerOverlay!!.selectedFeatureGeometry == null
        val featureId = editLayerOverlay!!.selectedFeatureId

        var featureName: String? =
            String.format(getString(com.nextgis.maplibui.R.string.feature_n), featureId)
        val labelField = mSelectedLayer!!.preferences.getString(
            SettingsConstantsUI.KEY_PREF_LAYER_LABEL,
            Constants.FIELD_ID
        )!!
        if ((labelField != Constants.FIELD_ID) && !noFeature && featureId != Constants.NOT_FOUND.toLong()) {
            val feature = mSelectedLayer!!.getFeature(featureId)
            if (feature != null) featureName = feature.getFieldValueAsString(labelField)
        }

        featureName =
            if (noFeature) getString(com.nextgis.maplibui.R.string.nothing_selected) else if (featureId == Constants.NOT_FOUND.toLong()) getString(
                com.nextgis.maplibui.R.string.new_feature
            ) else featureName
        mActivity!!.title = featureName
        mActivity!!.setSubtitle(mSelectedLayer!!.name)

        val hasSelectedFeature = editLayerOverlay!!.selectedFeature != null && !noFeature
        val toolbar = mActivity!!.bottomToolbar
        for (i in 0..<toolbar.menu.size()) {
            var item = toolbar.menu.findItem(R.id.menu_feature_delete)
            if (item != null) ControlHelper.setEnabled(
                item,
                hasSelectedFeature && mode != MODE_SELECT_FOR_VIEW
            )

            item = toolbar.menu.findItem(R.id.menu_feature_edit)
            if (item != null) ControlHelper.setEnabled(
                item,
                hasSelectedFeature && mode != MODE_SELECT_FOR_VIEW
            )

            item = toolbar.menu.findItem(R.id.menu_feature_attributes)
            if (item != null) ControlHelper.setEnabled(item, hasSelectedFeature)

            item = toolbar.menu.findItem(R.id.menu_feature_add)
            if (mode == MODE_SELECT_FOR_VIEW) ControlHelper.setEnabled(item, false)
        }
    }







    override fun onDestroyView() {
        if (mMap != null) {
            mMap!!.removeListener(this)
            mMap!!.map.clearMapListeners()
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout!!.removeView(mMap)
            }
        }

        super.onDestroyView()
    }


    protected fun drawScaleRuler() {
        val px =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 10f, resources.displayMetrics)
                .toInt()
        val notch =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1f, resources.displayMetrics)
                .toInt()
        val ruler = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(ruler)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = ContextCompat.getColor(activity!!, com.nextgis.maplibui.R.color.primary_dark)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(0f, px.toFloat(), px.toFloat(), px.toFloat(), paint)
        canvas.drawLine(0f, px.toFloat(), 0f, 0f, paint)
        canvas.drawLine(0f, 0f, notch.toFloat(), 0f, paint)
        canvas.drawLine(px.toFloat(), px.toFloat(), px.toFloat(), (px - notch).toFloat(), paint)
        mScaleRuler!!.setImageBitmap(ruler)
    }


    protected fun showMapButtons(
        show: Boolean,
        rl: RelativeLayout?
    ) {
        if (null == rl) {
            return
        }
        var v = rl.findViewById<View>(R.id.action_zoom_out)
        if (null != v) {
            if (show) {
                v.visibility = View.VISIBLE
            } else {
                v.visibility = View.GONE
            }
        }

        v = rl.findViewById(R.id.action_zoom_in)
        if (null != v) {
            if (show) {
                v.visibility = View.VISIBLE
            } else {
                v.visibility = View.GONE
            }
        }
    }


    override fun onLayerAdded(id: Int) {
    }


    override fun onLayerDeleted(id: Int) {
        setMode(MODE_NORMAL)
    }


    override fun onLayerChanged(id: Int) {

        Log.e("","");
        mMap!!.map!!.checkLayerVisibility(id);

    }


    override fun onExtentChanged(
        zoom: Float,
        center: GeoPoint
    ) {
        setZoomInEnabled(mMap!!.canZoomIn())
        setZoomOutEnabled(mMap!!.canZoomOut())
        mScaleRulerText!!.text = rulerText
        //        mZoomLevel.setText(getZoomText());
        if (mZoom != null) mZoom!!.text = zoomText
    }


    protected val zoomText: String
        get() = String.format("%.0fz", mMap!!.zoomLevel)


    protected val rulerText: String
        get() {
            var p1 =
                GeoPoint(mScaleRuler!!.left.toDouble(), mScaleRuler!!.bottom.toDouble())
            var p2 =
                GeoPoint(mScaleRuler!!.right.toDouble(), mScaleRuler!!.bottom.toDouble())
            p1 = mMap!!.map.screenToMap(p1)
            p2 = mMap!!.map.screenToMap(p2)
            p1.crs = GeoConstants.CRS_WEB_MERCATOR
            p2.crs = GeoConstants.CRS_WEB_MERCATOR
            val s = GeoLineString()
            s.add(p1)
            s.add(p2)

            return LocationUtil.formatLength(context, s.length, 1)
        }


    override fun onLayersReordered() {
    }


    override fun onLayerDrawFinished(
        id: Int,
        percent: Float
    ) {
        //Log.d(Constants.TAG, "onLayerDrawFinished: " + id + " percent " + percent);
        /*if (percent >= 1.0)
            mLayerDrawn++;
        MainActivity activity = (MainActivity) mActivity;
        if (null != activity){
            if (percent >= 1.0) {
                if (id == mMap.getTopVisibleLayerId()) {
                    activity.onRefresh(false, 0);
                } else {
                    activity.onRefresh(true, (mLayerDrawn * 100) / mMap.getVisibleLayerCount());
                }
            }
        }*/
        if (percent >= 1.0 && id == Constants.DRAW_FINISH_ID) {
            /** mMap.getMap().getId()) {finish id come at end */
            if (null != mActivity) {
                mActivity!!.onRefresh(false)
            }
        }
    }


    override fun onLayerDrawStarted() {
        if (null != mActivity) {
            mActivity!!.onRefresh(true)
        }
    }


    protected fun setZoomInEnabled(bEnabled: Boolean) {
        if (mivZoomIn == null) {
            return
        }

        mivZoomIn!!.isEnabled = bEnabled
    }


    protected fun setZoomOutEnabled(bEnabled: Boolean) {
        if (mivZoomOut == null) {
            return
        }
        mivZoomOut!!.isEnabled = bEnabled
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_KEY_IS_MEASURING, mRulerOverlay!!.isMeasuring)
        outState.putInt(KEY_MODE, mode)
        outState.putInt(
            BUNDLE_KEY_LAYER,
            if (null == mSelectedLayer) Constants.NOT_FOUND else mSelectedLayer!!.id
        )

        val feature = editLayerOverlay!!.selectedFeature
        outState.putLong(
            BUNDLE_KEY_FEATURE_ID,
            feature?.id ?: Constants.NOT_FOUND.toLong()
        )

        if (null != feature && feature.geometry != null) {
            try {
                outState.putByteArray(BUNDLE_KEY_SAVED_FEATURE, feature.geometry.toBlob())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    override fun onViewStateRestored(
        savedInstanceState: Bundle?
    ) {
        super.onViewStateRestored(savedInstanceState)
        if (null == savedInstanceState) {
            mode = MODE_NORMAL
        } else {
            mode = savedInstanceState.getInt(KEY_MODE)

            val layerId = savedInstanceState.getInt(BUNDLE_KEY_LAYER)
            val layer = mMap!!.getLayerById(layerId)
            var feature: Feature? = null

            if (null != layer && layer is VectorLayer) {
                mSelectedLayer = layer

                if (savedInstanceState.containsKey(BUNDLE_KEY_SAVED_FEATURE)) {
                    var geometry: GeoGeometry? = null

                    try {
                        geometry = GeoGeometryFactory.fromBlob(
                            savedInstanceState.getByteArray(
                                BUNDLE_KEY_SAVED_FEATURE
                            )
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    feature = Feature()
                    feature.id =
                        savedInstanceState.getLong(BUNDLE_KEY_FEATURE_ID)
                    feature.geometry = geometry
                }
            }

            editLayerOverlay!!.setSelectedLayer(mSelectedLayer)
            editLayerOverlay!!.selectedFeature = feature
        }

        if (WalkEditService.isServiceRunning(context)) {
            val preferences = context!!.getSharedPreferences(
                WalkEditService.TEMP_PREFERENCES,
                Context.MODE_MULTI_PROCESS
            )
            val layerId = preferences.getInt(ConstantsUI.KEY_LAYER_ID, Constants.NOT_FOUND)
            val featureId =
                preferences.getLong(ConstantsUI.KEY_FEATURE_ID, Constants.NOT_FOUND.toLong())
            val layer = mMap!!.map.getLayerById(layerId)
            if (layer != null && layer is VectorLayer) {
                mSelectedLayer = layer
                editLayerOverlay!!.setSelectedLayer(mSelectedLayer)

                if (featureId > Constants.NOT_FOUND) editLayerOverlay!!.setSelectedFeature(featureId)
                else editLayerOverlay!!.newGeometryByWalk()

                val geometry = GeoGeometryFactory.fromWKT(
                    preferences.getString(ConstantsUI.KEY_GEOMETRY, ""),
                    GeoConstants.CRS_WEB_MERCATOR
                )
                if (geometry != null) editLayerOverlay!!.setGeometryFromWalkEdit(geometry)

                mode = MODE_EDIT_BY_WALK
            }
        }

        setMode(mode)

        if (savedInstanceState != null && savedInstanceState.getBoolean(
                BUNDLE_KEY_IS_MEASURING,
                false
            )
        ) startMeasuring()
    }


    override fun onPause() {
        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay!!.stopShowingCurrentLocation()
        }
        if (null != mGpsEventSource) {
            mGpsEventSource!!.removeListener(this)
        }
        if (null != editLayerOverlay) {
            editLayerOverlay!!.removeListener(this)
            editLayerOverlay!!.onPause()
        }

        val edit = mPreferences!!.edit()
        if (null != mMap) {
            edit.putFloat(SettingsConstantsUI.KEY_PREF_ZOOM_LEVEL, mMap!!.zoomLevel)
            val point = mMap!!.mapCenter
            edit.putLong(
                SettingsConstantsUI.KEY_PREF_SCROLL_X,
                java.lang.Double.doubleToRawLongBits(point.x)
            )
            edit.putLong(
                SettingsConstantsUI.KEY_PREF_SCROLL_Y,
                java.lang.Double.doubleToRawLongBits(point.y)
            )

            mMap!!.removeListener(this)
        }
        edit.apply()

        super.onPause()
    }


    override fun onResume() {
        super.onResume()

        var showControls =
            mPreferences!!.getBoolean(AppSettingsConstants.KEY_PREF_SHOW_ZOOM_CONTROLS, true)
        showMapButtons(showControls, mMapRelativeLayout)

        if (Constants.DEBUG_MODE) Log.d(
            Constants.TAG,
            "KEY_PREF_SHOW_ZOOM_CONTROLS: " + (if (showControls) "ON" else "OFF")
        )

        showControls =
            mPreferences!!.getBoolean(AppSettingsConstants.KEY_PREF_SHOW_SCALE_RULER, true)
        if (showControls) mScaleRulerLayout!!.visibility = View.VISIBLE
        else mScaleRulerLayout!!.visibility = View.GONE

        showControls = mPreferences!!.getBoolean(AppSettingsConstants.KEY_PREF_SHOW_ZOOM, false)
        if (showControls) {
//            mZoomLevel.setVisibility(View.VISIBLE);
            if (mZoom != null) mZoom!!.visibility = View.VISIBLE
        } else {
//            mZoomLevel.setVisibility(View.GONE);
            if (mZoom != null) mZoom!!.visibility = View.GONE
        }

        showControls =
            mPreferences!!.getBoolean(AppSettingsConstants.KEY_PREF_SHOW_MEASURING, false)
        if (showControls) mRuler!!.visibility = View.VISIBLE
        else mRuler!!.visibility = View.GONE

        if (null != mMap) {
            mMap!!.map.setBackground(mApp!!.mapBackground)
            mMap!!.addListener(this)
        }

        val coordinatesFormat =
            mPreferences!!.getString(
                SettingsConstantsUI.KEY_PREF_COORD_FORMAT,
                Location.FORMAT_DEGREES.toString() + ""
            )!!
        mCoordinatesFormat =
            if (FileUtil.isIntegerParseInt(coordinatesFormat)) coordinatesFormat.toInt()
            else Location.FORMAT_DEGREES
        mCoordinatesFraction = mPreferences!!.getInt(
            SettingsConstantsUI.KEY_PREF_COORD_FRACTION,
            AppConstants.DEFAULT_COORDINATES_FRACTION_DIGITS
        )

        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay!!.updateMode(
                mPreferences!!.getString(
                    SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC,
                    "3"
                )
            )
            mCurrentLocationOverlay!!.startShowingCurrentLocation()
        }
        if (null != mGpsEventSource) {
            mGpsEventSource!!.addListener(this)
            if (mGPSDialog == null || !mGPSDialog!!.isShowing) mGPSDialog =
                NotificationHelper.showLocationInfo(
                    activity
                )
        }

        if (null != editLayerOverlay) {
            editLayerOverlay!!.addListener(this)
            editLayerOverlay!!.onResume()
        }

        try {
            val statusPanelModeStr =
                mPreferences!!.getString(SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL, "1")!!
            mStatusPanelMode =
                if (FileUtil.isIntegerParseInt(statusPanelModeStr)) statusPanelModeStr.toInt()
                else 0
        } catch (e: ClassCastException) {
            mStatusPanelMode = 0
            if (Constants.DEBUG_MODE) Log.d(
                Constants.TAG,
                "Previous version of KEY_PREF_SHOW_STATUS_PANEL of bool type. Let set it to 0"
            )
        }

        if (null != mStatusPanel) {
            if (mStatusPanelMode != 0) {
                mStatusPanel!!.visibility = View.VISIBLE
                fillStatusPanel(null)

                if (mode != MODE_NORMAL && mStatusPanelMode != 3) mStatusPanel!!.visibility =
                    View.INVISIBLE
            } else {
                mStatusPanel!!.removeAllViews()
            }

            setMarginsToPanel()
        }

        val showCompass =
            mPreferences!!.getBoolean(AppSettingsConstants.KEY_PREF_SHOW_COMPASS, true)
        checkCompass(showCompass)

        mCurrentCenter = null
    }


    protected fun setMarginsToPanel() {
        val toolbar = mActivity!!.bottomToolbar

        toolbar.post {
            val isToolbarVisible = toolbar.visibility == View.VISIBLE
            val isPanelVisible = mStatusPanel!!.visibility == View.VISIBLE
            val toolbarHeight = toolbar.measuredHeight

            val lp = mStatusPanel!!.layoutParams as RelativeLayout.LayoutParams
            var bottom = if (isToolbarVisible && isPanelVisible) toolbarHeight
            else 0

            lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, bottom)
            mStatusPanel!!.layoutParams = lp

            bottom = if (isToolbarVisible && !isPanelVisible) toolbarHeight
            else 0

            mStatusPanel!!.minimumHeight = bottom
            mStatusPanel!!.requestLayout()
        }
    }


    protected fun checkCompass(showCompass: Boolean) {
        val compassContainer = R.id.fl_compass
        val compass = mMapRelativeLayout!!.findViewById<FrameLayout>(compassContainer)

        if (!showCompass) {
            compass.visibility = View.GONE
            return
        }

        val fragmentManager = mActivity!!.supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        //get or create fragment
        var compassFragment =
            fragmentManager.findFragmentByTag("NEEDLE_COMPASS") as CompassFragment?
        if (null == compassFragment) compassFragment = CompassFragment()

        compass.isClickable = false
        compassFragment.setStyle(true)
        if (!compassFragment.isAdded) fragmentTransaction.add(
            compassContainer,
            compassFragment,
            "NEEDLE_COMPASS"
        )
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

        if (!compassFragment.isVisible) {
            fragmentTransaction.show(compassFragment)
        }

        fragmentTransaction.commit()

        compass.visibility = View.VISIBLE
        compass.setOnClickListener(this)
        compass.setOnLongClickListener {
            mIsCompassDragging = true
            mVibrator!!.vibrate(5)
            true
        }
        // Thanks to http://javatechig.com/android/how-to-drag-a-view-in-android
        compass.setOnTouchListener(object : OnTouchListener {
            private var _xDelta = 0
            private var _yDelta = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val X = event.rawX.toInt()
                val Y = event.rawY.toInt()
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val lParams = v.layoutParams as RelativeLayout.LayoutParams
                        _xDelta = X - lParams.leftMargin
                        _yDelta = Y - lParams.topMargin
                        return false
                    }

                    MotionEvent.ACTION_UP -> {
                        mIsCompassDragging = false
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!mIsCompassDragging) return false

                        val layoutParams = v.layoutParams as RelativeLayout.LayoutParams
                        val width = v.width
                        val height = v.height
                        var toolbarHeight = 0
                        if (mActivity!!.supportActionBar != null) toolbarHeight =
                            mActivity!!.supportActionBar!!
                                .height
                        if (X > width / 3 && X < v.rootView.width - width / 3) layoutParams.leftMargin =
                            X - _xDelta
                        if (Y > height / 2 + toolbarHeight && Y < v.rootView.height - height / 2) layoutParams.topMargin =
                            Y - _yDelta

                        v.layoutParams = layoutParams
                    }
                }
                mMapRelativeLayout!!.invalidate()
                return true
            }
        })
    }


    protected fun addNewGeometry() {
        mApp!!.sendEvent(ConstantsUI.GA_LAYER, ConstantsUI.GA_EDIT, ConstantsUI.GA_FAB)

        //show select layer dialog if several layers, else start default or custom form
        var layers = mMap!!.getVectorLayersByType(
            GeoConstants.GTPointCheck or GeoConstants.GTMultiPointCheck or
                    GeoConstants.GTLineStringCheck or GeoConstants.GTMultiLineStringCheck or
                    GeoConstants.GTPolygonCheck or GeoConstants.GTMultiPolygonCheck
        )
        layers = removeHideLayers(layers)
        if (layers.isEmpty()) {
            Toast.makeText(mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG)
                .show()
        } else if (layers.size == 1) {
            //open form
            val layer = layers[0] as VectorLayer

            mSelectedLayer = layer
            editLayerOverlay!!.setSelectedLayer(layer)
            setMode(MODE_SELECT_ACTION)

            Toast.makeText(
                mActivity,
                String.format(getString(R.string.edit_layer), layer.name),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (isDialogShown) return
            //open choose edit layer dialog
            mChooseLayerDialog = ChooseLayerDialog()
            mChooseLayerDialog!!.setLayerList(layers)
                .setCode(EDIT_LAYER)
                .setTitle(getString(com.nextgis.maplibui.R.string.choose_layers))
                .setTheme(mActivity!!.themeId) //.show(mActivity.getSupportFragmentManager(), "choose_layer");
                .show(childFragmentManager, ChooseLayerDialog.TAG)
        }
    }


    protected fun addPointByTap() {
        if (mSelectedLayer != null) mSelectedLayer!!.isLocked = false

        //show select layer dialog if several layers, else start default or custom form
        var layers =
            mMap!!.getVectorLayersByType(GeoConstants.GTPointCheck or GeoConstants.GTMultiPointCheck)
        layers = removeHideLayers(layers)
        if (layers.isEmpty()) {
            Toast.makeText(
                mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG
            )
                .show()
        } else if (layers.size == 1) {
            //open form
            val layer = layers[0] as VectorLayer

            mSelectedLayer = layer
            editLayerOverlay!!.setSelectedLayer(layer)
            createPointFromOverlay()

            Toast.makeText(
                mActivity,
                String.format(getString(R.string.edit_layer), layer.name),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (isDialogShown) return
            //open choose edit layer dialog
            mChooseLayerDialog = ChooseLayerDialog()
            mChooseLayerDialog!!.setLayerList(layers)
                .setCode(ADD_POINT_BY_TAP)
                .setTitle(getString(com.nextgis.maplibui.R.string.choose_layers))
                .setTheme(mActivity!!.themeId)
                .show(mActivity!!.supportFragmentManager, ChooseLayerDialog.TAG)
        }
    }

    protected fun createPointFromOverlay() {
        editLayerOverlay!!.selectedFeature = Feature()
        editLayerOverlay!!.selectedFeature.geometry = GeoPoint()
        setMode(MODE_EDIT)
        undoRedoOverlay!!.clearHistory()
        editLayerOverlay!!.createPointFromOverlay()
        editLayerOverlay!!.setHasEdits(true)
        undoRedoOverlay!!.saveToHistory(editLayerOverlay!!.selectedFeature)
    }

    protected fun addCurrentLocation() {
        //show select layer dialog if several layers, else start default or custom form
        var layers = mMap!!.getVectorLayersByType(
            GeoConstants.GTMultiPointCheck or GeoConstants.GTPointCheck
        )
        layers = removeHideLayers(layers)
        if (layers.isEmpty()) {
            Toast.makeText(
                mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG
            )
                .show()
        } else if (layers.size == 1) {
            //open form
            val vectorLayer = layers[0]
            if (vectorLayer is ILayerUI) {
                mSelectedLayer = vectorLayer as VectorLayer
                editLayerOverlay!!.setSelectedLayer(mSelectedLayer)
                val vectorLayerUI = vectorLayer as IVectorLayerUI
                vectorLayerUI.showEditForm(mActivity, Constants.NOT_FOUND.toLong(), null)

                Toast.makeText(
                    mActivity,
                    String.format(getString(R.string.edit_layer), vectorLayer.getName()),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    mActivity, getString(R.string.warning_no_edit_layers),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            if (isDialogShown) return
            //open choose dialog
            mChooseLayerDialog = ChooseLayerDialog()
            mChooseLayerDialog!!.setLayerList(layers)
                .setCode(ADD_CURRENT_LOC)
                .setTitle(getString(com.nextgis.maplibui.R.string.choose_layers))
                .setTheme(mActivity!!.themeId)
                .show(mActivity!!.supportFragmentManager, ChooseLayerDialog.TAG)
        }
    }

    protected fun removeHideLayers(layerList: MutableList<ILayer>): MutableList<ILayer> {
        var i = 0
        while (i < layerList.size) {
            val layerView = layerList[i] as ILayerView
            if (null != layerView) {
                if (!layerView.isVisible) {
                    layerList.removeAt(i)
                    i--
                }
            }
            i++
        }

        return layerList
    }


    protected fun addGeometryByWalk() {
        //show select layer dialog if several layers, else start default or custom form
        var layers = mMap!!.getVectorLayersByType(
            (GeoConstants.GTLineStringCheck or GeoConstants.GTPolygonCheck
                    or GeoConstants.GTMultiLineStringCheck or GeoConstants.GTMultiPolygonCheck)
        )
        layers = removeHideLayers(layers)

        if (layers.isEmpty()) {
            Toast.makeText(mActivity, getString(R.string.warning_no_edit_layers), Toast.LENGTH_LONG)
                .show()
        } else if (layers.size == 1) {
            //open form
            val layer = layers[0] as VectorLayer
            mSelectedLayer = layer
            editLayerOverlay!!.setSelectedLayer(layer)
            editLayerOverlay!!.newGeometryByWalk()
            setMode(MODE_EDIT_BY_WALK)

            Toast.makeText(
                mActivity,
                String.format(getString(R.string.edit_layer), layer.name),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (isDialogShown) return
            //open choose edit layer dialog
            mChooseLayerDialog = ChooseLayerDialog()
            mChooseLayerDialog!!.setLayerList(layers)
                .setCode(ADD_GEOMETRY_BY_WALK)
                .setTitle(getString(com.nextgis.maplibui.R.string.choose_layers))
                .setTheme(mActivity!!.themeId)
                .show(mActivity!!.supportFragmentManager, ChooseLayerDialog.TAG)
        }
    }


    fun onFinishChooseLayerDialog(
        code: Int,
        layer: ILayer?
    ) {
        val vectorLayer = layer as VectorLayer?
        if (layer == null) return  // TODO toast?


        if (mSelectedLayer != null) mSelectedLayer!!.isLocked = false

        mSelectedLayer = vectorLayer
        editLayerOverlay!!.setSelectedLayer(vectorLayer)

        if (code == ADD_CURRENT_LOC) {
            if (layer is ILayerUI) {
                val layerUI = layer as IVectorLayerUI
                layerUI.showEditForm(mActivity, Constants.NOT_FOUND.toLong(), null)
            }
        } else if (code == EDIT_LAYER) {
            setMode(MODE_SELECT_ACTION)
        } else if (code == ADD_GEOMETRY_BY_WALK) {
            editLayerOverlay!!.newGeometryByWalk()
            setMode(MODE_EDIT_BY_WALK)
        } else if (code == ADD_POINT_BY_TAP) {
            createPointFromOverlay()
        }
    }


    override fun processMapLongClick(clickeEnelope: GeoEnvelope, clickPoint: PointF): Boolean {
        onLongPressFromMaplibre(clickeEnelope, clickPoint)
        return true
    }

    override fun processMapClick(screenx: Float, screeny: Float): Boolean {    // x y - screen coordinates
        Log.e("CCCLLIICK", "screenX: " + screenx + " - " + " screeny: " + screeny)
        onSingleTapUpFromMaplibre(screenx, screeny)
        return true
    }

    fun onLongPressFromMaplibre(clickeEnelope: GeoEnvelope, clickPoint : PointF) {
        if (!(mode == MODE_NORMAL || mode == MODE_SELECT_ACTION) || mRulerOverlay!!.isMeasuring) {
            return
        }

        //  exactEnv = mMap!!.screenToMap(exactEnv)
        if (null == clickeEnelope) return
        val point = GeoPoint(clickeEnelope.maxX, clickeEnelope.minY)
        point.crs = GeoConstants.CRS_WEB_MERCATOR

        //show actions dialog
        val layers = mMap!!.getVectorLayersByType(GeoConstants.GTAnyCheck)
        var items: List<Long>

        var vectorLayer: VectorLayer? = null
        var selectedSingleVectorLayer: VectorLayer? = null
        var selectedSingleFeatureId: Long = -1

        val mSelectedLayers = ArrayList<String>()
        var geometry: GeoGeometry? = null
        var featureId: Long = -1

        val selectedVectorLayer: MutableList<VectorLayer> = ArrayList()
        val selectedGeometry: MutableList<GeoGeometry?> = ArrayList()
        val selectedFeatures: MutableList<Feature> = ArrayList()

        var originalFeatureForSelect : Feature? = null

        layersLoop@ for (layer in layers) {
            if (!layer.isValid) continue

            if (!(layer as ILayerView).isVisible) continue

            vectorLayer = layer as VectorLayer
            items = vectorLayer.query(clickeEnelope)

            for (i in items.indices) {    // FIXME hack for bad RTree cache
                featureId = items[i]
                geometry = vectorLayer.getGeometryForId(featureId)
                if (editLayerOverlay!!.notContains(geometry, point)) {
                    continue
                }
                val feature = vectorLayer.getFeature(featureId)

                originalFeatureForSelect = vectorLayer.getFeature(featureId)


                if (originalFeatureForSelect != null) {


                    var fieldToDisplay = ((vectorLayer.getRenderer() as SimpleFeatureRenderer)
                        .getStyle())
                        .getField()
                    if (fieldToDisplay == null)
                        fieldToDisplay = vectorLayer.getFields().get(0).getName()

                    val valueForHint: String? = vectorLayer
                        .getFeature(featureId)
                        .getFieldValue(fieldToDisplay).toString()

                    if (valueForHint == null || "null" == valueForHint || TextUtils.isEmpty(valueForHint)
                    ) {
                        mSelectedLayers.add(layer.getName() + ": " + featureId)
                    } else {
                        mSelectedLayers.add(layer.getName() + ": " + valueForHint)
                    }
                } else {
                    mSelectedLayers.add(layer.getName() + ": " + featureId + " is null")
                }

                selectedSingleVectorLayer = layer
                selectedSingleFeatureId = featureId

                selectedVectorLayer.add(vectorLayer)
                selectedGeometry.add(geometry)
                selectedFeatures.add(feature)
            }
        }

        if (mSelectedLayers.size > 1)
            showOverlayPointMultiChoise(
                clickPoint.x.toDouble(), clickPoint.y.toDouble(), mSelectedLayers,
                selectedVectorLayer,
                selectedGeometry,
                selectedFeatures,
                false)
        else {
            if (mSelectedLayer != null)
                mSelectedLayer!!.isLocked = false

            mSelectedLayer = selectedSingleVectorLayer
            editLayerOverlay!!.setSelectedLayer(selectedSingleVectorLayer)


            if (geometry != null && mSelectedLayer != null) {
                editLayerOverlay!!.setSelectedFeature(selectedSingleFeatureId)
                mMap!!.map!!.startFeatureSelectionForView(mSelectedLayer!!.id, originalFeatureForSelect)
            }

            setMode(MODE_SELECT_ACTION)
            showOverlayPoint(clickPoint.x.toDouble(), clickPoint.y.toDouble())
        }
        //set select action mode
        mMap!!.postInvalidate()
    }



    override fun onLongPress(event: MotionEvent) {
        if (!(mode == MODE_NORMAL || mode == MODE_SELECT_ACTION) || mRulerOverlay!!.isMeasuring) {
            return
        }

        val dMinX = (event.x - mTolerancePX).toDouble()
        val dMaxX = (event.x + mTolerancePX).toDouble()
        val dMinY = (event.y - mTolerancePX).toDouble()
        val dMaxY = (event.y + mTolerancePX).toDouble()

        val mapEnv = mMap!!.screenToMap(GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY)) ?: return

        var exactEnv: GeoEnvelope? = GeoEnvelope(
            event.x.toDouble(),
            event.x.toDouble(),
            event.y.toDouble(),
            event.y.toDouble()
        )
        exactEnv = mMap!!.screenToMap(exactEnv)
        if (null == exactEnv) return
        val point = GeoPoint(exactEnv.maxX, exactEnv.minY)
        point.crs = GeoConstants.CRS_WEB_MERCATOR

        //show actions dialog
        val layers = mMap!!.getVectorLayersByType(GeoConstants.GTAnyCheck)
        var items: List<Long>


        var vectorLayer: VectorLayer? = null
        var selectedSingleVectorLayer: VectorLayer? = null
        var selectedSingleFeatureId: Long = -1

        val mSelectedLayers = ArrayList<String>()
        var geometry: GeoGeometry? = null
        var featureId: Long = -1

        val selectedVectorLayer: MutableList<VectorLayer> = ArrayList()
        val selectedGeometry: MutableList<GeoGeometry?> = ArrayList()
        val selectedFeaturesList: MutableList<Feature> = ArrayList()


        layersLoop@ for (layer in layers) {
            if (!layer.isValid) continue

            if (!(layer as ILayerView).isVisible) continue

            vectorLayer = layer as VectorLayer
            items = vectorLayer.query(mapEnv)

            for (i in items.indices) {    // FIXME hack for bad RTree cache
                featureId = items[i]
                geometry = vectorLayer.getGeometryForId(featureId)
                if (editLayerOverlay!!.notContains(geometry, point)) {
                    continue
                }

                val feature = vectorLayer.getFeature(featureId)
                var valueForHint = vectorLayer
                    .getFeature(featureId)
                    .getFieldValue(
                        ((vectorLayer.renderer as SimpleFeatureRenderer)
                            .style)
                            .field
                    )?.toString()
                if (valueForHint == null) {
                    mSelectedLayers.add(layer.getName() + ": " + featureId)
                    valueForHint = layer.getName() + ": " + featureId
                } else {
                    mSelectedLayers.add(layer.getName() + ": " + valueForHint)
                    valueForHint = layer.getName() + ": " + valueForHint
                }

                selectedSingleVectorLayer = layer
                selectedSingleFeatureId = featureId

                selectedVectorLayer.add(vectorLayer)
                selectedGeometry.add(geometry)
                selectedFeaturesList.add(feature)
            }
        }

        if (mSelectedLayers.size > 1)
            showOverlayPointMultiChoise(
                event.x.toDouble(), event.y.toDouble(), mSelectedLayers,
                selectedVectorLayer,
                selectedGeometry,
                selectedFeaturesList,
                true)
        else {
            if (mSelectedLayer != null)
                mSelectedLayer!!.isLocked = false

            mSelectedLayer = selectedSingleVectorLayer
            editLayerOverlay!!.setSelectedLayer(selectedSingleVectorLayer)

            if (geometry != null) editLayerOverlay!!.setSelectedFeature(selectedSingleFeatureId)

            setMode(MODE_SELECT_ACTION)
            showOverlayPoint(event.x.toDouble(), event.y.toDouble())
        }
        //set select action mode
        mMap!!.postInvalidate()
    }


    fun showAddByTapButton() {
        mAddPointButton!!.visibility = View.VISIBLE
    }


    fun hideAddByTapButton() {
        mAddPointButton!!.visibility = View.GONE
    }


    fun showRulerButton() {
        if (mPreferences!!.getBoolean(
                AppSettingsConstants.KEY_PREF_SHOW_MEASURING,
                false
            )
        ) mRuler!!.visibility =
            View.VISIBLE
    }


    fun hideRulerButton() {
        mRuler!!.visibility = View.GONE
    }


    fun showMainButton() {
        if (mode == MODE_EDIT_BY_WALK) return

        mAddNewGeometry!!.iconDrawable.alpha = 255
        mMainButton!!.visibility = View.VISIBLE
    }


    fun hideMainButton() {
        mMainButton!!.visibility = View.GONE
    }


    fun hideOverlayPoint() {
        editLayerOverlay!!.hideOverlayPoint()
        mMap!!.postInvalidate()

        hideAddByTapButton()
        showMainButton()
    }


    fun showOverlayPoint(x : Double, y: Double) {
        hideMainButton()
        showAddByTapButton()
        editLayerOverlay!!.setOverlayPoint(x, y)
    }

    fun showOverlayPointMultiChoise(
        x : Double,  y: Double,
        featureNames: List<String>,
        vectorLayer: List<VectorLayer>,
        geometry: List<GeoGeometry?>,
        features: List<Feature> ,
        editMode : Boolean) {
        val items = featureNames.toTypedArray<String>()

        val builder = AlertDialog.Builder(
            context!!
        )
        builder.setTitle(R.string.choose_object)

        builder.setItems(items) { dialog, which -> //String selectedItem = items[which];
            // remove after some time
            //Toast.makeText(getContext(), "Selected item: " + selectedItem, Toast.LENGTH_SHORT).show();

            if (mSelectedLayer != null) mSelectedLayer!!.isLocked = false

            mSelectedLayer = vectorLayer[which]
            editLayerOverlay!!.setSelectedLayer(vectorLayer[which])

            if (geometry[which] != null) {
                editLayerOverlay!!.setSelectedFeature(features[which].id)
                //mMap!!.map!!.startFeatureSelectionForEdit(mSelectedLayer!!.id, featureId[which])
                if (editMode)
                    mMap!!.map!!.startFeatureSelectionForEdit(mSelectedLayer!!.id, mSelectedLayer!!.geometryType, features[which], false)
                else
                    mMap!!.map!!.startFeatureSelectionForView(mSelectedLayer!!.id, features[which])
            }

            setMode(MODE_SELECT_ACTION)
            showOverlayPoint(x,y)

            hideMainButton()
            showAddByTapButton()
            editLayerOverlay!!.setOverlayPoint(x,y)
        }
        builder.create().show()
    }





    override fun onSingleTapUp(event: MotionEvent) {
        if (mRulerOverlay!!.isMeasuring) return
        when (mode) {
            MODE_EDIT -> {
                if (editLayerOverlay!!.selectGeometryInScreenCoordinates(
                        event.x,
                        event.y
                    )
                ) undoRedoOverlay!!.saveToHistory(
                    editLayerOverlay!!.selectedFeature
                )
                defineMenuItems()
            }

            MODE_SELECT_ACTION -> {
                editLayerOverlay!!.selectGeometryInScreenCoordinates(event.x, event.y)
                defineMenuItems()
            }

            MODE_INFO -> {
                editLayerOverlay!!.selectGeometryInScreenCoordinates(event.x, event.y)

                if (null != editLayerOverlay) {
                    val attributesFragment =
                        mActivity!!.supportFragmentManager.findFragmentByTag("ATTRIBUTES") as AttributesFragment?
                    if (attributesFragment != null) {
                        attributesFragment.setSelectedFeature(
                            mSelectedLayer,
                            editLayerOverlay!!.selectedFeatureId
                        )
                        mMap!!.postInvalidate()
                    }
                }
            }

            else -> if (mode == MODE_NORMAL || mode == MODE_SELECT_FOR_VIEW) {
                // check objects on map to select
                val dMinX = (event.x - mTolerancePX).toDouble()
                val dMaxX = (event.x + mTolerancePX).toDouble()
                val dMinY = (event.y - mTolerancePX).toDouble()
                val dMaxY = (event.y + mTolerancePX).toDouble()

                val mapEnv = mMap!!.screenToMap(GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY)) ?: return

                var exactEnv: GeoEnvelope? = GeoEnvelope(
                    event.x.toDouble(),
                    event.x.toDouble(),
                    event.y.toDouble(),
                    event.y.toDouble()
                )
                exactEnv = mMap!!.screenToMap(exactEnv)
                if (null == exactEnv) return
                val point = GeoPoint(exactEnv.maxX, exactEnv.minY)
                point.crs = GeoConstants.CRS_WEB_MERCATOR

                //show actions dialog
                val layers = mMap!!.getVectorLayersByType(GeoConstants.GTAnyCheck)
                var items: List<Long>


                var vectorLayer: VectorLayer? = null
                var selectedSingleVectorLayer: VectorLayer? = null
                var selectedSingleFeatureId: Long = -1

                val mSelectedLayers = ArrayList<String>()
                var geometry: GeoGeometry? = null
                var featureId: Long = -1

                val selectedVectorLayer: MutableList<VectorLayer> = ArrayList()
                val selectedGeometry: MutableList<GeoGeometry?> = ArrayList()
                val selectedFeatures: MutableList<Feature> = ArrayList()


                layersLoop@ for (layer in layers) {
                    if (!layer.isValid) continue

                    if (!(layer as ILayerView).isVisible) continue

                    vectorLayer = layer as VectorLayer
                    items = vectorLayer.query(mapEnv)

                    var i = 0
                    while (i < items.size) {
                        // FIXME hack for bad RTree cache
                        featureId = items[i]
                        geometry = vectorLayer.getGeometryForId(featureId)
                        if (editLayerOverlay!!.notContains(geometry, point)) {
                            i++
                            continue
                        }
                        var fieldToDisplay = ((vectorLayer.renderer as SimpleFeatureRenderer)
                            .style)
                            .field
                        if (TextUtils.isEmpty(fieldToDisplay)) {
                            fieldToDisplay = vectorLayer.fields[0].name
                        }
                        val feature = vectorLayer.getFeature(featureId)
                        if (feature != null) {
                            val objectValueForHint = vectorLayer
                                .getFeature(featureId)
                                .getFieldValue((fieldToDisplay))
                            var valueForHint = objectValueForHint.toString()
                            if (objectValueForHint == null && fieldToDisplay == "_id") {
                                valueForHint = vectorLayer.getFeature(featureId).id.toString()
                            }


                            if (valueForHint == null) {
                                mSelectedLayers.add(layer.getName() + ": " + featureId)
                                valueForHint = layer.getName() + ": " + featureId
                            } else {
                                mSelectedLayers.add(layer.getName() + ": " + valueForHint)
                                valueForHint = layer.getName() + ": " + valueForHint
                            }
                        }

                        selectedSingleVectorLayer = layer
                        selectedSingleFeatureId = featureId

                        selectedVectorLayer.add(vectorLayer)
                        selectedGeometry.add(geometry)
                        selectedFeatures.add(feature)

                        mMap!!.map!!.startFeatureSelectionForView(mSelectedLayer!!.id, feature)

                        i++
                    }
                }

                if (mSelectedLayers.size == 0 && mode == MODE_SELECT_FOR_VIEW) {
                    // need select none
                    setMode(MODE_NORMAL)
                } else {
                    if (mSelectedLayers.size > 1) showOverlayPointMultiChoise(
                        event.x.toDouble(), event.y.toDouble(), mSelectedLayers,
                        selectedVectorLayer,
                        selectedGeometry,
                        selectedFeatures,
                        false )
                    else {
                        if (mSelectedLayer != null) mSelectedLayer!!.isLocked = false

                        mSelectedLayer = selectedSingleVectorLayer
                        editLayerOverlay!!.setSelectedLayer(selectedSingleVectorLayer)

                        if (geometry != null) editLayerOverlay!!.setSelectedFeature(
                            selectedSingleFeatureId
                        )

                        setMode(MODE_SELECT_FOR_VIEW)
                        //showOverlayPoint(event);
                    }
                }
                //set select action mode
                mMap!!.postInvalidate()
            } else if (!mRulerOverlay!!.isMeasuring) hideOverlayPoint()
        }
    }

    fun onSingleTapUpFromMaplibre(screenx: Float, screeny :Float) {
        if (mRulerOverlay!!.isMeasuring) return
        when (mode) {
            MODE_EDIT -> {
//                if (editLayerOverlay!!.selectGeometryInScreenCoordinates(
//                        event.x,
//                        event.y
//                    )
//                ) undoRedoOverlay!!.saveToHistory(
//                    editLayerOverlay!!.selectedFeature
//                )
                //defineMenuItems()
            }

//            MODE_SELECT_ACTION -> {
//
////                editLayerOverlay!!.selectGeometryInScreenCoordinates(event.x, event.y)
////                defineMenuItems()
//            }

            MODE_INFO -> {
//                editLayerOverlay!!.selectGeometryInScreenCoordinates(event.x, event.y)

                if (null != editLayerOverlay) {
                    val attributesFragment =
                        mActivity!!.supportFragmentManager.findFragmentByTag("ATTRIBUTES") as AttributesFragment?
                    if (attributesFragment != null) {
                        attributesFragment.setSelectedFeature(
                            mSelectedLayer,
                            editLayerOverlay!!.selectedFeatureId
                        )
                        mMap!!.postInvalidate()
                    }
                }
            }

            else -> if (mode == MODE_NORMAL || mode == MODE_SELECT_FOR_VIEW || mode == MODE_SELECT_ACTION) {
                // check objects on map to select
                val dMinX = (screenx - mTolerancePX)
                val dMaxX = (screenx + mTolerancePX)
                val dMinY = (screeny - mTolerancePX)
                val dMaxY = (screeny + mTolerancePX)

                val screenPointMin = PointF(dMinX, dMinY)
                val screenPointMax = PointF(dMaxX, dMaxY)

                val minPoint = mMap!!.map!!.maplibreMap.getProjection().fromScreenLocation(screenPointMin)
                val maxPoint = mMap!!.map!!.maplibreMap.getProjection().fromScreenLocation(screenPointMax)

//                mMap!!.map!!.showTouchArea(minPoint, maxPoint)
//                  LatLng latLng = maplibreMap.get().getProjection().fromScreenLocation(screenPoint); // todo add tolerance and rect
//                  double[] points = convert4326To3857(latLng.getLongitude(), latLng.getLatitude());

                Log.e("CCCLLIICK", " click at: " + screenx + " - " + " screeny: " + screeny)
                Log.e("CCCLLIICK", "points lnglong " + minPoint.longitude + " : " +  minPoint.latitude + " : "
                        + maxPoint.longitude + " : " + maxPoint.latitude )

                val pointsMin = convert4326To3857(minPoint.longitude, minPoint.latitude)
                val pointsMax = convert4326To3857(maxPoint.longitude, maxPoint.latitude);
                //val mapEnv = mMap!!.screenToMap(GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY)) ?: return

                var minx =   pointsMin[0];
                var maxx =   pointsMax[0];
                var miny =   pointsMin[1];
                var maxy =   pointsMax[1];

                if (minx > maxx){
                    minx =   pointsMax[0]
                    maxx =   pointsMin[0]
                }
                if (miny > maxy){
                    miny =   pointsMax[1]
                    maxy =   pointsMin[1]
                }

                //val exactEnv: GeoEnvelope = GeoEnvelope(pointsMin[0],  pointsMax[0], pointsMin[1], pointsMax[1])
                val exactEnv: GeoEnvelope = GeoEnvelope(minx,maxx , miny, maxy)

                Log.e("CCCLLIICK", "exactEnv " + pointsMin[0] + " : " +  pointsMax[0] + " : " + pointsMin[1] + " : " + pointsMax[1] )

                //exactEnv = mMap!!.screenToMap(exactEnv)
                //if (null == exactEnv) return
                val point = GeoPoint(exactEnv.maxX, exactEnv.minY)
                point.crs = GeoConstants.CRS_WEB_MERCATOR

                Log.e("CCCLLIICK", "point : " + point.toString())

                //show actions dialog
                val layers = mMap!!.getVectorLayersByType(GeoConstants.GTAnyCheck)
                var items: List<Long>


                var vectorLayer: VectorLayer? = null
                var selectedSingleVectorLayer: VectorLayer? = null
                var selectedSingleFeatureId: Long = -1

                val mSelectedLayers = ArrayList<String>()
                var geometry: GeoGeometry? = null
                var featureId: Long = -1

                val selectedVectorLayer: MutableList<VectorLayer> = ArrayList()
                val selectedGeometry: MutableList<GeoGeometry?> = ArrayList()
                val selectedFeatures: MutableList<Feature> = ArrayList()


                layersLoop@ for (layer in layers) {
                    if (!layer.isValid) continue

                    if (!(layer as ILayerView).isVisible) continue

                    vectorLayer = layer as VectorLayer
                    items = vectorLayer.query(exactEnv)

                    var i = 0
                    while (i < items.size) {
                        // FIXME hack for bad RTree cache
                        featureId = items[i]
                        geometry = vectorLayer.getGeometryForId(featureId)
                        if (editLayerOverlay!!.notContains(geometry, point)) {
                            i++
                            continue
                        }
                        var fieldToDisplay = ((vectorLayer.renderer as SimpleFeatureRenderer)                            .style)                            .field
                        if (TextUtils.isEmpty(fieldToDisplay)) {
                            fieldToDisplay = vectorLayer.fields[0].name
                        }
                        val feature = vectorLayer.getFeature(featureId)

                        if (feature != null) {
                            val objectValueForHint = vectorLayer
                                .getFeature(featureId)
                                .getFieldValue((fieldToDisplay))
                            var valueForHint = objectValueForHint.toString()
                            if (objectValueForHint == null && fieldToDisplay == "_id") {
                                valueForHint = vectorLayer.getFeature(featureId).id.toString()
                            }


                            if (valueForHint == null) {
                                mSelectedLayers.add(layer.getName() + ": " + featureId)
                                valueForHint = layer.getName() + ": " + featureId
                            } else {
                                mSelectedLayers.add(layer.getName() + ": " + valueForHint)
                                valueForHint = layer.getName() + ": " + valueForHint
                            }
                            selectedFeatures.add(feature)
                        }

                        selectedSingleVectorLayer = layer
                        selectedSingleFeatureId = featureId

                        selectedVectorLayer.add(vectorLayer)
                        selectedGeometry.add(geometry)
                        selectedFeatures.add(feature)

                        mSelectedLayer = layer

                        if (featureId != -1L && mSelectedLayer != null) {
                            mMap!!.map!!.startFeatureSelectionForView(mSelectedLayer!!.id,feature)
                            break
                        }
                        i++
                    }
                }

                if (mSelectedLayers.size == 0 && mode == MODE_SELECT_FOR_VIEW) {
                    // need select none
                    setMode(MODE_NORMAL)
                } else {
                    if (mSelectedLayers.size > 1) showOverlayPointMultiChoise(screenx.toDouble(), screeny.toDouble(), mSelectedLayers,
                        selectedVectorLayer,
                        selectedGeometry,
                        selectedFeatures,
                        false )
                    else {
                        if (mSelectedLayer != null)
                            mSelectedLayer!!.isLocked = false

                        mSelectedLayer = selectedSingleVectorLayer
                        editLayerOverlay!!.setSelectedLayer(selectedSingleVectorLayer)

                        if (geometry != null) editLayerOverlay!!.setSelectedFeature(
                            selectedSingleFeatureId
                        )

                        if (mode != MODE_SELECT_ACTION)
                            setMode(MODE_SELECT_FOR_VIEW)
                        //showOverlayPoint(event);
                    }
                }
                //set select action mode
                mMap!!.postInvalidate()
            } else if (!mRulerOverlay!!.isMeasuring) hideOverlayPoint()
        }
    }


    override fun panStart(e: MotionEvent) {
        if (editLayerOverlay!!.mode == EditLayerOverlay.MODE_CHANGE) mNeedSave = true
    }


    override fun panMoveTo(e: MotionEvent) {
    }


    override fun panStop() {
        if (mode == MODE_EDIT_BY_TOUCH || mNeedSave) {
            mNeedSave = false
            undoRedoOverlay!!.saveToHistory(editLayerOverlay!!.selectedFeature)
        }
    }


    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            if (mCurrentCenter == null) {
                mCurrentCenter = GeoPoint()
            }

            mCurrentCenter!!.setCoordinates(location.longitude, location.latitude)
            mCurrentCenter!!.crs = GeoConstants.CRS_WGS84

            if (!mCurrentCenter!!.project(GeoConstants.CRS_WEB_MERCATOR)) {
                mCurrentCenter = null
            }
        }

        fillStatusPanel(location)
    }

    override fun onBestLocationChanged(location: Location) {
    }


    private fun fillStatusPanel(location: Location?) {
        if (mStatusPanelMode == 0) return

        var panel = mStatusPanel!!.getChildAt(0)
        if (panel == null) {
            panel = mActivity!!.layoutInflater.inflate(R.layout.status_panel, mStatusPanel, false)
            defineTextViews(panel)
            fillTextViews(location)
            mStatusPanel!!.removeAllViews()
            panel.background.alpha = 128
            mStatusPanel!!.addView(panel)
        } else fillTextViews(location)

        //        boolean needViewUpdate = true;
//        boolean isCurrentOrientationOneLine = mStatusPanel.getChildCount() > 0 &&
//                mStatusPanel.getChildAt(0).getId() == R.id.status_container_land;
//
//
//        if (!isCurrentOrientationOneLine) {
//            panel = mActivity.getLayoutInflater().inflate(R.layout.status_panel_land, mStatusPanel, false);
//            defineTextViews(panel);
//        } else {
//            panel = mStatusPanel.getChildAt(0);
//            needViewUpdate = false;
//        }
//
//        fillTextViews(location);
//
//        if (!isFitOneLine()) {
//            panel = mActivity.getLayoutInflater().inflate(R.layout.status_panel, mStatusPanel, false);
//            defineTextViews(panel);
//            fillTextViews(location);
//            needViewUpdate = true;
//        }
//        if (needViewUpdate) {
//            mStatusPanel.removeAllViews();
//            panel.getBackground().setAlpha(128);
//            mStatusPanel.addView(panel);
//        }
    }

    private fun fillTextViews(location: Location?) {
        if (null == location) {
            setDefaultTextViews()
        } else {
            if (location.provider == LocationManager.GPS_PROVIDER) {
                var text = ""
                val satellites = if (location.extras != null) location.extras!!
                    .getInt("satellites") else 0
                if (satellites > 0) text += satellites

                mStatusSource!!.text = text
                mStatusSource!!.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(
                        activity!!,
                        com.nextgis.maplibui.R.drawable.ic_location
                    ), null, null, null
                )
            } else {
                mStatusSource!!.text = ""
                mStatusSource!!.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(
                        activity!!,
                        com.nextgis.maplibui.R.drawable.ic_signal_wifi
                    ), null, null, null
                )
            }

            mStatusAccuracy!!.text = String.format(
                Locale.getDefault(),
                "%.1f %s", location.accuracy, getString(com.nextgis.maplib.R.string.unit_meter)
            )
            mStatusAltitude!!.text = String.format(
                Locale.getDefault(),
                "%.1f %s", location.altitude, getString(com.nextgis.maplib.R.string.unit_meter)
            )
            mStatusSpeed!!.text = String.format(
                Locale.getDefault(),
                "%.1f %s/%s",
                location.speed * 3600 / 1000,
                getString(com.nextgis.maplib.R.string.unit_kilometer),
                getString(com.nextgis.maplib.R.string.unit_hour)
            )
            mStatusLatitude!!.text =
                formatCoordinate(
                    location.latitude,
                    com.nextgis.maplibui.R.string.latitude_caption_short
                )
            mStatusLongitude!!.text =
                formatCoordinate(
                    location.longitude,
                    com.nextgis.maplibui.R.string.longitude_caption_short
                )
        }
    }


    private fun formatCoordinate(value: Double, appendix: Int): String {
        return LocationUtil.formatCoordinate(
            value,
            mCoordinatesFormat,
            mCoordinatesFraction
        ) + " " + getString(appendix)
    }


    private fun setDefaultTextViews() {
        mStatusSource!!.setCompoundDrawables(null, null, null, null)
        mStatusSource!!.text = ""
        mStatusAccuracy!!.text = getString(com.nextgis.maplibui.R.string.n_a)
        mStatusAltitude!!.text = getString(com.nextgis.maplibui.R.string.n_a)
        mStatusSpeed!!.text = getString(com.nextgis.maplibui.R.string.n_a)
        mStatusLatitude!!.text = getString(com.nextgis.maplibui.R.string.n_a)
        mStatusLongitude!!.text = getString(com.nextgis.maplibui.R.string.n_a)
    }


    private val isFitOneLine: Boolean
        get() {
            mStatusLongitude!!.measure(0, 0)
            mStatusLatitude!!.measure(0, 0)
            mStatusAltitude!!.measure(0, 0)
            mStatusSpeed!!.measure(0, 0)
            mStatusAccuracy!!.measure(0, 0)
            mStatusSource!!.measure(0, 0)

            val totalWidth =
                mStatusSource!!.measuredWidth + mStatusLongitude!!.measuredWidth +
                        mStatusLatitude!!.measuredWidth + mStatusAccuracy!!.measuredWidth +
                        mStatusSpeed!!.measuredWidth + mStatusAltitude!!.measuredWidth

            val metrics = DisplayMetrics()
            mActivity!!.windowManager.defaultDisplay.getMetrics(metrics)

            return totalWidth < metrics.widthPixels
            //        return totalWidth < mStatusPanel.getWidth();
        }


    private fun defineTextViews(panel: View) {
        mStatusSource = panel.findViewById(R.id.tv_source)
        mStatusAccuracy = panel.findViewById(R.id.tv_accuracy)
        mStatusSpeed = panel.findViewById(R.id.tv_speed)
        mStatusAltitude = panel.findViewById(R.id.tv_altitude)
        mStatusLatitude = panel.findViewById(R.id.tv_latitude)
        mStatusLongitude = panel.findViewById(R.id.tv_longitude)
        mZoom = panel.findViewById(R.id.tv_zoom)
        if (mZoom != null) mZoom!!.visibility = if (mPreferences!!.getBoolean(
                AppSettingsConstants.KEY_PREF_SHOW_ZOOM,
                false
            )
        ) View.VISIBLE else View.GONE
    }


    override fun onGpsStatusChanged(event: Int) {
    }


    override fun onStartEditSession() {
    }


    override fun onFinishEditSession() {
        setMode(MODE_NORMAL)
    }

    override fun onFinishEditByWalkSession() {
    }


    fun hideBottomBar() {
        mActivity!!.bottomToolbar.visibility = View.GONE
    }


    fun restoreBottomBar(mode: Int) {
        setMode(if (mode != -1) mode else this.mode)
    }


    fun addLocalTMSLayer(uri: Uri?) {
        if (null != mMap) {
            mMap!!.addLocalTMSLayer(uri)
        }
    }


    fun addLocalVectorLayer(uri: Uri?) {
        if (null != mMap) {
            mMap!!.addLocalVectorLayer(uri)
        }
    }


    fun addLocalVectorLayerWithForm(uri: Uri?) {
        if (null != mMap) {
            mMap!!.addLocalVectorLayerWithForm(uri)
        }
    }


    fun locateCurrentPosition() {
        if (mCurrentCenter != null) {
            mMap!!.panTo(mCurrentCenter)
        } else {
            Toast.makeText(
                mActivity,
                com.nextgis.maplibui.R.string.error_no_location,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun addNGWLayer() {
        if (null != mMap) {
            mMap!!.addNGWLayer()
        }
    }


    fun addRemoteLayer() {
        if (null != mMap) {
            mMap!!.addRemoteLayer()
        }
    }


    fun refresh() {
        if (null != mMap) {
            mMap!!.drawMapDrawable()
        }
    }

    val isDialogShown: Boolean
        get() = mChooseLayerDialog != null && mChooseLayerDialog!!.isResumed

    protected fun showFullCompass() {
        val fragmentManager = mActivity!!.supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val compassFragment = FullCompassFragment()
        compassFragment.setClickable(true)

        val container = R.id.mainview
        fragmentTransaction.add(container, compassFragment, "COMPASS_FULL")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack(null)
            .commit()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fl_compass -> showFullCompass()
            R.id.add_current_location -> if (v.isEnabled) addCurrentLocation()
            R.id.add_new_geometry -> if (v.isEnabled) addNewGeometry()
            R.id.add_geometry_by_walk -> if (v.isEnabled) addGeometryByWalk()

            R.id.action_zoom_in -> {
                //if (v.isEnabled) mMap!!.zoomIn()

                val currentZoom =  mMap!!.map.maplibreMap.cameraPosition.zoom
                var newZoom = currentZoom + 1.0
                if (newZoom > mMap!!.map.maplibreMap.maxZoomLevel)
                    newZoom = mMap!!.map.maplibreMap.maxZoomLevel
                val cameraUpdate = CameraUpdateFactory.zoomTo(newZoom)
                mMap!!.map.maplibreMap.animateCamera(cameraUpdate)
            }
            R.id.action_zoom_out -> {
                //if (v.isEnabled) mMap!!.zoomOut()
                val currentZoom = mMap!!.map.maplibreMap.cameraPosition.zoom
                var newZoom = currentZoom - 1.0
                if (newZoom < mMap!!.map.maplibreMap.minZoomLevel)
                    newZoom = mMap!!.map.maplibreMap.minZoomLevel
                val cameraUpdate = CameraUpdateFactory.zoomTo(newZoom)
                mMap!!.map.maplibreMap.animateCamera(cameraUpdate)
            }

            R.id.add_point_by_tap -> if (mRulerOverlay!!.isMeasuring) {
                mRulerOverlay!!.stopMeasuring()
                showMainButton()
                showRulerButton()
                hideAddByTapButton()
                mAddPointButton!!.setIcon(com.nextgis.maplibui.R.drawable.ic_action_add_point)
                mActivity!!.title = mActivity!!.appName
                mActivity!!.setSubtitle(null)
            } else addPointByTap()

            R.id.action_ruler -> {
                startMeasuring()
                Toast.makeText(context, R.string.tap_to_measure, Toast.LENGTH_SHORT).show()
            }
        }
    }

    protected fun startMeasuring() {
        mRulerOverlay!!.startMeasuring(this, mCurrentCenter)
        hideOverlayPoint()
        hideMainButton()
        hideRulerButton()
        showAddByTapButton()
        mAddPointButton!!.setIcon(com.nextgis.maplibui.R.drawable.ic_action_apply_dark)
    }

    override fun onLengthChanged(length: Double) {
        mActivity!!.title = LocationUtil.formatLength(context, length, 3)
    }

    override fun onAreaChanged(area: Double) {
        mActivity!!.setSubtitle(LocationUtil.formatArea(context, area))
    }

    companion object {
        const val MODE_NORMAL: Int = 0
        const val MODE_SELECT_ACTION: Int = 1
        const val MODE_EDIT: Int = 2
        const val MODE_INFO: Int = 3
        const val MODE_EDIT_BY_WALK: Int = 4
        const val MODE_EDIT_BY_TOUCH: Int = 5
        const val MODE_SELECT_FOR_VIEW: Int = 6


        protected const val KEY_MODE: String = "mode"
        protected const val BUNDLE_KEY_LAYER: String = "layer"
        protected const val BUNDLE_KEY_FEATURE_ID: String = "feature"
        protected const val BUNDLE_KEY_SAVED_FEATURE: String = "feature_blob"
        protected const val BUNDLE_KEY_IS_MEASURING: String = "is_measuring"
        const val EDIT_LAYER: Int = 2
    }

    fun deleteFeature() {
        val selectedFeatureId = editLayerOverlay!!.selectedFeatureId
        val layer = mSelectedLayer

        val builder = AlertDialog.Builder(
            activity!!
        )
            .setTitle(com.nextgis.maplibui.R.string.delete_confirm_feature)
            .setMessage(com.nextgis.maplibui.R.string.delete_feature)
            .setPositiveButton(com.nextgis.maplibui.R.string.menu_delete) { dialog: DialogInterface?, which: Int ->
                val snackbar = Snackbar.make(
                    activity!!.findViewById<View>(R.id.mainview),
                    activity!!.getString(com.nextgis.maplibui.R.string.delete_item_done),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(com.nextgis.maplibui.R.string.undo) { v: View? ->
                        layer!!.showFeature(selectedFeatureId)
                        editLayerOverlay!!.setSelectedFeature(selectedFeatureId)
                        defineMenuItems()
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(snackbar: Snackbar, event: Int) {
                            super.onDismissed(snackbar, event)
                            if (event == DISMISS_EVENT_MANUAL)
                                return
                            if (event != DISMISS_EVENT_ACTION) {
                                layer!!.deleteAddChanges(selectedFeatureId)
                                mMap!!.map!!.deleteFeature(selectedFeatureId, layer.id)
                            }
                        }

                        override fun onShown(snackbar: Snackbar) {
                            super.onShown(snackbar)
                        }
                    })
                mSelectedLayer!!.hideFeature(selectedFeatureId)
                editLayerOverlay?.setSelectedFeature(null)
                defineMenuItems()

                val view = snackbar.view
                val textView =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                textView.setTextColor(
                    ContextCompat.getColor(
                        mActivity!!,
                        com.nextgis.maplibui.R.color.color_white
                    )
                )
                snackbar.show()
            }
            .setNegativeButton(
                com.nextgis.maplibui.R.string.cancel
            ) { dialog: DialogInterface?, which: Int -> }.create()
        builder.show()
    }

    fun loadJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }

    override
    fun setHasEdit() {
        editLayerOverlay!!.setHasEdits(true)
    }

    override
    fun updateActions(editObject: MLGeometryEditClass?){
        editLayerOverlay!!.updateActions(editObject)
    }

    override
    fun updateGeometryFromMaplibre(feature: org.maplibre.geojson.Feature?,
                                   originalSelectedFeature: Feature?,
                                   editObject: MLGeometryEditClass?) {
        if (feature == null || originalSelectedFeature == null)
            return
        originalSelectedFeature.geometry = getGeometryFromMaplibreGeometry(feature)
        editLayerOverlay!!.updateGeometryFromMaplibre(originalSelectedFeature.geometry)
        editLayerOverlay!!.updateActions(editObject)
        undoRedoOverlay!!.saveToHistory(originalSelectedFeature)
    }

    override fun getSelectedLayerId(): Int {
        mSelectedLayer?.let { return it.id }
        return -1

    }

    private fun getGeometryFromMaplibreGeometry(feature: org.maplibre.geojson.Feature?) : GeoGeometry? {

        if (feature == null)
            return null;


        if (feature.geometry()!= null && feature.geometry() is MultiPolygon){
            val multipoly = feature.geometry() as MultiPolygon

            val geomultiPolygon = GeoMultiPolygon()

            for (poly in multipoly.coordinates()){

                val geoPolygon = GeoPolygon()
                geoPolygon.crs = GeoConstants.CRS_WEB_MERCATOR

                var iter = 0
                for (outer in poly){

                    if (iter == 0){ // outer ring
                        for (outer2 in outer){
                            val points: DoubleArray = convert4326To3857(outer2.longitude(), outer2.latitude())
                            val geopoint = GeoPoint(points[0], points[1])
                            geopoint.crs = GeoConstants.CRS_WEB_MERCATOR
                            geoPolygon.add(geopoint)
                        }
                    } else {
                        // inner
                        val ring = GeoLinearRing()

                        for (outer2 in outer){
                            val points: DoubleArray = convert4326To3857(outer2.longitude(), outer2.latitude())
                            val geopoint = GeoPoint(points[0], points[1])
                            geopoint.crs = GeoConstants.CRS_WEB_MERCATOR
                            ring.add(geopoint)
                        }
                        geoPolygon.addInnerRing(ring)
                    }
                    iter++
                }

                geomultiPolygon.add(geoPolygon)
            }
            return geomultiPolygon
        }



        if (feature.geometry()!= null && feature.geometry() is Polygon){
            val poly = feature.geometry() as Polygon

            val geoPolygon = GeoPolygon()
            geoPolygon.crs = GeoConstants.CRS_WEB_MERCATOR

            var iter = 0
            for (outer in poly.coordinates()){

                if (iter == 0){ // outer ring
                    for (outer2 in outer){
                        val points: DoubleArray = convert4326To3857(outer2.longitude(), outer2.latitude())
                        val geopoint = GeoPoint(points[0], points[1])
                        geopoint.crs = GeoConstants.CRS_WEB_MERCATOR
                        geoPolygon.add(geopoint)
                    }
                } else {
                    // inner
                    val ring = GeoLinearRing()

                    for (outer2 in outer){
                        val points: DoubleArray = convert4326To3857(outer2.longitude(), outer2.latitude())
                        val geopoint = GeoPoint(points[0], points[1])
                        geopoint.crs = GeoConstants.CRS_WEB_MERCATOR
                        ring.add(geopoint)
                    }
                        geoPolygon.addInnerRing(ring)
                }
                iter++
            }
            return geoPolygon
        }
        if (feature.geometry()!= null && feature.geometry() is Point){
            val point = feature.geometry() as Point

            val geoPoint = GeoPoint()
            geoPoint.crs = GeoConstants.CRS_WEB_MERCATOR
            val points: DoubleArray = convert4326To3857(point.longitude(), point.latitude())
            geoPoint.setCoordinates(points[0], points[1])
            return geoPoint
        }
        if (feature.geometry()!= null && feature.geometry() is MultiPoint){
            val geoPoint = GeoMultiPoint()

            val multiPoint = feature.geometry() as MultiPoint
            for ( point in multiPoint.coordinates()){
                val newPoint = GeoPoint()

                val points: DoubleArray = convert4326To3857(point.longitude(), point.latitude())
                newPoint.setCoordinates(points[0], points[1])
                newPoint.crs = GeoConstants.CRS_WEB_MERCATOR
                geoPoint.add(newPoint)
            }
            geoPoint.crs = GeoConstants.CRS_WEB_MERCATOR
            return geoPoint
        }


        if (feature.geometry()!= null && feature.geometry() is LineString){

            val geoLineObj = GeoLineString()
            geoLineObj.crs = GeoConstants.CRS_WEB_MERCATOR

            val poly = feature.geometry() as LineString

            val geoLine = GeoLineString()
            geoLine.crs = GeoConstants.CRS_WEB_MERCATOR

            for (outer in poly.coordinates()){
                val points: DoubleArray = convert4326To3857(outer.longitude(), outer.latitude())
                val geopoint = GeoPoint(points[0], points[1])
                geopoint.crs = GeoConstants.CRS_WEB_MERCATOR
                geoLineObj.add(geopoint)
            }
            return geoLineObj
        }

        if (feature.geometry()!= null && feature.geometry() is MultiLineString){
            val geoMultiLineObj = GeoMultiLineString()
            geoMultiLineObj.crs = GeoConstants.CRS_WEB_MERCATOR

            val geoMultiLine = feature.geometry() as MultiLineString
            for (line in geoMultiLine.lineStrings()){

                val geoLineObj = GeoLineString()
                geoLineObj.crs = GeoConstants.CRS_WEB_MERCATOR

                val poly = line as LineString

                val geoLine = GeoLineString()
                geoLine.crs = GeoConstants.CRS_WEB_MERCATOR

                for (outer in poly.coordinates()){
                    val points: DoubleArray = convert4326To3857(outer.longitude(), outer.latitude())
                    val geopoint = GeoPoint(points[0], points[1])
                    geopoint.crs = GeoConstants.CRS_WEB_MERCATOR
                    geoLineObj.add(geopoint)
                }
                geoMultiLineObj.add(geoLineObj)
            }
            return geoMultiLineObj
        }

        return null;
    }


    private fun convert4326To3857(lon: Double, lat: Double): DoubleArray {
        val x = lon * 20037508.34 / 180
        val y = ln(tan(Math.PI / 4 + Math.toRadians(lat) / 2)) * 20037508.34 / Math.PI
        return doubleArrayOf(x, y)
    }

    fun convert3857To4326(x: Double, y: Double): DoubleArray {
        val lon = x * 180 / 20037508.34
        val lat = Math.toDegrees(atan(sinh(y * Math.PI / 20037508.34)))
        return doubleArrayOf(lon, lat)
    }

}
