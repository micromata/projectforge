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

public enum MenuItemDefId
{
  // Main menus in alphabetical order
  ADMINISTRATION("administration"), //
  COMMON("common"), //
  COST("fibu.kost"), //
  FIBU("fibu"), //
  MISC("misc"), //
  ORGA("orga"), //
  PROJECT_MANAGEMENT("projectmanagement"), //
  REPORTING("reporting"), //

  // Sub menus in alphabetical order:
  ACCESS_LIST("accessList"), //
  ACCOUNT_LIST("fibu.konten"), //
  ACCOUNTING_RECORD_LIST("fibu.buchungssaetze"), //
  ADDRESS_LIST("addressList"), //
  BANK_ACCOUNT_LIST("finance.bankAccounts"), //
  BOOK_LIST("bookList"), //
  CALENDAR("calendar"), //
  TEAMCALENDAR("plugins.teamcal"), //
  CHANGE_PASSWORD("changePassword"), //
  CHANGE_WLAN_PASSWORD("changeWlanPassword"), //
  CONFIGURATION("configuration"), //
  CONTACT_LIST("contactList"), //
  CONTRACTS("contracts"), //
  COST1_LIST("fibu.kost1"), //
  COST2_LIST("fibu.kost2"), //
  COST2_TYPE_LIST("fibu.kost2arten"), //
  CUSTOMER_LIST("fibu.kunden"), //
  DATEV_IMPORT("fibu.datevImport"), //
  DOCUMENTATION("documentation"), //
  EMPLOYEE_LIST("fibu.employees"), //
  EMPLOYEE_SALARY_LIST("fibu.employeeSalaries"), //
  GANTT("gantt"), //
  GROUP_LIST("groupList"), //
  HR_PLANNING_LIST("hrPlanningList"), //
  HR_VIEW("hrList"), //
  IMAGE_CROPPER("imageCropper"), //
  INBOX_LIST("orga.posteingang"), //
  VISITORBOOK("orga.visitorbook"), //
  INCOMING_INVOICE_LIST("fibu.eingangsrechnungen"), //
  MEB("meb"), //
  MONTHLY_EMPLOYEE_REPORT("monthlyEmployeeReport"), //
  TENANT_LIST("multiTenancy"), //
  MY_ACCOUNT("myAccount"), //
  MY_PREFERENCES("myPreferences"), //
  REPORT_OBJECTIVES("fibu.reporting.reportObjectives"), //
  ORDER_LIST("fibu.orderbook"), //
  OUTBOX_LIST("orga.postausgang"), //
  OUTGOING_INVOICE_LIST("fibu.rechnungen"), //
  PERSONAL_STATISTICS("personalStatistics"), //
  PHONE_CALL("phoneCall"), //
  PROJECT_LIST("fibu.projekte"), //
  SEND_SMS("sendSms"), //
  SCRIPT_LIST("scriptList"), //
  SCRIPTING("scripting"), //
  SEARCH("search"), //
  PACMAN("pacman"), //
  SPACE_LIST("spaceList"), //
  SQL_CONSOLE("sqlConsole"), //
  GROOVY_CONSOLE("groovyConsole"), // 
  LUCENE_CONSOLE("luceneConsole"), //
  PLUGIN_ADMIN("pluginAdmin"), //
  SYSTEM("system"), //
  SYSTEM_STATISTICS("systemStatistics"), //
  SYSTEM_UPDATE("systemUpdate"), //
  TASK_TREE("taskTree"), //
  TIMESHEET_LIST("timesheetList"), //
  USER_LIST("userList"), //
  HR("hr");

  private String i18nKey;

  /**
   * @return name().
   */
  public String getId()
  {
    return name();
  }

  /**
   * @return The i18n key ("menu.*").
   */
  public String getI18nKey()
  {
    return "menu." + i18nKey;
  }

  private MenuItemDefId(final String i18nKey)
  {
    this.i18nKey = i18nKey;
  }
}
