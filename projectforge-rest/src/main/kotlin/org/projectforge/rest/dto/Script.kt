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

package org.projectforge.rest.dto

import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptParameter
import org.projectforge.business.scripting.ScriptParameterType
import org.projectforge.business.task.TaskDO
import org.projectforge.business.user.UserDao
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.TimePeriod
import org.projectforge.registry.Registry
import java.math.BigDecimal
import java.time.LocalDate

class Script(
  var name: String? = null,
  var type: ScriptDO.ScriptType? = null,
  var description: String? = null,
  var script: String? = null,
  var parameter1: Param? = null,
  var parameter2: Param? = null,
  var parameter3: Param? = null,
  var parameter4: Param? = null,
  var parameter5: Param? = null,
  var parameter6: Param? = null,
  /**
   * Filename of older scripts, managed by classic Wicket version:
   */
  var filename: String? = null,
  var availableVariables: String? = "",
  override var attachmentsCounter: Int? = null,
  override var attachmentsSize: Long? = null,
  override var attachments: List<Attachment>? = null,
) : BaseDTO<ScriptDO>(), AttachmentsSupport {

  override fun copyFrom(src: ScriptDO) {
    super.copyFrom(src)
    val list = src.getParameterList(true)
    parameter1 = Param.from(list[0])
    parameter2 = Param.from(list[1])
    parameter3 = Param.from(list[2])
    parameter4 = Param.from(list[3])
    parameter5 = Param.from(list[4])
    parameter6 = Param.from(list[5])
  }

  override fun copyTo(dest: ScriptDO) {
    super.copyTo(dest)
    val list = listOf(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6)
    dest.setParameterList(list.map { it?.asScriptParameter() })
  }

  fun updateParameter(index: Int, parameter: ScriptParameter) {
    when (index) {
      0 -> parameter1 = updateParameter(parameter1, parameter)
      1 -> parameter2 = updateParameter(parameter2, parameter)
      2 -> parameter3 = updateParameter(parameter3, parameter)
      3 -> parameter4 = updateParameter(parameter4, parameter)
      4 -> parameter5 = updateParameter(parameter5, parameter)
      5 -> parameter6 = updateParameter(parameter6, parameter)
      else -> throw IllegalArgumentException("index $index not supported. Must be 0..5.")
    }
  }

  private fun updateParameter(param: Script.Param?, parameter: ScriptParameter): Script.Param {
    return if (param == null || param.type == parameter.type) {
      Param.from(parameter)
    } else {
      // Don't update parameter, return itself.
      param
    }
  }

  fun getParameters(): List<ScriptParameter> {
    val params = mutableListOf<ScriptParameter>()
    parameter1?.let { params.add(it.asScriptParameter()) }
    parameter2?.let { params.add(it.asScriptParameter()) }
    parameter3?.let { params.add(it.asScriptParameter()) }
    parameter4?.let { params.add(it.asScriptParameter()) }
    parameter5?.let { params.add(it.asScriptParameter()) }
    parameter6?.let { params.add(it.asScriptParameter()) }
    return params
  }

  class Param(
    var name: String? = null,
    var type: ScriptParameterType? = null,
    var stringValue: String? = null,
    var intValue: Int? = null,
    var decimalValue: BigDecimal? = null,
    var dateValue: LocalDate? = null,
    var toDateValue: LocalDate? = null,
    var userValue: User? = null,
    var taskValue: Task? = null,
  ) {
    fun asScriptParameter(): ScriptParameter {
      val result = ScriptParameter()
      result.type = type
      result.parameterName = name
      when (type) {
        ScriptParameterType.STRING -> result.stringValue = this.stringValue
        ScriptParameterType.INTEGER -> result.intValue = this.intValue
        ScriptParameterType.DECIMAL -> result.decimalValue = this.decimalValue
        ScriptParameterType.DATE ->
          result.dateValue = this.dateValue
        ScriptParameterType.TIME_PERIOD ->
          result.timePeriodValue = TimePeriod(this.dateValue, this.toDateValue)
        ScriptParameterType.TASK -> {
          this.taskValue.let { task ->
            if (task?.id == null) {
              result.task = null
            } else {
              val taskDO = TaskDO()
              taskDO.id = task.id
              result.task = taskDO
            }
          }
        }
        ScriptParameterType.USER -> {
          this.userValue.let { user ->
            if (user?.id == null) {
              result.user = null
            } else {
              val userDO = PFUserDO()
              userDO.id = user.id
              result.user = userDO
            }
          }
        }
        null -> {}
      }
      return result
    }

    companion object {
      fun from(parameter: ScriptParameter): Param {
        val result = Param()
        result.name = parameter.parameterName
        result.type = parameter.type
        parameter.type?.let { type ->
          when (type) {
            ScriptParameterType.STRING -> result.stringValue = parameter.stringValue
            ScriptParameterType.INTEGER -> result.intValue = parameter.intValue
            ScriptParameterType.DECIMAL -> result.decimalValue = parameter.decimalValue
            ScriptParameterType.DATE ->
              result.dateValue = parameter.dateValue
            ScriptParameterType.TIME_PERIOD -> {
              result.dateValue = parameter.timePeriodValue?.fromDay
              result.toDateValue = parameter.timePeriodValue?.toDay
            }
            ScriptParameterType.TASK -> {
              parameter.intValue?.let { taskId ->
                val task = Task()
                task.id = taskId
                result.taskValue = task
              }
            }
            ScriptParameterType.USER ->
              parameter.intValue?.let { userId ->
                Registry.getInstance().getDao(UserDao::class.java).internalGetById(userId)?.let { userDO ->
                  val user = User()
                  user.copyFromMinimal(userDO)
                  result.userValue = user
                }
              }
          }
        }
        return result
      }
    }
  }
}
