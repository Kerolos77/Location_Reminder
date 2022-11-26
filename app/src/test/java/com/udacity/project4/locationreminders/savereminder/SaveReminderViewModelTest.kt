package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import getOrAwaitValue
import junit.framework.Assert.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.DelayController
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import kotlin.coroutines.ContinuationInterceptor

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    //TODO: provide testing to the SaveReminderView and its live data objects

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var fakeDataSource: FakeDataSource

    private lateinit var appContext: Application

    @Before
    fun setupViewModel() = mainCoroutineRule.runBlockingTest {
        stopKoin()
        appContext = ApplicationProvider.getApplicationContext()
        fakeDataSource = FakeDataSource()
//        fakeDataSource.saveReminder(
//            ReminderDTO(
//                "title",
//                "description",
//                "location",
//                0.0,
//                0.0,
//                "id"
//            )
//        )
//        fakeDataSource.saveReminder(
//            ReminderDTO(
//                "title 1",
//                "description 1",
//                "location 1",
//                1.0,
//                1.0,
//                "id 1"
//            )
//        )
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    @Test
    fun saveReminder_loading() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDataItem(
            "title",
            "description",
            "location",
            0.0,
            0.0,
            "id"
        )
        (mainCoroutineRule.coroutineContext[ContinuationInterceptor]!! as DelayController).pauseDispatcher()
        saveReminderViewModel.validateAndSaveReminder(reminder)
        assertTrue(saveReminderViewModel.showLoading.getOrAwaitValue())
        (mainCoroutineRule.coroutineContext[ContinuationInterceptor]!! as DelayController).resumeDispatcher()
        assertFalse(saveReminderViewModel.showLoading.getOrAwaitValue())

    }

    @Test
    fun saveReminder_success() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDataItem(
            "title",
            "description",
            "location",
            0.0,
            0.0,
            "id"
        )
        saveReminderViewModel.validateAndSaveReminder(reminder)
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`(appContext.getString(R.string.reminder_saved))
        )

        assertThat(
            saveReminderViewModel.navigationCommand.getOrAwaitValue(),
            `is`(NavigationCommand.Back)
        )
    }

    @Test
    fun saveReminder_validate() = mainCoroutineRule.runBlockingTest {
        var reminder = ReminderDataItem(
            null,
            "description",
            "location",
            0.0,
            0.0,
            "id"
        )

        assertFalse(saveReminderViewModel.validateAndSaveReminder(reminder))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )
        reminder = ReminderDataItem(
            "title",
            "description",
            null,
            0.0,
            0.0,
            "id"
        )
        assertFalse(saveReminderViewModel.validateAndSaveReminder(reminder))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
        reminder = ReminderDataItem(
            "title",
            "description",
            "location",
            0.0,
            0.0,
            "id"
        )
        assertTrue(saveReminderViewModel.validateAndSaveReminder(reminder))
    }
}