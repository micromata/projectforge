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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.rest.SbomImportKind
import org.projectforge.plugins.cveradar.rest.SbomImportStatus

/**
 * The targets of a bulk import as a scanner of all repositories writes them: one folder per repository (with the
 * SBOMs of its modules below), one per image, an aggregate at the top.
 */
class SbomImportPlannerTest {
    private fun libs(prefix: String, n: Int) = (1..n).map { LibraryEntry("maven", "org.$prefix", "lib$it", "1.$it") }

    private fun doc(path: String, entries: List<LibraryEntry>, type: String? = "application", name: String? = null, version: String? = null) =
        SbomImportDocument(path, SbomDocument(LibraryFormat.CYCLONEDX, SbomSubject(type, name, version), entries, 1))

    private val repoA = libs("a", 10)
    private val repoB = libs("b", 8)
    private val image = libs("img", 6)

    private val documents = listOf(
        doc("all/all.cdx.json", repoA + repoB + image, name = "all"),
        doc("all/repositories/repo-a/bom.cdx.json", repoA.take(6), name = "repo-a"),
        // A module of the repository: the SBOM of a build plugin, named after the module.
        doc("all/repositories/repo-a/native/x1/bom.cdx.json", repoA.drop(4), type = "library", name = "module-of-a"),
        doc("all/repositories/repo-a/scan/syft.cdx.json", repoA.take(2), type = "file", name = "/home/someone/repos/repo-a"),
        doc("all/repositories/repo-b/bom.cdx.json", repoB, name = "repo-b"),
        doc("all/images/0815/syft.cdx.json", image, type = "container", name = "registry.example.org/team/app", version = "2.1"),
        doc("all/images/4711/syft.cdx.json", emptyList(), type = "container", name = "busybox", version = "1.36"),
    )

    @Test
    fun `repositories by their folder, images by name and tag, the aggregate recognised`() {
        val inventory = SbomImportInventory("PROJ", mapOf("REPO-B" to setOf("SBOM")), setOf("PROJ", "OTHER"))
        val targets = SbomImportPlanner.plan(documents, inventory, "Container images").associateBy { it.key }
        assertEquals(
            setOf("F:all.cdx.json", "R:repositories/repo-a", "R:repositories/repo-b", "C:registry.example.org/team/app:2.1", "C:busybox:1.36"),
            targets.keys,
        )
        val a = targets["R:repositories/repo-a"]!!
        assertEquals(SbomImportKind.REPOSITORY, a.kind)
        assertEquals("REPO-A", a.systemCode)
        assertEquals("repo-a", a.systemName)
        assertEquals("SBOM", a.componentCode)
        assertEquals(3, a.files.size)
        assertEquals(10, a.libraryCount)
        assertEquals(SbomImportStatus.NEW, a.status)
        assertTrue(a.proposed)

        assertEquals(SbomImportStatus.UPDATE, targets["R:repositories/repo-b"]!!.status)

        val app = targets["C:registry.example.org/team/app:2.1"]!!
        assertEquals(SbomImportKind.CONTAINER, app.kind)
        assertEquals("PROJ-IMAGES", app.systemCode)
        assertEquals("Container images", app.systemName)
        assertEquals("REGISTRY.EXAMPLE.ORG-TEAM-APP-2.1", app.componentCode)
        assertEquals("registry.example.org/team/app:2.1", app.componentName)

        val empty = targets["C:busybox:1.36"]!!
        assertEquals(SbomImportStatus.EMPTY, empty.status)
        assertFalse(empty.selectable)

        val aggregate = targets["F:all.cdx.json"]!!
        assertEquals(SbomImportStatus.AGGREGATE, aggregate.status)
        assertEquals(3, aggregate.aggregateOf)
        assertTrue(aggregate.selectable)
        assertFalse(aggregate.proposed)
    }

    @Test
    fun `a code taken elsewhere gets the area in front, a code of an area is a conflict`() {
        val docs = listOf(doc("x/bom.json", repoA, name = "x"), doc("y/bom.json", repoB, name = "y"), doc("z/bom.json", image, name = "z"))
        // X: a system of another area, Y and PROJ-Y both taken.
        val inventory = SbomImportInventory("PROJ", emptyMap(), setOf("PROJ", "X", "Y", "PROJ-Y"))
        val targets = SbomImportPlanner.plan(docs, inventory, "Images").associateBy { it.systemName }
        assertEquals("PROJ-X", targets["x"]!!.systemCode)
        assertEquals(SbomImportStatus.NEW, targets["x"]!!.status)
        assertEquals("PROJ-Y", targets["y"]!!.systemCode)
        assertEquals(SbomImportStatus.CONFLICT, targets["y"]!!.status)
        assertTrue(targets["y"]!!.selectable)
        assertFalse(targets["y"]!!.proposed)
        assertEquals("Z", targets["z"]!!.systemCode)
    }

    @Test
    fun `the system of the images gets a free code if another area has it`() {
        val docs = listOf(doc("images/1/syft.json", image, type = "container", name = "app", version = "1"))
        val inventory = SbomImportInventory("PROJ", emptyMap(), setOf("PROJ", "PROJ-IMAGES"))
        val target = SbomImportPlanner.plan(docs, inventory, "Images").single()
        assertEquals("PROJ-IMAGES-2", target.systemCode)
        assertEquals(SbomImportStatus.NEW, target.status)
    }

    @Test
    fun `codes as the inventory takes them`() {
        assertEquals("BACKEND-SHOP-API", SbomImportPlanner.code("backend-shop-api"))
        assertEquals("MCR.MICROSOFT.COM-DOTNET-SDK-8.0", SbomImportPlanner.code("mcr.microsoft.com/dotnet/sdk:8.0"))
        assertEquals("A-B", SbomImportPlanner.code("--a  ++ b--"))
        assertEquals("X", SbomImportPlanner.code("ÄÖÜ"))
        val long = SbomImportPlanner.code("registry.example.org/a/very/long/path/of/an/image/name:1.2.3-alpine")
        val other = SbomImportPlanner.code("registry.example.org/a/very/long/path/of/an/image/name:1.2.4-alpine")
        assertTrue(long.length <= 50)
        assertTrue(Regex("""^[A-Z0-9][A-Z0-9._-]{0,49}$""").matches(long))
        assertTrue(long != other)
    }

    @Test
    fun `two repositories of the same code are told apart`() {
        val docs = listOf(doc("a/app/bom.json", repoA, name = "app"), doc("b/app/bom.json", repoB, name = "app"))
        val targets = SbomImportPlanner.plan(docs, SbomImportInventory("P", emptyMap(), emptySet()), "Images")
        assertEquals(listOf("APP", "APP-2"), targets.map { it.systemCode })
    }
}
