package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.LocationUtils
import com.udacity.project4.utils.PermissionUtils
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

@Suppress("DEPRECATED_IDENTITY_EQUALS")
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var locationUtils: LocationUtils
    private val TAG = SelectLocationFragment::class.java.simpleName
    private var mark: Marker? = null

    private val request =
        this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            if (res.values.all { it }) {
                enableMyLocation()
            } else {
                snackBar(
                    getString(R.string.permission_denied_explanation)
                ) {
                    getLocation()
                    if (!PermissionUtils.isPermissionGranted(requireContext())) {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                            )
                        )
                    }
                }

            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        locationUtils = LocationUtils(requireContext())


        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            if (mark == null) {
                snackBar(getString(R.string.select_poi))
            } else {
                onLocationSelected()
            }
        }
        return binding.root
    }

    private fun onLocationSelected() {
        _viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val locationSnippets = _viewModel.reminderSelectedLocationStr.value
        val zoomLevel = 15f
        map = googleMap
        getLocation()
        setMapStyle(map)

        if (latitude != null && longitude != null && locationSnippets != null) {
            val latLng = LatLng(latitude, longitude)

            mark = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(locationSnippets)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
//            map.uiSettings.isZoomControlsEnabled = true
        } else if (PermissionUtils.isPermissionGranted(
                requireContext()
            )
        ) {
            locationUtils.lastLocation {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), zoomLevel)
                )
            }
        }
        setOnPoiClick(map)
        setMapOnClick(map)


    }

    private fun setMapOnClick(map: GoogleMap) {
        map.setOnMapClickListener {
            mark?.remove()

            val snippet = "Lat: ${it.latitude} Long: ${it.longitude}"
            _viewModel.latitude.postValue(it.latitude)
            _viewModel.longitude.postValue(it.longitude)
            _viewModel.reminderSelectedLocationStr.postValue(snippet)

            mark = map.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("Selected Location")
                    .snippet(snippet)
            )
        }
    }

    private fun setOnPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener {
            mark?.remove()

            val snippet = it.name
            _viewModel.selectedPOI.postValue(it)
            _viewModel.latitude.postValue(it.latLng.latitude)
            _viewModel.longitude.postValue(it.latLng.longitude)
            _viewModel.reminderSelectedLocationStr.postValue(snippet)

            mark = map.addMarker(
                MarkerOptions()
                    .position(it.latLng)
                    .title("Selected Location")
                    .snippet(snippet)
            )
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun getLocation() {
        if (PermissionUtils.isPermissionGranted(
                requireContext()
            )
        ) {
            enableMyLocation()
        } else {
            request.launch(PermissionUtils.PERMISSIONS)

        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener {
            locationUtils.checkDeviceLocationSettings(
                this,
                true,
                {
                    locationUtils.lastLocation {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                15f
                            )
                        )
                    }

                },
                {
                    snackBar(
                        getString(R.string.location_required_error)
                    )
//                    {
//                        getLocation()
//                    }
                }
            )
            true
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        Log.d("onRequestPermission", "onRequestPermissionResult")
//        if (requestCode == 1) {
//            if (grantResults.isNotEmpty()) {
//                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                    && (grantResults[1] == PackageManager.PERMISSION_GRANTED)
//                ) {
//                    Log.d("onRequestPermission", "Permission Granted")
//                    enableMyLocation()
//                } else {
//                    Log.d("onRequestPermission", "Permission Denied")
//
//                }
//            }
//        }
//    }

}
