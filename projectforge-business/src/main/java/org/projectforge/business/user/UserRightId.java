/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.user;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.RightRightIdProviderService;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Indexed
@ClassBridge(index = Index.YES /* TOKENIZED */, store = Store.NO, impl = HibernateSearchUserRightIdBridge.class)
public enum UserRightId implements IUserRightId
{

  //For test fix. Has to be removed.
  TEST("", "", ""),

  ADMIN_TENANT("ADMIN_TENANT", "admin",
      "access.right.admin.multitenancy"),

  ADMIN_CORE("ADMIN_CORE", "admin",
      "access.right.admin.core"),

  FIBU_EINGANGSRECHNUNGEN("FIBU_EINGANGSRECHNUNGEN", "fibu1",
      "access.right.fibu.eingangsrechnungen"),

  FIBU_AUSGANGSRECHNUNGEN("FIBU_AUSGANGSRECHNUNGEN", "fibu2",
      "access.right.fibu.ausgangsrechnungen"),

  FIBU_DATEV_IMPORT("FIBU_DATEV_IMPORT", "fibu5",
      "access.right.fibu.datevImport"),

  FIBU_COST_UNIT("FIBU_COST_UNIT", "fibu6",
      "access.right.fibu.costUnit"),

  FIBU_ACCOUNTS("FIBU_ACCOUNTS", "fibu7",
      "access.right.fibu.accounts"),

  MISC_MEB("MISC_MEB", "misc1", "access.right.misc.meb"),

  PM_GANTT("PM_GANTT", "pm1", "access.right.pm.gantt"),

  PM_ORDER_BOOK("PM_ORDER_BOOK", "pm2", "access.right.pm.orderbook"),

  PM_HR_PLANNING("PM_HR_PLANNING", "pm3",
      "access.right.pm.hrPlanning"),

  PM_PROJECT("PM_PROJECT", "pm4", "access.right.pm.project"),

  ORGA_CONTRACTS("ORGA_CONTRACTS", "orga1",
      "access.right.orga.contracts"),

  ORGA_INCOMING_MAIL("ORGA_INCOMING_MAIL", "orga2",
      "access.right.orga.incomingmail"),

  ORGA_OUTGOING_MAIL("ORGA_OUTGOING_MAIL", "orga3",
      "access.right.orga.outgoingmail"),

  ORGA_VISITORBOOK("ORGA_VISITORBOOK", "orga4",
      "access.right.orga.visitorbook"),

  HR_EMPLOYEE("HR_EMPLOYEE", "hr1",
      "access.right.hr.employee"),

  HR_EMPLOYEE_SALARY("HR_EMPLOYEE_SALARY", "hr2",
      "access.right.hr.employeeSalaries"),

  HR_VACATION("HR_VACATION", "hr3",
      "access.right.hr.vacation"),

  PLUGIN_CALENDAR("PLUGIN_CALENDAR", "plugin15",
      "plugins.teamcal.calendar"),

  PLUGIN_CALENDAR_EVENT("PLUGIN_CALENDAR_EVENT", "plugin15",
      "plugins.teamcalendar.event");

  public static class ProviderService implements RightRightIdProviderService
  {
    @Override
    public Collection<IUserRightId> getUserRightIds()
    {
      return Arrays.asList(UserRightId.values());
    }
  }

  private final String id;

  private final String orderString;

  private final String i18nKey;

  /**
   * @param id          Must be unique (including all plugins).
   * @param orderString For displaying the rights in e. g. UserEditPage in the correct order.
   * @param i18nKey
   */
  private UserRightId(final String id, final String orderString, final String i18nKey)
  {
    this.id = id;
    this.orderString = orderString;
    this.i18nKey = i18nKey;
  }

  @Override
  public String getId()
  {
    return id;
  }

  @Override
  public String getI18nKey()
  {
    return i18nKey;
  }

  @Override
  public String getOrderString()
  {
    return orderString;
  }

  @Override
  public String toString()
  {
    return String.valueOf(id);
  }

  @Override
  public int compareTo(IUserRightId o)
  {
    return this.compareTo(o);
  }

}
