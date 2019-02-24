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

import static org.projectforge.business.user.ProjectForgeGroup.*;
import static org.projectforge.framework.persistence.api.UserRightService.READONLY_PARTLYREADWRITE_READWRITE;
import static org.projectforge.framework.persistence.api.UserRightService.READONLY_READWRITE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.EingangsrechnungDao;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.datev.DatevImportDao;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.humanresources.HRPlanningDao;
import org.projectforge.business.login.Login;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.orga.ContractDao;
import org.projectforge.business.orga.PostausgangDao;
import org.projectforge.business.orga.PosteingangDao;
import org.projectforge.business.orga.VisitorbookDao;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.access.AccessListPage;
import org.projectforge.web.address.AddressListPage;
import org.projectforge.web.address.AddressMobileListPage;
import org.projectforge.web.address.AddressbookListPage;
import org.projectforge.web.address.PhoneCallPage;
import org.projectforge.web.address.SendSmsPage;
import org.projectforge.web.admin.AdminPage;
import org.projectforge.web.admin.ConfigurationListPage;
import org.projectforge.web.admin.GroovyConsolePage;
import org.projectforge.web.admin.LuceneConsolePage;
import org.projectforge.web.admin.PluginListPage;
import org.projectforge.web.admin.SqlConsolePage;
import org.projectforge.web.admin.SystemUpdatePage;
import org.projectforge.web.book.BookListPage;
import org.projectforge.web.core.SearchPage;
import org.projectforge.web.fibu.AccountingRecordListPage;
import org.projectforge.web.fibu.AuftragListPage;
import org.projectforge.web.fibu.CustomerListPage;
import org.projectforge.web.fibu.DatevImportPage;
import org.projectforge.web.fibu.EingangsrechnungListPage;
import org.projectforge.web.fibu.EmployeeListPage;
import org.projectforge.web.fibu.EmployeeSalaryListPage;
import org.projectforge.web.fibu.KontoListPage;
import org.projectforge.web.fibu.Kost1ListPage;
import org.projectforge.web.fibu.Kost2ArtListPage;
import org.projectforge.web.fibu.Kost2ListPage;
import org.projectforge.web.fibu.MonthlyEmployeeReportPage;
import org.projectforge.web.fibu.ProjektListPage;
import org.projectforge.web.fibu.RechnungListPage;
import org.projectforge.web.fibu.ReportObjectivesPage;
import org.projectforge.web.gantt.GanttChartListPage;
import org.projectforge.web.humanresources.HRListPage;
import org.projectforge.web.humanresources.HRPlanningListPage;
import org.projectforge.web.meb.MebListPage;
import org.projectforge.web.multitenancy.TenantListPage;
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
import org.projectforge.web.user.ChangePasswordPage;
import org.projectforge.web.user.ChangeWlanPasswordPage;
import org.projectforge.web.user.GroupListPage;
import org.projectforge.web.user.MyAccountEditPage;
import org.projectforge.web.user.UserListPage;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.vacation.MenuNewCounterVacation;
import org.projectforge.web.vacation.VacationListPage;
import org.projectforge.web.vacation.VacationViewPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The menu is build from the menu items which are registered in this registry. The order of the menu entries is defined
 * by the order number of the menu item definitions. <br/>
 * This menu item registry is the central instance for handling the order and common visibility of menu items. It
 * doesn't represent the individual user's menu (the individual user's menu is generated out of this registry).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class MenuItemRegistry implements Serializable
{
  private static final long serialVersionUID = -6988615451822648295L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MenuItemRegistry.class);

  private final List<MenuItemDef> menuItemList = new ArrayList<MenuItemDef>();
  private final List<MenuItemDef> favoritesItemList = new ArrayList<MenuItemDef>();

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private VacationService vacationService;

  @PostConstruct
  public void init()
  {
    initialize(this);
  }

  public MenuItemDef get(final String id)
  {
    for (final MenuItemDef entry : menuItemList) {
      if (id.equals(entry.getId()) == true) {
        return entry;
      }
    }
    return null;
  }

  public MenuItemDef get(final MenuItemDefId id)
  {
    return get(id.getId());
  }

  public List<MenuItemDef> getMenuItemList()
  {
    return menuItemList;
  }

  /**
   * Registers menu entry definition. It's important that a parent menu entry item definition is registered before its
   * sub menu entry items.
   *
   * @param menuItemDef
   * @return
   */
  public MenuItemDef register(final MenuItemDef menuItemDef)
  {
    // Check if ID already exists
    if (get(menuItemDef.getId()) != null) {
      throw (new IllegalArgumentException(String.format("Duplicated menu ID '%s' for entry '%s'", menuItemDef.getId(), menuItemDef.getI18nKey())));
    }

    menuItemList.add(menuItemDef);
    return menuItemDef;
  }

  public void registerFavorites(final MenuItemDef menuItemDef)
  {
    favoritesItemList.add(menuItemDef);
  }

  /**
   * Should be called after any modification of configuration parameters such as costConfigured. It refreshes the
   * visibility of some menu entries.
   */
  @SuppressWarnings("serial")
  public void refresh()
  {
    final MenuItemDefVisibility costConfiguredVisibility = new MenuItemDefVisibility()
    {
      @Override
      public boolean isVisible()
      {
        return Configuration.getInstance().isCostConfigured();
      }
    };
    get(MenuItemDefId.CUSTOMER_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.PROJECT_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.EMPLOYEE_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.EMPLOYEE_SALARY_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.ACCOUNT_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.COST1_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.COST2_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.COST2_TYPE_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.ACCOUNTING_RECORD_LIST).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.REPORT_OBJECTIVES).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.DATEV_IMPORT).setVisibility(costConfiguredVisibility);
    get(MenuItemDefId.MEB).setVisibility(new MenuItemDefVisibility()
    {
      @Override
      public boolean isVisible()
      {
        return Configuration.getInstance().isMebConfigured();
      }
    });
    get(MenuItemDefId.PHONE_CALL).setVisible(StringUtils.isNotEmpty(configurationService.getTelephoneSystemUrl()));
    get(MenuItemDefId.CONTRACTS).setVisible(CollectionUtils.isNotEmpty(configurationService.getContractTypes()));

    final SecurityConfig securityConfig = configurationService.getSecurityConfig();
    final boolean sqlConsoleAvailable = WebConfiguration.isDevelopmentMode() == true
        || configurationService.isSqlConsoleAvailable() == true
        || (securityConfig != null && securityConfig.isSqlConsoleAvailable() == true);
    get(MenuItemDefId.SQL_CONSOLE).setVisible(sqlConsoleAvailable);
    get(MenuItemDefId.LUCENE_CONSOLE).setVisible(sqlConsoleAvailable);
    get(MenuItemDefId.GROOVY_CONSOLE).setVisible(sqlConsoleAvailable);
  }

  private MenuItemDef register(final MenuItemDef parent, final MenuItemDefId defId, final int orderNumber,
      final ProjectForgeGroup... visibleForGroups)
  {
    return register(new MenuItemDef(parent, defId.getId(), orderNumber, defId.getI18nKey(), visibleForGroups));
  }

  private MenuItemDef register(final MenuItemDef parent, final MenuItemDefId defId, final int orderNumber,
      final Class<? extends Page> pageClass, final UserRightId requiredRightId,
      final UserRightValue... requiredRightValues)
  {
    return register(
        new MenuItemDef(parent, defId.getId(), orderNumber, defId.getI18nKey(), pageClass, null, requiredRightId,
            requiredRightValues));
  }

  private MenuItemDef register(final MenuItemDef parent, final MenuItemDefId defId, final int orderNumber,
      final Class<? extends Page> pageClass, final ProjectForgeGroup... visibleForGroups)
  {
    return register(parent, defId, orderNumber, pageClass, null, visibleForGroups);
  }

  private MenuItemDef register(final MenuItemDef parent, final MenuItemDefId defId, final int orderNumber,
      final Class<? extends Page> pageClass, final String[] params, final ProjectForgeGroup... visibleForGroups)
  {
    return register(parent, defId, orderNumber, pageClass, params, true, visibleForGroups);
  }

  private MenuItemDef register(final MenuItemDef parent, final MenuItemDefId defId, final int orderNumber,
      final Class<? extends Page> pageClass, final String[] params, final boolean visible,
      final ProjectForgeGroup... visibleForGroups)
  {
    return register(
        new MenuItemDef(parent, defId.getId(), orderNumber, defId.getI18nKey(), pageClass, params, visibleForGroups)
            .setVisible(visible));
  }

  // Needed as static method (because anonymous declared MenuItemDef are serialized).
  @SuppressWarnings("serial")
  private void initialize(final MenuItemRegistry reg)
  {
    // Super menus
    final MenuItemDef common = reg.register(null, MenuItemDefId.COMMON, 10);
    final MenuItemDef pm = reg.register(null, MenuItemDefId.PROJECT_MANAGEMENT, 20);
    final MenuItemDef hr = reg.register(null, MenuItemDefId.HR, 30, HR_GROUP);
    final MenuItemDef fibu = reg.register(null, MenuItemDefId.FIBU, 40, UserRightService.FIBU_ORGA_HR_GROUPS);
    final MenuItemDef cost = reg.register(null, MenuItemDefId.COST, 50, UserRightService.FIBU_ORGA_HR_GROUPS);
    final MenuItemDef reporting = reg.register(null, MenuItemDefId.REPORTING, 60, FINANCE_GROUP, CONTROLLING_GROUP,
        HR_GROUP);
    final MenuItemDef orga = reg.register(null, MenuItemDefId.ORGA, 70, UserRightService.FIBU_ORGA_HR_GROUPS);
    final MenuItemDef admin = reg.register(null, MenuItemDefId.ADMINISTRATION, 80).setVisibleForRestrictedUsers(true);
    final MenuItemDef misc = reg.register(null, MenuItemDefId.MISC, 100);

    // Menu entries
    // COMMON
    reg.register(common, MenuItemDefId.CALENDAR, 10, TeamCalCalendarPage.class); // Visible for all.
    reg.register(common, MenuItemDefId.TEAMCALENDAR, 20, TeamCalListPage.class); //
    final MenuItemDef vacation = new MenuItemDef(common, MenuItemDefId.VACATION.getId(), 21, MenuItemDefId.VACATION.getI18nKey(), VacationListPage.class)
    {
      @Override
      protected void afterMenuEntryCreation(final MenuEntry createdMenuEntry, final MenuBuilderContext context)
      {
        createdMenuEntry.setNewCounterModel(new MenuNewCounterVacation());
      }
    };
    reg.register(vacation);
    reg.register(common, MenuItemDefId.BOOK_LIST, 30, BookListPage.class); // Visible for all.
    reg.register(common, MenuItemDefId.ADDRESSBOOK_LIST, 35, AddressbookListPage.class); //
    reg.register(common, MenuItemDefId.ADDRESS_LIST, 40, AddressListPage.class)
        .setMobileMenu(AddressMobileListPage.class, 100); // Visible
    // for all.
    reg.register(common, MenuItemDefId.PHONE_CALL, 50, PhoneCallPage.class);
    reg.register(common, MenuItemDefId.SEND_SMS, 60, SendSmsPage.class);
    final MenuItemDef meb = new MenuItemDef(common, MenuItemDefId.MEB.getId(), 70, MenuItemDefId.MEB.getI18nKey(),
        MebListPage.class)
    {
      @Override
      protected void afterMenuEntryCreation(final MenuEntry createdMenuEntry, final MenuBuilderContext context)
      {
        createdMenuEntry.setNewCounterModel(new MenuNewCounterMeb());
      }
    };
    reg.register(meb);
    reg.register(common, MenuItemDefId.SEARCH, 100, SearchPage.class);

    // PROJECT_MANAGEMENT
    reg.register(pm, MenuItemDefId.TASK_TREE, 10, TaskTreePage.class);
    reg.register(pm, MenuItemDefId.TIMESHEET_LIST, 20, TimesheetListPage.class);
    reg.register(pm, MenuItemDefId.MONTHLY_EMPLOYEE_REPORT, 30, MonthlyEmployeeReportPage.class);
    reg.register(pm, MenuItemDefId.PERSONAL_STATISTICS, 40, PersonalStatisticsPage.class);
    reg.register(pm, MenuItemDefId.HR_VIEW, 50, HRListPage.class, HRPlanningDao.USER_RIGHT_ID, READONLY_READWRITE);
    reg.register(pm, MenuItemDefId.HR_PLANNING_LIST, 60, HRPlanningListPage.class);
    reg.register(pm, MenuItemDefId.GANTT, 70, GanttChartListPage.class);
    // Order book 80 (if user isn't member of FIBU groups.
    // Projects 90 (if user isn't member of FIBU groups.

    // FIBU
    reg.register(fibu, MenuItemDefId.OUTGOING_INVOICE_LIST, 10, RechnungListPage.class, RechnungDao.USER_RIGHT_ID,
        READONLY_READWRITE);
    reg.register(fibu, MenuItemDefId.INCOMING_INVOICE_LIST, 20, EingangsrechnungListPage.class,
        EingangsrechnungDao.USER_RIGHT_ID,
        READONLY_READWRITE);
    {
      // Only visible if cost is configured:
      reg.register(fibu, MenuItemDefId.CUSTOMER_LIST, 40, CustomerListPage.class, FINANCE_GROUP, CONTROLLING_GROUP);
      final MenuItemDef projects = new MenuItemDef(fibu, MenuItemDefId.PROJECT_LIST.getId(), 50,
          MenuItemDefId.PROJECT_LIST.getI18nKey(),
          ProjektListPage.class, ProjektDao.USER_RIGHT_ID, READONLY_READWRITE)
      {
        @Override
        protected void afterMenuEntryCreation(final MenuEntry createdMenuEntry, final MenuBuilderContext context)
        {
          if (context.getAccessChecker()
              .isLoggedInUserMemberOfGroup(UserRightService.FIBU_ORGA_HR_GROUPS) == false) {
            // Setting project management as parent because fibu isn't visible for this user:
            createdMenuEntry.setParent(context.getMenu(), pm.getId());
          }
        }
      };
      reg.register(projects);

      reg.register(hr, MenuItemDefId.EMPLOYEE_LIST, 10, EmployeeListPage.class, EmployeeDao.USER_RIGHT_ID,
          READONLY_READWRITE);
      reg.register(hr, MenuItemDefId.EMPLOYEE_SALARY_LIST, 11, EmployeeSalaryListPage.class,
          EmployeeSalaryDao.USER_RIGHT_ID, READONLY_READWRITE);
    }
    final MenuItemDef orderBook = new MenuItemDef(fibu, MenuItemDefId.ORDER_LIST.getId(), 80,
        MenuItemDefId.ORDER_LIST.getI18nKey(),
        AuftragListPage.class, AuftragDao.USER_RIGHT_ID, READONLY_PARTLYREADWRITE_READWRITE)
    {
      @Override
      protected void afterMenuEntryCreation(final MenuEntry createdMenuEntry, final MenuBuilderContext context)
      {
        if (context.getAccessChecker().isLoggedInUserMemberOfGroup(UserRightService.FIBU_ORGA_HR_GROUPS) == true) {
          createdMenuEntry.setNewCounterModel(new MenuNewCounterOrder());
          createdMenuEntry.setNewCounterTooltip("menu.fibu.orderbook.htmlSuffixTooltip");
        } else {
          // Setting project management as parent because fibu isn't visible for this user:
          createdMenuEntry.setParent(context.getMenu(), pm.getId());
        }
      }
    };
    reg.register(orderBook);

    {
      // COST
      // Only visible if cost is configured:
      reg.register(cost, MenuItemDefId.ACCOUNT_LIST, 10, KontoListPage.class, KontoDao.USER_RIGHT_ID,
          READONLY_READWRITE);
      reg.register(cost, MenuItemDefId.COST1_LIST, 20, Kost1ListPage.class, Kost2Dao.USER_RIGHT_ID, READONLY_READWRITE);
      reg.register(cost, MenuItemDefId.COST2_LIST, 30, Kost2ListPage.class, Kost2Dao.USER_RIGHT_ID, READONLY_READWRITE);
      reg.register(cost, MenuItemDefId.COST2_TYPE_LIST, 40, Kost2ArtListPage.class, Kost2Dao.USER_RIGHT_ID,
          READONLY_READWRITE);
    }

    // REPORTING
    reg.register(reporting, MenuItemDefId.SCRIPT_LIST, 10, ScriptListPage.class, FINANCE_GROUP, CONTROLLING_GROUP);
    reg.register(reporting, MenuItemDefId.SCRIPTING, 20, ScriptingPage.class, FINANCE_GROUP, CONTROLLING_GROUP);
    reg.register(reporting, MenuItemDefId.REPORT_OBJECTIVES, 30, ReportObjectivesPage.class, FINANCE_GROUP,
        CONTROLLING_GROUP);
    {
      // Only visible if cost is configured and DATEV-Import right is given:
      reg.register(reporting, MenuItemDefId.ACCOUNTING_RECORD_LIST, 40, AccountingRecordListPage.class, DatevImportDao.USER_RIGHT_ID,
          UserRightValue.TRUE);
      reg.register(reporting, MenuItemDefId.DATEV_IMPORT, 50, DatevImportPage.class, DatevImportDao.USER_RIGHT_ID,
          UserRightValue.TRUE);
    }

    // ORGA
    reg.register(orga, MenuItemDefId.OUTBOX_LIST, 10, PostausgangListPage.class, PostausgangDao.USER_RIGHT_ID,
        READONLY_READWRITE);
    reg.register(orga, MenuItemDefId.INBOX_LIST, 20, PosteingangListPage.class, PosteingangDao.USER_RIGHT_ID,
        READONLY_READWRITE);
    reg.register(orga, MenuItemDefId.CONTRACTS, 30, ContractListPage.class, ContractDao.USER_RIGHT_ID,
        READONLY_READWRITE);
    reg.register(orga, MenuItemDefId.VISITORBOOK, 30, VisitorbookListPage.class, VisitorbookDao.USER_RIGHT_ID,
        READONLY_READWRITE);

    // ADMINISTRATION
    reg.register(admin, MenuItemDefId.MY_ACCOUNT, 10, MyAccountEditPage.class);
    reg.register(
        new MenuItemDef(admin, MenuItemDefId.VACATION_VIEW.getId(), 11, MenuItemDefId.VACATION_VIEW.getI18nKey(),
            VacationViewPage.class)
        {
          @Override
          protected boolean isVisible(final MenuBuilderContext context)
          {
            return vacationService.couldUserUseVacationService(ThreadLocalUserContext.getUser(), false);
          }
        }
    );
    reg.register(admin, MenuItemDefId.MY_PREFERENCES, 20, UserPrefListPage.class);
    reg.register(
        new MenuItemDef(admin, MenuItemDefId.CHANGE_PASSWORD.getId(), 30, MenuItemDefId.CHANGE_PASSWORD.getI18nKey(),
            ChangePasswordPage.class)
        {
          /**
           * @see org.projectforge.web.MenuItemDef#isVisible(org.projectforge.web.MenuBuilderContext)
           */
          @Override
          protected boolean isVisible(final MenuBuilderContext context)
          {
            // The visibility of this menu entry is evaluated by the login handler implementation.
            final PFUserDO user = context.getLoggedInUser();
            return Login.getInstance().isPasswordChangeSupported(user);
          }
        });

    reg.register(
        new MenuItemDef(admin, MenuItemDefId.CHANGE_WLAN_PASSWORD.getId(), 32, MenuItemDefId.CHANGE_WLAN_PASSWORD.getI18nKey(),
            ChangeWlanPasswordPage.class)
        {
          @Override
          protected boolean isVisible(final MenuBuilderContext context)
          {
            // The visibility of this menu entry is evaluated by the login handler implementation.
            final PFUserDO user = context.getLoggedInUser();
            return Login.getInstance().isWlanPasswordChangeSupported(user);
          }
        });

    reg.register(new MenuItemDef(admin, MenuItemDefId.TENANT_LIST.getId(), 35, MenuItemDefId.TENANT_LIST.getI18nKey(),
        TenantListPage.class)
    {
      /**
       * @see org.projectforge.web.MenuItemDef#isVisible(org.projectforge.web.MenuBuilderContext)
       */
      @Override
      protected boolean isVisible(final MenuBuilderContext context)
      {
        final PFUserDO user = context.getLoggedInUser();
        return TenantChecker.isSuperAdmin(user);
      }
    });
    // reg.register(admin, MenuItemDefId.TENANT_LIST, 35, TenantListPage.class, TenantDao.USER_RIGHT_ID, READONLY_READWRITE);
    reg.register(admin, MenuItemDefId.USER_LIST, 40, UserListPage.class);
    reg.register(admin, MenuItemDefId.GROUP_LIST, 50, GroupListPage.class); // Visible for all.
    reg.register(admin, MenuItemDefId.ACCESS_LIST, 60, AccessListPage.class); // Visible for all.
    reg.register(admin, MenuItemDefId.SYSTEM, 70, AdminPage.class, ADMIN_GROUP);
    // Only available in development mode or if SQL console is configured in SecurityConfig.
    reg.register(admin, MenuItemDefId.SQL_CONSOLE, 71, SqlConsolePage.class, ADMIN_GROUP);
    reg.register(admin, MenuItemDefId.GROOVY_CONSOLE, 72, GroovyConsolePage.class, ADMIN_GROUP);
    reg.register(admin, MenuItemDefId.LUCENE_CONSOLE, 72, LuceneConsolePage.class, ADMIN_GROUP);
    reg.register(admin, MenuItemDefId.SYSTEM_UPDATE, 80, SystemUpdatePage.class, ADMIN_GROUP);
    reg.register(admin, MenuItemDefId.SYSTEM_STATISTICS, 90, SystemStatisticsPage.class);
    reg.register(admin, MenuItemDefId.CONFIGURATION, 100, ConfigurationListPage.class, ADMIN_GROUP);
    reg.register(admin, MenuItemDefId.PLUGIN_ADMIN, 110, PluginListPage.class, ADMIN_GROUP);

    reg.refresh();
  }

  public List<MenuItemDef> getFavoritesItemList()
  {
    return favoritesItemList;
  }

}
