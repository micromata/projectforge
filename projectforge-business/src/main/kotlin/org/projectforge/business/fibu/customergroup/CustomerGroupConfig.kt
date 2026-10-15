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

package org.projectforge.business.fibu.customergroup

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.json.JsonUtils

private val log = KotlinLogging.logger {}

/**
 * The customer groups and business units, stored as JSON in the configuration parameter
 * [org.projectforge.framework.configuration.ConfigurationParam.CUSTOMER_GROUPS] and maintained by finance and
 * controlling on a page of their own (`CustomerGroupPageRest`).
 *
 * A group combines customers that belong together (ACME Germany, ACME Logistics, ACME Holding -> ACME); a
 * business unit combines groups and single customers. Each customer belongs to at most one group, each group
 * and each customer to at most one business unit (see [CustomerGroupValidator]).
 *
 * The classes have no-arg constructors (all properties have defaults): the mapper ignoring unknown
 * properties, so that a newer version's JSON still reads, has no Kotlin module.
 */
class CustomerGroupConfig(
    var version: Int = VERSION,
    var groups: MutableList<CustomerGroup> = mutableListOf(),
    var businessUnits: MutableList<BusinessUnit> = mutableListOf(),
    /**
     * Free text of finance and controlling about the definition as a whole: how the groups and business units
     * are drawn, or why they were changed. Not evaluated; changes are traced through the history of the
     * configuration parameter.
     */
    var remark: String? = null,
) {
    fun toJson(): String = JsonUtils.toJson(this, ignoreNullableProps = true)

    companion object {
        const val VERSION = 1

        /**
         * @return The parsed configuration, an empty one for a blank value. A value that can't be read is
         * logged and taken as empty: the lists then offer no groups, rather than failing.
         */
        fun parse(json: String?): CustomerGroupConfig {
            if (json.isNullOrBlank()) {
                return CustomerGroupConfig()
            }
            return try {
                JsonUtils.fromJson(json, CustomerGroupConfig::class.java, failOnUnknownProps = false)
                    ?: CustomerGroupConfig()
            } catch (ex: Exception) {
                log.error(ex) { "Can't parse the customer groups (configuration parameter fibu.customerGroups): ${ex.message}" }
                CustomerGroupConfig()
            }
        }
    }
}

/**
 * The members both a group and a business unit have: customer entities and free-text customers.
 */
abstract class CustomerSet {
    /**
     * Stable key, assigned by the server on the first save. Filters refer to it (`g:<key>`, `b:<key>`), not
     * to the name, so renaming keeps the users' saved filters.
     */
    var key: String? = null

    var name: String? = null

    /** Numbers of the customer entities ([org.projectforge.business.fibu.KundeDO.nummer]). */
    var customers: MutableList<Long> = mutableListOf()

    /**
     * Customer names: an exact name or a pattern, see [TextPattern]. They apply to the name of a customer
     * entity ([org.projectforge.business.fibu.KundeDO.name]) as well as to the `kundeText` of orders and
     * invoices without one, so a new "ACME …" customer joins its group without being added by number.
     */
    var texts: MutableList<String> = mutableListOf()
}

class CustomerGroup : CustomerSet()

class BusinessUnit : CustomerSet() {
    /** Keys of the groups belonging to this business unit. */
    var groups: MutableList<String> = mutableListOf()

    /**
     * Ids of tasks: a project whose task is one of them or lies below one belongs to this business unit, if
     * its customer leads to none (see [CustomerGroupIndex.businessUnitOf]).
     */
    var tasks: MutableList<Long> = mutableListOf()
}
