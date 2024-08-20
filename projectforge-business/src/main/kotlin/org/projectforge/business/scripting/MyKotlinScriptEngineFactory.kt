/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.jetbrains.kotlin.script.jsr223.KotlinStandardJsr223ScriptTemplate
import org.projectforge.framework.configuration.ConfigXml
import org.slf4j.LoggerFactory
import java.io.File
import java.util.jar.JarFile
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

private val whiteListJars = listOf(
  "de.micromata.mgc.jpa.emgr",
  "de.micromata.mgc.jpa.tabattr",
  "merlin-core",
  "org.projectforge",
  "projectforge",
  "kotlin-stdlib",
  "kotlin-script-runtime",
  "kotlin-script-util",
  "poi"
)

/**
 * Kotlin scripts don't run out of the box with spring boot. This workarround is needed.
 */
// https://stackoverflow.com/questions/44781462/kotlin-jsr-223-scriptenginefactory-within-the-fat-jar-cannot-find-kotlin-compi
class MyKotlinScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {
  override fun getScriptEngine(): ScriptEngine {
    if (jarFile == null) {
      // We're not running in a jar file:
      val engineManager = ScriptEngineManager()
      return engineManager.getEngineByExtension("kts")
    }
    return KotlinJsr223JvmLocalScriptEngine(
      //Disposer.newDisposable(),
      this,
      classPath,
      KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
      { ctx, types ->
        ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray())
      },
      arrayOf(Bindings::class)
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(MyKotlinScriptEngineFactory::class.java)
    private var jarFile: File? = null
    private val libDir = File(ConfigXml.getInstance().tempDirectory, "scriptClassPath")
    private val classPath = mutableListOf<File>()

    init {
      val uriString = MyKotlinScriptEngineFactory::class.java.protectionDomain.codeSource.location.toString()
      if (uriString.startsWith("file:")) {
        // We're not running in a jar file.
      } else {
        if (libDir.exists()) {
          log.info("Deleting existing tmp dir '$libDir'.")
          libDir.deleteRecursively()
        }
        val filename = uriString.substring(0, uriString.indexOf('!')).removePrefix("jar:file:")
        jarFile = File(filename)
        log.info("Detecting jar file: ${jarFile!!.absolutePath}")
        log.info("Creating new tmp dir '$libDir'.")
        libDir.mkdirs()
        JarFile(jarFile).use { zip ->
          zip.entries().asSequence().forEach { entry ->
            zip.getInputStream(entry).use { input ->
              if (entry.isDirectory) {
                // Do nothing (only jars required)
                // file.mkdirs()
              } else {
                val origFile = File(entry.name)
                if (origFile.extension == "jar" && whiteListJars.any { origFile.name.startsWith(it) }) {
                  val file = File(libDir, origFile.name)
                  classPath.add(file)
                  file.outputStream().use { output ->
                    input.copyTo(output)
                  }
                }
              }
            }
          }
        }
        //classPath.add(jarFile)
        log.info("Setting script classPath: ${classPath.joinToString(";") { it.absolutePath }}")
      }
    }
  }
}
