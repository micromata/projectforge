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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.Separators
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.projectforge.framework.utils.SourcesUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.system.exitProcess

/**
 * Generates the changelog of the website and of the app from `changelog/changelog.json`, the single
 * source of truth (English, it is published). The app also shows it in German, translated in
 * `changelog/changelog.de.json` (see [validateTranslation]); the website is English only.
 *
 * Every release has three levels of detail, shown by both targets: its `summary` (a few keywords and the important
 * fixes, always shown), its `overview` (about one line per topic, unfolded on request) and all its changes (`intro`
 * and `sections`, typed like `site/_data/tags.yml`, unfolded on request). The commits are referenced only by the
 * commit range of a snapshot.
 *
 * The releases are grouped by their major version (see [groups]): the release opening it (9.0.0), its updates
 * (9.0.1, 9.0.2, …) and the snapshots of develop leading up to it.
 *
 * All texts use a small markdown subset that both targets can render (see [validateText]):
 * `**bold**`, `*italic*`, `` `code` ``, `[text](url)`, `- ` list lines, paragraphs via a blank line and
 * `{red}text{/red}` for red text.
 *
 * Output:
 * 1. `site/_changelogs/changelog-major-<major>.adoc`, one per major version, the levels as collapsible blocks
 *    (`<details>` in HTML, no script needed; the directory is owned by this generator, other files there are
 *    removed),
 * 2. `site/changelog-posts.adoc`, the changelog page of the website listing them,
 * 3. `projectforge-next/lib/generated/changelog.json` and `changelog.de.json` (the German version) for the
 *    page `/next/changelog`, already grouped.
 *
 * A tagged release with `"published": false` is a mini release: `bin/pfDev.sh publish` only pushes its tag, without
 * a GitHub release, jar or docker images. Its changes are part of the release notes of the next published
 * release (see [releaseNotesMarkdown]).
 *
 * New changes are collected in `changelog/unreleased/`, one file per change with its English and German
 * text (see [parseFragment]), so that branches working on the same release don't touch the same lines.
 * `/next/changelog` shows them on top as a release "Not yet released" (see [withUnreleased]), the website
 * doesn't. The summary and overview of the next release (and optionally its title and intro) are written in
 * `changelog/unreleased/release.json` (see [parseReleaseSummary]). `bin/pfDev.sh release` moves all of it into the
 * release in `changelog.json` and `changelog.de.json` (`--fold`, see [fold]).
 *
 * The generated files are never edited by hand — [GenerateChangelogMainTest] fails if they differ from
 * what [generate] produces.
 */
object GenerateChangelogMain {
  internal const val SOURCE = "changelog/changelog.json"
  internal const val SOURCE_DE = "changelog/changelog.de.json"
  internal const val FRAGMENTS_DIR = "changelog/unreleased"

  /** The id of the release of the not yet folded fragments, shown by `/next/changelog` only. */
  internal const val UNRELEASED_ID = "unreleased"
  private const val UNRELEASED_TITLE = "Not yet released"
  private const val UNRELEASED_TITLE_DE = "Noch nicht veröffentlicht"
  private const val CHANGELOGS_DIR = "site/_changelogs"
  private const val POSTS_PAGE = "site/changelog-posts.adoc"
  private const val NEXT_FILE = "projectforge-next/lib/generated/changelog.json"
  private const val NEXT_FILE_DE = "projectforge-next/lib/generated/changelog.de.json"
  private const val REPO_URL = "https://github.com/micromata/projectforge"
  private const val GENERATED_NOTE = "Generated from $SOURCE by GenerateChangelogMain — do not edit."
  private val ENCODING = StandardCharsets.UTF_8

  /** Section types, the keys of `site/_data/tags.yml` (in the order they are expected to appear). */
  internal val TYPES = listOf(
    "added", "improved", "changed", "fixed", "removed", "deprecated",
    "security", "privacy", "admin", "technology", "docker",
  )

  /** The fields a translation may replace, everything else (version, date, commits, types) is the source's. */
  private val TRANSLATED_RELEASE_FIELDS = setOf("title", "intro", "summary", "overview", "sections")

  /** The order of the fields of a release, as written to the sources (other fields follow). */
  private val RELEASE_FIELD_ORDER = listOf(
    "id", "version", "date", "title", "tag", "downloadLink", "published", "fromCommit", "toCommit",
    "intro", "summary", "overview", "sections",
  )

  /** The file of `changelog/unreleased/` with the summary and overview of the next release. */
  internal const val RELEASE_SUMMARY_FILE = "release.json"
  private val RELEASE_SUMMARY_FIELDS = setOf("title", "intro", "summary", "overview")

  private val FRAGMENT_FIELDS = setOf("type", "en", "de", "items")
  private val FRAGMENT_NAME_REGEX = Regex("""(\d{8})-[a-z0-9]+(-[a-z0-9]+)*\.json""")
  private val ID_REGEX = Regex("""[a-z0-9]+(-[a-z0-9]+)*""")
  private val COMMIT_REGEX = Regex("""[0-9a-f]{7,40}""")
  private val RELEASE_VERSION_REGEX = Regex("""(\d+)\.(\d+)\.(\d+)""")
  private val HTML_REGEX = Regex("""<\s*[a-zA-Z/!]""")
  private val MARKER_REGEX = Regex("""\{/?([a-zA-Z]+)}""")
  private val CODE_REGEX = Regex("""`([^`]+)`""")
  private val LINK_REGEX = Regex("""\[([^\]]+)]\(([^)\s]+)\)""")
  private val BOLD_REGEX = Regex("""\*\*(.+?)\*\*""")
  private val ITALIC_REGEX = Regex("""(?<![\w*])\*(?![\s*])([^*]+?)(?<!\s)\*(?![\w*])""")
  private val RED_REGEX = Regex("""\{red}(.*?)\{/red}""")

  /**
   * Generates all files. With `--check-release X.Y.Z` (used by `bin/pfDev.sh release`) it only checks that the
   * changelog is ready for that release (see [checkRelease]) and writes its GitHub release notes to
   * `build/release-notes-X.Y.Z.md`; it exits with 1 if the changelog isn't ready. With `--fold [id]` (used by
   * `bin/pfDev.sh changelog-fold`) it first moves the fragments of `changelog/unreleased/` into the release `id`,
   * the newest one by default (see [fold]). Both take `--add-release true|false` (used by `bin/pfDev.sh release`):
   * a missing entry of the release is added first, published or not (see [withRelease]); `--fold` then folds into it.
   */
  @JvmStatic
  fun main(args: Array<String>) {
    val rootDir = resolveRootDir()
    fun option(name: String) =
      args.indexOf(name).takeIf { it >= 0 }?.let { args.getOrNull(it + 1)?.takeIf { value -> !value.startsWith("--") }.orEmpty() }
    val addRelease = option("--add-release")?.takeIf { it.isNotBlank() }?.toBooleanStrict()
    option("--check-release")?.let { version ->
      exitProcess(checkReleaseMain(rootDir, version, addRelease))
    }
    option("--fold")?.let { value ->
      foldMain(rootDir, value.takeIf { it.isNotBlank() }, addRelease)
    }
    val files = generate(rootDir)
    files.forEach { (path, content) ->
      val file = File(rootDir, path)
      if (file.exists() && file.readText(ENCODING) == content) {
        println("Unchanged ${file.path}")
      } else {
        file.parentFile.mkdirs()
        file.writeText(content, ENCODING)
        println("Wrote ${file.path}")
      }
    }
    staleFiles(rootDir, files.keys).forEach { file ->
      file.delete()
      println("Deleted ${file.path}")
    }
    // The website page used to be a hand-written markdown file, replaced by the generated adoc.
    File(rootDir, "site/changelog-posts.md").let { if (it.delete()) println("Deleted ${it.path}") }
  }

  internal fun resolveRootDir(): File = SourcesUtils.getBasePath().toFile()

  /**
   * The check of `bin/pfDev.sh release`, which adds the release entry and folds `changelog/unreleased/` into it
   * only after its confirmation: so the release is checked (and its notes are written) as it will be, with its
   * entry added if [published] is given (see [withRelease]) and the fragments folded into the newest release (if
   * that isn't the one of [version], [checkRelease] says so).
   */
  private fun checkReleaseMain(rootDir: File, version: String, published: Boolean?): Int {
    var root = ObjectMapper().readTree(File(rootDir, SOURCE).readText(ENCODING))
    var translation = ObjectMapper().readTree(File(rootDir, SOURCE_DE).readText(ENCODING))
    if (published != null && RELEASE_VERSION_REGEX.matches(version)) {
      withRelease(root, translation, version, published).let { (added, addedTranslation) ->
        root = added
        translation = addedTranslation
      }
    }
    val fragments = readFragments(rootDir)
    val summary = readReleaseSummary(rootDir)
    val releaseId = root["releases"]?.get(0)?.get("id")?.asText()
    if ((fragments.isNotEmpty() || summary != null) && releaseId != null && translation["releases"]?.get(releaseId) != null) {
      fold(root, translation, releaseId, fragments, summary).let { (folded, foldedTranslation) ->
        root = folded
        translation = foldedTranslation
      }
    }
    val errors = checkRelease(root, translation, version)
    if (errors.isNotEmpty()) {
      System.err.println("The changelog isn't ready for the release $version:\n${errors.joinToString("\n")}")
      return 1
    }
    val notes = File(rootDir, releaseNotesPath(version))
    notes.parentFile.mkdirs()
    notes.writeText(releaseNotesMarkdown(root, version), ENCODING)
    println("The changelog is ready for the release $version, release notes: ${notes.path}")
    return 0
  }

  /**
   * `--fold [releaseId]`, with `--add-release` the release [releaseId] = `X.Y.Z` (a version, not an id): its entry is
   * added first if missing, [published] or not (see [withRelease]), and the fragments are folded into it.
   */
  private fun foldMain(rootDir: File, releaseId: String?, published: Boolean?) {
    var root = ObjectMapper().readTree(File(rootDir, SOURCE).readText(ENCODING))
    var translation = ObjectMapper().readTree(File(rootDir, SOURCE_DE).readText(ENCODING))
    if (published != null) {
      val version = requireNotNull(releaseId) { "--add-release needs the version of the release: --fold X.Y.Z." }
      val (added, addedTranslation) = withRelease(root, translation, version, published)
      if (added != root) {
        File(rootDir, SOURCE).writeText(sourceJson(added), ENCODING)
        File(rootDir, SOURCE_DE).writeText(sourceJson(addedTranslation), ENCODING)
        println("Added the release entry ${releaseId(version)} to $SOURCE and $SOURCE_DE.")
        root = added
        translation = addedTranslation
      }
    }
    val files = fragmentFiles(rootDir) + listOfNotNull(releaseSummaryFile(rootDir).takeIf { it.isFile })
    if (files.isEmpty()) {
      println("Nothing to fold, $FRAGMENTS_DIR has no entries.")
      return
    }
    val id = if (published != null) {
      root["releases"].first { it["version"]?.asText() == releaseId }["id"].asText()
    } else {
      releaseId ?: root["releases"][0]["id"].asText()
    }
    val (folded, foldedTranslation) = fold(root, translation, id, readFragments(rootDir), readReleaseSummary(rootDir))
    val errors = validate(folded) + validateTranslation(folded, foldedTranslation)
    require(errors.isEmpty()) { "The folded changelog is invalid:\n${errors.joinToString("\n")}" }
    File(rootDir, SOURCE).writeText(sourceJson(folded), ENCODING)
    File(rootDir, SOURCE_DE).writeText(sourceJson(foldedTranslation), ENCODING)
    files.forEach { it.delete() }
    println("Folded ${files.size} entries of $FRAGMENTS_DIR into the release $id of $SOURCE and $SOURCE_DE.")
  }

  /** The GitHub release notes of [version], written by the check mode of [main] (a build artifact). */
  internal fun releaseNotesPath(version: String) = "build/release-notes-$version.md"

  /** The git tag of a release, `8.2.37-RELEASE` (the scheme of all tags since 7.0). */
  internal fun releaseTag(version: String) = "$version-RELEASE"

  /** The id of the release entry of [version], `8-2-37` of `8.2.37`. */
  internal fun releaseId(version: String) = version.replace('.', '-')

  /**
   * The source [root] and its [translation] with the entry of the release [version] `X.Y.Z` on top, if there is no
   * entry of that version yet (unchanged otherwise, a misplaced entry is reported by [checkRelease]): dated [today],
   * with the default titles and tagged, [published] with a download link or as a mini release (`"published": false`).
   * Its sections come from the fragments (see [fold]).
   */
  internal fun withRelease(
    root: JsonNode,
    translation: JsonNode,
    version: String,
    published: Boolean,
    today: LocalDate = LocalDate.now(),
  ): Pair<JsonNode, JsonNode> {
    require(RELEASE_VERSION_REGEX.matches(version)) { "'$version' is no release version, expected X.Y.Z (e.g. 8.2.37)." }
    if (root["releases"].any { it["version"]?.asText() == version }) return root to translation
    val id = releaseId(version)
    val copy = root.deepCopy<JsonNode>()
    val translationCopy = translation.deepCopy<ObjectNode>()
    (copy["releases"] as ArrayNode).insertObject(0)
      .put("id", id)
      .put("version", version)
      .put("date", today.toString())
      .put("title", "ProjectForge $version released")
      .put("tag", releaseTag(version))
      .also { if (published) it.put("downloadLink", true) else it.put("published", false) }
    // An object keeps the order of its fields: the new release on top, as in the source.
    val translatedReleases = translationCopy.objectNode()
    translatedReleases.putObject(id).put("title", "ProjectForge $version veröffentlicht")
    translationCopy["releases"]?.let { translatedReleases.setAll<ObjectNode>(it as ObjectNode) }
    translationCopy.set<ObjectNode>("releases", translatedReleases)
    return copy to translationCopy
  }

  /**
   * Checks that the changelog [root] (and its [translation]) is ready for the release [version] `X.Y.Z`: valid,
   * its release is the newest entry of `releases`, tagged `X.Y.Z-RELEASE`, not dated in the future and not
   * released before. It needs its `summary` and `overview` (written in `changelog/unreleased/release.json`).
   */
  internal fun checkRelease(
    root: JsonNode,
    translation: JsonNode,
    version: String,
    today: LocalDate = LocalDate.now(),
  ): List<String> {
    if (!RELEASE_VERSION_REGEX.matches(version)) return listOf("'$version' is no release version, expected X.Y.Z (e.g. 8.2.37).")
    val errors = validate(root).toMutableList()
    if (errors.isNotEmpty()) return errors
    errors += validateTranslation(root, translation)
    val tag = releaseTag(version)
    val releases = root["releases"]
    val release = releases[0]
    val where = "releases[0] (${release["id"]?.asText()})"
    if (release["version"]?.asText() != version) {
      errors.add("$where: the newest release must be the one of version $version, add its entry on top of 'releases'.")
    }
    if (release["tag"]?.asText() != tag) errors.add("$where: 'tag' must be '$tag'.")
    if (LocalDate.parse(release["date"].asText()).isAfter(today)) errors.add("$where: the date is in the future.")
    releases.drop(1).filter { it["version"]?.asText() == version || it["tag"]?.asText() == tag }.forEach {
      errors.add("releases (${it["id"]?.asText()}): version $version is released already.")
    }
    if (release["summary"] == null || release["overview"] == null) {
      errors.add("$where: 'summary' and 'overview' are missing, write them in $FRAGMENTS_DIR/$RELEASE_SUMMARY_FILE.")
    }
    return errors
  }

  /**
   * The GitHub release notes of the release [version] (Markdown, English): the summaries, intros and overviews and,
   * folded, all changes (the sections, merged by type) of the release and of the unpublished releases since the last
   * published one (see [aggregatedReleases]), newest first.
   */
  internal fun releaseNotesMarkdown(root: JsonNode, version: String): String {
    val releases = aggregatedReleases(root, version)
    val sb = StringBuilder()
    sb.appendLine("# ProjectForge $version")
    if (releases.size > 1) {
      sb.appendLine()
      val versions = releases.drop(1).map { it["version"].asText() }
      val list = if (versions.size == 1) versions[0] else "${versions.dropLast(1).joinToString(", ")} and ${versions.last()}"
      sb.appendLine("Also includes the changes of $list, released without downloads.")
    }
    fun bullets(field: String) {
      val texts = releases.flatMap { it[field]?.toList().orEmpty() }
      if (texts.isEmpty()) return
      sb.appendLine()
      texts.forEach { appendMarkdownItem(sb, it.asText()) }
    }
    bullets("summary")
    releases.flatMap { it["intro"]?.toList().orEmpty() }.forEach {
      sb.appendLine()
      sb.appendLine(textToMarkdown(it.asText()))
    }
    if (releases.any { it["overview"] != null }) {
      sb.appendLine()
      sb.appendLine("## Overview")
      bullets("overview")
    }
    // The order of the types is the one of their first appearance, items of newer releases first.
    val sections = linkedMapOf<String, MutableList<JsonNode>>()
    releases.forEach { release ->
      release["sections"].forEach { sections.getOrPut(it["type"].asText()) { mutableListOf() }.addAll(it["items"]) }
    }
    sb.appendLine()
    sb.appendLine("<details>")
    sb.appendLine("<summary>All changes</summary>")
    sections.forEach { (type, items) ->
      sb.appendLine()
      sb.appendLine("## ${type.replaceFirstChar { it.uppercase() }}")
      sb.appendLine()
      items.forEach { item ->
        if (item.isTextual) {
          appendMarkdownItem(sb, item.asText())
        } else {
          sb.appendLine("- **${item["title"].asText()}**")
          item["items"].forEach { sb.appendLine("  - ${textToMarkdown(it.asText())}") }
        }
      }
    }
    sb.appendLine()
    sb.appendLine("</details>")
    return sb.toString()
  }

  /** A list item, its `- ` continuation lines as its sub list. */
  private fun appendMarkdownItem(sb: StringBuilder, text: String) {
    val lines = textToMarkdown(text).split('\n')
    sb.appendLine("- ${lines.first()}")
    lines.drop(1).forEach { sb.appendLine("  $it") }
  }

  /**
   * The release [version] followed by the older tagged releases with `"published": false` up to the next older
   * published one, newest first. Snapshots in between aren't part of a GitHub release and are skipped.
   */
  internal fun aggregatedReleases(root: JsonNode, version: String): List<JsonNode> {
    val releases = root["releases"].toList()
    val index = releases.indexOfFirst { it["version"]?.asText() == version }
    require(index >= 0) { "No release of version $version." }
    val older = releases.drop(index + 1).filter { it["tag"] != null }.takeWhile { !isPublished(it) }
    return listOf(releases[index]) + older
  }

  /** A release is published (GitHub release, jar, docker images) unless it says `"published": false`. */
  internal fun isPublished(release: JsonNode): Boolean = release["published"]?.asBoolean() != false

  /** The markdown subset is GitHub Markdown already, except red text: a bold warning there. */
  internal fun textToMarkdown(text: String): String =
    RED_REGEX.replace(text) { "**⚠️ ${it.groupValues[1].replace("**", "")}**" }

  /**
   * All generated files, relative path to content. Throws [IllegalArgumentException] listing every
   * problem found in the source.
   */
  internal fun generate(rootDir: File): Map<String, String> {
    val root = ObjectMapper().readTree(File(rootDir, SOURCE).readText(ENCODING))
    val errors = validate(root)
    require(errors.isEmpty()) { "$SOURCE is invalid:\n${errors.joinToString("\n")}" }
    val translation = ObjectMapper().readTree(File(rootDir, SOURCE_DE).readText(ENCODING))
    val translationErrors = validateTranslation(root, translation)
    require(translationErrors.isEmpty()) { "$SOURCE_DE is invalid:\n${translationErrors.joinToString("\n")}" }
    val (nextRoot, nextTranslation) =
      withUnreleased(root, translation, readFragments(rootDir), readReleaseSummary(rootDir))
    val result = linkedMapOf<String, String>()
    val releases = root["releases"].toList()
    // Releases are sorted newest first, the rank keeps the order of majors on the website whose newest releases share a date.
    groups(root).forEach { group ->
      val rank = releases.size - releases.indexOf(group.newest)
      result["$CHANGELOGS_DIR/${groupFileName(group)}"] = groupToAdoc(group, rank)
    }
    result[POSTS_PAGE] = postsPage()
    result[NEXT_FILE] = nextJson(nextRoot)
    result[NEXT_FILE_DE] = nextJson(translate(nextRoot, nextTranslation))
    return result
  }

  /**
   * A change of `changelog/unreleased/`, [en] and [de] are items of a section of the type [type]: a text or a
   * group `{"title", "items"}`.
   */
  internal class Fragment(val name: String, val date: LocalDate, val type: String, val en: JsonNode, val de: JsonNode)

  /** The fragment files, sorted by name, so by date. Other files (README.md, [RELEASE_SUMMARY_FILE]) are ignored. */
  internal fun fragmentFiles(rootDir: File): List<File> =
    File(rootDir, FRAGMENTS_DIR).listFiles()
      ?.filter { it.isFile && it.name.endsWith(".json") && it.name != RELEASE_SUMMARY_FILE }?.sortedBy { it.name }
      ?: emptyList()

  internal fun releaseSummaryFile(rootDir: File) = File(rootDir, "$FRAGMENTS_DIR/$RELEASE_SUMMARY_FILE")

  /** The fields of `changelog/unreleased/release.json` for the next release, [en] for the source, [de] for its translation. */
  internal class ReleaseSummary(val en: ObjectNode, val de: ObjectNode)

  /** The summary of the next release if written, throws [IllegalArgumentException] listing every problem found. */
  internal fun readReleaseSummary(rootDir: File): ReleaseSummary? {
    val file = releaseSummaryFile(rootDir).takeIf { it.isFile } ?: return null
    val errors = mutableListOf<String>()
    val summary = parseReleaseSummary(file.readText(ENCODING), errors)
    require(errors.isEmpty()) { "$FRAGMENTS_DIR/$RELEASE_SUMMARY_FILE is invalid:\n${errors.joinToString("\n")}" }
    return summary
  }

  /**
   * Parses `changelog/unreleased/release.json`, the levels of the next release written before it is released:
   * `{"summary": {"en": ["…"], "de": ["…"]}, "overview": {"en": ["…"], "de": ["…"]}}`, optionally with
   * `"title": {"en": "…", "de": "…"}` and `"intro": {"en": ["…"], "de": ["…"]}`. Null if invalid, the problems are
   * added to [errors].
   */
  internal fun parseReleaseSummary(content: String, errors: MutableList<String>): ReleaseSummary? {
    val where = "$FRAGMENTS_DIR/$RELEASE_SUMMARY_FILE"
    val errorCount = errors.size
    val node = try {
      ObjectMapper().readTree(content)
    } catch (ex: JsonProcessingException) {
      errors.add("$where: invalid JSON, ${ex.originalMessage}")
      return null
    }
    if (node == null || !node.isObject) {
      errors.add("$where: must be an object.")
      return null
    }
    node.fieldNames().asSequence().filter { it !in RELEASE_SUMMARY_FIELDS }.forEach {
      errors.add("$where: unknown field '$it', expected $RELEASE_SUMMARY_FIELDS.")
    }
    listOf("summary", "overview").filter { node[it] == null }.forEach { errors.add("$where: '$it' is missing.") }
    val mapper = ObjectMapper()
    val en = mapper.createObjectNode()
    val de = mapper.createObjectNode()
    RELEASE_SUMMARY_FIELDS.forEach { field ->
      val value = node[field] ?: return@forEach
      val fieldWhere = "$where.$field"
      if (!value.isObject || value.fieldNames().asSequence().toSet() != setOf("en", "de")) {
        errors.add("$fieldWhere: must be {\"en\", \"de\"}.")
        return@forEach
      }
      if (field == "title") {
        listOf("en", "de").forEach { language -> requireText(value, language, fieldWhere, errors)?.let { validateTitle(it, "$fieldWhere.$language", errors) } }
      } else {
        listOf("en", "de").forEach { language ->
          validateTexts(value[language], "$fieldWhere.$language", errors, inline = field == "summary", item = field == "overview")
        }
        if (value["en"].size() != value["de"].size()) errors.add("$fieldWhere: 'en' and 'de' must have the same number of entries.")
      }
      en.set<JsonNode>(field, value["en"])
      de.set<JsonNode>(field, value["de"])
    }
    if (errors.size > errorCount) return null
    return ReleaseSummary(en, de)
  }

  /** All fragments, throws [IllegalArgumentException] listing every problem found. */
  internal fun readFragments(rootDir: File): List<Fragment> {
    val errors = mutableListOf<String>()
    val fragments = fragmentFiles(rootDir).mapNotNull { parseFragment(it.name, it.readText(ENCODING), errors) }
    require(errors.isEmpty()) { "$FRAGMENTS_DIR is invalid:\n${errors.joinToString("\n")}" }
    return fragments
  }

  /**
   * Parses the fragment file [name] (`yyyyMMdd-<slug>.json`, the date orders the entries). An item:
   * `{"type": "added", "en": "Text", "de": "Text"}`, a group: `{"type": "changed", "en": "Title", "de": "Titel",
   * "items": [{"en": "Text", "de": "Text"}]}`. The texts follow the rules of the items of a release. Null if
   * invalid, the problems are added to [errors].
   */
  internal fun parseFragment(name: String, content: String, errors: MutableList<String>): Fragment? {
    val where = "$FRAGMENTS_DIR/$name"
    val errorCount = errors.size
    val date = FRAGMENT_NAME_REGEX.matchEntire(name)?.let {
      try {
        LocalDate.parse(it.groupValues[1], DateTimeFormatter.BASIC_ISO_DATE)
      } catch (_: DateTimeParseException) {
        null
      }
    }
    if (date == null) errors.add("$where: the name must be yyyyMMdd-<slug>.json, the slug lower case words joined by '-'.")
    val node = try {
      ObjectMapper().readTree(content)
    } catch (ex: JsonProcessingException) {
      errors.add("$where: invalid JSON, ${ex.originalMessage}")
      return null
    }
    if (node == null || !node.isObject) {
      errors.add("$where: must be an object.")
      return null
    }
    node.fieldNames().asSequence().filter { it !in FRAGMENT_FIELDS }.forEach {
      errors.add("$where: unknown field '$it', expected $FRAGMENT_FIELDS.")
    }
    val type = node["type"]?.asText()
    if (type !in TYPES) errors.add("$where: unknown type '$type', expected one of $TYPES.")
    val en = requireText(node, "en", where, errors)
    val de = requireText(node, "de", where, errors)
    val children = node["items"]
    val enItem: JsonNode
    val deItem: JsonNode
    if (children == null) {
      en?.let { validateText(it, "$where.en", errors, item = true) }
      de?.let { validateText(it, "$where.de", errors, item = true) }
      if (en != null && de != null && en.split('\n').size != de.split('\n').size) {
        errors.add("$where: 'en' and 'de' must have the same number of sub list lines.")
      }
      enItem = TextNode(en.orEmpty())
      deItem = TextNode(de.orEmpty())
    } else {
      en?.let { validateTitle(it, "$where.en", errors) }
      de?.let { validateTitle(it, "$where.de", errors) }
      val mapper = ObjectMapper()
      val enGroup = mapper.createObjectNode().put("title", en)
      val deGroup = mapper.createObjectNode().put("title", de)
      val enChildren = enGroup.putArray("items")
      val deChildren = deGroup.putArray("items")
      if (!children.isArray || children.isEmpty) {
        errors.add("$where: 'items' of a group must be a non-empty array of {\"en\", \"de\"} (one level only).")
      } else {
        children.forEachIndexed { c, child ->
          val childWhere = "$where.items[$c]"
          if (!child.isObject || child.fieldNames().asSequence().any { it != "en" && it != "de" }) {
            errors.add("$childWhere: must be {\"en\", \"de\"}.")
            return@forEachIndexed
          }
          requireText(child, "en", childWhere, errors)?.let {
            validateText(it, "$childWhere.en", errors, inline = true)
            enChildren.add(it)
          }
          requireText(child, "de", childWhere, errors)?.let {
            validateText(it, "$childWhere.de", errors, inline = true)
            deChildren.add(it)
          }
        }
      }
      enItem = enGroup
      deItem = deGroup
    }
    if (errors.size > errorCount) return null
    return Fragment(name, date!!, type!!, enItem, deItem)
  }

  /**
   * The source [root] and its [translation] with the [fragments] added to the release [releaseId], in the order
   * given: to the section of their type, a missing section is inserted in the order of [TYPES]. The German item
   * gets the same position as the English one, so the translation keeps the structure of the source. The fields of
   * the [summary] replace those of the release.
   */
  internal fun fold(
    root: JsonNode,
    translation: JsonNode,
    releaseId: String,
    fragments: List<Fragment>,
    summary: ReleaseSummary? = null,
  ): Pair<JsonNode, JsonNode> {
    val folded = root.deepCopy<JsonNode>()
    val foldedTranslation = translation.deepCopy<JsonNode>()
    val release = folded["releases"].firstOrNull { it["id"]?.asText() == releaseId } as? ObjectNode
      ?: throw IllegalArgumentException("$SOURCE: there is no release with id '$releaseId'.")
    val translatedRelease = foldedTranslation["releases"]?.get(releaseId) as? ObjectNode
      ?: throw IllegalArgumentException("$SOURCE_DE: the release '$releaseId' isn't translated.")
    fragments.forEach { fragment ->
      addItem(release, fragment.type, fragment.en)
      addItem(translatedRelease, fragment.type, fragment.de)
    }
    summary?.let {
      release.setAll<JsonNode>(it.en.deepCopy())
      translatedRelease.setAll<JsonNode>(it.de.deepCopy())
    }
    orderFields(release)
    orderFields(translatedRelease)
    return folded to foldedTranslation
  }

  /** Sorts the fields of [release] (of the source or of the translation) by [RELEASE_FIELD_ORDER]. */
  private fun orderFields(release: ObjectNode) {
    val fields = release.fields().asSequence().map { it.key to it.value }.toList()
    release.removeAll()
    fields.sortedBy { (name, _) -> RELEASE_FIELD_ORDER.indexOf(name).takeIf { it >= 0 } ?: RELEASE_FIELD_ORDER.size }
      .forEach { (name, value) -> release.set<JsonNode>(name, value) }
  }

  private fun addItem(release: ObjectNode, type: String, item: JsonNode) {
    val sections = release["sections"] as? ArrayNode ?: release.putArray("sections")
    val section = sections.firstOrNull { it["type"]?.asText() == type } as? ObjectNode
      ?: release.objectNode().put("type", type).also { section ->
        val index = sections.indexOfFirst { TYPES.indexOf(it["type"]?.asText()) > TYPES.indexOf(type) }
        sections.insert(if (index >= 0) index else sections.size(), section)
      }
    (section["items"] as? ArrayNode ?: section.putArray("items")).add(item)
  }

  /**
   * The source [root] and its [translation] with the not yet folded [fragments] as the newest release
   * [UNRELEASED_ID] (with the [summary] of the next release), dated by the newest fragment. Unchanged without both.
   */
  internal fun withUnreleased(
    root: JsonNode,
    translation: JsonNode,
    fragments: List<Fragment>,
    summary: ReleaseSummary? = null,
  ): Pair<JsonNode, JsonNode> {
    if (fragments.isEmpty() && summary == null) return root to translation
    val copy = root.deepCopy<JsonNode>()
    val translationCopy = translation.deepCopy<JsonNode>()
    (copy["releases"] as ArrayNode).insertObject(0)
      .put("id", UNRELEASED_ID)
      .put("version", copy["releases"][1]["version"].asText())
      .put("date", (fragments.maxOfOrNull { it.date } ?: LocalDate.now()).toString())
      .put("title", UNRELEASED_TITLE)
      .putArray("sections")
    (translationCopy["releases"] as ObjectNode).putObject(UNRELEASED_ID).put("title", UNRELEASED_TITLE_DE)
    return fold(copy, translationCopy, UNRELEASED_ID, fragments, summary)
  }

  /** The sources as they are written by hand: two spaces, `"key": value`, one array element per line. */
  internal fun sourceJson(node: JsonNode): String {
    val separators = Separators.createDefaultInstance()
      .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
      .withObjectEmptySeparator("")
      .withArrayEmptySeparator("")
    val indenter = DefaultIndenter("  ", "\n")
    val printer = DefaultPrettyPrinter(separators).withObjectIndenter(indenter).withArrayIndenter(indenter)
    return ObjectMapper().writer(printer).writeValueAsString(node) + "\n"
  }

  /** Files in the changelog directory not produced by the current source (renamed or removed releases). */
  internal fun staleFiles(rootDir: File, generated: Set<String>): List<File> {
    val dir = File(rootDir, CHANGELOGS_DIR)
    return dir.listFiles()?.filter { it.isFile && "$CHANGELOGS_DIR/${it.name}" !in generated }?.sortedBy { it.name }
      ?: emptyList()
  }

  /**
   * A major version: its [head], the release opening it (X.0.0 or X.0, else its oldest tagged release; null if there
   * is no tagged release yet), its other tagged releases ([updates], newest first) and the snapshots of develop
   * leading up to it ([snapshots], newest first).
   */
  internal class Group(val major: String, val head: JsonNode?, val updates: List<JsonNode>, val snapshots: List<JsonNode>) {
    /** The newest release of the major, the source is sorted newest first. */
    val newest: JsonNode get() = (updates + listOfNotNull(head) + snapshots).maxBy { it["date"].asText() }
  }

  /** The major version of [version]: `9` of `9.0.3` or `8.2-SNAPSHOT`. */
  internal fun majorOf(version: String): String = version.substringBefore('-').substringBefore('.')

  /**
   * The releases grouped by major version, newest first. A tagged release belongs to the major of its version, a
   * snapshot to the major of the release it leads up to: the next newer tagged one (the 8.2 snapshots to 9.0.0),
   * without one (develop ahead of the latest release) to the major of its own version.
   */
  internal fun groups(root: JsonNode): List<Group> {
    val releases = root["releases"].toList()
    return releases.withIndex().groupBy({ (index, release) ->
      val target = if (release["tag"] != null) release else releases.take(index).lastOrNull { it["tag"] != null }
      majorOf((target ?: release)["version"].asText())
    }, { it.value }).map { (major, members) ->
      val (tagged, snapshots) = members.partition { it["tag"] != null }
      val head = tagged.firstOrNull { isOpening(it) } ?: tagged.lastOrNull()
      Group(major, head, tagged.filter { it !== head }, snapshots)
    }
  }

  /** Whether [release] opens its major version: `9.0.0` or `7.0`. */
  private fun isOpening(release: JsonNode): Boolean =
    release["version"].asText().substringBefore('-').split('.').drop(1).all { it == "0" }

  internal fun validate(root: JsonNode): List<String> {
    val errors = mutableListOf<String>()
    val releases = root["releases"]
    root.fieldNames().asSequence().filter { it != "releases" }.forEach {
      errors.add("'$it' is not supported, only 'releases' (the summary and overview of a release are its 'summary' and 'overview').")
    }
    if (releases == null || !releases.isArray || releases.isEmpty) {
      errors.add("'releases' must be a non-empty array.")
      return errors
    }
    val ids = mutableSetOf<String>()
    var previousDate: LocalDate? = null
    releases.forEachIndexed { index, release ->
      val id = release["id"]?.asText()
      val where = "releases[$index] ($id)"
      when {
        id == null || !ID_REGEX.matches(id) -> errors.add("$where: 'id' must be lower case words joined by '-'.")
        !ids.add(id) -> errors.add("$where: duplicate id.")
        id == UNRELEASED_ID -> errors.add("$where: the id '$UNRELEASED_ID' is reserved for $FRAGMENTS_DIR.")
      }
      requireText(release, "version", where, errors)
      requireText(release, "title", where, errors)?.let { validateTitle(it, "$where.title", errors) }
      requireDate(release, where, errors)?.let { date ->
        if (previousDate?.let { date.isAfter(it) } == true) {
          errors.add("$where: releases must be sorted by date, newest first.")
        }
        previousDate = date
      }
      release["published"]?.let { published ->
        when {
          !published.isBoolean -> errors.add("$where: 'published' must be true or false.")
          published.asBoolean() -> {}
          release["tag"] == null -> errors.add("$where: only a tagged release can be unpublished.")
          release["downloadLink"]?.asBoolean() == true -> errors.add("$where: an unpublished release has no download link.")
        }
      }
      val from = release["fromCommit"]?.asText()
      val to = release["toCommit"]?.asText()
      if (release["tag"] == null && (from == null || to == null)) {
        errors.add("$where: a release without 'tag' (snapshot) needs 'fromCommit' and 'toCommit'.")
      }
      listOfNotNull(from, to).filterNot { COMMIT_REGEX.matches(it) }.forEach {
        errors.add("$where: '$it' is no commit hash.")
      }
      // Snapshots are contiguous: each one starts where the previous (older) one ended.
      val older = releases[index + 1]?.get("toCommit")?.asText()
      if (from != null && older != null && from != older) {
        errors.add("$where: 'fromCommit' $from must be the 'toCommit' $older of the next older release.")
      }
      release["intro"]?.forEachIndexed { i, text -> validateText(text.asText(), "$where.intro[$i]", errors) }
      release["summary"]?.let { validateTexts(it, "$where.summary", errors, inline = true) }
      release["overview"]?.let { validateTexts(it, "$where.overview", errors, item = true) }
      val sections = release["sections"]
      if (sections == null || !sections.isArray || sections.isEmpty) {
        errors.add("$where: 'sections' must be a non-empty array.")
        return@forEachIndexed
      }
      sections.forEachIndexed { s, section ->
        val type = section["type"]?.asText()
        if (type !in TYPES) errors.add("$where.sections[$s]: unknown type '$type', expected one of $TYPES.")
        val items = section["items"]
        if (items == null || !items.isArray || items.isEmpty) {
          errors.add("$where.sections[$s]: 'items' must be a non-empty array.")
          return@forEachIndexed
        }
        items.forEachIndexed { i, item ->
          val itemWhere = "$where.sections[$s].items[$i]"
          if (item.isTextual) {
            validateText(item.asText(), itemWhere, errors, item = true)
          } else {
            requireText(item, "title", itemWhere, errors)?.let { validateTitle(it, "$itemWhere.title", errors) }
            val children = item["items"]
            if (children == null || !children.isArray || children.isEmpty || children.any { !it.isTextual }) {
              errors.add("$itemWhere: a group needs 'items', a non-empty array of strings (one level only).")
            } else {
              children.forEachIndexed { c, child ->
                validateText(child.asText(), "$itemWhere.items[$c]", errors, inline = true)
              }
            }
          }
        }
      }
    }
    return errors
  }

  /**
   * Checks the German [translation] against the (valid) source [root]. The translation holds the texts only,
   * by the id of a release: `{"releases": {"8-1": {title, intro, summary, overview, sections}}}`. Every release must
   * be translated, with the
   * structure of the source: the same sections (in order and type), the same number of items, a group
   * for a group and the same number of sub list lines in an item. The texts follow the rules of the source.
   */
  internal fun validateTranslation(root: JsonNode, translation: JsonNode): List<String> {
    val errors = mutableListOf<String>()
    val releases = translation["releases"]
    translation.fieldNames().asSequence().filter { it != "releases" }.forEach {
      errors.add("'$it' is not supported, only 'releases'.")
    }
    if (releases == null || !releases.isObject) {
      errors.add("'releases' must be an object, the releases by id.")
      return errors
    }
    val sourceReleases = root["releases"].associateBy { it["id"].asText() }
    releases.fieldNames().asSequence().filter { it !in sourceReleases }.forEach {
      errors.add("releases $it: there is no release with this id in $SOURCE.")
    }
    sourceReleases.forEach { (id, original) ->
      val where = "releases $id"
      val release = releases[id]
      if (release == null) {
        errors.add("$where: the translation is missing.")
        return@forEach
      }
      validateFields(release, TRANSLATED_RELEASE_FIELDS, where, errors)
      requireText(release, "title", where, errors)?.let { validateTitle(it, "$where.title", errors) }
      requireSameSize(original["intro"], release["intro"], "$where.intro", errors)
      release["intro"]?.forEachIndexed { i, text -> validateText(text.asText(), "$where.intro[$i]", errors) }
      if (requireSameSize(original["summary"], release["summary"], "$where.summary", errors)) {
        release["summary"]?.let { validateTexts(it, "$where.summary", errors, inline = true) }
      }
      if (requireSameSize(original["overview"], release["overview"], "$where.overview", errors)) {
        release["overview"]?.let { validateTexts(it, "$where.overview", errors, item = true) }
      }
      val sections = release["sections"]
      if (!requireSameSize(original["sections"], sections, "$where.sections", errors)) return@forEach
      original["sections"].forEachIndexed { s, originalSection ->
        val section = sections[s]
        val sectionWhere = "$where.sections[$s]"
        val type = originalSection["type"].asText()
        if (section["type"]?.asText() != type) errors.add("$sectionWhere: the type must be '$type', as in $SOURCE.")
        val items = section["items"]
        if (!requireSameSize(originalSection["items"], items, "$sectionWhere.items", errors)) return@forEachIndexed
        originalSection["items"].forEachIndexed { i, originalItem ->
          val item = items[i]
          val itemWhere = "$sectionWhere.items[$i]"
          when {
            originalItem.isTextual && item.isTextual -> {
              if (item.asText().split('\n').size != originalItem.asText().split('\n').size) {
                errors.add("$itemWhere: the number of sub list lines differs from $SOURCE.")
              }
              validateText(item.asText(), itemWhere, errors, item = true)
            }

            !originalItem.isTextual && item.isObject -> {
              requireText(item, "title", itemWhere, errors)?.let { validateTitle(it, "$itemWhere.title", errors) }
              if (requireSameSize(originalItem["items"], item["items"], "$itemWhere.items", errors)) {
                item["items"].forEachIndexed { c, child ->
                  if (!child.isTextual) errors.add("$itemWhere.items[$c]: must be a string.")
                  validateText(child.asText(), "$itemWhere.items[$c]", errors, inline = true)
                }
              }
            }

            else -> errors.add("$itemWhere: must be a ${if (originalItem.isTextual) "string" else "group"}, as in $SOURCE.")
          }
        }
      }
    }
    return errors
  }

  /** The source [root] with the texts of the (valid) [translation] in place of its own. */
  internal fun translate(root: JsonNode, translation: JsonNode): JsonNode {
    val copy = root.deepCopy<JsonNode>()
    copy["releases"].forEach {
      (it as ObjectNode).setAll<JsonNode>(translation["releases"][it["id"].asText()] as ObjectNode)
    }
    return copy
  }

  private fun validateFields(node: JsonNode, allowed: Set<String>, where: String, errors: MutableList<String>) {
    node.fieldNames().asSequence().filter { it !in allowed }.forEach {
      errors.add("$where: '$it' can't be translated, only $allowed.")
    }
  }

  /** Whether [translated] is an array of the size of [original] (both missing is fine), else adds an error. */
  private fun requireSameSize(original: JsonNode?, translated: JsonNode?, where: String, errors: MutableList<String>): Boolean {
    val expected = original?.size() ?: 0
    if (translated != null && !translated.isArray || (translated?.size() ?: 0) != expected) {
      errors.add("$where: must be an array of $expected entries, as in $SOURCE.")
      return false
    }
    return true
  }

  /** [texts] must be a non-empty array of texts, each checked by [validateText]. */
  private fun validateTexts(
    texts: JsonNode?,
    where: String,
    errors: MutableList<String>,
    inline: Boolean = false,
    item: Boolean = false,
  ) {
    if (texts == null || !texts.isArray || texts.isEmpty || texts.any { !it.isTextual }) {
      errors.add("$where: must be a non-empty array of strings.")
      return
    }
    texts.forEachIndexed { i, text -> validateText(text.asText(), "$where[$i]", errors, inline = inline, item = item) }
  }

  /** Titles are plain text: the app renders them in headings and buttons, where no markup belongs. */
  internal fun validateTitle(title: String, where: String, errors: MutableList<String>) {
    if (title.contains('\n') || title.any { it in "*`[]{}<>#" }) {
      errors.add("$where: titles are plain text, without formatting or line breaks.")
    }
  }

  /**
   * Checks [text] against the markdown subset both targets render. [inline]: a single line without
   * lists; [item]: a list item, which may continue with `- ` lines (its sub list), but no paragraphs.
   */
  internal fun validateText(
    text: String,
    where: String,
    errors: MutableList<String>,
    inline: Boolean = false,
    item: Boolean = false,
  ) {
    if (text.isBlank()) errors.add("$where: empty text.")
    if (HTML_REGEX.containsMatchIn(text)) errors.add("$where: raw HTML is not supported.")
    MARKER_REGEX.findAll(text).map { it.groupValues[1] }.filter { it != "red" }.toSet().forEach {
      errors.add("$where: unknown marker {$it}, only {red}…{/red} is supported.")
    }
    val withoutRed = RED_REGEX.replace(text) { it.groupValues[1] }
    if (withoutRed.contains("{red}") || withoutRed.contains("{/red}")) {
      errors.add("$where: unbalanced {red}…{/red}.")
    }
    // The website pages are run through Liquid, which would interpret braces.
    if (MARKER_REGEX.replace(withoutRed, "").let { it.contains('{') || it.contains('}') }) {
      errors.add("$where: curly braces are not supported.")
    }
    RED_REGEX.findAll(text).forEach {
      if (it.groupValues[1].contains('#')) errors.add("$where: '#' is not supported in red text.")
    }
    val lines = text.split('\n')
    if (inline && lines.size > 1) errors.add("$where: line breaks are not supported here.")
    if (item && lines.any { it.isBlank() }) errors.add("$where: paragraphs are not supported in a list item.")
    if (item && lines.first().startsWith("- ")) errors.add("$where: a list item can't start with a sub list.")
    if (item && lines.drop(1).any { it.isNotBlank() && !it.startsWith("- ") }) {
      errors.add("$where: continuation lines of a list item must be '- ' sub list lines.")
    }
    lines.forEach { line ->
      when {
        line.startsWith("#") -> errors.add("$where: headings are not supported.")
        line.startsWith("|") -> errors.add("$where: tables are not supported.")
        line.startsWith(" ") || line.startsWith("\t") -> errors.add("$where: indented lines are not supported.")
        line.matches(Regex("""(\* |\+ |\d+\. ).*""")) -> errors.add("$where: use '- ' for list lines.")
      }
    }
  }

  internal fun groupFileName(group: Group): String = "changelog-major-${group.major}.adoc"

  /**
   * Sort key of the website's layout (`site/_layouts/changelog.html`, sorted descending): the date of the newest
   * release of the major and its [rank] (the position in the source, counted from the oldest) for majors whose
   * newest releases share a date.
   */
  private fun sortKey(newest: JsonNode, rank: Int): String = "\"${newest["date"].asText()} ${"%04d".format(rank)}\""

  /**
   * The page of a major version: its opening release, its updates and, folded, its snapshots, each release with its
   * summary shown and its overview and all of its changes as collapsible blocks (see [releaseToAdoc]).
   */
  internal fun groupToAdoc(group: Group, rank: Int): String {
    val sb = StringBuilder()
    sb.appendLine("---")
    sb.appendLine("title: ${yamlString("ProjectForge ${group.major}")}")
    sb.appendLine("date: ${group.newest["date"].asText()}")
    sb.appendLine("sort_key: ${sortKey(group.newest, rank)}")
    sb.appendLine("---")
    sb.appendLine(":page-liquid:")
    sb.appendLine("// $GENERATED_NOTE")
    (listOfNotNull(group.head) + group.updates).forEach { releaseToAdoc(it, sb, "====") }
    if (group.snapshots.isNotEmpty()) {
      sb.appendLine()
      sb.appendLine(".Development snapshots (${group.snapshots.size})")
      sb.appendLine("[%collapsible.changelog-snapshots]")
      sb.appendLine("======")
      group.snapshots.forEach { releaseToAdoc(it, sb, "====") }
      sb.appendLine("======")
    }
    return sb.toString()
  }

  /**
   * A release: its title, date and tag (or commit range), its summary and, as collapsible blocks delimited by
   * [delimiter], its overview and all changes (intro, sections, download link).
   */
  internal fun releaseToAdoc(release: JsonNode, sb: StringBuilder, delimiter: String) {
    sb.appendLine()
    sb.appendLine("[discrete.changelog-release]")
    sb.appendLine("=== ${release["title"].asText()}")
    sb.appendLine()
    val tag = release["tag"]?.asText()
    val from = release["fromCommit"]?.asText()
    val to = release["toCommit"]?.asText()
    val source = when {
      tag != null -> "tag $REPO_URL/tree/$tag[$tag]"
      from != null && to != null -> "snapshot build $REPO_URL/commit/$to[develop@$to], $REPO_URL/compare/$from..$to[changes since $from]"
      else -> null
    }
    sb.appendLine("[.changelog-source]")
    sb.appendLine("__${listOfNotNull(release["date"].asText(), source).joinToString(", ")}__")
    release["summary"]?.let { summary ->
      sb.appendLine()
      summary.forEach { sb.appendLine("- ${inlineToAdoc(it.asText())}") }
    }
    release["overview"]?.let { overview ->
      sb.appendLine()
      sb.appendLine(".Overview")
      sb.appendLine("[%collapsible]")
      sb.appendLine(delimiter)
      overview.forEach { appendAdocItem(sb, it.asText()) }
      sb.appendLine(delimiter)
    }
    sb.appendLine()
    sb.appendLine(".All changes")
    sb.appendLine("[%collapsible]")
    sb.appendLine(delimiter)
    release["intro"]?.forEachIndexed { index, intro ->
      if (index > 0) sb.appendLine()
      sb.appendLine(blockToAdoc(intro.asText()))
    }
    release["sections"].forEach { section ->
      sb.appendLine()
      sb.appendLine("++++")
      sb.appendLine("{% include tag.html tag=\"${section["type"].asText()}\" %}")
      sb.appendLine("++++")
      section["items"].forEach { item ->
        if (item.isTextual) {
          appendAdocItem(sb, item.asText())
        } else {
          sb.appendLine("- ${groupTitleToAdoc(item["title"].asText())}")
          item["items"].forEach { sb.appendLine("  * ${inlineToAdoc(it.asText())}") }
        }
      }
    }
    if (release["downloadLink"]?.asBoolean() == true) {
      sb.appendLine()
      sb.appendLine("++++")
      sb.appendLine("{% include download-link.html %}")
      sb.appendLine("++++")
    }
    sb.appendLine(delimiter)
  }

  /** A list item, its `- ` continuation lines as its sub list. */
  private fun appendAdocItem(sb: StringBuilder, text: String) {
    val lines = text.split('\n')
    sb.appendLine("- ${inlineToAdoc(lines.first())}")
    lines.drop(1).forEach { sb.appendLine("  * ${inlineToAdoc(it.removePrefix("- "))}") }
  }

  /** The page itself only has the front matter, the layout lists the majors of the collection. */
  internal fun postsPage(): String {
    val sb = StringBuilder()
    sb.appendLine("---")
    sb.appendLine("layout: changelog")
    sb.appendLine("title: Changelog")
    sb.appendLine("permalink: /changelog-posts/")
    sb.appendLine("---")
    sb.appendLine("// $GENERATED_NOTE")
    return sb.toString()
  }

  /**
   * The source as the next page reads it, grouped by major version (see [groups]), with a note that it is generated:
   * `{"unreleased": release, "groups": [{"major", "head", "updates", "snapshots"}]}`, `unreleased` (see
   * [withUnreleased]) only if there are entries in `changelog/unreleased/`.
   */
  internal fun nextJson(root: JsonNode): String {
    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    val result = mapper.createObjectNode().put("_generated", GENERATED_NOTE)
    val copy = root.deepCopy<JsonNode>() as ObjectNode
    val releases = copy["releases"] as ArrayNode
    if (releases[0]["id"]?.asText() == UNRELEASED_ID) result.set<JsonNode>("unreleased", releases.remove(0))
    val groups = result.putArray("groups")
    groups(copy).forEach { group ->
      groups.addObject().also { node ->
        node.put("major", group.major)
        group.head?.let { node.set<JsonNode>("head", it) }
        node.putArray("updates").addAll(group.updates)
        node.putArray("snapshots").addAll(group.snapshots)
      }
    }
    return mapper.writeValueAsString(result).replace("\r\n", "\n") + "\n"
  }

  /** Paragraphs and `- ` list lines of a text block. */
  internal fun blockToAdoc(text: String): String =
    text.split(Regex("""\n\s*\n""")).joinToString("\n\n") { paragraph ->
      paragraph.split('\n').joinToString("\n") { line ->
        if (line.startsWith("- ")) "- ${inlineToAdoc(line.removePrefix("- "))}" else inlineToAdoc(line)
      }
    }

  /**
   * Converts the inline markdown subset to AsciiDoc. Unconstrained marks (`**`, `__`, `##`) are used, so
   * a mark may touch a word character. Code spans and link targets are protected from the other rules.
   */
  internal fun inlineToAdoc(text: String): String {
    val protected = mutableListOf<String>()
    fun protect(value: String): String {
      protected.add(value)
      return "\u0000${protected.size - 1}\u0000"
    }
    var result = CODE_REGEX.replace(text) { protect("`+${it.groupValues[1]}+`") }
    result = LINK_REGEX.replace(result) { protect("${it.groupValues[2]}[") + it.groupValues[1] + protect("]") }
    result = BOLD_REGEX.replace(result) { protect("**") + it.groupValues[1] + protect("**") }
    result = ITALIC_REGEX.replace(result) { "__${it.groupValues[1]}__" }
    result = RED_REGEX.replace(result) { "[red]##${it.groupValues[1]}##" }
    // Placeholders may be nested (a link inside bold), so restore until none is left.
    val placeholder = Regex("\u0000(\\d+)\u0000")
    while (placeholder.containsMatchIn(result)) {
      result = placeholder.replace(result) { protected[it.groupValues[1].toInt()] }
    }
    return result
  }

  private fun groupTitleToAdoc(title: String): String = "**$title**"

  private fun yamlString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

  private fun requireText(node: JsonNode, field: String, where: String, errors: MutableList<String>): String? {
    val value = node[field]
    if (value == null || !value.isTextual || value.asText().isBlank()) {
      errors.add("$where: '$field' is missing.")
      return null
    }
    return value.asText()
  }

  private fun requireDate(node: JsonNode, where: String, errors: MutableList<String>): LocalDate? {
    val value = requireText(node, "date", where, errors) ?: return null
    return try {
      LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
      errors.add("$where: 'date' must be an ISO date (yyyy-MM-dd), got '$value'.")
      null
    }
  }
}
