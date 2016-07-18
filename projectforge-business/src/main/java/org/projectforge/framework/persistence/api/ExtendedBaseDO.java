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

package org.projectforge.framework.persistence.api;

import java.io.Serializable;
import java.util.Date;

import de.micromata.genome.jpa.MarkDeletableRecord;

/**
 * Extends BaseDO: Supports extended functionalities: deleted, created and lastUpdate.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public interface ExtendedBaseDO<I extends Serializable>extends BaseDO<I>, MarkDeletableRecord<I>
{
  /**
   * If any re-calculations have to be done before displaying, indexing etc. Such re-calculations are use-full for e. g.
   * transient fields calculated from persistent fields.
   */
  public void recalculate();

  @Override
  public boolean isDeleted();

  @Override
  public void setDeleted(boolean deleted);

  public Date getCreated();

  public void setCreated(Date created);

  public void setCreated();

  /**
   * 
   * Last update will be modified automatically for every update of the database object.
   * 
   * @return
   */
  public Date getLastUpdate();

  public void setLastUpdate(Date lastUpdate);

  public void setLastUpdate();
}
