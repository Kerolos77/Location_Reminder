package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    private lateinit var reminder: ReminderDTO


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        reminder = ReminderDTO(
            "title",
            "description",
            "location",
            0.0,
            0.0
        )
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun save_and_get_reminder() = runBlocking {
        // GIVEN - Insert
        database.reminderDao().saveReminder(reminder)
        // WHEN - Get
        val loaded = database.reminderDao().getReminderById(reminder.id)
        // THEN
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.title, `is`(reminder.title))

    }

    @Test
    fun delete_reminder() = runBlocking {
        // GIVEN - Insert
        database.reminderDao().saveReminder(reminder)
        // WHEN - Delete
        database.reminderDao().deleteAllReminders()
        // THEN
        val loaded = database.reminderDao().getReminderById(reminder.id)
        assertThat(loaded, nullValue())
    }

}