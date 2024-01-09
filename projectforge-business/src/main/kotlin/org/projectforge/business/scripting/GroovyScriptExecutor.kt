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

package org.projectforge.business.scripting

import org.projectforge.framework.configuration.ApplicationContextProvider

class GroovyScriptExecutor : ScriptExecutor() {
  /**
   * @param script Common imports will be prepended.
   * @param variables Variables to bind. Variables are usable via binding["key"] or directly, if #autobind# is part of script.
   * @see GroovyExecutor.executeTemplate
   */
  override fun execute(): ScriptExecutionResult {
    val groovyExecutor = ApplicationContextProvider.getApplicationContext().getBean(GroovyExecutor::class.java)
    return groovyExecutor.execute(scriptExecutionResult, effectiveScript, allVariables)
  }
}
