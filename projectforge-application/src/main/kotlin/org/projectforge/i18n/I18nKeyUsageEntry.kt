/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.i18n

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.framework.json.JsonUtils
import java.io.File

private val log = KotlinLogging.logger {}

internal class I18nKeyUsageEntry(val i18nKey: String) {
  var bundleName: String? = null
  var translation: String? = null
  var translationDE: String? = null
  var usedInClasses = mutableListOf<Class<*>>()
  var usedInFiles = mutableListOf<String>()

  val classes: String
    @JsonIgnore
    get() = usedInClasses.sortedBy { it.name }.joinToString { it.simpleName }

  val files: String
    @JsonIgnore
    get() = usedInFiles.sorted().joinToString()

  /**
   * If file represents a Java or Kotlin class, the class will be get byName and the class will be added instead of
   * the file.
   */
  fun addUsage(file: File) {
    val clazz = getClassByFile(file)
    if (clazz != null) {
      addUsage(clazz)
    } else {
      val path = file.absolutePath.replace(basePath, ".")
      if (!usedInFiles.contains(path)) {
        usedInFiles.add(path)
        usedInFiles.sort()
      }
    }
  }

  /**
   * Adds the class to the list of used classes.
   * The list will be sorted by class name.
   * @param clazz The class to add.
   * @return true if the class was added, false if the class was already in the list.
   * @see usedInClasses
   */
  fun addUsage(clazz: Class<*>) {
    if (!usedInClasses.contains(clazz)) {
      usedInClasses.add(clazz)
      usedInClasses.sortBy { it.name }
    }
  }

  override fun toString(): String {
    return JsonUtils.toJson(this)
  }

  companion object {
    private val classCacheMap = mutableMapOf<File, Class<*>>()
    private val basePath = I18nKeysSourceAnalyzer.basePath.toFile().absolutePath

    fun read(str: String): I18nKeyUsageEntry? {
      return JsonUtils.fromJson(str, I18nKeyUsageEntry::class.java)
    }

    internal fun getClassByFile(file: File, showError: Boolean = true): Class<*>? {
      classCacheMap[file]?.let { return it }
      val className = if (file.extension == "java") {
        file.absolutePath.substringAfter("/src/main/java/").removeSuffix(".java").replace('/', '.')
      } else if (file.extension == "kt") {
        file.absolutePath.substringAfter("/src/main/kotlin/").removeSuffix(".kt").replace('/', '.')
      } else {
        return null
      }
      if (
        className == "org.projectforge.framework.i18n.I18n" ||
        className == "org.projectforge.common.logging.LogConstants" ||
        className == "org.projectforge.rest.json.Deserializers"
      ) {
        // No such classes.
        return null
      }
      return try {
        val clazz = Class.forName(className)
        classCacheMap[file] = clazz
        clazz
      } catch (ex: Throwable) {
        if (showError) {
          val msg = "*** Class '$className' not found (file=${file.absolutePath}): ${ex.message}"
          log.warn { msg }
          println(msg)
        }
        null
      }
    }
  }
}
