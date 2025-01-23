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
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDataSourceFactory
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDocumentNodeStoreBuilder
import org.apache.jackrabbit.oak.spi.state.NodeStore
import javax.sql.DataSource


private val log = KotlinLogging.logger {}

/**
 * UNDER CONSTRUCTION!
 *
 * JCR repository with RDB storage (PostgreSQL is tested).
 * ```
 * CREATE DATABASE projectforge_jcr;
 * GRANT ALL PRIVILEGES ON DATABASE projectforge_jcr TO projectforge;
 * ```
 * @param mainNodeName The name of the top node.
 *
 */
internal class RDBStorage(
    mainNodeName: String,
) : OakStorage(mainNodeName) {
    override fun onLogout() {
    }

    override fun afterSessionClose() {
    }

    override fun shutdown() {
        log.info { "Shutting down jcr RDB repository '$mainNodeName'..." }
        nodeStore.let {
            //log.warn { "Method not yet implemented: ${it.javaClass}.dispose()" }
            /*if (it is DocumentNodeStore) {
              it.dispose()
            }*/
        }
    }

    override fun cleanup() {
        log.info { "Cleaning JCR repository up..." }
    }

    init {
        if (mainNodeName.isBlank()) {
            throw IllegalArgumentException("Top node shouldn't be empty!")
        }
        log.info { "Initializing JCR repository with main node '$mainNodeName'..." }

        /*        FileStoreBuilder.fileStoreBuilder(fileStoreLocation).build().let { fileStore ->
                    this.fileStore = fileStore
                    nodeStore = SegmentNodeStoreBuilders.builder(fileStore).build()
                    repository = Jcr(Oak(nodeStore)).createRepository()
                }*/

        runInSession { session ->
            if (!session.rootNode.hasNode(mainNodeName)) {
                log.info { "Creating top level node '$mainNodeName'." }
                session.rootNode.addNode(mainNodeName)
            }
            session.save()
        }
    }

    // Function to create a PostgreSQL DataSource
    fun createPostgresDataSource(): DataSource {
        // Configure the PostgreSQL DataSource
        return RDBDataSourceFactory.forJdbcUrl(
            "jdbc:postgresql://localhost:5432/your_database", // Replace with your DB URL
            "your_user", // Replace with your DB user
            "your_password" // Replace with your DB password
        )
    }

    // Function to create a NodeStore with PostgreSQL
    fun createPostgresNodeStore(): NodeStore {
        // Create a PostgreSQL DataSource
        val dataSource = createPostgresDataSource()

        // Build the DocumentNodeStore for PostgreSQL
        return RDBDocumentNodeStoreBuilder()//.newRDBDocumentNodeStoreBuilder()
            .setRDBConnection(dataSource).build()
    }

    // Initialize the Oak repository with PostgreSQL
    fun initializeOakWithPostgres(): Oak {
        // Create a NodeStore using the PostgreSQL setup
        val nodeStore = createPostgresNodeStore()

        // Initialize Oak with the NodeStore
        return Oak(nodeStore)
    }

    fun main() {
        // Initialize Oak repository
        val oak = initializeOakWithPostgres()

        // Print a simple log message
        println("Oak repository initialized with PostgreSQL")
    }
}
