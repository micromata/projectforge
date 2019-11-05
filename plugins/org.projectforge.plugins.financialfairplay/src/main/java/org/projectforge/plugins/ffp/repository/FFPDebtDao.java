/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.ffp.repository;

import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.impl.DBPredicate;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.FinancialFairPlayPluginUserRightId;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.wicket.FFPDebtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to ffp events.
 * <p>
 * Buggy: Deselect plugin on startup: update t_configuration set stringValue='extendemployeedata,ihkexport,licenseManagementPlugin,liquidplanning,marketing,memo,skillmatrix,todo' where parameter='pluginsActivated';
 *
 * @author Florian Blumenstein
 */
@Repository
public class FFPDebtDao extends BaseDao<FFPDebtDO> {
  @Autowired
  private PfEmgrFactory emgrFactory;

  public FFPDebtDao() {
    super(FFPDebtDO.class);
    userRightId = FinancialFairPlayPluginUserRightId.PLUGIN_FINANCIALFAIRPLAY;
  }

  @Override
  public FFPDebtDO newInstance() {
    return new FFPDebtDO();
  }

  public List<FFPDebtDO> getDebtList(PFUserDO user) {
    List<FFPDebtDO> debtList = emgrFactory.runRoTrans(emgr ->
            emgr.select(FFPDebtDO.class, "SELECT d FROM FFPDebtDO d WHERE d.from = :from OR d.to = :to", "from", user, "to", user)
    );
    debtList.removeIf(this::checkRemoveUser);
    return debtList;
  }

  public Integer getOpenFromDebts(PFUserDO user) {
    Integer result = 0;
    List<FFPDebtDO> debtList = getDebtList(user);
    for (FFPDebtDO debt : debtList) {
      if (debt.getFrom().equals(user) && !debt.isApprovedByFrom()) {
        result++;
      }
    }
    return result;
  }

  public Integer getOpenToDebts(PFUserDO user) {
    Integer result = 0;
    List<FFPDebtDO> debtList = getDebtList(user);
    for (FFPDebtDO debt : debtList) {
      if (debt.getTo().equals(user) && debt.isApprovedByFrom() && !debt.isApprovedByTo()) {
        result++;
      }
    }
    return result;
  }

  public List<FFPDebtDO> getDebts(FFPEventDO event) {
    return emgrFactory.runRoTrans(emgr -> {
      return emgr.select(FFPDebtDO.class, "SELECT d FROM FFPDebtDO d WHERE d.event = :event", "event", event);
    });
  }

  @Override
  public List<FFPDebtDO> getList(final BaseSearchFilter filter) {
    final FFPDebtFilter myFilter;
    if (filter instanceof FFPDebtFilter) {
      myFilter = (FFPDebtFilter) filter;
    } else {
      myFilter = new FFPDebtFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(filter);
    PFUserDO userFromFilter = emgrFactory.runRoTrans(emgr -> emgr.selectByPk(PFUserDO.class, myFilter.getUserId()));
    queryFilter.add(QueryFilter.or(
            QueryFilter.eq("from", userFromFilter),
            QueryFilter.eq("to", userFromFilter)));
    if (myFilter.isFromMe()) {
      queryFilter.add(QueryFilter.eq("from", userFromFilter));
    }
    if (myFilter.isToMe()) {
      queryFilter.add(QueryFilter.eq("to", userFromFilter));
    }
    if (myFilter.isiNeedToApprove()) {
      DBPredicate fromMe = QueryFilter.and(//
              QueryFilter.eq("from", userFromFilter), //
              QueryFilter.eq("approvedByFrom", false));
      DBPredicate toMe = QueryFilter.and(//
              QueryFilter.eq("to", userFromFilter), //
              QueryFilter.eq("approvedByTo", false));
      queryFilter.add(QueryFilter.or(fromMe, toMe));
    }
    if (myFilter.isHideBothApproved()) {
      DBPredicate notApprovedByFrom = QueryFilter.eq("approvedByFrom", false);
      DBPredicate notApprovedByTo = QueryFilter.eq("approvedByTo", false);
      queryFilter.add(QueryFilter.or(notApprovedByFrom, notApprovedByTo));
    }
    List<FFPDebtDO> debtList = getList(queryFilter);
    debtList.removeIf(this::checkRemoveUser);
    return debtList;
  }

  private boolean checkRemoveUser(final FFPDebtDO debt) {
    return debt.getFrom().getDeactivated()
            || debt.getFrom().isDeleted()
            || debt.getTo().getDeactivated()
            || debt.getTo().isDeleted();
  }
}
