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

package org.projectforge.business.fibu

import java.time.LocalDate

/**
 * The rules relating the period of performance of an order to the ones of its positions.
 *
 * These rules used to live in the Wicket form only (`PeriodOfPerformanceHelper.createValidator` plus two
 * `setRequiredSupplier` calls), where they were expressed through form components — which the hand built
 * next form has none of. They are lifted out of that form so they can be applied to a posted DTO, and so
 * that they are testable without one. Wicket keeps its own copy for as long as it exists; it is being
 * removed, and rewiring it would risk a regression in a form nobody maintains anymore.
 *
 * Not part of [AuftragDao.onInsertOrModify], although that is where the other order rules live: that
 * method throws a `UserException` at the first problem it finds, which is right for a script or an API
 * client, but a form should be able to show every offending row at once. So these rules answer a list
 * and the caller decides what to do with it.
 *
 * @author Kai Reinhard
 */
object PeriodOfPerformanceValidator {
    /**
     * One position, reduced to what the rules look at. A DTO or a form row is converted into this, so the
     * rules don't depend on either.
     *
     * @param type Null counts as [PeriodOfPerformanceType.SEEABOVE], the default of the column.
     */
    class Position(
        val type: PeriodOfPerformanceType? = null,
        val begin: LocalDate? = null,
        val end: LocalDate? = null,
    ) {
        val hasOwnPeriodOfPerformance: Boolean
            get() = type == PeriodOfPerformanceType.OWN
    }

    /**
     * @param fieldId The field the error belongs to, as a path relative to the order, e.g.
     * `periodOfPerformanceEnd` or `positionen[0].periodOfPerformanceBegin`.
     * @param messageKey The i18n key of the message.
     * @param labelKey The i18n key of the field's label, if [messageKey] takes it as a parameter (the
     * required messages do). Null for a message that reads on its own.
     */
    class Error(
        val fieldId: String,
        val messageKey: String,
        val labelKey: String? = null,
    )

    /**
     * @param positions The positions **excluding the deleted ones**: a deleted position is only sent so
     * the persistence layer doesn't remove it physically, and its dates are none of the user's business
     * anymore.
     * @return The violated rules, empty if there are none. In the order the form shows the fields, so a
     * caller that only reports the first one reports the topmost.
     */
    fun validate(
        periodOfPerformanceBegin: LocalDate?,
        periodOfPerformanceEnd: LocalDate?,
        positions: List<Position>?,
    ): List<Error> {
        val errors = mutableListOf<Error>()
        // The order's period is what a position of type SEEABOVE refers to, so that position makes the
        // order's begin a required field. Wicket expressed this with a supplier on the date panel.
        if (periodOfPerformanceBegin == null && positions?.any { !it.hasOwnPeriodOfPerformance } == true) {
            errors.add(
                Error(
                    fieldId = "periodOfPerformanceBegin",
                    messageKey = REQUIRED_MESSAGE_KEY,
                    labelKey = "fibu.periodOfPerformance",
                )
            )
        }
        if (periodOfPerformanceBegin != null && periodOfPerformanceEnd != null &&
            periodOfPerformanceEnd.isBefore(periodOfPerformanceBegin)
        ) {
            errors.add(Error(fieldId = "periodOfPerformanceEnd", messageKey = END_BEFORE_BEGIN_MESSAGE_KEY))
        }
        positions?.forEachIndexed { index, position ->
            if (!position.hasOwnPeriodOfPerformance) {
                // The dates of such a position are ignored everywhere downstream (see
                // AuftragsPositionDO.hasOwnPeriodOfPerformance), and the forms hide them - validating them
                // would report an error at a field the user cannot see. Wicket checked them regardless,
                // because its validator walked the date panels without asking for their type.
                return@forEachIndexed
            }
            if (position.end == null) {
                errors.add(
                    Error(
                        fieldId = "positionen[$index].periodOfPerformanceEnd",
                        messageKey = REQUIRED_MESSAGE_KEY,
                        labelKey = "fibu.periodOfPerformance",
                    )
                )
            }
            if (position.begin != null && position.end != null && position.end.isBefore(position.begin)) {
                errors.add(
                    Error(
                        fieldId = "positionen[$index].periodOfPerformanceEnd",
                        messageKey = END_BEFORE_BEGIN_MESSAGE_KEY,
                    )
                )
            }
            if (position.begin != null && periodOfPerformanceBegin != null &&
                position.begin.isBefore(periodOfPerformanceBegin)
            ) {
                // Anchored at the begin date, the field that is actually wrong. Wicket reported both
                // position rules at the end date, an artefact of how its validator walked the panels.
                errors.add(
                    Error(
                        fieldId = "positionen[$index].periodOfPerformanceBegin",
                        messageKey = POS_BEGIN_BEFORE_BEGIN_MESSAGE_KEY,
                    )
                )
            }
        }
        return errors
    }

    const val REQUIRED_MESSAGE_KEY = "validation.error.fieldRequired"
    const val END_BEFORE_BEGIN_MESSAGE_KEY = "error.endDateBeforeBeginDate"
    const val POS_BEGIN_BEFORE_BEGIN_MESSAGE_KEY = "error.posFromDateBeforeFromDate"
}
