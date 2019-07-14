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
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import org.projectforge.framework.i18n.MessageParam;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class TimesheetDao extends BaseDao<TimesheetDO>
{
  /**
   * Maximum allowed duration of time sheets is 14 hours.
   */
  public static final long MAXIMUM_DURATION = 1000 * 3600 * 14;

  /**
   * Internal error message if maximum duration is exceeded.
   */
  private static final String MAXIMUM_DURATION_EXCEEDED = "Maximum duration of time sheet exceeded. Maximum is "
      + (MAXIMUM_DURATION / 3600 / 1000)
      + "h!";

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "user.username", "user.firstname",
      "user.lastname", "task", "kost2.nummer", "kost2.description", "kost2.projekt.name" };

  public static final String HIDDEN_FIELD_MARKER = "[...]";

  private static final Logger log = LoggerFactory.getLogger(TimesheetDao.class);

  @Autowired
  private UserDao userDao;

  @Autowired
  private Kost2Dao kost2Dao;

  private final Map<Integer, Set<Integer>> timesheetsWithOverlapByUser = new HashMap<Integer, Set<Integer>>();

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * List of all years with time sheets of the given user: select min(startTime), max(startTime) from t_timesheet where
   * user=?.
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public int[] getYears(final Integer userId)
  {
    final List<Object[]> list = (List<Object[]>) getHibernateTemplate().find(
        "select min(startTime), max(startTime) from TimesheetDO t where user.id=? and deleted=false", userId);
    return SQLHelper.getYears(list);
  }

  /**
   * @param sheet
   * @param userId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setUser(final TimesheetDO sheet, final Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    sheet.setUser(user);
  }

  /**
   * @param sheet
   * @param taskId If null, then task will be set to null;
   * @see TaskTree#getTaskById(Integer)
   */
  public void setTask(final TimesheetDO sheet, final Integer taskId)
  {
    final TaskDO task = TaskTreeHelper.getTaskTree(sheet).getTaskById(taskId);
    sheet.setTask(task);
  }

  /**
   * @param sheet
   * @param kost2Id If null, then kost2 will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKost2(final TimesheetDO sheet, final Integer kost2Id)
  {
    final Kost2DO kost2 = kost2Dao.getOrLoad(kost2Id);
    sheet.setKost2(kost2);
  }

  /**
   * Gets the available Kost2DO's for the given time sheet. The task must already be assigned to this time sheet.
   *
   * @param timesheet
   * @return Available list of Kost2DO's or null, if not exist.
   */
  public List<Kost2DO> getKost2List(final TimesheetDO timesheet)
  {
    if (timesheet == null || timesheet.getTaskId() == null) {
      return null;
    }
    return TaskTreeHelper.getTaskTree(timesheet).getKost2List(timesheet.getTaskId());
  }

  public QueryFilter buildQueryFilter(final TimesheetFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter(filter);
    if (filter.getUserId() != null) {
      final PFUserDO user = new PFUserDO();
      user.setId(filter.getUserId());
      queryFilter.add(Restrictions.eq("user", user));
    }
    if (filter.getStartTime() != null && filter.getStopTime() != null) {
      queryFilter.add(Restrictions.and(Restrictions.ge("stopTime", filter.getStartTime()),
          Restrictions.le("startTime", filter.getStopTime())));
    } else if (filter.getStartTime() != null) {
      queryFilter.add(Restrictions.ge("startTime", filter.getStartTime()));
    } else if (filter.getStopTime() != null) {
      queryFilter.add(Restrictions.le("startTime", filter.getStopTime()));
    }
    if (filter.getTaskId() != null) {
      if (filter.isRecursive() == true) {
        final TaskNode node = TaskTreeHelper.getTaskTree().getTaskNodeById(filter.getTaskId());
        final List<Integer> taskIds = node.getDescendantIds();
        taskIds.add(node.getId());
        queryFilter.add(Restrictions.in("task.id", taskIds));
        if (log.isDebugEnabled() == true) {
          log.debug("search in tasks: " + taskIds);
        }
      } else {
        queryFilter.add(Restrictions.eq("task.id", filter.getTaskId()));
      }
    }
    if (filter.getOrderType() == OrderDirection.DESC) {
      queryFilter.addOrder(Order.desc("startTime"));
    } else {
      queryFilter.addOrder(Order.asc("startTime"));
    }
    if (log.isDebugEnabled() == true) {
      log.debug(ToStringBuilder.reflectionToString(filter));
    }
    return queryFilter;
  }

  public TimesheetDao()
  {
    super(TimesheetDO.class);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getListForSearchDao(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  public List<TimesheetDO> getListForSearchDao(final BaseSearchFilter filter)
  {
    final TimesheetFilter timesheetFilter = new TimesheetFilter(filter);
    if (filter.getModifiedByUserId() == null) {
      timesheetFilter.setUserId(ThreadLocalUserContext.getUserId());
    }
    return getList(timesheetFilter);
  }

  /**
   * Gets the list filtered by the given filter.
   *
   * @param filter
   * @return
   */
  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<TimesheetDO> getList(final BaseSearchFilter filter) throws AccessException
  {
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
    List<TimesheetDO> result = getList(queryFilter);
    if (result == null) {
      return null;
    }
    // Check time period overlaps:
    for (final TimesheetDO entry : result) {
      Validate.notNull(entry.getUserId());
      if (entry.getMarked() == true) {
        continue; // Is already marked.
      }
      final Set<Integer> overlapSet = getTimesheetsWithTimeoverlap(entry.getUserId());
      if (overlapSet.contains(entry.getId()) == true) {
        log.info("Overlap of time sheet decteced: " + entry);
        entry.setMarked(true);
      }
    }
    if (myFilter.isMarked() == true) {
      // Show only time sheets with time period violation (overlap):
      final List<TimesheetDO> list = result;
      result = new ArrayList<TimesheetDO>();
      for (final TimesheetDO entry : list) {
        if (entry.getMarked() == true) {
          result.add(entry);
        }
      }
    }
    if (myFilter.isOnlyBillable() == true) {
      final List<TimesheetDO> list = result;
      result = new ArrayList<TimesheetDO>();
      for (final TimesheetDO entry : list) {
        if (entry.getKost2() != null && entry.getKost2().getKost2Art() != null && entry.getKost2().getKost2Art().getFakturiert()) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  //TODO:
  //  public List<TimesheetDO> getTimeperiodOverlapList(final TimesheetListFilter actionFilter)
  //  {
  //    if (actionFilter.getUserId() != null) {
  //      final QueryFilter queryFilter = new QueryFilter(actionFilter, tenantsCache);
  //      final Set<Integer> set = getTimesheetsWithTimeoverlap(actionFilter.getUserId());
  //      if (set == null || set.size() == 0) {
  //        // No time sheets with overlap found.
  //        return new ArrayList<TimesheetDO>();
  //      }
  //      queryFilter.add(Restrictions.in("id", set));
  //      final List<TimesheetDO> result = getList(queryFilter);
  //      for (final TimesheetDO entry : result) {
  //        entry.setMarked(true);
  //      }
  //      Collections.sort(result, Collections.reverseOrder());
  //      return result;
  //    }
  //    return getList(actionFilter);
  //  }

  /**
   * Rechecks the time sheet overlaps.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterSaveOrModify(final TimesheetDO obj)
  {
    super.afterSaveOrModify(obj);
    if (obj.getUser() != null) {
      // Force re-analysis of time sheet overlaps after any modification of time sheets.
      recheckTimesheetOverlap(obj.getUserId());
    }
    TaskTreeHelper.getTaskTree(obj).resetTotalDuration(obj.getTaskId());
  }

  /**
   * Checks the start and stop time. If seconds or millis is not null, a RuntimeException will be thrown.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final TimesheetDO obj)
  {
    validateTimestamp(obj.getStartTime(), "startTime");
    validateTimestamp(obj.getStopTime(), "stopTime");
    Validate.isTrue(obj.getDuration() >= 60000, "Duration of time sheet must be at minimum 60s!");
    Validate.isTrue(obj.getDuration() <= MAXIMUM_DURATION, MAXIMUM_DURATION_EXCEEDED);
    Validate.isTrue(obj.getStartTime().before(obj.getStopTime()), "Stop time of time sheet is before start time!");
    final List<Kost2DO> kost2List = TaskTreeHelper.getTaskTree(obj).getKost2List(obj.getTaskId());
    final Integer kost2Id = obj.getKost2Id();
    if (CollectionUtils.isNotEmpty(kost2List) == true) {
      Validate.notNull(kost2Id, "Kost2Id must be given for time sheet and given kost2 list!");
      boolean kost2IdFound = false;
      for (final Kost2DO kost2 : kost2List) {
        if (NumberHelper.isEqual(kost2Id, kost2.getId()) == true) {
          kost2IdFound = true;
          break;
        }
      }
      Validate.isTrue(kost2IdFound, "Kost2Id of time sheet is not available in the task's kost2 list!");
    } else {
      Validate.isTrue(kost2Id == null, "Kost2Id can't be given for task without any kost2 entries!");
    }
  }

  @Override
  protected void onChange(final TimesheetDO obj, final TimesheetDO dbObj)
  {
    if (obj.getTaskId().compareTo(dbObj.getTaskId()) != 0) {
      TaskTreeHelper.getTaskTree(obj).resetTotalDuration(dbObj.getTaskId());
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#prepareHibernateSearch(org.projectforge.core.ExtendedBaseDO,
   * org.projectforge.framework.access.OperationType)
   */
  @Override
  protected void prepareHibernateSearch(final TimesheetDO obj, final OperationType operationType)
  {
    final PFUserDO user = obj.getUser();
    if (user != null && Hibernate.isInitialized(user) == false) {
      obj.setUser(getUserGroupCache().getUser(user.getId()));
    }
    final TaskDO task = obj.getTask();
    if (task != null && Hibernate.isInitialized(task) == false) {
      obj.setTask(TaskTreeHelper.getTaskTree(obj).getTaskById(task.getId()));
    }
  }

  private void validateTimestamp(final Date date, final String name)
  {
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
   * Analyses all time sheets of the user and detects any collision (overlap) of the user's time sheets. The result will
   * be cached and the duration of a new analysis is only a few milliseconds!
   *
   * @param user
   * @return
   */
  public Set<Integer> getTimesheetsWithTimeoverlap(final Integer userId)
  {
    long begin = System.currentTimeMillis();
    Validate.notNull(userId);
    final PFUserDO user = getUserGroupCache().getUser(userId);
    Validate.notNull(user);
    synchronized (timesheetsWithOverlapByUser) {
      if (timesheetsWithOverlapByUser.get(userId) != null) {
        return timesheetsWithOverlapByUser.get((userId));
      }
      // log.info("Getting time sheet overlaps for user: " + user.getUsername());
      final Set<Integer> result = new HashSet<Integer>();
      final QueryFilter queryFilter = new QueryFilter();
      queryFilter.add(Restrictions.eq("user", user));
      queryFilter.addOrder(Order.asc("startTime"));
      final List<TimesheetDO> list = getList(queryFilter);
      long endTime = 0;
      TimesheetDO lastEntry = null;
      for (final TimesheetDO entry : list) {
        if (entry.getStartTime().getTime() < endTime) {
          // Time collision!
          result.add(entry.getId());
          if (lastEntry != null) { // Only for first iteration
            result.add(lastEntry.getId()); // Also collision for last entry.
          }
        }
        endTime = entry.getStopTime().getTime();
        lastEntry = entry;
      }
      timesheetsWithOverlapByUser.put(user.getId(), result);
      if (CollectionUtils.isNotEmpty(result) == true) {
        log.info("Time sheet overlaps for user '" + user.getUsername() + "': " + result);
      }
      long end = System.currentTimeMillis();
      log.info("TimesheetDao.getTimesheetsWithTimeoverlap took: " + (end - begin) + " ms.");
      return result;
    }
  }

  /**
   * Deletes any existing time sheet overlap analysis and forces therefore a new analysis before next time sheet list
   * selection. (The analysis will not be started inside this method!)
   *
   * @param userId
   */
  public void recheckTimesheetOverlap(final Integer userId)
  {
    Validate.notNull(userId);
    timesheetsWithOverlapByUser.remove(userId);
  }

  /**
   * Checks if the time sheet overlaps with another time sheet of the same user. Should be checked on every insert or
   * update (also undelete). For time collision detection deleted time sheets are ignored.
   *
   * @return The existing time sheet with the time period collision.
   */
  public boolean hasTimeOverlap(final TimesheetDO timesheet, final boolean throwException)
  {
    long begin = System.currentTimeMillis();
    Validate.notNull(timesheet);
    Validate.notNull(timesheet.getUser());
    final QueryFilter queryFilter = new QueryFilter();
    queryFilter.add(Restrictions.eq("user", timesheet.getUser()));
    queryFilter.add(Restrictions.lt("startTime", timesheet.getStopTime()));
    queryFilter.add(Restrictions.gt("stopTime", timesheet.getStartTime()));
    if (timesheet.getId() != null) {
      // Update time sheet, do not compare with itself.
      queryFilter.add(Restrictions.ne("id", timesheet.getId()));
    }
    final List<TimesheetDO> list = getList(queryFilter);
    if (list != null && list.size() > 0) {
      final TimesheetDO ts = list.get(0);
      if (throwException == true) {
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
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final TimesheetDO obj, final TimesheetDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    if (accessChecker.userEquals(user, obj.getUser()) == true) {
      // Own time sheet
      if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.OWN_TIMESHEETS, operationType,
          throwException) == false) {
        return false;
      }
    } else {
      // Foreign time sheet
      if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP) == true) {
        return true;
      }
      if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TIMESHEETS, operationType,
          throwException) == false) {
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
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(PFUserDO,
   * org.projectforge.core.ExtendedBaseDO, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final TimesheetDO obj, final boolean throwException)
  {
    if (hasAccess(user, obj, null, OperationType.SELECT, false) == false) {
      // User has no access by definition.
      if (accessChecker.userEquals(user, obj.getUser()) == true
          || accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.PROJECT_MANAGER) == true) {
        if (accessChecker.userEquals(user, obj.getUser()) == false) {
          // Check protection of privacy for foreign time sheets:
          final List<TaskNode> pathToRoot = TaskTreeHelper.getTaskTree(obj).getPathToRoot(obj.getTaskId());
          for (final TaskNode node : pathToRoot) {
            if (node.getTask().getProtectionOfPrivacy() == true) {
              return false;
            }
          }
        }
        // An user should see his own time sheets, but the values should be hidden.
        // A project manager should also see all time sheets, but the values should be hidden.
        getSession().evict(obj);
        obj.setDescription(HIDDEN_FIELD_MARKER);
        obj.setLocation(HIDDEN_FIELD_MARKER);
        log.debug("User has no access to own time sheet (or project manager): " + obj);
        return true;
      }
    }
    return super.hasSelectAccess(user, obj, throwException);
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final TimesheetDO obj, final boolean throwException)
  {
    return hasAccess(user, obj, null, OperationType.SELECT, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUpdateAccess(Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final TimesheetDO obj, final TimesheetDO dbObj,
      final boolean throwException)
  {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    Validate.notNull(dbObj.getTaskId());
    Validate.notNull(obj.getTaskId());
    if (hasAccess(user, obj, dbObj, OperationType.UPDATE, throwException) == false) {
      return false;
    }
    if (dbObj.getUserId().equals(obj.getUserId()) == false) {
      // User changes the owner of the time sheet:
      if (hasAccess(user, dbObj, null, OperationType.DELETE, throwException) == false) {
        // Deleting of time sheet of another user is not allowed.
        return false;
      }
    }
    if (dbObj.getTaskId().equals(obj.getTaskId()) == false) {
      // User moves the object to another task:
      if (hasAccess(user, obj, null, OperationType.INSERT, throwException) == false) {
        // Inserting of object under new task not allowed.
        return false;
      }
      if (hasAccess(user, dbObj, null, OperationType.DELETE, throwException) == false) {
        // Deleting of object under old task not allowed.
        return false;
      }
    }
    if (hasTimeOverlap(obj, throwException) == true) {
      return false;
    }
    boolean result = checkTimesheetProtection(user, obj, dbObj, OperationType.UPDATE, throwException);
    if (result == true) {
      result = checkTaskBookable(obj, dbObj, OperationType.UPDATE, throwException);
    }
    return result;
  }

  @Override
  public boolean hasInsertAccess(final PFUserDO user, final TimesheetDO obj, final boolean throwException)
  {
    if (hasAccess(user, obj, null, OperationType.INSERT, throwException) == false) {
      return false;
    }
    if (hasTimeOverlap(obj, throwException) == true) {
      return false;
    }
    boolean result = checkTimesheetProtection(user, obj, null, OperationType.INSERT, throwException);
    if (result == true) {
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
   * @param timesheet      The time sheet to insert or update.
   * @param oldTimesheet   The origin time sheet from the data base (could be null, if no update is done).
   * @param operationType
   * @param throwException
   * @return True if none of the rules above matches.
   */
  public boolean checkTaskBookable(final TimesheetDO timesheet, final TimesheetDO oldTimesheet,
      final OperationType operationType,
      final boolean throwException)
  {
    if (operationType == OperationType.UPDATE) {
      if (timesheet.getStartTime().getTime() == oldTimesheet.getStartTime().getTime()
          && timesheet.getStopTime().getTime() == oldTimesheet.getStopTime().getTime()
          && Objects.equals(timesheet.getKost2Id(), oldTimesheet.getKost2Id()) == true
          && Objects.equals(timesheet.getTaskId(), oldTimesheet.getTaskId()) == true
          && Objects.equals(timesheet.getUserId(), oldTimesheet.getUserId()) == true) {
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
      if (task.isDeleted() == true) {
        errorMessage = "timesheet.error.taskNotBookable.taskDeleted";
      } else if (task.getStatus().isIn(TaskStatus.O, TaskStatus.N) == false) {
        errorMessage = "timesheet.error.taskNotBookable.taskNotOpened";
      } else if (task.getTimesheetBookingStatus() == TimesheetBookingStatus.TREE_CLOSED) {
        errorMessage = "timesheet.error.taskNotBookable.treeClosedForBooking";
      }
      if (errorMessage != null) {
        if (throwException == true) {
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
      if (throwException == true) {
        throw new AccessException("timesheet.error.taskNotBookable.taskClosedForBooking",
            taskNode.getTask().getTitle()
                + " (#"
                + taskNode.getId()
                + ")");
      }
      return false;
    }
    if (taskNode.hasChilds() == true) {
      // 3. Is the task not a leaf node and has this task or ancestor task the booking status ONLY_LEAFS?
      node = taskNode;
      do {
        final TaskDO task = node.getTask();
        if (task.getTimesheetBookingStatus() == TimesheetBookingStatus.ONLY_LEAFS) {
          if (throwException == true) {
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
      for (final TaskNode child : taskNode.getChilds()) {
        if (TaskTreeHelper.getTaskTree(timesheet).hasOrderPositions(child.getId(), true) == true) {
          if (throwException == true) {
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
   * @param timesheet
   * @param oldTimesheet   (null for delete and insert)
   * @param throwException If true and the time sheet protection is violated then an AccessException will be thrown.
   * @return true, if no time sheet protection is violated or if the logged in user is member of the finance group.
   * @see ProjectForgeGroup#FINANCE_GROUP
   */
  public boolean checkTimesheetProtection(final PFUserDO user, final TimesheetDO timesheet,
      final TimesheetDO oldTimesheet,
      final OperationType operationType, final boolean throwException)
  {
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP) == true
        && accessChecker.userEquals(user, timesheet.getUser()) == false) {
      // Member of financial group are able to book foreign time sheets.
      return true;
    }
    if (operationType == OperationType.UPDATE) {
      if (timesheet.getStartTime().getTime() == oldTimesheet.getStartTime().getTime()
          && timesheet.getStopTime().getTime() == oldTimesheet.getStopTime().getTime()
          && Objects.equals(timesheet.getKost2Id(), oldTimesheet.getKost2Id()) == true) {
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
      if ((oldTimesheet != null && oldTimesheet.getStartTime().before(dh.getDate()) == true) || timesheet.getStartTime().before(dh.getDate()) == true) {
        if (throwException == true) {
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
   *
   * @param searchString
   */
  @SuppressWarnings("unchecked")
  public List<String> getLocationAutocompletion(final String searchString)
  {
    checkLoggedInUserSelectAccess();
    final String s = "select distinct location from "
        + clazz.getSimpleName()
        + " t where deleted=false and t.user.id = ? and lastUpdate > ? and lower(t.location) like ?) order by t.location";
    final Query query = getSession().createQuery(s);
    query.setInteger(0, ThreadLocalUserContext.getUser().getId());
    final DateHolder dh = new DateHolder();
    dh.add(Calendar.YEAR, -1);
    query.setDate(1, dh.getDate());
    query.setString(2, "%" + StringUtils.lowerCase(searchString) + "%");
    final List<String> list = query.list();
    return list;
  }

  /**
   * Get all locations of the user's time sheet (not deleted ones) with modification date within last year.
   *
   * @param maxResults Limit the result to the recent locations.
   * @return result as Json object.
   */
  @SuppressWarnings("unchecked")
  public Collection<String> getRecentLocation(final int maxResults)
  {
    checkLoggedInUserSelectAccess();
    log.info("Get recent locations from the database.");
    final String s = "select location from "
        + (clazz.getSimpleName()
        + " t where deleted=false and t.user.id = ? and lastUpdate > ? and t.location != null and t.location != '' order by t.lastUpdate desc");
    final Query query = getSession().createQuery(s);
    query.setInteger(0, ThreadLocalUserContext.getUser().getId());
    final DateHolder dh = new DateHolder();
    dh.add(Calendar.YEAR, -1);
    query.setDate(1, dh.getDate());
    final List<Object> list = query.list();
    int counter = 0;
    final List<String> res = new ArrayList<String>();
    for (final Object loc : list) {
      if (res.contains(loc) == true) {
        continue;
      }
      res.add((String) loc);
      if (++counter >= maxResults) {
        break;
      }
    }
    return res;
  }

  @Override
  protected Object prepareMassUpdateStore(final List<TimesheetDO> list, final TimesheetDO master)
  {
    if (master.getTaskId() != null) {
      return getKost2List(master);
    }
    return null;
  }

  private boolean contains(final List<Kost2DO> kost2List, final Integer kost2Id)
  {
    for (final Kost2DO entry : kost2List) {
      if (kost2Id.compareTo(entry.getId()) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean massUpdateEntry(final TimesheetDO entry, final TimesheetDO master, final Object store)
  {
    if (store != null) {
      @SuppressWarnings("unchecked")
      final List<Kost2DO> kost2List = (List<Kost2DO>) store;
      if (master.getKost2Id() != null) {
        if (contains(kost2List, master.getKost2Id()) == false) {
          throw new UserException("timesheet.error.massupdate.kost2notsupported");
        }
        setKost2(entry, master.getKost2Id());
      } else if (entry.getKost2Id() == null) {
        throw new UserException("timesheet.error.massupdate.kost2null");
      } else if (contains(kost2List, entry.getKost2Id()) == false) {
        // Try to convert kost2 ids from old project to new project.
        boolean success = false;
        for (final Kost2DO kost2 : kost2List) {
          if (kost2.getKost2ArtId().compareTo(entry.getKost2().getKost2ArtId()) == 0) {
            success = true; // found.
            entry.setKost2(kost2);
            break;
          }
        }
        if (success == false) {
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
    if (StringUtils.isNotBlank(master.getLocation()) == true) {
      entry.setLocation(master.getLocation());
    }
    return true;
  }

  @Override
  public TimesheetDO newInstance()
  {
    return new TimesheetDO();
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
