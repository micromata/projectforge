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

package org.projectforge.business.gantt;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

@XmlObject(alias = "ganttChartSettings")
public class GanttChartSettings implements Serializable
{
  private static final long serialVersionUID = 314444138347629986L;

  @XmlField(defaultStringValue = "Gantt diagram by ProjectForge®")
  private String title = "Gantt diagram by ProjectForge®";

  private Date fromDate;

  private Date toDate;
  
  private boolean showOnlyVisibles;
  
  private Set<Serializable> openNodes;

  /**
   * Title to show at the upper left corner.
   */
  public String getTitle()
  {
    return title;
  }

  public GanttChartSettings setTitle(String title)
  {
    this.title = title;
    return this;
  }

  /**
   * If not set then the start date of the Gantt diagram will be automatically calculated.
   */
  public Date getFromDate()
  {
    return fromDate;
  }

  public GanttChartSettings setFromDate(Date fromDate)
  {
    this.fromDate = fromDate;
    return this;
  }

  /**
   * If not set then the end date of the Gantt diagram will be automatically calculated.
   */
  public Date getToDate()
  {
    return toDate;
  }

  public GanttChartSettings setToDate(Date toDate)
  {
    this.toDate = toDate;
    return this;
  }
  
  public boolean isShowOnlyVisibles()
  {
    return showOnlyVisibles;
  }
  
  public GanttChartSettings setShowOnlyVisibles(boolean showOnlyVisibles)
  {
    this.showOnlyVisibles = showOnlyVisibles;
    return this;
  }
  
  /**
   * Persist the tree status (which tree nodes are open?).
   * @return
   */
  public Set<Serializable> getOpenNodes()
  {
    return openNodes;
  }
  
  public void setOpenNodes(Set<Serializable> openNodes)
  {
    this.openNodes = openNodes;
  }
}
