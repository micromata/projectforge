package org.projectforge.plugins.ffp.repository;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
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

  public List<FFPDebtDO> getDebtList(EmployeeDO employee)
  {
    return emgrFactory.runRoTrans(emgr -> {
      return emgr.select(FFPDebtDO.class, "SELECT d FROM FFPDebtDO d WHERE d.from = :from OR d.to = :to", "from", employee, "to", employee);
    });
  }

  public Integer getOpenFromDebts(EmployeeDO employee)
  {
    Integer result = 0;
    List<FFPDebtDO> debtList = getDebtList(employee);
    for (FFPDebtDO debt : debtList) {
      if (debt.getFrom().equals(employee) && debt.isApprovedByFrom() == false) {
        result++;
      }
    }
    return result;
  }

  public Integer getOpenToDebts(EmployeeDO employee)
  {
    Integer result = 0;
    List<FFPDebtDO> debtList = getDebtList(employee);
    for (FFPDebtDO debt : debtList) {
      if (debt.getTo().equals(employee) && debt.isApprovedByFrom() == true && debt.isApprovedByTo() == false) {
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
    EmployeeDO employeeFromFilter = emgrFactory.runRoTrans(emgr -> emgr.selectByPk(EmployeeDO.class, myFilter.getEmployeeId()));
    queryFilter.add(Restrictions.or(
        Restrictions.eq("from", employeeFromFilter),
        Restrictions.eq("to", employeeFromFilter)));
    return getList(queryFilter);
  }
}
