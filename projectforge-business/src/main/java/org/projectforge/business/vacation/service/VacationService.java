package org.projectforge.business.vacation.service;

import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;

/**
 * Access to vacation.
 * 
 * @author Florian Blumenstein
 *
 */
public interface VacationService extends IPersistenceService<VacationDO>, IDao<VacationDO>
{

}
