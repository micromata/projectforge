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
import org.projectforge.framework.jcr.Attachment

class Script(
  var name: String? = null,
  var type: ScriptDO.ScriptType? = null,
  var description: String? = null,
  var script: String? = null,
  var parameter1: ScriptParameter? = null,
  var parameter2: ScriptParameter? = null,
  var parameter3: ScriptParameter? = null,
  var parameter4: ScriptParameter? = null,
  var parameter5: ScriptParameter? = null,
  var parameter6: ScriptParameter? = null,
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
    parameter1 = list[0]
    parameter2 = list[1]
    parameter3 = list[2]
    parameter4 = list[3]
    parameter5 = list[4]
    parameter6 = list[5]
  }

  override fun copyTo(dest: ScriptDO) {
    super.copyTo(dest)
    val list = listOf(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6)
    dest.setParameterList(list)
  }
}
