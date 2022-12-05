package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    //Get the local repository instance
    private val remindersLocalRepository: ReminderDataSource by inject()
    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        val TAG = GeofenceTransitionsJobIntentService::class.java.simpleName
        private const val JOB_ID = 573

        //        TODO: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        //TODO: handle the geofencing transition events and
        // send a notification to the user when he enters the geofence area
        //TODO call @sendNotification
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            when {
                geofencingEvent.hasError() -> {
                    Log.e(TAG, "Error ${geofencingEvent.errorCode}")
                    return
                }
                geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.v(TAG, "Geofence entered")
                    sendNotification(geofencingEvent.triggeringGeofences!!)
                }
                else -> {
                    Log.e(TAG, "Geofence transition error")
                }
            }
        }

    }

    //TODO: get the request id of the current geofence
    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        var requestId = ""
        if (triggeringGeofences.isEmpty()) {
            Log.e(TAG, " No geofence trigger found.")
            return

        }
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            //get the reminder with the request id
            for (geofence in triggeringGeofences) {
                requestId = geofence.requestId
                val result = remindersLocalRepository.getReminder(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }

            }

        }
    }

}
