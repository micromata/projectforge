/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.word.AbstractConditional

/**
 * Represents info of an conditional expression.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MerlinConditional(conditional: AbstractConditional) {
  var statement: String? = null
  var variable: String? = null
  var childConditionals = mutableListOf<MerlinConditional>()

  init {
    statement = conditional.conditionalStatement
    variable = conditional.variable
    conditional.childConditionals?.forEach { child ->
      childConditionals.add(MerlinConditional(child))
    }
  }

  fun usesVariable(name: String): Boolean {
    if (name == variable) {
      return true
    }
    childConditionals.forEach { child ->
      if (child.usesVariable(name)) {
        return true
      }
    }
    return false
  }

  internal fun asMarkDown(sb: StringBuilder, indent: String) {
    sb.appendLine("$indent* `$statement`")
    childConditionals.forEach { child ->
      child.asMarkDown(sb, "$indent  ")
    }
  }
}
