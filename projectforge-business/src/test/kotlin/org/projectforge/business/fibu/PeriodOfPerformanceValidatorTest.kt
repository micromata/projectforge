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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * These rules used to be expressed through Wicket form components, where they could only be tested by
 * driving a form. Being a plain function is the point of extracting them, so this test needs no Spring
 * context and no database.
 */
class PeriodOfPerformanceValidatorTest {
    private val begin = LocalDate.of(2026, 3, 1)
    private val end = LocalDate.of(2026, 6, 30)

    @Test
    fun `an order whose positions follow it needs a begin date`() {
        val errors = PeriodOfPerformanceValidator.validate(
            periodOfPerformanceBegin = null,
            periodOfPerformanceEnd = null,
            positions = listOf(position(PeriodOfPerformanceType.SEEABOVE)),
        )
        assertEquals(1, errors.size)
        assertEquals("periodOfPerformanceBegin", errors[0].fieldId)
        assertEquals(PeriodOfPerformanceValidator.REQUIRED_MESSAGE_KEY, errors[0].messageKey)
        // The message takes the field's label as its parameter, so the caller has to be given one.
        assertEquals("fibu.periodOfPerformance", errors[0].labelKey)
    }

    @Test
    fun `a null type counts as SEEABOVE, the default of the column`() {
        val errors = PeriodOfPerformanceValidator.validate(null, null, listOf(position(null)))
        assertEquals(listOf("periodOfPerformanceBegin"), errors.map { it.fieldId })
    }

    @Test
    fun `an order all of whose positions have their own period needs no begin date`() {
        val errors = PeriodOfPerformanceValidator.validate(
            periodOfPerformanceBegin = null,
            periodOfPerformanceEnd = null,
            positions = listOf(position(PeriodOfPerformanceType.OWN, begin, end)),
        )
        assertTrue(errors.isEmpty(), "Expected no error, but got ${errors.map { it.fieldId }}.")
    }

    @Test
    fun `an order without positions is not checked at all`() {
        assertTrue(PeriodOfPerformanceValidator.validate(null, null, null).isEmpty())
        assertTrue(PeriodOfPerformanceValidator.validate(null, null, emptyList()).isEmpty())
    }

    @Test
    fun `the order's end must not be before its begin`() {
        val errors = PeriodOfPerformanceValidator.validate(begin, begin.minusDays(1), null)
        assertEquals(1, errors.size)
        assertEquals("periodOfPerformanceEnd", errors[0].fieldId)
        assertEquals(PeriodOfPerformanceValidator.END_BEFORE_BEGIN_MESSAGE_KEY, errors[0].messageKey)
        // Reads on its own, so no label is passed.
        assertEquals(null, errors[0].labelKey)
    }

    @Test
    fun `a position with its own period needs an end date`() {
        val errors = PeriodOfPerformanceValidator.validate(
            begin, end, listOf(position(PeriodOfPerformanceType.OWN, begin, null))
        )
        assertEquals(listOf("positionen[0].periodOfPerformanceEnd"), errors.map { it.fieldId })
        assertEquals(PeriodOfPerformanceValidator.REQUIRED_MESSAGE_KEY, errors[0].messageKey)
    }

    @Test
    fun `a position's end must not be before its begin`() {
        val errors = PeriodOfPerformanceValidator.validate(
            begin, end, listOf(position(PeriodOfPerformanceType.OWN, end, begin))
        )
        // Its begin is after the order's, so the third rule doesn't fire — only this one.
        assertEquals(listOf("positionen[0].periodOfPerformanceEnd"), errors.map { it.fieldId })
        assertEquals(PeriodOfPerformanceValidator.END_BEFORE_BEGIN_MESSAGE_KEY, errors[0].messageKey)
    }

    @Test
    fun `a position must not begin before the order does, and the error names its begin date`() {
        val errors = PeriodOfPerformanceValidator.validate(
            begin, end, listOf(position(PeriodOfPerformanceType.OWN, begin.minusDays(1), end))
        )
        // Wicket reported this at the end date, because its validator walked the date panels.
        assertEquals(listOf("positionen[0].periodOfPerformanceBegin"), errors.map { it.fieldId })
        assertEquals(PeriodOfPerformanceValidator.POS_BEGIN_BEFORE_BEGIN_MESSAGE_KEY, errors[0].messageKey)
    }

    @Test
    fun `the dates of a position that follows the order are not checked`() {
        // They are ignored everywhere downstream and hidden by both forms, so reporting them would point
        // at a field the user cannot see.
        val errors = PeriodOfPerformanceValidator.validate(
            begin, end, listOf(position(PeriodOfPerformanceType.SEEABOVE, begin.minusYears(1), null))
        )
        assertTrue(errors.isEmpty(), "Expected no error, but got ${errors.map { it.fieldId }}.")
    }

    @Test
    fun `every offending row is reported, at the index of the posted list`() {
        val errors = PeriodOfPerformanceValidator.validate(
            begin,
            end,
            listOf(
                position(PeriodOfPerformanceType.OWN, begin, end),
                position(PeriodOfPerformanceType.OWN, begin, null),
                position(PeriodOfPerformanceType.OWN, begin.minusDays(1), end),
            ),
        )
        // A form shows all of them at once — that is why this answers a list instead of throwing.
        assertEquals(
            listOf("positionen[1].periodOfPerformanceEnd", "positionen[2].periodOfPerformanceBegin"),
            errors.map { it.fieldId },
        )
    }

    private fun position(
        type: PeriodOfPerformanceType?,
        positionBegin: LocalDate? = null,
        positionEnd: LocalDate? = null,
    ) = PeriodOfPerformanceValidator.Position(type = type, begin = positionBegin, end = positionEnd)
}
