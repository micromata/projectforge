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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Provides the configured number ranges ([NumberRangeConfig]). An inconsistent configuration stops the start.
 */
@Service
open class NumberRangeService {
    @Autowired
    private lateinit var config: NumberRangeConfig

    lateinit var numberRanges: NumberRanges
        private set

    @PostConstruct
    private fun postConstruct() {
        numberRanges = NumberRanges(config.ranges, config.allowFreeNumbers, config.defaultSegments)
        if (numberRanges.isConfigured) {
            log.info { "Number ranges: ${numberRanges.ranges.joinToString()}, allowFreeNumbers=${numberRanges.allowFreeNumbers}" }
        } else {
            log.info { "No number ranges configured: customer and project numbers are free." }
        }
        instance = this
    }

    companion object {
        private var instance: NumberRangeService? = null

        private val unconfigured = NumberRanges(emptyList())

        /**
         * For static helpers (formatters, parsers) that can't be injected. Without Spring context (plain unit tests)
         * no range is configured.
         */
        @JvmStatic
        val current: NumberRanges
            get() = instance?.numberRanges ?: unconfigured
    }
}
