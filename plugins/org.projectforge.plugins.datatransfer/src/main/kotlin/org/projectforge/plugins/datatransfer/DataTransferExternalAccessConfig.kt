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

package org.projectforge.plugins.datatransfer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Where the external access (download/upload) of data transfer areas runs: on this instance, on the gateway, or
 * nowhere (see [mode]).
 */
@Service
class DataTransferExternalAccessConfig(
    @param:Value("\${projectforge.gateway.enabled:false}")
    val gatewayMode: Boolean = false,
    @param:Value("\${projectforge.gateway.push.enabled:false}")
    private val gatewayPushEnabled: Boolean = false,
    @param:Value("\${projectforge.gateway.push.url:}")
    private val gatewayPushUrl: String = "",
    @param:Value("\${projectforge.gateway.push.publicUrl:}")
    private val gatewayPublicUrlConfig: String = "",
    /**
     * true: external access on this instance, even with a gateway. false: no external access at all. Unset: on
     * this instance without a gateway, via the gateway otherwise.
     */
    @param:Value("\${projectforge.datatransfer.externalAccess.localAllowed:#{null}}")
    private val localAllowed: Boolean? = null,
) {
    enum class Mode {
        /** External access is configured and used on this instance. */
        LOCAL,

        /**
         * External access is configured here, but the areas are pushed to the gateway: the link, the external
         * download/upload and the files of these areas are there.
         */
        VIA_GATEWAY,

        /** External access can't be switched on (only off for areas that still have it). */
        NONE,
    }

    val mode: Mode = when {
        localAllowed == true -> Mode.LOCAL
        localAllowed == false -> Mode.NONE
        gatewayPushEnabled -> Mode.VIA_GATEWAY
        else -> Mode.LOCAL
    }

    val viaGateway: Boolean
        get() = mode == Mode.VIA_GATEWAY

    /**
     * The root url of the gateway for browsers (context path included, without trailing slash):
     * projectforge.gateway.push.publicUrl, or the push url without the sync API path.
     */
    val gatewayPublicUrl: String =
        gatewayPublicUrlConfig.ifBlank { gatewayPushUrl.trimEnd('/').removeSuffix(SYNC_API_PATH) }.trimEnd('/')

    /**
     * Whether the external access of this area isn't served by this instance, but by the gateway.
     */
    fun isOnGateway(area: DataTransferAreaDO): Boolean {
        return viaGateway && area.externalAccessEnabled && !area.isPersonalBox()
    }

    companion object {
        const val SYNC_API_PATH = "/api/gateway/sync"

        /** The public page of an area (next), relative to the root url: followed by the access token. */
        const val PUBLIC_PAGE_PATH = "/next/public/datatransfer/"
    }
}
