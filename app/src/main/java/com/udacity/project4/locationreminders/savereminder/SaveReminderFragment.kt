package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.LocationUtils
import com.udacity.project4.utils.PermissionUtils
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding

    private val args by navArgs<SaveReminderFragmentArgs>()

    private lateinit var dataItem: ReminderDataItem

    private lateinit var geoClient: GeofencingClient

//    private val locationUtils = LocationUtils(requireContext())

    private val geoPendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    private val resultForPermission =
        this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            Log.i("RESULT+++++++++++++++++++++++++++++++++", "$res")
            if (res.values.all { it }) {
                Log.i("RESULT+++++++++++++++++++++++++++++++++", "all true")
                PermissionUtils.checkDeviceLocationSettings(
                    this,
                    true,
                    {
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsTrue++")
                        addGeofence()
                    },
                    { exception ->
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsFalse++")

                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution  ).build()
                    resultForLocationSettings.launch(intentSenderRequest)
                })
            } else {
                Log.i("RESULT+++++++++++++++++++++++++++++++++", "all false")
                snackBar(
                    getString(R.string.permission_denied_explanation)
                , Snackbar.LENGTH_INDEFINITE
                ) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            }
        }

    private val resultForLocationSettings =
        this.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            Log.i("RESULT+++++++++++++++++++++++++++++++++", "$it")
            if (it.resultCode == RESULT_OK) {
                Log.i("RESULT+++++++++++++++++++++++++++++++++", "RESULT_OK")
                addGeofence()
            } else {
                snackBar(
                    getString(R.string.location_required_error)
                )
            }
        }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args.dataRemender?.let {
            _viewModel.apply {
                reminderTitle.value = it.title
                reminderDescription.value = it.description
                reminderSelectedLocationStr.value = it.location
                latitude.value = it.latitude
                longitude.value = it.longitude
            }
        }
        geoClient = LocationServices.getGeofencingClient(requireContext())
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value = NavigationCommand.To(
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            )
        }

        binding.saveReminder.setOnClickListener {
                dataItem = ReminderDataItem(
                    _viewModel.reminderTitle.value,
                    _viewModel.reminderDescription.value,
                    _viewModel.reminderSelectedLocationStr.value,
                    _viewModel.latitude.value,
                    _viewModel.longitude.value
                )
            if (_viewModel.validateEnteredData(dataItem)) {
                Log.i("RESULT+++++++++++++++++++++++++++++++++", "validateEnteredData")
                checkPermissions()
            }
        }
        return binding.root
    }


    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @SuppressLint("MissingPermission", "SuspiciousIndentation")
    private fun addGeofence() {
        val geo = Geofence.Builder()
            .setRequestId(dataItem.id)
            .setCircularRegion(
                dataItem.latitude!!,
                dataItem.longitude!!,
                100f
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geoRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geo)
            .build()
                geoClient.addGeofences(geoRequest, geoPendingIntent).run {
                    addOnSuccessListener {
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "addOnSuccessListener")
                        _viewModel.validateAndSaveReminder(dataItem)
                        Log.i("GEOFENCE", "GEOFENCE ADDED")
                    }
                    addOnFailureListener {
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "addOnFailureListener")
                        _viewModel.showErrorMessage.value = it.message
                        it.message?.let { msg -> Log.i("GEOFENCE", msg) }
                    }
                }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissions() {

        Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkPermissions")
        if (PermissionUtils.foregroundPermissionApproved(this) && PermissionUtils.backgroundPermissionApproved(this)) {

            Log.i("RESULT+++++++++++++++++++++++++++++++++", "all permission approved")
            PermissionUtils.checkDeviceLocationSettings(
                this,
                true,
                {
                    Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsTrue")
                    addGeofence()
                },
                { exception ->
                    Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsFalse")
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution  ).build()
                    resultForLocationSettings.launch(intentSenderRequest)

                })
        }
        else {
            if (!PermissionUtils.foregroundPermissionApproved(this)) {
                Log.i("RESULT+++++++++++++++++++++++++++++++++", "foregroundPermissionApprovedFalse")
                PermissionUtils.requestForegroundLocationPermission(this
                ) {
                    resultForPermission.launch(
                        PermissionUtils.FORGRAOUND_PERMISSIONS
                    )
                }

            }

            if (!PermissionUtils.backgroundPermissionApproved(this)) {

                Log.i("RESULT+++++++++++++++++++++++++++++++++", "backgroundPermissionApprovedFalse")
                PermissionUtils.requestBackgroundLocationPermission(this,
                    {
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "backgroundLocationPermissionTrue")
                        PermissionUtils.checkDeviceLocationSettings(
                            this,
                            true,
                            {
                                Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsTrue--")
                                addGeofence()
                            },
                            { exception ->
                                Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsFalse--")
                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(exception.resolution  ).build()
                                resultForLocationSettings.launch(intentSenderRequest)

                            })
                    },
                    {
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "backgroundLocationPermissionFalse++")
                    resultForPermission.launch(
                        PermissionUtils.BACKGROUND_PERMISSIONS
                    )
                },)
            }

            if (PermissionUtils.foregroundPermissionApproved(this) && PermissionUtils.backgroundPermissionApproved(this)) {
                Log.i("RESULT+++++++++++++++++++++++++++++++++", "all permission approved++")
                PermissionUtils.checkDeviceLocationSettings(
                    this,
                    true,
                    {
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsTrue**")
                        addGeofence()
                    },
                    { exception ->
                        Log.i("RESULT+++++++++++++++++++++++++++++++++", "checkDeviceLocationSettingsFalse**")
                        val intentSenderRequest =
                            IntentSenderRequest.Builder(exception.resolution  ).build()
                        resultForLocationSettings.launch(intentSenderRequest)
                    })
            }
        }
    }

    companion object {
        private const val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT"
    }
}
