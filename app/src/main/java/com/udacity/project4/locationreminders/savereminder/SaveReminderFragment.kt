package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding

    private val args by navArgs<SaveReminderFragmentArgs>()

    private lateinit var dataItem: ReminderDataItem

    private lateinit var geoClient: GeofencingClient

    private val geoPendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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

    }


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
            if (!::dataItem.isInitialized) {
                dataItem = ReminderDataItem(
                    _viewModel.reminderTitle.value,
                    _viewModel.reminderDescription.value,
                    _viewModel.reminderSelectedLocationStr.value,
                    _viewModel.latitude.value,
                    _viewModel.longitude.value
                )
            } else {
                dataItem.apply {
                    title = _viewModel.reminderTitle.value
                    description = _viewModel.reminderDescription.value
                    location = _viewModel.reminderSelectedLocationStr.value
                    latitude = _viewModel.latitude.value
                    longitude = _viewModel.longitude.value
                }
            }
            if (_viewModel.validateAndSaveReminder(dataItem)) {
                addGeofence(dataItem)
            }
        }


        geoClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(dataItem: ReminderDataItem) {
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

        geoClient.removeGeofences(geoPendingIntent).run {
            addOnCompleteListener {
                geoClient.addGeofences(geoRequest, geoPendingIntent).run {
                    addOnSuccessListener {
                        Log.i("GEOFENCE", "GEOFENCE ADDED")
                    }
                    addOnFailureListener {
                        _viewModel.showErrorMessage.value = it.message
                        it.message?.let { msg -> Log.i("GEOFENCE", msg) }
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT"
    }
}
