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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.AccessException
import org.projectforge.plugins.cveradar.model.CvrCatalogKind
import org.projectforge.plugins.cveradar.model.CvrOrigin
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.CatalogNodeRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrCatalogRest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.LibraryFile
import org.projectforge.plugins.cveradar.rest.LibraryRequest
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.projectforge.plugins.cveradar.rest.TechnologyRequest
import org.springframework.beans.factory.annotation.Autowired

/**
 * The libraries of a component read from lists (INV-26): the group of the system uploads, an outsider is refused, lists
 * are merged, `replace` removes what is missing, technologies entered by hand stay, the catalogue is linked.
 */
class CvrLibraryTest : CveRadarTestBase() {
    @Autowired
    private lateinit var catalogService: CvrCatalogService

    /** Done by the plugin when it starts. */
    @BeforeEach
    fun seedEcosystems() {
        logon(TEST_ADMIN_USER)
        catalogService.ensureEcosystems()
    }

    @Autowired
    private lateinit var rest: CvrInventoryRest

    @Autowired
    private lateinit var catalog: CvrCatalogRest

    private val mavenA = "    com.fasterxml.jackson.core:jackson-databind:jar:2.17.2:compile\n    org.slf4j:slf4j-api:jar:2.0.9:compile\n"
    private val mavenB = "    org.slf4j:slf4j-api:jar:2.0.9:compile\n    io.netty:netty-codec:jar:4.1.100.Final:runtime\n"

    private fun libraries(component: String) =
        rest.tree().rows.single { it.code == component }.technologies.filter { it.origin != CvrOrigin.MANUAL }

    @Test
    fun `lists are merged, replace removes the missing, manual technologies stay, the catalogue is linked`() {
        val st = user("lib-st", CvrRole.SECURITY_TEAM)
        logon(st)
        val ecosystem = catalog.tree().rows.first { it.ecosystemKey == "Maven" }.id
        val jackson = catalog.create(
            CatalogNodeRequest(
                kind = CvrCatalogKind.LIBRARY, name = "jackson-databind", parentIds = listOf(ecosystem),
                cpeVendor = "fasterxml", purlType = "maven", purlNamespace = "com.fasterxml.jackson.core", purlName = "jackson-databind",
            ),
        )
        val owner = user("lib-owner", null)
        val outsider = user("lib-outsider", null)
        val groupId = (initTestDB.getGroup("cveradar-lib-g") ?: initTestDB.addGroup("cveradar-lib-g")).id!!
        addToGroup(owner, "cveradar-lib-g")
        logon(owner)
        val area = rest.createArea(AreaRequest(code = "LIB1", name = "Lib", groupId = groupId))
        val system = rest.createSystem(SystemRequest(areaId = area, code = "LIB1-S", name = "S"))
        val component = rest.createComponent(
            ComponentRequest(
                systemId = system, code = "APP", name = "App",
                technologies = listOf(TechnologyRequest(kind = CvrTechnologyKind.PRODUCT, product = "Java", version = "21")),
            ),
        )
        val request = { text: String, replace: Boolean -> LibraryRequest(listOf(LibraryFile("deps.txt", text)), replace) }

        // Seen first, nothing stored.
        val preview = rest.previewLibraries(component, request(mavenA, false))
        assertEquals(2, preview.addedCount)
        assertEquals(0, libraries("LIB1-S-APP").size)

        rest.applyLibraries(component, request(mavenA, false))
        val first = libraries("LIB1-S-APP")
        assertEquals(setOf("jackson-databind", "slf4j-api"), first.map { it.product }.toSet())
        val linked = first.single { it.product == "jackson-databind" }
        assertEquals(jackson, linked.catalogNodeId)
        assertEquals("fasterxml", linked.vendor)
        assertEquals("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.2", linked.purl)
        assertNull(first.single { it.product == "slf4j-api" }.catalogNodeId)

        // A second list only adds.
        val merged = rest.applyLibraries(component, request(mavenB, false))
        assertEquals(1, merged.addedCount)
        assertEquals(1, merged.unchangedCount)
        assertEquals(3, libraries("LIB1-S-APP").size)

        // Replace removes what the lists no longer name.
        val replaced = rest.applyLibraries(component, request(mavenB, true))
        assertEquals(1, replaced.removedCount)
        assertEquals(setOf("slf4j-api", "netty-codec"), libraries("LIB1-S-APP").map { it.product }.toSet())

        // Saving the component dialog (manual technologies only) keeps the libraries; the manual one is untouched.
        val manual = rest.tree().rows.single { it.code == "LIB1-S-APP" }.technologies.single { it.origin == CvrOrigin.MANUAL }
        rest.updateComponent(
            component,
            ComponentRequest(code = "APP", name = "App", technologies = listOf(TechnologyRequest(id = manual.id, product = "Java", version = "22"))),
        )
        assertEquals(2, libraries("LIB1-S-APP").size)
        assertEquals("22", rest.tree().rows.single { it.code == "LIB1-S-APP" }.technologies.single { it.origin == CvrOrigin.MANUAL }.version)

        // All libraries of the lists go with an empty replace.
        rest.applyLibraries(component, LibraryRequest(emptyList(), replace = true))
        assertEquals(0, libraries("LIB1-S-APP").size)
        assertNotNull(rest.tree().rows.single { it.code == "LIB1-S-APP" }.technologies.singleOrNull { it.origin == CvrOrigin.MANUAL })

        // A jar listing has no groupId: the catalogue gives it where the name is unique, the rest is counted.
        val jars = "BOOT-INF/lib/jackson-databind-2.17.2.jar\nBOOT-INF/lib/mystery-1.0.jar\n"
        val jarDiff = rest.applyLibraries(component, LibraryRequest(listOf(LibraryFile("jars.txt", jars)), false))
        assertEquals(2, jarDiff.addedCount)
        assertEquals(1, jarDiff.withoutGroupIdCount)
        val fromJars = libraries("LIB1-S-APP").filter { it.origin == CvrOrigin.JAR_LIST }
        assertEquals(
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.2",
            fromJars.single { it.product == "jackson-databind" }.purl,
        )
        assertEquals(jackson, fromJars.single { it.product == "jackson-databind" }.catalogNodeId)
        assertEquals("pkg:maven/mystery@1.0", fromJars.single { it.product == "mystery" }.purl)

        logon(outsider)
        assertThrows(AccessException::class.java) { rest.previewLibraries(component, request(mavenA, false)) }
        assertThrows(AccessException::class.java) { rest.applyLibraries(component, request(mavenA, false)) }
    }
}
