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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Generates the message catalogs of the mobile apps (`<app>/assets/i18n/generated.<locale>.json`) from the
 * I18nResources bundles, as [GenerateNextI18nMessagesMain] does for projectforge-next. One [Target] per app:
 * projectforge-app (React Native) and, until it is removed, projectforge-flutter.
 *
 * Exported are the keys the sources name literally (`t('businessCard.title')`, see [KEY_REGEX]) plus the keys
 * starting with one of the target's prefixes. The catalogs are flat (dotted key to text), the placeholders stay in
 * the MessageFormat syntax (`{0}`), only the escaped apostrophe (`''`) is resolved: the apps substitute the
 * placeholders themselves. App only texts are hand-written in `app.<locale>.json`.
 *
 * Run by [DevelopmentMainForRelease] (`bin/pfDev.sh gen`); [GenerateAppI18nMessagesTest] fails if the files are out
 * of date.
 */
object GenerateAppI18nMessagesMain {
  /**
   * @param sourceDir The sources scanned, below [appDir].
   * @param prefixes Keys the app picks by value, which no scan finds.
   */
  class Target(
    val appDir: String,
    val sourceDir: String,
    val extensions: Set<String>,
    val prefixes: List<String>,
  ) {
    val outDir = "$appDir/assets/i18n"
  }

  val TARGETS = listOf(
    Target(
      appDir = "projectforge-app",
      sourceDir = "src",
      extensions = setOf("ts", "tsx"),
      // The field labels of the business cards.
      prefixes = listOf("businessCard."),
    ),
    Target(
      appDir = "projectforge-flutter",
      sourceDir = "lib",
      extensions = setOf("dart"),
      // The field labels of the business card editor (lib/features/business_cards/business_card_fields.dart).
      prefixes = listOf("businessCard."),
    ),
  )

  private const val MARKER_KEY = "_generated"
  private const val MARKER_VALUE =
    "Generated from I18nResources by GenerateAppI18nMessagesMain (DevelopmentMainForRelease) " +
        "— do not edit; hand-written texts belong in app.<locale>.json."
  private val LOCALES = mapOf("en" to "", "de" to "_de")

  /**
   * `t('key'` or `t("key"`, also as `context.t(`/`translations.t(`/`i18n.t(`. Plain keys (`cancel`) as well: a match
   * is only exported if the bundle knows it. Keys in template literals aren't found: always write them out.
   */
  private val KEY_REGEX = Regex("""\bt\(\s*['"]([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)*)['"]""")

  @JvmStatic
  fun main(args: Array<String>) {
    val rootDir = GenerateNextI18nMessagesMain.resolveRootDir()
    TARGETS.forEach { target ->
      if (!File(rootDir, target.appDir).isDirectory) {
        println("No ${target.appDir}, nothing to generate.")
        return@forEach
      }
      generate(rootDir, target).forEach { (locale, json) ->
        val file = outFile(rootDir, target, locale)
        file.parentFile.mkdirs()
        if (file.exists() && file.readText(StandardCharsets.UTF_8) == json) {
          println("Unchanged ${file.path}")
        } else {
          file.writeText(json, StandardCharsets.UTF_8)
          println("Wrote ${file.path}")
        }
      }
    }
  }

  /**
   * @return locale to the json content of its catalog.
   */
  internal fun generate(rootDir: File, target: Target): Map<String, String> {
    val defaults = GenerateNextI18nMessagesMain.readProperties(rootDir, "")
    val bundleKeys = defaults.stringPropertyNames()
    val requested = scan(rootDir, target)
    val exported = (requested.filter { it in bundleKeys } +
        bundleKeys.filter { key -> target.prefixes.any { key.startsWith(it) } })
      .distinct()
      .sorted()
    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    return LOCALES.entries.associate { (locale, suffix) ->
      val properties = if (suffix.isEmpty()) defaults else GenerateNextI18nMessagesMain.readProperties(rootDir, suffix)
      val catalog = LinkedHashMap<String, String>()
      catalog[MARKER_KEY] = MARKER_VALUE
      exported.forEach { key ->
        (properties.getProperty(key) ?: defaults.getProperty(key))?.let { catalog[key] = it.replace("''", "'") }
      }
      locale to mapper.writeValueAsString(catalog).replace("\r\n", "\n") + "\n"
    }
  }

  /**
   * The keys named literally in the sources of the app (tests excluded: they may name keys the app doesn't use).
   */
  internal fun scan(rootDir: File, target: Target): Set<String> {
    val keys = sortedSetOf<String>()
    File(rootDir, "${target.appDir}/${target.sourceDir}").walkTopDown()
      .onEnter { it.name != "__tests__" && it.name != "node_modules" }
      .filter { it.isFile && it.extension in target.extensions }
      .forEach { file ->
        KEY_REGEX.findAll(file.readText(StandardCharsets.UTF_8)).forEach { keys.add(it.groupValues[1]) }
      }
    return keys
  }

  internal fun outFile(rootDir: File, target: Target, locale: String) =
    File(rootDir, "${target.outDir}/generated.$locale.json")
}
