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

package org.projectforge.ui

import org.projectforge.business.scripting.ScriptDO

/**
 * UIEditor is used for e. g. source code editing (Kotlin/Groovy-scripts).
 */
class UIEditor(
  val id: String,
  var mode: String = "kotlin",
  type: ScriptDO.ScriptType? = null,
  /**
   * Optional height of editor, default is '600px' (by client React component if null)
   */
  var height: String? = null,
) : UIElement(UIElementType.EDITOR) {
  init {
    type?.let {
      mode = if (type == ScriptDO.ScriptType.GROOVY) {
        "groovy"
      } else {
        "kotlin"
      }
    }
  }
}
