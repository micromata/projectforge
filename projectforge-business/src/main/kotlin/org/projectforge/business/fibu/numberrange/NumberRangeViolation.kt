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

/** Why a customer or project number is rejected by the configured number ranges. */
enum class NumberRangeViolation(val i18nKey: String) {
    INVALID_NUMBER("fibu.numberRange.error.invalidNumber"),

    /** Free numbers aren't allowed and the customer number belongs to no range. */
    CUSTOMER_NOT_IN_RANGE("fibu.numberRange.error.customerNotInRange"),

    /** Free numbers aren't allowed, so a project needs a customer of a range (or the range without customers). */
    PROJECT_NOT_IN_RANGE("fibu.numberRange.error.projectNotInRange"),

    /** A free project number lies in the project numbers of a range. */
    FREE_NUMBER_IN_RANGE("fibu.numberRange.error.freeNumberInRange"),
}
