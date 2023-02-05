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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.jira.JiraUtils.buildJiraIssueBrowseLink
import org.projectforge.jira.JiraUtils.buildJiraIssueBrowseLinkUrl
import org.projectforge.jira.JiraUtils.hasJiraIssues
import org.projectforge.jira.JiraUtils.linkJiraIssues
import org.projectforge.jira.JiraUtils.parseJiraIssues
import org.projectforge.jira.JiraUtils.parseJiraIssuesForProject
import org.projectforge.test.TestSetup.init

class JiraUtilsTest {
  @Test
  fun parseJiraIssues() {
    val nullString: String? = null
    Assertions.assertNull(parseJiraIssues(nullString))
    Assertions.assertNull(parseJiraIssues(""))
    check(arrayOf("P-0"), parseJiraIssues("P-0"))
    check(arrayOf("PF-0"), parseJiraIssues("PF-0"))
    check(arrayOf("PF-222"), parseJiraIssues("PF-222"))
    check(arrayOf("PF-1"), parseJiraIssues("PF-1"))
    check(arrayOf("PF-1"), parseJiraIssues("Worked on PF-1."))
    check(arrayOf("PF1DF-1"), parseJiraIssues("Worked on PF1DF-1."))
    check(
      arrayOf("PF-222", "PROJECT2-123"),
      parseJiraIssues("Worked on PF-222 and PROJECT2-123 and finished this work.")
    )
    Assertions.assertNull(parseJiraIssues("PF222"))
    check(arrayOf("PF-222"), parseJiraIssues("1234PF-222"))
    check(arrayOf("PF-2", "PF-10", "PF-070"), parseJiraIssues("Worked on PF-2 and PF-10.", "PF-070"))

    val task = TaskDO()
    task.title = "Activities on PF-10 and PF-9"
    task.shortDescription = "Former activities on PF-2"
    task.description = "Also activities on PF-100"

    check(arrayOf("PF-2", "PF-9", "PF-10", "PF-100"), parseJiraIssues(task))

    val timesheet = TimesheetDO()
    timesheet.description = "Worked on PF-42"
    timesheet.reference = "And on PF-7"
    check(arrayOf("PF-7", "PF-42"), parseJiraIssues(timesheet))

    timesheet.task = task
    check(arrayOf("PF-2", "PF-7", "PF-9", "PF-10", "PF-42", "PF-100"), parseJiraIssues(timesheet))
    timesheet.description = "Worked on PF-7"
    check(arrayOf("PF-2", "PF-7", "PF-9", "PF-10", "PF-100"), parseJiraIssues(timesheet))


    check(arrayOf("PF-222"), parseJiraIssuesForProject("PF","PF-222", "PROJECTFORGE-12"))
    check(arrayOf("PROJECTFORGE-12"), parseJiraIssuesForProject("projectforge","PF-222", "PROJECTFORGE-12"))
    check(arrayOf("PF-222", "PROJECTFORGE-12"), parseJiraIssuesForProject("*","PF-222", "PROJECTFORGE-12"))
  }

  @Test
  fun hasJiraIssues() {
    Assertions.assertFalse(hasJiraIssues(null))
    Assertions.assertFalse(hasJiraIssues(""))
    Assertions.assertTrue(hasJiraIssues("P-0"))
    Assertions.assertTrue(hasJiraIssues("PF-0"))
    Assertions.assertTrue(hasJiraIssues("PF-222"))
    Assertions.assertTrue(hasJiraIssues("PF-1"))
    Assertions.assertTrue(hasJiraIssues("Worked on PF-1."))
    Assertions.assertTrue(hasJiraIssues("Worked on PF1DF-1."))
    Assertions.assertTrue(hasJiraIssues("Worked on PF-222 and PROJECT2-123 and finished this work."))
    Assertions.assertFalse(hasJiraIssues("PF222"))
    Assertions.assertTrue(hasJiraIssues("1234PF-222"))
  }

  @Test
  fun buildJiraIssueBrowseLinkUrl() {
    Assertions.assertEquals(JIRA_BASE_URL + "PF-222", buildJiraIssueBrowseLinkUrl("PF-222"))
  }

  @Test
  fun buildJiraIssueLink() {
    Assertions.assertEquals(
      "<a href=\"" + JIRA_BASE_URL + "PF-222" + "\">PF-222</a>",
      buildJiraIssueBrowseLink("PF-222")
    )
    Assertions.assertEquals(
      "<a href=\"" + JIRA_ACME_BASE_URL + "PORTAL-222" + "\">PORTAL-222</a>",
      buildJiraIssueBrowseLink("PORTAL-222")
    )
  }

  @Test
  fun linkJiraIssues() {
    Assertions.assertEquals(buildJiraIssueBrowseLink("PF-222"), linkJiraIssues("PF-222"))
    Assertions.assertEquals(" " + buildJiraIssueBrowseLink("PF-222"), linkJiraIssues(" PF-222"))
    Assertions.assertEquals(buildJiraIssueBrowseLink("PF-222") + " ", linkJiraIssues("PF-222 "))
    Assertions.assertEquals(
      "Worked on " + buildJiraIssueBrowseLink("PF-222") + ".",
      linkJiraIssues("Worked on PF-222.")
    )
    Assertions.assertEquals(
      "Worked on " + buildJiraIssueBrowseLink("PF-222") + " and "
          + buildJiraIssueBrowseLink("PF-1") + ".",
      linkJiraIssues("Worked on PF-222 and PF-1.")
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
      init()
    }
  }
}
