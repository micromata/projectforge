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
import org.projectforge.framework.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Provides the configured number ranges ([NumberRangeConfig]). An inconsistent configuration stops the start.
 *
 * Without any configured range, the formerly built-in ranges 4 and 5 ([NumberRanges.legacyRanges]) apply as long as
 * cost units are configured (`COST_CONFIGURED`, may change at runtime), otherwise customer and project numbers are
 * free.
 */
@Service
open class NumberRangeService {
    @Autowired
    private lateinit var environment: Environment

    private lateinit var configured: NumberRanges

    private lateinit var legacy: NumberRanges

    val numberRanges: NumberRanges
        get() = if (configured.isConfigured || !isCostConfigured()) configured else legacy

    @PostConstruct
    private fun postConstruct() {
        // Bound explicitly (not as @ConfigurationProperties bean), so the test context without Spring Boot gets it too:
        val config = Binder.get(environment).bind(NumberRangeConfig.PREFIX, NumberRangeConfig::class.java)
            .orElseGet { NumberRangeConfig() }
        configured = NumberRanges(config.ranges, config.allowFreeNumbers, config.defaultSegments)
        legacy = NumberRanges(NumberRanges.legacyRanges(), config.allowFreeNumbers, config.defaultSegments)
        if (configured.isConfigured) {
            log.info { "Number ranges: ${configured.ranges.joinToString()}, allowFreeNumbers=${configured.allowFreeNumbers}" }
        } else {
            log.info {
                "No number ranges configured (projectforge.fibu.number-ranges.*): with cost units configured the" +
                        " former ranges 4 (internal) and 5 (customers 0-999) apply, otherwise customer and project" +
                        " numbers are free."
            }
        }
        instance = this
    }

    companion object {
        private var instance: NumberRangeService? = null

        private val unconfigured = NumberRanges(emptyList())

        /** False before the configuration is initialized (e.g. plain unit tests). */
        private fun isCostConfigured(): Boolean =
            runCatching { Configuration.instance.isCostConfigured }.getOrDefault(false)

        /**
         * For static helpers (formatters, parsers) that can't be injected. Without Spring context (plain unit tests)
         * no range is configured.
         */
        @JvmStatic
        val current: NumberRanges
            get() = instance?.numberRanges ?: unconfigured
    }
}
