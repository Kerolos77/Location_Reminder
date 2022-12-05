package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    var reminders: MutableList<ReminderDTO> = mutableListOf()
) : ReminderDataSource {

    //    TODO: Create a fake data source to act as a double to the real data source
    private var makeError = false

    fun setMakeError(makeError: Boolean) {
        this.makeError = makeError
    }


    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        return try{
            if (makeError)
            {
                Result.Error("Error occurred")
            }
            else
            {
                Result.Success(ArrayList(reminders))
            }
        } catch (e: Exception) {
            Result.Error("Error occurred")
        }

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        return try{
            if (makeError)
            {
                Result.Error("Error occurred")
            }
            else
            {
                val reminder = reminders?.find { it.id == id }
                if (reminder != null) {
                    Result.Success(reminder)
                } else {
                    Result.Error("Reminder not found!")
                }
            }
        } catch (e: Exception) {
            Result.Error("Error occurred")
        }
        
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}