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

import java.io.Serializable;

import org.projectforge.framework.utils.ILabelValueBean;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * Represents one available issue type of the JIRA system ProjectForge is connected to.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XmlObject(alias = "issueType")
public class JiraIssueType implements Serializable, ILabelValueBean<String, Integer>
{
  private static final long serialVersionUID = -1829919749775403828L;

  @XmlField(asAttribute = true)
  private String label;

  private Integer value;

  /**
   * Label to show.
   */
  public String getLabel()
  {
    return label;
  }

  public void setLabel(String label)
  {
    this.label = label;
  }

  /**
   * Issue type supported by JIRA.
   */
  public Integer getValue()
  {
    return value;
  }

  public void setValue(Integer value)
  {
    this.value = value;
  }
}
