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
import org.projectforge.plugins.cveradar.CvrCaseDao
import org.projectforge.plugins.cveradar.CvrMatchingService
import org.projectforge.plugins.cveradar.CvrOsvAdvisoryService
import org.projectforge.plugins.cveradar.CvrVulnerabilityDao
import org.projectforge.plugins.cveradar.Cvss3
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Imports the vulnerabilities of the EUVD of the ENISA (PF-CVR IMP-16) as a supplement to the NVD:
 * - the EUVD filters by the publication day, so every run reads the days since the watermark of the last run minus
 *   [CveRadarConfig.Euvd.rescanDays] again (a rating or a CVE id comes later); the first run starts
 *   [CveRadarConfig.Euvd.initialDays] back. The watermark is advanced after each week read completely,
 * - idempotent: a vulnerability is stored once, under its CVE id, or under its EUVD id if the EUVD knows no CVE id (yet),
 * - a vulnerability the NVD or OSV made gets the EUVD id, the EPSS and the day of exploitation, nothing else (only
 *   the CVSS rating, if it has none): the NVD stays the source of the details. One only the EUVD knows (status
 *   [STATUS_EUVD]) is kept up to date from the EUVD, until the NVD modifies the CVE and takes it over,
 * - new vulnerabilities only from [CveRadarConfig.Euvd.minCvss] on, one not rated yet is skipped,
 * - once a day all entries known to be exploited are read, to update the vulnerabilities stored already (no new ones),
 * - the EUVD has no CPE: its product names give index entries that match as possible only
 *   ([CvrMatchingService.replaceApproximateIndex]).
 */
@Service
class EuvdImportService {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var euvdApiClient: EuvdApiClient

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    @Autowired
    private lateinit var matchingService: CvrMatchingService

    @Autowired
    private lateinit var osvService: CvrOsvAdvisoryService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    class Result {
        var created = 0
        var updated = 0
        var unchanged = 0

        /** Below the threshold, not rated yet, or (exploited sweep) not stored. */
        var skipped = 0

        /** The sweep of the exploited entries ran. */
        var swept = false

        /** The new vulnerabilities and those with a changed index, to be matched against the inventory (MAT-01). */
        internal val changedIds = mutableSetOf<Long>()

        override fun toString() = "created=$created, updated=$updated, unchanged=$unchanged, skipped=$skipped" +
                if (swept) ", exploited entries read" else ""
    }

    fun import(): Result = import(euvdApiClient)

    @Synchronized
    internal fun import(source: EuvdSource, now: Instant = Instant.now()): Result {
        val state = loadState(SOURCE)
        val result = Result()
        val today = LocalDate.ofInstant(now, ZoneOffset.UTC)
        try {
            runImport(source, state, now, today, result)
            sweepExploited(source, now, result)
        } finally {
            match(result) // Also after a failing piece: the pieces imported before it are matched.
        }
        return result
    }

    private fun runImport(source: EuvdSource, state: CvrImportStateDO, now: Instant, today: LocalDate, result: Result) {
        try {
            val start = state.lastModWatermark?.let { LocalDate.ofInstant(it.toInstant(), ZoneOffset.UTC).minusDays(config.euvd.rescanDays.toLong()) }
                ?: today.minusDays(config.euvd.initialDays.toLong())
            splitRange(start, today).forEach { (from, to) ->
                // No minimum score in the request: a stored vulnerability is updated whatever the EUVD rates it ([create] checks it).
                importRange(source, from, to, true, null, result)
                // Everything published before the end of that week is read; the days after the watermark are read again anyway.
                state.lastModWatermark = Date.from(minOf(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC), now))
                saveState(state)
            }
            state.lastError = null
            log.info { "EUVD import finished: $result" }
        } catch (ex: Exception) {
            state.lastError = (ex.message ?: ex.javaClass.simpleName).take(4000)
            throw ex
        } finally {
            state.lastRun = Date.from(now)
            saveState(state)
        }
    }

    /** Once a day: the entries known to be exploited, to update the vulnerabilities stored. A failure doesn't fail the import. */
    private fun sweepExploited(source: EuvdSource, now: Instant, result: Result) {
        val state = loadState(SWEEP_SOURCE)
        if (state.lastRun != null && Duration.between(state.lastRun!!.toInstant(), now) < SWEEP_INTERVAL) {
            return
        }
        try {
            importRange(source, null, null, false, null, result, exploited = true)
            result.swept = true
            state.lastError = null
            state.lastRun = Date.from(now)
        } catch (ex: Exception) {
            // lastRun stays: the next run (in a few hours) tries again.
            log.error(ex) { "EUVD: reading the exploited entries failed: ${ex.message}" }
            state.lastError = (ex.message ?: ex.javaClass.simpleName).take(4000)
        }
        saveState(state)
    }

    private fun importRange(
        source: EuvdSource, from: LocalDate?, to: LocalDate?, createNew: Boolean, minScore: Double?, result: Result,
        exploited: Boolean = false,
    ) {
        var page = 0
        var read = 0
        while (true) {
            val answer = source.search(from, to, minScore, exploited, page)
            store(answer.items, createNew, result)
            read += answer.items.size
            page++
            if (answer.items.isEmpty() || read >= answer.total) {
                return
            }
        }
    }

    private fun store(items: List<EuvdItem>, createNew: Boolean, result: Result) {
        if (items.isEmpty()) {
            return
        }
        // One query each, not one per entry. An entry without CVE id may be stored under a GHSA id (OSV) already.
        val byKey = vulnerabilityDao.findByCveIds(items.flatMap { listOf(it.key) + if (it.cveId == null) it.aliases else emptyList() }.toSet())
            .toMutableMap()
        val byEuvdId = vulnerabilityDao.findByEuvdIds(items.map { it.id })
        items.forEach { item ->
            val keyed = byKey[item.key]
            val existing = keyed ?: byEuvdId[item.id]
                ?: if (item.cveId == null) item.aliases.firstNotNullOfOrNull { byKey[it] } else null
            if (existing == null) {
                // The EUVD may list one CVE under several ids, also within a page.
                create(item, createNew, result)?.let { byKey[item.key] = it }
            } else {
                // The NVD made a record for a CVE the EUVD knew without it: the record of the EUVD id gives way (unique euvd_id).
                byEuvdId[item.id]?.takeIf { it !== existing && existing.euvdId == null }?.let { retire(it) }
                update(item, existing, result)
                existing.cveId?.let { byKey[it] = existing }
            }
        }
    }

    /** A duplicate made of an EUVD entry: it loses the EUVD id and is marked rejected. */
    private fun retire(duplicate: CvrVulnerabilityDO) {
        duplicate.euvdId = null
        duplicate.nvdStatus = "Rejected"
        vulnerabilityDao.update(duplicate, checkAccess = false)
        vulnerabilityDao.reindex(duplicate)
        matchingService.replaceApproximateIndex(duplicate.id!!, emptyList())
    }

    private fun create(item: EuvdItem, createNew: Boolean, result: Result): CvrVulnerabilityDO? {
        val score = item.score
        if (!createNew || score == null || score < BigDecimal.valueOf(config.euvd.minCvss)) {
            result.skipped++
            return null
        }
        val vulnerability = CvrVulnerabilityDO()
        vulnerability.cveId = item.key
        copyDetails(item, vulnerability)
        copySupplement(item, vulnerability)
        vulnerabilityDao.insert(vulnerability, checkAccess = false)
        vulnerabilityDao.reindex(vulnerability)
        caseDao.linkToVulnerability(vulnerability)
        // The advisories of OSV with this key belong to it now.
        osvService.linkToVulnerability(vulnerability).let { before ->
            osvService.refresh(before)
            result.changedIds.addAll(before)
        }
        index(item, vulnerability, result)
        result.changedIds.add(vulnerability.id!!)
        result.created++
        return vulnerability
    }

    private fun update(item: EuvdItem, existing: CvrVulnerabilityDO, result: Result) {
        if (existing.deleted) {
            result.unchanged++
            return
        }
        // The EUVD gave a CVE id to what it knew without one: the key follows (the NVD import then takes it over).
        val rekeyed = item.cveId != null && existing.cveId == item.id
        if (rekeyed) {
            existing.cveId = item.cveId
        }
        copySupplement(item, existing)
        if (isFromEuvd(existing)) {
            copyDetails(item, existing)
        } else if (existing.cvssScore == null) {
            copyRating(item, existing) // Not rated by the NVD (yet).
        }
        if (vulnerabilityDao.update(existing, checkAccess = false) == EntityCopyStatus.NONE) {
            result.unchanged++
        } else {
            vulnerabilityDao.reindex(existing)
            result.updated++
        }
        if (rekeyed) {
            caseDao.linkToVulnerability(existing)
            osvService.linkToVulnerability(existing).let { before ->
                osvService.refresh(before)
                result.changedIds.addAll(before)
            }
        }
        index(item, existing, result)
    }

    private fun index(item: EuvdItem, vulnerability: CvrVulnerabilityDO, result: Result) {
        if (vulnerability.nvdStatus.equals("Rejected", ignoreCase = true)) {
            return
        }
        if (matchingService.replaceApproximateIndex(vulnerability.id!!, item.products)) {
            result.changedIds.add(vulnerability.id!!)
        }
    }

    /** A failing match must not fail the import: the next change of the inventory or the next import matches again. */
    private fun match(result: Result) {
        try {
            matchingService.rematchVulnerabilities(result.changedIds)
        } catch (ex: Exception) {
            log.error(ex) { "Matching after the EUVD import failed: ${ex.message}" }
        }
    }

    private fun loadState(source: String): CvrImportStateDO {
        return persistenceService.find(CvrImportStateDO::class.java, source)
            ?: CvrImportStateDO().also { it.source = source }
    }

    private fun saveState(state: CvrImportStateDO) {
        persistenceService.runInTransaction { context -> context.em.merge(state) }
    }

    companion object {
        const val SOURCE = "EUVD"

        /** The import state of the daily read of the exploited entries (`source` has 20 characters at most). */
        const val SWEEP_SOURCE = "EUVD:exploited"

        /** The status of a vulnerability made of an EUVD entry, in the field of the NVD status. */
        const val STATUS_EUVD = "EUVD"

        private val SWEEP_INTERVAL: Duration = Duration.ofHours(23)

        /** Days per request series: the watermark is saved after each piece. */
        private const val PIECE_DAYS = 7L

        /** Not modified by the NVD yet and made of an EUVD entry. */
        internal fun isFromEuvd(vulnerability: CvrVulnerabilityDO): Boolean =
            vulnerability.nvdLastModified == null && vulnerability.nvdStatus == STATUS_EUVD

        internal fun splitRange(start: LocalDate, end: LocalDate): List<Pair<LocalDate, LocalDate>> {
            val result = mutableListOf<Pair<LocalDate, LocalDate>>()
            var from = start
            while (!from.isAfter(end)) {
                val to = minOf(from.plusDays(PIECE_DAYS - 1), end)
                result.add(from to to)
                from = to.plusDays(1)
            }
            return result
        }

        /** What the EUVD adds to every vulnerability. A value it doesn't give doesn't erase a stored one. */
        internal fun copySupplement(item: EuvdItem, dest: CvrVulnerabilityDO) {
            // The EUVD may list one CVE under several ids: the first one stays.
            if (dest.euvdId == null) {
                dest.euvdId = item.id
            }
            item.epss?.let { dest.epss = it }
            item.exploitedSince?.let { dest.exploitedSince = Date.from(it) }
        }

        /** The details of a vulnerability only the EUVD knows. */
        internal fun copyDetails(item: EuvdItem, dest: CvrVulnerabilityDO) {
            dest.nvdStatus = STATUS_EUVD
            dest.published = item.published?.let { Date.from(it) }
            dest.nvdLastModified = null
            dest.description = item.description?.take(CvrVulnerabilityDO.DESCRIPTION_LENGTH)
            copyRating(item, dest)
            dest.cwes = null
            dest.kevAdded = null
            dest.rawDocument = null
        }

        /** The CVSS rating of the entry, in the fields of the version it is of (3.x or 4.0). */
        internal fun copyRating(item: EuvdItem, dest: CvrVulnerabilityDO) {
            val score = item.score
            val severity = score?.let { Cvss3.severity(it) }
            dest.cvssScore = score
            dest.severity = severity
            dest.cvss31Score = score.takeIf { !item.cvss4 }
            dest.cvss31Severity = severity.takeIf { !item.cvss4 }
            dest.cvss31Vector = item.vector.takeIf { !item.cvss4 && score != null }
            dest.cvss40Score = score.takeIf { item.cvss4 }
            dest.cvss40Severity = severity.takeIf { item.cvss4 }
            dest.cvss40Vector = item.vector.takeIf { item.cvss4 && score != null }
        }
    }
}
