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
import java.util.Properties

/**
 * Generates the next-intl message catalogs of projectforge-next from the I18nResources bundle,
 * so translations aren't maintained twice.
 *
 * The bundle is the single source of truth: `I18nResources.properties` holds the English texts
 * (default), `I18nResources_de.properties` the German ones. Run [SortAndCheckI18nPropertiesMain]
 * first — that is what [DevelopmentMainForRelease] does.
 *
 * Keys are selected by prefix ([PREFIXES]) rather than listed individually, which keeps this in
 * sync as entities gain fields. That exports a few keys the frontend doesn't use; the alternative
 * is a list that silently goes stale. Exporting the whole bundle is not an option though: the
 * frontend ships its catalog to the browser.
 *
 * Output goes to `messages/generated.<locale>.json`, kept separate from the hand-written catalogs
 * so frontend-only texts (e.g. `login.username`, which has no backend counterpart) survive.
 * next-intl reads dotted keys as nested namespaces, so `book.signature` is written as
 * `{"book":{"signature":…}}` and read via `useTranslations("book")` + `t("signature")`.
 */
object GenerateNextI18nMessagesMain {
  private const val BUNDLE = "projectforge-business/src/main/resources/I18nResources"
  private const val OUT_DIR = "projectforge-next/messages"
  private val ENCODING = StandardCharsets.UTF_8

  /** Generated locale to the properties suffix it reads from (default bundle = English). */
  private val LOCALES = mapOf("en" to "", "de" to "_de")

  /**
   * Key prefixes to export. Everything matching lands in the catalog under its dotted path.
   * Prefixes without a dot match a single top-level key.
   */
  private val PREFIXES = listOf(
    "book.",
    // Without the dot, so the bare "columns" key exports too (as "columns._", see JsonNode.put).
    "columns",
    "filter.",
    "created",
    "modified",
    // Wait indicator, e.g. while a column filter builds its value list.
    "loading",
    // Generic button labels. "delete" also matches "deleted" — harmless, and the alternative is
    // spelling out every key that happens to share a prefix.
    "apply",
    "delete",
    "save",
    // Top navigation. Not the whole "menu." tree: the entry titles come translated from /rs/menu,
    // only the chrome around them needs its own texts.
    "menu.main.title",
    "menu.favorites.more",
    "menu.myAccount",
    // Category above a list page's heading, e.g. "Common / Books" — the entry's menu parent.
    "menu.common",
    // Authentication (login, 2FA, password reset). Only the keys the frontend can show —
    // user.My2FA.setup.* holds long markdown blobs of the setup page, which next doesn't have.
    "cancel",
    "login", // login (the button label), login.title, login.error.* …
    "password", // password, passwordRepeat and password.forgotten/reset.*
    "username",
    "user.My2FA.expired",
    "user.My2FA.required",
    "user.My2FACode.",
    "user.changePassword.",
    "webauthn.error.",
    "webauthn.registration.button.",
  )

  @JvmStatic
  fun main(args: Array<String>) {
    val defaults = readProperties("")
    val exported = defaults.stringPropertyNames()
      .filter { key -> PREFIXES.any { key.startsWith(it) } }
      .sorted()

    require(exported.isNotEmpty()) { "No keys matched $PREFIXES — check the prefixes." }

    LOCALES.forEach { (locale, suffix) ->
      val properties = if (suffix.isEmpty()) defaults else readProperties(suffix)
      val root = JsonNode()
      exported.forEach { key ->
        // Fall back to the default (English) bundle for untranslated keys.
        val value = properties.getProperty(key) ?: defaults.getProperty(key)
        if (value != null) {
          root.put(key, toIcu(value))
        }
      }
      val outFile = File("$OUT_DIR/generated.$locale.json")
      outFile.parentFile.mkdirs()
      outFile.writeText(root.toJson(), ENCODING)
      println("Wrote ${outFile.path} (${exported.size} keys)")
    }
  }

  private fun readProperties(suffix: String): Properties {
    val file = File("$BUNDLE$suffix.properties")
    require(file.exists()) { "Properties file not found: ${file.path}" }
    return Properties().apply {
      file.inputStream().use { load(it.reader(ENCODING)) }
    }
  }

  /**
   * Java's MessageFormat escapes a literal apostrophe as `''` and numbers its placeholders, while
   * the ICU syntax next-intl uses wants a single apostrophe and named arguments.
   */
  internal fun toIcu(value: String): String {
    return value
      .replace("''", "'")
      .replace(Regex("\\{(\\d+)}")) { "{arg${it.groupValues[1]}}" }
  }

  /**
   * Minimal JSON object writer, enough to emit a nested catalog. Avoids adding a JSON dependency
   * to this dev-only main.
   */
  internal class JsonNode {
    private val children = LinkedHashMap<String, Any>() // String or JsonNode

    /**
     * Adds a dotted key as a nested path. A key that would nest below an existing leaf (e.g.
     * `book.title` and `book.title.add`) keeps the leaf under the reserved "_" name, because JSON
     * cannot hold both a string and an object at the same place.
     */
    fun put(dottedKey: String, value: String) {
      val parts = dottedKey.split('.')
      var node = this
      parts.dropLast(1).forEach { part ->
        val existing = node.children[part]
        node = when (existing) {
          is JsonNode -> existing
          is String -> JsonNode().also {
            it.children["_"] = existing
            node.children[part] = it
          }
          else -> JsonNode().also { node.children[part] = it }
        }
      }
      val last = parts.last()
      when (val existing = node.children[last]) {
        // A nested object already claimed this name; store the leaf beside it.
        is JsonNode -> existing.children["_"] = value
        else -> node.children[last] = value
      }
    }

    fun toJson(): String = StringBuilder().also { append(it, 0) }.append('\n').toString()

    private fun append(sb: StringBuilder, indent: Int) {
      val pad = "  ".repeat(indent + 1)
      sb.append("{\n")
      children.entries.forEachIndexed { index, (key, value) ->
        sb.append(pad).append(quote(key)).append(": ")
        when (value) {
          is JsonNode -> value.append(sb, indent + 1)
          else -> sb.append(quote(value.toString()))
        }
        if (index < children.size - 1) sb.append(',')
        sb.append('\n')
      }
      sb.append("  ".repeat(indent)).append('}')
    }

    private fun quote(value: String): String {
      val sb = StringBuilder("\"")
      value.forEach { c ->
        when (c) {
          '"' -> sb.append("\\\"")
          '\\' -> sb.append("\\\\")
          '\n' -> sb.append("\\n")
          '\r' -> sb.append("\\r")
          '\t' -> sb.append("\\t")
          else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
      }
      return sb.append('"').toString()
    }
  }
}
