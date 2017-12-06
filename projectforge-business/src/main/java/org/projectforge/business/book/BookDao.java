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

package org.projectforge.business.book;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class BookDao extends BaseDao<BookDO>
{
  // private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "lendOutBy.username", "lendOutBy.firstname",
      "lendOutBy.lastname", "task.title" };

  @Autowired
  private UserDao userDao;

  @Autowired
  private TaskDao taskDao;

  public BookDao()
  {
    super(BookDO.class);
  }

  /**
   * Books will be assigned to a default task.
   */
  public Integer getDefaultTaskId()
  {
    return Configuration.getInstance().getTaskIdValue(ConfigurationParam.DEFAULT_TASK_ID_4_BOOKS);
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public List<BookDO> getList(final BaseSearchFilter filter)
  {
    final BookFilter myFilter;
    if (filter instanceof BookFilter) {
      myFilter = (BookFilter) filter;
    } else {
      myFilter = new BookFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    Collection<BookStatus> col = null;
    Criterion inCrit = null;
    if (myFilter.isPresent() == true || myFilter.isMissed() == true || myFilter.isDisposed() == true) {
      col = new ArrayList<BookStatus>();
      if (myFilter.isPresent() == true) {
        // Book must be have status 'present'.
        col.add(BookStatus.PRESENT);
      }
      if (myFilter.isMissed() == true) {
        // Book must be have status 'missed'.
        col.add(BookStatus.MISSED);
      }
      if (myFilter.isDisposed() == true) {
        // Book must be have status 'disposed'.
        col.add(BookStatus.DISPOSED);
      }
      queryFilter.add(Restrictions.in("status", col));
    }
    queryFilter.addOrder(Order.desc("created"));
    queryFilter.addOrder(Order.asc("authors"));
    return getList(queryFilter);
  }

  /**
   * Does the book's signature already exists? If signature is null, then return always false.
   *
   * @param signature
   * @return
   */
  @SuppressWarnings("unchecked")
  public boolean doesSignatureAlreadyExist(final BookDO book)
  {
    Validate.notNull(book);
    if (book.getSignature() == null) {
      return false;
    }
    List<BookDO> list = null;
    if (book.getId() == null) {
      // New book
      list = (List<BookDO>) getHibernateTemplate().find("from BookDO b where b.signature = ?", book.getSignature());
    } else {
      // Book already exists. Check maybe changed signature:
      list = (List<BookDO>) getHibernateTemplate().find("from BookDO b where b.signature = ? and pk <> ?",
          new Object[] { book.getSignature(), book.getId() });
    }
    if (CollectionUtils.isNotEmpty(list) == true) {
      return true;
    }
    return false;
  }

  @Override
  protected void onSaveOrModify(final BookDO obj)
  {
    if (obj.getTaskId() == null) {
      setTask(obj, getDefaultTaskId());
    }
  }

  /**
   * @param book
   * @param taskId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setTask(final BookDO book, final Integer taskId)
  {
    final TaskDO task = taskDao.getOrLoad(taskId);
    book.setTask(task);
  }

  /**
   * @param book
   * @param lendOutById If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setLendOutBy(final BookDO book, final Integer lendOutById)
  {
    final PFUserDO user = userDao.getOrLoad(lendOutById);
    book.setLendOutBy(user);
  }

  /**
   * return Always true, no generic select access needed for book objects.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final BookDO obj, final BookDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, operationType, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUpdateAccess(Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final BookDO obj, final BookDO dbObj,
      final boolean throwException)
  {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    Validate.notNull(dbObj.getTaskId());
    Validate.notNull(obj.getTaskId());
    if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, OperationType.UPDATE,
        throwException) == false) {
      return false;
    }
    if (dbObj.getTaskId().equals(obj.getTaskId()) == false) {
      // User moves the object to another task:
      if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, OperationType.INSERT,
          throwException) == false) {
        // Inserting of object under new task not allowed.
        return false;
      }
      if (accessChecker.hasPermission(user, dbObj.getTaskId(), AccessType.TASKS, OperationType.DELETE,
          throwException) == false) {
        // Deleting of object under old task not allowed.
        return false;
      }
    }
    return true;
  }

  @Override
  public BookDO newInstance()
  {
    return new BookDO();
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
