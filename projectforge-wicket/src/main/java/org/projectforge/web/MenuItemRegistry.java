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

package org.projectforge.web;

import org.apache.wicket.Page;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.web.access.AccessListPage;
import org.projectforge.web.address.AddressListPage;
import org.projectforge.web.address.AddressbookListPage;
import org.projectforge.web.address.PhoneCallPage;
import org.projectforge.web.address.SendSmsPage;
import org.projectforge.web.admin.*;
import org.projectforge.web.core.SearchPage;
import org.projectforge.web.fibu.*;
import org.projectforge.web.gantt.GanttChartListPage;
import org.projectforge.web.humanresources.HRListPage;
import org.projectforge.web.humanresources.HRPlanningListPage;
import org.projectforge.web.meb.MebListPage;
import org.projectforge.web.orga.ContractListPage;
import org.projectforge.web.orga.PostausgangListPage;
import org.projectforge.web.orga.PosteingangListPage;
import org.projectforge.web.orga.VisitorbookListPage;
import org.projectforge.web.scripting.ScriptListPage;
import org.projectforge.web.scripting.ScriptingPage;
import org.projectforge.web.statistics.PersonalStatisticsPage;
import org.projectforge.web.statistics.SystemStatisticsPage;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.teamcal.admin.TeamCalListPage;
import org.projectforge.web.teamcal.integration.TeamCalCalendarPage;
import org.projectforge.web.timesheet.TimesheetListPage;
import org.projectforge.web.user.*;
import org.projectforge.web.vacation.VacationListPage;
import org.projectforge.web.vacation.VacationViewPage;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The menu is build from the menu items which are registered in this registry. The order of the menu entries is defined
 * by the order number of the menu item definitions. <br/>
 * This menu item registry is the central instance for handling the order and common visibility of menu items. It
 * doesn't represent the individual user's menu (the individual user's menu is generated out of this registry).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class MenuItemRegistry implements Serializable {
  private static final long serialVersionUID = -6988615451822648295L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MenuItemRegistry.class);

  private final Map<String, Class<? extends Page>> wicketClassesMap = new HashMap<>();

  @PostConstruct
  public void init() {
    initialize(this);
  }

  public Class<? extends Page> getPageClass(String menuItemDefId) {
    return wicketClassesMap.get(menuItemDefId);
  }

  /**
   * Registers all Wicket page classes.
   */
  private void initialize(final MenuItemRegistry reg) {
    register(MenuItemDefId.CALENDAR, TeamCalCalendarPage.class);
    register(MenuItemDefId.TEAMCALENDAR, TeamCalListPage.class);
    register(MenuItemDefId.VACATION, VacationListPage.class);
    // createdMenuEntry.setNewCounterModel(new MenuNewCounterVacation());

    register(MenuItemDefId.ADDRESSBOOK_LIST, AddressbookListPage.class);
    register(MenuItemDefId.ADDRESS_LIST, AddressListPage.class);
    register(MenuItemDefId.PHONE_CALL, PhoneCallPage.class);
    register(MenuItemDefId.SEND_SMS, SendSmsPage.class);
    register(MenuItemDefId.MEB, MebListPage.class);
    // createdMenuEntry.setNewCounterModel(new MenuNewCounterMeb());
    register(MenuItemDefId.SEARCH, SearchPage.class);
    register(MenuItemDefId.TASK_TREE, TaskTreePage.class);
    register(MenuItemDefId.TIMESHEET_LIST, TimesheetListPage.class);
    register(MenuItemDefId.MONTHLY_EMPLOYEE_REPORT, MonthlyEmployeeReportPage.class);
    register(MenuItemDefId.PERSONAL_STATISTICS, PersonalStatisticsPage.class);
    register(MenuItemDefId.HR_VIEW, HRListPage.class);
    register(MenuItemDefId.HR_PLANNING_LIST, HRPlanningListPage.class);
    register(MenuItemDefId.GANTT, GanttChartListPage.class);
    register(MenuItemDefId.OUTGOING_INVOICE_LIST, RechnungListPage.class);
    register(MenuItemDefId.INCOMING_INVOICE_LIST, EingangsrechnungListPage.class);
    register(MenuItemDefId.CUSTOMER_LIST, CustomerListPage.class);
    register(MenuItemDefId.PROJECT_LIST, ProjektListPage.class);
    register(MenuItemDefId.EMPLOYEE_LIST, EmployeeListPage.class);
    register(MenuItemDefId.EMPLOYEE_SALARY_LIST, EmployeeSalaryListPage.class);
    register(MenuItemDefId.ORDER_LIST, AuftragListPage.class);
    // createdMenuEntry.setNewCounterModel(new MenuNewCounterOrder());
    // createdMenuEntry.setNewCounterTooltip("menu.fibu.orderbook.htmlSuffixTooltip");
    register(MenuItemDefId.ACCOUNT_LIST, KontoListPage.class);
    register(MenuItemDefId.COST1_LIST, Kost1ListPage.class);
    register(MenuItemDefId.COST2_LIST, Kost2ListPage.class);
    register(MenuItemDefId.COST2_TYPE_LIST, Kost2ArtListPage.class);
    register(MenuItemDefId.SCRIPT_LIST, ScriptListPage.class);
    register(MenuItemDefId.SCRIPTING, ScriptingPage.class);
    register(MenuItemDefId.REPORT_OBJECTIVES, ReportObjectivesPage.class);
    register(MenuItemDefId.ACCOUNTING_RECORD_LIST, AccountingRecordListPage.class);
    register(MenuItemDefId.DATEV_IMPORT, DatevImportPage.class);
    register(MenuItemDefId.OUTBOX_LIST, PostausgangListPage.class);
    register(MenuItemDefId.INBOX_LIST, PosteingangListPage.class);
    register(MenuItemDefId.CONTRACTS, ContractListPage.class);
    register(MenuItemDefId.VISITORBOOK, VisitorbookListPage.class);
    register(MenuItemDefId.MY_ACCOUNT, MyAccountEditPage.class);
    register(MenuItemDefId.VACATION_VIEW, VacationViewPage.class);
    register(MenuItemDefId.MY_PREFERENCES, UserPrefListPage.class);
    register(MenuItemDefId.CHANGE_PASSWORD, ChangePasswordPage.class);
    register(MenuItemDefId.CHANGE_WLAN_PASSWORD, ChangeWlanPasswordPage.class);
    register(MenuItemDefId.USER_LIST, UserListPage.class);
    register(MenuItemDefId.GROUP_LIST, GroupListPage.class);
    register(MenuItemDefId.ACCESS_LIST, AccessListPage.class);
    register(MenuItemDefId.SYSTEM, AdminPage.class);
    register(MenuItemDefId.SQL_CONSOLE, SqlConsolePage.class);
    register(MenuItemDefId.GROOVY_CONSOLE, GroovyConsolePage.class);
    register(MenuItemDefId.LUCENE_CONSOLE, LuceneConsolePage.class);
    register(MenuItemDefId.SYSTEM_UPDATE, SystemUpdatePage.class);
    register(MenuItemDefId.SYSTEM_STATISTICS, SystemStatisticsPage.class);
    register(MenuItemDefId.CONFIGURATION, ConfigurationListPage.class);
    register(MenuItemDefId.PLUGIN_ADMIN, PluginListPage.class);
  }

  public void register(MenuItemDefId defId, Class<? extends Page> pageClass) {
    register(defId.getId(), pageClass);
  }

  public void register(String menuItemId, Class<? extends Page> pageClass) {
    this.wicketClassesMap.put(menuItemId, pageClass);
  }
}
