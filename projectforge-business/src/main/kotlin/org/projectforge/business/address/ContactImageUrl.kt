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

package org.projectforge.business.address

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The public url of the contact image of an address on the gateway:
 * `<gateway>/rsPublic/contactImage/{base64url(addressUid)}/{signature}`.
 *
 * The signature is an HMAC of the address uid, so only urls handed out by the main instance deliver an image and
 * nobody gets one by trying uids. It is stateless: the gateway knows nothing about the business cards, it only checks
 * the signature and delivers the image of the address (which the address push brought there anyway). Both instances
 * share `projectforge.security.authenticationTokenEncryptionKey`, the signing key is derived from it for this
 * purpose only.
 *
 * A url stays valid as long as the address and its image exist: it can't be revoked, except by deleting the image or
 * changing the key.
 */
@Component
open class ContactImageUrl(
    @param:Value("\${projectforge.security.authenticationTokenEncryptionKey:}")
    private val encryptionKey: String = "",
    @param:Value("\${projectforge.gateway.push.publicUrl:}")
    private val gatewayPublicUrlConfig: String = "",
    @param:Value("\${projectforge.gateway.push.url:}")
    private val gatewayPushUrl: String = "",
) {
    private val signingKey: ByteArray? = encryptionKey.takeIf { it.isNotBlank() }?.let {
        hmac(it.toByteArray(Charsets.UTF_8), PURPOSE)
    }

    /**
     * The root url of the gateway for browsers (without trailing slash): projectforge.gateway.push.publicUrl, or the
     * push url without the sync api path. Blank, if there is no gateway.
     */
    open val gatewayPublicUrl: String =
        gatewayPublicUrlConfig.ifBlank { gatewayPushUrl.trimEnd('/').removeSuffix(SYNC_API_PATH) }.trimEnd('/')

    /**
     * @return The url, or null if there is no gateway or no key to sign with.
     */
    open fun url(addressUid: String?): String? {
        if (addressUid.isNullOrBlank() || gatewayPublicUrl.isBlank()) {
            return null
        }
        val signature = sign(addressUid) ?: return null
        return "$gatewayPublicUrl$PATH${encodeUid(addressUid)}/$signature"
    }

    open fun url(address: AddressDO): String? = url(addressUid(address))

    /**
     * @return base64url signature of the address uid or null, if there is no key.
     */
    open fun sign(addressUid: String): String? {
        val key = signingKey ?: return null
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(key, addressUid))
    }

    /**
     * Checks the signature in constant time.
     */
    open fun verify(addressUid: String?, signature: String?): Boolean {
        if (addressUid.isNullOrBlank() || signature.isNullOrBlank()) {
            return false
        }
        val expected = sign(addressUid) ?: return false
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), signature.toByteArray(Charsets.UTF_8))
    }

    private fun hmac(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun encodeUid(uid: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(uid.toByteArray(Charsets.UTF_8))

    companion object {
        /**
         * The uid under which the address is known on the gateway (the one pushed by the main instance).
         */
        fun addressUid(address: AddressDO): String = addressUid(address.uid, address.id)

        /** [addressUid] of an address read as single columns (uid, id), e.g. by a query. */
        fun addressUid(uid: String?, id: Any?): String = uid ?: "pf-$id"

        /**
         * @return The uid of the path segment, null if it isn't valid base64url.
         */
        fun decodeUid(pathSegment: String?): String? {
            pathSegment ?: return null
            return try {
                String(Base64.getUrlDecoder().decode(pathSegment), Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        const val PATH = "/rsPublic/contactImage/"
        const val SYNC_API_PATH = "/api/gateway/sync"
        private const val PURPOSE = "contactImage"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
