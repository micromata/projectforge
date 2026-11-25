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

package org.projectforge.start

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Properties

class PluginBuildInfoTest {
  @Test
  fun `version, build date and git id of a plugin jar`() {
    val props = Properties().apply {
      setProperty("plugin.version", "9.2.16-SNAPSHOT")
      setProperty("git.branch", "main")
      setProperty("git.commit.id.abbrev", "00d896d")
      setProperty("git.commit.time", "2026-11-24T23:39:24+01:00")
      setProperty("git.dirty", "false")
    }
    assertEquals(
      "Plugin cveradar 9.2.16-SNAPSHOT: build=2026-11-24T23:39:24+01:00, git=main@00d896d",
      PluginBuildInfo.format("cveradar", "9.2.16-SNAPSHOT", props),
    )
    props.setProperty("git.dirty", "true")
    assertEquals(
      "Plugin cveradar 9.2.16-SNAPSHOT: build=2026-11-24T23:39:24+01:00, git=main@00d896d*",
      PluginBuildInfo.format("cveradar", "9.2.16-SNAPSHOT", props),
    )
  }

  @Test
  fun `an older plugin jar without build info shows only its version`() {
    assertEquals("Plugin cveradar 9.2.14: no build info", PluginBuildInfo.format("cveradar", "9.2.14", Properties()))
  }
}
