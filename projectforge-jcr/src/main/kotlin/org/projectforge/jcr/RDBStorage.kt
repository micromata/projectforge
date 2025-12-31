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

package org.projectforge.jcr

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.jcr.Jcr
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDocumentNodeStoreBuilder
import javax.sql.DataSource


private val log = KotlinLogging.logger {}

/**
 * UNDER CONSTRUCTION!
 *
 * JCR repository with RDB storage (PostgreSQL is tested).
 *
 * #### Preparation of the database (PostgreSQL):
 * ```
 * CREATE DATABASE projectforge_jcr;
 * GRANT ALL PRIVILEGES ON DATABASE projectforge_jcr TO projectforge;
 * ```
 *
 * #### Important note:
 * Any existing segment store will not be migrated when switching to RDB storage! You have to export and import the data manually.
 *
 * @param mainNodeName The name of the top node.
 *
 */
internal class RDBStorage(
    mainNodeName: String,
    val dataSource: DataSource,
) : OakStorage(mainNodeName) {
    override fun afterSessionClosed() {
        // Nothing to do.
    }

    override fun shutdown() {
        log.info { "Shutting down jcr RDB repository '$mainNodeName'..." }
        shutdownChecksumScope()
        try {
            (nodeStore as? DocumentNodeStore)?.dispose()
            log.info { "Repository shutdown completed." }
        } catch (e: Exception) {
            log.error { "Error during repository shutdown: ${e.message}" }
        }
        if (dataSource is HikariDataSource) {
            dataSource.close()
        }
    }

    override fun cleanup() {
        log.info { "Cleanup job invoked, no action required for RDB repository." }
    }

    internal val isRepositoryInitialized: Boolean
        get() {
            val connection = dataSource.connection ?: return false
            try {
                val stmt = connection.prepareStatement("SELECT ID FROM NODES WHERE ID like '%/jcr:system'")
                val resultSet = stmt.executeQuery()
                return resultSet.next()
            } finally {
                connection.close()
            }
        }


    init {
        if (mainNodeName.isBlank()) {
            throw IllegalArgumentException("Top node shouldn't be empty!")
        }
        log.info { "Initializing JCR repository with main node '$mainNodeName'..." }

        nodeStore = RDBDocumentNodeStoreBuilder()
            .setRDBConnection(dataSource)
            //.setClusterId(1)
            .build()
        val oak = Oak(nodeStore)
        val jcr = Jcr(oak)
        repository = jcr.createRepository()
        initRepository()
    }
}
