/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.scripting

import mu.KotlinLogging
import org.apache.commons.io.IOUtils

private val log = KotlinLogging.logger {}

object ExampleScripts {
  class ExampleScript(val filename: String, val title: String)

  fun loadScript(idx: Int): String {
    val example = exampleFiles[idx]
    val resourcePath = "example-scripts/${example.filename}"
    val resourceStream = ExampleScripts::class.java.classLoader.getResourceAsStream(resourcePath)
    if (resourceStream == null) {
      log.error("Internal error: Can't read initial config data from class path: $resourcePath")
      return "// ${example.filename} not found."
    }
    return IOUtils.toString(resourceStream, "UTF-8")
  }

  val exampleFiles = listOf(
    ExampleScript("helloWorld.kts", "Simple hello world."),
    ExampleScript("simpleExcelExport.kts", "Simple Excel® export."),
    ExampleScript("advancedExcelExport.kts", "Advanced Excel® export."),
  )
}
