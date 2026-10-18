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

package org.projectforge.business.fibu.numberrange

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Provides the configured number ranges ([NumberRangeConfig]). An inconsistent configuration stops the start.
 *
 * Without any configured range, the formerly built-in ranges 4 and 5 ([NumberRanges.legacyRanges]) apply only to an
 * installation that used them: one with Kost2 of projects in 4 or 5 ([legacyInUse]), built by the former code (or by
 * configured ranges 4/5). Otherwise customer and project numbers are free; cost units (`COST_CONFIGURED`) don't
 * change the ranges.
 */
@Service
open class NumberRangeService {
    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private lateinit var configured: NumberRanges

    private lateinit var legacy: NumberRanges

    @Volatile
    private var legacyInUseResult: Boolean? = null

    val numberRanges: NumberRanges
        get() = select(configured, legacy) { legacyInUse }

    /** Which ranges apply, for the frontend and the logs. */
    val mode: Mode
        get() = when {
            configured.isConfigured -> Mode.CONFIGURED
            legacyInUse -> Mode.LEGACY
            else -> Mode.FREE
        }

    /**
     * Whether Kost2 of projects in the former ranges 4 or 5 exist (deleted ones included). Evaluated once, on first
     * use: the former ranges can't be taken into use later, as new Kost2 of projects need a range (`Kost2Dao`). Not
     * stored; a failing query (e.g. database not ready yet) counts as false and is repeated on the next use.
     */
    private val legacyInUse: Boolean
        get() {
            legacyInUseResult?.let { return it }
            return try {
                val count = countLegacyKost2()
                (count > 0).also {
                    legacyInUseResult = it
                    if (it) {
                        log.info { "No number ranges configured, but $count Kost2 of projects in the former ranges 4/5: these ranges apply." }
                    } else {
                        log.info { "No number ranges configured and no Kost2 of projects in the former ranges 4/5: customer and project numbers are free." }
                    }
                }
            } catch (ex: Exception) {
                log.debug(ex) { "Can't detect the use of the former ranges 4/5 yet." }
                false
            }
        }

    /** The Kost2 of projects in the former ranges 4/5, deleted ones included (not cached, see [legacyInUse]). */
    internal fun countLegacyKost2(): Long =
        persistenceService.selectSingleResult(
            "SELECT COUNT(k.id) FROM Kost2DO k WHERE k.projekt IS NOT NULL AND k.nummernkreis IN (4, 5)",
            java.lang.Long::class.java,
        )?.toLong() ?: 0L

    @PostConstruct
    private fun postConstruct() {
        // Bound explicitly (not as @ConfigurationProperties bean), so the test context without Spring Boot gets it too:
        val config = Binder.get(environment).bind(NumberRangeConfig.PREFIX, NumberRangeConfig::class.java)
            .orElseGet { NumberRangeConfig() }
        configured = NumberRanges(config.ranges, config.allowFreeNumbers, config.defaultSegments)
        legacy = NumberRanges(NumberRanges.legacyRanges(), config.allowFreeNumbers, config.defaultSegments)
        if (configured.isConfigured) {
            log.info { "Number ranges: ${configured.ranges.joinToString()}, allowFreeNumbers=${configured.allowFreeNumbers}" }
        }
        instance = this
    }

    enum class Mode { CONFIGURED, LEGACY, FREE }

    companion object {
        private var instance: NumberRangeService? = null

        private val unconfigured = NumberRanges(emptyList())

        /**
         * The configured ranges if any, else the former ranges 4/5 if in use, else none (free numbers). [legacyInUse]
         * is only asked without configured ranges.
         */
        internal fun select(configured: NumberRanges, legacy: NumberRanges, legacyInUse: () -> Boolean): NumberRanges =
            if (configured.isConfigured || !legacyInUse()) configured else legacy

        /**
         * For static helpers (formatters, parsers) that can't be injected. Without Spring context (plain unit tests)
         * no range is configured.
         */
        @JvmStatic
        val current: NumberRanges
            get() = instance?.numberRanges ?: unconfigured
    }
}
