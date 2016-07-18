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

import org.projectforge.business.gantt.GanttTask;
import org.projectforge.web.tree.TreeTable;

/**
 * The implementation of TreeTable for tasks. Used for browsing the tasks (tree view).
 */
public class GanttTreeTable extends TreeTable<GanttTreeTableNode>
{
  private static final long serialVersionUID = -5169823286484221430L;

  public GanttTreeTable(final GanttTask rootNode)
  {
    root = new GanttTreeTableNode(null, rootNode);
    addDescendantNodes(root);
    updateOpenStatus();
  }

  protected void addDescendantNodes(GanttTreeTableNode parent)
  {
    final GanttTask parentObj = parent.getGanttObject();
    if (parentObj.getChildren() != null) {
      for (final GanttTask childNode : parentObj.getChildren()) {
        final GanttTreeTableNode child = new GanttTreeTableNode(parent, childNode);
        addTreeTableNode(child);
        addDescendantNodes(child);
      }
    }
  }
}
