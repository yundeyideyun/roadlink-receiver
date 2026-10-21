// SPDX-License-Identifier: GPL-3.0-or-later
package com.shilapi.xcertplay.mfi

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Date
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERBitString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x509.Time
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IdentityProvisionerTest {
    @get:Rule val temporary = TemporaryFolder()
    private data class Material(val key: ByteArray, val certificate: ByteArray)

    // Uses the existing LocalMfiAuthenticationClientTest synthetic-certificate approach.
    private fun material(): Material {
        val generator = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }
        val pair = generator.generateKeyPair()
        val algorithm = AlgorithmIdentifier(X9ObjectIdentifiers.ecdsa_with_SHA256)
        val name = X500Name("CN=RoadLink synthetic test only")
        val tbs = V3TBSCertificateGenerator().apply {
            setSerialNumber(ASN1Integer(BigInteger.ONE)); setSignature(algorithm)
            setIssuer(name); setSubject(name); setStartDate(Time(Date(0)))
            setEndDate(Time(Date(4102444800000L)))
            setSubjectPublicKeyInfo(SubjectPublicKeyInfo.getInstance(pair.public.encoded))
        }.generateTBSCertificate()
        val signer = Signature.getInstance("SHA256withECDSA").apply { initSign(pair.private); update(tbs.encoded) }
        return Material(pair.private.encoded, DERSequence(arrayOf<ASN1Encodable>(tbs, algorithm, DERBitString(signer.sign()))).encoded)
    }

    @Test fun successfulImportReloadsButDoesNotClaimPhoneTrust() {
        val root = temporary.newFolder(); val store = IdentityProvisioner(root); val first = material()
        assertFalse(store.configured())
        store.install(first.key.inputStream(), first.certificate.inputStream())
        assertTrue(IdentityProvisioner(root).configured())
        assertEquals(64, LocalMfiAuthenticationClient.load(File(root, "offline-mfi")).signChallenge(ByteArray(32)).size)
        assertFalse(File(root, "roadlink-identity-staged").exists())
    }

    @Test fun invalidOrMismatchedImportPreservesPreviouslyValidMaterial() {
        val root = temporary.newFolder(); val store = IdentityProvisioner(root)
        val first = material(); val other = material()
        store.install(first.key.inputStream(), first.certificate.inputStream())
        assertThrows(Exception::class.java) { store.install(other.key.inputStream(), first.certificate.inputStream()) }
        assertThrows(Exception::class.java) { store.install(byteArrayOf(1, 2).inputStream(), first.certificate.inputStream()) }
        assertTrue(store.configured())
        assertArrayEquals(first.certificate, LocalMfiAuthenticationClient.load(File(root, "offline-mfi")).readCertificate())
    }

    @Test fun oversizedEmptyAndFailedProviderReadsNeverRemoveCurrentIdentity() {
        val root = temporary.newFolder(); val store = IdentityProvisioner(root); val first = material()
        store.install(first.key.inputStream(), first.certificate.inputStream())
        assertThrows(Exception::class.java) { store.install(ByteArrayInputStream(ByteArray(IdentityProvisioner.MAX_INPUT_BYTES + 1)), first.certificate.inputStream()) }
        assertThrows(Exception::class.java) { store.install(byteArrayOf().inputStream(), first.certificate.inputStream()) }
        val broken = object : InputStream() { override fun read(): Int = throw IOException("synthetic provider failure") }
        assertThrows(Exception::class.java) { store.install(first.key.inputStream(), broken) }
        assertTrue(store.configured())
        assertArrayEquals(first.certificate, LocalMfiAuthenticationClient.load(File(root, "offline-mfi")).readCertificate())
    }

    @Test fun sessionStartingDuringValidationPreventsReplacement() {
        val root = temporary.newFolder(); val store = IdentityProvisioner(root)
        val first = material(); val second = material(); var checks = 0
        store.install(first.key.inputStream(), first.certificate.inputStream())
        assertThrows(Exception::class.java) {
            store.install(second.key.inputStream(), second.certificate.inputStream()) { ++checks < 2 }
        }
        assertArrayEquals(first.certificate, LocalMfiAuthenticationClient.load(File(root, "offline-mfi")).readCertificate())
    }

    @Test fun interruptedDirectoryCommitRecoversThePreviousIdentity() {
        val root = temporary.newFolder(); val first = material(); val store = IdentityProvisioner(root)
        store.install(first.key.inputStream(), first.certificate.inputStream())
        assertTrue(File(root, "offline-mfi").renameTo(File(root, "roadlink-identity-previous")))
        assertTrue(IdentityProvisioner(root).configured())
        assertFalse(File(root, "roadlink-identity-previous").exists())
    }

    @Test fun removalRequiresAnIdleReceiver() {
        val root = temporary.newFolder(); val store = IdentityProvisioner(root); val first = material()
        store.install(first.key.inputStream(), first.certificate.inputStream())
        assertThrows(Exception::class.java) { store.remove { false } }
        assertTrue(store.configured())
        store.remove()
        assertFalse(store.configured())
    }
}
