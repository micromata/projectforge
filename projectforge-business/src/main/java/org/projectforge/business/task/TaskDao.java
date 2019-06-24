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

package org.projectforge.business.task;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserDao;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class TaskDao extends BaseDao<TaskDO>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "responsibleUser.username",
      "responsibleUser.firstname",
      "responsibleUser.lastname", "taskpath", "projekt.name", "projekt.kunde.name", "kost2.nummer",
      "kost2.description" };

  public static final String I18N_KEY_ERROR_CYCLIC_REFERENCE = "task.error.cyclicReference";

  public static final String I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND = "task.error.parentTaskNotFound";

  public static final String I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN = "task.error.parentTaskNotGiven";

  public static final String I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS = "task.error.duplicateChildTasks";

  @Autowired
  private UserDao userDao;

  public TaskDao()
  {
    super(TaskDO.class);
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * Checks constraint violation.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final TaskDO obj)
  {
    synchronized (this) {
      checkConstraintVioloation(obj);
    }
  }

  /**
   * @param task
   * @param parentTaskId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public TaskDO setParentTask(final TaskDO task, final Integer parentTaskId)
  {
    final TaskDO parentTask = getOrLoad(parentTaskId);
    task.setParentTask(parentTask);
    return task;
  }

  /**
   * @param task
   * @param predecessorId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setGanttPredecessor(final TaskDO task, final Integer predecessorId)
  {
    final TaskDO predecessor = getOrLoad(predecessorId);
    task.setGanttPredecessor(predecessor);
  }

  /**
   * @param task
   * @param responsibleUserId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setResponsibleUser(final TaskDO task, final Integer responsibleUserId)
  {
    final PFUserDO user = userDao.getOrLoad(responsibleUserId);
    task.setResponsibleUser(user);
  }

  /**
   * Gets the total duration of all time sheets of all tasks (excluding the child tasks).
   *
   * @param node
   * @return
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<Object[]> readTotalDurations()
  {
    log.debug("Calculating duration for all tasks");
    final String intervalInSeconds = DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime");
    if (intervalInSeconds != null) {
      @SuppressWarnings("unchecked")
      final List<Object[]> list = (List<Object[]>) getHibernateTemplate().find(
          "select " + intervalInSeconds + ", task.id from TimesheetDO where deleted=false group by task.id");
      return list;
    }
    @SuppressWarnings("unchecked")
    final List<Object[]> result = (List<Object[]>) getHibernateTemplate().find(
        "select startTime, stopTime, task.id from TimesheetDO where deleted=false order by task.id");
    final List<Object[]> list = new ArrayList<Object[]>();
    if (CollectionUtils.isEmpty(result) == false) {
      Integer currentTaskId = null;
      long totalDuration = 0;
      for (final Object[] oa : result) {
        final Timestamp startTime = (Timestamp) oa[0];
        final Timestamp stopTime = (Timestamp) oa[1];
        final Integer taskId = (Integer) oa[2];
        final long duration = (stopTime.getTime() - startTime.getTime()) / 1000;
        if (currentTaskId == null || currentTaskId.equals(taskId) == false) {
          if (currentTaskId != null) {
            list.add(new Object[] { totalDuration, currentTaskId });
          }
          // New row.
          currentTaskId = taskId;
          totalDuration = 0;
        }
        totalDuration += duration;
      }
      if (currentTaskId != null) {
        list.add(new Object[] { totalDuration, currentTaskId });
      }
    }
    return list;
  }

  /**
   * Gets the total duration of all time sheets of the given task (excluding the child tasks).
   *
   * @param node
   * @return
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public long readTotalDuration(final Integer taskId)
  {
    log.debug("Calculating duration for all tasks");
    final String intervalInSeconds = DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime");
    if (intervalInSeconds != null) {
      @SuppressWarnings("unchecked")
      final List<Object> list = (List<Object>) getHibernateTemplate().find(
          "select "
              + DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime")
              + " from TimesheetDO where task.id = ? and deleted=false",
          taskId);
      if (list.size() == 0) {
        return new Long(0);
      }
      Validate.isTrue(list.size() == 1);
      if (list.get(0) == null) { // Has happened one time, why (PROJECTFORGE-543)?
        return new Long(0);
      } else if (list.get(0) instanceof Integer) {
        return new Long((Integer) list.get(0));
      } else {
        return (Long) list.get(0);
      }
    }
    @SuppressWarnings("unchecked")
    final List<Object[]> result = (List<Object[]>) getHibernateTemplate().find(
        "select startTime, stopTime from TimesheetDO where task.id = ? and deleted=false", taskId);
    if (CollectionUtils.isEmpty(result) == true) {
      return new Long(0);
    }
    long totalDuration = 0;
    for (final Object[] oa : result) {
      final Timestamp startTime = (Timestamp) oa[0];
      final Timestamp stopTime = (Timestamp) oa[1];
      final long duration = stopTime.getTime() - startTime.getTime();
      totalDuration += duration;
    }
    return totalDuration / 1000;
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<TaskDO> getList(final BaseSearchFilter filter) throws AccessException
  {
    final TaskFilter myFilter;
    if (filter instanceof TaskFilter) {
      myFilter = (TaskFilter) filter;
    } else {
      myFilter = new TaskFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final Collection<TaskStatus> col = new ArrayList<TaskStatus>(4);
    if (myFilter.isNotOpened() == true) {
      col.add(TaskStatus.N);
    }
    if (myFilter.isOpened() == true) {
      col.add(TaskStatus.O);
    }
    if (myFilter.isClosed() == true) {
      col.add(TaskStatus.C);
    }
    if (col.size() > 0) {
      queryFilter.add(Restrictions.in("status", col));
    } else {
      // Note: Result set should be empty, because every task should has one of the following status values.
      queryFilter.add(
          Restrictions.not(Restrictions.in("status", new TaskStatus[] { TaskStatus.N, TaskStatus.O, TaskStatus.C })));
    }
    queryFilter.addOrder(Order.asc("title"));
    if (log.isDebugEnabled() == true) {
      log.debug(myFilter.toString());
    }
    return getList(queryFilter);
  }

  /**
   * Checks if the given task has already a sister task with the same title.
   *
   * @param task
   * @throws UserException
   */
  @SuppressWarnings("unchecked")
  public void checkConstraintVioloation(final TaskDO task) throws UserException
  {
    if (task.getParentTaskId() == null) {
      // Root task or task without parent task.
      final TaskTree taskTree = getTaskTree(task);
      if (taskTree.isRootNode(task) == false) {
        // Task is not root task!
        throw new UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN);
      }
    } else {
      List<TaskDO> list;
      if (task.getId() != null) {
        list = (List<TaskDO>) getHibernateTemplate().find(
            "from TaskDO t where t.parentTask.id = ? and t.title = ? and t.id != ?",
            new Object[] { task.getParentTaskId(), task.getTitle(), task.getId() });
      } else {
        list = (List<TaskDO>) getHibernateTemplate().find("from TaskDO t where t.parentTask.id = ? and t.title = ?",
            new Object[] { task.getParentTaskId(), task.getTitle() });
      }
      if (CollectionUtils.isNotEmpty(list) == true) {
        throw new UserException(I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS);
      }
    }
  }

  @Override
  protected void afterSaveOrModify(final TaskDO obj)
  {
    // Reread it from the database to get the current version (given obj could be different, for example after markAsDeleted):
    final TaskDO task = internalGetById(obj.getId());
    final TaskTree taskTree = getTaskTree(task);
    taskTree.addOrUpdateTaskNode(task);
  }

  /**
   * Must be visible for TaskTree.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(java.lang.Object, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final TaskDO obj, final boolean throwException)
  {
    if (accessChecker.isUserMemberOfGroup(user, false, ProjectForgeGroup.ADMIN_GROUP, ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP) == true) {
      return true;
    }
    return super.hasSelectAccess(user, obj, throwException);
  }

  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final TaskDO obj, final TaskDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.hasPermission(user, obj.getId(), AccessType.TASKS, operationType, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUpdateAccess(java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final TaskDO obj, final TaskDO dbObj,
      final boolean throwException)
  {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    final TaskTree taskTree = getTaskTree(obj);
    if (taskTree.isRootNode(obj) == true) {
      if (obj.getParentTaskId() != null) {
        throw new UserException(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE);
      }
      if (accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.ADMIN_GROUP,
          ProjectForgeGroup.FINANCE_GROUP) == false) {
        return false;
      }
      return true;
    }
    Validate.notNull(dbObj.getParentTaskId());
    if (obj.getParentTaskId() == null) {
      throw new UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN);
    }
    final TaskNode parent = taskTree.getTaskNodeById(obj.getParentTaskId());
    if (parent == null) {
      throw new UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND);
    }
    // Checks cyclic and self reference. The parent task is not allowed to be a self reference.
    checkCyclicReference(obj);
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ADMIN_GROUP,
        ProjectForgeGroup.FINANCE_GROUP) == true) {
      return true;
    }
    if (accessChecker.hasPermission(user, obj.getId(), AccessType.TASKS, OperationType.UPDATE,
        throwException) == false) {
      return false;
    }
    if (dbObj.getParentTaskId().equals(obj.getParentTaskId()) == false) {
      // User moves the object to another task:
      if (hasInsertAccess(user, obj, throwException) == false) {
        // Inserting of object under new task not allowed.
        return false;
      }
      if (accessChecker.hasPermission(user, dbObj.getParentTaskId(), AccessType.TASKS, OperationType.DELETE,
          throwException) == false) {
        // Deleting of object under old task not allowed.
        return false;
      }
    }
    return true;
  }

  public boolean hasAccessForKost2AndTimesheetBookingStatus(final PFUserDO user, final TaskDO obj)
  {
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP) == true) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    final Integer taskId = obj.getId() != null ? obj.getId() : obj.getParentTaskId();
    final TaskTree taskTree = getTaskTree(obj);
    final ProjektDO projekt = taskTree.getProjekt(taskId);
    // Parent task because id of current task is null and project can't be found.
    if (projekt != null && getUserGroupCache().isUserProjectManagerOrAssistantForProject(projekt) == true) {
      return true;
    }
    return false;
  }

  @Override
  protected void checkInsertAccess(final PFUserDO user, final TaskDO obj) throws AccessException
  {
    super.checkInsertAccess(user, obj);
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP) == false) {
      if (obj.getProtectTimesheetsUntil() != null) {
        throw new AccessException("task.error.protectTimesheetsUntilReadonly");
      }
      if (obj.getProtectionOfPrivacy() == true) {
        throw new AccessException("task.error.protectionOfPrivacyReadonly");
      }
    }
    if (hasAccessForKost2AndTimesheetBookingStatus(user, obj) == false) {
      // Non project managers are not able to manipulate the following fields:
      if (StringUtils.isNotBlank(obj.getKost2BlackWhiteList()) == true || obj.getKost2IsBlackList() == true) {
        throw new AccessException("task.error.kost2Readonly");
      }
      if (obj.getTimesheetBookingStatus() != TimesheetBookingStatus.DEFAULT) {
        throw new AccessException("task.error.timesheetBookingStatus2Readonly");
      }
    }
  }

  @Override
  protected void checkUpdateAccess(final PFUserDO user, final TaskDO obj, final TaskDO dbObj) throws AccessException
  {
    super.checkUpdateAccess(user, obj, dbObj);
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP) == false) {
      Long ts1 = null, ts2 = null;
      if (obj.getProtectTimesheetsUntil() != null) {
        ts1 = obj.getProtectTimesheetsUntil().getTime();
      }
      if (dbObj.getProtectTimesheetsUntil() != null) {
        ts2 = dbObj.getProtectTimesheetsUntil().getTime();
      }
      if (Objects.equals(ts1, ts2) == false) {
        throw new AccessException("task.error.protectTimesheetsUntilReadonly");
      }
      if (Objects.equals(obj.getProtectionOfPrivacy(), dbObj.getProtectionOfPrivacy()) == false) {
        throw new AccessException("task.error.protectionOfPrivacyReadonly");
      }
    }
    if (hasAccessForKost2AndTimesheetBookingStatus(user, obj) == false) {
      // Non project managers are not able to manipulate the following fields:
      if (Objects.equals(obj.getKost2BlackWhiteList(), dbObj.getKost2BlackWhiteList()) == false
          || obj.getKost2IsBlackList() != dbObj.getKost2IsBlackList()) {
        throw new AccessException("task.error.kost2Readonly");
      }
      if (obj.getTimesheetBookingStatus() != dbObj.getTimesheetBookingStatus()) {
        throw new AccessException("task.error.timesheetBookingStatus2Readonly");
      }
    }
  }

  @Override
  public boolean hasInsertAccess(final PFUserDO user, final TaskDO obj, final boolean throwException)
  {
    Validate.notNull(obj);
    // Checks if the task is orphan.
    final TaskTree taskTree = getTaskTree(obj);
    final TaskNode parent = taskTree.getTaskNodeById(obj.getParentTaskId());
    if (parent == null) {
      if (taskTree.isRootNode(obj) == true && obj.isDeleted() == true) {
        // Oups, the user has deleted the root task!
      } else {
        throw new UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND);
      }
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ADMIN_GROUP,
        ProjectForgeGroup.FINANCE_GROUP) == true) {
      return true;
    }
    return accessChecker.hasPermission(user, obj.getParentTaskId(), AccessType.TASKS, OperationType.INSERT,
        throwException);
  }

  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final TaskDO obj, final TaskDO dbObj,
      final boolean throwException)
  {
    Validate.notNull(obj);
    if (hasUpdateAccess(user, obj, dbObj, throwException) == true) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ADMIN_GROUP,
        ProjectForgeGroup.FINANCE_GROUP) == true) {
      return true;
    }
    return accessChecker.hasPermission(user, obj.getParentTaskId(), AccessType.TASKS, OperationType.DELETE,
        throwException);
  }

  @Override
  protected ModificationStatus copyValues(final TaskDO src, final TaskDO dest, final String... ignoreFields)
  {
    ModificationStatus modified = super.copyValues(src, dest, ignoreFields);
    // Priority value is null-able (may be was not copied from super.copyValues):
    if (Objects.equals(dest.getPriority(), src.getPriority()) == false) {
      dest.setPriority(src.getPriority());
      modified = ModificationStatus.MAJOR;
    }
    // User object is null-able:
    if (src.getResponsibleUser() == null) {
      if (dest.getResponsibleUser() != null) {
        dest.setResponsibleUser(src.getResponsibleUser());
        modified = ModificationStatus.MAJOR;
      }
    }
    return modified;
  }

  private void checkCyclicReference(final TaskDO obj)
  {
    if (obj.getId().equals(obj.getParentTaskId()) == true) {
      // Self reference
      throw new UserException(I18N_KEY_ERROR_CYCLIC_REFERENCE);
    }
    final TaskTree taskTree = getTaskTree(obj);
    final TaskNode parent = taskTree.getTaskNodeById(obj.getParentTaskId());
    if (parent == null) {
      // Task is orphan because it has no parent task.
      throw new UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND);
    }
    final TaskNode node = taskTree.getTaskNodeById(obj.getId());
    if (node.isParentOf(parent) == true) {
      // Cyclic reference because task is ancestor of itself.
      throw new UserException(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE);
    }
  }

  /**
   * Checks only root task (can't be deleted).
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onDelete(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onDelete(final TaskDO obj)
  {
    final TaskTree taskTree = getTaskTree(obj);
    if (taskTree.isRootNode(obj) == true) {
      throw new UserException("task.error.couldNotDeleteRootTask");
    }
  }

  public TaskTree getTaskTree()
  {
    return TaskTreeHelper.getTaskTree();
  }

  public TaskTree getTaskTree(final TaskDO task)
  {
    final TenantDO tenant = task.getTenant();
    return TaskTreeHelper.getTaskTree(tenant);
  }

  /**
   * Re-index all dependent objects only if the title was changed.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#wantsReindexAllDependentObjects(org.projectforge.core.ExtendedBaseDO,
   * org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected boolean wantsReindexAllDependentObjects(final TaskDO obj, final TaskDO dbObj)
  {
    if (super.wantsReindexAllDependentObjects(obj, dbObj) == false) {
      return false;
    }
    return StringUtils.equals(obj.getTitle(), dbObj.getTitle()) == false;
  }

  @Override
  public TaskDO newInstance()
  {
    return new TaskDO();
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }
}
