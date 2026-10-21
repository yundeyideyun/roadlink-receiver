// SPDX-License-Identifier: GPL-3.0-or-later
// RoadLink original integration, 2026-10-21. Receiver core attribution is preserved.
package com.shilapi.xcertplay

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.mfi.IdentityProvisioner
import com.shilapi.xcertplay.orchestration.MfiTarget

/** A generic launcher and runtime provisioning flow, independent of a particular vehicle. */
class RoadLinkSetupActivity : ComponentActivity() {
    private lateinit var model: RoadLinkIdentityViewModel
    private var keyUri: Uri? = null
    private var certificateUri: Uri? = null
    private var busy = false
    private var configured = false
    private var ink = Color.WHITE
    private var muted = Color.LTGRAY
    private var surface = Color.DKGRAY
    private var accent = Color.CYAN
    private var onAccent = Color.BLACK
    internal lateinit var pageContent: LinearLayout
        private set
    private val store get() = IdentityProvisioner(noBackupFilesDir)
    private fun sessionIdle() = !CarPlayBackgroundSession.hasSession()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[RoadLinkIdentityViewModel::class.java]
        RoadLinkProfile.applyFirstRun(this)
        keyUri = savedInstanceState?.getString("key_uri")?.let(Uri::parse)
        certificateUri = savedInstanceState?.getString("certificate_uri")?.let(Uri::parse)
        configured = runCatching { store.configured() }.getOrDefault(false)
        render()
        model.state.observe(this) { state ->
            busy = state == RoadLinkIdentityViewModel.State.WORKING
            if (!busy) configured = runCatching { store.configured() }.getOrDefault(false)
            if (state == RoadLinkIdentityViewModel.State.IMPORTED) { keyUri = null; certificateUri = null }
            render()
            if (state == RoadLinkIdentityViewModel.State.IMPORTED) message(R.string.roadlink_import_success)
            else if (state == RoadLinkIdentityViewModel.State.FAILED) message(R.string.roadlink_import_failed)
            model.acknowledgeResult()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::pageContent.isInitialized) render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("key_uri", keyUri?.toString())
        outState.putString("certificate_uri", certificateUri?.toString())
    }

    private fun render() {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val background = Color.parseColor(if (night) "#12151B" else "#F3F6FA")
        surface = Color.parseColor(if (night) "#202835" else "#FFFFFF")
        ink = Color.parseColor(if (night) "#EFF4FC" else "#17263A")
        muted = Color.parseColor(if (night) "#B6C4D7" else "#52647C")
        accent = Color.parseColor(if (night) "#ADC8FF" else "#245EDB")
        onAccent = Color.parseColor(if (night) "#102549" else "#FFFFFF")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val scroll = ScrollView(this).apply { setBackgroundColor(background); isFillViewport = true }
        pageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(dp(PADDING_DP), dp(PADDING_DP), dp(PADDING_DP), dp(PADDING_DP))
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        title(R.string.roadlink_title)
        paragraph(R.string.roadlink_intro)
        paragraph(R.string.roadlink_validation_boundary)
        section {
            this.title(R.string.roadlink_auth_heading, SMALL_HEADING_SP)
            this.paragraph(if (configured) R.string.roadlink_local_ready else R.string.roadlink_local_missing)
            val selected = AirPlayPersistence.loadMfiTarget(this@RoadLinkSetupActivity)
            this.paragraph(if (selected == MfiTarget.USB_CH341) R.string.roadlink_usb_selected else R.string.roadlink_local_selected)
            if (!sessionIdle()) this.paragraph(R.string.roadlink_session_busy)
            this.action(R.string.roadlink_import, false, !busy && sessionIdle()) {
                AlertDialog.Builder(this@RoadLinkSetupActivity).setTitle(R.string.roadlink_import)
                    .setMessage(R.string.roadlink_import_notice)
                    .setPositiveButton(R.string.roadlink_pick_key) { _, _ -> choose(KEY_REQUEST) }
                    .setNegativeButton(android.R.string.cancel, null).show()
            }
            this.action(R.string.roadlink_pick_key, false, !busy && sessionIdle()) { choose(KEY_REQUEST) }
            this.action(R.string.roadlink_pick_certificate, false, !busy && sessionIdle()) { choose(CERTIFICATE_REQUEST) }
            val selection = getString(R.string.roadlink_selection,
                getString(if (keyUri != null) R.string.roadlink_yes else R.string.roadlink_no),
                getString(if (certificateUri != null) R.string.roadlink_yes else R.string.roadlink_no))
            addView(label(selection, BODY_SP, muted))
            this.action(if (busy) R.string.roadlink_working else R.string.roadlink_commit, true,
                !busy && sessionIdle() && keyUri != null && certificateUri != null) { installSelected() }
            this.action(R.string.roadlink_use_usb, false, !busy && sessionIdle()) {
                AlertDialog.Builder(this@RoadLinkSetupActivity).setTitle(R.string.roadlink_use_usb)
                    .setMessage(R.string.roadlink_usb_notice)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (!sessionIdle()) message(R.string.roadlink_session_busy)
                        else { AirPlayPersistence.saveMfiTarget(this@RoadLinkSetupActivity, MfiTarget.USB_CH341); render() }
                    }.setNegativeButton(android.R.string.cancel, null).show()
            }
            this.action(R.string.roadlink_remove, false, !busy && sessionIdle() && configured) {
                AlertDialog.Builder(this@RoadLinkSetupActivity).setTitle(R.string.roadlink_remove)
                    .setMessage(R.string.roadlink_remove_confirm)
                    .setPositiveButton(R.string.roadlink_remove) { _, _ -> removeIdentity() }
                    .setNegativeButton(android.R.string.cancel, null).show()
            }
        }
        action(R.string.roadlink_open_receiver, true, !busy) {
            startActivity(Intent(this, DiPlayActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        paragraph(R.string.roadlink_not_trust)
        paragraph(R.string.roadlink_credits)
        scroll.addView(pageContent)
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
    }

    private fun choose(request: Int) {
        if (!sessionIdle() || busy) return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivityForResult(intent, request) }
        catch (_: android.content.ActivityNotFoundException) { message(R.string.roadlink_picker_missing) }
    }

    @Deprecated("Legacy activity result is used for Android 10 head-unit compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        when (requestCode) {
            KEY_REQUEST -> keyUri = data.data
            CERTIFICATE_REQUEST -> certificateUri = data.data
            else -> return
        }
        render()
    }

    private fun installSelected() {
        val key = keyUri ?: return
        val certificate = certificateUri ?: return
        if (busy || !sessionIdle()) return
        model.install(key, certificate)
    }

    private fun removeIdentity() {
        if (busy || !sessionIdle()) return
        runCatching { store.remove(::sessionIdle) }.onSuccess {
            configured = false; DiPlayBootstrap.invalidateLocalIdentity(); render()
        }.onFailure { message(R.string.roadlink_import_failed) }
    }

    private fun section(build: LinearLayout.() -> Unit) {
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(PADDING_DP), dp(PADDING_DP), dp(PADDING_DP), dp(PADDING_DP))
            background = GradientDrawable().apply { setColor(surface); cornerRadius = dp(CONTROL_HEIGHT_DP) * CORNER_RADIUS_RATIO }
            build()
        }
        pageContent.addView(block, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(SECTION_GAP_DP); bottomMargin = dp(SECTION_GAP_DP)
        })
    }

    private fun LinearLayout.title(resource: Int, size: Int = HEADING_SP) { addView(label(getString(resource), size, ink, true)) }
    private fun title(resource: Int, size: Int = HEADING_SP) = pageContent.title(resource, size)
    private fun LinearLayout.paragraph(resource: Int) {
        addView(label(getString(resource), BODY_SP, muted), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(TEXT_GAP_DP) })
    }
    private fun paragraph(resource: Int) = pageContent.paragraph(resource)
    private fun LinearLayout.action(resource: Int, primary: Boolean, enabled: Boolean, action: () -> Unit) {
        val control = Button(this@RoadLinkSetupActivity).apply {
            text = getString(resource); isAllCaps = false; textSize = BODY_SP.toFloat()
            minHeight = dp(CONTROL_HEIGHT_DP); setPadding(dp(CONTROL_PADDING_DP), dp(TEXT_GAP_DP), dp(CONTROL_PADDING_DP), dp(TEXT_GAP_DP))
            setTextColor(if (primary) onAccent else ink)
            background = GradientDrawable().apply {
                setColor(if (primary) accent else surface)
                cornerRadius = dp(CONTROL_HEIGHT_DP) * CORNER_RADIUS_RATIO
                setStroke(dp(BORDER_DP), accent)
            }
            backgroundTintList = null
            isEnabled = enabled; alpha = if (enabled) 1f else DISABLED_ALPHA
            setOnClickListener { action() }
        }
        addView(control, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(CONTROL_GAP_DP) })
    }
    private fun action(resource: Int, primary: Boolean, enabled: Boolean, action: () -> Unit) = pageContent.action(resource, primary, enabled, action)
    private fun label(text: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun message(resource: Int) = Toast.makeText(this, resource, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    companion object {
        private const val KEY_REQUEST = 510
        private const val CERTIFICATE_REQUEST = 511
        private const val PADDING_DP = 20
        private const val SECTION_GAP_DP = 20
        private const val TEXT_GAP_DP = 8
        private const val CONTROL_GAP_DP = 12
        private const val CONTROL_HEIGHT_DP = 52
        private const val CONTROL_PADDING_DP = 16
        private const val BORDER_DP = 1
        private const val HEADING_SP = 28
        private const val SMALL_HEADING_SP = 22
        private const val BODY_SP = 17
        private const val CORNER_RADIUS_RATIO = .25f
        private const val DISABLED_ALPHA = .5f
    }
}
