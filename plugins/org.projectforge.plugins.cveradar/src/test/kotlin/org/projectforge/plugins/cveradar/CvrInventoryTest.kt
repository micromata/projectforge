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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrConfidentiality
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.InstanceRequest
import org.projectforge.plugins.cveradar.rest.InventoryLevel
import org.projectforge.plugins.cveradar.rest.InventoryRow
import org.projectforge.plugins.cveradar.rest.MoveRequest
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.projectforge.plugins.cveradar.rest.TechnologyRequest
import org.springframework.beans.factory.annotation.Autowired

/**
 * The inventory: the groups along the tree decide who sees and changes a node, the security team reads internal
 * systems only, whoever sees a node sees the way to it as a frame, and only the security officer moves an area
 * around.
 */
class CvrInventoryTest : CveRadarTestBase() {
    @Autowired
    private lateinit var rest: CvrInventoryRest

    private fun rows(user: PFUserDO): Map<String, InventoryRow> {
        logon(user)
        return rest.tree().rows.associateBy { it.code!! }
    }

    private fun groupId(name: String): Long = (initTestDB.getGroup(name) ?: initTestDB.addGroup(name)).id!!

    @Test
    fun `a root area needs a group of the creator, below it the write access to the parent`() {
        val owner = user("inv-owner", null)
        val groupId = groupId("cveradar-inv-g1")
        val other = user("inv-other", null)
        logon(owner)
        // Not a member of the group yet.
        assertThrows(UserException::class.java) { rest.createArea(AreaRequest(code = "IV1", name = "A", groupId = groupId)) }
        addToGroup(owner, "cveradar-inv-g1")
        logon(owner)
        assertThrows(UserException::class.java) { rest.createArea(AreaRequest(code = "IV1", name = "A")) }
        val area = rest.createArea(AreaRequest(code = "iv1", name = "Acme", groupId = groupId))
        assertEquals(setOf("IV1"), rows(owner).keys.filter { it.startsWith("IV") }.toSet(), "the code is normalised")
        assertThrows(UserException::class.java) { rest.createArea(AreaRequest(code = "IV1", name = "Again", groupId = groupId)) }
        val sub = rest.createArea(AreaRequest(parentId = area, code = "IV1-L", name = "Logistics"))
        assertNotNull(rows(owner)["IV1-L"])
        logon(other)
        assertThrows(AccessException::class.java) { rest.createArea(AreaRequest(parentId = area, code = "IV1-X", name = "X")) }
        assertThrows(AccessException::class.java) { rest.updateArea(sub, AreaRequest(code = "IV1-L", name = "Hijacked")) }
        assertThrows(AccessException::class.java) { rest.createSystem(SystemRequest(areaId = sub, code = "IVS", name = "S")) }
        assertThrows(AccessException::class.java) { rest.delete(InventoryLevel.AREA, sub) }
        assertTrue(rows(other).keys.none { it.startsWith("IV1") }, "an outsider sees nothing")
    }

    @Test
    fun `the group sees the way to its area as a frame only, without the siblings, after the officer moved it`() {
        val so = user("inv-so", CvrRole.SECURITY_OFFICER)
        val member = user("inv-member", null)
        val groupId = groupId("cveradar-inv-g2")
        addToGroup(member, "cveradar-inv-g2")
        logon(so)
        val customer = rest.createArea(AreaRequest(code = "IV2-CUST", name = "Customer", groupId = groupId("cveradar-inv-g2other")))
        val sibling = rest.createArea(AreaRequest(parentId = customer, code = "IV2-SIB", name = "Sibling"))
        logon(member)
        val own = rest.createArea(AreaRequest(code = "IV2-OWN", name = "Own", groupId = groupId))
        assertThrows(AccessException::class.java) { rest.move(InventoryLevel.AREA, own, MoveRequest(customer)) }
        assertThrows(AccessException::class.java) { rest.move(InventoryLevel.AREA, own, MoveRequest(null)) }
        logon(so)
        rest.move(InventoryLevel.AREA, own, MoveRequest(customer))
        val seen = rows(member)
        assertEquals(true, seen["IV2-CUST"]!!.frameOnly)
        assertFalse(seen["IV2-CUST"]!!.canEdit)
        assertNull(seen["IV2-CUST"]!!.effectiveGroupName, "a frame shows no values")
        assertEquals(false, seen["IV2-OWN"]!!.frameOnly)
        assertTrue(seen["IV2-OWN"]!!.canEdit)
        assertNull(seen["IV2-SIB"], "the sibling stays hidden")
        // Never into itself or below a descendant.
        logon(so)
        assertThrows(UserException::class.java) { rest.move(InventoryLevel.AREA, customer, MoveRequest(own)) }
        assertThrows(UserException::class.java) { rest.move(InventoryLevel.AREA, customer, MoveRequest(customer)) }
        assertNotNull(sibling)
    }

    @Test
    fun `systems, instances and components with technologies, the security team reads internal systems only`() {
        val owner = user("inv-owner3", null)
        addToGroup(owner, "cveradar-inv-g3")
        val team = user("inv-team3", CvrRole.SECURITY_TEAM)
        logon(owner)
        val area = rest.createArea(AreaRequest(code = "IV3", name = "Three", groupId = groupId("cveradar-inv-g3")))
        val internal = rest.createSystem(SystemRequest(areaId = area, code = "IV3-PF", name = "ProjectForge"))
        val secret = rest.createSystem(
            SystemRequest(areaId = area, code = "IV3-SEC", name = "Secret", confidentiality = CvrConfidentiality.CONFIDENTIAL),
        )
        val db = rest.createComponent(
            ComponentRequest(
                systemId = internal, code = "DB", name = "Database",
                technologies = listOf(TechnologyRequest(kind = CvrTechnologyKind.PRODUCT, vendor = "postgresql", product = "postgresql", version = "16.4")),
            ),
        )
        var seen = rows(owner)
        assertEquals(listOf("16.4"), seen["IV3-PF-DB"]!!.technologies.map { it.version })
        rest.createInstance(InstanceRequest(systemId = internal, code = "hq-prod", stage = "Prod", site = "HQ", confidentiality = CvrConfidentiality.STRICTLY_CONFIDENTIAL))
        seen = rows(owner)
        assertEquals("IV3-PF-HQ-PROD", seen["IV3-PF-HQ-PROD"]!!.code)
        assertEquals(CvrConfidentiality.STRICTLY_CONFIDENTIAL, seen["IV3-PF-HQ-PROD"]!!.confidentiality)
        val own = rest.createComponent(ComponentRequest(instanceId = seen["IV3-PF-HQ-PROD"]!!.id, code = "CACHE", name = "Cache"))
        assertNotNull(rows(owner)["IV3-PF-HQ-PROD-CACHE"])
        // Changing the technologies: the missing one is removed, a new one added.
        val techId = rows(owner)["IV3-PF-DB"]!!.technologies.single().id
        rest.updateComponent(
            db,
            ComponentRequest(
                code = "DB", name = "Database",
                technologies = listOf(
                    TechnologyRequest(id = techId, product = "postgresql", vendor = "postgresql", version = "16.5"),
                    TechnologyRequest(kind = CvrTechnologyKind.ECOSYSTEM, product = "maven"),
                ),
            ),
        )
        assertEquals(setOf("16.5", null), rows(owner)["IV3-PF-DB"]!!.technologies.map { it.version }.toSet())
        assertThrows(UserException::class.java) { rest.createComponent(ComponentRequest(systemId = internal, code = "DB", name = "Twice")) }
        assertThrows(UserException::class.java) { rest.delete(InventoryLevel.SYSTEM, internal) }

        val seenByTeam = rows(team)
        assertNotNull(seenByTeam["IV3-PF"], "internal system")
        assertNull(seenByTeam["IV3-SEC"], "confidential system")
        assertNull(seenByTeam["IV3-PF-HQ-PROD"], "an instance raised to strictly confidential")
        assertEquals(true, seenByTeam["IV3"]!!.frameOnly)
        assertFalse(seenByTeam["IV3-PF"]!!.canEdit, "the team reads, the group writes")
        logon(team)
        assertThrows(AccessException::class.java) { rest.updateSystem(internal, SystemRequest(code = "IV3-PF", name = "x")) }
        assertThrows(AccessException::class.java) { rest.createComponent(ComponentRequest(systemId = internal, code = "X", name = "x")) }

        logon(owner)
        rest.delete(InventoryLevel.COMPONENT, own)
        rest.delete(InventoryLevel.SYSTEM, secret)
        assertNull(rows(owner)["IV3-SEC"])
        assertThrows(UserException::class.java) { rest.delete(InventoryLevel.AREA, area) }
    }

    @Test
    fun `a user without a group sees nothing and changes nothing`() {
        val owner = user("inv-owner4", null)
        addToGroup(owner, "cveradar-inv-g4")
        logon(owner)
        val area = rest.createArea(AreaRequest(code = "IV4", name = "Four", groupId = groupId("cveradar-inv-g4")))
        val system = rest.createSystem(SystemRequest(areaId = area, code = "IV4-S", name = "S"))
        val nobody = user("inv-nobody", null)
        assertTrue(rows(nobody).values.none { it.canEdit })
        assertThrows(AccessException::class.java) { rest.updateSystem(system, SystemRequest(code = "IV4-S", name = "x")) }
        assertThrows(AccessException::class.java) { rest.createInstance(InstanceRequest(systemId = system, code = "A")) }
        assertThrows(AccessException::class.java) { rest.move(InventoryLevel.SYSTEM, system, MoveRequest(area)) }
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { rest.delete(InventoryLevel.SYSTEM, system) }
    }
}
