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

package org.projectforge.rest.task

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal
import java.math.RoundingMode

class Consumption(
  /**
   * 350PT/188PT
   */
  val title: String,
  val status: Status,
  val percentage: Int?,
  /**
   * If overbooked, the colors will be changed and barPercentage will be 0-100, but percentage will be >100.
   * barPercentage = if (percentage > 100) 100 / percentage else percentage
   */
  val barPercentage: Int?,
  val id: Int?,
) {

  enum class Status {
    @JsonProperty("progress-done")
    DONE,

    @JsonProperty("progress-none")
    NONE,

    @JsonProperty("progress-80")
    PROGRESS_80,

    @JsonProperty("progress-90")
    PROGRESS_90,

    @JsonProperty("progress-overbooked")
    OVERBOOKED,

    @JsonProperty("progress-overbooked-min")
    OVERBOOKED_MIN
  }

  companion object {
    fun create(node: TaskNode): Consumption? {
      val maxHours = node.task.maxHours
      val finished = node.isFinished
      val taskTree = TaskTree.getInstance()
      val maxDays = if (maxHours != null && maxHours.toInt() == 0) {
        null
      } else {
        NumberHelper.setDefaultScale(taskTree.getPersonDays(node))
      }
      var usage = BigDecimal(node.getDuration(taskTree, true)).divide(
        DateHelper.SECONDS_PER_WORKING_DAY, 2,
        RoundingMode.HALF_UP
      )
      usage = NumberHelper.setDefaultScale(usage)!!

      val percentage = if (maxDays != null && maxDays.toDouble() > 0)
        usage.divide(maxDays, 2, RoundingMode.HALF_UP).multiply(NumberHelper.HUNDRED).toInt()
      else
        0
      var barPercentage = if (percentage <= 100) percentage else 10000 / percentage
      if (barPercentage < 0) {
        barPercentage = 0
      }
      //bar.add(AttributeModifier.replace("class", "progress"))
      val status =
        if (percentage <= 80 || finished && percentage <= 100) {
          if (percentage > 0) {
            Status.DONE
          } else {
            Status.NONE
            //progressLabel.setVisible(false)
          }
        } else if (percentage <= 90) {
          Status.PROGRESS_80
        } else if (percentage <= 100) {
          Status.PROGRESS_90
        } else if (finished && percentage <= 110) {
          Status.OVERBOOKED_MIN
        } else {
          Status.OVERBOOKED
        }
      if (maxDays == null && usage.compareTo(BigDecimal.ZERO) == 0) {
        return null
      }
      val locale = ThreadLocalUserContext.getLocale()
      val usageStr = NumberHelper.getNumberFractionFormat(locale, usage.scale()).format(usage)
      val unitStr = translate("projectmanagement.personDays.short")
      val maxValueStr =
        if (maxDays != null) {
          "/${NumberHelper.getNumberFractionFormat(locale, maxDays.scale()).format(maxDays)}$unitStr ($percentage%)"
        } else {
          ""
        }
      val title = "$usageStr$unitStr$maxValueStr"
      return Consumption(title, status, percentage = percentage, barPercentage = barPercentage, id = node.taskId)
    }
  }
}
