/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import fr.opensagres.xdocreport.itext.extension.font.IFontProvider
import mu.KotlinLogging
import java.awt.Color

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MerlinFontProvider(val fontService: MerlinFontService) : IFontProvider {
  override fun getFont(familyName: String?, encoding: String?, size: Float, style: Int, color: Color?): Font {
    // Thanx to: https://github.com/opensagres/xdocreport/issues/129
    //log.info { "Trying to get font familyName='$familyName', encoding='$encoding', size=$size, style=$style, color=$color" }
    fontService.getFont(familyName)?.let { baseFont ->
      try {
        return Font(baseFont, size, style, color)
      } catch (ex: Exception) {
        log.error("Error while creating font: ${ex.message}", ex)
        throw RuntimeException(ex)
      }
    }
    return FontFactory.getFont(familyName, encoding, size, style, color)
  }
}
