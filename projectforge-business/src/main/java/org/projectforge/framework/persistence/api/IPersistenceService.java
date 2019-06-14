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

package org.projectforge.framework.persistence.api;

import java.util.List;

import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.database.DatabaseDao;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Legacy Persistence Service.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 * @param <O>
 */
public interface IPersistenceService<O extends ExtendedBaseDO<Integer>>
    extends IDao<O>, ICorePersistenceService<Integer, O>
{

  @Override
  public boolean hasInsertAccess(final PFUserDO user);

  /**
   * Checks write access of the readWriteUserRight. If not given, true is returned at default. This method should only
   * be used for checking the insert access to show an insert button or not. Before inserting any object the write
   * access is checked by has*Access(...) independent of the result of this method.
   * 
   * @see org.projectforge.framework.persistence.api.IDao#hasLoggedInUserInsertAccess()
   */
  public boolean hasLoggedInUserInsertAccess();

  /**
   * Checks insert access right by calling hasAccess(obj, OperationType.INSERT).
   * 
   * @param obj Check access to this object.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  boolean hasLoggedInUserInsertAccess(final O obj, final boolean throwException);

  /**
   * Checks update access right by calling hasAccess(obj, OperationType.UPDATE).
   * 
   * @param dbObj The original object (stored in the database)
   * @param obj Check access to this object.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  public boolean hasLoggedInUserUpdateAccess(final O obj, final O dbObj, final boolean throwException);

  /**
   * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
   * 
   * @param obj Check access to this object.
   * @param dbObj current version of this object in the data base.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  public boolean hasLoggedInUserDeleteAccess(final O obj, final O dbObj, final boolean throwException);

  /**
   * Checks delete access right by calling hasAccess(obj, OperationType.DELETE).
   * 
   * @param obj Check access to this object.
   * @param dbObj current version of this object in the data base.
   * @return
   * @see #hasAccess(Object, OperationType)
   */
  public boolean hasDeleteAccess(final PFUserDO user, final O obj, final O dbObj, final boolean throwException);

  /**
   * Only generic check access will be done. The matching entries will not be checked!
   * 
   * @param property Property of the data base entity.
   * @param searchString String the user has typed in.
   * @return All matching entries (like search) for the given property modified or updated in the last 2 years.
   */
  List<String> getAutocompletion(final String property, final String searchString);

  /**
   * Gets the history entries of the object in flat format.<br/>
   * Please note: If user has no access an empty list will be returned.
   * 
   * @param id The id of the object.
   * @return
   */
  List<DisplayHistoryEntry> getDisplayHistoryEntries(final O obj);

  /**
   * Re-indexes the entries of the last day, 1,000 at max.
   * 
   * @see DatabaseDao#createReindexSettings(boolean)
   */
  void rebuildDatabaseIndex4NewestEntries();

  /**
   * Re-indexes all entries (full re-index).
   */
  void rebuildDatabaseIndex();
}
