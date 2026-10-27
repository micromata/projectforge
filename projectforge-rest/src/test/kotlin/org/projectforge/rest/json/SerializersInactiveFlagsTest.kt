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

package org.projectforge.rest.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate

/**
 * A user, group or employee serialized as a DO reference carries whether it is still active, so the frontend
 * strikes a no longer active one through wherever it is shown.
 */
class SerializersInactiveFlagsTest : AbstractTestBase() {
    private val mapper = ObjectMapper().also { mapper ->
        mapper.registerModule(SimpleModule().also {
            it.addSerializer(PFUserDO::class.java, PFUserDOSerializer())
            it.addSerializer(GroupDO::class.java, GroupDOSerializer())
            it.addSerializer(EmployeeDO::class.java, EmployeeDOSerializer())
        })
    }

    @Test
    fun `a deactivated or deleted user is serialized with its flags`() {
        val user = PFUserDO().also {
            it.id = 4711
            it.username = "inactive"
            it.deactivated = true
        }
        mapper.readTree(mapper.writeValueAsString(user)).let {
            assertEquals(true, it["deactivated"].asBoolean())
            assertEquals(false, it["deleted"].asBoolean())
            assertEquals("inactive", it["username"].asText())
        }
        user.deactivated = false
        user.deleted = true
        mapper.readTree(mapper.writeValueAsString(user)).let {
            assertEquals(false, it["deactivated"].asBoolean())
            assertEquals(true, it["deleted"].asBoolean())
        }
    }

    @Test
    fun `a deleted group is serialized with its flag`() {
        val group = GroupDO().also {
            it.id = 4712
            it.name = "deleted group"
            it.deleted = true
        }
        mapper.readTree(mapper.writeValueAsString(group)).let {
            assertEquals(true, it["deleted"].asBoolean())
            assertEquals("deleted group", it["name"].asText())
        }
    }

    @Test
    fun `an employee who left long ago is serialized as deactivated`() {
        val employee = EmployeeDO().also {
            it.id = 4713
            it.user = PFUserDO().also { user -> user.id = 4714 }
            it.austrittsDatum = LocalDate.now().minusYears(1)
        }
        mapper.readTree(mapper.writeValueAsString(employee)).let {
            assertEquals(true, it["deactivated"].asBoolean())
        }
        employee.austrittsDatum = null
        mapper.readTree(mapper.writeValueAsString(employee)).let {
            assertEquals(false, it["deactivated"].asBoolean())
        }
    }
}
