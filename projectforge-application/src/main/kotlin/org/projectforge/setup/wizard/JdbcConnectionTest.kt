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

package org.projectforge.setup.wizard

import org.postgresql.core.v3.ConnectionFactoryImpl
import java.sql.DriverManager
import java.util.logging.Level
import java.util.logging.Logger

object JdbcConnectionTest {
    fun testConnection(jdbcUrl: String, username: String, password: String): String? {
        Class.forName("org.postgresql.Driver")
        synchronized(this) {
            val saveLogLevelConnection = Logger.getLogger(ConnectionFactoryImpl::class.java.name).level
            val saveLogLevelDriver = Logger.getLogger("org.postgresql.Driver").level
            try {
                // Will avoid logs on console (otherwise lanterna teminal window might be overlayed and results in crap).
                Logger.getLogger(ConnectionFactoryImpl::class.java.name).level = Level.OFF
                Logger.getLogger("org.postgresql.Driver").level = Level.OFF
                val connection = DriverManager.getConnection(jdbcUrl, username, password)
                if (connection.isValid(10)) {
                    return Texts.JDBC_TESTRESULT_OK
                } else {
                    return Texts.JDBC_TESTRESULT_NOT_VALID
                }
            } catch (ex: Exception) {
                return "${Texts.JDBC_TESTRESULT_CONNECTION_FAILED}: ${ex.message}!"
            } finally {
                Logger.getLogger(ConnectionFactoryImpl::class.java.name).level = saveLogLevelConnection
                Logger.getLogger("org.postgresql.Driver").level = saveLogLevelDriver
            }
        }
    }

    const val defaultJdbcUrl = "jdbc:postgresql://localhost:15432/projectforge"
}
