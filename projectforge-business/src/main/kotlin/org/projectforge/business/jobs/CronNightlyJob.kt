/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.projectforge.common.extensions.formatMillis
import org.projectforge.framework.persistence.search.HibernateSearchReindexer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Job should be scheduled nightly.
 *
 * @author Florian Blumenstein
 * @author Kai Reinhard
 */
@Component
class CronNightlyJob {
    @Autowired
    private lateinit var hibernateSearchReindexer: HibernateSearchReindexer

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    //@Scheduled(cron = "0 30 2 * * *")
    @Scheduled(cron = "\${projectforge.cron.nightly}")
    fun execute() {
        val started = System.currentTimeMillis()
        log.info("Nightly job started.")
        coroutineScope.launch {
            try {
                hibernateSearchReindexer.execute()
            } catch (ex: Throwable) {
                log.error("While executing hibernate search re-index job: " + ex.message, ex)
            } finally {
                log.info("Nightly job job finished after ${(System.currentTimeMillis() - started).formatMillis()}.")
            }
        }
    }
}
