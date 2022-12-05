package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText

import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastTest
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var context: Application
    private var binding = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at thi
    import org.koin.core.context.GlobalContext.startKoin
    import org.koin.core.context.GlobalContext.stopKoins step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin

        context = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    context,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    context,
                    get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(context) }
        }
        // new koin module
        startKoin { modules(listOf(myModule)) }
        //Get our repository
        repository = get()
        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().apply {
            register(EspressoIdlingResource.countingIdlingResource)
            register(binding)
        }
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().apply {
            unregister(EspressoIdlingResource.countingIdlingResource)
            unregister(binding)
        }
    }

    @Test
    fun launchReminder() {
        // GIVEN
        val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
        runBlocking {
            repository.saveReminder(reminder)
        }
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        binding.monitorActivity(scenario)
        // WHEN
        // THEN
        onView(withText(reminder.title))
            .check(
                matches(
                    isDisplayed()
                )
            )
        onView(withText(reminder.description))
            .check(
                matches(
                    isDisplayed()
                )
            )
        onView(withText(reminder.location))
            .check(
                matches(
                    isDisplayed()
                )
            )
        scenario.close()
    }

    @Test
    fun snapBarTest() {
        // GIVEN
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        binding.monitorActivity(scenario)
        // WHEN
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())
        // THEN
        onView(withText(R.string.err_enter_title))
            .check(
                matches(
                    isDisplayed()
                )
            )
        scenario.close()
    }
//    @Test
//    fun toastTest() {
//        // GIVEN
//        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
//
//        binding.monitorActivity(scenario)
//        // WHEN
//        onView(withId(R.id.addReminderFAB)).perform(click())
//        onView(withId(R.id.selectLocation)).perform(click())
//        onView(withId(R.id.map)).perform(click())
//        onView(withId(R.id.save_button)).perform(click())
//        onView(withId(R.id.reminderTitle)).perform(typeText("title"))
//        onView(withId(R.id.reminderDescription)).perform(typeText("description"))
//        closeSoftKeyboard()
//        onView(withId(R.id.saveReminder)).perform(click())
//        // THEN
//        onView(withText(R.string.reminder_saved))
//            .inRoot(ToastTest())
//            .check(
//                matches(
//                    isDisplayed()
//                )
//            )
//        scenario.close()
//    }

    @Test
    fun addReminder() {
        // GIVEN
        val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        binding.monitorActivity(scenario)
        // WHEN
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.save_button)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText(reminder.title))
        onView(withId(R.id.reminderDescription)).perform(typeText(reminder.description))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        // THEN
        // TODO Teat Toast
        onView(withText(R.string.reminder_saved))
            .inRoot(ToastTest())
            .check(
                matches(
                    isDisplayed()
                )
            )
        Thread.sleep(1000)
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withText(reminder.title))
            .check(matches(isDisplayed()))
        onView(withText(reminder.description))
            .check(matches(isDisplayed()))
        scenario.close()
    }

}
