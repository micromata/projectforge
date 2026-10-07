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

package org.projectforge.business.test

import javax.sql.DataSource

/**
 * Test cases don't run Flyway (the schema is created by hbm2ddl), but the file store tables (schema pf_files) are
 * plain JDBC without JPA entities. So the HSQLDB Flyway script is executed here.
 */
object PfFilesTestSchema {
    private const val SCRIPT = "/flyway/migrate/hsqldb/V8.0.37__RELEASE-PfFiles.sql"

    @JvmStatic
    fun ensure(dataSource: DataSource) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PF_FILES' AND TABLE_NAME='T_ATTACHMENT'"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    rs.next()
                    if (rs.getInt(1) > 0) {
                        return // Already created.
                    }
                }
            }
            val script = PfFilesTestSchema::class.java.getResource(SCRIPT)?.readText()
                ?: throw IllegalStateException("Flyway script '$SCRIPT' not found in classpath.")
            script.lines()
                .filterNot { it.trimStart().startsWith("--") }
                .joinToString("\n")
                .split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { sql ->
                    conn.createStatement().use { it.execute(sql) }
                }
        }
    }
}
