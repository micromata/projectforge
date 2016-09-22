package org.projectforge.framework.persistence.jpa.listener;

import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.events.EmgrEventHandler;
import de.micromata.genome.jpa.events.EmgrInitForInsertEvent;

/**
 * Bookkeeping of created and lastupdate fields.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Component
public class InitForInsertEventListener implements EmgrEventHandler<EmgrInitForInsertEvent>
{
  @Autowired
  private TenantService tenantService;

  @Override
  public void onEvent(EmgrInitForInsertEvent event)
  {
    DbRecord<?> rec = event.getRecord();
    if ((rec instanceof ExtendedBaseDO) == false) {
      return;
    }
    ExtendedBaseDO extb = (ExtendedBaseDO) rec;
    extb.setCreated();
    extb.setLastUpdate();
    if (extb.getTenant() == null) {
      //TODO FB: Is this the correct tenant?
      TenantDO tenant = ThreadLocalUserContext.getUser().getTenant();
      if (tenant == null) {
        tenant = tenantService.getDefaultTenant();
      }
      extb.setTenant(tenant);
    }
  }

}
