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

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.Session
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.model.CvrOsvAdvisoryDO
import org.projectforge.plugins.cveradar.model.CvrPurlMatchDO
import org.projectforge.plugins.cveradar.model.CvrSource
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.sync.OsvAdvisory
import org.projectforge.plugins.cveradar.sync.OsvAffected
import org.projectforge.plugins.cveradar.sync.OsvPackages
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

/**
 * The advisories of OSV.dev ([CvrOsvAdvisoryDO]) and the vulnerabilities they become (IMP-13):
 * - [store] writes the advisories of an import with their index of affected versions ([CvrPurlMatchDO]),
 * - [materialize] makes a vulnerability of the advisories of a key once they match the inventory, whatever their
 *   score: the threshold of the NVD import (IMP-10) doesn't hold for libraries the inventory has. It holds for a
 *   match that is only possible (no version, a jar listing without groupId): otherwise every advisory of an artifact
 *   name would become a vulnerability,
 * - a vulnerability made of advisories (status [STATUS_OSV]) is taken over by the NVD import as soon as the NVD
 *   modifies the CVE; until then [refresh] keeps it up to date with its advisories.
 */
@Service
class CvrOsvAdvisoryService {
    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    class StoreResult {
        var created = 0
        var updated = 0
        var unchanged = 0

        /** Not stored: no package of the ecosystems imported, and not known. */
        var skipped = 0

        /** The new and modified advisories, to be matched against the inventory. */
        val changedAdvisoryIds = mutableSetOf<Long>()

        /** The vulnerabilities the changed advisories belonged to or belong to now, to be matched again. */
        val vulnerabilityIds = mutableSetOf<Long>()

        internal fun add(other: StoreResult) {
            created += other.created
            updated += other.updated
            unchanged += other.unchanged
            skipped += other.skipped
            changedAdvisoryIds.addAll(other.changedAdvisoryIds)
            vulnerabilityIds.addAll(other.vulnerabilityIds)
        }

        override fun toString() = "created=$created, updated=$updated, unchanged=$unchanged"
    }

    /** The stored modification times by OSV id (no document loaded), for skipping the unchanged ones. */
    fun modifiedByOsvId(osvIds: Collection<String>): Map<String, Date> =
        persistenceService.executeQueryBatched(
            "select a.osvId, a.modified from CvrOsvAdvisoryDO a where a.osvId in :ids", Array<Any>::class.java, "ids", osvIds,
        ).associate { it[0] as String to it[1] as Date }

    /**
     * Stores the advisories in one transaction, the index replaced, the inserts sent in JDBC batches. An unchanged one
     * (same modification time) is skipped. An advisory is linked to the stored vulnerability of its key; if its key
     * changed (a GHSA got a CVE id), it leaves the vulnerability of the old key. [result] gets the counts and ids only
     * once the transaction is committed: a rolled back batch adds nothing to be matched.
     */
    @Synchronized
    fun store(advisories: List<OsvAdvisory>, result: StoreResult) {
        if (advisories.isEmpty()) return
        val batch = StoreResult()
        storeBatch(advisories, batch)
        result.add(batch)
    }

    private fun storeBatch(advisories: List<OsvAdvisory>, result: StoreResult) {
        // Only the changed ones are loaded: the entity holds the (large) document.
        val modifiedBefore = modifiedByOsvId(advisories.map { it.id })
        persistenceService.runInTransaction { context ->
            context.em.unwrap(Session::class.java).jdbcBatchSize = INSERT_BATCH
            val changedIds = advisories.filter { modifiedBefore[it.id]?.toInstant() != it.modified.truncatedTo(ChronoUnit.MILLIS) }
                .map { it.id }.filter { it in modifiedBefore }
            val stored = if (changedIds.isEmpty()) emptyMap() else context.executeQuery(
                "from CvrOsvAdvisoryDO a where a.osvId in :ids", CvrOsvAdvisoryDO::class.java,
                Pair("ids", changedIds), attached = true,
            ).associateBy { it.osvId!! }
            val vulnerabilityIds = context.executeQuery(
                "select v.cveId, v.id from CvrVulnerabilityDO v where v.cveId in :keys and v.deleted = false",
                Array<Any>::class.java, Pair("keys", advisories.map { it.vulnerabilityKey }.toSet()),
            ).associate { it[0] as String to it[1] as Long }
            advisories.forEach { advisory ->
                val existing = stored[advisory.id]
                // The bucket gives nanoseconds, the database keeps milliseconds.
                if (existing == null && modifiedBefore[advisory.id] != null) {
                    result.unchanged++
                    return@forEach
                }
                if (existing == null && advisory.affected.isEmpty() && advisory.withdrawn == null) {
                    result.skipped++
                    return@forEach
                }
                val dbObj = existing ?: CvrOsvAdvisoryDO()
                existing?.vulnerability?.id?.let { result.vulnerabilityIds.add(it) }
                copy(advisory, dbObj)
                dbObj.vulnerability = vulnerabilityIds[advisory.vulnerabilityKey]
                    ?.let { context.getReference(CvrVulnerabilityDO::class.java, it) }
                if (existing == null) {
                    context.insert(dbObj)
                    result.created++
                } else {
                    context.executeUpdate("delete from CvrPurlMatchDO m where m.advisory.id = :id", Pair("id", dbObj.id))
                    result.updated++
                }
                if (advisory.withdrawn == null) {
                    insertIndex(context, dbObj, advisory)
                }
                result.changedAdvisoryIds.add(dbObj.id!!)
                dbObj.vulnerability?.id?.let { result.vulnerabilityIds.add(it) }
            }
        }
    }

    private fun insertIndex(context: PfPersistenceContext, advisory: CvrOsvAdvisoryDO, osv: OsvAdvisory) {
        fun row(affected: OsvAffected) = CvrPurlMatchDO().also {
            it.advisory = advisory
            it.ecosystem = affected.ecosystem
            it.packageName = affected.packageName.take(500)
            it.shortName = affected.shortName?.take(255)
        }
        osv.affected.forEach { affected ->
            affected.ranges.forEach { range ->
                context.insert(row(affected).also {
                    it.introduced = range.introduced
                    it.fixed = range.fixed
                    it.lastAffected = range.lastAffected
                })
            }
            affected.versions.forEach { version -> context.insert(row(affected).also { it.exactVersion = version }) }
        }
    }

    /**
     * The vulnerabilities of the keys (matched by the inventory): a stored one gets the advisories of its key linked, a
     * missing one is created from them (unless all of them are withdrawn).
     * @param possibleOnly The keys matched as possible only, or by the advisories of a distribution only: a missing
     * vulnerability is created only from the threshold of the NVD import on ([reachesThreshold]).
     * @return The id of the vulnerability per key.
     */
    @Synchronized
    fun materialize(keys: Collection<String>, possibleOnly: Set<String> = emptySet()): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val keySet = keys.toSet()
        // In one query each (the first import of a large inventory has thousands of keys); the advisories only of the
        // keys without a vulnerability.
        val existingByKey = vulnerabilityDao.findByCveIds(keySet)
        val advisoriesByKey = advisoriesOf(keySet - existingByKey.keys)
        keySet.forEach { key ->
            val existing = existingByKey[key]
            val vulnerability = if (existing != null) {
                if (existing.deleted) return@forEach
                existing
            } else {
                val advisories = advisoriesByKey[key].orEmpty()
                if (advisories.none { it.withdrawn == null }) return@forEach
                val created = toVulnerability(advisories)
                if (key in possibleOnly && !reachesThreshold(created)) return@forEach
                vulnerabilityDao.insert(created, checkAccess = false)
                vulnerabilityDao.reindex(created)
                caseDao.linkToVulnerability(created)
                log.info { "Vulnerability ${created.cveId} created from the OSV advisories ${advisories.joinToString { it.osvId!! }}." }
                created
            }
            link(key, vulnerability.id!!)
            result[key] = vulnerability.id!!
        }
        return result
    }

    /**
     * Whether the vulnerability reaches [CveRadarConfig.Nvd.minCvss]: by its score, without one (CVSS 4 only) by the
     * lower bound of its severity.
     */
    private fun reachesThreshold(vulnerability: CvrVulnerabilityDO): Boolean {
        val score = vulnerability.cvssScore?.toDouble() ?: SEVERITY_FLOOR[vulnerability.severity] ?: return false
        return score >= config.nvd.minCvss
    }

    /**
     * After the NVD import stored a new vulnerability: the advisories of its CVE id belong to it, also those of a
     * vulnerability they were linked to before (an older key).
     * @return The ids of the vulnerabilities the advisories belonged to before, to be refreshed and matched again.
     */
    fun linkToVulnerability(vulnerability: CvrVulnerabilityDO): Set<Long> {
        val before = persistenceService.executeQuery(
            "select a.vulnerability.id from CvrOsvAdvisoryDO a where a.vulnerabilityKey = :key and a.vulnerability.id <> :id",
            Long::class.javaObjectType, Pair("key", vulnerability.cveId), Pair("id", vulnerability.id),
        ).toSet()
        link(vulnerability.cveId!!, vulnerability.id!!)
        return before
    }

    private fun link(key: String, vulnerabilityId: Long) {
        persistenceService.runInTransaction { context ->
            context.executeUpdate(
                "update CvrOsvAdvisoryDO a set a.vulnerability = :vulnerability where a.vulnerabilityKey = :key",
                Pair("vulnerability", context.getReference(CvrVulnerabilityDO::class.java, vulnerabilityId)), Pair("key", key),
            )
        }
    }

    /**
     * Updates the vulnerabilities made of advisories ([isFromOsv]) with their advisories; one without advisories any
     * more (all withdrawn, or they moved to another key) is marked rejected. The others belong to the NVD import, except
     * one without data yet ([CvrVulnerabilityDO.lacksDetails]): the advisories fill it until the NVD modifies it.
     */
    fun refresh(vulnerabilityIds: Collection<Long>) {
        if (vulnerabilityIds.isEmpty()) return
        persistenceService.executeQueryBatched(
            "from CvrVulnerabilityDO v where v.id in :ids and v.deleted = false", CvrVulnerabilityDO::class.java, "ids", vulnerabilityIds,
        ).filter { isFromOsv(it) || CvrVulnerabilityDO.lacksDetails(it) }.forEach { vulnerability ->
            val advisories = persistenceService.executeQuery(
                "from CvrOsvAdvisoryDO a where a.vulnerability.id = :id", CvrOsvAdvisoryDO::class.java, Pair("id", vulnerability.id),
            )
            if (advisories.isEmpty() || advisories.none { it.withdrawn == null }) {
                vulnerability.nvdStatus = STATUS_REJECTED
            } else {
                copy(advisories, vulnerability)
            }
            if (vulnerabilityDao.update(vulnerability, checkAccess = false) != EntityCopyStatus.NONE) {
                vulnerabilityDao.reindex(vulnerability)
            }
        }
    }

    /** The ids of the vulnerabilities (of [ids]) with at least one advisory. */
    fun idsWithAdvisories(ids: Collection<Long>): Set<Long> =
        if (ids.isEmpty()) emptySet() else persistenceService.executeQueryBatched(
            "select distinct a.vulnerability.id from CvrOsvAdvisoryDO a where a.vulnerability.id in :ids", Long::class.javaObjectType, "ids", ids,
        ).toSet()

    /** The ids of all vulnerabilities with at least one advisory. */
    fun idsWithAdvisories(): List<Long> = persistenceService.executeQuery(
        "select distinct a.vulnerability.id from CvrOsvAdvisoryDO a where a.vulnerability is not null", Long::class.javaObjectType,
    )

    /** All advisories of the key, the preferred one first (see [copy]). */
    fun advisoriesOf(key: String): List<CvrOsvAdvisoryDO> = advisoriesOf(listOf(key))[key].orEmpty()

    /** The advisories of the keys, the preferred first, in one query. */
    fun advisoriesOf(keys: Collection<String>): Map<String, List<CvrOsvAdvisoryDO>> =
        persistenceService.executeQueryBatched(
            "from CvrOsvAdvisoryDO a where a.vulnerabilityKey in :keys", CvrOsvAdvisoryDO::class.java, "keys", keys,
        ).groupBy { it.vulnerabilityKey!! }.mapValues { it.value.sortedWith(PREFERRED) }

    /** A vulnerability (not stored) made of the advisories of one key, e.g. for the report of an ad-hoc check. */
    fun toVulnerability(advisories: List<CvrOsvAdvisoryDO>): CvrVulnerabilityDO =
        CvrVulnerabilityDO().also { copy(advisories, it) }

    companion object {
        /** The status of a vulnerability made of OSV advisories, in the field of the NVD status. */
        const val STATUS_OSV = "OSV"

        private const val STATUS_REJECTED = "Rejected"
        private const val INSERT_BATCH = 200

        /** The lowest score of a severity (CVSS 3 ratings). */
        private val SEVERITY_FLOOR = mapOf("CRITICAL" to 9.0, "HIGH" to 7.0, "MEDIUM" to 4.0, "LOW" to 0.1)

        /** The sources of the data: the NVD once it modified the CVE, OSV for linked advisories, the EUVD for its id. */
        fun sourcesOf(vulnerability: CvrVulnerabilityDO, hasAdvisories: Boolean): List<CvrSource> = listOfNotNull(
            CvrSource.NVD.takeIf { vulnerability.nvdLastModified != null },
            CvrSource.OSV.takeIf { hasAdvisories },
            CvrSource.EUVD.takeIf { vulnerability.euvdId != null },
        )

        /** Not modified by the NVD yet: made of advisories (perhaps rejected since). */
        fun isFromOsv(vulnerability: CvrVulnerabilityDO): Boolean =
            vulnerability.nvdLastModified == null &&
                (vulnerability.nvdStatus == STATUS_OSV || vulnerability.nvdStatus == STATUS_REJECTED)

        /**
         * Not withdrawn, then GitHub (it rates the severity), then not a malicious package, then not of a distribution
         * (it describes its packages), then the latest.
         */
        private val PREFERRED = compareBy<CvrOsvAdvisoryDO>(
            { it.withdrawn != null }, { it.osvId?.startsWith("GHSA-") != true }, { it.malicious },
            { advisory -> OsvPackages.DISTROS.any { advisory.osvId?.startsWith(it.idPrefix) == true } },
        ).thenByDescending { it.modified }

        internal fun copy(osv: OsvAdvisory, dest: CvrOsvAdvisoryDO) {
            dest.osvId = osv.id
            dest.vulnerabilityKey = osv.vulnerabilityKey
            dest.modified = Date.from(osv.modified)
            dest.published = osv.published?.let { Date.from(it) }
            dest.withdrawn = osv.withdrawn?.let { Date.from(it) }
            dest.aliases = osv.aliases.joinToString(", ").take(1000).ifEmpty { null }
            // A distribution names the CVE only: its description as summary, the document isn't kept (see below).
            dest.summary = osv.summary?.takeIf { it.isNotEmpty() } ?: osv.details?.takeIf { osv.distro }?.take(1000)
            dest.severity = osv.severity
            dest.cvss31Vector = osv.cvss3Vector
            dest.cvss40Vector = osv.cvss4Vector
            dest.cwes = osv.cwes.joinToString(", ").take(1000).ifEmpty { null }
            dest.malicious = osv.malicious
            // The document isn't kept for the many malicious packages (their summary says it all) and the advisories of the
            // distributions (some ten thousand, large lists of versions and binaries): they decide on the packages, the
            // NVD describes the CVE. Less in the dump of the sources of a backup, too.
            dest.rawDocument = if (osv.malicious || osv.distro) null else gzip(osv.rawJson)
        }

        /** The fields of the vulnerability from its advisories: the preferred one, the earliest publication. */
        internal fun copy(advisories: List<CvrOsvAdvisoryDO>, dest: CvrVulnerabilityDO) {
            val sorted = advisories.sortedWith(PREFERRED)
            val main = sorted.first()
            dest.cveId = main.vulnerabilityKey
            dest.nvdStatus = if (sorted.all { it.withdrawn != null }) STATUS_REJECTED else STATUS_OSV
            dest.published = sorted.mapNotNull { it.published }.minOrNull()
            dest.nvdLastModified = null
            dest.description = descriptionOf(main)?.take(CvrVulnerabilityDO.DESCRIPTION_LENGTH)
            val vector = sorted.firstNotNullOfOrNull { it.cvss31Vector }
            val score = Cvss3.baseScore(vector)
            dest.cvss31Vector = vector
            dest.cvss31Score = score
            dest.cvss31Severity = score?.let { Cvss3.severity(it) }
            dest.cvss40Vector = sorted.firstNotNullOfOrNull { it.cvss40Vector }
            dest.cvss40Score = null
            dest.cvss40Severity = null
            dest.cvssScore = score
            // A malicious package is critical: whoever installed it runs the code of an attacker.
            dest.severity = sorted.firstNotNullOfOrNull { it.severity } ?: dest.cvss31Severity
                ?: if (main.malicious) "CRITICAL" else null
            dest.cwes = sorted.flatMap { it.cwes?.split(',').orEmpty() }.map { it.trim() }.filter { it.isNotEmpty() }
                .distinct().joinToString(", ").take(CvrVulnerabilityDO.CWES_LENGTH).ifEmpty { null }
            dest.kevAdded = null
            dest.rawDocument = main.rawDocument
        }

        /** The summary and the details of the OSV document (Markdown), or the summary only. */
        private fun descriptionOf(advisory: CvrOsvAdvisoryDO): String? {
            val details = advisory.rawDocument?.let { raw ->
                runCatching {
                    val json = GZIPInputStream(raw.inputStream()).use { String(it.readNBytes(MAX_RAW_BYTES), Charsets.UTF_8) }
                    OBJECT_MAPPER.readTree(json).path("details").takeIf { it.isTextual }?.asText()?.trim()
                }.getOrNull()
            }
            return listOfNotNull(advisory.summary, details?.takeIf { it.isNotEmpty() }).joinToString("\n\n").ifEmpty { null }
        }

        private fun gzip(text: String): ByteArray {
            val out = ByteArrayOutputStream()
            GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            return out.toByteArray()
        }

        private const val MAX_RAW_BYTES = 5_000_000
        private val OBJECT_MAPPER = ObjectMapper()
    }
}
