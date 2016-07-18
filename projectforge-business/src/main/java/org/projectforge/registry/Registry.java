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

package org.projectforge.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.PersonalAddressDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.fibu.EingangsrechnungDao;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.EmployeeScriptingDao;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.business.fibu.PaymentScheduleDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.business.fibu.kost.BuchungssatzDao;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.fibu.kost.Kost1ScriptingDao;
import org.projectforge.business.fibu.kost.Kost2ArtDao;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.fibu.kost.KostZuweisungDao;
import org.projectforge.business.gantt.GanttChartDao;
import org.projectforge.business.humanresources.HRPlanningDao;
import org.projectforge.business.humanresources.HRPlanningEntryDO;
import org.projectforge.business.meb.MebDao;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.orga.ContractDao;
import org.projectforge.business.orga.PostausgangDao;
import org.projectforge.business.orga.PosteingangDao;
import org.projectforge.business.scripting.ScriptDao;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserPrefDao;
import org.projectforge.business.user.UserRightDao;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.persistence.DaoConst;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.context.ApplicationContext;

/**
 * Registry for dao's. Here you can register additional daos and plugins (extensions of ProjectForge).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Registry
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Registry.class);

  public static final Registry instance = new Registry();

  private static final Map<String, RegistryEntry> mapByName = new HashMap<String, RegistryEntry>();

  private static final Map<Class<? extends BaseDao<?>>, RegistryEntry> mapByDao = new HashMap<Class<? extends BaseDao<?>>, RegistryEntry>();

  private static final Map<Class<? extends BaseDO<?>>, RegistryEntry> mapByDO = new HashMap<Class<? extends BaseDO<?>>, RegistryEntry>();

  private static final List<RegistryEntry> orderedList = new ArrayList<RegistryEntry>();

  public static Registry getInstance()
  {
    return instance;
  }

  @SuppressWarnings("unchecked")
  public void init(ApplicationContext applicationContext)
  {
    register(DaoConst.CONFIGURATION, ConfigurationDao.class, applicationContext.getBean(ConfigurationDao.class),
        "administration.configuration")
            .setSearchable(false);
    register(DaoConst.TENANT, TenantDao.class, applicationContext.getBean(TenantDao.class), "tenant");
    register(DaoConst.USER, UserDao.class, applicationContext.getBean(UserDao.class), "user");
    register(DaoConst.GROUP, GroupDao.class, applicationContext.getBean(GroupDao.class), "group");
    register(DaoConst.TASK, TaskDao.class, applicationContext.getBean(TaskDao.class), "task"); // needs PFUserDO
    register(DaoConst.ACCESS, AccessDao.class, applicationContext.getBean(AccessDao.class), "access")
        .setNestedDOClasses(AccessEntryDO.class);

    register(DaoConst.ADDRESS, AddressDao.class, applicationContext.getBean(AddressDao.class), "address")
        .setNestedDOClasses(PersonalAddressDO.class);
    register(DaoConst.TIMESHEET, TimesheetDao.class, applicationContext.getBean(TimesheetDao.class), "timesheet") //
        .setSearchFilterClass(TimesheetFilter.class);
    register(DaoConst.BOOK, BookDao.class, applicationContext.getBean(BookDao.class), "book");

    register(DaoConst.CUSTOMER, KundeDao.class, applicationContext.getBean(KundeDao.class), "fibu.kunde");
    register(DaoConst.PROJECT, ProjektDao.class, applicationContext.getBean(ProjektDao.class), "fibu.projekt"); // Needs customer

    register(DaoConst.COST1, Kost1Dao.class, applicationContext.getBean(Kost1Dao.class), "fibu.kost1")
        .setScriptingDao(new Kost1ScriptingDao(applicationContext.getBean(Kost1Dao.class)));
    register(DaoConst.COST2_Type, Kost2ArtDao.class, applicationContext.getBean(Kost2ArtDao.class), "fibu.kost2art");
    register(DaoConst.COST2, Kost2Dao.class, applicationContext.getBean(Kost2Dao.class), "fibu.kost2"); // Needs kost2Art and project
    register(DaoConst.COST_ASSIGNMENT, KostZuweisungDao.class, applicationContext.getBean(KostZuweisungDao.class),
        "fibu.") // Needs kost, invoices, employee salaries
            .setFullTextSearchSupport(false).setSearchable(false);

    register(DaoConst.ORDERBOOK, AuftragDao.class, applicationContext.getBean(AuftragDao.class), "fibu.auftrag") // Needs customer, project
        .setNestedDOClasses(AuftragsPositionDO.class, PaymentScheduleDO.class);
    register(DaoConst.OUTGOING_INVOICE, RechnungDao.class, applicationContext.getBean(RechnungDao.class),
        "fibu.rechnung") // Needs customer, project
            .setNestedDOClasses(RechnungsPositionDO.class);
    register(DaoConst.INCOMING_INVOICE, EingangsrechnungDao.class,
        applicationContext.getBean(EingangsrechnungDao.class), "fibu.eingangsrechnung") //
            .setNestedDOClasses(EingangsrechnungsPositionDO.class);
    register(DaoConst.ACCOUNTING_RECORD, BuchungssatzDao.class, applicationContext.getBean(BuchungssatzDao.class),
        "fibu.buchungssatz")
            .setSearchable(false); // Need account, cost1
    // and cost2.
    register(DaoConst.ACCOUNT, KontoDao.class, applicationContext.getBean(KontoDao.class), "fibu.konto");
    register(DaoConst.EMPLOYEE, EmployeeDao.class, applicationContext.getBean(EmployeeDao.class), "fibu.employee")
        .setScriptingDao(new EmployeeScriptingDao(applicationContext.getBean(EmployeeDao.class)));
    register(DaoConst.EMPLOYEE_SALARY, EmployeeDao.class, applicationContext.getBean(EmployeeSalaryDao.class),
        "fibu.employee.salary")
            .setSearchable(false);

    register(DaoConst.CONTRACT, ContractDao.class, applicationContext.getBean(ContractDao.class),
        "legalAffaires.contract");
    register(DaoConst.OUTGOING_MAIL, PostausgangDao.class, applicationContext.getBean(PostausgangDao.class),
        "orga.postausgang");
    register(DaoConst.INCOMING_MAIL, PosteingangDao.class, applicationContext.getBean(PosteingangDao.class),
        "orga.posteingang");

    register(DaoConst.GANTT, GanttChartDao.class, applicationContext.getBean(GanttChartDao.class), "gantt");
    register(DaoConst.HR_PLANNING, HRPlanningDao.class, applicationContext.getBean(HRPlanningDao.class), "hr.planning") //
        .setNestedDOClasses(HRPlanningEntryDO.class).setSearchable(false);

    register(DaoConst.MEB, MebDao.class, applicationContext.getBean(MebDao.class), "meb");
    register(DaoConst.SCRIPT, ScriptDao.class, applicationContext.getBean(ScriptDao.class), "scripting")
        .setSearchable(false);
    register(DaoConst.USER_PREF, UserPrefDao.class, applicationContext.getBean(UserPrefDao.class)).setSearchable(false);
    register(DaoConst.USER_RIGHT, UserRightDao.class, applicationContext.getBean(UserRightDao.class))
        .setSearchable(false);

    register("teamCal", TeamCalDao.class, applicationContext.getBean(TeamCalDao.class), "plugins.teamcal");
    register("teamEvent", TeamEventDao.class, applicationContext.getBean(TeamEventDao.class), "plugins.teamcal.event");

  }

  /**
   * Registers a new dao, which is available
   * 
   * @param id
   * @param daoClassType
   * @param dao
   * @param i18nPrefix
   * @return
   */
  private RegistryEntry register(final String id, final Class<? extends BaseDao<?>> daoClassType,
      final BaseDao<?> dao,
      final String i18nPrefix)
  {
    if (dao == null) {
      log.error("Dao for '" + id + "' is null! Ignoring dao in registry.");
      return new RegistryEntry(null, null, null); // Create dummy.
    }
    final RegistryEntry entry = new RegistryEntry(id, daoClassType, dao, i18nPrefix);
    register(entry);
    log.debug("Dao '" + id + "' registerd.");
    return entry;
  }

  private RegistryEntry register(final String id, final Class<? extends BaseDao<?>> daoClassType,
      final BaseDao<?> dao)
  {
    return register(id, daoClassType, dao, null);
  }

  /**
   * Registers the given entry and appends it to the ordered list of registry entries.
   * 
   * @param entry The entry to register.
   * @return this for chaining.
   */
  public Registry register(final RegistryEntry entry)
  {
    Validate.notNull(entry);
    mapByName.put(entry.getId(), entry);
    mapByDao.put(entry.getDaoClassType(), entry);
    mapByDO.put(entry.getDOClass(), entry);
    orderedList.add(entry);
    return instance;
  }

  /**
   * Registers the given entry and inserts it to the ordered list of registry entries at the given position.
   * 
   * @param existingEntry A previous added entry, at which the new entry should be inserted.
   * @param insertBefore If true then the given entry will be added before the existing entry, otherwise after.
   * @param entry The entry to register.
   * @return this for chaining.
   */
  public Registry register(final RegistryEntry existingEntry, final boolean insertBefore, final RegistryEntry entry)
  {
    Validate.notNull(existingEntry);
    Validate.notNull(entry);
    mapByName.put(entry.getId(), entry);
    mapByDao.put(entry.getDaoClassType(), entry);
    mapByDO.put(entry.getDOClass(), entry);
    final int idx = orderedList.indexOf(existingEntry);
    if (idx < 0) {
      log.error("Registry entry '" + existingEntry.getId() + "' not found. Appending the given entry to the list.");
      orderedList.add(entry);
    } else if (insertBefore == true) {
      orderedList.add(idx, entry);
    } else {
      orderedList.add(idx + 1, entry);
    }
    return this;
  }

  public RegistryEntry getEntry(final String id)
  {
    return mapByName.get(id);
  }

  public RegistryEntry getEntry(final Class<? extends BaseDao<?>> daoClass)
  {
    return mapByDao.get(daoClass);
  }

  public RegistryEntry getEntryByDO(final Class<? extends BaseDO<?>> doClass)
  {
    return mapByDO.get(doClass);
  }

  /**
   * @return The list of entries in the order of their registration: the first registered entry is the first of the
   *         returned list etc.
   */
  public List<RegistryEntry> getOrderedList()
  {
    return orderedList;
  }

}
