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

package org.projectforge.web.rest.converter;

import org.hibernate.Hibernate;
import org.projectforge.business.converter.DOConverter;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.model.rest.TaskObject;
import org.springframework.stereotype.Service;

/**
 * For conversion of TaskDO to task object.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class TaskDOConverter
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskDOConverter.class);

  public TaskObject getTaskObject(TaskDO taskDO)
  {
    if (taskDO == null) {
      return null;
    }
    if (Hibernate.isInitialized(taskDO) == false) {
      final Integer taskId = taskDO.getId();
      taskDO = TaskTreeHelper.getTaskTree().getTaskById(taskId);
      if (taskDO == null) {
        log.error("Oups, task with id '" + taskId + "' not found.");
        return null;
      }
    }
    final TaskObject task = new TaskObject();
    DOConverter.copyFields(task, taskDO);
    task.setParentTaskId(taskDO.getParentTaskId());
    task.setDescription(taskDO.getDescription());
    task.setReference(taskDO.getReference());
    task.setTitle(taskDO.getTitle());
    task.setShortDescription(taskDO.getShortDescription());
    task.setMaxHours(taskDO.getMaxHours());
    task.setPriority(taskDO.getPriority());
    task.setStatus(taskDO.getStatus());
    return task;
  }
}
