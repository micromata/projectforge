package org.projectforge.plugins.ffp.repository;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.wicket.FFPEventFilter;
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
  @Override
	public List<FFPEventDO> getList(BaseSearchFilter filter) {
    final FFPEventFilter myFilter;
    if (filter instanceof FFPEventFilter) {
      myFilter = (FFPEventFilter) filter;
    } else {
      myFilter = new FFPEventFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(filter);
    if (myFilter.isShowOnlyActiveEntries()) {
      queryFilter.add(Restrictions.eq("finished", false));
    }
    return getList(queryFilter);
  }
}
