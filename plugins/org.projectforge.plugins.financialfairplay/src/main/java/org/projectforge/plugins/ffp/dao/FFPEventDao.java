package org.projectforge.plugins.ffp.dao;

import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.plugins.ffp.model.FFPEventDO;

/**
 * Access to ffp events.
 * 
 * @author Florian Blumenstein
 *
 */
public interface FFPEventDao extends IPersistenceService<FFPEventDO>, IDao<FFPEventDO>
{

}
