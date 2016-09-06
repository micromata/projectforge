package org.projectforge.plugins.ffp.service;

import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.plugins.ffp.model.FFPEventDO;

public interface FinancialFairPlayService
    extends IPersistenceService<FFPEventDO>, IDao<FFPEventDO>
{

}
