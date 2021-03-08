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

package org.projectforge.ui

import org.projectforge.jira.JiraUtils

/**
 * For showing JIRA issues as links, if JIRA is configured.
 * @see [JiraUtils.isJiraConfigured]
 */
object JiraSupport {
    /**
     * @param text Text to parse for JIRA issues.
     * @param element If given, a tooltip "tooltip.jiraSupport.field.content" is added to this element. If JIRA isn't configured, no tooltip will be set,
     * @return JIRA element if JIRA is configured and given text seems to contain JIRA issues, otherwise null.
     */
    @JvmStatic
    @JvmOverloads
    fun createJiraElement(text: String?, element: UILabelledElement? = null): UICustomized? {
        if (!JiraUtils.isJiraConfigured()) {
            return null
        }
        element?.let {
            it.tooltip = "tooltip.jiraSupport.field.content"
        }
        if (text.isNullOrBlank()) {
            return null
        }
        val jiraIdentifiers = JiraUtils.checkForJiraIssues(text)
        if (jiraIdentifiers.isNullOrEmpty()) {
            return null
        }
        val jiraIssues = mutableMapOf<String, String>()
        jiraIdentifiers.forEach {
            jiraIssues[it] = JiraUtils.buildJiraIssueBrowseLinkUrl(it)
        }
        val jiraIssuesElement = UICustomized("jira.issuesLinks")
        jiraIssuesElement.add("jiraIssues", jiraIssues)
        return jiraIssuesElement
    }
}
