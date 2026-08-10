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

import org.projectforge.framework.utils.SourcesUtils
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
 *
 * The generated files are never edited by hand — [GenerateNextI18nMessagesTest] fails if they
 * differ from what [generate] produces, which catches both a manual edit and a bundle change that
 * was committed without regenerating.
 */
object GenerateNextI18nMessagesMain {
  private const val BUNDLE = "projectforge-business/src/main/resources/I18nResources"
  private const val OUT_DIR = "projectforge-next/messages"
  private val ENCODING = StandardCharsets.UTF_8

  /**
   * Warning carried in the catalog itself, since JSON can't hold a comment. Must not contain a dot,
   * otherwise [JsonNode.put] would nest it.
   */
  internal const val MARKER_KEY = "_generated"
  private const val MARKER_VALUE =
    "Generated from I18nResources by GenerateNextI18nMessagesMain (DevelopmentMainForRelease) " +
        "— do not edit; hand-written texts belong in de.json/en.json."

  /** Generated locale to the properties suffix it reads from (default bundle = English). */
  private val LOCALES = mapOf("en" to "", "de" to "_de")

  /**
   * Key prefixes to export. Everything matching lands in the catalog under its dotted path.
   * Prefixes without a dot match a single top-level key.
   */
  private val PREFIXES = listOf(
    "book.",
    // Cost units (Kost1PagesRest, category cost1): the field labels of Kost1DO, the status of a cost
    // unit and the errors its DAO refuses a write with (fibu.kost.error.*). Deliberately not the whole
    // "fibu." tree — that is ~500 keys of invoices, orders and accounting, none of which next shows.
    // Without the dot on "fibu.kost1", so the bare key exports too: it titles the number column and
    // lands under "fibu.kost1._" (see JsonNode.put), its own subtree making it a namespace.
    "fibu.kost1",
    "fibu.kost.",
    // Without the trailing digit, so the menu parent of the cost pages exports too: the cost 1 list
    // sits under Finance > Cost (MenuItemDefId.COST), and that heading is what its page shows above
    // its title. Brings the sibling entries (kost2, kost2arten, kostSearch) along, which is four keys.
    "menu.fibu.kost",
    // Without the dot, so the bare "columns" key exports too (as "columns._", see JsonNode.put).
    "columns",
    "filter.",
    "created",
    "modified",
    // Label of the combined history filter's period ("Änderungszeitraum"). Not covered by
    // "modified" — the key is modificationTime.
    "modificationTime",
    // Quick-select periods of that filter (components/data-table/history-interval-presets.ts):
    // search.lastMinute(s), lastHour(s), lastDay(s). Deliberately not the whole "search." tree —
    // search.string.info and search.lucene.expression are long help blobs next never shows.
    "search.last",
    "search.today",
    "search.sinceYesterday",
    // Field labels shared by many entities, as their @PropertyInfo names them: BookDO.comment,
    // BookDO.status, BookDO.lendOutDate ("date"). Without the dot, so the bare "date" key exports
    // too — as "date._", since its own subtree (date.begin, date.end …) makes it a namespace.
    // "dateFormat" comes along for the ride, as "deleted" does with "delete".
    "comment",
    "status",
    "date",
    // Wait indicator, e.g. while a column filter builds its value list.
    "loading",
    // The popovers of a date and a time input (components/shared/date-input.tsx,
    // components/shared/time-input.tsx). Only these keys, not the whole "calendar." tree — that one
    // holds the holiday names and the calendar module's own texts, none of which next shows.
    "calendar.chooseDate",
    "calendar.chooseTime",
    "calendar.today",
    // Generic button labels. "delete" also matches "deleted" — harmless, and the alternative is
    // spelling out every key that happens to share a prefix.
    "apply",
    "delete",
    "markAsDeleted",
    "undelete",
    "save",
    "name",
    "rename",
    // Clears a select back to no value (SelectField in book-edit-fields).
    "reset",
    // Attachments of an entity: the list, its hints and the errors an upload is refused with
    // (see components/shared/attachments/). "edit" and "download" are the row actions,
    // "description" the second editable field of an attachment (Attachment.description).
    // Without the dot, so the bare "attachment" key exports too (as "attachment._"): it titles the
    // detail dialog. "copy" is the checksum's copy button, which the legacy layout marks canCopy.
    "attachment",
    "copy",
    "file.upload.",
    // Multi-selection of attachments: the row checkboxes ("select") and the select-all one
    // ("selectAll"). Without the dot, so the bare "select" key exports too — as "select._", since
    // select.placeholder makes it a namespace. selectDate/selectGroup/selectTask come along.
    "select",
    "edit",
    "download",
    "description",
    // Deleting an attachment is final — the JCR keeps no history of removed files, so this is the
    // irreversible question, not markAsDeletedQuestion.
    "question.deleteQuestion",
    "uptodate",
    // Cell renderers of the data table: boolean ticks read "yes"/"no" as their accessible name,
    // the rating stars "rating", the tree cell "expand"/"collapse". "no" also matches
    // "nothingFound" (exported anyway), "notEnded", "notLoggedIn", "notVisible".
    "yes",
    "no",
    "rating",
    "expand",
    "collapse",
    // Consumption bar (task lists) and the attachment column's icon-only header.
    "task.consumption",
    "attachments.short",
    // Confirmation before an entity is marked as deleted, plus the generic messages the server
    // answers a write with (message.successfull*, validation.error.*).
    "question.markAsDeletedQuestion",
    // Change history of an entity: the tab's title, the entry texts and the operation names the
    // backend puts into DisplayHistoryEntry.operation / diffSummary.
    "label.historyOfChanges",
    "history.",
    "operation.",
    "changes",
    "nothingFound",
    "message.successfull",
    "validation.error.",
    // Saved list filters. Without the dot so the bare "favorite"/"favorites" keys export too.
    "favorite",
    // Start page (app/(authenticated)/page-client.tsx): the greeting and the labels of the two
    // links it offers (website, sources).
    "index.",
    // Top navigation. Not the whole "menu." tree: the entry titles come translated from /rs/menu,
    // only the chrome around them needs its own texts.
    "menu.main.title",
    "menu.favorites.more",
    "menu.myAccount",
    // Category above a list page's heading, e.g. "Common / Books" — the entry's menu parent.
    "menu.common",
    // The way back to the page's legacy version (see LegacyPageLink). One key, written for the
    // Wicket -> React migration and reused verbatim for React -> next: it names the older version
    // of the page at hand, whichever that is.
    "goreact.menu.classics",
    // Gear menu of a list page (see ListGearMenu). Without the dot so the tooltip subkeys come
    // along; the bare title then lands under "_" (see JsonNode.put).
    "settings",
    "menu.reindexNewestDatabaseEntries",
    "menu.reindexAllDatabaseEntries",
    "menu.resetFilter",
    // Background jobs the frontend watches (see components/shared/jobs/): status names, the cancel
    // question and the error a refused job carries. The progress texts themselves come translated
    // from the server in JobInfo, but the toast around them is the frontend's.
    "jobs.",
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
    val rootDir = resolveRootDir()
    generate(rootDir).forEach { (locale, json) ->
      val outFile = outFile(rootDir, locale)
      outFile.parentFile.mkdirs()
      outFile.writeText(json, ENCODING)
      println("Wrote ${outFile.path}")
    }
  }

  /**
   * Builds the catalogs without writing them, so [GenerateNextI18nMessagesTest] can compare them
   * against the committed files.
   *
   * @return locale (as in [LOCALES]) to the full JSON content of its catalog.
   */
  internal fun generate(rootDir: File): Map<String, String> {
    val defaults = readProperties(rootDir, "")
    val exported = defaults.stringPropertyNames()
      .filter { key -> PREFIXES.any { key.startsWith(it) } }
      .sorted()

    require(exported.isNotEmpty()) { "No keys matched $PREFIXES — check the prefixes." }

    return LOCALES.entries.associate { (locale, suffix) ->
      val properties = if (suffix.isEmpty()) defaults else readProperties(rootDir, suffix)
      val root = JsonNode()
      // First key, so whoever opens the file sees the warning right away (JsonNode preserves the
      // insertion order).
      root.put(MARKER_KEY, MARKER_VALUE)
      exported.forEach { key ->
        // Fall back to the default (English) bundle for untranslated keys.
        val value = properties.getProperty(key) ?: defaults.getProperty(key)
        if (value != null) {
          root.put(key, toIcu(value))
        }
      }
      locale to root.toJson()
    }
  }

  internal fun outFile(rootDir: File, locale: String) = File(rootDir, "$OUT_DIR/generated.$locale.json")

  /**
   * The repository root. Needed because Gradle runs tests in the module directory while [main] is
   * started from the IDE with the root as working directory.
   */
  internal fun resolveRootDir(): File = SourcesUtils.getBasePath().toFile()

  private fun readProperties(rootDir: File, suffix: String): Properties {
    val file = File(rootDir, "$BUNDLE$suffix.properties")
    require(file.exists()) { "Properties file not found: ${file.absolutePath}" }
    return Properties().apply {
      file.inputStream().use { load(it.reader(ENCODING)) }
    }
  }

  /**
   * Java's MessageFormat numbers its placeholders (`{0}`), while the ICU syntax next-intl uses names
   * them — so `{0}` becomes `{arg0}`.
   *
   * The apostrophes stay as they are, escaped as `''`: ICU quotes exactly like MessageFormat does, a
   * single `'` before a `{` opening a *literal* section. Unescaping them (as this did) turned
   * `Feld ''{0}''` into `Feld '{arg0}'`, and next-intl then rendered the placeholder verbatim instead
   * of substituting it — the message showed "Feld {arg0} muss ausgefüllt werden."
   */
  internal fun toIcu(value: String): String {
    return value.replace(Regex("\\{(\\d+)}")) { "{arg${it.groupValues[1]}}" }
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
