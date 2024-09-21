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

class PfHistoryMasterUtilsTest {
    @Test
    fun testCreateDiffEntries() {
        val attrs = mutableListOf<PfHistoryAttrDO>()
        attrs.add(createAttr("city:ov", "Old City"))
        attrs.add(createAttr("city:nv", "New City"))
        attrs.add(createAttr("city:op", "Update"))
        // For real entries, old and new format isn't mixed:
        attrs.add(createAttr("street", "New Street", "Old Street", PropertyOpType.Update))
        val entries = PFHistoryMasterUtils.createDiffEntries(attrs)!!
        Assertions.assertEquals(2, entries.size)
        Assertions.assertEquals("Old City", entries[0].oldValue)
        Assertions.assertEquals("New City", entries[0].newValue)
        Assertions.assertEquals(PropertyOpType.Update, entries[0].propertyOpType)
        Assertions.assertEquals("Old Street", entries[1].oldValue)
        Assertions.assertEquals("New Street", entries[1].newValue)
        Assertions.assertEquals(PropertyOpType.Update, entries[1].propertyOpType)

    }

    @Test
    fun testGetPropertyName() {
        Assertions.assertEquals("street", PFHistoryMasterUtils.getPropertyName(createAttr("street")))
        Assertions.assertEquals("street", PFHistoryMasterUtils.getPropertyName(createAttr("street:ov")))
        Assertions.assertNull(PFHistoryMasterUtils.getPropertyName(createAttr(null)))
    }

    private fun createAttr(
        propertyName: String?,
        value: String? = null,
        oldValue: String? = null,
        opType: PropertyOpType? = null
    ): PfHistoryAttrDO {
        val attr = PfHistoryAttrDO()
        attr.propertyName = propertyName
        attr.value = value
        attr.oldValue = oldValue
        attr.optype = opType
        return attr
    }
}
