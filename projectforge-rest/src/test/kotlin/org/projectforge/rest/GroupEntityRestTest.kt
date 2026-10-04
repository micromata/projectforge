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


package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.ldap.GroupDOConverter
import org.projectforge.business.ldap.LdapGroupValues
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.springframework.beans.factory.annotation.Autowired

class GroupEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupEntityRest: GroupEntityRest

    @Autowired
    private lateinit var groupDOConverter: GroupDOConverter

    @Test
    fun `the gid must be a posix gid`() {
        logon(TEST_ADMIN_USER)
        listOf(0, -1, 65536).forEach { gid ->
            val errors = groupEntityRest.validate(GroupDO(), PostData(Group(name = "GroupEntityRestTest-$gid").also {
                it.gidNumber = gid
            }, watchFieldsTriggered = null, serverData = null))
            assertEquals(listOf("gidNumber"), errors?.map { it.fieldId }, "gid $gid")
        }
    }

    @Test
    fun `a save without a gid keeps the ldap values`() {
        logon(TEST_ADMIN_USER)
        val xml = groupDOConverter.getLdapValuesAsXml(LdapGroupValues().setGidNumber(4711))
        val dto = Group(name = "GroupEntityRestTest-keep").also { it.ldapValues = xml }
        assertEquals(xml, groupEntityRest.transformForDB(dto).ldapValues)

        dto.gidNumber = 4712
        val values = groupDOConverter.readLdapGroupValues(groupEntityRest.transformForDB(dto).ldapValues)
        assertEquals(4712, values?.gidNumber)
    }

    @Test
    fun `a copy drops the gid of its original`() {
        val dto = Group(id = 1, name = "GroupEntityRestTest-clone").also {
            it.gidNumber = 4711
            it.ldapValues = "<ldapConfig gidNumber=\"4711\"/>"
        }
        groupEntityRest.prepareClone(dto)
        assertNull(dto.gidNumber)
        assertNull(dto.ldapValues)
    }
}
