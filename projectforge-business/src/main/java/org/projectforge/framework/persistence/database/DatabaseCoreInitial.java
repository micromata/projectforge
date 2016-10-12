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

package org.projectforge.framework.persistence.database;

import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.PersonalAddressDO;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.gantt.GanttChartDO;
import org.projectforge.business.humanresources.HRPlanningDO;
import org.projectforge.business.humanresources.HRPlanningEntryDO;
import org.projectforge.business.meb.ImportedMebEntryDO;
import org.projectforge.business.meb.MebEntryDO;
import org.projectforge.business.orga.ContractDO;
import org.projectforge.business.orga.PostausgangDO;
import org.projectforge.business.orga.PosteingangDO;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.user.UserXmlPreferencesDO;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.continuousdb.Table;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DatabaseCoreInitial
{
  public static final String CORE_REGION_ID = "ProjectForge";

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry(final DatabaseUpdateService dao)
  {
    final Class<?>[] doClasses = new Class<?>[] { //
        // First needed data-base objects:
        TenantDO.class, //
        PFUserDO.class, GroupDO.class, TaskDO.class, GroupTaskAccessDO.class, //
        AccessEntryDO.class, //

        // To create second:
        KontoDO.class, //

        // To create third:
        KundeDO.class, ProjektDO.class, //
        Kost1DO.class, Kost2ArtDO.class, Kost2DO.class, //
        AuftragDO.class, AuftragsPositionDO.class, //
        EingangsrechnungDO.class, EingangsrechnungsPositionDO.class, //
        RechnungDO.class, RechnungsPositionDO.class, //
        EmployeeDO.class, //
        EmployeeSalaryDO.class, //
        KostZuweisungDO.class, //

        // All the rest:
        AddressDO.class, PersonalAddressDO.class, //
        BookDO.class, //
        ConfigurationDO.class, //
        DatabaseUpdateDO.class, //
        BuchungssatzDO.class, //
        ContractDO.class, //
        GanttChartDO.class, //
        HRPlanningDO.class, HRPlanningEntryDO.class, //
        MebEntryDO.class, ImportedMebEntryDO.class, //
        PostausgangDO.class, //
        PosteingangDO.class, //
        ScriptDO.class, //
        TimesheetDO.class, //
        UserPrefDO.class, //
        UserPrefEntryDO.class, //
        UserRightDO.class, //
        UserXmlPreferencesDO.class //
    };

    return new UpdateEntryImpl(CORE_REGION_ID, "2013-04-25", "Adds all core tables T_*.")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        //        // Does the data-base tables already exist?
        //        if (dao.doTablesExist(HibernateEntities.CORE_ENTITIES) == false
        //            || dao.doTablesExist(HibernateEntities.HISTORY_ENTITIES) == false) {
        //          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        //        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (dao.doExist(new Table(PFUserDO.class)) == false
            && HibernateUtils.getDialect() == DatabaseDialect.PostgreSQL) {
          // User table doesn't exist, therefore schema should be empty. PostgreSQL needs sequence for primary keys:
          dao.createSequence("hibernate_sequence", true);
        }
        //        final SchemaGenerator schemaGenerator = new SchemaGenerator(dao).add(HibernateEntities.CORE_ENTITIES).add(
        //            HibernateEntities.HISTORY_ENTITIES);
        //  HIBERNATE5 no longer working
        //        final Table propertyDeltaTable = schemaGenerator.getTable(PropertyDelta.class);
        //        final TableAttribute attr = propertyDeltaTable.getAttributeByName("clazz");
        //        attr.setNullable(false).setType(TableAttributeType.VARCHAR).setLength(31); // Discriminator value is may-be not handled correctly by
        //        propertyDeltaTable.getAttributeByName("old_value").setLength(20000); // Increase length.
        //        propertyDeltaTable.getAttributeByName("new_value").setLength(20000); // Increase length.
        //        // continuous-db.
        //        final Table historyEntryTable = schemaGenerator.getTable(HistoryEntry.class);
        //        final TableAttribute typeAttr = historyEntryTable.getAttributeByName("type");
        //        typeAttr.setType(TableAttributeType.INT);
        //        schemaGenerator.createSchema();
        // TODO FB (RK) Spring. this doesn't work
        // dao.createMissingIndices();

        return UpdateRunningStatus.DONE;
      }
    };
  }
}
