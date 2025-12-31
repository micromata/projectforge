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

package org.projectforge.jira

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.TestSetup
import org.projectforge.business.timesheet.TimesheetDO

class JiraUtilsTest {
  @Test
  fun parseJiraIssues() {
    val nullString: String? = null
    Assertions.assertNull(JiraUtils.parseJiraIssues(nullString))
    Assertions.assertNull(JiraUtils.parseJiraIssues(""))
    check(arrayOf("P-0"), JiraUtils.parseJiraIssues("P-0"))
    check(arrayOf("PF-0"), JiraUtils.parseJiraIssues("PF-0"))
    check(arrayOf("PF-222"), JiraUtils.parseJiraIssues("PF-222"))
    check(arrayOf("PF-1"), JiraUtils.parseJiraIssues("PF-1"))
    check(arrayOf("PF-1"), JiraUtils.parseJiraIssues("Worked on PF-1."))
    check(arrayOf("PF1DF-1"), JiraUtils.parseJiraIssues("Worked on PF1DF-1."))
    check(
      arrayOf("PF-222", "PROJECT2-123"),
        JiraUtils.parseJiraIssues("Worked on PF-222 and PROJECT2-123 and finished this work.")
    )
    Assertions.assertNull(JiraUtils.parseJiraIssues("PF222"))
    check(arrayOf("PF-222"), JiraUtils.parseJiraIssues("1234PF-222"))
    check(arrayOf("PF-2", "PF-10", "PF-070"), JiraUtils.parseJiraIssues("Worked on PF-2 and PF-10.", "PF-070"))

    val task = TaskDO()
    task.title = "Activities on PF-10 and PF-9"
    task.shortDescription = "Former activities on PF-2"
    task.description = "Also activities on PF-100"

    check(arrayOf("PF-2", "PF-9", "PF-10", "PF-100"), JiraUtils.parseJiraIssues(task))

    val timesheet = TimesheetDO()
    timesheet.description = "Worked on PF-42"
    timesheet.reference = "And on PF-7"
    check(arrayOf("PF-7", "PF-42"), JiraUtils.parseJiraIssues(timesheet))

    timesheet.task = task
    check(arrayOf("PF-2", "PF-7", "PF-9", "PF-10", "PF-42", "PF-100"), JiraUtils.parseJiraIssues(timesheet))
    timesheet.description = "Worked on PF-7"
    check(arrayOf("PF-2", "PF-7", "PF-9", "PF-10", "PF-100"), JiraUtils.parseJiraIssues(timesheet))


    check(arrayOf("PF-222"), JiraUtils.parseJiraIssuesForProject("PF", "PF-222", "PROJECTFORGE-12"))
    check(arrayOf("PROJECTFORGE-12"), JiraUtils.parseJiraIssuesForProject("projectforge", "PF-222", "PROJECTFORGE-12"))
    check(arrayOf("PF-222", "PROJECTFORGE-12"), JiraUtils.parseJiraIssuesForProject("*", "PF-222", "PROJECTFORGE-12"))
  }

  @Test
  fun hasJiraIssues() {
    Assertions.assertFalse(JiraUtils.hasJiraIssues(null))
    Assertions.assertFalse(JiraUtils.hasJiraIssues(""))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("P-0"))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("PF-0"))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("PF-222"))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("PF-1"))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("Worked on PF-1."))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("Worked on PF1DF-1."))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("Worked on PF-222 and PROJECT2-123 and finished this work."))
    Assertions.assertFalse(JiraUtils.hasJiraIssues("PF222"))
    Assertions.assertTrue(JiraUtils.hasJiraIssues("1234PF-222"))
  }

  @Test
  fun buildJiraIssueBrowseLinkUrl() {
    Assertions.assertEquals(JIRA_BASE_URL + "PF-222", JiraUtils.buildJiraIssueBrowseLinkUrl("PF-222"))
  }

  @Test
  fun buildJiraIssueLink() {
    Assertions.assertEquals(
      "<a href=\"" + JIRA_BASE_URL + "PF-222" + "\">PF-222</a>",
        JiraUtils.buildJiraIssueBrowseLink("PF-222")
    )
    Assertions.assertEquals(
      "<a href=\"" + JIRA_ACME_BASE_URL + "PORTAL-222" + "\">PORTAL-222</a>",
        JiraUtils.buildJiraIssueBrowseLink("PORTAL-222")
    )
  }

  @Test
  fun linkJiraIssues() {
    Assertions.assertEquals(JiraUtils.buildJiraIssueBrowseLink("PF-222"), JiraUtils.linkJiraIssues("PF-222"))
    Assertions.assertEquals(" " + JiraUtils.buildJiraIssueBrowseLink("PF-222"), JiraUtils.linkJiraIssues(" PF-222"))
    Assertions.assertEquals(JiraUtils.buildJiraIssueBrowseLink("PF-222") + " ", JiraUtils.linkJiraIssues("PF-222 "))
    Assertions.assertEquals(
      "Worked on " + JiraUtils.buildJiraIssueBrowseLink("PF-222") + ".",
        JiraUtils.linkJiraIssues("Worked on PF-222.")
    )
    Assertions.assertEquals(
      "Worked on " + JiraUtils.buildJiraIssueBrowseLink("PF-222") + " and "
          + JiraUtils.buildJiraIssueBrowseLink("PF-1") + ".",
        JiraUtils.linkJiraIssues("Worked on PF-222 and PF-1.")
    )
  }

  private fun check(expected: Array<String>, array: Array<String>?) {
    Assertions.assertEquals(expected.size, array!!.size)
    for (i in expected.indices) {
      Assertions.assertEquals(expected[i], array[i])
    }
  }

  companion object {
    const val JIRA_BASE_URL = "https://jira.acme.com/jira/browse/"
    const val JIRA_ACME_BASE_URL = "https://customer.acme.com/jira/browse/"

    @BeforeAll
    @JvmStatic
    fun setUp() {
      TestSetup.init()
    }
  }
}
