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

package org.projectforge.business.timesheet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.Hibernate;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserDao;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.i18n.MessageParam;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.utils.NumberHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class TimesheetDao extends BaseDao<TimesheetDO> {
  /**
   * Maximum allowed duration of time sheets is 14 hours.
   */
  public static final long MAXIMUM_DURATION = 1000 * 3600 * 14;
  public static final String HIDDEN_FIELD_MARKER = "[...]";
  /**
   * Internal error message if maximum duration is exceeded.
   */
  private static final String MAXIMUM_DURATION_EXCEEDED = "Maximum duration of time sheet exceeded. Maximum is "
          + (MAXIMUM_DURATION / 3600 / 1000)
          + "h!";
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"user.id", "user.username", "user.firstname",
          "user.lastname", "kost2.nummer", "kost2.description", "kost2.projekt.name"};
  private static final Logger log = LoggerFactory.getLogger(TimesheetDao.class);
  @Autowired
  private UserDao userDao;
  @Autowired
  private Kost2Dao kost2Dao;

  public TimesheetDao() {
    super(TimesheetDO.class);
  }

  public boolean showTimesheetsOfOtherUsers() {
    return accessChecker.isLoggedInUserMemberOfGroup(
            ProjectForgeGroup.CONTROLLING_GROUP,
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.HR_GROUP,
            ProjectForgeGroup.ORGA_TEAM,
            ProjectForgeGroup.PROJECT_MANAGER,
            ProjectForgeGroup.PROJECT_ASSISTANT);
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * List of all years with time sheets of the given user: select min(startTime), max(startTime) from t_timesheet where
   * user=?.
   */
  public int[] getYears(final Integer userId) {
    final Object[] minMaxDate = SQLHelper.ensureUniqueResult(em.createNamedQuery(TimesheetDO.SELECT_MIN_MAX_DATE_FOR_USER, Object[].class)
            .setParameter("userId", userId));
    return SQLHelper.getYears((java.util.Date) minMaxDate[0], (java.util.Date) minMaxDate[1]);
  }

  /**
   * @param userId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setUser(final TimesheetDO sheet, final Integer userId) {
    final PFUserDO user = userDao.getOrLoad(userId);
    sheet.setUser(user);
  }

  /**
   * @param taskId If null, then task will be set to null;
   * @see TaskTree#getTaskById(Integer)
   */
  public void setTask(final TimesheetDO sheet, final Integer taskId) {
    final TaskDO task = TaskTreeHelper.getTaskTree(sheet).getTaskById(taskId);
    sheet.setTask(task);
  }

  /**
   * @param kost2Id If null, then kost2 will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKost2(final TimesheetDO sheet, final Integer kost2Id) {
    final Kost2DO kost2 = kost2Dao.getOrLoad(kost2Id);
    sheet.setKost2(kost2);
  }

  /**
   * Gets the available Kost2DO's for the given time sheet. The task must already be assigned to this time sheet.
   *
   * @return Available list of Kost2DO's or null, if not exist.
   */
  public List<Kost2DO> getKost2List(final TimesheetDO timesheet) {
    if (timesheet == null || timesheet.getTaskId() == null) {
      return null;
    }
    return TaskTreeHelper.getTaskTree(timesheet).getKost2List(timesheet.getTaskId());
  }

  public QueryFilter buildQueryFilter(final TimesheetFilter filter) {
    final QueryFilter queryFilter = new QueryFilter(filter);
    if (filter.getUserId() != null) {
      queryFilter.add(QueryFilter.eq("user.id", filter.getUserId()));
    }
    if (filter.getStartTime() != null && filter.getStopTime() != null) {
      queryFilter.add(QueryFilter.and(QueryFilter.ge("stopTime", filter.getStartTime()),
              QueryFilter.le("startTime", filter.getStopTime())));
    } else if (filter.getStartTime() != null) {
      queryFilter.add(QueryFilter.ge("startTime", filter.getStartTime()));
    } else if (filter.getStopTime() != null) {
      queryFilter.add(QueryFilter.le("startTime", filter.getStopTime()));
    }
    if (filter.getTaskId() != null) {
      if (filter.isRecursive()) {
        final TaskNode node = TaskTreeHelper.getTaskTree().getTaskNodeById(filter.getTaskId());
        final List<Integer> taskIds = node.getDescendantIds();
        taskIds.add(node.getId());
        queryFilter.add(QueryFilter.isIn("task.id", taskIds));
        if (log.isDebugEnabled()) {
          log.debug("search in tasks: " + taskIds);
        }
      } else {
        queryFilter.add(QueryFilter.eq("task.id", filter.getTaskId()));
      }
    }
    if (filter.getOrderType() == OrderDirection.DESC) {
      queryFilter.addOrder(SortProperty.desc("startTime"));
    } else {
      queryFilter.addOrder(SortProperty.asc("startTime"));
    }
    if (log.isDebugEnabled()) {
      log.debug(ToStringBuilder.reflectionToString(filter));
    }
    return queryFilter;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getListForSearchDao(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  public List<TimesheetDO> getListForSearchDao(final BaseSearchFilter filter) {
    final TimesheetFilter timesheetFilter = new TimesheetFilter(filter);
    if (filter.getModifiedByUserId() == null) {
      timesheetFilter.setUserId(ThreadLocalUserContext.getUserId());
    }
    return getList(timesheetFilter);
  }

  /**
   * Gets the list filtered by the given filter.
   */
  @Override
  public List<TimesheetDO> getList(final BaseSearchFilter filter) throws AccessException {
    return internalGetList(filter, true);
  }

  public List<TimesheetDO> internalGetList(final BaseSearchFilter filter, boolean checkAccess) {
    final TimesheetFilter myFilter;
    if (filter instanceof TimesheetFilter) {
      myFilter = (TimesheetFilter) filter;
    } else {
      myFilter = new TimesheetFilter(filter);
    }
    if (myFilter.getStopTime() != null) {
      final DateHolder date = new DateHolder(myFilter.getStopTime());
      date.setEndOfDay();
      myFilter.setStopTime(date.getDate());
    }
    final QueryFilter queryFilter = buildQueryFilter(myFilter);
    List<TimesheetDO> result;
    if (checkAccess) {
      result = getList(queryFilter);
    } else {
      result = internalGetList(queryFilter);
    }
    if (result == null) {
      return null;
    }
    if (myFilter.isOnlyBillable()) {
      final List<TimesheetDO> list = result;
      result = new ArrayList<>();
      for (final TimesheetDO entry : list) {
        if (entry.getKost2() != null && entry.getKost2().getKost2Art() != null && entry.getKost2().getKost2Art().getFakturiert()) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  /**
   * Rechecks the time sheet overlaps.
   */
  @Override
  protected void afterSaveOrModify(final TimesheetDO obj) {
    super.afterSaveOrModify(obj);
    TaskTreeHelper.getTaskTree(obj).resetTotalDuration(obj.getTaskId());
  }

  /**
   * Checks the start and stop time. If seconds or millis is not null, a RuntimeException will be thrown.
   */
  @Override
  protected void onSaveOrModify(final TimesheetDO obj) {
    validateTimestamp(obj.getStartTime(), "startTime");
    validateTimestamp(obj.getStopTime(), "stopTime");
    if (obj.getDuration() < 60000) {
      throw new UserException("timesheet.error.zeroDuration"); // "Duration of time sheet must be at minimum 60s!
    }
    if (obj.getDuration() > MAXIMUM_DURATION) {
      throw new UserException("timesheet.error.maximumDurationExceeded");
    }
    Validate.isTrue(obj.getStartTime().before(obj.getStopTime()), "Stop time of time sheet is before start time!");
    if (Configuration.getInstance().isCostConfigured()) {
      final List<Kost2DO> kost2List = TaskTreeHelper.getTaskTree(obj).getKost2List(obj.getTaskId());
      final Integer kost2Id = obj.getKost2Id();
      if (kost2Id == null) {
        // Check, if there is any cost definition in any descendant task:
        TaskTree taskTree = TaskTreeHelper.getTaskTree(obj);
        TaskNode taskNode = taskTree.getTaskNodeById(obj.getTaskId());
        if (taskNode != null) {
          List<Integer> descendents = taskNode.getDescendantIds();
          for (Integer taskId : descendents) {
            if (CollectionUtils.isNotEmpty(taskTree.getKost2List(taskId))) {
              // But Kost2 is available for sub task, so user should book his time sheet
              // on a sub task with kost2s.
              throw new UserException("timesheet.error.kost2NeededChooseSubTask");
            }
          }
        }
      }
      if (CollectionUtils.isNotEmpty(kost2List)) {
        if (kost2Id == null) {
          throw new UserException("timesheet.error.kost2Required");
        }
        boolean kost2IdFound = false;
        for (final Kost2DO kost2 : kost2List) {
          if (NumberHelper.isEqual(kost2Id, kost2.getId())) {
            kost2IdFound = true;
            break;
          }
        }
        if (!kost2IdFound) {
          throw new UserException("timesheet.error.invalidKost2"); // Kost2Id of time sheet is not available in the task's kost2 list!
        }
      } else {
        if (kost2Id != null) {
          throw new UserException("timesheet.error.invalidKost2"); // Kost2Id can't be given for task without any kost2 entries!
        }
      }
    }
  }

  @Override
  protected void onChange(final TimesheetDO obj, final TimesheetDO dbObj) {
    if (obj.getTaskId().compareTo(dbObj.getTaskId()) != 0) {
      TaskTreeHelper.getTaskTree(obj).resetTotalDuration(dbObj.getTaskId());
    }
  }

  @Override
  protected void prepareHibernateSearch(final TimesheetDO obj, final OperationType operationType) {
    final PFUserDO user = obj.getUser();
    if (user != null && !Hibernate.isInitialized(user)) {
      obj.setUser(getUserGroupCache().getUser(user.getId()));
    }
    final TaskDO task = obj.getTask();
    if (task != null && !Hibernate.isInitialized(task)) {
      obj.setTask(TaskTreeHelper.getTaskTree(obj).getTaskById(task.getId()));
    }
  }

  private void validateTimestamp(final Date date, final String name) {
    if (date == null) {
      return;
    }
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    Validate.isTrue(cal.get(Calendar.MILLISECOND) == 0, "Millis of " + name + " is not 0!");
    Validate.isTrue(cal.get(Calendar.SECOND) == 0, "Seconds of " + name + " is not 0!");
    final int m = cal.get(Calendar.MINUTE);
    Validate.isTrue(m == 0 || m == 15 || m == 30 || m == 45, "Minutes of " + name + " must be 0, 15, 30 or 45");
  }

  /**
   * Checks if the time sheet overlaps with another time sheet of the same user. Should be checked on every insert or
   * update (also undelete). For time collision detection deleted time sheets are ignored.
   *
   * @return The existing time sheet with the time period collision.
   */
  public boolean hasTimeOverlap(final TimesheetDO timesheet, final boolean throwException) {
    long begin = System.currentTimeMillis();
    Validate.notNull(timesheet);
    Validate.notNull(timesheet.getUser());
    final QueryFilter queryFilter = new QueryFilter();
    queryFilter.add(QueryFilter.eq("user", timesheet.getUser()));
    queryFilter.add(QueryFilter.eq("deleted", false));
    queryFilter.add(QueryFilter.lt("startTime", timesheet.getStopTime()));
    queryFilter.add(QueryFilter.gt("stopTime", timesheet.getStartTime()));
    if (timesheet.getId() != null) {
      // Update time sheet, do not compare with itself.
      queryFilter.add(QueryFilter.ne("id", timesheet.getId()));
    }
    final List<TimesheetDO> list = getList(queryFilter);
    if (list != null && list.size() > 0) {
      final TimesheetDO ts = list.get(0);
      if (throwException) {
        log.info("Time sheet collision detected of time sheet " + timesheet + " with existing time sheet " + ts);
        final String startTime = DateHelper.formatIsoTimestamp(ts.getStartTime());
        final String stopTime = DateHelper.formatIsoTimestamp(ts.getStopTime());
        throw new UserException("timesheet.error.timeperiodOverlapDetection", new MessageParam(ts.getId()),
                new MessageParam(startTime),
                new MessageParam(stopTime));
      }
      long end = System.currentTimeMillis();
      log.info("TimesheetDao.hasTimeOverlap took: " + (end - begin) + " ms.");
      return true;
    }
    long end = System.currentTimeMillis();
    log.info("TimesheetDao.hasTimeOverlap took: " + (end - begin) + " ms.");
    return false;
  }

  /**
   * return Always true, no generic select access needed for address objects.
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final TimesheetDO obj, final TimesheetDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    if (accessChecker.userEquals(user, obj.getUser())) {
      // Own time sheet
      if (!accessChecker.hasPermission(user, obj.getTaskId(), AccessType.OWN_TIMESHEETS, operationType,
              throwException)) {
        return false;
      }
    } else {
      // Foreign time sheet
      if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
        return true;
      }
      if (!accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TIMESHEETS, operationType,
              throwException)) {
        return false;
      }
    }
    if (operationType == OperationType.DELETE) {
      // UPDATE and INSERT is already checked, SELECT will be ignored.
      final boolean result = checkTimesheetProtection(user, obj, null, operationType, throwException);
      return result;
    }
    return true;
  }

  /**
   * User can always see his own time sheets. But if he has no access then the location and description values are
   * hidden (empty strings).
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final TimesheetDO obj, final boolean throwException) {
    if (!hasAccess(user, obj, null, OperationType.SELECT, false)) {
      // User has no access by definition.
      if (accessChecker.userEquals(user, obj.getUser())
              || accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.PROJECT_MANAGER)) {
        if (!accessChecker.userEquals(user, obj.getUser())) {
          // Check protection of privacy for foreign time sheets:
          final List<TaskNode> pathToRoot = TaskTreeHelper.getTaskTree(obj).getPathToRoot(obj.getTaskId());
          for (final TaskNode node : pathToRoot) {
            if (node.getTask().getProtectionOfPrivacy()) {
              return false;
            }
          }
        }
        // An user should see his own time sheets, but the values should be hidden.
        // A project manager should also see all time sheets, but the values should be hidden.
        em.detach(obj);
        obj.setDescription(HIDDEN_FIELD_MARKER);
        obj.setLocation(HIDDEN_FIELD_MARKER);
        log.debug("User has no access to own time sheet (or project manager): " + obj);
        return true;
      }
    }
    return super.hasUserSelectAccess(user, obj, throwException);
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final TimesheetDO obj, final boolean throwException) {
    return hasAccess(user, obj, null, OperationType.SELECT, throwException);
  }

  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final TimesheetDO obj, final TimesheetDO dbObj,
                                 final boolean throwException) {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    Validate.notNull(dbObj.getTaskId());
    Validate.notNull(obj.getTaskId());
    if (!hasAccess(user, obj, dbObj, OperationType.UPDATE, throwException)) {
      return false;
    }
    if (!dbObj.getUserId().equals(obj.getUserId())) {
      // User changes the owner of the time sheet:
      if (!hasAccess(user, dbObj, null, OperationType.DELETE, throwException)) {
        // Deleting of time sheet of another user is not allowed.
        return false;
      }
    }
    if (!dbObj.getTaskId().equals(obj.getTaskId())) {
      // User moves the object to another task:
      if (!hasAccess(user, obj, null, OperationType.INSERT, throwException)) {
        // Inserting of object under new task not allowed.
        return false;
      }
      if (!hasAccess(user, dbObj, null, OperationType.DELETE, throwException)) {
        // Deleting of object under old task not allowed.
        return false;
      }
    }
    if (hasTimeOverlap(obj, throwException)) {
      return false;
    }
    boolean result = checkTimesheetProtection(user, obj, dbObj, OperationType.UPDATE, throwException);
    if (result) {
      result = checkTaskBookable(obj, dbObj, OperationType.UPDATE, throwException);
    }
    return result;
  }

  @Override
  public boolean hasInsertAccess(final PFUserDO user, final TimesheetDO obj, final boolean throwException) {
    if (!hasAccess(user, obj, null, OperationType.INSERT, throwException)) {
      return false;
    }
    if (hasTimeOverlap(obj, throwException)) {
      return false;
    }
    boolean result = checkTimesheetProtection(user, obj, null, OperationType.INSERT, throwException);
    if (result) {
      result = checkTaskBookable(obj, null, OperationType.INSERT, throwException);
    }
    return result;
  }

  /**
   * Checks whether the time sheet is book-able or not. The checks are:
   * <ol>
   * <li>Only for update mode: If the time sheet is unmodified in start and stop time, kost2, task and user then return
   * true without further checking.</li>
   * <li>Is the task or any of the ancestor tasks closed or deleted?</li>
   * <li>Has the task or any of the ancestor tasks the TimesheetBookingStatus.TREE_CLOSED?</li>
   * <li>Is the task not a leaf node and has this task or ancestor task the booking status ONLY_LEAFS?</li>
   * <li>Does any of the descendant task node has an assigned order position?</li>
   * </ol>
   *
   * @param timesheet    The time sheet to insert or update.
   * @param oldTimesheet The origin time sheet from the data base (could be null, if no update is done).
   * @return True if none of the rules above matches.
   */
  public boolean checkTaskBookable(final TimesheetDO timesheet, final TimesheetDO oldTimesheet,
                                   final OperationType operationType,
                                   final boolean throwException) {
    if (operationType == OperationType.UPDATE) {
      if (timesheet.getStartTime().getTime() == oldTimesheet.getStartTime().getTime()
              && timesheet.getStopTime().getTime() == oldTimesheet.getStopTime().getTime()
              && Objects.equals(timesheet.getKost2Id(), oldTimesheet.getKost2Id())
              && Objects.equals(timesheet.getTaskId(), oldTimesheet.getTaskId())
              && Objects.equals(timesheet.getUserId(), oldTimesheet.getUserId())) {
        // Only minor fields are modified (description, location etc.).
        return true;
      }
    }
    final TaskNode taskNode = TaskTreeHelper.getTaskTree(timesheet).getTaskNodeById(timesheet.getTaskId());
    // 1. Is the task or any of the ancestor tasks closed, deleted or has the booking status TREE_CLOSED?
    TaskNode node = taskNode;
    do {
      final TaskDO task = node.getTask();
      String errorMessage = null;
      if (task.isDeleted()) {
        errorMessage = "timesheet.error.taskNotBookable.taskDeleted";
      } else if (!task.getStatus().isIn(TaskStatus.O, TaskStatus.N)) {
        errorMessage = "timesheet.error.taskNotBookable.taskNotOpened";
      } else if (task.getTimesheetBookingStatus() == TimesheetBookingStatus.TREE_CLOSED) {
        errorMessage = "timesheet.error.taskNotBookable.treeClosedForBooking";
      }
      if (errorMessage != null) {
        if (throwException) {
          throw new AccessException(errorMessage, task.getTitle() + " (#" + task.getId() + ")");
        }
        return false;
      }
      node = node.getParent();
    } while (node != null);
    // 2. Has the task the booking status NO_BOOKING?
    TimesheetBookingStatus bookingStatus = taskNode.getTask().getTimesheetBookingStatus();
    node = taskNode;
    while (bookingStatus == TimesheetBookingStatus.INHERIT && node.getParent() != null) {
      node = node.getParent();
      bookingStatus = node.getTask().getTimesheetBookingStatus();
    }
    if (bookingStatus == TimesheetBookingStatus.NO_BOOKING) {
      if (throwException) {
        throw new AccessException("timesheet.error.taskNotBookable.taskClosedForBooking",
                taskNode.getTask().getTitle()
                        + " (#"
                        + taskNode.getId()
                        + ")");
      }
      return false;
    }
    if (taskNode.hasChildren()) {
      // 3. Is the task not a leaf node and has this task or ancestor task the booking status ONLY_LEAFS?
      node = taskNode;
      do {
        final TaskDO task = node.getTask();
        if (task.getTimesheetBookingStatus() == TimesheetBookingStatus.ONLY_LEAFS) {
          if (throwException) {
            throw new AccessException("timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking",
                    taskNode.getTask().getTitle()
                            + " (#"
                            + taskNode.getId()
                            + ")");
          }
          return false;
        }
        node = node.getParent();
      } while (node != null);
      // 4. Does any of the descendant task node has an assigned order position?
      for (final TaskNode child : taskNode.getChildren()) {
        if (TaskTreeHelper.getTaskTree(timesheet).hasOrderPositions(child.getId(), true)) {
          if (throwException) {
            throw new AccessException("timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks",
                    taskNode.getTask().getTitle()
                            + " (#"
                            + taskNode.getId()
                            + ")");
          }
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks if there exists any time sheet protection on the corresponding task or one of the ancestor tasks. If the
   * times sheet is protected and the duration of this time sheet is modified, and AccessException will be thrown. <br/>
   * Checks insert, update and delete operations. If an existing time sheet has to be modified, the check will only be
   * done, if any modifications of the time stamps is done (e. g. descriptions of the task are allowed if the start and
   * stop time is untouched).
   *
   * @param oldTimesheet   (null for delete and insert)
   * @param throwException If true and the time sheet protection is violated then an AccessException will be thrown.
   * @return true, if no time sheet protection is violated or if the logged in user is member of the finance group.
   * @see ProjectForgeGroup#FINANCE_GROUP
   */
  public boolean checkTimesheetProtection(final PFUserDO user, final TimesheetDO timesheet,
                                          final TimesheetDO oldTimesheet,
                                          final OperationType operationType, final boolean throwException) {
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)
            && !accessChecker.userEquals(user, timesheet.getUser())) {
      // Member of financial group are able to book foreign time sheets.
      return true;
    }
    if (operationType == OperationType.UPDATE) {
      if (timesheet.getStartTime().getTime() == oldTimesheet.getStartTime().getTime()
              && timesheet.getStopTime().getTime() == oldTimesheet.getStopTime().getTime()
              && Objects.equals(timesheet.getKost2Id(), oldTimesheet.getKost2Id())) {
        return true;
      }
    }
    final TaskTree taskTree = TaskTreeHelper.getTaskTree(timesheet);
    final TaskNode taskNode = taskTree.getTaskNodeById(timesheet.getTaskId());
    Validate.notNull(taskNode);
    final List<TaskNode> list = taskNode.getPathToRoot();
    list.add(0, taskTree.getRootTaskNode());
    for (final TaskNode node : list) {
      final Date date = node.getTask().getProtectTimesheetsUntil();
      if (date == null) {
        continue;
      }
      final DateHolder dh = new DateHolder(date);
      dh.setEndOfDay();
      //New and existing startdate have to be checked for protection
      if ((oldTimesheet != null && oldTimesheet.getStartTime().before(dh.getDate())) || timesheet.getStartTime().before(dh.getDate())) {
        if (throwException) {
          throw new AccessException("timesheet.error.timesheetProtectionVioloation", node.getTask().getTitle()
                  + " (#"
                  + node.getTaskId()
                  + ")", DateHelper.formatIsoDate(dh.getDate()));
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Get all locations of the user's time sheet (not deleted ones) with modification date within last year.
   */
  public List<String> getLocationAutocompletion(final String searchString) {
    checkLoggedInUserSelectAccess();
    PFDateTime oneYearAgo = PFDateTime.now().minusDays(365);
    return em.createNamedQuery(TimesheetDO.SELECT_USED_LOCATIONS_BY_USER_AND_LOCATION_SEARCHSTRING, String.class)
            .setParameter("userId", ThreadLocalUserContext.getUserId())
            .setParameter("lastUpdate", oneYearAgo.getUtilDate())
            .setParameter("locationSearch", "%" + StringUtils.lowerCase(searchString) + "%")
            .getResultList();
  }

  /**
   * Get all locations of the user's time sheet (not deleted ones) with modification date within last year.
   *
   * @param maxResults Limit the result to the recent locations.
   * @return result as Json object.
   */
  public Collection<String> getRecentLocation(final int maxResults) {
    checkLoggedInUserSelectAccess();
    log.info("Get recent locations from the database.");
    PFDateTime oneYearAgo = PFDateTime.now().minusDays(365);
    return em.createNamedQuery(TimesheetDO.SELECT_RECENT_USED_LOCATIONS_BY_USER_AND_LAST_UPDATE, String.class)
            .setParameter("userId", ThreadLocalUserContext.getUserId())
            .setParameter("lastUpdate", oneYearAgo.getUtilDate())
            .getResultList();
  }

  @Override
  protected Object prepareMassUpdateStore(final List<TimesheetDO> list, final TimesheetDO master) {
    if (master.getTaskId() != null) {
      return getKost2List(master);
    }
    return null;
  }

  private boolean contains(final List<Kost2DO> kost2List, final Integer kost2Id) {
    for (final Kost2DO entry : kost2List) {
      if (kost2Id.compareTo(entry.getId()) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean massUpdateEntry(final TimesheetDO entry, final TimesheetDO master, final Object store) {
    if (store != null) {
      @SuppressWarnings("unchecked") final List<Kost2DO> kost2List = (List<Kost2DO>) store;
      if (master.getKost2Id() != null) {
        if (!contains(kost2List, master.getKost2Id())) {
          throw new UserException("timesheet.error.massupdate.kost2notsupported");
        }
        setKost2(entry, master.getKost2Id());
      } else if (entry.getKost2Id() == null) {
        throw new UserException("timesheet.error.massupdate.kost2null");
      } else if (!contains(kost2List, entry.getKost2Id())) {
        // Try to convert kost2 ids from old project to new project.
        boolean success = false;
        for (final Kost2DO kost2 : kost2List) {
          if (kost2.getKost2ArtId().compareTo(entry.getKost2().getKost2ArtId()) == 0) {
            success = true; // found.
            entry.setKost2(kost2);
            break;
          }
        }
        if (!success) {
          throw new UserException("timesheet.error.massupdate.couldnotconvertkost2");
        }
      }
    }
    if (master.getTaskId() != null) {
      setTask(entry, master.getTaskId());
    }
    if (master.getKost2Id() != null) {
      setKost2(entry, master.getKost2Id());
    }
    //    } else if (store == null) {
    //      // clear destination kost2 if master has no kost2 and there is no kost2List
    //      entry.setKost2(null);
    //    }
    if (StringUtils.isNotBlank(master.getLocation())) {
      entry.setLocation(master.getLocation());
    }
    return true;
  }

  @Override
  public TimesheetDO newInstance() {
    return new TimesheetDO();
  }
}
