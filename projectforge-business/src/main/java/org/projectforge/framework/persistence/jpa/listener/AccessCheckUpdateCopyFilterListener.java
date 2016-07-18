package org.projectforge.framework.persistence.jpa.listener;

import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
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
    IUserRightId rightId = genericPersistenceService.getUserRight(dbObject);
    accessChecker.hasLoggedInUserAccess(rightId, newObj, dbObject, opType, true);

  }
}
