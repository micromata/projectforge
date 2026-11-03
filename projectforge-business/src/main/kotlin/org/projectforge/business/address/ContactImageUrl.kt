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
import java.net.URI

/**
 * The public url of the contact image of an address, e.g. as the photo of a digital business card:
 * `<base>/rsPublic/contactImage/{token}`.
 *
 * The token is random (256 bit, see [AddressImageDao.getOrCreatePublicToken]) and belongs to the image, not to the
 * address: nothing in the url can be derived or guessed. Replacing the image keeps the url (it then shows the new image), deleting the image revokes it. The
 * token is pushed to the gateway with the address, so main instance and gateway deliver the image under the same url.
 *
 * The base is, in this order: `projectforge.businessCard.photoBaseUrl` (overrides everything, e.g. a reverse proxy),
 * the public url of the gateway (`projectforge.gateway.push.publicUrl`, or the push url without the sync api path),
 * the domain of this instance (`projectforge.domain` plus context path, unless it is localhost).
 */
@Component
open class ContactImageUrl(
    @param:Value("\${projectforge.businessCard.photoBaseUrl:}")
    private val photoBaseUrlConfig: String = "",
    @param:Value("\${projectforge.gateway.push.publicUrl:}")
    private val gatewayPublicUrlConfig: String = "",
    @param:Value("\${projectforge.gateway.push.url:}")
    private val gatewayPushUrl: String = "",
    @param:Value("\${projectforge.domain:}")
    private val domain: String = "",
    @param:Value("\${projectforge.servletContextPath:}")
    private val contextPath: String = "",
) {
    /**
     * The root url the images are reachable at for browsers (without trailing slash). Blank, if there is none.
     */
    open val baseUrl: String =
        photoBaseUrlConfig.trim().trimEnd('/').ifBlank {
            gatewayPublicUrlConfig.trim().ifBlank { gatewayPushUrl.trim().trimEnd('/').removeSuffix(SYNC_API_PATH) }
                .trimEnd('/')
        }.ifBlank { domainUrl() }

    /**
     * @return The url of the image with the given token, or null if there is no token or base url.
     */
    open fun url(token: String?): String? {
        if (token.isNullOrBlank() || baseUrl.isBlank()) {
            return null
        }
        return "$baseUrl$PATH$token"
    }

    private fun domainUrl(): String {
        val root = domain.trim().trimEnd('/')
        if (root.isBlank() || isLocal(root)) {
            return ""
        }
        val context = contextPath.trim().trim('/')
        return if (context.isBlank()) root else "$root/$context"
    }

    private fun isLocal(url: String): Boolean {
        val host = try {
            URI(url).host
        } catch (_: Exception) {
            null
        }?.lowercase() ?: return true
        return host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]" || host.endsWith(".localhost")
    }

    companion object {
        /**
         * The uid under which the address is known on the gateway (the one pushed by the main instance).
         */
        fun addressUid(address: AddressDO): String = addressUid(address.uid, address.id)

        /** [addressUid] of an address read as single columns (uid, id), e.g. by a query. */
        fun addressUid(uid: String?, id: Any?): String = uid ?: "pf-$id"

        const val PATH = "/rsPublic/contactImage/"
        const val SYNC_API_PATH = "/api/gateway/sync"
    }
}
