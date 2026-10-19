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

import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.numberrange.NumberRange
import org.projectforge.business.fibu.numberrange.NumberRangeService
import org.projectforge.framework.configuration.Configuration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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

    @Autowired
    private lateinit var projektDao: ProjektDao

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
        /**
         * Which ranges apply: the configured ones, the former ranges 4/5 (no ranges configured, but in use, see
         * [NumberRangeService]) or none (free numbers).
         */
        val mode: NumberRangeService.Mode,
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
            mode = numberRangeService.mode,
            allowFreeNumbers = numberRanges.allowFreeNumbers,
            defaultSegments = numberRanges.defaultSegments,
            ranges = numberRanges.ranges.map { range ->
                toRange(range, if (withCustomers) numberRanges.nextFreeCustomerNumber(range, usedCustomerNumbers) else null)
            },
        )
    }

    class NextFreeProjectNumber(
        /** The local number of the project (per customer or of the range without customers). */
        val nummer: Int,
        val projektnummer: Int,
    )

    /**
     * The next free project number in the number range of the customer (or, without customer, of the range without
     * customers), for the pick link of the project form. Null without such a range or if it is exhausted.
     *
     * @param projectId The edited project, whose own number doesn't count as taken.
     */
    @AccessChecked("Project select access (ProjektDao), otherwise null")
    @GetMapping("nextFreeProjectNumber")
    fun getNextFreeProjectNumber(
        @RequestParam("customerId", required = false) customerId: Long?,
        @RequestParam("projectId", required = false) projectId: Long?,
    ): NextFreeProjectNumber? {
        if (!projektDao.hasLoggedInUserSelectAccess(throwException = false)) {
            return null
        }
        // A customer not stored yet (the project wizard creates it with the project) has no projects, so all
        // numbers of its range are free:
        val kunde = customerId?.let { kundeCache.getKunde(it) ?: KundeDO().also { new -> new.nummer = it } }
        val numberRanges = numberRangeService.numberRanges
        val range = if (kunde != null) numberRanges.rangeForCustomer(kunde.nummer) else numberRanges.rangeWithoutCustomers
        range ?: return null
        val nummer = projektDao.nextFreeLocalNumber(kunde, projectId) ?: return null
        return NextFreeProjectNumber(nummer, numberRanges.projectNumber(range, kunde?.nummer, nummer))
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
