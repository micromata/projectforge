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

package org.projectforge.business.fibu;

import org.hibernate.Hibernate;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import java.util.List;

@Repository
public class ProjektDao extends BaseDao<ProjektDO> {
  public static final UserRightId USER_RIGHT_ID = UserRightId.PM_PROJECT;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjektDao.class);
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"kunde.name", "kunde.division", "projektManagerGroup.name"};

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private PfEmgrFactory emgrFactory;

  public ProjektDao() {
    super(ProjektDO.class);
    this.supportAfterUpdate = true;
    userRightId = USER_RIGHT_ID;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * @param projekt
   * @param kundeId If null, then kunde will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKunde(final ProjektDO projekt, final Integer kundeId) {
    if (kundeId == null) {
      projekt.setKunde(null);
    } else {
      final KundeDO kunde = kundeDao.getOrLoad(kundeId);
      projekt.setKunde(kunde);
    }
  }

  /**
   * @param projekt
   * @param taskId  If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setTask(final ProjektDO projekt, final Integer taskId) {
    if (taskId == null) {
      projekt.setTask(null);
    } else {
      final TaskDO task = taskDao.getOrLoad(taskId);
      projekt.setTask(task);
    }
  }

  public void setProjektManagerGroup(final ProjektDO projekt, final Integer groupId) {
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
  public void initializeProjektManagerGroup(final ProjektDO projekt) {
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
  public ProjektDO getProjekt(final KundeDO kunde, final int nummer) {
    return emgrFactory.runRoTrans(emgr -> {
      try {
        return emgr
                .selectSingleAttached(ProjektDO.class, "SELECT p FROM ProjektDO p WHERE p.kunde = :kunde and p.nummer = :nummer", "kunde", kunde, "nummer", nummer);
      } catch (NoResultException e) {
        return null;
      }
    });
  }

  @SuppressWarnings("unchecked")
  public ProjektDO getProjekt(final int intern_kost2_4, final int nummer) {
    return SQLHelper.ensureUniqueResult(em
            .createNamedQuery(ProjektDO.FIND_BY_INTERNKOST24_AND_NUMMER, ProjektDO.class)
            .setParameter("internKost24", intern_kost2_4)
            .setParameter("nummer", nummer));
  }

  @Override
  public List<ProjektDO> getList(final BaseSearchFilter filter) {
    final ProjektFilter myFilter;
    if (filter instanceof ProjektFilter) {
      myFilter = (ProjektFilter) filter;
    } else {
      myFilter = new ProjektFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.isEnded()) {
      queryFilter.add(QueryFilter.eq("status", ProjektStatus.ENDED));
    } else if (myFilter.isNotEnded()) {
      queryFilter.add(QueryFilter.or(QueryFilter.ne("status", ProjektStatus.ENDED), QueryFilter.isNull("status")));
    }
    queryFilter.addOrder(SortProperty.asc("internKost2_4")).addOrder(SortProperty.asc("kunde.nummer")).addOrder(SortProperty.asc("nummer"));
    return getList(queryFilter);
  }

  public List<ProjektDO> getKundenProjekte(final Integer kundeId) {
    if (kundeId == null) {
      return null;
    }
    final QueryFilter queryFilter = new QueryFilter();
    queryFilter.add(QueryFilter.eq("kunde.id", kundeId));
    queryFilter.addOrder(SortProperty.asc("nummer"));
    return getList(queryFilter);
  }

  @Override
  protected void onSaveOrModify(final ProjektDO obj) {
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
  protected void afterSaveOrModify(final ProjektDO projekt) {
    if (projekt.getTaskId() != null) {
      taskDao.getTaskTree().internalSetProject(projekt.getTaskId(), projekt);
    }
    super.afterSaveOrModify(projekt);
  }

  @Override
  protected void afterUpdate(final ProjektDO obj, final ProjektDO dbObj) {
    if (dbObj.getTaskId() != null && obj.getTaskId() == null) {
      // Project task was removed:
      taskDao.getTaskTree().internalSetProject(dbObj.getTaskId(), null);
    }
    super.afterUpdate(obj, dbObj);
  }

  @Override
  public ProjektDO newInstance() {
    return new ProjektDO();
  }

}
