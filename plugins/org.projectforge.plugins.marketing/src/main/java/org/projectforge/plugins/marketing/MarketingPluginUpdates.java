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

package org.projectforge.plugins.marketing;

import org.projectforge.continuousdb.*;
import org.projectforge.framework.persistence.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the initial data-base set-up script and later all update scripts if any data-base schema updates are
 * required by any later release of this to-do plugin. <br/>
 * This is a part of the convenient auto update functionality of ProjectForge. You only have to insert update methods
 * here for any further release (with e. g. required data-base modifications). ProjectForge will do the rest.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MarketingPluginUpdates
{
  static DatabaseService dao;

  final static Class<?>[] doClasses = new Class<?>[] { //
      AddressCampaignDO.class, AddressCampaignValueDO.class };

  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<>();
    // /////////////////////////////////////////////////////////////////
    // 5.1
    // /////////////////////////////////////////////////////////////////
    list.add(new UpdateEntryImpl(
        MarketingPlugin.ADDRESS_CAMPAIGN_ID,
        "5.2",
        "2013-05-13",
        "Renames T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE.values to s_values.")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        if (dao.doTableAttributesExist(AddressCampaignDO.class, "values")) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (!dao.doTableAttributesExist(AddressCampaignDO.class, "values")) {
          dao.renameTableAttribute(new Table(AddressCampaignDO.class).getName(), "values", "s_values");
        }
        return UpdateRunningStatus.DONE;
      }
    });
    return list;
  }

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(MarketingPlugin.ADDRESS_CAMPAIGN_ID, "2011-11-24", "Adds tables T_PLUGIN_MARKETING_*.")
    {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base table already exist?
        // Check only the oldest table.
        if (dao.doTablesExist(AddressCampaignDO.class)) {
          return UpdatePreCheckStatus.ALREADY_UPDATED;
        } else {
          // The oldest table doesn't exist, therefore the plug-in has to initialized completely.
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        // Create initial data-base table:
        new SchemaGenerator(dao).add(doClasses).createSchema();
        dao.createMissingIndices();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
