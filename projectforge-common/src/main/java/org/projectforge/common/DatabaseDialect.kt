/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.common

enum class DatabaseDialect(
  /**
   * @return the asString
   */
  // Not yet supported:
  // MYSQL, ORACLE, MS_SQL_SERVER, DB2, INFORMIX, DERBY, UNKOWN;
  val asString: String
) {
  PostgreSQL("org.hibernate.dialect.PostgreSQLDialect"), HSQL("org.hibernate.dialect.HSQLDialect");

  companion object {
    @JvmStatic
    fun fromString(asString: String): DatabaseDialect? {
      if (PostgreSQL.asString == asString) {
        return PostgreSQL
      }
      return if (HSQL.asString == asString) {
        HSQL
      } else null
    }

    fun getFlywayVendorName(databaseProductName: String): String {
      return if (databaseProductName.contains("hsql", ignoreCase = true)) {
        "hsqldb"
      } else {
        "postgresql"
      }
    }
  }
}
