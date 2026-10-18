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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.numberrange.NumberRange
import org.projectforge.business.fibu.numberrange.NumberRangeService
import org.projectforge.framework.configuration.Configuration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The configured number ranges of customers and projects ([NumberRangeService]) for the customer, project and
 * cost 2 forms of projectforge-next, which shape their number fields by them.
 */
@RestController
@RequestMapping("${Rest.URL}/numberRanges")
class NumberRangeRest {
    @Autowired
    private lateinit var numberRangeService: NumberRangeService

    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    class Range(
        val prefix: Int,
        val name: String?,
        val customerMin: Long?,
        val customerMax: Long?,
        val projectDigits: Int,
        val segments: List<Int>,
        /** The next free customer number of the range, null without customers or if exhausted. */
        val nextFreeCustomerNumber: Long? = null,
    )

    class NumberRangesInfo(
        /** Whether cost units are configured at all: without, projects have no Kost2 types. */
        val costConfigured: Boolean,
        val allowFreeNumbers: Boolean,
        val defaultSegments: List<Int>,
        val ranges: List<Range>,
    )

    @AccessChecked("Any logged-in user; configuration only. Next free customer numbers only with customer select access (KundeDao)")
    @GetMapping
    fun getNumberRanges(): NumberRangesInfo {
        val numberRanges = numberRangeService.numberRanges
        val withCustomers = kundeDao.hasLoggedInUserSelectAccess(throwException = false)
        val usedCustomerNumbers = if (withCustomers) kundeCache.all.keys else emptySet()
        return NumberRangesInfo(
            costConfigured = Configuration.instance.isCostConfigured,
            allowFreeNumbers = numberRanges.allowFreeNumbers,
            defaultSegments = numberRanges.defaultSegments,
            ranges = numberRanges.ranges.map { range ->
                toRange(range, if (withCustomers) numberRanges.nextFreeCustomerNumber(range, usedCustomerNumbers) else null)
            },
        )
    }

    private fun toRange(range: NumberRange, nextFreeCustomerNumber: Long?) = Range(
        prefix = range.prefix,
        name = range.name,
        customerMin = range.customerMin,
        customerMax = range.customerMax,
        projectDigits = range.projectDigits,
        segments = range.segments,
        nextFreeCustomerNumber = nextFreeCustomerNumber,
    )
}
