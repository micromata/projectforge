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

import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.io.Serializable;
import java.util.List;

/**
 * Delegates to getNested() Service.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 * @param <O>
 */
public abstract class PersistenceServiceDelegate<O extends ExtendedBaseDO<Integer>>
    implements IPersistenceService<O>
{

  abstract public IPersistenceService<O> getNested();

  @Override
  public boolean hasInsertAccess(PFUserDO user)
  {
    return getNested().hasInsertAccess(user);
  }

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return getNested().hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(O obj, boolean throwException)
  {
    return getNested().hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public List<O> getList(BaseSearchFilter filter)
  {
    return getNested().getList(filter);
  }

  @Override
  public boolean isHistorizable()
  {
    return getNested().isHistorizable();
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(O obj, O dbObj, boolean throwException)
  {
    return getNested().hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(O obj, O dbObj, boolean throwException)
  {
    return getNested().hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, O obj, O dbObj, boolean throwException)
  {
    return getNested().hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public O getById(Serializable id) throws AccessException
  {
    return getNested().getById(id);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    return getNested().getAutocompletion(property, searchString);
  }

  @Override
  public Integer save(O obj) throws AccessException
  {
    return getNested().save(obj);
  }

  @Override
  public ModificationStatus update(O obj) throws AccessException
  {
    return getNested().update(obj);
  }

  @Override
  public void markAsDeleted(O obj) throws AccessException
  {
    getNested().markAsDeleted(obj);
  }

  @Override
  public void undelete(O obj) throws AccessException
  {
    getNested().undelete(obj);
  }

  @Override
  public void delete(O obj) throws AccessException
  {
    getNested().delete(obj);
  }

  @Override
  public O newInstance()
  {
    return getNested().newInstance();
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(O obj)
  {
    return getNested().getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    getNested().rebuildDatabaseIndex4NewestEntries();

  }

  @Override
  public void rebuildDatabaseIndex()
  {
    getNested().rebuildDatabaseIndex();
  }

}
