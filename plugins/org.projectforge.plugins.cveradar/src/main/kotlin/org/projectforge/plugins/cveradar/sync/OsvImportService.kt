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
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.projectforge.plugins.cveradar.CvrMatchingService
import org.projectforge.plugins.cveradar.CvrOsvAdvisoryService
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Imports the advisories of OSV.dev for the libraries of the configured ecosystems (IMP-13), per ecosystem with a
 * watermark of its own (import state `OSV:<ecosystem>`):
 * - the first run reads all advisories of the ecosystem (`all.zip`), the runs after the ones modified since the
 *   watermark (minus [OVERLAP], an advisory may appear in the bucket later than modified); many of them (more than
 *   [MAX_SINGLE]) are read from the zip again,
 * - idempotent: an advisory is stored once per OSV id and only read again if its modification time changed,
 * - the malicious packages only if configured ([CveRadarConfig.Osv.malicious]),
 * - an ecosystem failing doesn't stop the others; its watermark stays, so the next run tries again.
 *
 * Afterwards the changed advisories are matched against the inventory: those that match become vulnerabilities
 * ([CvrMatchingService.materializeAdvisories]).
 */
@Service
class OsvImportService {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var osvClient: OsvClient

    @Autowired
    private lateinit var osvService: CvrOsvAdvisoryService

    @Autowired
    private lateinit var matchingService: CvrMatchingService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    class Result {
        val stored = CvrOsvAdvisoryService.StoreResult()

        /** Malicious packages (if not configured), advisories without packages of the ecosystems or not readable. */
        var skipped = 0

        /** The ecosystems read from their zip. */
        val fullImports = mutableListOf<String>()

        override fun toString() = "$stored, skipped=$skipped" +
                if (fullImports.isEmpty()) "" else ", complete: ${fullImports.joinToString()}"
    }

    fun import(): Result = import(osvClient)

    @Synchronized
    internal fun import(source: OsvSource, now: Instant = Instant.now()): Result {
        val result = Result()
        val ecosystems = config.osv.ecosystemKeys
        var error: Exception? = null
        ecosystems.forEach { ecosystem ->
            val state = loadState(ecosystem)
            try {
                val watermark = state.lastModWatermark?.toInstant()
                val newest = if (watermark == null) {
                    importAll(source, ecosystem, ecosystems.toSet(), result) ?: now
                } else {
                    importModified(source, ecosystem, ecosystems.toSet(), watermark, result)
                }
                newest?.let { state.lastModWatermark = Date.from(if (watermark != null && watermark > it) watermark else it) }
                state.lastError = null
            } catch (ex: Exception) {
                log.error(ex) { "OSV import of $ecosystem failed: ${ex.message}" }
                state.lastError = (ex.message ?: ex.javaClass.simpleName).take(4000)
                error = error ?: ex
            } finally {
                state.lastRun = Date.from(now)
                saveState(state)
            }
        }
        log.info { "OSV import finished: $result" }
        match(result)
        error?.let { throw it }
        return result
    }

    /** @return The latest modification time read, null if none. */
    private fun importAll(source: OsvSource, ecosystem: String, ecosystems: Set<String>, result: Result): Instant? {
        result.fullImports.add(ecosystem)
        var newest: Instant? = null
        val batch = mutableListOf<OsvAdvisory>()
        source.all(ecosystem, { !config.osv.malicious && it.startsWith(MALICIOUS) }) { json ->
            parse(json, ecosystems, result)?.let { advisory ->
                newest = maxOf(newest ?: advisory.modified, advisory.modified)
                batch.add(advisory)
            }
            if (batch.size >= BATCH) {
                osvService.store(batch, result.stored)
                batch.clear()
            }
        }
        osvService.store(batch, result.stored)
        return newest
    }

    /** @return The latest modification time listed, null if none. */
    private fun importModified(source: OsvSource, ecosystem: String, ecosystems: Set<String>, watermark: Instant, result: Result): Instant? {
        val modified = source.modifiedSince(ecosystem, watermark.minus(OVERLAP))
            .filter { config.osv.malicious || !it.second.startsWith(MALICIOUS) }
        if (modified.size > MAX_SINGLE) {
            return importAll(source, ecosystem, ecosystems, result)
        }
        val stored = osvService.modifiedByOsvId(modified.map { it.second })
        // The bucket gives nanoseconds, the database keeps milliseconds.
        val todo = modified.filter { (time, id) -> stored[id]?.toInstant() != time.truncatedTo(ChronoUnit.MILLIS) }
        todo.chunked(BATCH).forEach { chunk ->
            osvService.store(chunk.mapNotNull { (_, id) -> source.advisory(ecosystem, id)?.let { parse(it, ecosystems, result) } }, result.stored)
        }
        result.stored.unchanged += modified.size - todo.size
        return modified.maxOfOrNull { it.first }
    }

    private fun parse(json: String, ecosystems: Set<String>, result: Result): OsvAdvisory? {
        val advisory = try {
            OsvParser.parse(json, ecosystems)
        } catch (ex: Exception) {
            log.warn { "OSV advisory not readable: ${ex.message?.take(200)}" }
            null
        }
        // One without a package of the ecosystems is stored as well if known: it may have lost them, and its index is replaced.
        if (advisory == null || (advisory.malicious && !config.osv.malicious)) {
            result.skipped++
            return null
        }
        return advisory
    }

    /** A failing match must not fail the import: the next change of the inventory or the next import matches again. */
    private fun match(result: Result) {
        try {
            val vulnerabilityIds = matchingService.materializeAdvisories(result.stored.changedAdvisoryIds) + result.stored.vulnerabilityIds
            osvService.refresh(vulnerabilityIds)
            matchingService.rematchVulnerabilities(vulnerabilityIds)
        } catch (ex: Exception) {
            log.error(ex) { "Matching after the OSV import failed: ${ex.message}" }
        }
    }

    private fun loadState(ecosystem: String): CvrImportStateDO {
        val source = "$SOURCE_PREFIX$ecosystem"
        return persistenceService.find(CvrImportStateDO::class.java, source)
            ?: CvrImportStateDO().also { it.source = source }
    }

    private fun saveState(state: CvrImportStateDO) {
        persistenceService.runInTransaction { context -> context.em.merge(state) }
    }

    companion object {
        /** The import states are `OSV:Maven`, `OSV:npm`, ... */
        const val SOURCE_PREFIX = "OSV:"

        private const val MALICIOUS = "MAL-"
        private const val BATCH = 500

        /** More modified advisories than this are read from the zip of the ecosystem instead of one by one. */
        internal const val MAX_SINGLE = 2000

        internal val OVERLAP: Duration = Duration.ofDays(1)
    }
}
