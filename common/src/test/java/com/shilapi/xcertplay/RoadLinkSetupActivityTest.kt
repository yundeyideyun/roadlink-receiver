// SPDX-License-Identifier: GPL-3.0-or-later
package com.shilapi.xcertplay

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.setup.DiLinkGeneration
import com.shilapi.xcertplay.update.UpdateAvailability
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 35])
class RoadLinkSetupActivityTest {
    @Test fun unconfiguredSourceBuildRemainsHonestAndCanOpenTheReceiver() {
        Robolectric.buildActivity(RoadLinkSetupActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertTrue(views(activity.pageContent).filterIsInstance<TextView>().any { it.text == activity.getString(R.string.roadlink_local_missing) })
            val commit = views(activity.pageContent).filterIsInstance<Button>().single { it.text == activity.getString(R.string.roadlink_commit) }
            assertFalse(commit.isEnabled)
            val open = views(activity.pageContent).filterIsInstance<Button>().single { it.text == activity.getString(R.string.roadlink_open_receiver) }
            open.performClick()
            assertEquals(DiPlayActivity::class.java.name, shadowOf(activity).nextStartedActivity.component?.className)
        }
    }

    @Test fun genericFirstRunDisablesVehicleSpecificAndUnsolicitedBehavior() {
        Robolectric.buildActivity(RoadLinkSetupActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertFalse(AirPlayPersistence.loadClusterMapEnabled(activity))
            assertFalse(AirPlayPersistence.loadLocationReportingEnabled(activity))
            assertFalse(DiPlayPreferences.autoConnect(activity))
            assertFalse(UpdateAvailability.backgroundChecksEnabled(activity))
            assertEquals(DiLinkGeneration.UNKNOWN, DiLinkGeneration.current(activity))
            AirPlayPersistence.saveLocationReportingEnabled(activity, true)
            RoadLinkProfile.applyFirstRun(activity)
            assertTrue(AirPlayPersistence.loadLocationReportingEnabled(activity))
        }
    }

    @Test fun selectingAKeyUsesSystemDocumentsInsteadOfBroadStoragePermissions() {
        Robolectric.buildActivity(RoadLinkSetupActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            views(activity.pageContent).filterIsInstance<Button>().single { it.text == activity.getString(R.string.roadlink_pick_key) }.performClick()
            val request = shadowOf(activity).nextStartedActivityForResult.intent
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.action)
            assertTrue(request.hasCategory(Intent.CATEGORY_OPENABLE))
            assertEquals("*/*", request.type)
        }
    }

    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(views(view.getChildAt(i)))
    }
}
