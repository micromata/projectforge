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

package org.projectforge.persistence.jpa

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDO.Companion.TITLE_LENGTH
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.jpa.EntityMetaDataRegistry
import org.projectforge.jira.JiraUtils.buildJiraIssueBrowseLink
import org.projectforge.jira.JiraUtils.buildJiraIssueBrowseLinkUrl
import org.projectforge.jira.JiraUtils.hasJiraIssues
import org.projectforge.jira.JiraUtils.linkJiraIssues
import org.projectforge.jira.JiraUtils.parseJiraIssues
import org.projectforge.jira.JiraUtils.parseJiraIssuesForProject
import org.projectforge.test.TestSetup.init

class EntityMetaDataTest {
  @Test
  fun entityMetaDataTest() {
    val column = EntityMetaDataRegistry.getColumnMetaData(TaskDO::class.java, "title")
    Assertions.assertEquals("title", column!!.name)
    Assertions.assertEquals(TITLE_LENGTH, column.length)
    Assertions.assertFalse(column.nullable)
  }
}
