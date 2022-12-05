package com.udacity.project4.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar

class LocationUtils(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }


    @SuppressLint("MissingPermission")
    fun lastLocation(
        listener: (Location) -> Unit,
        fragment: Fragment,
    ) {
        if (PermissionUtils.foregroundPermissionApproved(fragment)&&PermissionUtils.backgroundPermissionApproved(fragment)) {
            fusedLocationClient.lastLocation
                .addOnCompleteListener { task ->
                    if (task.result != null) {
                        newLocationData(listener)
                    } else {
                        listener.invoke(task.result!!)
                    }
                }
        }

    }

    @SuppressLint("MissingPermission")
    private fun newLocationData(listener: (Location) -> Unit) {
        val lCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                listener.invoke(locationResult.lastLocation!!)
            }
        }
        with(LocationRequest()) {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
            fusedLocationClient.requestLocationUpdates(this, lCallback, null)
        }

    }




}
const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
const val TAG = "HuntMainActivity"
const val LOCATION_FINE_PERMISSION_INDEX = 0
const val LOCATION_CROSSE_PERMISSION_INDEX = 1
const val BACKGROUND_LOCATION_PERMISSION_INDEX = 2

