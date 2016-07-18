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

package org.projectforge.web.gantt;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.projectforge.business.gantt.GanttTask;
import org.projectforge.business.gantt.GanttUtils;
import org.projectforge.common.StringHelper;
import org.projectforge.web.tree.TreeTableNode;

/**
 * Represents a single node as part of the TreeTable.
 */
public class GanttTreeTableNode extends TreeTableNode implements Serializable
{
  private static final long serialVersionUID = -4489818119486920351L;

  private GanttTask ganttObject;

  public GanttTask getGanttObject()
  {
    return ganttObject;
  }

  /**
   * Only for deserialization.
   */
  protected GanttTreeTableNode()
  {
  }

  protected GanttTreeTableNode(GanttTreeTableNode parent, GanttTask ganttObject)
  {
    super(parent, ganttObject.getId());
    this.ganttObject = ganttObject;
  }

  @Override
  public int compareTo(TreeTableNode obj)
  {
    final GanttTreeTableNode other = (GanttTreeTableNode) obj;
    final int result = GanttUtils.GANTT_OBJECT_COMPARATOR.compare(this.ganttObject, other.ganttObject);
    if (result != 0) {
      return result;
    }
    return StringHelper.compareTo(this.ganttObject.getTitle(), other.ganttObject.getTitle());
  }

  @Override
  public String toString()
  {
    final ToStringBuilder tos = new ToStringBuilder(this);
    tos.append("id", ganttObject.getId());
    tos.append("title", ganttObject.getTitle());
    if (getChilds() != null) {
      tos.append("children", getChilds());
    }
    return tos.toString();
  }
}
