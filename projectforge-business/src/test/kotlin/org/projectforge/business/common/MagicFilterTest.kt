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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.user.UserPrefDao

class MagicFilterTest {
    @Test
    fun serializationTest() {
        val filter = MagicFilter<AddressFilter>()
        val addressFilter = AddressFilter()
        addressFilter.setLeaved(true)
        filter.searchFilter = addressFilter
        val om = UserPrefDao.createObjectMapper()
        val json = om.writeValueAsString(filter)
        val obj = om.readValue(json, MagicFilter::class.java) as MagicFilter<AddressFilter>
        assertTrue(obj.searchFilter!!.isLeaved)
    }
}
