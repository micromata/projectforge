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
import org.projectforge.plugins.cveradar.sync.DependencyTrackClient
import org.projectforge.plugins.cveradar.sync.EuvdImportService
import org.projectforge.plugins.cveradar.sync.GraylogSyncService
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.projectforge.plugins.cveradar.sync.OsvImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * The sync of the external source (EXT-04), the imports from the NVD (IMP-03), OSV.dev (IMP-13) and the EUVD (IMP-16)
 * and the probe of Dependency-Track (PF-CVR-DT-MIG 4.1), also startable in the jobs monitor. Inactive while the plugin isn't activated or the source isn't configured. The sync of the devices
 * also deletes the outdated devices (EXT-07).
 */
@Service
class CveRadarJobs {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var dependencyTrackClient: DependencyTrackClient

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    @Autowired
    private lateinit var euvdImportService: EuvdImportService

    @Autowired
    private lateinit var graylogSyncService: GraylogSyncService

    @Autowired
    private lateinit var nvdImportService: NvdImportService

    @Autowired
    private lateinit var osvImportService: OsvImportService

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(GRAYLOG_JOB, ::graylogSync) {
            inactiveReason() ?: if (!config.graylog.isConfigured()) "Graylog isn't configured." else null
        }
        schedulerJobRunner.register(NVD_JOB, ::nvdImport) {
            inactiveReason() ?: if (!config.nvd.enabled) "The NVD import isn't enabled." else null
        }
        schedulerJobRunner.register(OSV_JOB, ::osvImport) {
            inactiveReason() ?: if (!config.osv.enabled) "The OSV import isn't enabled." else null
        }
        schedulerJobRunner.register(EUVD_JOB, ::euvdImport) {
            inactiveReason() ?: if (!config.euvd.enabled) "The EUVD import isn't enabled." else null
        }
        schedulerJobRunner.register(DEPTRACK_PROBE_JOB, ::deptrackProbe) {
            inactiveReason() ?: if (!config.deptrack.isConfigured()) "Dependency-Track isn't configured." else null
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

    @Scheduled(cron = OSV_CRON)
    fun osvImport() {
        schedulerJobRunner.runAsync(OSV_JOB) { run ->
            run.note(osvImportService.import().toString())
        }
    }

    @Scheduled(cron = EUVD_CRON)
    fun euvdImport() {
        schedulerJobRunner.runAsync(EUVD_JOB) { run ->
            run.note(euvdImportService.import().toString())
        }
    }

    @Scheduled(cron = DEPTRACK_PROBE_CRON)
    fun deptrackProbe() {
        schedulerJobRunner.runAsync(DEPTRACK_PROBE_JOB) { run ->
            try {
                val result = dependencyTrackClient.probe()
                run.note("Dependency-Track ${result.version ?: "(no version received)"}")
                if (result.version.isNullOrBlank()) {
                    run.fail("Dependency-Track: no version received from /api/version.")
                }
                if (result.missingPermissions.isNotEmpty()) {
                    run.fail("The team of the API key misses the permissions ${result.missingPermissions.sorted().joinToString()}.")
                }
            } catch (ex: Exception) {
                // Recorded by the sync stats: the tile of the system dashboard shows it.
                run.fail(ex.message ?: ex.javaClass.simpleName)
            }
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
        private const val GRAYLOG_CRON = "\${projectforge.plugin.cveradar.graylog.cron:0 15 * * * *}"

        private const val NVD_CRON = "\${projectforge.plugin.cveradar.nvd.cron:0 17 */2 * * *}"

        private const val OSV_CRON = "\${projectforge.plugin.cveradar.osv.cron:0 47 */6 * * *}"

        // After the NVD (minute 17), so the EUVD supplements what the NVD brought.
        private const val EUVD_CRON = "\${projectforge.plugin.cveradar.euvd.cron:0 37 */2 * * *}"

        private const val DEPTRACK_PROBE_CRON = "\${projectforge.plugin.cveradar.deptrack.probeCron:0 */15 * * * *}"

        val GRAYLOG_JOB = SchedulerJobDefinition(
            "cveradar.graylog", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "graylogSync",
            SchedulerSchedule.Cron(GRAYLOG_CRON),
        )

        val NVD_JOB = SchedulerJobDefinition(
            "cveradar.nvd", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "nvdImport",
            SchedulerSchedule.Cron(NVD_CRON),
        )

        val EUVD_JOB = SchedulerJobDefinition(
            "cveradar.euvd", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "euvdImport",
            SchedulerSchedule.Cron(EUVD_CRON),
        )

        val DEPTRACK_PROBE_JOB = SchedulerJobDefinition(
            "cveradar.deptrack.probe", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "deptrackProbe",
            SchedulerSchedule.Cron(DEPTRACK_PROBE_CRON),
        )

        val OSV_JOB = SchedulerJobDefinition(
            "cveradar.osv", SchedulerJobArea.INTEGRATION, CveRadarJobs::class.java, "osvImport",
            SchedulerSchedule.Cron(OSV_CRON),
        )
    }
}
