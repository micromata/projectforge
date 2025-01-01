/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

enum class ImageType(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), GIF("gif", "image/gif");

    fun asVCardImageType(): ezvcard.parameter.ImageType {
        return when (this) {
            JPEG -> ezvcard.parameter.ImageType.JPEG
            PNG -> ezvcard.parameter.ImageType.PNG
            GIF -> ezvcard.parameter.ImageType.GIF
        }
    }

    companion object {
        fun fromExtension(filename: String?): ImageType? {
            filename ?: return null
            return if (filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true)) {
                JPEG
            } else if (filename.endsWith(".png", ignoreCase = true)) {
                PNG
            } else if (filename.endsWith(".gif", ignoreCase = true)) {
                GIF
            } else {
                null
            }
        }

        fun fromString(value: String?): ImageType? {
            value ?: return null
            return when (value.lowercase()) {
                "jpg", "jpeg" -> JPEG
                "png" -> PNG
                "gif" -> GIF
                else -> null
            }
        }

        fun from(imageType: ezvcard.parameter.ImageType): ImageType? {
            return when (imageType) {
                ezvcard.parameter.ImageType.JPEG -> JPEG
                ezvcard.parameter.ImageType.PNG -> PNG
                ezvcard.parameter.ImageType.GIF -> GIF
                else -> null
            }
        }
    }
}
