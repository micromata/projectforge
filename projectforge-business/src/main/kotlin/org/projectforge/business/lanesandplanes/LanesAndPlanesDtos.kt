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

package org.projectforge.business.lanesandplanes

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body of `POST /ext/users`. Fields not set aren't sent at all, so the values maintained in Lanes & Planes
 * (e.g. roles, travel policy groups, managers) are kept: only fields sent empty are cleared there.
 */
data class LanesAndPlanesUsersRequest(val users: List<LanesAndPlanesUser>)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LanesAndPlanesUser(
    /** The id of the ProjectForge user; not sent for the additional users (so kept in Lanes & Planes). */
    val ident: String? = null,
    @JsonProperty("first_name")
    val firstName: String,
    /** Sent empty for the employees (cleared in L&P): there is no middle name in ProjectForge. */
    @JsonProperty("middle_name")
    val middleName: String? = null,
    @JsonProperty("last_name")
    val lastName: String,
    /** The unique key of the user in Lanes & Planes. */
    val email: String,
    /** The nickname of the user, sent empty if not set (so cleared in L&P). */
    val abbreviation: String? = null,
    @JsonProperty("personnel_number")
    val personnelNumber: String? = null,
    @JsonProperty("reference_cost_center")
    val referenceCostCenter: String? = null,
    /** The account of the employee (number of the DATEV account). */
    @JsonProperty("creditor_account")
    val creditorAccount: String? = null,
    /** The Kost2: the general ones and the bookable ones. */
    @JsonProperty("cost_units")
    val costUnits: List<LanesAndPlanesCostObject>? = null,
    /** The general Kost1. */
    @JsonProperty("cost_centers")
    val costCenters: List<LanesAndPlanesCostObject>? = null,
    @JsonProperty("accounting_invoice_profile_ids")
    val accountingInvoiceProfileIds: List<Long>,
)

data class LanesAndPlanesCostObject(val ident: String, val name: String)
