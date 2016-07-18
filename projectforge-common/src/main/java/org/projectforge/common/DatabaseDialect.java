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

package org.projectforge.common;

public enum DatabaseDialect
{
  PostgreSQL("org.hibernate.dialect.PostgreSQLDialect"), HSQL("org.hibernate.dialect.HSQLDialect");
  // Not yet supported:
  // MYSQL, ORACLE, MS_SQL_SERVER, DB2, INFORMIX, DERBY, UNKOWN;

  private String asString;

  public static DatabaseDialect fromString(final String asString)
  {
    if (PostgreSQL.asString.equals(asString) == true) {
      return PostgreSQL;
    }
    if (HSQL.asString.equals(asString) == true) {
      return HSQL;
    }
    return null;
  }

  /**
   * @return the asString
   */
  public String getAsString()
  {
    return asString;
  }

  private DatabaseDialect(final String asString)
  {
    this.asString = asString;
  }
}
