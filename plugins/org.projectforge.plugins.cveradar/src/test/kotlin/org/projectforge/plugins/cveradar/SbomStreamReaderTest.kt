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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** The SBOMs read as a stream: only purls with a version, places of syft and of the evidence, any order of the fields. */
class SbomStreamReaderTest {
    @Test
    fun `CycloneDX with nested components, scopes, files and the subject after the components`() {
        val json = """
            {
              "components": [
                {"type": "file", "name": "a.jar", "hashes": [{"alg": "SHA-256", "content": "x"}]},
                {"type": "library", "name": "jackson-databind", "purl": "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.2",
                 "properties": [{"name": "syft:location:0:path", "value": "/app/lib/jackson.jar"}, {"name": "syft:package:type", "value": "java"}],
                 "components": [{"purl": "pkg:npm/%40scope/inner@1.0.0", "evidence": {"occurrences": [{"location": "web/package-lock.json"}]}}]},
                {"purl": "pkg:maven/junit/junit@4.13.2", "scope": "excluded"},
                {"purl": "pkg:maven/org.example/declared@${'$'}{version}"},
                {"purl": "pkg:maven/org.example/without-version"},
                {"purl": "pkg:golang/golang.org/x/net@v0.20.0", "properties": [{"value": "/bin/a", "name": "syft:location:0:path"}]},
                {"purl": "pkg:golang/golang.org/x/net@v0.20.0", "properties": [{"name": "syft:location:0:path", "value": "/bin/b"}]}
              ],
              "metadata": {"timestamp": "2026-01-01", "component": {"type": "container", "name": "nginx", "version": "1.27", "properties": []}},
              "dependencies": [{"ref": "a", "dependsOn": ["b"]}],
              "bomFormat": "CycloneDX"
            }
        """.trimIndent()
        val doc = SbomStreamReader.read(json)!!
        assertEquals(LibraryFormat.CYCLONEDX, doc.format)
        assertEquals("container", doc.subject?.type)
        assertEquals("nginx", doc.subject?.name)
        assertEquals("1.27", doc.subject?.version)
        val byName = doc.entries.associateBy { it.name }
        assertEquals(setOf("jackson-databind", "inner", "net", "declared"), byName.keys)
        assertEquals("/app/lib/jackson.jar", byName["jackson-databind"]!!.location)
        assertEquals("web/package-lock.json", byName["inner"]!!.location)
        assertEquals("@scope", byName["inner"]!!.group)
        // The same library found twice: one entry with both places.
        assertEquals("/bin/a, /bin/b", byName["net"]!!.location)
        // The file, the one without a version; the excluded one is not counted.
        assertEquals(2, doc.withoutPurl)
    }

    @Test
    fun `the same libraries as the tree parser before`() {
        val json = """{"bomFormat":"CycloneDX","components":[{"purl":"pkg:deb/debian/openssl@3.0.11?arch=amd64&distro=debian-12"},
            {"purl":"pkg:pypi/requests@2.31.0","scope":"optional"}]}"""
        val parsed = LibraryListParser.parse("bom.json", json)
        assertEquals(LibraryFormat.CYCLONEDX, parsed.format)
        assertEquals(listOf("pkg:deb/debian/openssl@3.0.11?distro=debian-12"), parsed.entries.map { it.purl })
    }

    @Test
    fun `SPDX packages by their purl references`() {
        val json = """{"spdxVersion":"SPDX-2.3","packages":[
            {"name":"a","externalRefs":[{"referenceCategory":"SECURITY","referenceType":"cpe23Type","referenceLocator":"cpe:2.3:a:x:y:1"},
              {"referenceLocator":"pkg:npm/lodash@4.17.21","referenceType":"purl"}]},
            {"name":"b","externalRefs":[{"referenceType":"purl","referenceLocator":"pkg:npm/lodash@4.17.21"}]}]}"""
        val doc = SbomStreamReader.read(json)!!
        assertEquals(LibraryFormat.SPDX, doc.format)
        assertEquals(listOf("pkg:npm/lodash@4.17.21"), doc.entries.map { it.purl })
    }

    @Test
    fun `other JSON is no SBOM, broken JSON throws`() {
        assertNull(SbomStreamReader.read("""{"artifacts":[{"purl":"pkg:npm/a@1"}],"source":{}}"""))
        assertNull(SbomStreamReader.read("""[1, 2]"""))
        assertThrows<Exception> { SbomStreamReader.read("""{"bomFormat":"CycloneDX","components":[{"purl":""") }
    }
}
