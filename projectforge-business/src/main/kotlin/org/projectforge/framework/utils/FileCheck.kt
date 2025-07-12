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

package org.projectforge.framework.utils

import org.projectforge.common.FileUtils.calculateBytes
import org.projectforge.common.FileUtils.checkExtension
import org.projectforge.common.FileUtils.checkMaxFileSize
import org.projectforge.common.extensions.formatBytesForUser
import org.projectforge.framework.i18n.translateMsg
import java.io.File

object FileCheck {
    /**
     * Calls [checkFile] with the file's absolute path and length.
     * @see checkFile
     */
    fun checkFile(
        file: File,
        vararg extensions: String,
        bytes: Long = 0,
        kiloBytes: Long = 0,
        megaBytes: Long = 0,
        gigaBytes: Long = 0
    ): String? {
        return checkFile(
            file.absolutePath,
            file.length(),
            *extensions,
            bytes = bytes,
            kiloBytes = kiloBytes,
            megaBytes = megaBytes,
            gigaBytes = gigaBytes,
        )
    }

    /**
     * Checks if the file has a valid extension and does not exceed the specified maximum size.
     * If the file does not have a valid extension, it returns an error message key for unsupported format.
     * If the file exceeds the maximum size, it returns an error message key for size exceeded.
     * If both checks pass, it returns null.
     *
     * @param file The file to check.
     * @param extensions The allowed file extensions.
     * @param bytes The maximum allowed file size in bytes.
     * @param kiloBytes The maximum allowed file size in kilobytes.
     * @param megaBytes The maximum allowed file size in megabytes.
     * @param gigaBytes The maximum allowed file size in gigabytes.
     * @return An error message key if any check fails, or null if all checks pass.
     */
    fun checkFile(
        fileName: String,
        fileSize: Long,
        vararg extensions: String,
        bytes: Long = 0,
        kiloBytes: Long = 0,
        megaBytes: Long = 0,
        gigaBytes: Long = 0
    ): String? {
        if (!checkExtension(fileName, *extensions)) {
            return translateMsg("file.upload.error.unsupportedFormat", extensions.joinToString(", "))
        }
        if (!checkMaxFileSize(fileSize, bytes, kiloBytes, megaBytes, gigaBytes)) {
            return translateMsg(
                "file.upload.error.maxSizeOfExceeded",
                calculateBytes(bytes, kiloBytes, megaBytes, gigaBytes).formatBytesForUser(),
            )
        }
        return null
    }
}
