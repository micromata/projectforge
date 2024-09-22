/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa.candh

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import org.projectforge.business.task.TaskDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

class CandHHandlerTest {
    @Test
    fun defaultHandlerTest() {
        val handler = DefaultHandler()
        // String tests:
        assert(handler, TaskDO::class, "title", "", "title2")
        assert(handler, TaskDO::class, "title", "title1", "")
        assert(handler, TaskDO::class, "title", "title1", "title2")

        // LocalDate tests:
        assert(
            handler,
            TaskDO::class,
            "protectTimesheetsUntil",
            LocalDate.of(2024, Month.SEPTEMBER, 12),
            LocalDate.of(2024, Month.SEPTEMBER, 13),
        )

        // Boolean tests.
        assert(
            handler,
            VacationDO::class,
            "special",
            true,
            false,
        )
    }

    @Test
    fun bigDecimalHandlerTest() {
        val handler = BigDecimalHandler()
        assert(
            handler,
            TaskDO::class,
            "duration",
            BigDecimal.ONE,
            BigDecimal.TEN,
        )
        Assertions.assertTrue(
            handler.propertyValuesEqual(BigDecimal.TEN.setScale(0), BigDecimal.TEN.setScale(2)),
            "BigDecimalHandler should ignore scale.",
        )
    }

    @Test
    fun utilDateHandlerTest() {
        val handler = UtilDateHandler()
        val date1 = Date()
        val date2 = Date(date1.time - Constants.MILLIS_PER_DAY)
        assert(
            handler,
            PFUserDO::class,
            "lastPasswordChange",
            date1,
            date2,
        )
    }

    private fun assert(
        handler: CandHIHandler,
        kClass: KClass<*>,
        fieldName: String,
        fieldValue1: Any, // first value for testing.
        fieldValue2: Any, // second value for testing (should be different to fieldValue1.‚
    ) {
        val src = kClass.createInstance() as BaseDO<Long>
        val dest = kClass.createInstance() as BaseDO<Long>
        processAndCheckContext(handler, kClass, fieldName, src, dest, null, null, false)
        processAndCheckContext(handler, kClass, fieldName, src, dest, fieldValue1, null, true)
        processAndCheckContext(handler, kClass, fieldName, src, dest, null, fieldValue2, true)
        processAndCheckContext(handler, kClass, fieldName, src, dest, fieldValue1, fieldValue2, true)
        processAndCheckContext(handler, kClass, fieldName, src, dest, fieldValue1, fieldValue1, false)
        processAndCheckContext(handler, kClass, fieldName, src, dest, fieldValue2, fieldValue2, false)
    }

    private fun processAndCheckContext(
        handler: CandHIHandler,
        kClass: KClass<*>,
        propertyName: String,
        src: BaseDO<Long>,
        dest: BaseDO<Long>,
        srcFieldValue: Any?,
        destFieldValue: Any?,
        modificationExpected: Boolean,
    ) {
        val context = CandHContext(debug = true)
        val property = kClass.memberProperties.find { it.name == propertyName }!!
        @Suppress("UNCHECKED_CAST")
        property as KMutableProperty1<BaseDO<*>, Any?>
        property.set(src, srcFieldValue)  // Setzt den neuen Wert
        property.set(dest, destFieldValue)
        val fieldContext = PropertyContext(
            kClass = kClass,
            src = src,
            dest = dest,
            propertyName = propertyName,
            property = property,
            srcPropertyValue = srcFieldValue,
            destPropertyValue = destFieldValue,
        )
        handler.process(fieldContext, context = context)
        Assertions.assertEquals(property.get(src), property.get(dest))
        val debugEntries = context.debugContext!!.entries
        if (modificationExpected) {
            Assertions.assertTrue(debugEntries.isNotEmpty())
        } else {
            Assertions.assertTrue(debugEntries.isEmpty())
        }
    }
}
