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

package org.projectforge.common

import java.io.File

object FileUtils {
    /**
     * Return the given path itself if it is already absolute, otherwise absolute path of given path relative to given parent.
     * @param parent
     * @param path
     * @return
     */
    @JvmStatic
    fun getAbsolutePath(parent: String, path: String): String {
        var file = File(path)
        if (file.isAbsolute) {
            return path
        }
        file = File(parent, path)
        return file.absolutePath
    }

    fun createFile(vararg path: String): File {
        if (path.isEmpty()) {
            return File("")
        }
        var file = File(path[0])
        for (i in 1 until path.size) {
            file = File(file, path[i])
        }
        return file
    }

    fun createFile(parent: File, vararg path: String): File {
        if (path.isEmpty()) {
            return parent
        }
        var file = parent
        for (i in 0 until path.size) {
            file = File(file, path[i])
        }
        return file
    }

    fun checkExtension(file: File, vararg extensions: String): Boolean {
        return checkExtension(file.absolutePath, *extensions)
    }

    /**
     * Verifies if a given file name has an extension that matches any from the specified list of extensions.
     * This method works case-insensitively and also allows for checking if the file has no extension at all.
     *
     * @param fileName The name of the file whose extension is to be checked.
     * @param extensions A variable-length list of acceptable file extensions for comparison.
     * @return `true` if the file name has no extension, and the list of extensions is empty, or if the file
     *         extension matches any of the specified extensions; otherwise, `false`.
     */
    fun checkExtension(fileName: String, vararg extensions: String): Boolean {
        if (extensions.isEmpty()) {
            return true
        }
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()
        for (extension in extensions) {
            if (fileExtension == extension.lowercase()) {
                return true
            }
        }
        return false
    }

    /**
     * Calls [checkMaxFileSize] with the file's length.
     * @see checkMaxFileSize
     */
    fun checkMaxFileSize(
        file: File,
        bytes: Long = 0,
        kiloBytes: Long = 0,
        megaBytes: Long = 0,
        gigaBytes: Long = 0,
    ): Boolean {
        return checkMaxFileSize(file.length(), bytes, kiloBytes, megaBytes, gigaBytes)
    }

    /**
     * Checks if the file size is within the specified maximum size in bytes, kilobytes, megabytes, and gigabytes.
     * If all sizes are zero or negative, it returns true, indicating no size limit.
     * All parameters are optional and default to zero.
     * The maximum size is calculated as the sum of all provided sizes.
     * @param fileSize The file size to check.
     * @param bytes The maximum allowed file size in bytes.
     * @param kiloBytes The maximum allowed file size in kilobytes.
     * @param megaBytes The maximum allowed file size in megabytes.
     * @param gigaBytes The maximum allowed file size in gigabytes.
     * @return `true` if the file size is within the limit, or if there is no limit; `false` otherwise.
     */
    fun checkMaxFileSize(
        fileSize: Long,
        bytes: Long = 0,
        kiloBytes: Long = 0,
        megaBytes: Long = 0,
        gigaBytes: Long = 0,
    ): Boolean {
        val max = gigaBytes * GB + megaBytes * MB + kiloBytes * KB + bytes
        if (max <= 0) {
            return true // No size limit
        }
        return fileSize <= max
    }

    private const val KB = 1024

    private const val MB = KB * 1024

    private const val GB = MB * 1024
}
