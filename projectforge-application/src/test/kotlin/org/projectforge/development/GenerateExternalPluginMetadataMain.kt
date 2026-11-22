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

package org.projectforge.development

import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

/**
 * Generates the field metadata of the entities of a plugin developed outside of this repository
 * (`pf.extraPlugins`, see dev-docs/external-plugins.md) for its own frontend, with the rules of
 * [GenerateNextFieldMetadataMain]. The files go into the directory of the system property `pf.metadata.out`, not into
 * `projectforge-next`: this repository knows nothing of the plugin.
 *
 * Files of the directory the plugin has no entity for any more (`*.generated.ts`) are deleted. With the system property
 * `pf.metadata.check=true` nothing is written, the run fails if a file is missing, differs or is left over.
 */
object GenerateExternalPluginMetadataMain {
  private val ENCODING = StandardCharsets.UTF_8

  @JvmStatic
  fun main(args: Array<String>) {
    val outDir = File(requireNotNull(System.getProperty("pf.metadata.out")) { "Property pf.metadata.out is missing." })
    val check = System.getProperty("pf.metadata.check") == "true"
    val entities = GenerateNextFieldMetadataMain.externalEntityClasses()
    require(entities.isNotEmpty()) {
      "No entities of an external plugin found: is the plugin given as pf.extraPlugins?"
    }
    val expected = GenerateNextFieldMetadataMain.generate(entities, "@/lib/metadata/types")
    val existing = outDir.listFiles { file -> file.name.endsWith(".generated.ts") }.orEmpty().associateBy { it.name }
    // Leftovers are deleted below: never do that in a directory holding the metadata of this repository.
    val coreFiles = GenerateNextFieldMetadataMain.generate(GenerateNextFieldMetadataMain.entityClasses()).keys
    require(existing.keys.none { it in coreFiles }) {
      "${outDir.path} holds the field metadata of this repository, not of a plugin: check pf.metadata.out."
    }
    val problems = mutableListOf<String>()
    expected.forEach { (name, content) ->
      val file = File(outDir, name)
      if (!file.exists() || file.readText(ENCODING) != content) {
        problems.add(name)
        if (!check) {
          outDir.mkdirs()
          file.writeText(content, ENCODING)
          println("Wrote ${file.path}")
        }
      }
    }
    (existing.keys - expected.keys).sorted().forEach { name ->
      problems.add("$name (left over)")
      if (!check) {
        existing.getValue(name).delete()
        println("Deleted ${outDir.resolve(name).path}")
      }
    }
    if (check && problems.isNotEmpty()) {
      System.err.println("Out of date: ${problems.joinToString()}. Run the generator (bin/pf.sh metadata).")
      exitProcess(1)
    }
    if (problems.isEmpty()) {
      println("Field metadata of ${expected.size} entities up to date in ${outDir.path}")
    }
  }
}
