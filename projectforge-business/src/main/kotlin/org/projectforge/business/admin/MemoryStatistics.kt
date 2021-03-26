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

package org.projectforge.business.admin

import mu.KotlinLogging
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal
import java.math.RoundingMode

class MemoryStatistics(
  val max: Long,
  val used: Long,
  val committed: Long,
  val init: Long
) {
  override fun toString(): String {
    val percent = if (max > 0 && used < max) {
      " (${
        NumberFormatter.format(
          BigDecimal(used).multiply(NumberHelper.HUNDRED)
            .divide(BigDecimal(max), 0, RoundingMode.HALF_UP), 0
        )
      }%)"
    } else {
      ""
    }
    val max = if (max > 0) {
      " / ${NumberHelper.formatBytes(max)}"
    } else {
      ""
    }
    val used = NumberHelper.formatBytes(used)

    return "used=[$used$max]$percent, committed=[${NumberHelper.formatBytes(committed)}], init=[${
      NumberHelper.formatBytes(
        init
      )
    }]"
  }

}
