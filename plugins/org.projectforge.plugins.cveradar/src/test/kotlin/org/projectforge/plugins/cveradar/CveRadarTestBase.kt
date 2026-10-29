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

package org.projectforge.plugins.cveradar

import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * Test users of the CVE-Radar: the roles are PF groups named in the configuration, so a user gets a role by membership
 * in the group of the role.
 */
abstract class CveRadarTestBase : AbstractTestBase() {
    @Autowired
    protected lateinit var cveRadarConfig: CveRadarConfig

    @Autowired
    private lateinit var testGroupDao: GroupDao

    @Autowired
    private lateinit var testUserGroupCache: UserGroupCache

    /** A new user with the given role (null: none) in the groups configured for the roles (created on first use). */
    protected fun user(name: String, role: CvrRole?): PFUserDO {
        val username = "${javaClass.simpleName}.$name"
        initTestDB.getUser(username)?.let { return it }
        logon(TEST_ADMIN_USER)
        val user = PFUserDO().also {
            it.username = username
            it.firstname = name
            it.lastname = "CveRadar"
        }
        initTestDB.addUser(user)
        when (role) {
            CvrRole.READER -> addToGroup(user, "cveradar-read-test") { cveRadarConfig.readGroup = it }
            CvrRole.SECURITY_TEAM -> addToGroup(user, "cveradar-write-test") { cveRadarConfig.writeGroup = it }
            CvrRole.SECURITY_OFFICER -> addToGroup(user, "cveradar-so-test") { cveRadarConfig.securityOfficerGroup = it }
            null -> {}
        }
        return initTestDB.getUser(username)!!
    }

    private fun addToGroup(user: PFUserDO, groupName: String, configure: (String) -> Unit) {
        val group: GroupDO = initTestDB.getGroup(groupName) ?: initTestDB.addGroup(groupName)
        val db = testGroupDao.find(group.id, checkAccess = false)!!
        db.assignedUsers = (db.assignedUsers ?: mutableSetOf()).also { it.add(user) }
        testGroupDao.update(db, checkAccess = false)
        testUserGroupCache.setExpired()
        configure(groupName)
    }
}
