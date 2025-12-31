/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.database;

import org.projectforge.common.DatabaseDialect;

/**
 * All database dialect specific implementations should be placed here.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DatabaseSupport
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseSupport.class);

  private static boolean errorMessageShown = false;

  private static DatabaseSupport instance;

  private final DatabaseDialect dialect;

  public static void setInstance(final DatabaseSupport instance)
  {
    DatabaseSupport.instance = instance;
  }

  public static DatabaseSupport getInstance()
  {
    return instance;
  }

  public DatabaseSupport(final DatabaseDialect dialect)
  {
    log.info("Setting database dialect to: " + dialect.getAsString());
    this.dialect = dialect;
  }

  public DatabaseDialect getDialect()
  {
    return dialect;
  }

  /**
   * Optimization for getting sum of durations. Currently only an optimization for PostgreSQL is implemented:
   * "extract(epoch from sum(toProperty - fromProperty))". <br/>
   * If no optimization is given, the caller selects all database entries and aggregates via Java the sum (full table
   * scan).
   *
   * @param fromProperty
   * @param toProperty
   * @return part of select string or null if for the used database no optimization is given.
   */
  public String getIntervalInSeconds(final String fromProperty, final String toProperty)
  {
    if (dialect == DatabaseDialect.PostgreSQL) {
      return "SUM(EXTRACT(EPOCH FROM " + toProperty + ") - EXTRACT(EPOCH FROM " + fromProperty + "))";
      // return "EXTRACT(EPOCH FROM SUM(" + toProperty + " - " + fromProperty + "))"; // Seconds since 1970
    } else {
      if (!errorMessageShown) {
        errorMessageShown = true;
        log.warn(
            "No database optimization implemented for the used database. Please contact the developer if you have an installation with more than 10.000 time sheet entries for increasing performance");
      }
      // No optimization for this database.
      return null;
    }
  }

  /**
   * Will be called on shutdown by WicketApplication.
   */
  public String getShutdownDatabaseStatement()
  {
    if (dialect == DatabaseDialect.HSQL) {
      return "SHUTDOWN COMPACT";
    } else {
      return null;
    }
  }
}
