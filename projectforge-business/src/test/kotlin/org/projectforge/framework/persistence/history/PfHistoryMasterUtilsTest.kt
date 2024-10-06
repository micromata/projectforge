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

import org.hibernate.proxy.HibernateProxy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PfHistoryMasterUtilsTest {
    @Test
    fun testTransformAttributes() {
        val attrs = mutableListOf<PfHistoryAttrDO>()
        var id = 0L
        attrs.add(createAttr("city:op", ++id, "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("city:ov", id, "Old City"))
        attrs.add(createAttr("city:nv", id, "New City"))
        // re-ordered:
        attrs.add(createAttr("street:ov", ++id, "Old Street"))
        attrs.add(createAttr("street:op", id, "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("street:nv", id, "New Street"))
        // re-ordered:
        attrs.add(createAttr("zip:ov", ++id, "Old Zip", propertyTypeClass = "java.lang.Integer"))
        attrs.add(createAttr("zip:nv", id, "New Zip", propertyTypeClass = "java.lang.Integer"))
        attrs.add(createAttr("zip:op", id, "Update", propertyTypeClass = PropertyOp))
        // 2 entries:
        attrs.add(createAttr("state:nv", ++id, "New State"))
        attrs.add(createAttr("state:op", id, "Insert", propertyTypeClass = PropertyOp))
        // re-ordered:
        attrs.add(createAttr("country:op", ++id, "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("country:nv", id, "New Country"))
        // timeableAttribute
        attrs.add(
            createAttr(
                "timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.arrive:op",
                ++id,
                "Update",
                propertyTypeClass = PropertyOp
            )
        )
        attrs.add(createAttr("timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.arrive:nv", id, "New Visit"))

        // New format (since 2024):
        attrs.add(createAttr("name", ++id, value = "B", oldValue = "A", opType = PropertyOpType.Update))
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
        assertAttr(
            master.attributes,
            "timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.arrive",
            "New Visit",
            null
        )
    }

    @Test
    fun testFixOfProxyClassEntries() {
        val attrs = mutableListOf<PfHistoryAttrDO>()
        var id = 0L
        attrs.add(createAttr("account:op", ++id, "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("account:nv", id, "123", propertyTypeClass = "org.projectforge.business.fibu.KontoDO_\$\$_jvst148_1c"))
        attrs.add(createAttr("address:op", ++id, "Update", propertyTypeClass = PropertyOp))
        attrs.add(createAttr("address:nv", id, "456", propertyTypeClass = "org.projectforge.business.address.AddressDO\$HibernateProxy\$En8hKwh8"))
        val master = PfHistoryMasterDO()
        attrs.forEach { attr -> master.add(attr) }
        PFHistoryMasterUtils.transformOldAttributes(master)
        Assertions.assertEquals(2, master.attributes?.size)
        assertAttr(master.attributes, "account", "123", null, propertyTypeClass = "org.projectforge.business.fibu.KontoDO")
        assertAttr(master.attributes, "address", "456", null, propertyTypeClass = "org.projectforge.business.address.AddressDO")
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

    private fun assertAttr(
        attrs: Set<PfHistoryAttrDO>?,
        propertyName: String?,
        newValue: String?,
        oldValue: String?,
        opType: PropertyOpType = PropertyOpType.Update,
        propertyTypeClass: String = "java.lang.String"
    ) {
        Assertions.assertNotNull(attrs)
        attrs!!.find { it.propertyName == propertyName }?.let { attr ->
            Assertions.assertNotNull(attrs, "Attribute not found: $propertyName")
            Assertions.assertEquals(propertyName, attr.propertyName)
            Assertions.assertEquals(newValue, attr.value)
            Assertions.assertEquals(oldValue, attr.oldValue)
            Assertions.assertEquals(opType, attr.opType)
            Assertions.assertEquals(propertyTypeClass, attr.propertyTypeClass)
        }
    }

    private fun createAttr(
        propertyName: String?,
        id: Long = 0,
        value: String? = null,
        oldValue: String? = null,
        opType: PropertyOpType? = null,
        propertyTypeClass: String? = "java.lang.String",
    ): PfHistoryAttrDO {
        val attr = PfHistoryAttrDO()
        attr.id = id
        attr.propertyName = propertyName
        attr.propertyTypeClass = propertyTypeClass
        attr.value = value
        attr.oldValue = oldValue
        attr.opType = opType
        return attr
    }

    companion object {
        val PropertyOp = "de.micromata.genome.db.jpa.history.entities.PropertyOpType"
    }
}
