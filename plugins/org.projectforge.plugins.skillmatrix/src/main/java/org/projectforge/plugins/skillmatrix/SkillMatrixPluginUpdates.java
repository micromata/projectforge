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

package org.projectforge.plugins.skillmatrix;

import org.projectforge.continuousdb.*;
import org.projectforge.framework.persistence.database.DatabaseService;

/**
 * Contains the initial data-base set-up script and later all update scripts if any data-base schema updates are
 * required by any later release of this skillmatrix plugin.
 * 
 * @author Billy Duong (b.duong@micromata.de)
 */
public class SkillMatrixPluginUpdates
{
  static DatabaseService dao;

  final static Class<?>[] doClasses = new Class<?>[] { SkillDO.class, SkillRatingDO.class, TrainingDO.class,
      TrainingAttendeeDO.class };

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry()
  {
    return new UpdateEntryImpl(SkillMatrixPlugin.ID_SKILL_RATING, "2014-03-05",
        "Adds tables T_PLUGIN_SKILL and T_PLUGIN_SKILL_RATING and T_PLUGIN_SKILL_TRAINING and T_PLUGIN_SKILL_ATTENDEE.")
    {
      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base tables already exist?
        // Check only the oldest table.
        if (!dao.doTablesExist(doClasses)) {
          // The oldest table doesn't exist, therefore the plugin has to initialized completely.
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        new SchemaGenerator(dao).add(doClasses).createSchema();
        dao.createMissingIndices();
        return UpdateRunningStatus.DONE;
      }
    };
  }
}
