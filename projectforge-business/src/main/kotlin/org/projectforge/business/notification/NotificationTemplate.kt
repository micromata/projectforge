/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.business.notification

import org.projectforge.business.utils.HtmlHelper
import org.projectforge.framework.utils.HtmlSanitizer

/**
 * Renders subject and text of a rule: the variables `{{key}}` are replaced (HTML-escaped in the text), unknown
 * ones stay as they are; the text is sanitized afterwards.
 */
object NotificationTemplate {
    private val VARIABLE_REGEX = """\{\{\s*([a-zA-Z0-9_]+)\s*}}""".toRegex()

    /** The plain text subject. */
    fun renderSubject(subject: String?, variables: Map<String, String>): String {
        return replaceVariables(subject ?: "", variables, escape = false).trim()
    }

    /** The sanitized HTML text. */
    fun renderText(text: String?, variables: Map<String, String>): String {
        return HtmlSanitizer.sanitize(replaceVariables(text ?: "", variables, escape = true))
    }

    /** Replaces the known variables `{{key}}`, HTML-escaped if [escape]; unknown ones stay as they are. */
    fun replaceVariables(text: String, variables: Map<String, String>, escape: Boolean): String {
        return VARIABLE_REGEX.replace(text) { match ->
            val value = variables[match.groupValues[1]] ?: return@replace match.value
            if (escape) HtmlHelper.escapeHtml(value) else value
        }
    }
}
