/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.jira.JiraUtils;
import org.projectforge.test.TestSetup;

import static org.junit.jupiter.api.Assertions.*;

public class JiraUtilsTest
{
  public static final String JIRA_BASE_URL = "https://jira.acme.com/jira/browse/";

  @BeforeAll
  public static void setUp()
  {
    TestSetup.init();
  }

  @Test
  public void parseJiraIssues()
  {
    assertNull(JiraUtils.parseJiraIssues(null));
    assertNull(JiraUtils.parseJiraIssues(""));
    check(new String[] { "P-0" }, JiraUtils.parseJiraIssues("P-0"));
    check(new String[] { "PF-0" }, JiraUtils.parseJiraIssues("PF-0"));
    check(new String[] { "PF-222" }, JiraUtils.parseJiraIssues("PF-222"));
    check(new String[] { "PF-1" }, JiraUtils.parseJiraIssues("PF-1"));
    check(new String[] { "PF-1" }, JiraUtils.parseJiraIssues("Worked on PF-1."));
    check(new String[] { "PF1DF-1" }, JiraUtils.parseJiraIssues("Worked on PF1DF-1."));

    check(new String[] { "PF-222", "PROJECT2-123" },
        JiraUtils.parseJiraIssues("Worked on PF-222 and PROJECT2-123 and finished this work."));

    assertNull(JiraUtils.parseJiraIssues("PF222"));
    check(new String[] { "PF-222" }, JiraUtils.parseJiraIssues("1234PF-222"));
  }

  @Test
  public void hasJiraIssues()
  {
    assertFalse(JiraUtils.hasJiraIssues(null));
    assertFalse(JiraUtils.hasJiraIssues(""));
    assertTrue(JiraUtils.hasJiraIssues("P-0"));
    assertTrue(JiraUtils.hasJiraIssues("PF-0"));
    assertTrue(JiraUtils.hasJiraIssues("PF-222"));
    assertTrue(JiraUtils.hasJiraIssues("PF-1"));
    assertTrue(JiraUtils.hasJiraIssues("Worked on PF-1."));
    assertTrue(JiraUtils.hasJiraIssues("Worked on PF1DF-1."));
    assertTrue(JiraUtils.hasJiraIssues("Worked on PF-222 and PROJECT2-123 and finished this work."));
    assertFalse(JiraUtils.hasJiraIssues("PF222"));
    assertTrue(JiraUtils.hasJiraIssues("1234PF-222"));
  }

  @Test
  public void buildJiraIssueBrowseLinkUrl()
  {
    assertEquals(JIRA_BASE_URL + "PF-222", JiraUtils.buildJiraIssueBrowseLinkUrl("PF-222"));
  }

  @Test
  public void buildJiraIssueLink()
  {
    assertEquals("<a href=\"" + JIRA_BASE_URL + "PF-222" + "\">PF-222</a>",
        JiraUtils.buildJiraIssueBrowseLink("PF-222"));
  }

  @Test
  public void linkJiraIssues()
  {
    assertEquals(JiraUtils.buildJiraIssueBrowseLink("PF-222"), JiraUtils.linkJiraIssues("PF-222"));
    assertEquals(" " + JiraUtils.buildJiraIssueBrowseLink("PF-222"), JiraUtils.linkJiraIssues(" PF-222"));
    assertEquals(JiraUtils.buildJiraIssueBrowseLink("PF-222") + " ", JiraUtils.linkJiraIssues("PF-222 "));
    assertEquals("Worked on " + JiraUtils.buildJiraIssueBrowseLink("PF-222") + ".",
        JiraUtils.linkJiraIssues("Worked on PF-222."));
    assertEquals("Worked on " + JiraUtils.buildJiraIssueBrowseLink("PF-222") + " and "
        + JiraUtils.buildJiraIssueBrowseLink("PF-1") + ".",
        JiraUtils.linkJiraIssues("Worked on PF-222 and PF-1."));
  }

  private void check(final String[] expected, final String[] array)
  {
    assertEquals(expected.length, array.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], array[i]);
    }
  }
}
