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
 * Generates the message catalogs of the mobile app (`projectforge-flutter/assets/i18n/generated.<locale>.json`)
 * from the I18nResources bundles, as [GenerateNextI18nMessagesMain] does for projectforge-next.
 *
 * Exported are the keys the Dart sources name literally (`t('businessCard.title')`, see [KEY_REGEX]) plus the
 * keys starting with one of [PREFIXES]. The catalogs are flat (dotted key to text), the placeholders stay in the
 * MessageFormat syntax (`{0}`), only the escaped apostrophe (`''`) is resolved: the app substitutes the
 * placeholders itself (lib/core/i18n/translations.dart). App only texts are hand-written in `app.<locale>.json`.
 *
 * Run by [DevelopmentMainForRelease] (`bin/pfDev.sh gen`); [GenerateFlutterI18nMessagesTest] fails if the files are
 * out of date.
 */
object GenerateFlutterI18nMessagesMain {
  private const val APP_DIR = "projectforge-flutter"
  private const val OUT_DIR = "$APP_DIR/assets/i18n"
  private const val MARKER_KEY = "_generated"
  private const val MARKER_VALUE =
    "Generated from I18nResources by GenerateFlutterI18nMessagesMain (DevelopmentMainForRelease) " +
        "— do not edit; hand-written texts belong in app.<locale>.json."
  private val LOCALES = mapOf("en" to "", "de" to "_de")

  /**
   * `t('key'` or `t("key"`, also as `context.t(`/`translations.t(`. Plain keys (`cancel`) as well: a match is only
   * exported if the bundle knows it.
   */
  private val KEY_REGEX = Regex("""\bt\(\s*['"]([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)*)['"]""")

  /**
   * Keys the app picks by value, which no scan finds.
   */
  private val PREFIXES = listOf(
    // The field labels of the business card editor (lib/features/business_cards/business_card_fields.dart).
    "businessCard.",
  )

  @JvmStatic
  fun main(args: Array<String>) {
    val rootDir = GenerateNextI18nMessagesMain.resolveRootDir()
    if (!File(rootDir, APP_DIR).isDirectory) {
      println("No $APP_DIR, nothing to generate.")
      return
    }
    generate(rootDir).forEach { (locale, json) ->
      val file = outFile(rootDir, locale)
      file.parentFile.mkdirs()
      if (file.exists() && file.readText(StandardCharsets.UTF_8) == json) {
        println("Unchanged ${file.path}")
      } else {
        file.writeText(json, StandardCharsets.UTF_8)
        println("Wrote ${file.path}")
      }
    }
  }

  /**
   * @return locale to the json content of its catalog.
   */
  internal fun generate(rootDir: File): Map<String, String> {
    val defaults = GenerateNextI18nMessagesMain.readProperties(rootDir, "")
    val bundleKeys = defaults.stringPropertyNames()
    val requested = scan(rootDir)
    val exported = (requested.filter { it in bundleKeys } +
        bundleKeys.filter { key -> PREFIXES.any { key.startsWith(it) } })
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
   * The keys named literally in the Dart sources of the app.
   */
  internal fun scan(rootDir: File): Set<String> {
    val keys = sortedSetOf<String>()
    File(rootDir, "$APP_DIR/lib").walkTopDown().filter { it.isFile && it.extension == "dart" }.forEach { file ->
      KEY_REGEX.findAll(file.readText(StandardCharsets.UTF_8)).forEach { keys.add(it.groupValues[1]) }
    }
    return keys
  }

  internal fun outFile(rootDir: File, locale: String) = File(rootDir, "$OUT_DIR/generated.$locale.json")
}
