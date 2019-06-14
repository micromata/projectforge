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

package org.projectforge.framework.persistence.api;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.DateHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Stores the expressions and settings for creating a hibernate criteria object. This template is useful for avoiding
 * the need of a hibernate session in the stripes action classes.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class QueryFilter {
  private final List<Object> filterSettings = new ArrayList<Object>();

  private String name;

  private String alias;

  private BaseSearchFilter filter;

  private FetchMode fetchMode;

  private String associationPath = null;

  private Locale locale;

  /**
   * Creates new QueryFilter with a new SearchFilter as filter.
   */
  public QueryFilter() {
    this.filter = new BaseSearchFilter();
  }

  public QueryFilter(final BaseSearchFilter filter) {
    this(filter, false);
  }

  /**
   * @param filter
   * @param ignoreTenant default is false.
   */
  public QueryFilter(final BaseSearchFilter filter, final boolean ignoreTenant) {
    if (filter == null) {
      this.filter = new BaseSearchFilter();
    } else {
      this.filter = filter;
    }
    TenantService tenantService = ApplicationContextProvider.getApplicationContext().getBean(TenantService.class);
    if (ignoreTenant == false && tenantService.isMultiTenancyAvailable() == true) {
      final UserContext userContext = ThreadLocalUserContext.getUserContext();
      final TenantDO currentTenant = userContext.getCurrentTenant();
      if (currentTenant != null) {
        if (currentTenant.isDefault() == true) {
          this.add(Restrictions.or(Restrictions.eq("tenant", userContext.getCurrentTenant()),
                  Restrictions.isNull("tenant")));
        } else {
          this.add(Restrictions.eq("tenant", userContext.getCurrentTenant()));
        }
      }
    }
  }

  private QueryFilter(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getAlias() {
    return alias;
  }

  public BaseSearchFilter getFilter() {
    return filter;
  }

  /**
   * Locale is needed for lucene stemmers (hibernate search).
   *
   * @return
   */
  public Locale getLocale() {
    if (locale == null) {
      return Locale.GERMAN;
    }
    return locale;
  }

  public void setLocale(final Locale locale) {
    this.locale = locale;
  }

  /**
   * If an error occured (e. g. lucene parse exception) this message will be returned.
   *
   * @return
   */
  public String getErrorMessage() {
    return filter.getErrorMessage();
  }

  public void setErrorMessage(final String errorMessage) {
    filter.setErrorMessage(errorMessage);
  }

  public boolean hasErrorMessage() {
    return filter.hasErrorMessage();
  }

  public void clearErrorMessage() {
    filter.clearErrorMessage();
  }

  /**
   * @param criterion
   * @return
   * @see org.hibernate.Criteria#add(Criterion)
   */
  public QueryFilter add(final Criterion criterion) {
    filterSettings.add(criterion);
    return this;
  }

  /**
   * @param order
   * @return
   * @see org.hibernate.Criteria#addOrder(Order)
   */
  public QueryFilter addOrder(final Order order) {
    filterSettings.add(order);
    return this;
  }

  public void setFetchMode(final String associationPath, final FetchMode mode) {
    this.associationPath = associationPath;
    this.fetchMode = mode;
  }

  /**
   * Adds Expression.between for given time period.
   *
   * @param dateField
   * @param year      if <= 0 do nothing.
   * @param month     if < 0 choose whole year, otherwise given month. (Calendar.MONTH);
   */
  public void setYearAndMonth(final String dateField, final int year, final int month) {
    if (year > 0) {
      final Calendar cal = DateHelper.getUTCCalendar();
      cal.set(Calendar.YEAR, year);
      java.sql.Date lo = null;
      java.sql.Date hi = null;
      if (month >= 0) {
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        lo = new java.sql.Date(cal.getTimeInMillis());
        final int lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, lastDayOfMonth);
        hi = new java.sql.Date(cal.getTimeInMillis());
      } else {
        cal.set(Calendar.DAY_OF_YEAR, 1);
        lo = new java.sql.Date(cal.getTimeInMillis());
        final int lastDayOfYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
        cal.set(Calendar.DAY_OF_YEAR, lastDayOfYear);
        hi = new java.sql.Date(cal.getTimeInMillis());
      }
      add(Restrictions.between(dateField, lo, hi));
    }
  }

  public Criteria buildCriteria(final Session session, final Class<?> clazz) {
    final Criteria criteria = session.createCriteria(clazz);
    buildCriteria(criteria);
    return criteria;
  }

  private void buildCriteria(final Criteria criteria) {
    for (final Object obj : filterSettings) {
      if (obj instanceof Criterion) {
        criteria.add((Criterion) obj);
      } else if (obj instanceof Order) {
        criteria.addOrder((Order) obj);
      } else if (obj instanceof Alias) {
        final Alias alias = (Alias) obj;
        criteria.createAlias(alias.arg0, alias.arg1, alias.joinType);
      } else if (obj instanceof QueryFilter) {
        final QueryFilter filter = (QueryFilter) obj;
        Criteria subCriteria;
        if (StringUtils.isEmpty(filter.getAlias()) == true) {
          subCriteria = criteria.createCriteria(filter.getName());
        } else {
          subCriteria = criteria.createCriteria(filter.getName(), filter.getAlias());
        }
        filter.buildCriteria(subCriteria);
      }
    }
    if (associationPath != null) {
      criteria.setFetchMode(associationPath, fetchMode);
    }
    if (filter.sortAndLimitMaxRowsWhileSelect) {
      if (filter.getMaxRows() > 0) {
        criteria.setMaxResults(filter.getMaxRows());
      }
      if (filter.getSortProperty() != null) {
        if (filter.getSortOrder() == SortOrder.DESCENDING)
          criteria.addOrder(Order.desc(filter.getSortProperty()));
        else
          criteria.addOrder(Order.asc(filter.getSortProperty()));
      }
    }
  }

  /**
   * @see org.hibernate.Criteria#createAlias(String, String)
   */
  public QueryFilter createAlias(final String arg0, final String arg1) {
    filterSettings.add(new Alias(arg0, arg1));
    return this;
  }

  /**
   * @see org.hibernate.Criteria#createAlias(String, String, int)
   */
  public QueryFilter createAlias(final String arg0, final String arg1, final int joinType) {
    filterSettings.add(new Alias(arg0, arg1, joinType));
    return this;
  }

  public QueryFilter createCriteria(final String name) {
    final QueryFilter filter = new QueryFilter(name);
    filterSettings.add(filter);
    return filter;
  }

  public QueryFilter createCriteria(final String name, final String alias) {
    final QueryFilter filter = new QueryFilter(name);
    filter.alias = alias;
    filterSettings.add(filter);
    return filter;
  }

  class Alias {
    String arg0;

    String arg1;

    int joinType = Criteria.INNER_JOIN;

    Alias(final String arg0, final String arg1) {
      this.arg0 = arg0;
      this.arg1 = arg1;
    }

    Alias(final String arg0, final String arg1, final int joinType) {
      this.arg0 = arg0;
      this.arg1 = arg1;
      this.joinType = joinType;
    }
  }
}
