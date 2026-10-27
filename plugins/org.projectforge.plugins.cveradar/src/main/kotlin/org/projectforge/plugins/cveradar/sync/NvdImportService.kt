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


package org.projectforge.plugins.cveradar.sync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.CvrVulnerabilityDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

/**
 * Imports the vulnerabilities modified since the last run from the NVD (PF-CVR-IMP):
 * - incremental: the range from the watermark of the last run until now, split into pieces of at most 120 days
 *   (IMP-02); the first run starts [CveRadarConfig.Nvd.initialDays] back (IMP-09),
 * - idempotent: a vulnerability is stored once per CVE id and updated afterwards (IMP-06),
 * - the watermark is advanced after each completely imported piece, so an aborted run resumes there,
 * - new vulnerabilities only from [CveRadarConfig.Nvd.minCvss] on (IMP-10); one not rated yet is skipped, it is
 *   modified when the NVD rates it and imported by the run after; a stored one is always updated, also when
 *   rejected (IMP-15) or rated lower.
 */
@Service
class NvdImportService {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var nvdApiClient: NvdApiClient

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    class Result {
        var created = 0
        var updated = 0
        var unchanged = 0

        /** Below the threshold or not rated yet. */
        var skipped = 0

        override fun toString() = "created=$created, updated=$updated, unchanged=$unchanged, skipped=$skipped"
    }

    fun import(): Result = import(nvdApiClient)

    @Synchronized
    internal fun import(source: NvdCveSource, now: Instant = Instant.now()): Result {
        val state = loadState()
        val result = Result()
        try {
            val start = state.lastModWatermark?.toInstant() ?: now.minus(Duration.ofDays(config.nvd.initialDays.toLong()))
            splitRange(start, now).forEach { (pieceStart, pieceEnd) ->
                importRange(source, pieceStart, pieceEnd, result)
                state.lastModWatermark = Date.from(pieceEnd)
                saveState(state)
            }
            state.lastError = null
            log.info { "NVD import finished: $result" }
            return result
        } catch (ex: Exception) {
            state.lastError = (ex.message ?: ex.javaClass.simpleName).take(4000)
            throw ex
        } finally {
            state.lastRun = Date.from(now)
            saveState(state)
        }
    }

    private fun importRange(source: NvdCveSource, start: Instant, end: Instant, result: Result) {
        var startIndex = 0
        while (true) {
            val page = source.fetch(start, end, startIndex)
            page.cves.forEach { store(it, result) }
            startIndex += page.cves.size
            if (page.cves.isEmpty() || startIndex >= page.totalResults) {
                return
            }
        }
    }

    private fun store(cve: NvdCve, result: Result) {
        val existing = vulnerabilityDao.findByCveId(cve.cveId)
        if (existing == null) {
            val score = cve.rating?.score
            if (score == null || score < BigDecimal.valueOf(config.nvd.minCvss)) {
                result.skipped++
                return
            }
            val vulnerability = CvrVulnerabilityDO()
            copy(cve, vulnerability)
            vulnerabilityDao.insert(vulnerability, checkAccess = false)
            vulnerabilityDao.reindex(vulnerability)
            result.created++
            return
        }
        if (cve.lastModified != null && existing.nvdLastModified?.toInstant() == cve.lastModified) {
            result.unchanged++ // The NVD changes lastModified with every change of the document.
            return
        }
        copy(cve, existing)
        if (vulnerabilityDao.update(existing, checkAccess = false) == EntityCopyStatus.NONE) {
            result.unchanged++
        } else {
            vulnerabilityDao.reindex(existing)
            result.updated++
        }
    }

    private fun loadState(): CvrImportStateDO {
        return persistenceService.find(CvrImportStateDO::class.java, SOURCE)
            ?: CvrImportStateDO().also { it.source = SOURCE }
    }

    private fun saveState(state: CvrImportStateDO) {
        persistenceService.runInTransaction { context -> context.em.merge(state) }
    }

    companion object {
        const val SOURCE = "NVD"

        /** The NVD answers at most 120 days per request. */
        internal val MAX_RANGE: Duration = Duration.ofDays(120)

        internal fun splitRange(start: Instant, end: Instant): List<Pair<Instant, Instant>> {
            val result = mutableListOf<Pair<Instant, Instant>>()
            var pieceStart = start
            while (pieceStart < end) {
                val pieceEnd = minOf(pieceStart.plus(MAX_RANGE), end)
                result.add(pieceStart to pieceEnd)
                pieceStart = pieceEnd
            }
            return result
        }

        internal fun copy(cve: NvdCve, dest: CvrVulnerabilityDO) {
            val max = cve.rating
            dest.cveId = cve.cveId
            dest.nvdStatus = cve.status
            dest.published = cve.published?.let { Date.from(it) }
            dest.nvdLastModified = cve.lastModified?.let { Date.from(it) }
            dest.description = cve.description?.take(CvrVulnerabilityDO.DESCRIPTION_LENGTH)
            dest.cvssScore = max?.score
            dest.severity = max?.severity
            dest.cvss31Score = cve.cvss3?.score
            dest.cvss31Severity = cve.cvss3?.severity
            dest.cvss31Vector = cve.cvss3?.vector
            dest.cvss40Score = cve.cvss4?.score
            dest.cvss40Severity = cve.cvss4?.severity
            dest.cvss40Vector = cve.cvss4?.vector
            dest.cwes = cve.cwes.joinToString(", ").take(CvrVulnerabilityDO.CWES_LENGTH).ifEmpty { null }
            dest.rawDocument = gzip(cve.rawJson)
        }

        private fun gzip(text: String): ByteArray {
            val out = ByteArrayOutputStream()
            GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            return out.toByteArray()
        }
    }
}
