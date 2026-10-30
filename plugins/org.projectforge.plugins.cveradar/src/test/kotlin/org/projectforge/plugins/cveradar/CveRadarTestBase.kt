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
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrAreaDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCatalogEdgeDO
import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.model.CvrCpeMatchDO
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrConfirmationDO
import org.projectforge.plugins.cveradar.model.CvrDeviceDO
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrInstanceDO
import org.projectforge.plugins.cveradar.model.CvrRatingDO
import org.projectforge.plugins.cveradar.model.CvrSystemDO
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
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

    init {
        // All entities of the plugin for every test class: the scan happens once, so the outcome must not depend on
        // which test class runs first.
        listOf(
            CvrCaseDO::class.java, CvrTeamCheckDO::class.java, CvrConfirmationDO::class.java, CvrDeviceDO::class.java,
            CvrVulnerabilityDO::class.java, CvrImportStateDO::class.java, CvrRatingDO::class.java,
            CvrAreaDO::class.java, CvrSystemDO::class.java, CvrInstanceDO::class.java, CvrComponentDO::class.java,
            CvrTechnologyDO::class.java, CvrCatalogNodeDO::class.java, CvrCatalogEdgeDO::class.java,
            CvrCpeMatchDO::class.java, CvrFindingDO::class.java,
        ).forEach { MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(it.canonicalName) }
    }

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
            CvrRole.MANAGEMENT -> addToGroup(user, "cveradar-mg-test") { cveRadarConfig.managementGroup = it }
            null -> {}
        }
        return initTestDB.getUser(username)!!
    }

    /** Makes the user a member of the group (created on first use). */
    protected fun addToGroup(user: PFUserDO, groupName: String) = addToGroup(user, groupName) {}

    private fun addToGroup(user: PFUserDO, groupName: String, configure: (String) -> Unit) {
        val group: GroupDO = initTestDB.getGroup(groupName) ?: initTestDB.addGroup(groupName)
        val db = testGroupDao.find(group.id, checkAccess = false)!!
        db.assignedUsers = (db.assignedUsers ?: mutableSetOf()).also { it.add(user) }
        testGroupDao.update(db, checkAccess = false)
        testUserGroupCache.setExpired()
        configure(groupName)
    }
}
