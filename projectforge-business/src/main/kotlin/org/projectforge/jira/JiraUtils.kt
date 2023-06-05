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

package org.projectforge.jira

import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.configuration.ConfigXml
import java.util.regex.Pattern

object JiraUtils {
  private const val PATTERN = "([A-Z][A-Z_0-9]*-[0-9]+)"
  val isJiraConfigured: Boolean
    get() = ConfigXml.getInstance().isJIRAConfigured

  /**
   * PROJECTFORGE-222 -> https://jira.acme.com/jira/browse/PROJECTFORGE-222.
   *
   * @param jiraIssue
   * @return
   */
  @JvmStatic
  fun buildJiraIssueBrowseLinkUrl(jiraIssue: String): String {
    return "${getJiraBrowseBaseUrl(jiraIssue)}$jiraIssue"
  }

  private fun getJiraBrowseBaseUrl(jiraIssue: String): String {
    val config = ConfigXml.getInstance()
    config.jiraServers?.forEach { server ->
      server.baseUrl?.let { baseUrl ->
        if (server.projects?.any { jiraIssue.startsWith(it) } == true) {
          return getNonBlankServerUrl(baseUrl)
        }
      }
    }
    return getNonBlankServerUrl(config.jiraBrowseBaseUrl)
  }

  private fun getNonBlankServerUrl(baseUrl: String?): String {
    return if (baseUrl.isNullOrBlank()) {
      "config-xml-jiraBrowseBaseUrl-undefined"
    } else {
      baseUrl
    }
  }

  /**
   * PROJECTFORGE-222 -> [PROJECTFORGE-222](https://jira.acme.com/jira/browse/PROJECTFORGE-222).
   *
   * @param jiraIssue
   * @return
   */
  @JvmStatic
  fun buildJiraIssueBrowseLink(jiraIssue: String): String {
    return "<a href=\"${getJiraBrowseBaseUrl(jiraIssue)}$jiraIssue\">$jiraIssue</a>"
  }

  /**
   * If no JIRA browse base url is set in the configuration this method returns always null.
   *
   * @param text
   * @return [.parseJiraIssues]
   * @see ConfigXml.isJIRAConfigured
   */
  @JvmStatic
  fun checkForJiraIssues(text: String?): Array<String>? {
    return if (!ConfigXml.getInstance().isJIRAConfigured) {
      null
    } else parseJiraIssues(text)
  }

  @JvmStatic
  fun hasJiraIssues(text: String?): Boolean {
    if (text.isNullOrBlank()) {
      return false
    }
    val p = Pattern.compile(PATTERN, Pattern.MULTILINE)
    val m = p.matcher(text)
    return m.find()
  }

  /**
   * Replaces all found jira issues by links to JIRA.
   *
   * @param text should be already html escaped.
   * @return text where jira issues are replaced via html url.
   * @see .checkForJiraIssues
   */
  @JvmStatic
  fun linkJiraIssues(text: String?): String? {
    text ?: return null
    val jiraIssues = checkForJiraIssues(text) ?: return text
    var result = text
    for (jiraIssue in jiraIssues) {
      result = result?.replace(jiraIssue, buildJiraIssueBrowseLink(jiraIssue))
    }
    return result
  }

  /**
   * Returns found matches for JIRA issues: UPPERCASE_LETTERS-###: [A-Z][A-Z_0-9]*-[0-9]+
   *
   * @param text
   * @return
   */
  @JvmStatic
  fun parseJiraIssues(vararg text: String?): Array<String>? {
    return parseJiraIssuesForProject(null, *text)
  }

  /**
   * Returns found matches for JIRA issues: UPPERCASE_LETTERS-###: [A-Z][A-Z_0-9]*-[0-9]+
   *
   * @param project ID of Jira project (PROJECTFORGE, ...). (case-insensitive)
   * @param text
   * @return
   */
  @JvmStatic
  fun parseJiraIssuesForProject(project: String?, vararg text: String?): Array<String>? {
    val result = mutableSetOf<JiraIssue>()
    text.forEach { str ->
      parseJiraIssues(project, result, str)
    }
    return if (result.isEmpty()) {
      null
    } else {
      result.sorted().map { it.toString() }.toTypedArray()
    }
  }

  /**
   * Parses title, shortDescription and description of task.
   */
  @JvmStatic
  @JvmOverloads
  fun parseJiraIssues(task: TaskDO?, project: String? = null): Array<String>? {
    task ?: return null
    return parseJiraIssuesForProject(project, task.title, task.shortDescription, task.description)
  }

  /**
   * Parses description and reference of given timesheet and task.title, task.shortDescription and task.description.
   */
  @JvmStatic
  @JvmOverloads
  fun parseJiraIssues(timesheet: TimesheetDO, project: String? = null): Array<String>? {
    val task = timesheet.task
    return parseJiraIssuesForProject(
      project,
      timesheet.description,
      timesheet.reference,
      task?.title,
      task?.shortDescription,
      task?.description
    )
  }

  private fun parseJiraIssues(project: String?, result: MutableSet<JiraIssue>, text: String?) {
    if (text.isNullOrBlank()) {
      return
    }
    val p = Pattern.compile(PATTERN, Pattern.MULTILINE)
    val m = p.matcher(text)
    while (m.find()) {
      if (m.group(1) != null) {
        val issue = JiraIssue(m.group(1))
        if (project == null || project.trim() == "*" || issue.project.equals(project, ignoreCase = true)) {
          result.add(issue)
        }
      }
    }
  }

  private class JiraIssue(val str: String) : Comparable<JiraIssue> {
    val project: String
    val number: Int

    init {
      val parts = str.split('-')
      project = parts[0]
      number = parts[1].toInt()
    }

    override fun compareTo(other: JiraIssue): Int {
      val cmp = project.compareTo(other.project)
      if (cmp != 0) {
        return cmp
      }
      return number.compareTo(other.number)
    }

    override fun equals(other: Any?): Boolean {
      if (other == null || other !is JiraIssue) {
        return false
      }
      return str == other.str
    }

    override fun hashCode(): Int {
      return str.hashCode()
    }

    override fun toString(): String {
      return str
    }
  }
}
