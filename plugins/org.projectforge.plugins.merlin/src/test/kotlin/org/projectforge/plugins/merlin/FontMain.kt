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

package org.projectforge.plugins.merlin

import com.lowagie.text.pdf.BaseFont
import java.io.File

class FontMain {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val dir = File("${System.getProperty("user.home")}/ProjectForge/resources/fonts")
      if (!dir.exists()) {
        println("******** Can't find fonts directory, so no additional fonts available: ${dir.absolutePath}'")
        return
      }
      dir.list()?.forEach { filename ->
        val file = File(dir, filename)
        if (file.extension == "otf") {
          // Thanx to: https://stackoverflow.com/questions/7821024/retrieving-font-name-from-font-file-in-java
          val font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, file)
          val name: String = font.getName()
          println("name=${name.replace("[ -._]".toRegex(), "")}")
          val baseFont = BaseFont.createFont(file.absolutePath, "Identity-H", BaseFont.EMBEDDED)
          println("name: ${baseFont.getPostscriptFontName()}")
        }
      }

    }
  }
}
