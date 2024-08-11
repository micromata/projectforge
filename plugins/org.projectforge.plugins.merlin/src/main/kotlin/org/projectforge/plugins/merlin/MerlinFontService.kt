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

package org.projectforge.plugins.merlin

import com.lowagie.text.pdf.BaseFont
import mu.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import jakarta.annotation.PostConstruct


private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class MerlinFontService {
  @Autowired
  private lateinit var configurationService: ConfigurationService

  private val fonts = mutableMapOf<String, BaseFont>()

  fun getFont(name: String?): BaseFont? {
    name ?: return null
    return fonts[getCanonicalFontname(name)]
  }

  @PostConstruct
  private fun postConstruct() {
    val dir = File(configurationService.fontsDir)
    if (!dir.exists()) {
      log.warn("Can't find fonts directory, so no additional fonts available: ${dir.absolutePath}'")
      return
    }
    dir.list()?.forEach { filename ->
      val file = File(dir, filename)
      if (file.extension == "ttf" || file.extension == "otf") {
        log.info { "Proceeding font '${file.absolutePath}'..." }
        val baseFont = BaseFont.createFont(file.absolutePath, "Identity-H", BaseFont.EMBEDDED)
        val name = getCanonicalFontname(baseFont.postscriptFontName)
        fonts[name] = baseFont
      }
    }
  }

  private fun getCanonicalFontname(fontName: String): String {
    return fontName.replace("[ -._]".toRegex(), "")
  }
}
