// SPDX-License-Identifier: GPL-3.0-or-later
package com.shilapi.xcertplay

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.shilapi.xcertplay.mfi.IdentityProvisioner
import com.shilapi.xcertplay.orchestration.MfiTarget
import java.util.concurrent.Executors

/** Keeps an explicit import across rotation, without retaining an Activity or logging URIs. */
class RoadLinkIdentityViewModel(application: Application) : AndroidViewModel(application) {
    enum class State { IDLE, WORKING, IMPORTED, FAILED }
    val state = MutableLiveData(State.IDLE)
    private val worker = Executors.newSingleThreadExecutor()

    fun acknowledgeResult() {
        if (state.value == State.IMPORTED || state.value == State.FAILED) state.value = State.IDLE
    }

    fun install(key: Uri, certificate: Uri) {
        if (state.value == State.WORKING || CarPlayBackgroundSession.hasSession()) return
        state.value = State.WORKING
        val context = getApplication<Application>()
        worker.execute {
            val result = runCatching {
                context.contentResolver.openInputStream(key).use { keyInput ->
                    requireNotNull(keyInput)
                    context.contentResolver.openInputStream(certificate).use { certInput ->
                        requireNotNull(certInput)
                        IdentityProvisioner(context.noBackupFilesDir).install(keyInput, certInput) {
                            !Thread.currentThread().isInterrupted && !CarPlayBackgroundSession.hasSession()
                        }
                    }
                }
                DiPlayBootstrap.invalidateLocalIdentity()
                AirPlayPersistence.saveMfiTarget(context, MfiTarget.LOCAL)
            }
            state.postValue(if (result.isSuccess) State.IMPORTED else State.FAILED)
        }
    }

    override fun onCleared() { worker.shutdownNow() }
}
