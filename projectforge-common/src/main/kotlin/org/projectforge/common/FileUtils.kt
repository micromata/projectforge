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
}
