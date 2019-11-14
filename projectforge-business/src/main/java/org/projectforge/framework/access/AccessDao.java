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

package org.projectforge.framework.access;

import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.GroupDao;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class AccessDao extends BaseDao<GroupTaskAccessDO> {
  // private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccessDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"task.title", "group.name"};

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private GroupDao groupDao;

  public AccessDao() {
    super(GroupTaskAccessDO.class);
    this.supportAfterUpdate = true;
  }

  /**
   * @param access
   * @param taskId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setTask(final GroupTaskAccessDO access, final Integer taskId) {
    final TaskDO task = taskDao.getOrLoad(taskId);
    access.setTask(task);
  }

  /**
   * @param access
   * @param groupId If null, then group will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setGroup(final GroupTaskAccessDO access, final Integer groupId) {
    final GroupDO group = groupDao.getOrLoad(groupId);
    access.setGroup(group);
  }

  /**
   * Loads all GroupTaskAccessDO (not deleted ones) without any access checking.
   *
   * @return
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<GroupTaskAccessDO> internalLoadAll() {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<GroupTaskAccessDO> cr = cb.createQuery(GroupTaskAccessDO.class);
    From root = cr.from(clazz);
    root.fetch("accessEntries", JoinType.LEFT);
    cr.select(root).where(
            cb.equal(root.get("deleted"), false))
            .orderBy(cb.asc(root.get("task").get("id")), cb.asc(root.get("group").get("id")))
            .distinct(true);
    return em.createQuery(cr).getResultList();
    // from GroupTaskAccessDO g join fetch g.accessEntries where deleted=false order by g.task.id, g.group.id");
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @SuppressWarnings("unchecked")
  public GroupTaskAccessDO getEntry(final TaskDO task, final GroupDO group) {
    Validate.notNull(task);
    Validate.notNull(task.getId());
    Validate.notNull(group);
    Validate.notNull(group.getId());
    final List<GroupTaskAccessDO> list = em
            .createNamedQuery(GroupTaskAccessDO.FIND_BY_TASK_AND_GROUP, GroupTaskAccessDO.class)
            .setParameter("taskId", task.getId())
            .setParameter("groupId", group.getId())
            .getResultList();
    if (list != null && list.size() == 1) {
      final GroupTaskAccessDO access = list.get(0);
      checkLoggedInUserSelectAccess(access);
      return access;
    }
    return null;
  }

  @Override
  public List<GroupTaskAccessDO> getList(final BaseSearchFilter filter) {
    final AccessFilter myFilter;
    if (filter instanceof AccessFilter) {
      myFilter = (AccessFilter) filter;
    } else {
      myFilter = new AccessFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    TaskTree taskTree = null;
    if (myFilter.getTaskId() != null) {
      List<Integer> descendants = null;
      List<Integer> ancestors = null;
      taskTree = TaskTreeHelper.getTaskTree();
      final TaskNode node = taskTree.getTaskNodeById(myFilter.getTaskId());
      if (myFilter.isIncludeDescendentTasks()) {
        descendants = node.getDescendantIds();
      }
      if (myFilter.isInherit() || myFilter.isIncludeAncestorTasks()) {
        ancestors = node.getAncestorIds();
      }
      if (descendants != null || ancestors != null) {
        final List<Integer> taskIds = new ArrayList<>();
        if (descendants != null) {
          taskIds.addAll(descendants);
        }
        if (ancestors != null) {
          taskIds.addAll(ancestors);
        }
        taskIds.add(node.getId());
        queryFilter.add(QueryFilter.isIn("task.id", taskIds));
      } else {
        queryFilter.add(QueryFilter.eq("task.id", myFilter.getTaskId()));
      }
    }
    if (myFilter.getGroupId() != null) {
      final GroupDO group = new GroupDO();
      group.setId(myFilter.getGroupId());
      queryFilter.add(QueryFilter.eq("group", group));
    }
    final List<GroupTaskAccessDO> qlist = getList(queryFilter);
    List<GroupTaskAccessDO> list;
    if (myFilter.getTaskId() != null && myFilter.isInherit() && !myFilter.isIncludeAncestorTasks()) {
      // Now we have to remove all inherited entries of ancestor nodes which are not declared as recursive.
      list = new ArrayList<>();
      final TaskNode taskNode = taskTree.getTaskNodeById(myFilter.getTaskId());
      if (taskNode == null) { // Paranoia
        list = qlist;
      } else {
        for (final GroupTaskAccessDO access : qlist) {
          if (!access.isRecursive()) {
            final TaskNode accessNode = taskTree.getTaskNodeById(access.getTaskId());
            // && myFilter.getTaskId().equals(access.getTaskId()) == false) {
            if (accessNode.isParentOf(taskNode)) {
              // This entry is not recursive and inherited, therefore this entry will be ignored.
              continue;
            }
          }
          list.add(access);
        }
      }
    } else {
      list = qlist;
    }
    if (myFilter.getUserId() != null) {
      final List<GroupTaskAccessDO> result = new ArrayList<>();
      for (final GroupTaskAccessDO access : list) {
        if (getUserGroupCache().isUserMemberOfGroup(myFilter.getUserId(), access.getGroupId())) {
          result.add(access);
        }
      }
      return result;
    }
    return list;
  }

  /**
   * @return Always true, no generic select access needed for group task access objects.
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  /**
   * @return false, if no admin user and the context user is not member of the group. Also deleted entries are only
   * visible for admin users.
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(BaseDO, boolean)
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final GroupTaskAccessDO obj, final boolean throwException) {
    Validate.notNull(obj);
    boolean result = accessChecker.isUserMemberOfAdminGroup(user);
    if (!result && !obj.isDeleted()) {
      Validate.notNull(user);
      result = getUserGroupCache().isUserMemberOfGroup(user.getId(), obj.getGroupId());
    }
    if (throwException && !result) {
      throw new AccessException(AccessType.GROUP, OperationType.SELECT);
    }
    return result;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final GroupTaskAccessDO obj, final GroupTaskAccessDO oldObj,
                           final OperationType operationType, final boolean throwException) {
    return accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, operationType,
            throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUpdateAccess(Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final GroupTaskAccessDO obj, final GroupTaskAccessDO dbObj,
                                 final boolean throwException) {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    Validate.notNull(dbObj.getTaskId());
    Validate.notNull(obj.getTaskId());
    if (!accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, OperationType.UPDATE, throwException)) {
      return false;
    }
    if (!dbObj.getTaskId().equals(obj.getTaskId())) {
      // User moves the object to another task:
      if (!accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, OperationType.INSERT, throwException)) {
        // Inserting of object under new task not allowed.
        return false;
      }
      if (!accessChecker.hasPermission(user, dbObj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, OperationType.DELETE, throwException)) {
        // Deleting of object under old task not allowed.
        return false;
      }
    }
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#prepareHibernateSearch(ExtendedBaseDO,
   * org.projectforge.framework.access.OperationType)
   */
  @Override
  protected void prepareHibernateSearch(final GroupTaskAccessDO obj, final OperationType operationType) {
    final TaskDO task = obj.getTask();
    if (task != null && !Hibernate.isInitialized(task)) {
      Hibernate.initialize(obj.getTask());
      obj.setTask(TaskTreeHelper.getTaskTree(task).getTaskById(task.getId()));
    }
    final GroupDO group = obj.getGroup();
    if (group != null && !Hibernate.isInitialized(group)) {
      obj.setGroup(groupDao.getOrLoad(obj.getGroupId()));
    }
  }

  @Override
  protected void afterSaveOrModify(final GroupTaskAccessDO obj) {
    super.afterSaveOrModify(obj);
    TaskTreeHelper.getTaskTree(obj).setGroupTaskAccess(obj);
  }

  @Override
  protected void afterUpdate(final GroupTaskAccessDO obj, final GroupTaskAccessDO dbObj) {
    Validate.notNull(dbObj);
    final List<AccessEntryDO> entries = obj.getOrderedEntries();
    final StringBuilder bufNew = new StringBuilder();
    final StringBuilder bufOld = new StringBuilder();
    boolean firstNew = true, firstOld = true;
    for (final AccessEntryDO entry : entries) {
      final AccessEntryDO dbEntry = dbObj.getAccessEntry(entry.getAccessType());
      if (dbEntry != null
              && dbEntry.getAccessSelect() == entry.getAccessSelect()
              && dbEntry.getAccessInsert() == entry.getAccessInsert()
              && dbEntry.getAccessUpdate() == entry.getAccessUpdate()
              && dbEntry.getAccessDelete() == entry.getAccessDelete()) {
        // Nothing changed.
        continue;
      }
      if (firstNew) {
        firstNew = false;
      } else {
        bufNew.append(";");
      }
      bufNew.append(entry.getAccessType()).append("={").append(entry.getAccessSelect()).append(",")
              .append(entry.getAccessInsert())
              .append(",").append(entry.getAccessUpdate()).append(",").append(entry.getAccessDelete()).append("}");
      if (dbEntry != null) {
        if (firstOld) {
          firstOld = false;
        } else {
          bufOld.append(";");
        }
        bufOld.append(dbEntry.getAccessType()).append("={").append(dbEntry.getAccessSelect()).append(",")
                .append(dbEntry.getAccessInsert())
                .append(",").append(dbEntry.getAccessUpdate()).append(",").append(dbEntry.getAccessDelete()).append("}");
      }
    }
    if (!firstOld || !firstNew) {
      createHistoryEntry(obj, obj.getId(), "entries", String.class, bufOld.toString(), bufNew.toString());
    }
  }

  @Override
  protected GroupTaskAccessDO getBackupObject(final GroupTaskAccessDO dbObj) {
    final GroupTaskAccessDO access = new GroupTaskAccessDO();
    for (final AccessEntryDO dbEntry : dbObj.getAccessEntries()) {
      final AccessEntryDO entry = new AccessEntryDO(dbEntry.getAccessType());
      entry.setAccessSelect(dbEntry.getAccessSelect());
      entry.setAccessInsert(dbEntry.getAccessInsert());
      entry.setAccessUpdate(dbEntry.getAccessUpdate());
      entry.setAccessDelete(dbEntry.getAccessDelete());
      access.addAccessEntry(entry);
    }
    return access;
  }

  @Override
  protected void afterDelete(final GroupTaskAccessDO obj) {
    TaskTreeHelper.getTaskTree(obj.getTask()).removeGroupTaskAccess(obj);
  }

  @Override
  protected void afterUndelete(final GroupTaskAccessDO obj) {
    TaskTreeHelper.getTaskTree(obj.getTask()).setGroupTaskAccess(obj);
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  @Override
  public GroupTaskAccessDO newInstance() {
    return new GroupTaskAccessDO();
  }
}
