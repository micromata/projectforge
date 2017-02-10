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

package org.projectforge.business.task;

import java.util.List;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.persistence.database.PfJpaXmlDumpServiceImpl;

/**
 * TaskPathBridge for hibernate search to search in the parent task titles.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class HibernateSearchTaskPathBridge implements FieldBridge
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(HibernateSearchTaskPathBridge.class);

  /**
   * Get all names of ancestor tasks and task itself and creates an index containing all task titles separated by '|'.
   * <br/>
   * Please note: does not work in JUnit test mode.
   *
   * @see org.hibernate.search.bridge.FieldBridge#set(java.lang.String, java.lang.Object,
   * org.apache.lucene.document.Document, org.hibernate.search.bridge.LuceneOptions)
   */
  @Override
  public void set(final String name, final Object value, final Document document,
      final LuceneOptions luceneOptions)
  {
    // DESIGN bug, low level index should not rely on other.
    // did a workoround.
    if (PfJpaXmlDumpServiceImpl.isTransaction == true) {
      log.warn("PfJpaXmlDumpServiceImpl.isTransaction = true");
      return;
    }

    final TaskDO task = (TaskDO) value;
    final TaskTree taskTree = TaskTreeHelper.getTaskTree(task.getTenant());
    final TaskNode taskNode = taskTree.getTaskNodeById(task.getId());
    if (taskNode == null) {
      return;
    }
    final List<TaskNode> list = taskNode.getPathToRoot();
    final StringBuffer buf = new StringBuffer();
    list.forEach(node -> {
      buf.append(node.getTask().getTitle()).append("|");
    });
    if (log.isDebugEnabled() == true) {
      log.debug(buf.toString());
    }
    luceneOptions.addFieldToDocument(name, buf.toString(), document);
  }
}
