package org.projectforge.plugins.ffp.repository;

import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Access to ffp events.
 *
 * @author Florian Blumenstein
 */
@Repository
public class FFPEventDao extends BaseDao<FFPEventDO>
{
  @Autowired
  private PfEmgrFactory emgrFactory;

  public FFPEventDao()
  {
    super(FFPEventDO.class);
  }

  @Override
  public FFPEventDO newInstance()
  {
    return new FFPEventDO();
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final FFPEventDO obj, final FFPEventDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return true;
  }
}
