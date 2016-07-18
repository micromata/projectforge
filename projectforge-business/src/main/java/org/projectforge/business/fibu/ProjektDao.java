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

package org.projectforge.business.fibu;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProjektDao extends BaseDao<ProjektDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.PM_PROJECT;

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "kunde.name", "kunde.division", "kost2",
      "projektManagerGroup.name" };

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private TaskDao taskDao;

  public ProjektDao()
  {
    super(ProjektDO.class);
    this.supportAfterUpdate = true;
    userRightId = USER_RIGHT_ID;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * @param projekt
   * @param kundeId If null, then kunde will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKunde(final ProjektDO projekt, final Integer kundeId)
  {
    if (kundeId == null) {
      projekt.setKunde(null);
    } else {
      final KundeDO kunde = kundeDao.getOrLoad(kundeId);
      projekt.setKunde(kunde);
    }
  }

  /**
   * @param projekt
   * @param tadkId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setTask(final ProjektDO projekt, final Integer taskId)
  {
    if (taskId == null) {
      projekt.setTask(null);
    } else {
      final TaskDO task = taskDao.getOrLoad(taskId);
      projekt.setTask(task);
    }
  }

  public void setProjektManagerGroup(final ProjektDO projekt, final Integer groupId)
  {
    if (groupId == null) {
      projekt.setProjektManagerGroup(null);
    } else {
      final GroupDO group = groupDao.getOrLoad(groupId);
      projekt.setProjektManagerGroup(group);
    }
  }

  /**
   * Initializes the projekt (projektManagerGroup), so any LazyInitializationException are avoided.
   * 
   * @param projekt Null safe.
   */
  public void initializeProjektManagerGroup(final ProjektDO projekt)
  {
    if (projekt == null) {
      return;
    }
    // Needed because Hibernate Search rolls back because the project manager group is not loadable.
    Hibernate.initialize(projekt);
    final GroupDO projectManagerGroup = projekt.getProjektManagerGroup();
    if (projectManagerGroup != null) {
      final GroupDO group = groupDao.internalGetById(projectManagerGroup.getId());
      projekt.setProjektManagerGroup(group);
      //Hibernate.initialize(projectManagerGroup); // Does not work.
    }
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public ProjektDO getProjekt(final KundeDO kunde, final int nummer)
  {
    final List<ProjektDO> list = (List<ProjektDO>) getHibernateTemplate().find(
        "from ProjektDO p where p.kunde.id=? and p.nummer=?",
        new Object[] { kunde.getId(), nummer });
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list.get(0);
  }

  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public ProjektDO getProjekt(final int intern_kost2_4, final int nummer)
  {
    final List<ProjektDO> list = (List<ProjektDO>) getHibernateTemplate().find(
        "from ProjektDO p where p.internKost2_4=? and p.nummer=?",
        new Object[] { intern_kost2_4, nummer });
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list.get(0);
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<ProjektDO> getList(final BaseSearchFilter filter)
  {
    final ProjektFilter myFilter;
    if (filter instanceof ProjektFilter) {
      myFilter = (ProjektFilter) filter;
    } else {
      myFilter = new ProjektFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.isEnded() == true) {
      queryFilter.add(Restrictions.eq("status", ProjektStatus.ENDED));
    } else if (myFilter.isNotEnded() == true) {
      queryFilter.add(Restrictions.or(Restrictions.ne("status", ProjektStatus.ENDED), Restrictions.isNull("status")));
    }
    queryFilter.addOrder(Order.asc("internKost2_4")).addOrder(Order.asc("kunde.id")).addOrder(Order.asc("nummer"));
    return getList(queryFilter);
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<ProjektDO> getKundenProjekte(final Integer kundeId)
  {
    if (kundeId == null) {
      return null;
    }
    final QueryFilter queryFilter = new QueryFilter();
    queryFilter.add(Restrictions.eq("kunde.id", kundeId));
    queryFilter.addOrder(Order.asc("nummer"));
    return getList(queryFilter);
  }

  @Override
  protected void onSaveOrModify(final ProjektDO obj)
  {
    if (obj.getKunde() != null) {
      // Ein Kundenprojekt kann keine interne Kundennummer haben:
      obj.setInternKost2_4(null);
    }
    if (obj.getStatus() == ProjektStatus.NONE) {
      obj.setStatus(null);
    }
    super.onSaveOrModify(obj);
  }

  @Override
  protected void afterSaveOrModify(final ProjektDO projekt)
  {
    if (projekt.getTaskId() != null) {
      taskDao.getTaskTree().internalSetProject(projekt.getTaskId(), projekt);
    }
    super.afterSaveOrModify(projekt);
  }

  @Override
  protected void afterUpdate(final ProjektDO obj, final ProjektDO dbObj)
  {
    if (dbObj.getTaskId() != null && obj.getTaskId() == null) {
      // Project task was removed:
      taskDao.getTaskTree().internalSetProject(dbObj.getTaskId(), null);
    }
    super.afterUpdate(obj, dbObj);
  }

  @Override
  public ProjektDO newInstance()
  {
    return new ProjektDO();
  }
}
