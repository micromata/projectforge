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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookStatus
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class HistoryValueServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var historyValueService: HistoryValueService

    @Test
    fun testGetValueType() {
        assertValueType(HistoryValueService.ValueType.BASE_TYPE, "java.lang.String")
        Assertions.assertEquals(
            HistoryValueService.ValueType.BASE_TYPE,
            historyValueService.typeMapping["java.lang.String"]
        )
        assertValueType(HistoryValueService.ValueType.BINARY, "[B")
        Assertions.assertEquals(ByteArray::class.java, historyValueService.typeClassMapping["[B"])
        Assertions.assertEquals(HistoryValueService.ValueType.BINARY, historyValueService.typeMapping["[B"])
        assertValueType(HistoryValueService.ValueType.BASE_TYPE, "boolean")
        assertValueType(
            HistoryValueService.ValueType.ENUM,
            "org.projectforge.framework.persistence.history.PropertyOpType"
        )
        Assertions.assertEquals(
            HistoryValueService.ValueType.ENUM,
            historyValueService.typeMapping["org.projectforge.framework.persistence.history.PropertyOpType"]
        )
        Assertions.assertEquals(
            PropertyOpType::class.java,
            historyValueService.typeClassMapping["org.projectforge.framework.persistence.history.PropertyOpType"]
        )

        assertValueType(HistoryValueService.ValueType.I18N_ENUM, "org.projectforge.business.book.BookStatus")
        Assertions.assertEquals(
            HistoryValueService.ValueType.I18N_ENUM,
            historyValueService.typeMapping["org.projectforge.business.book.BookStatus"]
        )
        Assertions.assertEquals(
            BookStatus::class.java,
            historyValueService.typeClassMapping["org.projectforge.business.book.BookStatus"]
        )
        assertValueType(HistoryValueService.ValueType.ENTITY, "org.projectforge.business.book.BookDO")
        Assertions.assertEquals(
            HistoryValueService.ValueType.ENTITY,
            historyValueService.typeMapping["org.projectforge.business.book.BookDO"]
        )
        assertValueType(HistoryValueService.ValueType.ENTITY, "org.projectforge.business.book.BookDO_\$\$jvst148_2f")
        Assertions.assertEquals(
            BookDO::class.java,
            historyValueService.typeClassMapping["org.projectforge.business.book.BookDO"]
        )
        assertValueType(
            HistoryValueService.ValueType.ENTITY,
            "org.projectforge.business.book.BookDO\$HibernateProxy\$vst148_2f"
        )
        assertValueType(
            HistoryValueService.ValueType.UNKNOWN,
            "org.projectforge.framework.persistence.history.HistoryValueService"
        )
        assertValueType(HistoryValueService.ValueType.UNKNOWN, "org.unknown.Hurzel1")
        Assertions.assertTrue(historyValueService.unknownPropertyTypes.contains("org.unknown.Hurzel1"))
        assertValueType(HistoryValueService.ValueType.UNKNOWN, "org.unknown.Hurzel2_\$\$jvst148_2f")
        Assertions.assertTrue(historyValueService.unknownPropertyTypes.contains("org.unknown.Hurzel2"))
    }

    @Test
    fun testGetTypeClassName() {
        assertTypeClassName("", "")
        assertTypeClassName("java.lang.String", "java.lang.String")
        assertTypeClassName("[B", "[B")
        assertTypeClassName("boolean", "boolean")
        assertTypeClassName(
            "org.projectforge.business.address.AddressDO",
            "org.projectforge.business.address.AddressDO_\$\$_jvst148_2f",
        )
        assertTypeClassName(
            "org.projectforge.business.address.AddressDO",
            "org.projectforge.business.address.AddressDO\$HibernateProxy\$En8hKwh8",
        )
    }

    private fun assertTypeClassName(expected: String, propertyType: String) {
        Assertions.assertEquals(expected, HistoryValueService.getUnifiedTypeName(propertyType))
    }

    private fun assertValueType(expected: HistoryValueService.ValueType, dbPropertyType: String) {
        val propertyType = HistoryValueService.getUnifiedTypeName(dbPropertyType)
        Assertions.assertEquals(expected, historyValueService.getValueType(propertyType))
    }
}
