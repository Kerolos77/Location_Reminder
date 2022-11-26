package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import getOrAwaitValue
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource

    @Before
    fun setupViewModel() = mainCoroutineRule.runBlockingTest {
        stopKoin()
        fakeDataSource = FakeDataSource()
        fakeDataSource.saveReminder(
            ReminderDTO(
                "title",
                "description",
                "location",
                0.0,
                0.0,
                "id"
            )
        )
        fakeDataSource.saveReminder(
            ReminderDTO(
                "title 1",
                "description 1",
                "location 1",
                1.0,
                1.0,
                "id 1"
            )
        )
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    @Test
    fun loadReminders_loading() = mainCoroutineRule.runBlockingTest {
        (mainCoroutineRule.coroutineContext[ContinuationInterceptor]!! as DelayController).pauseDispatcher()
        remindersListViewModel.loadReminders()
        assertTrue(remindersListViewModel.showLoading.getOrAwaitValue())
        (mainCoroutineRule.coroutineContext[ContinuationInterceptor]!! as DelayController).resumeDispatcher()
        assertFalse(remindersListViewModel.showLoading.getOrAwaitValue())
    }

    @Test
    fun loadReminders_resultNotEmpty() = mainCoroutineRule.runBlockingTest {
        remindersListViewModel.loadReminders()
        assertFalse(remindersListViewModel.remindersList.getOrAwaitValue().isEmpty())
        assertFalse(remindersListViewModel.showNoData.getOrAwaitValue())
    }

    @Test
    fun loadReminders_resultEmpty() = mainCoroutineRule.runBlockingTest {
        fakeDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assertTrue(remindersListViewModel.remindersList.getOrAwaitValue().isEmpty())
        assertTrue(remindersListViewModel.showNoData.getOrAwaitValue())
    }

    @Test
    fun loadReminders_error() = mainCoroutineRule.runBlockingTest {
        fakeDataSource.setMakeError(true)
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Error occurred"))
    }
}