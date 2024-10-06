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
    fun testTransformAttributes() {
        val attrs = mutableListOf<PfHistoryAttrDO>()
        attrs.add(createAttr("city:op", "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("city:ov", "Old City"))
        attrs.add(createAttr("city:nv", "New City"))
        // re-ordered:
        attrs.add(createAttr("street:ov", "Old Street"))
        attrs.add(createAttr("street:op", "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("street:nv", "New Street"))
        // re-ordered:
        attrs.add(createAttr("zip:ov", "Old Zip", propertyTypeClass = "java.lang.Integer"))
        attrs.add(createAttr("zip:nv", "New Zip", propertyTypeClass = "java.lang.Integer"))
        attrs.add(createAttr("zip:op", "Update", propertyTypeClass = PropertyOp))
        // 2 entries:
        attrs.add(createAttr("state:nv", "New State"))
        attrs.add(createAttr("state:op", "Insert", propertyTypeClass = PropertyOp))
        // re-ordered:
        attrs.add(createAttr("country:op", "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("country:nv", "New Country"))
        // timeableAttribute
        attrs.add(createAttr("timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.arrive:op", "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.arrive:nv", "New Visit"))

        // New format (since 2024):
        attrs.add(createAttr("name", value = "B", oldValue = "A", opType = PropertyOpType.Update))
        val master = PfHistoryMasterDO()
        attrs.forEach { attr -> master.add(attr) }
        PFHistoryMasterUtils.transformOldAttributes(master)
        Assertions.assertEquals(7, master.attributes?.size)
        assertAttr(master.attributes, "city", "New City", "Old City")
        assertAttr(master.attributes, "street", "New Street", "Old Street")
        assertAttr(master.attributes, "zip", "New Zip", "Old Zip", propertyTypeClass = "java.lang.Integer")
        assertAttr(master.attributes, "state", "New State", null, PropertyOpType.Insert)
        assertAttr(master.attributes, "country", "New Country", null)
        assertAttr(master.attributes, "name", "B", "A")
        assertAttr(master.attributes, "timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.arrive", "New Visit", null)
    }

    @Test
    fun testGetPropertyName() {
        Assertions.assertEquals("street", PFHistoryMasterUtils.getPropertyName(createAttr("street")))
        Assertions.assertEquals("street", PFHistoryMasterUtils.getPropertyName(createAttr("street:ov")))
        Assertions.assertEquals("street", PFHistoryMasterUtils.getPropertyName(createAttr("street:op")))
        Assertions.assertEquals("street", PFHistoryMasterUtils.getPropertyName(createAttr("street:nv")))
        Assertions.assertEquals(
            "timeableAttributes.timeofvisit.2023-09-12 00:00:00:000.arrive",
            PFHistoryMasterUtils.getPropertyName(createAttr("timeableAttributes.timeofvisit.2023-09-12 00:00:00:000.arrive:nv"))
        )

        Assertions.assertNull(PFHistoryMasterUtils.getPropertyName(createAttr(null)))
    }

    private fun assertAttr(attrs: Set<PfHistoryAttrDO>?, propertyName: String?, newValue: String?, oldValue: String?, opType: PropertyOpType = PropertyOpType.Update, propertyTypeClass: String = "java.lang.String") {
        Assertions.assertNotNull(attrs)
        attrs!!.find { it.propertyName == propertyName }?.let { attr ->
            Assertions.assertNotNull(attrs, "Attribute not found: $propertyName")
            Assertions.assertEquals(propertyName, attr.propertyName)
            Assertions.assertEquals(newValue, attr.value)
            Assertions.assertEquals(oldValue, attr.oldValue)
            Assertions.assertEquals(opType, attr.optype)
            Assertions.assertEquals(propertyTypeClass, attr.propertyTypeClass)
        }
    }

    private fun createAttr(
        propertyName: String?,
        value: String? = null,
        oldValue: String? = null,
        opType: PropertyOpType? = null,
        propertyTypeClass: String? = "java.lang.String",
    ): PfHistoryAttrDO {
        val attr = PfHistoryAttrDO()
        attr.propertyName = propertyName
        attr.propertyTypeClass = propertyTypeClass
        attr.value = value
        attr.oldValue = oldValue
        attr.optype = opType
        return attr
    }

    companion object {
        val PropertyOp = "de.micromata.genome.db.jpa.history.entities.PropertyOpType"
    }
}
