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

package org.projectforge.plugins.liquidityplanning;

import org.projectforge.business.fibu.AmountType;
import org.projectforge.business.fibu.PaymentStatus;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.time.DayHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Repository
public class LiquidityEntryDao extends BaseDao<LiquidityEntryDO>
{
  public LiquidityEntryDao()
  {
    super(LiquidityEntryDO.class);
    userRightId = LiquidityplanningPluginUserRightId.PLUGIN_LIQUIDITY_PLANNING;
  }

  public LiquidityEntriesStatistics buildStatistics(final List<LiquidityEntryDO> list)
  {
    final LiquidityEntriesStatistics stats = new LiquidityEntriesStatistics();
    if (list == null) {
      return stats;
    }
    for (final LiquidityEntryDO entry : list) {
      stats.add(entry);
    }
    return stats;
  }

  @Override
  public List<LiquidityEntryDO> getList(final BaseSearchFilter filter)
  {
    final LiquidityFilter myFilter;
    if (filter instanceof LiquidityFilter) {
      myFilter = (LiquidityFilter) filter;
    } else {
      myFilter = new LiquidityFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final List<LiquidityEntryDO> list = getList(queryFilter);
    if (myFilter.getPaymentStatus() == PaymentStatus.ALL
        && myFilter.getAmountType() == AmountType.ALL
        && myFilter.getNextDays() <= 0
        || myFilter.isDeleted()) {
      return list;
    }
    final List<LiquidityEntryDO> result = new ArrayList<>();
    final DayHolder today = new DayHolder();
    for (final LiquidityEntryDO entry : list) {
      if (myFilter.getPaymentStatus() == PaymentStatus.PAID && !entry.getPaid()) {
        continue;
      }
      if (myFilter.getPaymentStatus() == PaymentStatus.UNPAID && entry.getPaid()) {
        continue;
      }
      if (entry.getAmount() != null) {
        if (myFilter.getAmountType() == AmountType.CREDIT && entry.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
          continue;
        }
        if (myFilter.getAmountType() == AmountType.DEBIT && entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
          continue;
        }
      }
      if (myFilter.getNextDays() > 0) {
        Date dateOfPayment = entry.getDateOfPayment();
        if (dateOfPayment == null) {
          dateOfPayment = today.getSQLDate();
        }
        if (dateOfPayment.before(today.getDate())) {
          // Entry is before today:
          if (myFilter.getPaymentStatus() == PaymentStatus.PAID || entry.getPaid()) {
            // Ignore entries of the past if they were paid. Also ignore unpaid entries of the past if the user wants to filter only paid
            // entries.
            continue;
          }
        } else {
          if (today.daysBetween(entry.getDateOfPayment()) > myFilter.getNextDays()) {
            continue;
          }
        }
      }
      result.add(entry);
    }
    return result;
  }

  @Override
  public LiquidityEntryDO newInstance()
  {
    return new LiquidityEntryDO();
  }
}
