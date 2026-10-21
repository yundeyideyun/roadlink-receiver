// SPDX-License-Identifier: GPL-3.0-or-later
// RoadLink addition, 2026-10-21. No embedded identities or certificate issuance.
package com.shilapi.xcertplay.mfi

import java.io.File
import java.io.InputStream

/** Fixed-name private storage with validation, rollback and bounded reads. */
class IdentityProvisioner(root: File) {
    private val root = root.canonicalFile.apply { check(isDirectory || mkdirs()) }
    private fun child(name: String): File = File(root, name).canonicalFile.also {
        check(it.parentFile == root) { "Identity path escapes private storage" }
    }
    private val target get() = child(LocalMfiAuthenticationClient.DIRECTORY)
    private val staged get() = child("roadlink-identity-staged")
    private val previous get() = child("roadlink-identity-previous")

    /** Returns material consistency only; it does not establish Apple/iPhone trust. */
    fun configured(): Boolean = synchronized(LOCK) {
        recover()
        target.isDirectory && runCatching { LocalMfiAuthenticationClient.load(target) }.isSuccess
    }

    fun install(key: InputStream, certificate: InputStream, canCommit: () -> Boolean = { true }) {
        // Provider I/O must not hold the private-store lock: it may be a slow document provider.
        val keyBytes = readBounded(key)
        var certBytes = byteArrayOf()
        try {
            certBytes = readBounded(certificate)
            synchronized(LOCK) {
                recover()
                check(canCommit()) { "A receiver session is running" }
                staged.deleteRecursively()
                check(staged.mkdir()) { "Could not prepare private staging" }
                try {
                    writePrivate(File(staged, "identity.pk8"), keyBytes)
                    writePrivate(File(staged, "certificate.p7b"), certBytes)
                    LocalMfiAuthenticationClient.load(staged)
                    check(canCommit()) { "A receiver session started while importing" }
                    previous.deleteRecursively()
                    if (target.exists()) check(target.renameTo(previous)) { "Could not preserve previous identity" }
                    try {
                        check(staged.renameTo(target)) { "Could not commit identity" }
                    } catch (failure: Exception) {
                        if (!target.exists() && previous.exists()) check(previous.renameTo(target)) { "Identity recovery is required" }
                        throw failure
                    }
                    previous.deleteRecursively()
                } finally { staged.deleteRecursively() }
            }
        } finally {
            keyBytes.fill(0); certBytes.fill(0)
        }
    }

    fun remove(canCommit: () -> Boolean = { true }) = synchronized(LOCK) {
        check(canCommit()) { "A receiver session is running" }
        check(target.deleteRecursively()) { "Could not remove local identity" }
        check(previous.deleteRecursively()) { "Could not remove previous local identity" }
        check(staged.deleteRecursively()) { "Could not remove staging" }
    }

    private fun recover() {
        if (!target.exists() && previous.isDirectory) check(previous.renameTo(target)) { "Could not recover previous identity" }
        if (target.isDirectory && previous.exists()) {
            if (runCatching { LocalMfiAuthenticationClient.load(target) }.isSuccess) previous.deleteRecursively()
            else if (runCatching { LocalMfiAuthenticationClient.load(previous) }.isSuccess) {
                check(target.deleteRecursively())
                check(previous.renameTo(target))
            }
        }
    }

    private fun readBounded(input: InputStream): ByteArray {
        var total = 0
        val buffer = ByteArray(MAX_INPUT_BYTES + 1)
        try {
            while (total < buffer.size) {
                val count = input.read(buffer, total, buffer.size - total)
                if (count == -1) break
                require(count > 0) { "Identity stream made no progress" }
                total += count
                require(total <= MAX_INPUT_BYTES) { "Identity input is too large" }
            }
            require(total > 0) { "Identity input is empty" }
            return buffer.copyOf(total)
        } finally { buffer.fill(0) }
    }

    private fun writePrivate(destination: File, data: ByteArray) {
        destination.outputStream().use { output ->
            output.write(data)
            output.fd.sync()
        }
        destination.setReadable(false, false); destination.setReadable(true, true)
        destination.setWritable(false, false); destination.setWritable(true, true)
    }

    companion object {
        const val MAX_INPUT_BYTES = 16 * 1024
        private val LOCK = Any()
    }
}
