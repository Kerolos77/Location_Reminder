package com.udacity.project4.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

class LocationUtils(private val context: Context) {


    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun checkDeviceLocationSettings(
        fragment: Fragment,
        resolve: Boolean = true,
        onSuccessCallback: (() -> Unit)? = null,
        onFailureCallback: (() -> Unit)? = null
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(context)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnSuccessListener {
            onSuccessCallback?.invoke()
        }

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    fragment.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                        checkDeviceLocationSettings(
                            fragment,
                            false,
                            onSuccessCallback,
                            onFailureCallback
                        )
                    }.launch(IntentSenderRequest.Builder(exception.resolution.intentSender).build())
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            } else {
                onFailureCallback?.invoke()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun lastLocation(
        listener: (Location) -> Unit
    ) {
        if (PermissionUtils.isPermissionGranted(context)) {
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

    private fun startLocationUpdates(listener: (Location) -> Unit) {
        val lCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                listener.invoke(locationResult.lastLocation!!)
            }
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            LocationRequest(),
            lCallback,
            Looper.getMainLooper()
        )
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "HuntMainActivity"
private const val LOCATION_FINE_PERMISSION_INDEX = 0
private const val LOCATION_CROSSE_PERMISSION_INDEX = 1
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 2
