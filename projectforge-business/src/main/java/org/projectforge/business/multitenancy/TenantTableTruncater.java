package org.projectforge.business.multitenancy;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

import de.micromata.genome.jpa.IEmgr;
import de.micromata.genome.jpa.impl.TableTruncater;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class TenantTableTruncater implements TableTruncater
{

  @Override
  public int truncateTable(IEmgr<?> emgr, EntityMetadata entity)
  {
    EntityManager entityManager = emgr.getEntityManager();
    List<TenantDO> tenants = entityManager
        .createQuery("select e from " + TenantDO.class.getName() + " e", TenantDO.class).getResultList();
    for (TenantDO t : tenants) {
      Set<PFUserDO> assignedUser = t.getAssignedUsers();
      if (assignedUser != null) {
        assignedUser.clear();
      }
      entityManager.persist(t);
      entityManager.remove(t);
    }
    return tenants.size();
  }

}
