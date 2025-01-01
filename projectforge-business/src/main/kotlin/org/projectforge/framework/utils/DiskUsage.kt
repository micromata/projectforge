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

import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DiskUsage() {
  constructor(dir: File?) : this() {
    dir?.let { path = dir.toPath() }
    inititalize()
  }

  constructor(dir: String?) : this() {
    if (!dir.isNullOrBlank()) {
      path = Paths.get(dir)
    }
    inititalize()
  }

  var totalSpace: Long = 0
    private set
  var freeSpace: Long = 0
    private set
  var used: Long = 0
    private set
  var percentage: Int = 0
    private set
  var path: Path? = null
    private set

  private fun inititalize() {
    path?.let {
      if (it.toFile().exists()) {
        val store = Files.getFileStore(it)
        totalSpace = store.totalSpace
        freeSpace = store.usableSpace
        used = FileUtils.sizeOfDirectory(it.toFile())
        percentage = (100L * used / totalSpace).toInt()
      }
    }
  }
}

