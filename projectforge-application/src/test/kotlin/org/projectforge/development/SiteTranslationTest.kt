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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Keeps the German pages of the website (`site/`, see `languages` of `site/_config.yml` and `site/README.adoc`) in
 * step with the English ones, so that a change of an English page fails here until its German page follows:
 *
 * - every English page has its German page (`de/` for pages, `_docs/de/`, `_faqs/de/`), unless it says
 *   `untranslated: true` in its front matter, and every German page has its English one,
 * - a German page has the permalink of its English one, prefixed with `/de`, and links the German pages of
 *   translated pages,
 * - a German adoc page has the structure of its English one: the same headings (levels and anchors: every heading of
 *   a translated page has an explicit anchor, the deep link of the app and of the cross references in both
 *   languages), attributes, images, cross references, tables and Liquid includes,
 * - a German data file (`_data/de/`) has the structure of its English one, and `_data/translation.yml` has the same
 *   texts for every language.
 *
 * The changelogs are generated in both languages and checked by [GenerateChangelogMainTest].
 */
class SiteTranslationTest {
  private val siteDir = File(GenerateChangelogMain.resolveRootDir(), "site")

  @Test
  fun everyPageIsTranslatedOrMarkedUntranslated() {
    assertTrue(File(siteDir, "_config.yml").isFile, "Cannot locate the website, ${siteDir.absolutePath} has no _config.yml.")
    val errors = mutableListOf<String>()
    englishPages().forEach { english ->
      val german = germanFile(english)
      val untranslated = frontMatter(english)["untranslated"] == true
      when {
        untranslated && german.exists() -> errors.add("${rel(german)}: ${rel(english)} says 'untranslated: true', remove it.")
        !untranslated && !german.exists() -> errors.add(
          "${rel(german)} is missing: translate ${rel(english)} or mark it with 'untranslated: true' in its front matter."
        )
      }
    }
    germanPages().forEach { german ->
      val english = englishFile(german)
      if (!english.exists()) errors.add("${rel(german)}: there is no English page ${rel(english)}.")
    }
    assertNoErrors(errors)
  }

  @Test
  fun germanPagesHaveTheStructureOfTheirEnglishPages() {
    val errors = mutableListOf<String>()
    translatedPairs().forEach { (english, german) ->
      val where = rel(german)
      val englishFrontMatter = frontMatter(english)
      val germanFrontMatter = frontMatter(german)
      val permalink = englishFrontMatter["permalink"]?.toString()
      val expectedPermalink = permalink?.let { "/de$it" }
      if (germanFrontMatter["permalink"]?.toString() != expectedPermalink) {
        errors.add("$where: the permalink must be ${expectedPermalink ?: "missing (as in ${rel(english)})"}.")
      }
      if (englishFrontMatter["categories"] != germanFrontMatter["categories"]) {
        errors.add("$where: the categories must be ${englishFrontMatter["categories"]}, as in ${rel(english)}.")
      }
      if (english.extension == "adoc") {
        compareStructure(adocStructure(english.readText(StandardCharsets.UTF_8)), adocStructure(german.readText(StandardCharsets.UTF_8)), rel(english), where, errors)
      } else if (includes(english.readText(StandardCharsets.UTF_8)) != includes(german.readText(StandardCharsets.UTF_8))) {
        errors.add("$where: the Liquid includes differ from ${rel(english)}.")
      }
    }
    assertNoErrors(errors)
  }

  @Test
  fun germanPagesLinkGermanPages() {
    val translatedUrls = translatedPairs().mapNotNull { (english, _) -> url(english) }.filter { it != "/" }.toSet()
    val errors = mutableListOf<String>()
    val germanFiles = translatedPairs().map { it.second } + germanDataFiles()
    germanFiles.forEach { german ->
      val text = german.readText(StandardCharsets.UTF_8)
      LINK_REGEX.findAll(text).map { it.groupValues[1].substringBefore('#') }.filter { it in translatedUrls }.toSet().forEach {
        errors.add("${rel(german)}: links the English page $it, link the German one: /de$it.")
      }
    }
    assertNoErrors(errors)
  }

  @Test
  fun germanDataFilesHaveTheStructureOfTheEnglishOnes() {
    val errors = mutableListOf<String>()
    germanDataFiles().forEach { german ->
      val english = File(siteDir, "_data/${german.name}")
      if (!english.isFile) {
        errors.add("${rel(german)}: there is no English data file ${rel(english)}.")
        return@forEach
      }
      if (german.name == TAGS_FILE) {
        // Only the texts: the colors are the English file's.
        val englishTags = yaml(english) as Map<*, *>
        val germanTags = yaml(german) as Map<*, *>
        if (englishTags.keys != germanTags.keys) errors.add("${rel(german)}: the tags must be ${englishTags.keys}.")
        germanTags.forEach { (tag, value) ->
          if ((value as? Map<*, *>)?.keys != setOf("text")) errors.add("${rel(german)}: $tag must have its text only.")
        }
      } else {
        compareData(yaml(english), yaml(german), rel(german), "", errors)
      }
    }
    // Data files of the pages' sections need their German version, the German pages would show English texts.
    File(siteDir, "_data").listFiles { file -> file.extension == "yml" && file.name != TRANSLATION_FILE }?.forEach { english ->
      if (!File(siteDir, "_data/de/${english.name}").isFile) {
        errors.add("${rel(english)}: the German version _data/de/${english.name} is missing.")
      }
    }
    val translation = yaml(File(siteDir, "_data/$TRANSLATION_FILE")) as Map<*, *>
    val languages = (yaml(File(siteDir, "_config.yml")) as Map<*, *>)["languages"] as List<*>
    languages.forEach { language ->
      val texts = translation[language] as? Map<*, *>
      when {
        texts == null -> errors.add("_data/$TRANSLATION_FILE: the texts of '$language' are missing.")
        texts.keys != (translation[languages.first()] as Map<*, *>).keys ->
          errors.add("_data/$TRANSLATION_FILE: '$language' must have the same keys as '${languages.first()}'.")
      }
    }
    assertNoErrors(errors)
  }

  @Test
  fun adocStructureIsParsedOutsideOfBlocks() {
    val structure = adocStructure(
      """
      ---
      title: Test
      ---
      :toc:

      [[intro]]
      == Introduction

      See <<setup>> and image:a.png[A].

      ----
      == not a heading
      image::not-an-image.png[]
      ----

      [id=2fa]
      [discrete]
      === Two factor

      === Missing anchor
      |===
      | a
      |===
      ++++
      {% include boxes.html data="x" %}
      ++++
      """.trimIndent()
    )
    assertEquals(listOf("== intro", "=== 2fa", "=== ?"), structure.headings)
    assertEquals(listOf("Missing anchor"), structure.unanchored)
    assertEquals(listOf("toc"), structure.attributes)
    assertEquals(listOf("a.png"), structure.images)
    assertEquals(setOf("setup"), structure.xrefs)
    assertEquals(2, structure.tables)
    assertEquals(listOf("boxes.html"), structure.includes)
  }

  /** The parts of an adoc page a translation must keep, see [adocStructure]. */
  internal class AdocStructure(
    val headings: List<String>,
    val unanchored: List<String>,
    val attributes: List<String>,
    val images: List<String>,
    val xrefs: Set<String>,
    /** The number of table delimiters (`|===`, two per table). */
    val tables: Int,
    val includes: List<String>,
  )

  private fun compareStructure(english: AdocStructure, german: AdocStructure, englishWhere: String, where: String, errors: MutableList<String>) {
    english.unanchored.forEach { errors.add("$englishWhere: the heading '$it' needs an explicit anchor ([[id]] above it), it is translated.") }
    german.unanchored.forEach { errors.add("$where: the heading '$it' needs the anchor of its English heading in $englishWhere.") }
    if (english.headings != german.headings) {
      val index = english.headings.indices.firstOrNull { english.headings[it] != german.headings.getOrNull(it) } ?: english.headings.size
      errors.add(
        "$where: the headings differ from $englishWhere (level and anchor), the first one at #${index + 1}: " +
          "'${german.headings.getOrNull(index) ?: "none"}' instead of '${english.headings.getOrNull(index) ?: "none"}'."
      )
    }
    fun same(name: String, a: Any, b: Any) {
      if (a != b) errors.add("$where: the $name differ from $englishWhere: $b instead of $a.")
    }
    same("attributes", english.attributes, german.attributes)
    same("images", english.images, german.images)
    same("cross references", english.xrefs, german.xrefs)
    same("tables", english.tables, german.tables)
    same("Liquid includes", english.includes, german.includes)
  }

  /**
   * The structure of an adoc page: its headings (level and anchor, `?` without an explicit anchor) and other parts
   * outside of listing, literal and comment blocks.
   */
  internal fun adocStructure(text: String): AdocStructure {
    val headings = mutableListOf<String>()
    val unanchored = mutableListOf<String>()
    val attributes = mutableListOf<String>()
    var tables = 0
    var block: String? = null
    val lines = stripFrontMatter(text).split('\n')
    val content = StringBuilder()
    lines.forEachIndexed { index, line ->
      if (block != null) {
        if (line == block) block = null
        return@forEachIndexed
      }
      if (line in VERBATIM_DELIMITERS) {
        block = line
        return@forEachIndexed
      }
      content.appendLine(line)
      if (line == "|===") tables++
      ATTRIBUTE_REGEX.matchEntire(line)?.let { attributes.add(it.groupValues[1]) }
      HEADING_REGEX.matchEntire(line)?.let { heading ->
        val anchor = lines.subList(0, index).asReversed().takeWhile { it.startsWith("[") }
          .firstNotNullOfOrNull { attributeLine -> ANCHOR_REGEXES.firstNotNullOfOrNull { it.matchEntire(attributeLine)?.groupValues?.get(1) } }
        if (anchor == null) unanchored.add(heading.groupValues[2])
        headings.add("${heading.groupValues[1]} ${anchor ?: "?"}")
      }
    }
    val body = content.toString()
    return AdocStructure(
      headings = headings,
      unanchored = unanchored,
      attributes = attributes,
      images = IMAGE_REGEX.findAll(body).map { it.groupValues[1] }.toList(),
      xrefs = XREF_REGEX.findAll(body).map { it.groupValues[1] }.toSet(),
      tables = tables,
      includes = includes(text),
    )
  }

  private fun includes(text: String): List<String> = INCLUDE_REGEX.findAll(text).map { it.groupValues[1] }.toList()

  /** [german] has the structure of [english]: the same keys and list sizes, the same values except for texts and urls. */
  private fun compareData(english: Any?, german: Any?, where: String, path: String, errors: MutableList<String>) {
    when (english) {
      is Map<*, *> -> {
        if (german !is Map<*, *> || german.keys != english.keys) {
          errors.add("$where: ${path.ifEmpty { "the file" }} must have the keys ${english.keys}, as the English file.")
          return
        }
        english.forEach { (key, value) -> compareData(value, german[key], where, "$path.$key".removePrefix("."), errors) }
      }

      is List<*> -> {
        if (german !is List<*> || german.size != english.size) {
          errors.add("$where: ${path.ifEmpty { "the file" }} must have ${english.size} entries, as the English file.")
          return
        }
        english.forEachIndexed { index, value -> compareData(value, german[index], where, "$path[$index]", errors) }
      }

      is String -> {
        val key = path.substringAfterLast('.')
        if (german !is String) {
          errors.add("$where: $path must be a text.")
        } else if (key in VERBATIM_DATA_KEYS && german != english) {
          errors.add("$where: $path must be '$english', as in the English file.")
        }
      }

      else -> if (german != english) errors.add("$where: $path must be $english, as in the English file.")
    }
  }

  /** The English pages: of the site itself (and `misc/`), the docs and the FAQs. */
  private fun englishPages(): List<File> =
    listOf(siteDir, File(siteDir, "misc"), File(siteDir, "_docs"), File(siteDir, "_faqs")).flatMap { dir ->
      dir.listFiles { file -> file.isFile && file.extension in PAGE_EXTENSIONS && file.name !in EXCLUDED_PAGES }.orEmpty().sortedBy { it.name }
    }

  private fun germanPages(): List<File> =
    listOf(File(siteDir, "de"), File(siteDir, "_docs/de"), File(siteDir, "_faqs/de")).flatMap { dir ->
      dir.walkTopDown().filter { it.isFile && it.extension in PAGE_EXTENSIONS }.sortedBy { it.path }.toList()
    }

  private fun germanDataFiles(): List<File> =
    File(siteDir, "_data/de").listFiles { file -> file.extension == "yml" }.orEmpty().sortedBy { it.name }

  /** The English pages with their German ones. */
  private fun translatedPairs(): List<Pair<File, File>> =
    englishPages().map { it to germanFile(it) }.filter { it.second.isFile }

  /** `de/` for a page, `de/` of the collection directory for a document. */
  private fun germanFile(english: File): File {
    val relative = english.relativeTo(siteDir).path
    return if (relative.startsWith("_")) {
      File(siteDir, "${relative.substringBefore('/')}/de/${relative.substringAfter('/')}")
    } else {
      File(siteDir, "de/$relative")
    }
  }

  private fun englishFile(german: File): File {
    val relative = german.relativeTo(siteDir).path
    return File(siteDir, if (relative.startsWith("_")) relative.replaceFirst("/de/", "/") else relative.removePrefix("de/"))
  }

  /** The url of an English page: its permalink, a doc `/docs/<name>/`, null for a FAQ (not a page of its own). */
  private fun url(english: File): String? {
    val relative = english.relativeTo(siteDir).path
    return when {
      relative.startsWith("_docs/") -> "/docs/${english.nameWithoutExtension}/"
      relative.startsWith("_") -> null
      else -> frontMatter(english)["permalink"]?.toString()
    }
  }

  private fun frontMatter(file: File): Map<*, *> {
    val text = file.readText(StandardCharsets.UTF_8)
    if (!text.startsWith("---\n")) return emptyMap<Any, Any>()
    return Yaml().load<Any?>(text.substring(4, text.indexOf("\n---", 3))) as? Map<*, *> ?: emptyMap<Any, Any>()
  }

  private fun stripFrontMatter(text: String): String =
    if (text.startsWith("---\n")) text.substring(text.indexOf("\n---", 3) + 4) else text

  private fun yaml(file: File): Any? = Yaml().load(file.readText(StandardCharsets.UTF_8))

  private fun rel(file: File) = "site/${file.relativeTo(siteDir).path}"

  private fun assertNoErrors(errors: List<String>) =
    assertTrue(errors.isEmpty(), "The German pages of the website are out of step, see site/README.adoc:\n${errors.joinToString("\n")}")

  private companion object {
    val PAGE_EXTENSIONS = setOf("adoc", "md", "html")

    /** Not part of the website (see `exclude` of `site/_config.yml`). */
    val EXCLUDED_PAGES = setOf("README.adoc")

    const val TRANSLATION_FILE = "translation.yml"
    const val TAGS_FILE = "tags.yml"

    /** Values of the data files that are the same in every language (urls and texts may differ). */
    val VERBATIM_DATA_KEYS = setOf("img", "icon", "style", "match")

    /** Delimiters of blocks whose content isn't AsciiDoc: listing, literal and comment blocks. */
    val VERBATIM_DELIMITERS = setOf("----", "....", "////")

    val HEADING_REGEX = Regex("""(=+) (\S.*)""")
    val ATTRIBUTE_REGEX = Regex(""":([\w-]+!?):.*""")
    val ANCHOR_REGEXES = listOf(
      Regex("""\[\[([A-Za-z_][\w.-]*)(?:,[^\]]*)?]]"""),
      Regex("""\[id=([^\],]+)(?:,.*)?]"""),
      Regex("""\[#([A-Za-z_][\w-]*)[^\]]*]"""),
    )
    val IMAGE_REGEX = Regex("""image::?([^\[\s]+)\[""")
    val XREF_REGEX = Regex("""<<([\w.-]+)""")
    val INCLUDE_REGEX = Regex("""\{%-?\s*include\s+([\w./-]+)""")

    /** Site-relative link targets: in a link macro, a markdown link, a quoted attribute or a yaml url. */
    val LINK_REGEX = Regex("""(?:link:|]\(|="|url: )(/[\w./#-]*)""")
  }
}
