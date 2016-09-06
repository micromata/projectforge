package org.projectforge.plugins.ffp.repository;

import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.springframework.stereotype.Repository;

/**
 * Access to ffp events.
 * 
 * @author Florian Blumenstein
 *
 */
@Repository
public class FFPEventDao extends BaseDao<FFPEventDO>
{
  public FFPEventDao()
  {
    super(FFPEventDO.class);
  }

  @Override
  public FFPEventDO newInstance()
  {
    return new FFPEventDO();
  }
}
