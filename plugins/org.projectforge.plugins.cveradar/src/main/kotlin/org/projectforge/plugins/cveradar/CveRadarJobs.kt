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

import jakarta.annotation.PostConstruct
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.cveradar.sync.GraylogSyncService
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.projectforge.plugins.cveradar.sync.TenableSyncService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * The syncs of the external sources (EXT-01, EXT-04) and the import from the NVD (IMP-03), also startable in the jobs
 * monitor. Inactive while the plugin isn't activated or the source isn't configured. Every sync of a device source
 * also deletes the outdated devices (EXT-07).
 */
@Service
class CveRadarJobs {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    @Autowired
    private lateinit var graylogSyncService: GraylogSyncService

    @Autowired
    private lateinit var nvdImportService: NvdImportService

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @Autowired
    private lateinit var tenableSyncService: TenableSyncService

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(TENABLE_JOB, ::tenableSync) {
            inactiveReason() ?: if (!config.tenable.isConfigured()) "Tenable.sc isn't configured." else null
        }
        schedulerJobRunner.register(GRAYLOG_JOB, ::graylogSync) {
            inactiveReason() ?: if (!config.graylog.isConfigured()) "Graylog isn't configured." else null
        }
        schedulerJobRunner.register(NVD_JOB, ::nvdImport) {
            inactiveReason() ?: if (!config.nvd.enabled) "The NVD import isn't enabled." else null
        }
    }

    @Scheduled(cron = TENABLE_CRON)
    fun tenableSync() {
        schedulerJobRunner.runAsync(TENABLE_JOB) { run ->
            val summary = tenableSyncService.sync()
            run.note("$summary${deleteOutdated()}")
            if (summary.failedAssets > 0) {
                run.fail("${summary.failedAssets} assets failed, see the log.")
            }
        }
    }

    @Scheduled(cron = GRAYLOG_CRON)
    fun graylogSync() {
        schedulerJobRunner.runAsync(GRAYLOG_JOB) { run ->
            run.note("${graylogSyncService.sync()}${deleteOutdated()}")
        }
    }

    @Scheduled(cron = NVD_CRON)
    fun nvdImport() {
        schedulerJobRunner.runAsync(NVD_JOB) { run ->
            run.note(nvdImportService.import().toString())
        }
    }

    private fun deleteOutdated(): String {
        val deleted = deviceService.deleteOutdated(config.deviceRetentionDays)
        return if (deleted > 0) ", $deleted outdated devices deleted" else ""
    }

    private fun inactiveReason(): String? {
        return if (!pluginAdminService.isActive(PluginAdminService.PLUGIN_CVE_RADAR_ID)) "Plugin CVE-Radar isn't activated." else null
    }

    companion object {
        private const val TENABLE_CRON = "\${projectforge.plugin.cveradar.tenable.cron:0 40 5 * * *}"

        private const val GRAYLOG_CRON = "\${projectforge.plugin.cveradar.graylog.cron:0 15 * * * *}"

        private const val NVD_CRON = "\${projectforge.plugin.cveradar.nvd.cron:0 17 */2 * * *}"

        val TENABLE_JOB = SchedulerJobDefinition(
            "cveradar.tenable", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "tenableSync",
            SchedulerSchedule.Cron(TENABLE_CRON),
        )

        val GRAYLOG_JOB = SchedulerJobDefinition(
            "cveradar.graylog", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "graylogSync",
            SchedulerSchedule.Cron(GRAYLOG_CRON),
        )

        val NVD_JOB = SchedulerJobDefinition(
            "cveradar.nvd", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "nvdImport",
            SchedulerSchedule.Cron(NVD_CRON),
        )
    }
}
