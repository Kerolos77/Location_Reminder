package com.udacity.project4.utils

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository

object ServiceLocator {

    private val lock = Any()

    private var reminderDatabase: RemindersDatabase? = null

    @Volatile
    var repo: RemindersLocalRepository? = null
        @VisibleForTesting set

    fun provideTasksRepository(context: Context): RemindersLocalRepository {
        synchronized(this) {
            return repo ?: createTasksRepository(context)
        }
    }

    private fun createTasksRepository(context: Context): RemindersLocalRepository {
        val newRepo =
            RemindersLocalRepository(createDataBase(context).reminderDao())
        repo = newRepo
        return newRepo
    }

    private fun createDataBase(context: Context): RemindersDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java,
            "Tasks.db"
        ).build()
        reminderDatabase = result
        return result
    }

    @VisibleForTesting
    fun resetRepository() {
        synchronized(lock) {
            reminderDatabase?.apply {
                clearAllTables()
                close()
            }
            reminderDatabase = null
            repo = null
        }
    }
}