package org.projectforge.plugins.ffp.repository;

import java.util.List;

import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.FinancialFairPlayPluginUserRightId;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.wicket.FFPDebtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Access to ffp events.
 *
 * @author Florian Blumenstein
 */
@Repository
public class FFPDebtDao extends BaseDao<FFPDebtDO>
{
  @Autowired
  private PfEmgrFactory emgrFactory;

  public FFPDebtDao()
  {
    super(FFPDebtDO.class);
    userRightId = FinancialFairPlayPluginUserRightId.PLUGIN_FINANCIALFAIRPLAY;
  }

  @Override
  public FFPDebtDO newInstance()
  {
    return new FFPDebtDO();
  }

  public List<FFPDebtDO> getDebtList(PFUserDO user)
  {
    return emgrFactory.runRoTrans(emgr -> 
      emgr.select(FFPDebtDO.class, "SELECT d FROM FFPDebtDO d WHERE d.from = :from OR d.to = :to", "from", user, "to", user)
    );
  }

  public Integer getOpenFromDebts(PFUserDO user)
  {
    Integer result = 0;
    List<FFPDebtDO> debtList = getDebtList(user);
    for (FFPDebtDO debt : debtList) {
      if (debt.getFrom().equals(user) && debt.isApprovedByFrom() == false) {
        result++;
      }
    }
    return result;
  }

  public Integer getOpenToDebts(PFUserDO user)
  {
    Integer result = 0;
    List<FFPDebtDO> debtList = getDebtList(user);
    for (FFPDebtDO debt : debtList) {
      if (debt.getTo().equals(user) && debt.isApprovedByFrom() == true && debt.isApprovedByTo() == false) {
        result++;
      }
    }
    return result;
  }

  public List<FFPDebtDO> getDebts(FFPEventDO event)
  {
    return emgrFactory.runRoTrans(emgr -> {
      return emgr.select(FFPDebtDO.class, "SELECT d FROM FFPDebtDO d WHERE d.event = :event", "event", event);
    });
  }

  @Override
  public List<FFPDebtDO> getList(final BaseSearchFilter filter)
  {
    final FFPDebtFilter myFilter;
    if (filter instanceof FFPDebtFilter) {
      myFilter = (FFPDebtFilter) filter;
    } else {
      myFilter = new FFPDebtFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(filter);
    PFUserDO userFromFilter = emgrFactory.runRoTrans(emgr -> emgr.selectByPk(PFUserDO.class, myFilter.getUserId()));
    queryFilter.add(Restrictions.or(
        Restrictions.eq("from", userFromFilter),
        Restrictions.eq("to", userFromFilter)));
    if (myFilter.isFromMe()) {
      queryFilter.add(Restrictions.eq("from", userFromFilter));
    }
    if (myFilter.isToMe()) {
      queryFilter.add(Restrictions.eq("to", userFromFilter));
    }
    if (myFilter.isiNeedToApprove()) {
        LogicalExpression fromMe = Restrictions.and(//
            Restrictions.eq("from", userFromFilter), //
            Restrictions.eq("approvedByFrom", false));
        LogicalExpression toMe = Restrictions.and(//
            Restrictions.eq("to", userFromFilter), //
            Restrictions.eq("approvedByTo", false));
        queryFilter.add(Restrictions.or(fromMe, toMe));
      }
    if (myFilter.isHideBothApproved()) {
        SimpleExpression notApprovedByFrom = Restrictions.eq("approvedByFrom", false);
        SimpleExpression notApprovedByTo = Restrictions.eq("approvedByTo", false);
        queryFilter.add(Restrictions.or(notApprovedByFrom, notApprovedByTo));
      }
    return getList(queryFilter);
  }
}
