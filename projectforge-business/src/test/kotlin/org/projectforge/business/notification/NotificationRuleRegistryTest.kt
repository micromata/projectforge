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

package org.projectforge.business.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

/**
 * The rule types registered by their handlers: the params of rules saved before (all params of all types flat in one
 * JSON) read unchanged, writing keeps those of the type only, the checks come from the declared params.
 */
class NotificationRuleRegistryTest : AbstractTestBase() {
    @Autowired
    private lateinit var registry: NotificationRuleRegistry

    private fun rule(type: String, json: String? = null) = NotificationRuleDO().also {
        it.ruleType = type
        it.params = json
    }

    @Test
    fun `the params of a rule saved before read unchanged`() {
        val json = """{"referredMonth":"PREVIOUS","vacationExpiry":"YEAR_END","daysBeforeExpiry":10,"passwordMaxAgeDays":60,""" +
                """"kost2MissingMonths":3,"dueWorkingDays":5,"cvrMinSeverity":"LOW","cvrIncludePossible":true,""" +
                """"cvrIncludeUnscored":false,"cvrMaxAgeDays":40,"cvrCaseId":null,"linkMode":"MENU_ITEM","linkMenuItem":"TIMESHEET_LIST"}"""
        val params = rule(VacationLeftRuleHandler.TYPE, json).readParams()
        assertEquals(NotificationParams.LinkMode.MENU_ITEM, params.linkMode)
        assertEquals("TIMESHEET_LIST", params.linkMenuItem)
        assertEquals(TimesheetsMissingRuleHandler.ReferredMonth.PREVIOUS, TimesheetsMissingRuleHandler.REFERRED_MONTH.of(params))
        assertEquals(VacationLeftRuleHandler.VacationExpiry.YEAR_END, VacationLeftRuleHandler.EXPIRY.of(params))
        assertEquals(10, VacationLeftRuleHandler.DAYS_BEFORE_EXPIRY.of(params))
        assertEquals(60, PasswordNotChangedRuleHandler.MAX_AGE_DAYS.of(params))
        assertEquals(3, TimesheetsWithoutKost2RuleHandler.MISSING_MONTHS.of(params))
        assertEquals(5, CreditorInvoicesDueRuleHandler.DUE_WORKING_DAYS.of(params))
        // Without a value (or with an unreadable one): the default.
        val empty = rule(VacationLeftRuleHandler.TYPE, """{"daysBeforeExpiry":"many"}""").readParams()
        assertEquals(30, VacationLeftRuleHandler.DAYS_BEFORE_EXPIRY.of(empty))
        assertEquals(TimesheetsMissingRuleHandler.ReferredMonth.CURRENT, TimesheetsMissingRuleHandler.REFERRED_MONTH.of(empty))

        // The editor gets the values of the type only, written back as the JSON stores them.
        val vacation = rule(VacationLeftRuleHandler.TYPE, json)
        assertEquals(mapOf("vacationExpiry" to "YEAR_END", "daysBeforeExpiry" to 10), registry.values(vacation))
        registry.writeValues(vacation, registry.values(vacation))
        val written = vacation.readParams()
        assertEquals(setOf("vacationExpiry", "daysBeforeExpiry"), written.values.keys, "left-overs of other types dropped")
        assertEquals(NotificationParams.LinkMode.MENU_ITEM, written.linkMode)
        assertTrue(vacation.params!!.contains(""""daysBeforeExpiry":10"""), vacation.params)
        assertFalse(vacation.params!!.contains("values"), vacation.params)
    }

    @Test
    fun `posted values are parsed and checked by the params of the type`() {
        val rule = rule(VacationLeftRuleHandler.TYPE)
        registry.writeValues(rule, mapOf("vacationExpiry" to "CARRY_OVER", "daysBeforeExpiry" to "7"))
        assertEquals(7, VacationLeftRuleHandler.DAYS_BEFORE_EXPIRY.of(rule))
        assertTrue(registry.errors(rule).isEmpty())

        registry.writeValues(rule, mapOf("vacationExpiry" to "SOMETIME", "daysBeforeExpiry" to -1))
        val errors = registry.errors(rule)
        assertEquals(NotificationRuleParam.REQUIRED, errors["vacationExpiry"]?.i18nKey, "unknown option")
        assertEquals("notification.rule.error.daysBeforeExpiry", errors["daysBeforeExpiry"]?.i18nKey)

        val months = rule(TimesheetsWithoutKost2RuleHandler.TYPE)
        registry.writeValues(months, emptyMap())
        assertEquals(NotificationRuleParam.REQUIRED, registry.errors(months)["kost2MissingMonths"]?.i18nKey)
    }

    @Test
    fun `the types are those of the registered handlers`() {
        val types = registry.all.map { it.ruleType }
        assertEquals(ManualRuleHandler.TYPE, types.last(), "the manual rule last of the core types: $types")
        assertTrue(types.containsAll(listOf(TimesheetsMissingRuleHandler.TYPE, CreditorInvoicesDueRuleHandler.TYPE)))
        assertNull(registry.handler("NO_SUCH_TYPE"))
        assertTrue(registry.errors(rule("NO_SUCH_TYPE")).isEmpty())
        assertTrue(registry.isManualDoneForced(rule(ManualRuleHandler.TYPE).also { it.display = NotificationDisplay.BANNER }))
        assertFalse(registry.isManualDoneForced(rule(ManualRuleHandler.TYPE).also { it.display = NotificationDisplay.TOAST }))
        assertFalse(registry.isManualDoneForced(rule(TimesheetsMissingRuleHandler.TYPE)))
    }
}
