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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import org.projectforge.framework.utils.SourcesUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Generates the changelog of the website and of the app from `changelog/changelog.json`, the single
 * source of truth (English only, it is published).
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
 * 3. `projectforge-next/lib/generated/changelog.json` for the page `/next/changelog`.
 *
 * Both targets show a news directly above the newest release of its version (see [newsAnchors]): 8.2 above
 * the latest 8.2 snapshot, 8.1 above the 8.1 release and so on.
 *
 * The generated files are never edited by hand — [GenerateChangelogMainTest] fails if they differ from
 * what [generate] produces.
 */
object GenerateChangelogMain {
  internal const val SOURCE = "changelog/changelog.json"
  private const val CHANGELOGS_DIR = "site/_changelogs"
  private const val POSTS_PAGE = "site/changelog-posts.adoc"
  private const val NEXT_FILE = "projectforge-next/lib/generated/changelog.json"
  private const val REPO_URL = "https://github.com/micromata/projectforge"
  private const val GENERATED_NOTE = "Generated from $SOURCE by GenerateChangelogMain — do not edit."
  private val ENCODING = StandardCharsets.UTF_8

  /** Section types, the keys of `site/_data/tags.yml` (in the order they are expected to appear). */
  internal val TYPES = listOf(
    "added", "improved", "changed", "fixed", "removed", "deprecated",
    "security", "privacy", "admin", "technology", "docker",
  )

  private val ID_REGEX = Regex("""[a-z0-9]+(-[a-z0-9]+)*""")
  private val COMMIT_REGEX = Regex("""[0-9a-f]{7,40}""")
  private val HTML_REGEX = Regex("""<\s*[a-zA-Z/!]""")
  private val MARKER_REGEX = Regex("""\{/?([a-zA-Z]+)}""")
  private val CODE_REGEX = Regex("""`([^`]+)`""")
  private val LINK_REGEX = Regex("""\[([^\]]+)]\(([^)\s]+)\)""")
  private val BOLD_REGEX = Regex("""\*\*(.+?)\*\*""")
  private val ITALIC_REGEX = Regex("""(?<![\w*])\*(?![\s*])([^*]+?)(?<!\s)\*(?![\w*])""")
  private val RED_REGEX = Regex("""\{red}(.*?)\{/red}""")

  @JvmStatic
  fun main(args: Array<String>) {
    val rootDir = resolveRootDir()
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
   * All generated files, relative path to content. Throws [IllegalArgumentException] listing every
   * problem found in the source.
   */
  internal fun generate(rootDir: File): Map<String, String> {
    val root = ObjectMapper().readTree(File(rootDir, SOURCE).readText(ENCODING))
    val errors = validate(root)
    require(errors.isEmpty()) { "$SOURCE is invalid:\n${errors.joinToString("\n")}" }
    val result = linkedMapOf<String, String>()
    val anchors = newsAnchors(root)
    root["releases"].forEach { release ->
      result["$CHANGELOGS_DIR/${adocFileName(release)}"] = releaseToAdoc(release)
    }
    root["news"].forEachIndexed { index, news ->
      val anchor = root["releases"].first { it["id"].asText() == anchors[index] }
      result["$CHANGELOGS_DIR/${newsFileName(news)}"] = newsToAdoc(news, anchor)
    }
    result[POSTS_PAGE] = postsPage()
    result[NEXT_FILE] = nextJson(root)
    return result
  }

  /** Files in the changelog directory not produced by the current source (renamed or removed releases). */
  internal fun staleFiles(rootDir: File, generated: Set<String>): List<File> {
    val dir = File(rootDir, CHANGELOGS_DIR)
    return dir.listFiles()?.filter { it.isFile && "$CHANGELOGS_DIR/${it.name}" !in generated }?.sortedBy { it.name }
      ?: emptyList()
  }

  /**
   * The id of the release each news is shown above, by index of the news: the newest release whose version
   * without qualifier (`8.2` of `8.2-SNAPSHOT`) is the version of the news. Null if there is none.
   */
  internal fun newsAnchors(root: JsonNode): List<String?> =
    root["news"].map { news ->
      val version = news["version"]?.asText()
      root["releases"].firstOrNull { it["version"]?.asText()?.substringBefore('-') == version }?.get("id")?.asText()
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
      }
      requireText(release, "version", where, errors)
      requireText(release, "title", where, errors)?.let { validateTitle(it, "$where.title", errors) }
      requireDate(release, where, errors)?.let { date ->
        if (previousDate?.let { date.isAfter(it) } == true) {
          errors.add("$where: releases must be sorted by date, newest first.")
        }
        previousDate = date
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
   * release, a news ranking above the release it belongs to.
   */
  private fun sortKey(release: JsonNode, news: Boolean): String =
    "\"${release["date"].asText()} ${if (news) 1 else 0}\""

  internal fun newsToAdoc(news: JsonNode, anchor: JsonNode): String {
    val sb = StringBuilder()
    sb.appendLine("---")
    sb.appendLine("title: ${yamlString(news["title"].asText())}")
    sb.appendLine("date: ${news["date"].asText()}")
    sb.appendLine("sort_key: ${sortKey(anchor, true)}")
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

  internal fun releaseToAdoc(release: JsonNode): String {
    val sb = StringBuilder()
    sb.appendLine("---")
    sb.appendLine("title: ${yamlString(release["title"].asText())}")
    sb.appendLine("date: ${release["date"].asText()}")
    sb.appendLine("sort_key: ${sortKey(release, false)}")
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
