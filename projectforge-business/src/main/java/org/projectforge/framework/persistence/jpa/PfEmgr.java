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

package org.projectforge.framework.persistence.jpa;

import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.hibernate.Session;

import de.micromata.genome.jpa.EmgrTx;
import de.micromata.genome.jpa.EntityCopyStatus;
import de.micromata.genome.jpa.MarkDeletableRecord;
import de.micromata.genome.jpa.events.EmgrBeforeDeleteEvent;
import de.micromata.genome.jpa.events.EmgrInitForInsertEvent;
import de.micromata.mgc.jpa.hibernatesearch.api.SearchEmgrFactory;
import de.micromata.mgc.jpa.hibernatesearch.impl.SearchEmgr;

/**
 * ProjectForge entity Manager, with modified behaivour.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class PfEmgr extends SearchEmgr<PfEmgr>
{

  /**
   * The check access.
   */
  private boolean checkAccess = true;

  /**
   * Instantiates a new pf emgr.
   *
   * @param entityManager the entity manager
   * @param emgrFactory the emgr factory
   */
  public PfEmgr(EntityManager entityManager, SearchEmgrFactory<PfEmgr> emgrFactory, EmgrTx<PfEmgr> emgrTx)
  {
    super(entityManager, emgrFactory, emgrTx);
  }

  /**
   * Disable for a code block the access check.
   *
   * @param <T> the generic type
   * @param func the func
   * @return the t
   */
  public <T> T runInternal(Supplier<T> func)
  {
    boolean prev = checkAccess;
    try {
      checkAccess = false;
      return func.get();
    } finally {
      checkAccess = prev;
    }
  }

  /**
   * Pf has a difference semantic on mark delete. It has to update {@inheritDoc}
   *
   * Different to Pf, check also for update right, because entity changes should also be persisted.
   */
  @Override
  public <T extends MarkDeletableRecord<?>> boolean markDeleted(T newE)
  {
    newE.setDeleted(true);
    invokeEvent(new EmgrBeforeDeleteEvent(this, newE));
    EntityCopyStatus status = update(newE.getClass(), newE.getClass(), newE, true);
    return status != EntityCopyStatus.NONE;

  }

  /**
   * Different to Pf, check also for update right, because entity changes should also be persisted. {@inheritDoc}
   *
   */
  @Override
  public <T extends MarkDeletableRecord<?>> boolean markUndeleted(T newE)
  {
    newE.setDeleted(false);
    invokeEvent(new EmgrInitForInsertEvent(this, newE));
    EntityCopyStatus status = update(newE.getClass(), newE.getClass(), newE, true);
    return status != EntityCopyStatus.NONE;
  }

  /**
   * For backward compatility.
   * 
   * @return
   */
  public Session getSession()
  {
    return getEntityManager().unwrap(Session.class);
  }

  /**
   * Checks if is check access.
   *
   * @return true, if is check access
   */
  public boolean isCheckAccess()
  {
    return checkAccess;
  }

  /**
   * Sets the check access.
   *
   * @param checkAccess the new check access
   */
  public void setCheckAccess(boolean checkAccess)
  {
    this.checkAccess = checkAccess;
  }

}
