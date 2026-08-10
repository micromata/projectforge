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
import org.projectforge.framework.utils.SourcesUtils
import com.fasterxml.jackson.databind.JsonNode as JsonNodeJackson
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
 * Which keys are exported is *derived*, not listed. Exporting the whole bundle is no option — the
 * frontend ships its catalog to the browser — so three sources answer the question, and each of them
 * is a place that has to be edited anyway when a text is needed:
 *
 * 1. [GenerateNextFieldMetadataMain.i18nKeys] — every key the generated field metadata refers to, i.e.
 *    the `@PropertyInfo` labels, tooltips and enum value labels of all entities. A new field of an
 *    entity brings its label along without anything being edited here.
 * 2. [NextI18nKeyScanner] — the keys the sources of projectforge-next name, in a `t("…")` call, in the
 *    `…Key` of a page definition or as a namespace of `useTranslations`.
 * 3. [PREFIXES] — the key families the frontend builds at runtime, where no scan can see the key: the
 *    markers of the validation, the operation names of a history entry, the status of a background job.
 *
 * Only keys the bundle actually knows are written, so the first two sources may over-deliver: an unused
 * line in the catalog is cheap, a missing text shows the raw key on the screen. Whatever stays
 * unresolved is reported (see [unresolvedKeys]) — for a page definition that is the typo check.
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
   * Key prefixes of the families the frontend builds at runtime, where neither the entity metadata nor a
   * scan of the sources can name the key: the message belongs to a value the server sends or to a marker a
   * component maps. Everything a page spells out is found by [NextI18nKeyScanner] and does not belong here.
   *
   * Everything matching lands in the catalog under its dotted path; a prefix without a trailing dot
   * matches the bare key as well, which then lands under the reserved "_" (see [JsonNode.put]).
   */
  private val PREFIXES = listOf(
    // Answers of a write: the generic success messages and the field errors, mapped from the markers of
    // lib/validation/markers.ts by components/shared/form/use-field-errors.ts.
    "message.successfull",
    "validation.error.",
    // Rules of a period of performance, reported by key at the date field they belong to (see
    // PeriodOfPerformanceValidator.END_BEFORE_BEGIN_MESSAGE_KEY).
    "error.endDateBeforeBeginDate",
    "error.posFromDateBeforeFromDate",
    // Errors a DAO refuses a write with, reported by key.
    "fibu.kost.error.",
    // Change history: the entry texts and the operation names the backend puts into
    // DisplayHistoryEntry.operation / diffSummary.
    "history.",
    "operation.",
    // Background jobs the frontend watches (components/shared/jobs/): the status names of a JobInfo.
    "jobs.",
    // List filters: the chrome of the filter bar and the quick-select periods of the history filter
    // (components/data-table/history-interval-presets.ts builds search.lastMinutes & co. from a unit).
    "filter.",
    "favorite",
    "search.last",
    "search.today",
    "search.sinceYesterday",
    // Column chooser and the column titles the data table asks for by name.
    "columns",
    // Zip mode of an attachment, whose value names its own key
    // (components/shared/attachments/attachment-metadata.tsx).
    "attachment.zip.",
    // Errors an upload is refused with, reported by key.
    "file.upload.",
    // Status names of the structure tree filter, sent as enum values.
    "task.status.",
    // Two factor authentication and webauthn: the code channels and the errors the browser API answers
    // with, both named by their value. Not user.My2FA.setup.* — long markdown blobs of a page next
    // doesn't have.
    "user.My2FACode.",
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
    unresolvedKeys(rootDir).let { unresolved ->
      if (unresolved.isNotEmpty()) {
        println("Not found in the bundle (${unresolved.size} keys): ${unresolved.joinToString()}")
      }
    }
  }

  /**
   * The keys the entity metadata and the sources of projectforge-next name that neither the bundle nor the
   * hand-written catalogs know. Not an error: most of them are the keys of the plugins, which keep their own
   * resource bundles this generator doesn't read (see `SortAndCheckI18nPropertiesMain.FILES`), and some are
   * no keys at all (`ContractDO` declares `i18nKey = "'C-"`, a literal prefix).
   *
   * Worth a look nevertheless — a key of a page definition in here is a typo, and the page will show it
   * verbatim instead of its text.
   */
  internal fun unresolvedKeys(rootDir: File): List<String> {
    val known = readProperties(rootDir, "").stringPropertyNames() + handWrittenKeys(rootDir)
    // A key that is a namespace of known keys counts as known: it may name the subtree a select reads its
    // labels from (fibu.periodOfPerformance.type) or the namespace a component translates in.
    val namespaces = known.mapTo(mutableSetOf()) { it.substringBeforeLast('.') }
    val isKnown = { key: String -> key in known || key in namespaces }
    val fromEntities = GenerateNextFieldMetadataMain.i18nKeys().filterNot(isKnown)
    val fromFrontend = NextI18nKeyScanner.scanUnknownKeys(rootDir, isKnown)
    return (fromEntities + fromFrontend).distinct().sorted()
  }

  /**
   * The dotted keys of the hand-written catalogs (`messages/en.json`, `messages/de.json`), which hold the
   * texts of the frontend itself (`books.searchPlaceholder`, `table.*`) — no backend counterpart to miss.
   *
   * Parsed, not matched by a regex: the values are ICU messages and carry braces of their own
   * (`"Last saved: {time}"`), which no brace counting can tell from the nesting.
   */
  private fun handWrittenKeys(rootDir: File): Set<String> {
    val keys = mutableSetOf<String>()
    LOCALES.keys.forEach { locale ->
      val file = File(rootDir, "$OUT_DIR/$locale.json")
      if (file.exists()) {
        collectKeys(ObjectMapper().readTree(file.readText(ENCODING)), "", keys)
      }
    }
    return keys
  }

  /**
   * Adds the dotted path of every leaf of [node] to [keys], plus the path of the node itself: a namespace
   * of the hand-written catalog may well be the counterpart of a bare key of the bundle.
   */
  private fun collectKeys(node: JsonNodeJackson, path: String, keys: MutableSet<String>) {
    if (path.isNotEmpty()) {
      keys.add(path)
    }
    node.fields().forEach { (name, child) ->
      val childPath = if (path.isEmpty()) name else "$path.$name"
      if (child.isObject) {
        collectKeys(child, childPath, keys)
      } else {
        keys.add(childPath)
      }
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
    val bundleKeys = defaults.stringPropertyNames()
    val requested = GenerateNextFieldMetadataMain.i18nKeys() + NextI18nKeyScanner.scan(rootDir)
    // A key the frontend asks for as "<key>._" needs its subtree exported too, otherwise the catalog holds a
    // plain string where the frontend expects a namespace (see NextI18nKeyScanner.scanSubtreePrefixes).
    val prefixes = PREFIXES + NextI18nKeyScanner.scanSubtreePrefixes(rootDir)
    val exported = (requested.filter { it in bundleKeys } +
        bundleKeys.filter { key -> prefixes.any { key.startsWith(it) } })
      .distinct()
      .sorted()

    require(exported.isNotEmpty()) { "No keys matched — check the key sources." }
    // A prefix that matches nothing is a typo the catalog wouldn't show.
    PREFIXES.forEach { prefix ->
      require(exported.any { it.startsWith(prefix) }) { "No key starts with '$prefix' — check PREFIXES." }
    }

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
