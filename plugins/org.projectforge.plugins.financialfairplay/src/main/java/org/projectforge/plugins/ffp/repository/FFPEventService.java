package org.projectforge.plugins.ffp.repository;

import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.plugins.ffp.model.FFPEventDO;

/**
 * Access to ffp event.
 * 
 * @author Florian Blumenstein
 *
 */
public interface FFPEventService extends IPersistenceService<FFPEventDO>, IDao<FFPEventDO>
{

}
