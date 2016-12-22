package org.projectforge.business.orga;

import java.util.Collection;

import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;

public interface VisitorbookService extends IPersistenceService<VisitorbookDO>, IDao<VisitorbookDO>
{
  Collection<Integer> getAssignedContactPersonsIds(VisitorbookDO data);

  VisitorbookTimedDO addNewTimeAttributeRow(final VisitorbookDO visitor, final String groupName);

}
