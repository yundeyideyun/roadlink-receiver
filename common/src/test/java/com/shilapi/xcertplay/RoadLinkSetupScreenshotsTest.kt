// SPDX-License-Identifier: GPL-3.0-or-later
package com.shilapi.xcertplay

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoadLinkSetupScreenshotsTest {
    @Test fun onboardingFitsCompactFullAndArabicAtLargeFontScale() {
        for ((name, widthDp, language) in listOf(
            Triple("compact-zh", 320, "zh-CN"),
            Triple("full-en", 960, "en-US"),
            Triple("compact-ar-rtl", 320, "ar-SA"),
        )) {
            val controller = Robolectric.buildActivity(RoadLinkSetupActivity::class.java)
            val activity = controller.get()
            val locale = Locale.forLanguageTag(language)
            val config = Configuration(activity.resources.configuration).apply {
                fontScale = 1.5f; setLocale(locale); setLayoutDirection(locale)
            }
            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
            controller.setup()
            try {
                val content = activity.pageContent
                val width = (widthDp * activity.resources.displayMetrics.density).toInt()
                content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                content.layout(0, 0, width, content.measuredHeight)
                assertTrue(content.measuredHeight > 0)
                val bitmap = Bitmap.createBitmap(width, content.measuredHeight, Bitmap.Config.ARGB_8888)
                try {
                    content.draw(Canvas(bitmap))
                    val directory = File("build/reports/roadlink-setup-ui").apply { mkdirs() }
                    File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                } finally { bitmap.recycle() }
            } finally { controller.close() }
        }
    }
}
