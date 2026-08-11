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

package org.projectforge.ui

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class UIDataTypeUtils {
    companion object {
        /**
         * Determine [UIDataType] by property type.
         * @return mapped type or [UIDataType.STRING] if no mapping found.
         */
        internal fun ensureDataType(elementInfo: ElementInfo?): UIDataType {
            return getDataType(elementInfo) ?: UIDataType.STRING
        }

        fun getDataType(elementInfo: ElementInfo?): UIDataType? {
            elementInfo ?: return null
            return when (elementInfo.propertyClass) {
                String::class.java -> UIDataType.STRING
                Boolean::class.java, java.lang.Boolean::class.java -> UIDataType.BOOLEAN
                Date::class.java -> UIDataType.TIMESTAMP
                LocalDate::class.java, java.sql.Date::class.java -> UIDataType.DATE
                java.sql.Timestamp::class.java -> UIDataType.TIMESTAMP
                PFUserDO::class.java -> UIDataType.USER
                GroupDO::class.java -> UIDataType.GROUP
                EmployeeDO::class.java -> UIDataType.EMPLOYEE
                Kost1DO::class.java -> UIDataType.COST1
                Kost2DO::class.java -> UIDataType.COST2
                // Both the boxed and the primitive class, because Kotlin's `Int::class.java` is
                // `int` while `Integer::class.java` is `java.lang.Integer` — and a nullable Kotlin
                // property (`var year: Int?`) reflects as the boxed one, a Java `int` field as the
                // primitive. Missing either half means the property falls back to STRING.
                Integer::class.java, Int::class.java -> UIDataType.INT
                // The row numbers of the nested collections (AuftragsPositionDO.number,
                // PaymentScheduleDO.number/positionNumber) are stored as Short. Both halves again, for
                // the reason above: `number: Short = 0` reflects as the primitive, `positionNumber:
                // Short?` as the boxed class. Mapped to INT, since the frontend has no narrower type
                // and 32767 is no limit a row count reaches.
                java.lang.Short::class.java, Short::class.java -> UIDataType.INT
                java.lang.Long::class.java, Long::class.java -> UIDataType.LONG
                BigDecimal::class.java -> UIDataType.DECIMAL
                TaskDO::class.java -> UIDataType.TASK
                Locale::class.java -> UIDataType.LOCALE
                TimeZone::class.java -> UIDataType.TIMEZONE
                LocalTime::class.java -> UIDataType.TIME
                else -> null
            }
        }
    }
}
