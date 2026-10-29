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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.plugins.cveradar.model.CvrCatalogKind
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.CatalogMergeRequest
import org.projectforge.plugins.cveradar.rest.CatalogNodeRequest
import org.projectforge.plugins.cveradar.rest.CatalogRow
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrCatalogRest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.projectforge.plugins.cveradar.rest.TechnologyRequest
import org.springframework.beans.factory.annotation.Autowired

/**
 * The technology catalogue (INV-28): everybody logged in reads and picks, the security team maintains; a library
 * may hang below several frameworks, nodes are merged and deleted without losing the references of the inventory.
 */
class CvrCatalogTest : CveRadarTestBase() {
    @Autowired
    private lateinit var catalogService: CvrCatalogService

    /** Done by the plugin when it starts. */
    @BeforeEach
    fun seedEcosystems() {
        logon(TEST_ADMIN_USER)
        catalogService.ensureEcosystems()
    }

    @Autowired
    private lateinit var rest: CvrCatalogRest

    @Autowired
    private lateinit var inventory: CvrInventoryRest

    private fun rows(): List<CatalogRow> = rest.tree().rows

    private fun node(
        name: String,
        kind: CvrCatalogKind,
        vararg parents: Long,
        purlName: String? = null,
    ): Long = rest.create(
        CatalogNodeRequest(kind = kind, name = name, parentIds = parents.toList(), purlType = "maven", purlName = purlName),
    )

    private fun maven(): Long = rows().first { it.ecosystemKey == "Maven" }.id

    @Test
    fun `everybody reads and picks, only the security team changes`() {
        val st = user("cat-st", CvrRole.SECURITY_TEAM)
        logon(TEST_USER)
        val tree = rest.tree()
        assertFalse(tree.canEdit)
        assertTrue(tree.rows.any { it.ecosystemKey == "npm" }, "the ecosystems are there")
        assertTrue(rest.autosearch("maven", 30).any { it.ecosystem == "Maven" })
        val request = CatalogNodeRequest(kind = CvrCatalogKind.PRODUCT, name = "Denied")
        assertThrows(AccessException::class.java) { rest.create(request) }
        assertThrows(AccessException::class.java) { rest.update(maven(), request) }
        assertThrows(AccessException::class.java) { rest.merge(maven(), CatalogMergeRequest(maven())) }
        assertThrows(AccessException::class.java) { rest.delete(maven()) }
        logon(st)
        assertTrue(rest.tree().canEdit)
        assertTrue(node("Cat Denied Now Allowed", CvrCatalogKind.PRODUCT) > 0)
    }

    @Test
    fun `the ecosystems are created once`() {
        logon(user("cat-st2", CvrRole.SECURITY_TEAM))
        rest.tree()
        assertEquals(1, rows().count { it.ecosystemKey == "Maven" })
        assertEquals(1, rows().count { it.ecosystemKey == "crates.io" })
        assertThrows(UserException::class.java) {
            rest.create(CatalogNodeRequest(kind = CvrCatalogKind.ECOSYSTEM, name = "Again", ecosystemKey = "Maven"))
        }
        val renamed = assertThrows(UserException::class.java, {
            rest.update(maven(), CatalogNodeRequest(kind = CvrCatalogKind.ECOSYSTEM, name = "Java", ecosystemKey = "maven2", purlType = "maven"))
        }, "the key of an ecosystem stays")
        assertEquals(CvrCatalogService.ERROR_ECOSYSTEM_KEY, renamed.i18nKey)
        rest.update(maven(), CatalogNodeRequest(kind = CvrCatalogKind.ECOSYSTEM, name = "Java/Maven", ecosystemKey = "Maven", purlType = "maven"))
    }

    @Test
    fun `a library below two frameworks shows twice, a cycle is refused`() {
        logon(user("cat-st3", CvrRole.SECURITY_TEAM))
        val boot = node("Cat3 Spring Boot", CvrCatalogKind.PRODUCT, maven())
        val quarkus = node("Cat3 Quarkus", CvrCatalogKind.PRODUCT, maven())
        val jackson = node("Cat3 jackson-databind", CvrCatalogKind.LIBRARY, boot, quarkus, purlName = "jackson-databind")
        val shown = rows().filter { it.id == jackson }
        assertEquals(2, shown.size)
        assertEquals(setOf("Cat3 Spring Boot", "Cat3 Quarkus"), shown[0].parents.map { it.displayName }.toSet())
        assertEquals(2, shown.map { it.key }.toSet().size)
        assertTrue(shown.all { it.parentKey != null })
        // The framework below its own library, and a node below itself.
        assertThrows(UserException::class.java) {
            rest.update(boot, CatalogNodeRequest(kind = CvrCatalogKind.PRODUCT, name = "Cat3 Spring Boot", parentIds = listOf(jackson)))
        }
        assertThrows(UserException::class.java) {
            rest.update(boot, CatalogNodeRequest(kind = CvrCatalogKind.PRODUCT, name = "Cat3 Spring Boot", parentIds = listOf(boot)))
        }
        assertThrows(UserException::class.java, {
            node("Cat3 duplicate purl", CvrCatalogKind.LIBRARY, boot, purlName = "jackson-databind")
        }, "a purl names one node")
        // One parent less.
        rest.update(
            jackson,
            CatalogNodeRequest(kind = CvrCatalogKind.LIBRARY, name = "Cat3 jackson-databind", parentIds = listOf(boot), purlType = "maven", purlName = "jackson-databind"),
        )
        assertEquals(1, rows().count { it.id == jackson })
        val ref = rest.autosearch("jackson", 30).single { it.id == jackson }
        assertEquals("Maven", ref.ecosystem)
        assertEquals("pkg:maven/jackson-databind", ref.purl)
        assertEquals("Cat3 jackson-databind (Maven)", ref.displayName)
    }

    @Test
    fun `merging moves the references and the edges, deleting needs an empty node`() {
        logon(user("cat-st4", CvrRole.SECURITY_TEAM))
        val boot = node("Cat4 Spring Boot", CvrCatalogKind.PRODUCT, maven())
        val twin = node("Cat4 SpringBoot", CvrCatalogKind.PRODUCT, maven())
        val lib = node("Cat4 tomcat", CvrCatalogKind.LIBRARY, twin)
        val so = user("cat-so4", CvrRole.SECURITY_OFFICER)
        val owner = user("cat-owner4", null)
        val groupId = (initTestDB.getGroup("cveradar-cat-g4") ?: initTestDB.addGroup("cveradar-cat-g4")).id!!
        addToGroup(owner, "cveradar-cat-g4")
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = "CAT4", name = "Cat4", groupId = groupId))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "CAT4-S", name = "S"))
        inventory.createComponent(
            ComponentRequest(
                systemId = system, code = "CAT4-C", name = "App",
                technologies = listOf(TechnologyRequest(kind = CvrTechnologyKind.PRODUCT, product = "SpringBoot", version = "3.5", catalogNodeId = twin)),
            ),
        )
        assertEquals(twin, inventory.tree().rows.single { it.code == "CAT4-S-CAT4-C" }.technologies.single().catalogNodeId)
        assertThrows(UserException::class.java, {
            inventory.createComponent(
                ComponentRequest(
                    systemId = system, code = "CAT4-D", name = "Bad",
                    technologies = listOf(TechnologyRequest(kind = CvrTechnologyKind.PRODUCT, product = "x", catalogNodeId = -7)),
                ),
            )
        }, "an unknown node is refused")
        logon(so)
        assertThrows(UserException::class.java, { rest.delete(twin) }, "children and references")
        assertThrows(UserException::class.java) { rest.merge(maven(), CatalogMergeRequest(boot)) }
        rest.merge(twin, CatalogMergeRequest(boot))
        val tech = inventory.tree().rows.single { it.code == "CAT4-S-CAT4-C" }.technologies.single()
        assertEquals(boot, tech.catalogNodeId)
        assertEquals("Cat4 Spring Boot", tech.catalogName)
        assertNull(rows().firstOrNull { it.id == twin })
        assertTrue(rows().single { it.id == boot }.aliases!!.contains("Cat4 SpringBoot"))
        assertEquals(boot, rows().single { it.id == lib }.parents.single().id, "the library moved to the target")
        // A node nobody refers to is deleted.
        assertThrows(UserException::class.java) { rest.delete(boot) }
        rest.delete(lib)
        assertNotNull(rows().firstOrNull { it.id == boot })
        assertNull(rows().firstOrNull { it.id == lib })
    }
}
