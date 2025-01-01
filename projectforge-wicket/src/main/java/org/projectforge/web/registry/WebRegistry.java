/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.registry;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.projectforge.framework.persistence.DaoConst;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.registry.Registry;
import org.projectforge.web.access.AccessEditPage;
import org.projectforge.web.access.AccessListPage;
import org.projectforge.web.address.*;
import org.projectforge.web.admin.AdminPage;
import org.projectforge.web.admin.ConfigurationListPage;
import org.projectforge.web.admin.SetupPage;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.core.SearchPage;
import org.projectforge.web.fibu.*;
import org.projectforge.web.gantt.GanttChartEditPage;
import org.projectforge.web.gantt.GanttChartListPage;
import org.projectforge.web.humanresources.HRListPage;
import org.projectforge.web.humanresources.HRPlanningEditPage;
import org.projectforge.web.humanresources.HRPlanningListPage;
import org.projectforge.web.statistics.PersonalStatisticsPage;
import org.projectforge.web.task.TaskEditPage;
import org.projectforge.web.task.TaskListPage;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.teamcal.event.TeamEventListPage;
import org.projectforge.web.teamcal.integration.TeamCalCalendarPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.timesheet.TimesheetListPage;
import org.projectforge.web.user.*;
import org.projectforge.web.wicket.ErrorPage;
import org.projectforge.web.wicket.FeedbackPage;
import org.projectforge.web.wicket.IListPageColumnsCreator;

import java.util.*;

/**
 * Registry for dao's. Here you can register additional daos and plugins (extensions of ProjectForge). This registry is
 * used e. g. by the general SearchPage.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class WebRegistry
{
  public static final WebRegistry instance = new WebRegistry();

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebRegistry.class);

  private final Map<String, WebRegistryEntry> map = new HashMap<String, WebRegistryEntry>();

  private final List<WebRegistryEntry> orderedList = new ArrayList<WebRegistryEntry>();

  private final Map<String, Class<? extends WebPage>> mountPages = new HashMap<String, Class<? extends WebPage>>();

  public static WebRegistry getInstance()
  {
    return instance;
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   */
  public WebRegistryEntry register(final String id)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id));
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   * @param listPageColumnsCreatorClass Needed by SearchPage.
   */
  public WebRegistryEntry register(final String id,
      final Class<? extends IListPageColumnsCreator<?>> listPageColumnsCreatorClass)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id, listPageColumnsCreatorClass));
  }

  public WebRegistryEntry register(final WebRegistryEntry entry)
  {
    Objects.requireNonNull(entry);
    map.put(entry.getId(), entry);
    orderedList.add(entry);
    return entry;
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   */
  public WebRegistryEntry register(final String id, final boolean insertBefore, final WebRegistryEntry entry)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id), insertBefore, entry);
  }

  /**
   * Creates a new WebRegistryEntry and registers it. Id must be found in {@link Registry}.
   *
   * @param id
   */
  public WebRegistryEntry register(final String id,
      final Class<? extends IListPageColumnsCreator<?>> listPageColumnsCreatorClass,
      final boolean insertBefore, final WebRegistryEntry entry)
  {
    return register(new WebRegistryEntry(Registry.getInstance(), id, listPageColumnsCreatorClass), insertBefore, entry);
  }

  /**
   * @param existingEntry
   * @param insertBefore  If true then the given entry will be inserted before the existing entry, otherwise after.
   * @param entry
   * @return
   */
  public WebRegistryEntry register(final WebRegistryEntry existingEntry, final boolean insertBefore,
      final WebRegistryEntry entry)
  {
    Objects.requireNonNull(existingEntry);
    Objects.requireNonNull(entry);
    map.put(entry.getId(), entry);
    final int idx = orderedList.indexOf(existingEntry);
    if (idx < 0) {
      log.error("Registry entry '" + existingEntry.getId() + "' not found. Appending the given entry to the list.");
      orderedList.add(entry);
    } else if (insertBefore) {
      orderedList.add(idx, entry);
    } else {
      orderedList.add(idx + 1, entry);
    }
    return entry;
  }

  public WebRegistryEntry getEntry(final String id)
  {
    return map.get(id);
  }

  public List<WebRegistryEntry> getOrderedList()
  {
    return orderedList;
  }

  public BaseDao<?> getDao(final String id)
  {
    final WebRegistryEntry entry = getEntry(id);
    return entry != null ? entry.getDao() : null;
  }

  /**
   * Adds the page class as mount page.
   *
   * @param mountPage
   * @param pageClass
   * @return this for chaining.
   */
  public WebRegistry addMountPage(final String mountPage, final Class<? extends WebPage> pageClass)
  {
    this.mountPages.put(mountPage, pageClass);
    return this;
  }

  /**
   * Adds the both page classes as mount pages: mountPageBasename + "{List,Edit}.
   *
   * @param mountPageBasename
   * @param pageListClass
   * @param pageEditClass
   * @return this for chaining.
   */
  public WebRegistry addMountPages(final String mountPageBasename, final Class<? extends WebPage> pageListClass,
      final Class<? extends WebPage> pageEditClass)
  {
    addMountPage(mountPageBasename + "List", pageListClass);
    addMountPage(mountPageBasename + "Edit", pageEditClass);
    return this;
  }

  /**
   * Adds all page classes as mount pages: mountPageBasename + "{List,Edit,View}.
   *
   * @param mountPageBasename
   * @param pageListClass
   * @param pageEditClass
   * @param pageViewClass
   * @return this for chaining.
   */
  public WebRegistry addMountPages(final String mountPageBasename, final Class<? extends WebPage> pageListClass,
      final Class<? extends WebPage> pageEditClass, final Class<? extends WebPage> pageViewClass)
  {
    addMountPage(mountPageBasename + "List", pageListClass);
    addMountPage(mountPageBasename + "Edit", pageEditClass);
    addMountPage(mountPageBasename + "View", pageViewClass);
    return this;
  }

  public Map<String, Class<? extends WebPage>> getMountPages()
  {
    return mountPages;
  }

  public String getMountPoint(Class<? extends Page> pageClass) {
    for (Map.Entry<String, Class<? extends WebPage>> entry : mountPages.entrySet()) {
      if (pageClass.equals(entry.getValue())) {
        final String key = entry.getKey();
        if (key.contains("wa")) {
          return key;
        }
        return "wa/" + entry.getKey();
      }
    }
    return "wa/wicket/bookmarkable/" + pageClass.getName();
  }

  public void init()
  {
    // This order is used by SearchPage:
    register(DaoConst.ADDRESS, AddressListPage.class);
    addMountPages(DaoConst.ADDRESS, AddressListPage.class, AddressEditPage.class);

    register(DaoConst.TASK, TaskListPage.class);
    addMountPages(DaoConst.TASK, TaskListPage.class, TaskEditPage.class);

    register(DaoConst.TIMESHEET, TimesheetListPage.class);
    addMountPages(DaoConst.TIMESHEET, TimesheetListPage.class, TimesheetEditPage.class);

    register(DaoConst.GROUP, GroupListPage.class);
    addMountPages(DaoConst.GROUP, GroupListPage.class, GroupEditPage.class);

    register(DaoConst.ORDERBOOK, AuftragListPage.class);
    addMountPages(DaoConst.ORDERBOOK, AuftragListPage.class, AuftragEditPage.class);

    register(DaoConst.INCOMING_INVOICE, EingangsrechnungListPage.class);
    addMountPages(DaoConst.INCOMING_INVOICE, EingangsrechnungListPage.class, EingangsrechnungEditPage.class);

    register(DaoConst.OUTGOING_INVOICE, RechnungListPage.class);
    addMountPages(DaoConst.OUTGOING_INVOICE, RechnungListPage.class, RechnungEditPage.class);

    register(DaoConst.ACCESS, AccessListPage.class);
    addMountPages(DaoConst.ACCESS, AccessListPage.class, AccessEditPage.class);
    register(DaoConst.ACCOUNT, KontoListPage.class);
    addMountPages(DaoConst.ACCOUNT, KontoListPage.class, KontoEditPage.class);
    register(DaoConst.ACCOUNTING_RECORD, AccountingRecordListPage.class);
    addMountPages(DaoConst.ACCOUNTING_RECORD, AccountingRecordListPage.class, AccountingRecordEditPage.class);
    register(DaoConst.COST1, Kost1ListPage.class);
    addMountPages(DaoConst.COST1, Kost1ListPage.class, Kost1EditPage.class);
    register(DaoConst.COST2, Kost2ListPage.class);
    addMountPages(DaoConst.COST2, Kost2ListPage.class, Kost2EditPage.class);
    register(DaoConst.COST2_Type, Kost2ArtListPage.class);
    addMountPages(DaoConst.COST2_Type, Kost2ArtListPage.class, Kost2ArtEditPage.class);
    register(DaoConst.CUSTOMER, CustomerListPage.class);
    addMountPages(DaoConst.CUSTOMER, CustomerListPage.class, CustomerEditPage.class);
    register(DaoConst.PROJECT, ProjektListPage.class);
    addMountPages(DaoConst.PROJECT, ProjektListPage.class, ProjektEditPage.class);

    addMountPages(DaoConst.EMPLOYEE_SALARY, EmployeeSalaryListPage.class, EmployeeSalaryEditPage.class);
    addMountPages(DaoConst.GANTT, GanttChartListPage.class, GanttChartEditPage.class);
    addMountPages(DaoConst.HR_PLANNING, HRPlanningListPage.class, HRPlanningEditPage.class);
    addMountPage(DaoConst.HR_LIST, HRListPage.class);
    addMountPages(DaoConst.USER_PREF, UserPrefListPage.class, UserPrefEditPage.class);

    addMountPage("admin", AdminPage.class);
    addMountPage("oldCalendar", CalendarPage.class); // Backup url for deprecated calendar, will be removed.
    addMountPage("configuration", ConfigurationListPage.class);
    addMountPage("datevImport", DatevImportPage.class);
    addMountPage("error", ErrorPage.class);
    addMountPage("feedback", FeedbackPage.class);
    addMountPage("monthlyEmployeeReport", MonthlyEmployeeReportPage.class);
    addMountPage("personalStatistics", PersonalStatisticsPage.class);
    addMountPage("phoneCall", PhoneCallPage.class);
    addMountPage("reportObjectives", ReportObjectivesPage.class);
    addMountPage("search", SearchPage.class);
    addMountPage("sendSms", SendSmsPage.class);
    addMountPage("setup", SetupPage.class);
    addMountPage("taskTree", TaskTreePage.class);

    register("teamEvent", TeamEventListPage.class);
    addMountPage("oldTeamCalendar", TeamCalCalendarPage.class); // Backup url for deprecated calendar, will be removed.
  }
}
