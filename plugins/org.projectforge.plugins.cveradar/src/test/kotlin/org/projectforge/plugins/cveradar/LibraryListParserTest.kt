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
    fun `a CycloneDX SBOM of an image tells where a library was found, the places of one found twice together`() {
        val json = """
            {"bomFormat":"CycloneDX","specVersion":"1.6","components":[
              {"type":"library","name":"stdlib","purl":"pkg:golang/stdlib@1.18.2",
               "properties":[{"name":"syft:package:language","value":"go"},{"name":"syft:location:0:path","value":"/usr/local/bin/gosu"}]},
              {"type":"library","name":"stdlib","purl":"pkg:golang/stdlib@1.18.2",
               "properties":[{"name":"syft:location:0:path","value":"/usr/bin/other"}]},
              {"type":"library","name":"curl","purl":"pkg:deb/debian/curl@8.14.1-2?distro=debian-13",
               "evidence":{"occurrences":[{"location":"/var/lib/dpkg/status"}]}},
              {"type":"library","name":"left-pad","purl":"pkg:npm/left-pad@1.3.0"}
            ]}
        """.trimIndent()
        val entries = LibraryListParser.parse("image.cdx.json", json).entries
        assertEquals(3, entries.size, "the stdlib once")
        assertEquals("/usr/local/bin/gosu, /usr/bin/other", entries[0].location)
        assertEquals("/var/lib/dpkg/status", entries[1].location, "the evidence of CycloneDX 1.5")
        assertNull(entries[2].location)
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

    @Test
    fun `further lockfiles are read, only registry crates, no packages for development`() {
        val cargo = LibraryListParser.parse(
            "Cargo.lock",
            "[[package]]\nname = \"app\"\nversion = \"0.1.0\"\n\n[[package]]\nname = \"serde\"\nversion = \"1.0.190\"\n" +
                "source = \"registry+https://github.com/rust-lang/crates.io-index\"\n",
        )
        assertEquals(LibraryFormat.CARGO, cargo.format)
        assertEquals(listOf("pkg:cargo/serde@1.0.190"), purls(cargo))

        val composer = LibraryListParser.parse(
            "composer.lock",
            """{"packages":[{"name":"Symfony/Console","version":"v6.3.4"}],"packages-dev":[{"name":"phpunit/phpunit","version":"10.0.0"}]}""",
        )
        assertEquals(LibraryFormat.COMPOSER, composer.format)
        assertEquals(listOf("pkg:composer/symfony/console@6.3.4"), purls(composer))

        val yarn = LibraryListParser.parse(
            "yarn.lock",
            "# yarn lockfile v1\n\n\"@babel/core@^7.0.0\", \"@babel/core@^7.1.0\":\n  version \"7.22.5\"\n  resolved \"x\"\n\n" +
                "lodash@^4.17.0:\n  version \"4.17.21\"\n",
        )
        assertEquals(LibraryFormat.YARN, yarn.format)
        assertEquals(listOf("pkg:npm/%40babel/core@7.22.5", "pkg:npm/lodash@4.17.21"), purls(yarn))

        val berry = LibraryListParser.parse(
            "yarn.lock",
            "__metadata:\n  version: 6\n\n\"app@workspace:.\":\n  version: 0.0.0-use.local\n\n" +
                "\"lodash@npm:^4.17.0\":\n  version: 4.17.21\n  resolution: \"lodash@npm:4.17.21\"\n",
        )
        assertEquals(listOf("pkg:npm/lodash@4.17.21"), purls(berry))

        val go = LibraryListParser.parse(
            "go.mod",
            "module example.com/app\n\ngo 1.21\n\nrequire (\n\tgolang.org/x/net v0.17.0\n\tgithub.com/pkg/errors v0.9.1 // indirect\n)\n",
        )
        assertEquals(LibraryFormat.GO, go.format)
        assertEquals(listOf("pkg:golang/golang.org/x/net@v0.17.0", "pkg:golang/github.com/pkg/errors@v0.9.1"), purls(go))
        assertEquals("golang.org/x/net", go.entries.first().product)

        val pip = LibraryListParser.parse("requirements.txt", "# pinned\nDjango==4.2.7\nrequests[security]==2.31.0 ; python_version>'3'\nPyYAML_x==6.0\n-r other.txt\nflask>=2\nurllib3==2.0.7 \\\n    --hash=sha256:abc\n")
        assertEquals(LibraryFormat.PIP, pip.format)
        assertEquals(listOf("pkg:pypi/django@4.2.7", "pkg:pypi/requests@2.31.0", "pkg:pypi/pyyaml-x@6.0", "pkg:pypi/urllib3@2.0.7"), purls(pip))
    }

    @Test
    fun `pnpm-lock reads the packages reached from the runtime dependencies, without development dependencies and links`() {
        val v9 = LibraryListParser.parse(
            "pnpm-lock.yaml",
            """
            |lockfileVersion: '9.0'
            |
            |importers:
            |
            |  .:
            |    dependencies:
            |      '@scope/ui':
            |        specifier: ^2.0.0
            |        version: 2.1.0(react@18.3.1)
            |      shared:
            |        specifier: workspace:*
            |        version: link:packages/shared
            |      string-width-cjs:
            |        specifier: npm:string-width@^4.2.0
            |        version: string-width@4.2.3
            |    devDependencies:
            |      jest:
            |        specifier: 29.7.0
            |        version: 29.7.0
            |
            |packages:
            |
            |  '@scope/ui@2.1.0':
            |    resolution: {integrity: sha512-x}
            |
            |snapshots:
            |
            |  '@scope/ui@2.1.0(react@18.3.1)':
            |    dependencies:
            |      react: 18.3.1
            |    optionalDependencies:
            |      fsevents: 2.3.3
            |
            |  fsevents@2.3.3:
            |    optional: true
            |
            |  jest@29.7.0:
            |    dependencies:
            |      jest-cli: 29.7.0
            |
            |  jest-cli@29.7.0: {}
            |
            |  react@18.3.1: {}
            |
            |  string-width@4.2.3: {}
            """.trimMargin(),
        )
        assertEquals(LibraryFormat.PNPM, v9.format)
        assertEquals(
            listOf("pkg:npm/%40scope/ui@2.1.0", "pkg:npm/string-width@4.2.3", "pkg:npm/react@18.3.1", "pkg:npm/fsevents@2.3.3"),
            purls(v9),
        )

        val v6 = LibraryListParser.parse(
            "pnpm-lock.yaml",
            "lockfileVersion: '6.0'\n\npackages:\n\n  /@babel/core@7.22.5(supports-color@8.1.1):\n    resolution: {integrity: x}\n" +
                "    dev: false\n\n  /jest@29.7.0:\n    resolution: {integrity: x}\n    dev: true\n\n  /lodash@4.17.21:\n    dev: false\n",
        )
        assertEquals(LibraryFormat.PNPM, v6.format)
        assertEquals(listOf("pkg:npm/%40babel/core@7.22.5", "pkg:npm/lodash@4.17.21"), purls(v6))

        val v5 = LibraryListParser.parse(
            "pnpm-lock.yaml",
            "lockfileVersion: 5.4\n\npackages:\n\n  /@babel/core/7.22.5_supports-color@8.1.1:\n    dev: false\n\n  /jest/29.7.0:\n    dev: true\n",
        )
        assertEquals(listOf("pkg:npm/%40babel/core@7.22.5"), purls(v5))
    }

    @Test
    fun `package lists of Debian, Alpine and RPM systems are told apart`() {
        val dpkg = LibraryListParser.parse(
            "packages.txt",
            "ii  libssl3:amd64  3.0.11-1~deb12u2  amd64  Secure Sockets Layer toolkit\nii  openssl  3.0.11-1~deb12u2  amd64  tool\n",
        )
        assertEquals(LibraryFormat.DEB, dpkg.format)
        assertEquals(listOf("pkg:deb/libssl3@3.0.11-1~deb12u2", "pkg:deb/openssl@3.0.11-1~deb12u2"), purls(dpkg))
        val query = LibraryListParser.parse("p.txt", "bash 5.2.15-2+b2\ncurl 1:7.88.1-10\n")
        assertEquals(LibraryFormat.DEB, query.format)
        assertEquals(2, query.entries.size)

        val apk = LibraryListParser.parse("apk.txt", "musl-1.2.4-r2\nlibcrypto3-3.1.4-r0\nalpine-baselayout-data-3.4.3-r1\n")
        assertEquals(LibraryFormat.APK, apk.format)
        assertEquals(listOf("pkg:apk/musl@1.2.4-r2", "pkg:apk/libcrypto3@3.1.4-r0", "pkg:apk/alpine-baselayout-data@3.4.3-r1"), purls(apk))

        val rpm = LibraryListParser.parse("rpm.txt", "openssl-libs-3.0.7-24.el9.x86_64\nbash-5.1.8-6.el9.x86_64\ntzdata-2023c-1.el9.noarch\n")
        assertEquals(LibraryFormat.RPM, rpm.format)
        assertEquals(listOf("pkg:rpm/openssl-libs@3.0.7-24.el9", "pkg:rpm/bash@5.1.8-6.el9", "pkg:rpm/tzdata@2023c-1.el9"), purls(rpm))

        // The names of jar files in a listing are no packages.
        val unzip = LibraryListParser.parse("u.txt", "  1234  2024-01-01 12:00   BOOT-INF/lib/spring-core-6.1.2.jar\n")
        assertEquals(LibraryFormat.JAR, unzip.format)
    }

    @Test
    fun `the source package of a distribution package is kept from the purl, with its version if that differs`() {
        val ssl = LibraryListParser.fromPurl("pkg:deb/debian/libssl3@3.0.15-1~deb12u1?arch=arm64&upstream=openssl&distro=debian-12")!!
        assertEquals("openssl", ssl.source)
        assertEquals("debian-12", ssl.distro)
        assertEquals("pkg:deb/debian/libssl3@3.0.15-1~deb12u1?distro=debian-12&upstream=openssl", ssl.purl)
        assertEquals(ssl, LibraryListParser.fromPurl(ssl.purl), "round trip")

        val gnupg = LibraryListParser.fromPurl("pkg:deb/debian/gnupg@2.2.40-1.1?upstream=gnupg2%402.2.40-1.1&distro=debian-12")!!
        assertEquals("gnupg2@2.2.40-1.1", gnupg.source)
        assertEquals("pkg:deb/debian/gnupg@2.2.40-1.1?distro=debian-12&upstream=gnupg2%402.2.40-1.1", gnupg.purl)
        assertEquals(gnupg, LibraryListParser.fromPurl(gnupg.purl), "round trip")
    }

    @Test
    fun `a dpkg list with the lines of os-release and the source packages is read like an SBOM`() {
        val debian = LibraryListParser.parse(
            "os.txt",
            """
                PRETTY_NAME="Debian GNU/Linux 12 (bookworm)"
                NAME="Debian GNU/Linux"
                VERSION_ID="12"
                VERSION="12 (bookworm)"
                VERSION_CODENAME=bookworm
                ID=debian
                HOME_URL="https://www.debian.org/"
                libssl3 3.0.15-1~deb12u1 openssl 3.0.15-1~deb12u1
                gnupg 2.2.40-1.1 gnupg2 2.2.40-1.1
                bash 5.2.15-2+b2 bash 5.2.15-2
                openssl 3.0.15-1~deb12u1 openssl 3.0.15-1~deb12u1
            """.trimIndent(),
        )
        assertEquals(LibraryFormat.DEB, debian.format)
        assertEquals(
            listOf(
                "pkg:deb/debian/libssl3@3.0.15-1~deb12u1?distro=debian-12&upstream=openssl",
                "pkg:deb/debian/gnupg@2.2.40-1.1?distro=debian-12&upstream=gnupg2",
                "pkg:deb/debian/bash@5.2.15-2+b2?distro=debian-12&upstream=bash%405.2.15-2",
                "pkg:deb/debian/openssl@3.0.15-1~deb12u1?distro=debian-12",
            ),
            purls(debian),
        )
        assertEquals("libssl3", debian.entries.first().product)

        val ubuntu = LibraryListParser.parse("os.txt", "ID=ubuntu\nVERSION_ID=\"22.04\"\nID_LIKE=debian\ncurl 7.81.0-1ubuntu1.30\n")
        assertEquals(listOf("pkg:deb/ubuntu/curl@7.81.0-1ubuntu1.30?distro=ubuntu-22.04"), purls(ubuntu))

        val testing = LibraryListParser.parse("os.txt", "ID=debian\nVERSION_CODENAME=forky\ncurl 8.14.1-2\n")
        assertEquals(listOf("pkg:deb/debian/curl@8.14.1-2?distro=debian-forky"), purls(testing), "testing has no VERSION_ID")
    }
}
