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

package org.projectforge.web.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskFilter;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.TaskObject;
import org.projectforge.rest.JsonUtils;
import org.projectforge.web.rest.converter.TaskDOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
@Path(RestPaths.TIMESHEET)
public class TimesheetDaoRest
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimesheetDaoRest.class);

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private TaskDOConverter taskDOConverter;

  /**
   * Rest-Call für: {@link TaskDao#getList(org.projectforge.core.BaseSearchFilter)}
   *
   * @param searchTerm
   */
  @GET
  @Path("list")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList( //
      @QueryParam("search") final String searchTerm, //
      @QueryParam("notopened") final Boolean notOpened, //
      @QueryParam("opened") final Boolean opened, //
      @QueryParam("closed") final Boolean closed, //
      @QueryParam("deleted") final Boolean deleted)
  {
    final List<TaskDO> list = queryList(searchTerm, notOpened, opened, closed, deleted);
    final List<TaskObject> result = new ArrayList<TaskObject>();
    if (list != null) {
      for (final TaskDO task : list) {
        result.add(createRTask(task));
      }
    }
    final String json = JsonUtils.toJson(result);
    return Response.ok(json).build();
  }

  /**
   * Rest-Call für: {@link TaskDao#getList(org.projectforge.core.BaseSearchFilter)}
   *
   * @param searchTerm
   */
  @GET
  @Path("tree")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTree( //
      @QueryParam("search") final String searchTerm, //
      @QueryParam("notopened") final Boolean notOpened, //
      @QueryParam("opened") final Boolean opened, //
      @QueryParam("closed") final Boolean closed, //
      @QueryParam("deleted") final Boolean deleted)
  {
    final List<TaskDO> list = queryList(searchTerm, notOpened, opened, closed, deleted);
    final List<TaskObject> result = convertTasks(list);
    final String json = JsonUtils.toJson(result);
    return Response.ok(json).build();
  }

  private List<TaskDO> queryList(final String searchTerm, final Boolean notOpened, final Boolean opened,
      final Boolean closed,
      final Boolean deleted)
  {
    final TaskFilter filter = new TaskFilter();
    if (closed != null) {
      filter.setClosed(closed.booleanValue());
    }
    if (deleted != null) {
      filter.setDeleted(deleted.booleanValue());
    }
    if (opened != null) {
      filter.setOpened(opened.booleanValue());
    }
    if (notOpened != null) {
      filter.setNotOpened(notOpened.booleanValue());
    }
    filter.setSearchString(searchTerm);
    final List<TaskDO> list = taskDao.getList(filter);
    return list;
  }

  /**
   * Builds task tree.
   *
   * @param tasks
   * @return
   */
  private List<TaskObject> convertTasks(final List<TaskDO> tasks)
  {
    final List<TaskObject> topLevelTasks = new ArrayList<TaskObject>();
    if (tasks == null || tasks.isEmpty() == true) {
      return topLevelTasks;
    }
    final TaskTree taskTree = taskDao.getTaskTree();
    final Map<Integer, TaskObject> rtaskMap = new HashMap<Integer, TaskObject>();
    for (final TaskDO task : tasks) {
      final TaskObject rtask = createRTask(task);
      rtaskMap.put(task.getId(), rtask);
    }
    for (final TaskDO task : tasks) {
      addTask(taskTree, topLevelTasks, task, rtaskMap);
    }
    return topLevelTasks;
  }

  private TaskObject addTask(final TaskTree taskTree, final List<TaskObject> topLevelTasks, final TaskDO task,
      final Map<Integer, TaskObject> rtaskMap)
  {
    TaskObject rtask = rtaskMap.get(task.getId());
    if (rtask == null) {
      // ancestor task not part of the result list, create it:
      if (taskDao.hasSelectAccess(ThreadLocalUserContext.getUser(), task, false) == false) {
        // User has no access, ignore this part of the task tree.
        return null;
      }
      rtask = createRTask(task);
      rtaskMap.put(task.getId(), rtask);
    }
    final TaskDO parent = taskTree.getTaskById(task.getParentTaskId());
    if (parent == null) {
      // this is the root node, ignore it:
      return null;
    }
    if (taskTree.isRootNode(parent) == true) {
      topLevelTasks.add(rtask);
      return rtask;
    }
    TaskObject parentRTask = rtaskMap.get(task.getParentTaskId());
    if (parentRTask == null) {
      // Get and insert parent task first:
      parentRTask = addTask(taskTree, topLevelTasks, parent, rtaskMap);
    }
    if (parentRTask != null) {
      parentRTask.add(rtask);
    }
    return rtask;
  }

  private TaskObject createRTask(final TaskDO taskDO)
  {
    final TaskObject task = taskDOConverter.getTaskObject(taskDO);
    if (taskDO == null) {
      log.error("Oups, task is null.");
      return task;
    }
    final TaskNode taskNode = taskDao.getTaskTree().getTaskNodeById(taskDO.getId());
    if (taskNode == null) {
      log.error("Oups, task node with id '" + taskDO.getId() + "' not found in taskTree.");
      return task;
    }
    task.setBookableForTimesheets(taskNode.isBookableForTimesheets());
    return task;
  }
}
