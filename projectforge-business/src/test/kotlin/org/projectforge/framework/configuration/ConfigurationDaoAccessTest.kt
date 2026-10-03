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

package org.projectforge.framework.configuration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * Who may see and change which configuration parameter ([ConfigurationDao.hasAccess]): admins the admin
 * parameters (and see the finance ones), PF_Finance and PF_Controlling the finance ones only, and nobody a
 * parameter with a page of its own through the configuration page.
 */
class ConfigurationDaoAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Test
    fun `select and update per editor group`() {
        // user -> (select admin param, update admin param, select finance param, update finance param)
        val expected = mapOf(
            TEST_ADMIN_USER to listOf(true, true, true, false),
            TEST_FINANCE_USER to listOf(false, false, true, true),
            TEST_CONTROLLING_USER to listOf(false, false, true, true),
            TEST_USER to listOf(false, false, false, false),
        )
        expected.forEach { (username, access) ->
            val actual = listOf(
                access(username, ConfigurationParam.SYSTEM_ADMIN_E_MAIL, OperationType.SELECT),
                access(username, ConfigurationParam.SYSTEM_ADMIN_E_MAIL, OperationType.UPDATE),
                access(username, ConfigurationParam.FIBU_DEFAULT_VAT, OperationType.SELECT),
                access(username, ConfigurationParam.FIBU_DEFAULT_VAT, OperationType.UPDATE),
            )
            assertEquals(access, actual, "user: $username")
        }
    }

    @Test
    fun `a parameter with a page of its own is seen but never updated here`() {
        listOf(TEST_ADMIN_USER, TEST_FINANCE_USER, TEST_CONTROLLING_USER).forEach { username ->
            assertEquals(true, access(username, ConfigurationParam.CUSTOMER_GROUPS, OperationType.SELECT), username)
            assertEquals(false, access(username, ConfigurationParam.CUSTOMER_GROUPS, OperationType.UPDATE), username)
        }
    }

    @Test
    fun `nobody inserts or deletes a parameter`() {
        listOf(TEST_ADMIN_USER, TEST_FINANCE_USER).forEach { username ->
            listOf(OperationType.INSERT, OperationType.DELETE, OperationType.UNDELETE).forEach { op ->
                assertEquals(false, access(username, ConfigurationParam.FIBU_DEFAULT_VAT, op), "$username $op")
            }
        }
    }

    private fun access(username: String, param: ConfigurationParam, operationType: OperationType): Boolean {
        val obj = ConfigurationDO().also { it.parameter = param.key }
        return configurationDao.hasAccess(getUser(username), obj, obj, operationType, throwException = false)
    }
}
