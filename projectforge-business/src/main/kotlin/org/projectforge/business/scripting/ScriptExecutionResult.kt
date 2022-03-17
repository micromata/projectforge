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

package org.projectforge.business.scripting

import org.projectforge.business.utils.HtmlHelper
import java.io.Serializable

class ScriptExecutionResult(val scriptLogger: ScriptLogger) : Serializable {
    @Transient
    var result: Any? = null
    @Transient
    var exception: Exception? = null
    var output: String? = null
    /**
     * The effective script (including any auto-imports and bindings).
     */
    var script: String = ""

    fun hasResult(): Boolean {
        return result != null
    }

    /**
     * Escapes all html characters. If Groovy result is from type string then all '\n' will be replaced by "<br></br>\n".
     *
     * @return
     */
    val resultAsHtmlString: String?
        get() = if (result == null) {
            null
        } else HtmlHelper.escapeHtml(result.toString(), true)

    fun hasException(): Boolean {
        return exception != null
    }

    companion object {
        private const val serialVersionUID = -4561647483563741849L
    }
}
