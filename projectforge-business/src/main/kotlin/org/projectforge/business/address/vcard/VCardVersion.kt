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

package org.projectforge.business.address.vcard

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * VCard version to use for generating vcard strings.
 */
enum class VCardVersion(internal val ezVCardType: ezvcard.VCardVersion) {
    /**
     * VCard 3.0 is supported by Apple Address Book und Mac OS X.
     */
    V_3_0(ezvcard.VCardVersion.V3_0), V_4_0(ezvcard.VCardVersion.V4_0);

    companion object {
        fun from(str: String): VCardVersion {
            return when (str) {
                "3.0" -> V_3_0
                "4.0" -> V_4_0
                else -> {
                    log.error { "Unknown VCardVersion: $str, using 3.0 at default." }
                    V_3_0
                }
            }
        }
    }
}
