/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.common

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.user.UserPrefDao

class MagicFilterTest {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun serializationTest() {
        val filter = MagicFilter<AddressFilter>()
        val addressFilter = AddressFilter()
        addressFilter.isLeaved = true
        filter.searchFilter = addressFilter
        filter.entries.add(MagicFilterEntry("zipCode", "12345"))
        val om = UserPrefDao.createObjectMapper()
        var json = om.writeValueAsString(filter)
        var obj = om.readValue(json, MagicFilter::class.java) as MagicFilter<AddressFilter>
        assertTrue(obj.searchFilter!!.isLeaved)

        json = "{\"searchFilter\":{\"type\":\"org.projectforge.business.address.AddressFilter\",\"searchString\":\"\",\"deleted\":false,\"ignoreDeleted\":false,\"maxRows\":50,\"useModificationFilter\":false,\"searchHistory\":false,\"sortProperties\":[{\"sortOrder\":\"ASCENDING\",\"property\":\"name\"},{\"sortOrder\":\"ASCENDING\",\"property\":\"firstName\"}],\"uptodate\":true,\"outdated\":false,\"leaved\":false,\"active\":true,\"nonActive\":false,\"uninteresting\":false,\"personaIngrata\":false,\"departed\":false,\"listType\":\"filter\"},\"entries\":[{\"search\":\"reinh\"},{\"field\":\"modifiedByUser\",\"value\":\"\"}]}"
        obj = om.readValue(json, MagicFilter::class.java) as MagicFilter<AddressFilter>
        assertFalse(obj.searchFilter!!.isLeaved)
    }
}
