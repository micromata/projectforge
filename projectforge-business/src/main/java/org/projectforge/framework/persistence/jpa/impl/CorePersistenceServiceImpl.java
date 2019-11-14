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

package org.projectforge.framework.persistence.jpa.impl;

import de.micromata.genome.jpa.EntityCopyStatus;
import de.micromata.genome.jpa.MarkDeletableRecord;
import de.micromata.genome.util.bean.PrivateBeanUtils;
import de.micromata.genome.util.runtime.ClassUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 * @param <O>
 */
public class CorePersistenceServiceImpl<PK extends Serializable, ENT extends MarkDeletableRecord<PK>>
    implements ICorePersistenceService<PK, ENT>, IDao<ENT>
{

  private static final Logger LOG = LoggerFactory.getLogger(JpaPfPersistenceServiceImpl.class);
  @Autowired
  private PfEmgrFactory emf;

  @Autowired
  UserRightService userRights;

  @Autowired
  protected AccessChecker accessChecker;

  private Class<ENT> entityClass;

  @Override
  public Class<ENT> getEntityClass()
  {
    if (entityClass != null) {
      return entityClass;
    }
    entityClass = (Class<ENT>) ClassUtils.getGenericTypeArgument(getClass(), 1);
    return entityClass;
  }

  @Override
  @Deprecated
  public ENT getById(final Serializable id) throws AccessException
  {
    return selectByPkDetached((PK) id);
  }

  @Override
  public ENT newInstance()
  {
    return PrivateBeanUtils.createInstance(getEntityClass());
  }

  @Override
  public ENT selectByPkDetached(PK pk) throws AccessException
  {
    return emf.runInTrans((emgr) -> {
      return emgr.selectByPkDetached(getEntityClass(), pk);
    });

  }

  @Override
  public PK insert(ENT obj) throws AccessException
  {
    Validate.notNull(obj);
    emf.runInTrans((emgr) -> {
      emgr.insertDetached(obj);
      return obj.getPk();
    });

    return obj.getPk();
  }

  @Override
  public ModificationStatus update(ENT obj,String... ignoreCopyFields) {
    EntityCopyStatus mod = emf.runInTrans((emgr -> {
      return emgr.update(obj.getClass(),obj.getClass(), obj, true, ignoreCopyFields);

    }));
    return ModificationStatus.fromEntityCopyStatus(mod);
  }

  @Override
  public ModificationStatus update(ENT obj) throws AccessException
  {
    EntityCopyStatus mod = emf.runInTrans((emgr) -> {
      return emgr.update(obj.getClass(), obj.getClass(), obj, true);
    });
    return ModificationStatus.fromEntityCopyStatus(mod);
  }

  @Override
  public void markAsDeleted(ENT rec) throws AccessException
  {
    // special Pf handling see PfEmgr
    emf.runInTrans((emgr) -> emgr.markDeleted(rec));
  }

  @Override
  public void undelete(ENT obj) throws AccessException
  {
    // special Pf handling see PfEmgr
    emf.runInTrans((emgr) -> emgr.markUndeleted(obj));

  }

  @Override
  public void delete(ENT obj) throws AccessException
  {
    emf.runInTrans((emgr) -> {
      //TODO: RK Add boolean to fix comilable
      emgr.deleteDetached(obj, false);
      return null;
    });
  }

  @Override
  public List<ENT> getList(BaseSearchFilter filter)
  {
    LOG.error(
        "Not implemented yet: CorePersistenceServiceImpl.getList(BaseSearchFilter)");
    // TODO Auto-generated method stub
    return Collections.emptyList();
  }

  @Override
  public boolean isHistorizable()
  {
    return HistoryBaseDaoAdapter.isHistorizable(getEntityClass());
  }

  @Override
  public boolean hasInsertAccess(PFUserDO user)
  {
    Class<ENT> clz = getEntityClass();
    AUserRightId ur = clz.getAnnotation(AUserRightId.class);
    if (ur == null) {
      throw new IllegalArgumentException("Class " + clz.getName() + " missing EntityUserRightId annotation");
    }
    IUserRightId urid = userRights.getRightId(ur.value());
    return accessChecker.hasAccess(user, urid, null, null, OperationType.INSERT, false);
  }

}
