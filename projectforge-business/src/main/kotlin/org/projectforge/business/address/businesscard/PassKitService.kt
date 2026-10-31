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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

/**
 * Builds and signs the Apple Wallet pass (`.pkpass`) of a business card. A `.pkpass` is a ZIP with `pass.json`, the
 * images, `manifest.json` (SHA-1 of each file) and `signature` (detached PKCS#7 of the manifest).
 *
 * Style `generic`, QR code with the vCard of the card. The photo is embedded (`thumbnail.png`): Wallet doesn't load
 * remote images. The pass is not updated afterwards (no web service).
 */
@Service
open class PassKitService {
    @Autowired
    private lateinit var walletConfiguration: WalletConfiguration

    private class Signer(val key: PrivateKey, val certificates: List<X509Certificate>)

    private val bouncyCastle = BouncyCastleProvider()

    @Volatile
    private var signer: Signer? = null

    open fun isConfigured(): Boolean = walletConfiguration.isConfigured()

    /**
     * @param vCard Content of the QR code, see [BusinessCardService.buildVCard].
     * @param photo The photo (any image format) to embed, null for a pass without photo.
     * @param translate Translates the i18n keys of the field labels (user's language).
     * @return The content of the `.pkpass` file.
     * @throws IllegalStateException if the Wallet isn't configured or the certificates can't be read.
     */
    open fun buildPass(card: BusinessCard, vCard: String, photo: ByteArray? = null,
        translate: (String) -> String = { it },
    ): ByteArray {
        check(isConfigured()) { "Wallet is not configured." }
        val files = linkedMapOf<String, ByteArray>()
        val customLogo = loadCustomLogo()
        files["pass.json"] = passJson(card, vCard, translate, customLogo != null)
        val logo = customLogo ?: loadDefaultLogo()
        // The logo box of a generic pass is 160 x 50 points.
        files["logo.png"] = png(scale(logo, 160, 50))
        files["logo@2x.png"] = png(scale(logo, 320, 100))
        files["logo@3x.png"] = png(scale(logo, 480, 150))
        files["icon.png"] = png(square(logo, 58))
        files["icon@2x.png"] = png(square(logo, 116))
        files["icon@3x.png"] = png(square(logo, 174))
        photo?.let { bytes ->
            readImage(bytes)?.let { image ->
                files["thumbnail.png"] = png(scale(image, 180, 180))
                files["thumbnail@2x.png"] = png(scale(image, 360, 360))
            }
        }
        val manifest = MAPPER.writeValueAsBytes(files.mapValues { sha1(it.value) })
        files["manifest.json"] = manifest
        files["signature"] = sign(manifest)
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun passJson(
        card: BusinessCard, vCard: String, translate: (String) -> String, customLogo: Boolean,
    ): ByteArray {
        val name = listOfNotNull(card.title.clean(), card.firstName.clean(), card.lastName.clean()).joinToString(" ")
            .ifEmpty { card.organization.clean() ?: card.cardName.clean() ?: "" }
        val back = mutableListOf<Map<String, Any>>()
        fun back(key: String, label: String, value: String?, detector: String? = null) {
            value.clean()?.let {
                back.add(buildMap {
                    put("key", key); put("label", label); put("value", it)
                    detector?.let { d -> put("dataDetectorTypes", listOf(d)) }
                })
            }
        }
        back("email", translate("businessCard.email"), card.email, "PKDataDetectorTypeLink")
        back("phone", translate("businessCard.businessPhone"), card.businessPhone, "PKDataDetectorTypePhoneNumber")
        back("mobile", translate("businessCard.mobilePhone"), card.mobilePhone, "PKDataDetectorTypePhoneNumber")
        back("web", translate("businessCard.website"), card.website, "PKDataDetectorTypeLink")
        val address = listOfNotNull(
            card.street.clean(), listOfNotNull(card.zipCode.clean(), card.city.clean()).joinToString(" ").clean(),
            card.country.clean(),
        ).joinToString("\n")
        back("address", translate("address.addressText"), address, "PKDataDetectorTypeAddress")
        back("note", translate("businessCard.comment"), card.comment)
        val generic = mutableMapOf<String, Any>(
            "primaryFields" to listOf(mapOf("key" to "name", "label" to translate("name"), "value" to name)),
        )
        card.positionText.clean()?.let {
            generic["secondaryFields"] =
                listOf(mapOf("key" to "position", "label" to translate("businessCard.wallet.jobTitle"), "value" to it))
        }
        card.organization.clean()?.let {
            generic["auxiliaryFields"] =
                listOf(mapOf("key" to "company", "label" to translate("businessCard.wallet.company"), "value" to it))
        }
        if (back.isNotEmpty()) {
            generic["backFields"] = back
        }
        val pass = mutableMapOf(
            "formatVersion" to 1,
            "passTypeIdentifier" to walletConfiguration.passTypeIdentifier,
            "teamIdentifier" to walletConfiguration.teamIdentifier,
            "organizationName" to walletConfiguration.organizationName,
            "serialNumber" to (card.id ?: card.cardName ?: name),
            "description" to (card.cardName.clean() ?: name),
            "backgroundColor" to walletConfiguration.backgroundColor,
            "foregroundColor" to walletConfiguration.foregroundColor,
            "labelColor" to walletConfiguration.labelColor,
            "generic" to generic,
            "barcodes" to listOf(
                mapOf(
                    "format" to "PKBarcodeFormatQR",
                    "message" to vCard,
                    "messageEncoding" to "utf-8",
                    "altText" to (card.cardName.clean() ?: name),
                )
            ),
        )
        if (!customLogo) {
            // The bundled logo says nothing about the owner: show its name next to it. A customized logo (the
            // company logo) stands for itself.
            pass["logoText"] = walletConfiguration.organizationName
        }
        return MAPPER.writeValueAsBytes(pass)
    }

    private fun sign(manifest: ByteArray): ByteArray {
        val signer = loadSigner()
        val generator = CMSSignedDataGenerator()
        generator.addSignerInfoGenerator(
            JcaSimpleSignerInfoGeneratorBuilder().setProvider(bouncyCastle)
                .build("SHA256with${signer.key.algorithm}", signer.key, signer.certificates.first())
        )
        generator.addCertificates(JcaCertStore(signer.certificates))
        // Detached: the manifest isn't part of the signature file.
        return generator.generate(CMSProcessableByteArray(manifest), false).encoded
    }

    private fun loadSigner(): Signer {
        signer?.let { return it }
        synchronized(this) {
            signer?.let { return it }
            val keyStore = KeyStore.getInstance("PKCS12", bouncyCastle)
            val password = walletConfiguration.passCertificatePassword.toCharArray()
            File(walletConfiguration.passCertificatePath).inputStream().use { keyStore.load(it, password) }
            val alias = keyStore.aliases().asSequence().firstOrNull { keyStore.isKeyEntry(it) }
                ?: throw IllegalStateException("The Wallet certificate file contains no private key.")
            val key = keyStore.getKey(alias, password) as PrivateKey
            val certificate = keyStore.getCertificate(alias) as X509Certificate
            val wwdr = readCertificate(File(walletConfiguration.wwdrCertificatePath).readBytes())
            val result = Signer(key, listOf(certificate, wwdr))
            signer = result
            log.info { "Wallet certificate loaded: ${certificate.subjectX500Principal}, valid until ${certificate.notAfter}." }
            return result
        }
    }

    /** DER (as downloaded from Apple) or PEM. */
    private fun readCertificate(bytes: ByteArray): X509Certificate {
        val text = String(bytes, Charsets.ISO_8859_1)
        val der = if (text.contains("BEGIN CERTIFICATE")) {
            PEMParser(StringReader(text)).use { (it.readObject() as org.bouncycastle.cert.X509CertificateHolder).encoded }
        } else {
            bytes
        }
        return CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(der)) as X509Certificate
    }

    /** The customized logo, null if there is none or it isn't an image ImageIO reads (e.g. svg). */
    private fun loadCustomLogo(): BufferedImage? {
        val file = walletConfiguration.customLogoFile() ?: return null
        return readImage(file.readBytes())
    }

    private fun loadDefaultLogo(): BufferedImage {
        val bytes = javaClass.getResourceAsStream("/images/default-logo.png")?.use { it.readBytes() }
            ?: throw IllegalStateException("No logo for the Wallet pass.")
        return readImage(bytes) ?: throw IllegalStateException("The bundled Wallet logo is not an image.")
    }

    private fun readImage(bytes: ByteArray): BufferedImage? {
        return try {
            ImageIO.read(ByteArrayInputStream(bytes))
        } catch (ex: Exception) {
            log.warn { "Image for the Wallet pass can't be read: ${ex.message}" }
            null
        }
    }

    /** Fits into the box, keeps the aspect ratio, never enlarges. */
    private fun scale(image: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        val factor = minOf(1.0, maxWidth.toDouble() / image.width, maxHeight.toDouble() / image.height)
        val width = maxOf(1, (image.width * factor).toInt())
        val height = maxOf(1, (image.height * factor).toInt())
        return draw(image, width, height, width, height)
    }

    /** Centered on a transparent square, scaled to fit (the icon of the pass). */
    private fun square(image: BufferedImage, size: Int): BufferedImage {
        val factor = minOf(size.toDouble() / image.width, size.toDouble() / image.height)
        return draw(image, maxOf(1, (image.width * factor).toInt()), maxOf(1, (image.height * factor).toInt()), size, size)
    }

    private fun draw(image: BufferedImage, width: Int, height: Int, canvasWidth: Int, canvasHeight: Int): BufferedImage {
        val result = BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB)
        val g = result.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(image, (canvasWidth - width) / 2, (canvasHeight - height) / 2, width, height, null)
        } finally {
            g.dispose()
        }
        return result
    }

    private fun png(image: BufferedImage): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    private fun sha1(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private val MAPPER = ObjectMapper()
    }
}
