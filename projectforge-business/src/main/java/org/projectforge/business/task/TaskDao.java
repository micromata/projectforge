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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserDao;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class TaskDao extends BaseDao<TaskDO> {
  public static final String I18N_KEY_ERROR_CYCLIC_REFERENCE = "task.error.cyclicReference";
  public static final String I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND = "task.error.parentTaskNotFound";
  public static final String I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN = "task.error.parentTaskNotGiven";
  public static final String I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS = "task.error.duplicateChildTasks";
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskDao.class);
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"responsibleUser.username",
          "responsibleUser.firstname", "responsibleUser.lastname"};
  @Autowired
  private UserDao userDao;

  public TaskDao() {
    super(TaskDO.class);
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * Checks constraint violation.
   */
  @Override
  protected void onSaveOrModify(final TaskDO obj) {
    synchronized (this) {
      checkConstraintVioloation(obj);
    }
  }

  /**
   * @param task
   * @param parentTaskId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public TaskDO setParentTask(final TaskDO task, final Integer parentTaskId) {
    final TaskDO parentTask = getOrLoad(parentTaskId);
    task.setParentTask(parentTask);
    return task;
  }

  /**
   * @param task
   * @param predecessorId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setGanttPredecessor(final TaskDO task, final Integer predecessorId) {
    final TaskDO predecessor = getOrLoad(predecessorId);
    task.setGanttPredecessor(predecessor);
  }

  /**
   * @param task
   * @param responsibleUserId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setResponsibleUser(final TaskDO task, final Integer responsibleUserId) {
    final PFUserDO user = userDao.getOrLoad(responsibleUserId);
    task.setResponsibleUser(user);
  }

  /**
   * Gets the total duration of all time sheets of all tasks (excluding the child tasks).
   */
  public List<Object[]> readTotalDurations() {
    log.debug("Calculating duration for all tasks");
    final String intervalInSeconds = DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime");
    if (intervalInSeconds != null) {
      TypedQuery<Tuple> typedQuery = em.createQuery(
              "select " + intervalInSeconds + ", task.id from TimesheetDO where deleted=false group by task.id",
              Tuple.class);
      List<Tuple> result = typedQuery.getResultList();
      // select intervalInSeconds, task.id from TimesheetDO where deleted=false group by task.id
      final List<Object[]> list = new ArrayList<>();
      for (Tuple tuple : result) {
        list.add(new Object[]{tuple.get(0), tuple.get(1)});
      }
      return list;
    }

    TypedQuery<Tuple> typedQuery = em.createQuery(
            "select startTime, stopTime, task.id from TimesheetDO where deleted=false order by task.id",
            Tuple.class);
    List<Tuple> result = typedQuery.getResultList();
    // select startTime, stopTime, task.id from TimesheetDO where deleted=false order by task.id");
    final List<Object[]> list = new ArrayList<>();
    if (!CollectionUtils.isEmpty(result)) {
      Integer currentTaskId = null;
      long totalDuration = 0;
      for (final Tuple oa : result) {
        final Timestamp startTime = (Timestamp) oa.get(0);
        final Timestamp stopTime = (Timestamp) oa.get(1);
        final Integer taskId = (Integer) oa.get(2);
        final long duration = (stopTime.getTime() - startTime.getTime()) / 1000;
        if (currentTaskId == null || !currentTaskId.equals(taskId)) {
          if (currentTaskId != null) {
            list.add(new Object[]{totalDuration, currentTaskId});
          }
          // New row.
          currentTaskId = taskId;
          totalDuration = 0;
        }
        totalDuration += duration;
      }
      if (currentTaskId != null) {
        list.add(new Object[]{totalDuration, currentTaskId});
      }
    }
    return list;
  }

  /**
   * Gets the total duration of all time sheets of the given task (excluding the child tasks).
   */
  public long readTotalDuration(final Integer taskId) {
    log.debug("Calculating duration for all tasks");
    final String intervalInSeconds = DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime");
    if (intervalInSeconds != null) {
      // Expected type is Integer or Long.
      TypedQuery<Number> typedQuery = em.createQuery(
              "select " + intervalInSeconds + " from TimesheetDO where task.id=:taskId group by task.id",
              Number.class).setParameter("taskId", taskId);
      Number value = SQLHelper.ensureUniqueResult(typedQuery);
      // select DatabaseSupport.getInstance().getIntervalInSeconds("startTime", "stopTime") from TimesheetDO where task.id = :taskId and deleted=false")
      if (value == null) {
        return 0L;
      }
      return value.longValue();
    }
    List<Tuple> result = em.createNamedQuery(TimesheetDO.FIND_START_STOP_BY_TASKID, Tuple.class)
            .setParameter("taskId", taskId)
            .getResultList();
    if (CollectionUtils.isEmpty(result)) {
      return 0L;
    }
    long totalDuration = 0;
    for (final Tuple oa : result) {
      final Timestamp startTime = (Timestamp) oa.get(0);
      final Timestamp stopTime = (Timestamp) oa.get(1);
      final long duration = stopTime.getTime() - startTime.getTime();
      totalDuration += duration;
    }
    return totalDuration / 1000;
  }

  @Override
  public List<TaskDO> getList(final BaseSearchFilter filter) throws AccessException {
    final TaskFilter myFilter;
    if (filter instanceof TaskFilter) {
      myFilter = (TaskFilter) filter;
    } else {
      myFilter = new TaskFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final Collection<TaskStatus> col = new ArrayList<>(4);
    if (myFilter.isNotOpened()) {
      col.add(TaskStatus.N);
    }
    if (myFilter.isOpened()) {
      col.add(TaskStatus.O);
    }
    if (myFilter.isClosed()) {
      col.add(TaskStatus.C);
    }
    if (col.size() > 0) {
      queryFilter.add(QueryFilter.isIn("status", col));
    } else {
      // Note: Result set should be empty, because every task should has one of the following status values.
      queryFilter.add(
              QueryFilter.not(QueryFilter.isIn("status", TaskStatus.N, TaskStatus.O, TaskStatus.C)));
    }
    queryFilter.addOrder(SortProperty.asc("title"));
    if (log.isDebugEnabled()) {
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
  public void checkConstraintVioloation(final TaskDO task) throws UserException {
    if (task.getParentTaskId() == null) {
      // Root task or task without parent task.
      final TaskTree taskTree = getTaskTree(task);
      if (!taskTree.isRootNode(task)) {
        // Task is not root task!
        throw new UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_GIVEN);
      }
    } else {
      List<TaskDO> others;
      if (task.getId() != null) {
        others = em.createNamedQuery(TaskDO.FIND_OTHER_TASK_BY_PARENTTASKID_AND_TITLE, TaskDO.class)
                .setParameter("parentTaskId", task.getParentTaskId())
                .setParameter("title", task.getTitle())
                .setParameter("id", task.getId()) // Find other (different from this id).
                .getResultList();
      } else {
        others = em.createNamedQuery(TaskDO.FIND_BY_PARENTTASKID_AND_TITLE, TaskDO.class)
                .setParameter("parentTaskId", task.getParentTaskId())
                .setParameter("title", task.getTitle())
                .getResultList();
      }
      if (CollectionUtils.isNotEmpty(others)) {
        throw new UserException(I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS);
      }
    }
  }

  @Override
  protected void afterSaveOrModify(final TaskDO obj) {
    final TaskTree taskTree = getTaskTree(obj);
    taskTree.addOrUpdateTaskNode(obj);
  }

  /**
   * Must be visible for TaskTree.
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final TaskDO obj, final boolean throwException) {
    if (accessChecker.isUserMemberOfGroup(user, false, ProjectForgeGroup.ADMIN_GROUP, ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP)) {
      return true;
    }
    return super.hasUserSelectAccess(user, obj, throwException);
  }

  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final TaskDO obj, final TaskDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return accessChecker.hasPermission(user, obj.getId(), AccessType.TASKS, operationType, throwException);
  }

  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final TaskDO obj, final TaskDO dbObj,
                                 final boolean throwException) {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    final TaskTree taskTree = getTaskTree(obj);
    if (taskTree.isRootNode(obj)) {
      if (obj.getParentTaskId() != null) {
        throw new UserException(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE);
      }
      return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.ADMIN_GROUP,
              ProjectForgeGroup.FINANCE_GROUP);
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
            ProjectForgeGroup.FINANCE_GROUP)) {
      return true;
    }
    if (!accessChecker.hasPermission(user, obj.getId(), AccessType.TASKS, OperationType.UPDATE,
            throwException)) {
      return false;
    }
    if (!dbObj.getParentTaskId().equals(obj.getParentTaskId())) {
      // User moves the object to another task:
      if (!hasInsertAccess(user, obj, throwException)) {
        // Inserting of object under new task not allowed.
        return false;
      }
      // Deleting of object under old task not allowed.
      return accessChecker.hasPermission(user, dbObj.getParentTaskId(), AccessType.TASKS, OperationType.DELETE,
              throwException);
    }
    return true;
  }

  public boolean hasAccessForKost2AndTimesheetBookingStatus(final PFUserDO user, final TaskDO obj) {
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    final Integer taskId = obj.getId() != null ? obj.getId() : obj.getParentTaskId();
    final TaskTree taskTree = getTaskTree(obj);
    final ProjektDO projekt = taskTree.getProjekt(taskId);
    // Parent task because id of current task is null and project can't be found.
    return projekt != null && getUserGroupCache().isUserProjectManagerOrAssistantForProject(projekt);
  }

  @Override
  protected void checkInsertAccess(final PFUserDO user, final TaskDO obj) throws AccessException {
    super.checkInsertAccess(user, obj);
    if (!accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
      if (obj.getProtectTimesheetsUntil() != null) {
        throw new AccessException("task.error.protectTimesheetsUntilReadonly");
      }
      if (obj.getProtectionOfPrivacy()) {
        throw new AccessException("task.error.protectionOfPrivacyReadonly");
      }
    }
    if (!hasAccessForKost2AndTimesheetBookingStatus(user, obj)) {
      // Non project managers are not able to manipulate the following fields:
      if (StringUtils.isNotBlank(obj.getKost2BlackWhiteList()) || obj.getKost2IsBlackList()) {
        throw new AccessException("task.error.kost2Readonly");
      }
      if (obj.getTimesheetBookingStatus() != TimesheetBookingStatus.DEFAULT) {
        throw new AccessException("task.error.timesheetBookingStatus2Readonly");
      }
    }
  }

  @Override
  protected void checkUpdateAccess(final PFUserDO user, final TaskDO obj, final TaskDO dbObj) throws AccessException {
    super.checkUpdateAccess(user, obj, dbObj);
    if (!accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
      Long ts1 = null, ts2 = null;
      if (obj.getProtectTimesheetsUntil() != null) {
        ts1 = obj.getProtectTimesheetsUntil().getTime();
      }
      if (dbObj.getProtectTimesheetsUntil() != null) {
        ts2 = dbObj.getProtectTimesheetsUntil().getTime();
      }
      if (!Objects.equals(ts1, ts2)) {
        throw new AccessException("task.error.protectTimesheetsUntilReadonly");
      }
      if (!Objects.equals(obj.getProtectionOfPrivacy(), dbObj.getProtectionOfPrivacy())) {
        throw new AccessException("task.error.protectionOfPrivacyReadonly");
      }
    }
    if (!hasAccessForKost2AndTimesheetBookingStatus(user, obj)) {
      // Non project managers are not able to manipulate the following fields:
      if (!Objects.equals(obj.getKost2BlackWhiteList(), dbObj.getKost2BlackWhiteList())
              || obj.getKost2IsBlackList() != dbObj.getKost2IsBlackList()) {
        throw new AccessException("task.error.kost2Readonly");
      }
      if (obj.getTimesheetBookingStatus() != dbObj.getTimesheetBookingStatus()) {
        throw new AccessException("task.error.timesheetBookingStatus2Readonly");
      }
    }
  }

  @Override
  public boolean hasInsertAccess(final PFUserDO user, final TaskDO obj, final boolean throwException) {
    Validate.notNull(obj);
    // Checks if the task is orphan.
    final TaskTree taskTree = getTaskTree(obj);
    final TaskNode parent = taskTree.getTaskNodeById(obj.getParentTaskId());
    if (parent == null) {
      if (taskTree.isRootNode(obj) && obj.isDeleted()) {
        // Oups, the user has deleted the root task!
      } else {
        throw new UserException(I18N_KEY_ERROR_PARENT_TASK_NOT_FOUND);
      }
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ADMIN_GROUP,
            ProjectForgeGroup.FINANCE_GROUP)) {
      return true;
    }
    return accessChecker.hasPermission(user, obj.getParentTaskId(), AccessType.TASKS, OperationType.INSERT,
            throwException);
  }

  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final TaskDO obj, final TaskDO dbObj,
                                 final boolean throwException) {
    Validate.notNull(obj);
    if (hasUpdateAccess(user, obj, dbObj, throwException)) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ADMIN_GROUP,
            ProjectForgeGroup.FINANCE_GROUP)) {
      return true;
    }
    return accessChecker.hasPermission(user, obj.getParentTaskId(), AccessType.TASKS, OperationType.DELETE,
            throwException);
  }

  @Override
  protected ModificationStatus copyValues(final TaskDO src, final TaskDO dest, final String... ignoreFields) {
    ModificationStatus modified = super.copyValues(src, dest, ignoreFields);
    // Priority value is null-able (may be was not copied from super.copyValues):
    if (!Objects.equals(dest.getPriority(), src.getPriority())) {
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

  private void checkCyclicReference(final TaskDO obj) {
    if (obj.getId().equals(obj.getParentTaskId())) {
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
    if (node.isParentOf(parent)) {
      // Cyclic reference because task is ancestor of itself.
      throw new UserException(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE);
    }
  }

  /**
   * Checks only root task (can't be deleted).
   */
  @Override
  protected void onDelete(final TaskDO obj) {
    final TaskTree taskTree = getTaskTree(obj);
    if (taskTree.isRootNode(obj)) {
      throw new UserException("task.error.couldNotDeleteRootTask");
    }
  }

  public TaskTree getTaskTree() {
    return TaskTreeHelper.getTaskTree();
  }

  public TaskTree getTaskTree(final TaskDO task) {
    final TenantDO tenant = task.getTenant();
    return TaskTreeHelper.getTaskTree(tenant);
  }

  /**
   * Re-index all dependent objects only if the title was changed.
   */
  @Override
  protected boolean wantsReindexAllDependentObjects(final TaskDO obj, final TaskDO dbObj) {
    if (!super.wantsReindexAllDependentObjects(obj, dbObj)) {
      return false;
    }
    return !StringUtils.equals(obj.getTitle(), dbObj.getTitle());
  }

  @Override
  public TaskDO newInstance() {
    return new TaskDO();
  }
}
