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

import java.io.Serializable;

import org.projectforge.framework.access.AccessException;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.MarkDeletableRecord;

/**
 * Stores genericly via JPA.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Deprecated
public interface JpaPfGenericPersistenceService
{
  Serializable insert(DbRecord<?> obj) throws AccessException;

  ModificationStatus update(DbRecord<?> obj) throws AccessException;

  /**
   * Object will be marked as deleted (boolean flag), therefore undelete is always possible without any loss of data.
   * 
   * @param obj
   */
  void markAsDeleted(MarkDeletableRecord<?> obj) throws AccessException;

  /**
   * Object will be marked as deleted (booelan flag), therefore undelete is always possible without any loss of data.
   * 
   * @param obj
   */
  void undelete(MarkDeletableRecord<?> obj) throws AccessException;

  IUserRightId getUserRight(BaseDO<?> baseDo);
}
