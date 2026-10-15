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


package org.projectforge.business.testdata

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.TimeZone

private val log = KotlinLogging.logger {}

/**
 * Fills a fresh database with fictional sample data (company "Example Consulting", user "Laura Becker", ...),
 * for test systems and the screenshots of the website. Replaces the former SQL script: the data is created
 * through the DAOs, so history, caches and the search index are built as in real use, and all dates are relative
 * to today.
 *
 * The areas are [TestDataContributor]s: the ones of the core run by [seedCore] during the setup, the ones of
 * plugins by [seedPlugins] after the plugins are activated (their migrations must have been run).
 */
@Service
open class TestDataSeeder {
    @Autowired
    private lateinit var contributors: List<TestDataContributor>

    @Autowired
    private lateinit var databaseService: DatabaseService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Value("\${projectforge.testdata.scale:1}")
    private var scale: Int = 1

    /**
     * Creates the admin user [DatabaseService.DEFAULT_ADMIN_USER] (renamed by the setup afterwards), the root
     * task, the ProjectForge groups and the sample data of the core.
     *
     * @param password The password of the sample users, null or blank for none.
     * @param onProgress Receives the progress, 0.0 to 1.0.
     * @return The context to pass to [seedPlugins].
     */
    open fun seedCore(password: String?, timeZone: TimeZone, onProgress: (Double) -> Unit = {}): TestDataContext {
        log.info { "Inserting test data (scale=$scale)..." }
        val admin = PFUserDO().also { it.username = DatabaseService.DEFAULT_ADMIN_USER }
        databaseService.initializeDefaultData(admin, timeZone)
        val ctx = TestDataContext(password?.takeIf { it.isNotBlank() }, scale.coerceAtLeast(1))
        ctx.users[admin.username!!] = admin
        run(ctx, contributors.filter { it.pluginId == null }, onProgress)
        return ctx
    }

    /**
     * Sample data of the active plugins.
     */
    open fun seedPlugins(ctx: TestDataContext, onProgress: (Double) -> Unit = {}) {
        run(ctx, contributors.filter { contributor ->
            contributor.pluginId?.let { pluginAdminService.isActive(it) } == true
        }, onProgress)
    }

    private fun run(ctx: TestDataContext, list: List<TestDataContributor>, onProgress: (Double) -> Unit) {
        val totalWeight = list.sumOf { it.weight }.coerceAtLeast(1).toDouble()
        var doneWeight = 0
        list.sortedBy { it.order }.forEach { contributor ->
            ctx.progressListener = { fraction -> onProgress((doneWeight + fraction * contributor.weight) / totalWeight) }
            val name = contributor::class.simpleName
            val started = System.currentTimeMillis()
            try {
                persistenceService.runInTransaction { _ -> contributor.seed(ctx) }
                // After the commit: users, groups and rights of this contributor are needed by the following ones.
                userGroupCache.forceReload()
                log.info { "Test data of $name inserted in ${System.currentTimeMillis() - started}ms." }
            } catch (ex: Exception) {
                // Logged, not rethrown: a test system with an area missing is more useful than a failed setup.
                log.error(ex) { "Can't insert test data of $name: ${ex.message}" }
                ctx.failed.add(name ?: contributor.toString())
            }
            doneWeight += contributor.weight
            onProgress(doneWeight / totalWeight)
        }
    }
}
