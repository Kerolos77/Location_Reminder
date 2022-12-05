package com.udacity.project4.util

import android.os.IBinder
import android.view.WindowManager
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher


class ToastTest : TypeSafeMatcher<Root?>() {

    override fun describeTo(description: Description?) {
        description?.appendText("toast")
    }

    override fun matchesSafely(item: Root?): Boolean {
        val type = item?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val window: IBinder = item.decorView.windowToken
            val app: IBinder = item.decorView.applicationWindowToken
            if (window === app) { // means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }

}