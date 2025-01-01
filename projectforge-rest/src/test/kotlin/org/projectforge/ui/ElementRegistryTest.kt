/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.common.props.PropertyType
import org.projectforge.business.test.AbstractTestBase
import java.math.BigDecimal

class ElementRegistryTest : AbstractTestBase() {
    @Test
    fun testNestedProperties() {
        val userNameInfo = ElementsRegistry.getElementInfo(TimesheetDO::class.java, "task.responsibleUser.username")
        assertEquals("user.username", userNameInfo!!.i18nKey)
        val userInfo = userNameInfo.parent
        assertEquals("task.assignedUser", userInfo!!.i18nKey)
        val taskInfo = userInfo.parent
        assertEquals("task", taskInfo!!.i18nKey)
    }

    @Test
    fun testListElements() {
        val lc = LayoutContext(RechnungDO::class.java)
        var info = ElementsRegistry.getElementInfo(lc, "zahlBetrag")
        assertEquals(PropertyType.CURRENCY, info!!.propertyType)

        info = ElementsRegistry.getElementInfo(lc, "positionen")
        assertEquals(RechnungsPositionDO::class.java, info!!.genericType)

        lc.registerListElement("position", "positionen")
        info = ElementsRegistry.getElementInfo(lc, "position.menge")
        assertEquals(BigDecimal::class.java, info!!.propertyClass)
        assertFalse(info.readOnly)

        lc.registerListElement("kostZuweisung", "position.kostZuweisungen")
        info = ElementsRegistry.getElementInfo(lc, "kostZuweisung.netto")
        assertEquals(BigDecimal::class.java, info!!.propertyClass)
        assertFalse(info.readOnly)
        assertEquals("fibu.common.netto", info.i18nKey)
    }

    @Test
    fun testGetterProperties() {
        val lc = LayoutContext(RechnungDO::class.java)
        var info = ElementsRegistry.getElementInfo(lc, "nonExistingGetterAndField")
        assertNull(info)

        lc.registerListElement("position", "positionen")
        info = ElementsRegistry.getElementInfo(lc, "position.einzelNetto")
        assertEquals(BigDecimal::class.java, info!!.propertyClass)
        //assertTrue(info.readOnly)
        assertEquals("fibu.rechnung.position.einzelNetto", info.i18nKey)
    }
}
