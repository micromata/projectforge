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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException

/** The lists the libraries of a component are read from: SBOMs, npm lockfile, Maven and Gradle output. */
class LibraryListParserTest {
    private fun purls(file: ParsedLibraryFile) = file.entries.map { it.purl }

    @Test
    fun `package-lock v3 reads the packages by their name, without the root, links and development dependencies`() {
        val json = """
            {"lockfileVersion":3,"packages":{
              "":{"name":"app","version":"1.0.0"},
              "node_modules/lodash":{"version":"4.17.21"},
              "node_modules/@angular/core":{"version":"18.2.1"},
              "node_modules/a/node_modules/lodash":{"version":"3.10.1"},
              "node_modules/mocha":{"version":"10.0.0","dev":true},
              "node_modules/mine":{"link":true,"resolved":"packages/mine"},
              "packages/mine":{"version":"0.0.1"}
            }}
        """.trimIndent()
        val file = LibraryListParser.parse("package-lock.json", json)
        assertEquals(LibraryFormat.NPM, file.format)
        assertEquals(
            listOf("pkg:npm/lodash@4.17.21", "pkg:npm/%40angular/core@18.2.1", "pkg:npm/lodash@3.10.1"),
            purls(file),
        )
        assertEquals("@angular/core", file.entries[1].product)
    }

    @Test
    fun `package-lock v1 reads the nested dependencies`() {
        val json = """{"lockfileVersion":1,"dependencies":{"a":{"version":"1.0.0","dependencies":{"b":{"version":"2.0.0"}}}}}"""
        assertEquals(listOf("pkg:npm/a@1.0.0", "pkg:npm/b@2.0.0"), purls(LibraryListParser.parse("l.json", json)))
    }

    @Test
    fun `maven dependency list and tree skip the test and provided scopes and keep the classifier out`() {
        val text = """
            [INFO] The following files have been resolved:   (not a dependency line)

                com.fasterxml.jackson.core:jackson-databind:jar:2.17.2:compile
                io.netty:netty-transport-native-epoll:jar:linux-x86_64:4.1.100.Final:runtime -- module netty (auto)
                org.junit.jupiter:junit-jupiter:jar:5.10.0:test
                jakarta.servlet:jakarta.servlet-api:jar:6.0.0:provided
            +- org.slf4j:slf4j-api:jar:2.0.9:compile
            |  \- org.x:y:jar:1.0:runtime
            [INFO] +- org.apache.commons:commons-lang3:jar:3.14.0:compile
            [INFO] |  \- org.x:y:jar:1.0:runtime
        """.trimIndent()
        val file = LibraryListParser.parse("deps-maven.txt", text)
        assertEquals(LibraryFormat.MAVEN, file.format)
        assertEquals(
            listOf(
                "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.2",
                "pkg:maven/io.netty/netty-transport-native-epoll@4.1.100.Final",
                "pkg:maven/org.slf4j/slf4j-api@2.0.9",
                "pkg:maven/org.x/y@1.0",
                "pkg:maven/org.apache.commons/commons-lang3@3.14.0",
            ),
            purls(file),
            "the console output of dependency:tree keeps its [INFO] prefix",
        )
    }

    @Test
    fun `gradle output uses the resolved version and skips constraints, unresolved and project dependencies`() {
        val text = """
            runtimeClasspath - Runtime classpath of source set 'main'.
            +--- org.springframework.boot:spring-boot-starter-web:3.5.7
            |    +--- org.springframework.boot:spring-boot-starter:3.5.7
            |    |    \--- org.slf4j:slf4j-api:2.0.9 -> 2.0.16
            |    +--- com.fasterxml.jackson.core:jackson-databind -> 2.19.0 (*)
            |    +--- org.apache.tomcat.embed:tomcat-embed-core:{strictly 10.1.30} -> 10.1.30
            |    +--- org.x:constraint:1.0 (c)
            |    +--- org.x:missing:1.0 (n)
            |    +--- org.x:broken:1.0 FAILED
            |    \--- project :common
            \--- org.y:range:[1.0,2.0)
        """.trimIndent()
        val file = LibraryListParser.parse("deps-gradle.txt", text)
        assertEquals(LibraryFormat.GRADLE, file.format)
        assertEquals(
            listOf(
                "pkg:maven/org.springframework.boot/spring-boot-starter-web@3.5.7",
                "pkg:maven/org.springframework.boot/spring-boot-starter@3.5.7",
                "pkg:maven/org.slf4j/slf4j-api@2.0.16",
                "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.19.0",
                "pkg:maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.30",
            ),
            purls(file),
        )
    }

    @Test
    fun `jar names from unzip, ls and jar tf are read without a groupId`() {
        val text = """
            Archive:  app.jar
              Length      Date    Time    Name
            ---------  ---------- -----   ----
             2093125  2026-10-01 10:00   BOOT-INF/lib/jackson-databind-2.17.2.jar
              123456  2026-10-01 10:00   BOOT-INF/lib/netty-transport-native-epoll-4.1.100.Final-linux-x86_64.jar
                 999  2026-10-01 10:00   BOOT-INF/lib/guava-33.0.0-jre.jar
                 999  2026-10-01 10:00   BOOT-INF/lib/jakarta.annotation-api-2.1.1.jar
                 999  2026-10-01 10:00   BOOT-INF/lib/lib-1.0-sources.jar
                 999  2026-10-01 10:00   BOOT-INF/lib/own.jar
            testing: BOOT-INF/lib/commons-lang3-3.14.0.jar   OK
        """.trimIndent()
        val file = LibraryListParser.parse("jars.txt", text)
        assertEquals(LibraryFormat.JAR, file.format)
        assertEquals(
            listOf(
                "pkg:maven/jackson-databind@2.17.2",
                "pkg:maven/netty-transport-native-epoll@4.1.100.Final",
                "pkg:maven/guava@33.0.0-jre",
                "pkg:maven/jakarta.annotation-api@2.1.1",
                "pkg:maven/lib@1.0",
                "pkg:maven/commons-lang3@3.14.0",
            ),
            purls(file),
        )
        assertNull(file.entries.first().group)
    }

    @Test
    fun `a file without a known format is reported, the limits are enforced, a purl reads back`() {
        assertNull(LibraryListParser.parse("notes.txt", "hello\nworld").format)
        assertNull(LibraryListParser.parse("broken.json", "{not json").format)
        assertThrows(UserException::class.java) { LibraryListParser.parse("big.txt", "x".repeat(LibraryListParser.MAX_FILE_BYTES + 1)) }
        assertThrows(UserException::class.java) {
            LibraryListParser.parseAll((1..LibraryListParser.MAX_FILES + 1).map { "f$it" to "" })
        }
        val entry = LibraryListParser.fromPurl("pkg:npm/%40angular/core@18.2.1")!!
        assertEquals(LibraryEntry("npm", "@angular", "core", "18.2.1"), entry)
        assertEquals(LibraryEntry("maven", "org.x", "y", "1.0"), LibraryListParser.fromPurl("pkg:maven/org.x/y@1.0"))
        assertEquals(LibraryEntry("pypi", null, "x", "1"), LibraryListParser.fromPurl("pkg:pypi/x@1"))
        assertEquals(
            LibraryEntry("golang", "github.com/gin-gonic", "gin", "v1.9.1"),
            LibraryListParser.fromPurl("pkg:golang/github.com/gin-gonic/gin@v1.9.1"),
        )
        assertEquals("github.com/gin-gonic/gin", LibraryListParser.fromPurl("pkg:golang/github.com/gin-gonic/gin@v1.9.1")!!.product)
        assertNull(LibraryListParser.fromPurl("pkg:pypi/x"), "without a version")
        assertNull(LibraryListParser.fromPurl("cpe:2.3:a:x:y:1"))
    }

    @Test
    fun `a CycloneDX SBOM reads the components by purl, nested ones too, without those not needed at runtime`() {
        val json = """
            {"bomFormat":"CycloneDX","specVersion":"1.5",
             "metadata":{"component":{"type":"application","name":"app","purl":"pkg:maven/org.app/app@1.0"}},
             "components":[
              {"type":"library","group":"com.fasterxml.jackson.core","name":"jackson-databind","version":"2.15.2",
               "purl":"pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.15.2?type=jar",
               "components":[{"type":"library","name":"inner","purl":"pkg:maven/org.x/inner@1.1"}]},
              {"type":"library","name":"core","group":"@angular","purl":"pkg:npm/%40angular/core@18.2.1"},
              {"type":"library","name":"requests","purl":"pkg:pypi/requests@2.31.0"},
              {"type":"library","name":"openssl","purl":"pkg:deb/debian/openssl@3.0.11-1~deb12u2?arch=amd64&distro=debian-12"},
              {"type":"library","name":"mocha","purl":"pkg:npm/mocha@10.0.0","scope":"optional"},
              {"type":"library","name":"junit","purl":"pkg:maven/junit/junit@4.13.2","scope":"excluded"},
              {"type":"file","name":"README"}
             ]}
        """.trimIndent()
        val file = LibraryListParser.parse("bom.json", json)
        assertEquals(LibraryFormat.CYCLONEDX, file.format)
        assertEquals(
            listOf(
                "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.15.2", "pkg:maven/org.x/inner@1.1",
                "pkg:npm/%40angular/core@18.2.1", "pkg:pypi/requests@2.31.0",
                "pkg:deb/debian/openssl@3.0.11-1~deb12u2?distro=debian-12",
            ),
            purls(file),
            "the app itself, files and the qualifiers but the distribution are left out",
        )
        assertEquals("openssl", file.entries.last().product)
        assertEquals("debian-12", file.entries.last().distro)
    }

    @Test
    fun `an SPDX document reads the packages by their purl reference`() {
        val json = """
            {"spdxVersion":"SPDX-2.3","SPDXID":"SPDXRef-DOCUMENT","packages":[
              {"SPDXID":"SPDXRef-root","name":"app"},
              {"SPDXID":"SPDXRef-1","name":"lodash","versionInfo":"4.17.21","externalRefs":[
                {"referenceCategory":"SECURITY","referenceType":"cpe23Type","referenceLocator":"cpe:2.3:a:lodash:lodash:4.17.21:*:*:*:*:*:*:*"},
                {"referenceCategory":"PACKAGE-MANAGER","referenceType":"purl","referenceLocator":"pkg:npm/lodash@4.17.21"}]},
              {"SPDXID":"SPDXRef-2","name":"zlib","externalRefs":[
                {"referenceCategory":"PACKAGE_MANAGER","referenceType":"purl","referenceLocator":"pkg:apk/alpine/zlib@1.3.1-r0?arch=x86_64&distro=3.19.1"}]}
            ]}
        """.trimIndent()
        val file = LibraryListParser.parse("sbom.spdx.json", json)
        assertEquals(LibraryFormat.SPDX, file.format)
        assertEquals(listOf("pkg:npm/lodash@4.17.21", "pkg:apk/alpine/zlib@1.3.1-r0?distro=3.19.1"), purls(file))
        assertNull(LibraryListParser.parse("empty.json", """{"bomFormat":"CycloneDX","components":[]}""").format)
    }
}
