// SPDX-License-Identifier: GPL-3.0-or-later
package com.shilapi.xcertplay

import android.content.Context
import com.shilapi.xcertplay.setup.DiLinkGeneration
import com.shilapi.xcertplay.setup.SetupGuide
import com.shilapi.xcertplay.update.UpdateAvailability

/** Generic first-run defaults are applied once; later user choices are preserved. */
internal object RoadLinkProfile {
    fun applyFirstRun(context: Context) {
        val prefs = context.getSharedPreferences("roadlink_profile", Context.MODE_PRIVATE)
        if (prefs.getBoolean("initialized", false)) return
        DiPlayPreferences.saveAutoConnect(context, false)
        DiPlayPreferences.saveConnectOnPhoneBluetooth(context, false)
        AirPlayPersistence.saveClusterMapEnabled(context, false)
        AirPlayPersistence.saveAdbClusterEnabled(context, false)
        AirPlayPersistence.saveLegacyClusterEnabled(context, false)
        AirPlayPersistence.saveLocationReportingEnabled(context, false)
        AirPlayPersistence.saveDebugLogsEnabled(context, false)
        AirPlayPersistence.saveNavigationAudioChannel(context, 0)
        UpdateAvailability.saveBackgroundChecksEnabled(context, false)
        DiLinkGeneration.save(context, DiLinkGeneration.UNKNOWN)
        SetupGuide.markSeen(context)
        prefs.edit().putBoolean("initialized", true).commit()
    }
}
