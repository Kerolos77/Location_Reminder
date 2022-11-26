package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase

    private lateinit var reminder: ReminderDTO

    private lateinit var remindersRepo: RemindersLocalRepository


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        reminder = ReminderDTO(
            "title",
            "description",
            "location",
            0.0,
            0.0
        )

        remindersRepo = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun save_and_get_reminder() = runBlocking {
        // GIVEN - Insert
        remindersRepo.saveReminder(reminder)
        // WHEN - Get
        val loaded = remindersRepo.getReminder(reminder.id) as Result.Success
        // THEN
        assertThat(loaded.data, notNullValue())
        assertThat(loaded.data.id, `is`(reminder.id))
        assertThat(loaded.data.latitude, `is`(reminder.latitude))
        assertThat(loaded.data.longitude, `is`(reminder.longitude))
        assertThat(loaded.data.location, `is`(reminder.location))
        assertThat(loaded.data.description, `is`(reminder.description))
        assertThat(loaded.data.title, `is`(reminder.title))

    }

    @Test
    fun delete_reminder() = runBlocking {
        // GIVEN - Insert
        remindersRepo.saveReminder(reminder)
        // WHEN - Delete
        remindersRepo.deleteAllReminders()
        // THEN
        val loaded = remindersRepo.getReminders() as Result.Success
        assertThat(loaded.data.isEmpty(), `is`(true))
    }

    @Test
    fun reminderError() = runBlocking {
        // THEN
        val loaded = remindersRepo.getReminder(reminder.id) as Result.Error
        assertThat(loaded.message, `is`("Reminder not found!"))
    }


}