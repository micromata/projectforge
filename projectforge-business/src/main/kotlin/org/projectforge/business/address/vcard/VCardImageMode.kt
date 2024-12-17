/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Supported: "NONE", "EMBEDDED" or "URL".
 * If [ImageMode.URL] is being used, the image URL is provided in the vCard.
 * If [ImageMode.NONE] is being used, no image is stored.
 * If [ImageMode.EMBEDDED] is being used, the image will be embedded in the vCard.
 */
enum class VCardImageMode {
    NONE, EMBEDDED, URL
}
