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

package org.projectforge.jcr.store

import org.hsqldb.jdbc.JDBCDataSource
import java.io.File
import javax.sql.DataSource

/**
 * In-memory HSQLDB with the schema pf_files for the tests of the file store. The schema is created by the Flyway
 * script of projectforge-business (Flyway doesn't run in projectforge-jcr).
 */
object FileStoreTestDataSource {
    val dataSource: DataSource by lazy {
        val ds = JDBCDataSource()
        ds.setURL("jdbc:hsqldb:mem:fileStoreTest")
        ds.user = "sa"
        ds.setPassword("")
        val script = File("../projectforge-business/src/main/resources/flyway/migrate/hsqldb/V8.0.37__RELEASE-PfFiles.sql")
        ds.connection.use { conn ->
            script.readLines().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
                .split(';').map { it.trim() }.filter { it.isNotEmpty() }
                .forEach { sql -> conn.createStatement().use { it.execute(sql) } }
        }
        ds
    }
}
