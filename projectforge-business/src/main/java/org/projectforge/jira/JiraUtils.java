/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.jira;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.Configuration;

public class JiraUtils
{
  private static final String PATTERN = "([A-Z][A-Z_0-9]*-[0-9]+)";

  /**
   * PROJECTFORGE-222 -> https://jira.acme.com/jira/browse/PROJECTFORGE-222.
   * @param jiraIssue
   * @return
   */
  public static String buildJiraIssueBrowseLinkUrl(final String jiraIssue)
  {
    return ConfigXml.getInstance().getJiraBrowseBaseUrl() + jiraIssue;
  }

  /**
   * PROJECTFORGE-222 -> <a href="https://jira.acme.com/jira/browse/PROJECTFORGE-222">PROJECTFORGE-222</a>.
   * @param jiraIssue
   * @return
   */
  public static String buildJiraIssueBrowseLink(final String jiraIssue)
  {
    return "<a href=\"" + ConfigXml.getInstance().getJiraBrowseBaseUrl() + jiraIssue + "\">" + jiraIssue + "</a>";
  }

  /**
   * If no JIRA browse base url is set in the configuration this method returns always null.
   * @param text
   * @return {@link #parseJiraIssues(String)}
   * @see Configuration#getJiraBrowseBaseUrl()
   */
  public static String[] checkForJiraIssues(final String text)
  {
    if (ConfigXml.getInstance().getJiraBrowseBaseUrl() == null) {
      return null;
    }
    return parseJiraIssues(text);
  }

  public static boolean hasJiraIssues(final String text)
  {
    if (StringUtils.isBlank(text)) {
      return false;
    }
    final Pattern p = Pattern.compile(PATTERN, Pattern.MULTILINE);
    final Matcher m = p.matcher(text);
    return m.find();
  }

  /**
   * Replaces all found jira issues by links to JIRA.
   * @param text should be already html escaped.
   * @return text where jira issues are replaced via html url.
   * @see #checkForJiraIssues(String)
   */
  public static String linkJiraIssues(final String text)
  {
    final String[] jiraIssues = checkForJiraIssues(text);
    if (jiraIssues == null) {
      return text;
    }
    final StringBuffer buf = new StringBuffer();
    int current = 0;
    for (final String jiraIssue : jiraIssues) {
      final int pos = text.indexOf(jiraIssue, current);
      buf.append(text.substring(current, pos));
      buf.append(buildJiraIssueBrowseLink(jiraIssue));
      current = pos + jiraIssue.length();
    }
    if (current < text.length()) {
      buf.append(text.substring(current));
    }
    return buf.toString();
  }

  /**
   * Returns found matches for JIRA issues: UPPERCASE_LETTERS-###: [A-Z][A-Z_0-9]*-[0-9]+
   * @param text
   * @return
   */
  public static String[] parseJiraIssues(final String text)
  {
    if (StringUtils.isBlank(text)) {
      return null;
    }
    List<String> list = null;
    final Pattern p = Pattern.compile(PATTERN, Pattern.MULTILINE);
    final Matcher m = p.matcher(text);
    while (m.find()) {
      if (list == null) {
        list = new ArrayList<String>();
      }
      if (m.group(1) != null)
        list.add(m.group(1));
    }
    if (list == null) {
      return null;
    }
    final String[] result = new String[list.size()];
    return (String[]) list.toArray(result);
  }
}
