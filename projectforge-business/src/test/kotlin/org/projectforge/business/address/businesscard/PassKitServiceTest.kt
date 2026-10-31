/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////


package org.projectforge.business.address.businesscard

import com.fasterxml.jackson.databind.ObjectMapper
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Date
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

class PassKitServiceTest {
    private val bc = BouncyCastleProvider()

    private fun certificate(subject: String, keyPair: java.security.KeyPair): X509Certificate {
        val name = X500Name("CN=$subject")
        val holder = JcaX509v3CertificateBuilder(
            name, BigInteger.valueOf(System.nanoTime()), Date(System.currentTimeMillis() - 1000),
            Date(System.currentTimeMillis() + 86_400_000L), name, keyPair.public,
        ).build(JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private))
        return JcaX509CertificateConverter().getCertificate(holder)
    }

    private fun service(dir: File, configured: Boolean = true): Triple<PassKitService, X509Certificate, X509Certificate> {
        val generator = KeyPairGenerator.getInstance("RSA").also { it.initialize(2048) }
        val passKeys = generator.generateKeyPair()
        val pass = certificate("Pass Type ID: test", passKeys)
        val wwdr = certificate("Test WWDR", generator.generateKeyPair())
        val p12 = File(dir, "pass.p12")
        KeyStore.getInstance("PKCS12", bc).also {
            it.load(null, null)
            it.setKeyEntry("pass", passKeys.private, "secret".toCharArray(), arrayOf(pass))
            p12.outputStream().use { out -> it.store(out, "secret".toCharArray()) }
        }
        val wwdrFile = File(dir, "wwdr.cer").also { it.writeBytes(wwdr.encoded) }
        val configuration = WalletConfiguration().also {
            if (configured) {
                it.passCertificatePath = p12.path
                it.passCertificatePassword = "secret"
                it.wwdrCertificatePath = wwdrFile.path
                it.teamIdentifier = "TEAM123456"
                it.passTypeIdentifier = "pass.test.businesscard"
                it.organizationName = "ACME GmbH"
            }
        }
        val service = PassKitService()
        PassKitService::class.java.getDeclaredField("walletConfiguration").also {
            it.isAccessible = true
            it.set(service, configuration)
        }
        return Triple(service, pass, wwdr)
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { result[it.name] = zip.readBytes() }
        }
        return result
    }

    private fun photo(): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB), "jpg", out)
        return out.toByteArray()
    }

    @Test
    fun `not configured without certificate`(@TempDir dir: File) {
        assertFalse(service(dir, configured = false).first.isConfigured())
        assertTrue(service(dir).first.isConfigured())
    }

    @Test
    fun `pass has manifest hashes, a verifiable detached signature and the vcard as QR code`(@TempDir dir: File) {
        val (service, passCertificate, wwdr) = service(dir)
        val card = BusinessCardService.createFrom(
            org.projectforge.business.address.AddressDO().also {
                it.firstName = "Kai"; it.name = "Reinhard"; it.organization = "ACME GmbH"
                it.positionText = "CEO"; it.email = "kai@acme.test"; it.mobilePhone = "+49 171 1234"
            }
        ).also { it.id = "card-1" }
        val vCard = BusinessCardVCard.build(card)
        val files = unzip(service.buildPass(card, vCard, photo()))

        assertTrue(
            files.keys.containsAll(
                listOf("pass.json", "manifest.json", "signature", "icon.png", "icon@2x.png", "logo.png", "thumbnail.png")
            ), files.keys.toString()
        )
        val mapper = ObjectMapper()
        val manifest = mapper.readTree(files["manifest.json"])
        files.filterKeys { it != "manifest.json" && it != "signature" }.forEach { (name, content) ->
            val sha1 = MessageDigest.getInstance("SHA-1").digest(content).joinToString("") { "%02x".format(it) }
            assertEquals(sha1, manifest[name].asText(), name)
        }
        assertEquals(files.size - 2, manifest.size())

        val pass = mapper.readTree(files["pass.json"])
        assertEquals("pass.test.businesscard", pass["passTypeIdentifier"].asText())
        assertEquals("TEAM123456", pass["teamIdentifier"].asText())
        assertEquals("card-1", pass["serialNumber"].asText())
        assertEquals("Kai Reinhard", pass["generic"]["primaryFields"][0]["value"].asText())
        assertEquals("PKBarcodeFormatQR", pass["barcodes"][0]["format"].asText())
        assertEquals(vCard, pass["barcodes"][0]["message"].asText())

        // Detached signature of the manifest, with the pass certificate and the WWDR certificate in it.
        val signed = CMSSignedData(CMSProcessableByteArray(files["manifest.json"]!!), files["signature"]!!)
        val signer = signed.signerInfos.signers.single()
        assertTrue(signer.verify(JcaSimpleSignerInfoVerifierBuilder().setProvider(bc).build(passCertificate)))
        val embedded = signed.certificates.getMatches(null).size
        assertEquals(2, embedded)
        assertTrue(wwdr.encoded.isNotEmpty())
        // A changed manifest doesn't verify any more.
        val tampered = CMSSignedData(CMSProcessableByteArray("{}".toByteArray()), files["signature"]!!)
        assertFalse(
            tampered.signerInfos.signers.single()
                .runCatching { verify(JcaSimpleSignerInfoVerifierBuilder().setProvider(bc).build(passCertificate)) }
                .getOrDefault(false)
        )
    }

    @Test
    fun `pass without photo has no thumbnail`(@TempDir dir: File) {
        val (service, _, _) = service(dir)
        val files = unzip(service.buildPass(BusinessCard().also { it.cardName = "x"; it.lastName = "Y" }, "BEGIN:VCARD"))
        assertFalse(files.containsKey("thumbnail.png"))
        assertTrue(files.containsKey("signature"))
    }
}
