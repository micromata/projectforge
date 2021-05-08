/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.excel

import de.micromata.merlin.excel.ExcelColumnDef
import de.micromata.merlin.excel.ExcelSheet
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.i18n.translate

/**
 * For excel export.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ExcelUtils {
  /**
   * Registers an excel column by using the translated i18n-key of the given property as column head and the
   * property name as alias (for refering and [de.micromata.merlin.excel.ExcelRow.autoFillFromObject].
   */
  @JvmStatic
  @JvmOverloads
  fun registerColumn(sheet: ExcelSheet, clazz: Class<*>, property: String, size: Int? = null): ExcelColumnDef {
    val i18nKey = PropUtils.getI18nKey(clazz, property) ?: property
    val colDef = sheet.registerColumn(translate(i18nKey), property)
    size?.let { colDef.withSize(it) }
    return colDef
  }

  object Size {
    const val DATE = 10
    const val EMAIL = 30
    const val EXTRA_LONG = 80
    const val ID = 10
    const val PHONENUMBER = 20
    const val STANDARD = 30
    const val ZIPCODE = 7
  }
}
