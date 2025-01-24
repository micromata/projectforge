/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDataSourceFactory
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
    repoService: RepoService,
) : OakStorage(mainNodeName) {
    private val dataSource: DataSource
    private val jdbcUrl = repoService.jdbcUrl // For log messages, only.

    override fun afterSessionClosed() {
        // Nothing to do.
    }

    override fun shutdown() {
        log.info { "Shutting down jcr RDB repository '$mainNodeName' (database='$jdbcUrl')..." }
        try {
            (nodeStore as? DocumentNodeStore)?.dispose()
            log.info { "Repository shutdown completed." }
        } catch (e: Exception) {
            log.error { "Error during repository shutdown: ${e.message}" }
        }
    }

    override fun cleanup() {
        log.info { "Cleanup job invoked, no action required for RDB repository." }
    }

    init {
        if (mainNodeName.isBlank()) {
            throw IllegalArgumentException("Top node shouldn't be empty!")
        }
        log.info { "Initializing JCR repository with main node '$mainNodeName' and database='${repoService.jdbcUrl}'..." }

        dataSource =
            RDBDataSourceFactory.forJdbcUrl(repoService.jdbcUrl, repoService.jdbcUser, repoService.jdbcPassword)
        nodeStore = RDBDocumentNodeStoreBuilder().setRDBConnection(dataSource).build()
        initRepository()

        runInSession { session ->
            if (!session.rootNode.hasNode(mainNodeName)) {
                log.info { "Creating top level node '$mainNodeName'." }
                session.rootNode.addNode(mainNodeName)
            }
            session.save()
        }
    }
}
