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


package org.projectforge.plugins.cveradar

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.projectforge.business.notification.NotificationRuleDao
import org.projectforge.business.notification.NotificationRuleType
import org.projectforge.business.notification.NotificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

/**
 * Runs the notification rules of new findings ([CvrFindingsRuleHandler]) right after a matching created findings, not
 * only by their schedule (once a day at most). The runs of a short time (the pages of an NVD import, the components of
 * a library upload) are collected into one, a little later and outside of the transaction of the matching.
 */
@Service
class CvrFindingNotifier {
    @Autowired
    private lateinit var notificationRuleDao: NotificationRuleDao

    @Autowired
    private lateinit var notificationService: NotificationService

    private val scheduled = AtomicBoolean(false)

    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "cveradar-finding-notifier").also { it.isDaemon = true }
    }

    /** Findings were created: the rules run in [DELAY_SECONDS], if not already planned. */
    fun findingsCreated() {
        if (scheduled.compareAndSet(false, true)) {
            executor.schedule({
                scheduled.set(false)
                runRules()
            }, DELAY_SECONDS, TimeUnit.SECONDS)
        }
    }

    /** Runs the active rules of new findings now; returns the number of notifications created. */
    fun runRules(): Int {
        var created = 0
        try {
            notificationRuleDao.selectActive().filter { it.ruleType == NotificationRuleType.CVERADAR_FINDINGS }.forEach { rule ->
                created += notificationService.trigger(rule).created
            }
        } catch (ex: Exception) {
            log.error(ex) { "Notification of new findings failed: ${ex.message}" }
        }
        return created
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    companion object {
        const val DELAY_SECONDS = 30L
    }
}
