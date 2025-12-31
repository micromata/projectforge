/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.task;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hibernate.Hibernate;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.common.BeanHelper;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.WicketUtils;

public class TaskPropertyColumn<T> extends CellItemListenerPropertyColumn<T>
{
  private static final long serialVersionUID = -26352961662061891L;

  /**
   * @param clazz
   * @param sortProperty
   * @param propertyExpression
   * @param cellItemListener
   */
  public TaskPropertyColumn(final Class<?> clazz, final String sortProperty,
      final String propertyExpression,
      final CellItemListener<T> cellItemListener)
  {
    super(clazz, sortProperty, propertyExpression, cellItemListener);
  }

  /**
   * @param label
   * @param sortProperty
   * @param property         Should be from type TaskDO or Long for task id.
   * @param cellItemListener
   */
  public TaskPropertyColumn(final String label, final String sortProperty,
      final String property,
      final CellItemListener<T> cellItemListener)
  {
    super(new Model<String>(label), sortProperty, property, cellItemListener);
  }

  /**
   * @param label
   * @param sortProperty
   * @param property     Should be from type TaskDO or Long for task id.
   */
  public TaskPropertyColumn(final String label, final String sortProperty,
      final String property)
  {
    this(label, sortProperty, property, null);
  }

  @Override
  public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel)
  {
    final TaskDO task = getTask(rowModel);
    if (task == null) {
      item.add(new Label(componentId, ""));
    } else {
      final Label label = new Label(componentId, task.getTitle());
      final String taskPath = WicketTaskFormatter.getTaskPath(task.getId(), false, OutputType.PLAIN);
      WicketUtils.addTooltip(label, taskPath);
      label.setEscapeModelStrings(false);
      item.add(label);
    }
    if (cellItemListener != null) {
      cellItemListener.populateItem(item, componentId, rowModel);
    }
  }

  protected TaskDO getTask(final IModel<T> rowModel)
  {
    final Object obj = BeanHelper.getNestedProperty(rowModel.getObject(), getPropertyExpression());
    TaskDO task = null;
    if (obj != null) {
      if (obj instanceof TaskDO) {
        task = (TaskDO) obj;
        if (Hibernate.isInitialized(task) == false) {
          task = getTaskTree().getTaskById(task.getId());
        }
      } else if (obj instanceof Long) {
        final Long taskId = (Long) obj;
        task = getTaskTree().getTaskById(taskId);
      } else {
        throw new IllegalStateException("Unsupported column type: " + obj);
      }
    }
    return task;
  }

  private TaskTree getTaskTree()
  {
    return TaskTree.getInstance();
  }
}
