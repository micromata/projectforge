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

package org.projectforge.business.fibu.kost;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.stereotype.Repository;

@Repository
public class BuchungssatzDao extends BaseDao<BuchungssatzDO>
{
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "kost1.nummer", "kost1.description",
      "kost2.nummer",
      "kost2.description", "kost2.comment", "kost2.projekt.name", "kost2.projekt.kunde.name", "konto.nummer",
      "gegenKonto.nummer" };

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public BuchungssatzDao()
  {
    super(BuchungssatzDO.class);
  }

  /**
   * List of all years witch BuchungssatzDO entries: select min(year), max(year) from t_fibu_buchungssatz.
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public int[] getYears()
  {
    final List<Object[]> list = getSession().createQuery("select min(year), max(year) from BuchungssatzDO t").list();
    if (list.size() == 0 || list.get(0) == null || list.get(0)[0] == null) {
      return new int[] { Calendar.getInstance().get(Calendar.YEAR) };
    }
    final int minYear = (Integer) list.get(0)[0];
    final int maxYear = (Integer) list.get(0)[1];
    if (minYear > maxYear || maxYear - minYear > 30) {
      throw new UnsupportedOperationException("Paranoia Exception");
    }
    final int[] res = new int[maxYear - minYear + 1];
    int i = 0;
    for (int year = maxYear; year >= minYear; year--) {
      res[i++] = year;
    }
    return res;
  }

  @SuppressWarnings("unchecked")
  public BuchungssatzDO getBuchungssatz(final int year, final int month, final int satznr)
  {
    final List<BuchungssatzDO> list = (List<BuchungssatzDO>) getHibernateTemplate().find(
        "from BuchungssatzDO satz where satz.year = ? and satz.month = ? and satz.satznr = ?",
        new Object[] { year, month, satznr });
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list.get(0);
  }

  public boolean validateTimeperiod(final BuchungssatzFilter myFilter)
  {
    final int toMonth = myFilter.getToMonth();
    final int toYear = myFilter.getToYear();
    if (toMonth >= 0 && toYear < 0 || toMonth < 0 && toYear > 0) {
      // toMonth given, but not toYear or vice versa.
      return false;
    }
    if (myFilter.getFromMonth() < 0) {
      // Kein Von-Monat gesetzt.
      if (toMonth >= 0 || toYear > 0) {
        return false;
      }
    } else if (toYear > 0) {
      // Zeitraum gesetzt
      if (myFilter.getFromYear() > toYear) {
        return false;
      }
      if (myFilter.getFromYear() == myFilter.getToYear()) {
        if (myFilter.getFromMonth() > myFilter.getToMonth()) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public List<BuchungssatzDO> getList(final BaseSearchFilter filter)
  {
    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP);
    final BuchungssatzFilter myFilter;
    if (filter instanceof BuchungssatzFilter) {
      myFilter = (BuchungssatzFilter) filter;
    } else {
      myFilter = new BuchungssatzFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(filter);
    if (validateTimeperiod(myFilter) == false) {
      throw new UserException("fibu.buchungssatz.error.invalidTimeperiod");
    }
    if (myFilter.getFromMonth() < 0) {
      // Kein Von-Monat gesetzt.
      queryFilter.add(Restrictions.eq("year", myFilter.getFromYear()));
      queryFilter.add(Restrictions.between("month", 0, 11));
    } else if (myFilter.getToYear() > 0) {
      if (myFilter.getFromYear() == myFilter.getToYear()) {
        queryFilter.add(Restrictions.eq("year", myFilter.getFromYear()));
        queryFilter.add(Restrictions.between("month", myFilter.getFromMonth(), myFilter.getToMonth()));
      } else {
        // between but different years
        queryFilter.add(Restrictions.disjunction().add(
            Restrictions.and(Restrictions.eq("year", myFilter.getFromYear()),
                Restrictions.ge("month", myFilter.getFromMonth())))
            .add(
                Restrictions.and(Restrictions.eq("year", myFilter.getToYear()),
                    Restrictions.le("month", myFilter.getToMonth())))
            .add(
                Restrictions.and(Restrictions.gt("year", myFilter.getFromYear()),
                    Restrictions.lt("year", myFilter.getToYear()))));
      }
    } else {
      // Nur Von-Monat gesetzt.
      queryFilter.add(Restrictions.eq("year", myFilter.getFromYear()));
      queryFilter.add(Restrictions.eq("month", myFilter.getFromMonth()));
    }
    queryFilter.addOrder(Order.asc("year")).addOrder(Order.asc("month")).addOrder(Order.asc("satznr"));
    final List<BuchungssatzDO> list = getList(queryFilter);
    return list;
  }

  /**
   * User must member of group finance or controlling.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(PFUserDO,
   *      org.projectforge.core.ExtendedBaseDO, boolean)
   * @see #hasSelectAccess(PFUserDO, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final BuchungssatzDO obj, final boolean throwException)
  {
    return hasSelectAccess(user, throwException);
  }

  /**
   * User must member of group finance.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final BuchungssatzDO obj, final BuchungssatzDO oldObj,
      final OperationType operationType, final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP);
  }

  @Override
  public BuchungssatzDO newInstance()
  {
    return new BuchungssatzDO();
  }
}
