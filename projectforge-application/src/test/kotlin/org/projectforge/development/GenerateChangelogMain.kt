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
 * The source has three levels of abstraction: `news` (a few highlights per version), `releases`
 * (one entry per tagged release or per snapshot milestone of develop, sections typed like
 * `site/_data/tags.yml`) and — outside the file — the commits, referenced only by the commit range
 * of a snapshot.
 *
 * All texts use a small markdown subset that both targets can render (see [validateText]):
 * `**bold**`, `*italic*`, `` `code` ``, `[text](url)`, `- ` list lines, paragraphs via a blank line and
 * `{red}text{/red}` for red text.
 *
 * Output:
 * 1. `site/_changelogs/changelog-<yyyymmdd>--<id>.adoc`, one per release, and
 *    `changelog-<yyyymmdd>--news-<version>.adoc`, one per news (the directory is owned by this generator,
 *    other files there are removed),
 * 2. `site/changelog-posts.adoc`, the changelog page of the website listing them,
 * 3. `projectforge-next/lib/generated/changelog.json` and `changelog.de.json` (the German version) for the
 *    page `/next/changelog`.
 *
 * A tagged release with `"published": false` is a mini release: `bin/pfDev.sh publish` only pushes its tag, without
 * a GitHub release, jar or docker images. Its changes are part of the release notes of the next published
 * release (see [releaseNotesMarkdown]).
 *
 * Both targets show a news directly above the release opening its version (see [newsAnchors]): 9.0 above the
 * 9.0.0 release (below the builds 9.0.1, 9.0.2), 8.2 above the latest 8.2 snapshot (no 8.2 release yet) and so on.
 *
 * New changes are collected in `changelog/unreleased/`, one file per change with its English and German
 * text (see [parseFragment]), so that branches working on the same release don't touch the same lines.
 * `/next/changelog` shows them on top as a release "Not yet released" (see [withUnreleased]), the website
 * doesn't. `bin/pfDev.sh release` moves them into the release in `changelog.json` and `changelog.de.json`
 * (`--fold`, see [fold]).
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
  private val TRANSLATED_NEWS_FIELDS = setOf("title", "text", "highlights")
  private val TRANSLATED_RELEASE_FIELDS = setOf("title", "intro", "sections")

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
   * the newest one by default (see [fold]).
   */
  @JvmStatic
  fun main(args: Array<String>) {
    val rootDir = resolveRootDir()
    val checkIndex = args.indexOf("--check-release")
    if (checkIndex >= 0) {
      exitProcess(checkReleaseMain(rootDir, args.getOrNull(checkIndex + 1).orEmpty()))
    }
    val foldIndex = args.indexOf("--fold")
    if (foldIndex >= 0) {
      foldMain(rootDir, args.getOrNull(foldIndex + 1)?.takeIf { it.isNotBlank() && !it.startsWith("--") })
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
   * The check of `bin/pfDev.sh release`, which folds `changelog/unreleased/` into the release only after its
   * confirmation: so the release is checked (and its notes are written) as it will be, with the fragments folded
   * into the newest release (if that isn't the one of [version], [checkRelease] says so).
   */
  private fun checkReleaseMain(rootDir: File, version: String): Int {
    var root = ObjectMapper().readTree(File(rootDir, SOURCE).readText(ENCODING))
    var translation = ObjectMapper().readTree(File(rootDir, SOURCE_DE).readText(ENCODING))
    val fragments = readFragments(rootDir)
    val releaseId = root["releases"]?.get(0)?.get("id")?.asText()
    if (fragments.isNotEmpty() && releaseId != null && translation["releases"]?.get(releaseId) != null) {
      fold(root, translation, releaseId, fragments).let { (folded, foldedTranslation) ->
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

  private fun foldMain(rootDir: File, releaseId: String?) {
    val files = fragmentFiles(rootDir)
    if (files.isEmpty()) {
      println("Nothing to fold, $FRAGMENTS_DIR has no entries.")
      return
    }
    val root = ObjectMapper().readTree(File(rootDir, SOURCE).readText(ENCODING))
    val translation = ObjectMapper().readTree(File(rootDir, SOURCE_DE).readText(ENCODING))
    val id = releaseId ?: root["releases"][0]["id"].asText()
    val (folded, foldedTranslation) = fold(root, translation, id, readFragments(rootDir))
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

  /** The version a news is kept by: major.minor, `8.2` of `8.2.37`, `8.2.0-SNAPSHOT` or `8.2-SNAPSHOT`. */
  internal fun newsVersion(version: String): String =
    version.substringBefore('-').split('.').take(2).joinToString(".")

  /**
   * Checks that the changelog [root] (and its [translation]) is ready for the release [version] `X.Y.Z`: valid,
   * its release is the newest entry of `releases`, tagged `X.Y.Z-RELEASE`, not dated in the future and not
   * released before. Major and minor releases (`X.Y.0`) open a line and need its news (version `X.Y`), a build
   * (`X.Y.Z`, Z > 0) is listed above the news of its line.
   */
  internal fun checkRelease(
    root: JsonNode,
    translation: JsonNode,
    version: String,
    today: LocalDate = LocalDate.now(),
  ): List<String> {
    val match = RELEASE_VERSION_REGEX.matchEntire(version)
      ?: return listOf("'$version' is no release version, expected X.Y.Z (e.g. 8.2.37).")
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
    val line = newsVersion(version)
    if (match.groupValues[3] == "0" && root["news"].none { it["version"]?.asText() == line }) {
      errors.add("news: the release $version opens the version $line and needs a news with version \"$line\".")
    }
    return errors
  }

  /**
   * The GitHub release notes of the release [version] (Markdown, English): the intros, the news of a major or
   * minor release (`X.Y.0`) and the sections, merged by type, of the release and of the unpublished releases
   * since the last published one (see [aggregatedReleases]), newest first.
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
    releases.flatMap { it["intro"]?.toList().orEmpty() }.forEach {
      sb.appendLine()
      sb.appendLine(textToMarkdown(it.asText()))
    }
    releases.map { it["version"].asText() }.filter { it.endsWith(".0") }.forEach { releaseVersion ->
      root["news"].firstOrNull { it["version"]?.asText() == newsVersion(releaseVersion) }?.let { news ->
        sb.appendLine()
        sb.appendLine("## ${news["title"].asText()}")
        sb.appendLine()
        sb.appendLine(textToMarkdown(news["text"].asText()))
        news["highlights"]?.takeIf { !it.isEmpty }?.let { highlights ->
          sb.appendLine()
          highlights.forEach { sb.appendLine("- ${textToMarkdown(it.asText())}") }
        }
      }
    }
    // The order of the types is the one of their first appearance, items of newer releases first.
    val sections = linkedMapOf<String, MutableList<JsonNode>>()
    releases.forEach { release ->
      release["sections"].forEach { sections.getOrPut(it["type"].asText()) { mutableListOf() }.addAll(it["items"]) }
    }
    sections.forEach { (type, items) ->
      sb.appendLine()
      sb.appendLine("## ${type.replaceFirstChar { it.uppercase() }}")
      sb.appendLine()
      items.forEach { item ->
        if (item.isTextual) {
          val lines = textToMarkdown(item.asText()).split('\n')
          sb.appendLine("- ${lines.first()}")
          lines.drop(1).forEach { sb.appendLine("  $it") }
        } else {
          sb.appendLine("- **${item["title"].asText()}**")
          item["items"].forEach { sb.appendLine("  - ${textToMarkdown(it.asText())}") }
        }
      }
    }
    return sb.toString()
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
    val (nextRoot, nextTranslation) = withUnreleased(root, translation, readFragments(rootDir))
    val result = linkedMapOf<String, String>()
    val anchors = newsAnchors(root)
    val releases = root["releases"].toList()
    // Releases are sorted newest first, the rank keeps their order on the website for releases of the same date.
    fun rank(release: JsonNode) = releases.size - releases.indexOf(release)
    releases.forEach { release ->
      result["$CHANGELOGS_DIR/${adocFileName(release)}"] = releaseToAdoc(release, rank(release))
    }
    root["news"].forEachIndexed { index, news ->
      val anchor = releases.first { it["id"].asText() == anchors[index] }
      result["$CHANGELOGS_DIR/${newsFileName(news)}"] = newsToAdoc(news, anchor, rank(anchor))
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

  /** The fragment files, sorted by name, so by date. Other files (README.md) are ignored. */
  internal fun fragmentFiles(rootDir: File): List<File> =
    File(rootDir, FRAGMENTS_DIR).listFiles()?.filter { it.isFile && it.name.endsWith(".json") }?.sortedBy { it.name }
      ?: emptyList()

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
   * gets the same position as the English one, so the translation keeps the structure of the source.
   */
  internal fun fold(
    root: JsonNode,
    translation: JsonNode,
    releaseId: String,
    fragments: List<Fragment>,
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
    return folded to foldedTranslation
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
   * [UNRELEASED_ID], dated by the newest fragment. Unchanged without fragments.
   */
  internal fun withUnreleased(root: JsonNode, translation: JsonNode, fragments: List<Fragment>): Pair<JsonNode, JsonNode> {
    if (fragments.isEmpty()) return root to translation
    val copy = root.deepCopy<JsonNode>()
    val translationCopy = translation.deepCopy<JsonNode>()
    (copy["releases"] as ArrayNode).insertObject(0)
      .put("id", UNRELEASED_ID)
      .put("version", copy["releases"][1]["version"].asText())
      .put("date", fragments.maxOf { it.date }.toString())
      .put("title", UNRELEASED_TITLE)
    (translationCopy["releases"] as ObjectNode).putObject(UNRELEASED_ID).put("title", UNRELEASED_TITLE_DE)
    return fold(copy, translationCopy, UNRELEASED_ID, fragments)
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
   * The id of the release each news is shown above, by index of the news: the release opening the line of the
   * news (`8.2.0` or `8.2`), so the later builds of the line are listed above the news. Without such a release
   * (only snapshots so far) the newest release whose major.minor version (see [newsVersion], `8.2` of `8.2.37`
   * or `8.2-SNAPSHOT`) is the version of the news. Null if there is none.
   */
  internal fun newsAnchors(root: JsonNode): List<String?> =
    root["news"].map { news ->
      val version = news["version"]?.asText()?.let { newsVersion(it) }
      val line = root["releases"].filter { release ->
        release["version"]?.asText()?.let { newsVersion(it) } == version
      }
      val opening = line.firstOrNull { release ->
        release["version"].asText().let { it == version || it == "$version.0" }
      }
      (opening ?: line.firstOrNull())?.get("id")?.asText()
    }

  internal fun validate(root: JsonNode): List<String> {
    val errors = mutableListOf<String>()
    val news = root["news"]
    val releases = root["releases"]
    if (news == null || !news.isArray) errors.add("'news' must be an array.")
    if (releases == null || !releases.isArray || releases.isEmpty) errors.add("'releases' must be a non-empty array.")
    if (errors.isNotEmpty()) return errors
    news.forEachIndexed { index, entry ->
      val where = "news[$index]"
      requireText(entry, "version", where, errors)
      requireDate(entry, where, errors)
      requireText(entry, "title", where, errors)?.let { validateTitle(it, "$where.title", errors) }
      requireText(entry, "text", where, errors)?.let { validateText(it, "$where.text", errors) }
      entry["highlights"]?.forEachIndexed { i, highlight ->
        validateText(highlight.asText(), "$where.highlights[$i]", errors, inline = true)
      }
    }
    newsAnchors(root).forEachIndexed { index, anchor ->
      if (anchor == null) {
        errors.add("news[$index]: no release with version ${news[index]["version"]?.asText()}, the news is shown above it.")
      }
    }
    newsAnchors(root).filterNotNull().groupBy { it }.filter { it.value.size > 1 }.keys.forEach {
      errors.add("news: more than one news for the release $it.")
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
   * by the version of a news and the id of a release: `{"news": {"8.2": {title, text, highlights}},
   * "releases": {"8-1": {title, intro, sections}}}`. Every news and release must be translated, with the
   * structure of the source: the same sections (in order and type), the same number of items, a group
   * for a group and the same number of sub list lines in an item. The texts follow the rules of the source.
   */
  internal fun validateTranslation(root: JsonNode, translation: JsonNode): List<String> {
    val errors = mutableListOf<String>()
    val news = translation["news"]
    val releases = translation["releases"]
    if (news == null || !news.isObject) errors.add("'news' must be an object, the news by version.")
    if (releases == null || !releases.isObject) errors.add("'releases' must be an object, the releases by id.")
    if (errors.isNotEmpty()) return errors
    val sourceNews = root["news"].associateBy { it["version"].asText() }
    val sourceReleases = root["releases"].associateBy { it["id"].asText() }
    news.fieldNames().asSequence().filter { it !in sourceNews }.forEach {
      errors.add("news $it: there is no news of this version in $SOURCE.")
    }
    releases.fieldNames().asSequence().filter { it !in sourceReleases }.forEach {
      errors.add("releases $it: there is no release with this id in $SOURCE.")
    }
    sourceNews.forEach { (version, original) ->
      val where = "news $version"
      val entry = news[version]
      if (entry == null) {
        errors.add("$where: the translation is missing.")
        return@forEach
      }
      validateFields(entry, TRANSLATED_NEWS_FIELDS, where, errors)
      requireText(entry, "title", where, errors)?.let { validateTitle(it, "$where.title", errors) }
      requireText(entry, "text", where, errors)?.let { validateText(it, "$where.text", errors) }
      requireSameSize(original["highlights"], entry["highlights"], "$where.highlights", errors)
      entry["highlights"]?.forEachIndexed { i, highlight ->
        validateText(highlight.asText(), "$where.highlights[$i]", errors, inline = true)
      }
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
    copy["news"].forEach { (it as ObjectNode).setAll<JsonNode>(translation["news"][it["version"].asText()] as ObjectNode) }
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

  internal fun adocFileName(release: JsonNode): String =
    "changelog-${release["date"].asText().replace("-", "")}--${release["id"].asText()}.adoc"

  internal fun newsFileName(news: JsonNode): String =
    "changelog-${news["date"].asText().replace("-", "")}--news-${news["version"].asText().replace(".", "-")}.adoc"

  /**
   * Sort key of the website's layout (`site/_layouts/changelog.html`, sorted descending): the date of the
   * release, its [rank] (the position in the source, counted from the oldest) for releases of the same date and a
   * news ranking above the release it belongs to.
   */
  private fun sortKey(release: JsonNode, rank: Int, news: Boolean): String =
    "\"${release["date"].asText()} ${"%04d".format(rank)} ${if (news) 1 else 0}\""

  internal fun newsToAdoc(news: JsonNode, anchor: JsonNode, rank: Int): String {
    val sb = StringBuilder()
    sb.appendLine("---")
    sb.appendLine("title: ${yamlString(news["title"].asText())}")
    sb.appendLine("date: ${news["date"].asText()}")
    sb.appendLine("sort_key: ${sortKey(anchor, rank, true)}")
    sb.appendLine("news: true")
    sb.appendLine("---")
    sb.appendLine(":page-liquid:")
    sb.appendLine("// $GENERATED_NOTE")
    sb.appendLine()
    sb.appendLine(blockToAdoc(news["text"].asText()))
    news["highlights"]?.takeIf { !it.isEmpty }?.let { highlights ->
      sb.appendLine()
      highlights.forEach { sb.appendLine("- ${inlineToAdoc(it.asText())}") }
    }
    return sb.toString()
  }

  internal fun releaseToAdoc(release: JsonNode, rank: Int): String {
    val sb = StringBuilder()
    sb.appendLine("---")
    sb.appendLine("title: ${yamlString(release["title"].asText())}")
    sb.appendLine("date: ${release["date"].asText()}")
    sb.appendLine("sort_key: ${sortKey(release, rank, false)}")
    sb.appendLine("---")
    sb.appendLine(":page-liquid:")
    sb.appendLine("// $GENERATED_NOTE")
    release["intro"]?.forEach {
      sb.appendLine()
      sb.appendLine(blockToAdoc(it.asText()))
    }
    val tag = release["tag"]?.asText()
    val from = release["fromCommit"]?.asText()
    val to = release["toCommit"]?.asText()
    if (tag == null && from != null && to != null) {
      sb.appendLine()
      sb.appendLine(
        "__Snapshot build $REPO_URL/commit/$to[develop@$to], " +
            "$REPO_URL/compare/$from..$to[changes since $from].__"
      )
    } else if (tag != null) {
      sb.appendLine()
      sb.appendLine("__Tag $REPO_URL/tree/$tag[$tag].__")
    }
    release["sections"].forEach { section ->
      sb.appendLine()
      sb.appendLine("++++")
      sb.appendLine("{% include tag.html tag=\"${section["type"].asText()}\" %}")
      sb.appendLine("++++")
      section["items"].forEach { item ->
        if (item.isTextual) {
          val lines = item.asText().split('\n')
          sb.appendLine("- ${inlineToAdoc(lines.first())}")
          lines.drop(1).forEach { sb.appendLine("  * ${inlineToAdoc(it.removePrefix("- "))}") }
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
    return sb.toString()
  }

  /** The page itself only has the front matter, the layout lists the releases and news of the collection. */
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
   * The source as the next page reads it, reformatted, with a note that it is generated. Each news carries
   * the `releaseId` it is shown above.
   */
  internal fun nextJson(root: JsonNode): String {
    val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    val copy = (root.deepCopy<JsonNode>() as ObjectNode)
    val anchors = newsAnchors(root)
    copy["news"].forEachIndexed { index, news -> (news as ObjectNode).put("releaseId", anchors[index]) }
    val result = mapper.createObjectNode().put("_generated", GENERATED_NOTE)
    result.setAll<JsonNode>(copy)
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
