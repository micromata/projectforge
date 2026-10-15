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

package org.projectforge.jcr.store

import org.projectforge.common.ZipMode

/**
 * Compression of the stored content. The compression is done on the payload, so after an optional AES encryption.
 * Encrypted or already compressed payloads (zip, pdf, images, office documents...) aren't compressed again, because
 * this costs CPU without saving any space.
 */
enum class CompressionType {
    NONE,
    GZIP;

    companion object {
        /**
         * Extensions of formats which are already compressed (docx, xlsx etc. are zip files).
         */
        private val COMPRESSED_EXTENSIONS = setOf(
            "zip", "gz", "tgz", "bz2", "xz", "7z", "rar", "jar", "war", "zst",
            "pdf",
            "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "avif",
            "mp3", "mp4", "m4a", "m4v", "mov", "avi", "mkv", "webm", "ogg", "aac",
            "docx", "xlsx", "pptx", "docm", "xlsm", "pptm", "odt", "ods", "odp", "epub", "pages", "numbers", "key",
        )

        fun choose(fileName: String?, aesEncrypted: Boolean?, zipMode: ZipMode?): CompressionType {
            if (aesEncrypted == true || zipMode != null) {
                return NONE
            }
            val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
            return if (COMPRESSED_EXTENSIONS.contains(extension)) NONE else GZIP
        }
    }
}
