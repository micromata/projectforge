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

package org.projectforge.jira;

import java.io.Serializable;
import java.util.List;

import org.projectforge.framework.configuration.ConfigXml;

/**
 * Basic configuration of the JIRA ProjectForge is connected to.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class JiraConfig implements Serializable
{
  private static final long serialVersionUID = -427784191871257457L;

  private final String createIssueUrl = null;

  private List<JiraIssueType> issueTypes;

  /**
   * Base url for creating JIRA issues:
   * https://jira.acme.com/jira/secure/CreateIssueDetails!init.jspa?pid=10310&issuetype=3&priority=4&description=say+hello+world...<br/>
   * Example: https://jira.acme.com/jira/secure/CreateIssueDetails!init.jspa. <br/>
   * If null then no creation of JIRA issues is supported (e. g. for MEB).
   */
  public String getCreateIssueUrl()
  {
    return createIssueUrl;
  }

  public List<JiraIssueType> getIssueTypes()
  {
    return issueTypes;
  }

  /**
   * @see ConfigXml#toString(Object)
   */
  @Override
  public String toString()
  {
    return ConfigXml.toString(this);
  }
}
