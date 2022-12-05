package com.udacity.project4.utils

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R

object PermissionUtils {
    val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    val FORGRAOUND_PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    @RequiresApi(Build.VERSION_CODES.Q)
    val BACKGROUND_PERMISSIONS =
        if (runningQOrLater) {
            arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf()
        }
    val resultCode = when {
        runningQOrLater -> {
            REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
        }
        else -> {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
    }




    fun isPermissionGranted(context: Context): Boolean {
        return if (runningQOrLater) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    }

    fun requestPermission(
        fragment: Fragment,
        onResultAction: (Map<String, Boolean>) -> Unit
    ) {
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onResultAction(it)
        }.launch(FORGRAOUND_PERMISSIONS)
    }

    @TargetApi(29)
    fun foregroundPermissionApproved(fragment: Fragment): Boolean {
        return (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            fragment.requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        && PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            fragment.requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                )
    }

    @TargetApi(29)
    fun backgroundPermissionApproved(fragment: Fragment): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        fragment.requireContext(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }
    }

    @TargetApi(29)
    fun requestForegroundLocationPermission(fragment: Fragment, onFailed: (() -> Unit)? = null ) {
        when {
            foregroundPermissionApproved(fragment) -> return

            shouldShowRequestPermissionRationale(fragment.requireActivity(),Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    fragment.requireView(),
                    R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.settings) {
                        startActivity(fragment.requireContext(),Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },null)
                    }.show()
            }
            else -> {
                onFailed?.invoke()

            }
        }
    }

    @TargetApi(29)
    fun requestBackgroundLocationPermission(fragment: Fragment,onSuccess: (() -> Unit)? = null,
                                            onFailure: (() -> Unit)? = null) {
        if (backgroundPermissionApproved(fragment)) {
            onSuccess?.invoke()
            return
        }
        if (runningQOrLater) {
            onFailure?.invoke()
        } else return
    }

    @TargetApi(29)
    fun foregroundAndBackgroundLocationPermissionApproved(fragment: Fragment): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            fragment.requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        && PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            fragment.requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                )
        val backgroundPermissionApproved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        fragment.requireContext(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }
        return foregroundLocationApproved && backgroundPermissionApproved
    }
    fun checkDeviceLocationSettings(
        fragment: Fragment,
        resolve: Boolean = true,
        onSuccessCallback: (() -> Unit)? = null,
        onRequestCallback: ((ResolvableApiException) -> Unit)
    ){
          val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
          val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
          val settingsClient = LocationServices.getSettingsClient(fragment.requireContext())
          val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
          locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                onSuccessCallback?.invoke()
                      }
              }
          locationSettingsResponseTask.addOnFailureListener { exception ->
                  if (exception is ResolvableApiException && resolve) {
                           try {
                                   onRequestCallback.invoke(exception)
                } catch (sendEx: IntentSender.SendIntentException) {

                }
            } else {
                           Snackbar.make(
                    fragment.requireView(),
                    com.udacity.project4.R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings(fragment, resolve, onSuccessCallback,onRequestCallback)
                }.show()
            }
        }
    }

    @TargetApi(29)
    fun requestForegroundAndBackgroundLocationPermissions(fragment: Fragment) {
        if(foregroundAndBackgroundLocationPermissionApproved(fragment))
            return
        var permissionsArray= arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        val resultCode = when{
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> {
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            }
        }
        ActivityCompat.requestPermissions(
            fragment.requireActivity(),
            permissionsArray,
            resultCode
        )
    }
}