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

package org.projectforge.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object NumberOfBytes {
  const val KILO_BYTES = 1024L
  const val MEGA_BYTES = KILO_BYTES * 1024
  const val GIGA_BYTES = MEGA_BYTES * 1024
  const val TERRA_BYTES = GIGA_BYTES * 1024

  @JvmStatic
  val KILO_BYTES_BD = BigDecimal(KILO_BYTES)
  @JvmStatic
  val MEGA_BYTES_BD = BigDecimal(MEGA_BYTES)
  @JvmStatic
  val GIGA_BYTES_BD = BigDecimal(GIGA_BYTES)
  @JvmStatic
  val TERRA_BYTES_BD = BigDecimal(TERRA_BYTES)
}
