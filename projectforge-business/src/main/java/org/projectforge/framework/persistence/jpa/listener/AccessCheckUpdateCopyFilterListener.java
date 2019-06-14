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

package org.projectforge.framework.persistence.jpa.listener;

import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.JpaPfGenericPersistenceService;
import org.projectforge.framework.persistence.jpa.PfEmgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.events.EmgrEventHandler;
import de.micromata.genome.jpa.events.EmgrUpdateCopyFilterEvent;

/**
 * Checks if db object can be updated. For other, see BeforeModifyEventHandler.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Component
public class AccessCheckUpdateCopyFilterListener implements EmgrEventHandler<EmgrUpdateCopyFilterEvent>
{
  @Autowired
  protected AccessChecker accessChecker;

  @Autowired
  private JpaPfGenericPersistenceService genericPersistenceService;

  @Autowired
  TenantChecker tenantChecker;

  @Override
  public void onEvent(EmgrUpdateCopyFilterEvent event)
  {
    DbRecord<?> obj = event.getTarget();
    if ((obj instanceof BaseDO) == false) {
      return;
    }
    PfEmgr emgr = (PfEmgr) event.getEmgr();
    if (emgr.isCheckAccess() == false) {
      event.nextFilter();
      return;
    }
    BaseDO<?> dbObject = (BaseDO<?>) event.getTarget();
    Object newObj = event.getSource();
    checkEntity(genericPersistenceService, accessChecker, tenantChecker, dbObject, newObj, OperationType.UPDATE);

    event.nextFilter();
  }

  public static void checkEntity(JpaPfGenericPersistenceService genericPersistenceService, AccessChecker accessChecker,
      TenantChecker tenantChecker, BaseDO<?> dbObject, Object newObj, OperationType opType)
  {
    accessChecker.checkRestrictedOrDemoUser();
    tenantChecker.checkPartOfCurrentTenant(dbObject);
    AUserRightId aUserRightId = dbObject.getClass().getAnnotation(AUserRightId.class);
    if (aUserRightId != null && aUserRightId.checkAccess() == false) {
      return;
    }
    IUserRightId rightId = genericPersistenceService.getUserRight(dbObject);
    accessChecker.hasLoggedInUserAccess(rightId, newObj, dbObject, opType, true);
  }
}
