/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common.development

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Sort the I18nResources.properties and verifies the difference of keys between the languages.
 * The blocks (separated by new lines or comments) will be preserved. Sorting will only be done inside a block.
 * All specific language properties files will have the same order and blocks as the default properties file. Any non key-value-lines of
 * language specific properties file will be replaced by the lines of the default properties file.
 *
 * Running this script will preserve any key-value properties in all translation files, but will re-order them.
 * Any comment or blank line in the default properties file is preserved. Any comment or blank will be disregarded and replaced by
 * the lines of the default properties.
 *
 * Please note: You should make a copy of the resource files before using this script. Otherwise you may loose entries.
 */
object SortAndCheckI18nPropertiesMain {
  val ENCODING = StandardCharsets.ISO_8859_1 // The charset to use.
  val LANGUAGES = listOf("de")               // Add here all used languages in addition to the default language.
  val FILES = listOf(
    "projectforge-business/src/main/resources/I18nResources",
    "plugins/org.projectforge.plugins.extendemployeedata/src/main/resources/ExtendEmployeeDataI18nResources",
    "plugins/org.projectforge.plugins.datatransfer/src/main/resources/DataTransferI18nResources",
    "plugins/org.projectforge.plugins.ihk/src/main/resources/IHKI18nResources",
    "plugins/org.projectforge.plugins.todo/src/main/resources/ToDoI18nResources",
    "plugins/org.projectforge.plugins.memo/src/main/resources/MemoI18nResources",
    "plugins/org.projectforge.plugins.merlin/src/main/resources/MerlinI18nResources",
    "plugins/org.projectforge.plugins.marketing/src/main/resources/MarketingI18nResources",
    "plugins/org.projectforge.plugins.licensemanagement/src/main/resources/LicenseManagementI18nResources",
    "plugins/org.projectforge.plugins.skillmatrix/src/main/resources/SkillMatrixI18nResources",
    "plugins/org.projectforge.plugins.liquidityplanning/src/main/resources/LiquidityPlanningI18nResources"
  ) // All resource bundle files to proceed as a list.

  @JvmStatic
  fun main(args: Array<String>) {
    FILES.forEach { basename ->
      val defaultProperties = FileContent(basename, "") // Process the default properties file.
      defaultProperties.sortAndWrite()
      LANGUAGES.forEach { lang ->
        val langProperties =
          FileContent(basename, "_$lang")   // Process the lang file including diffs to the default file.
        langProperties.write(defaultProperties)
      }
    }
  }

  /**
   * Parses an I18n properties file and writes it back (sorted and compared to master (default) properties file.
   *
   * Translations will be processed in blocks. Blocks started with blank or comment lines and ends with the beginning
   * of a next block.
   * @param basename path of the resource bundle without extension.
   * @param lang Language specification of the properties file: "" for default bundle or "_de" for the de bundle.
   */
  class FileContent(basename: String, lang: String) {
    val blocks = mutableListOf<Block>()  // Block of translations including head lines (blank lines or comments).
    val entries = mutableListOf<Entry>() // Includes all translation entries of this resource bundle.
    val filename: String = "$basename$lang.properties"
    lateinit var currentBlock: Block

    init {
      newBlock() // Initially a block of key-values is needed.
      println("Reading file '$basename$lang.properties'...")
      // Read file line by line:
      File("$basename$lang.properties").forEachLine() { line ->
        if (line.indexOf('=') > 0 && line.trim()[0].isLetter()) {
          // This seems to be a key-value line with a translation.
          add(Entry.from(line))
        } else {
          // This seems to be a comment line or blank line.
          add(line)
        }
      }
    }

    /**
     * Adds a current line with key-value to the current block.
     */
    fun add(entry: Entry) {
      entries.add(entry)
      currentBlock.entries.add(entry)
    }

    /**
     * Adds a non-translation line (blank line or comment) to the current block. If the current block
     * contains already translations, a new block will be started as new current block.
     */
    fun add(line: String) {
      if (currentBlock.entries.isNotEmpty()) {
        // Translations do already exist, finish the current block and create a new one:
        newBlock()
      }
      currentBlock.headLines.add(line)
    }

    /**
     * Start a new block with translations and comments.
     */
    fun newBlock() {
      currentBlock = Block()
      blocks.add(currentBlock)
    }

    /**
     * Sort all translation entries by key for each block. Comment and blank lines are preserved. The output
     * is written directly to the source file (you should use Git commit before ;-).
     */
    fun sortAndWrite() {
      println("Sorting and writing file '$filename'...")
      blocks.forEach { it.sort() }
      File(filename).printWriter(ENCODING).use { out ->
        blocks.forEach { block ->
          // First write all the comment and blank lines of the block.
          block.headLines.forEach { line ->
            out.println(line)
          }
          // Append all (sorted) translation entries of the block:
          block.entries.forEach { entry ->
            out.println("${entry.key}=${entry.value}")
          }
        }
      }
    }

    /**
     * Writes the properties file for the language back using the sort order and comment/blank lines of
     * the given masterFile (default properties file of the resource bundle).
     *
     * Inserts in addition all differences between this language file and the default file (missing and additional
     * translations in comparison to the masterFile).
     * @param masterFile File to compare and use as a template (blocks and comments).
     */
    fun write(masterFile: FileContent) {
      println("Writing file '$filename' by using master file...")
      val writtenKeys = mutableSetOf<String>()
      val missedKeyInLang = mutableSetOf<String>()
      // Write the lang file back:
      File(filename).printWriter(ENCODING).use { out ->
        masterFile.blocks.forEach { block ->
          block.headLines.forEach { line ->
            // Write the header of the block of the master file (the comment and blank lines of the
            // language properties file will be ignored.
            out.println(line)
          }
          block.sort() // Sort all translations inside block by key.
          block.entries.forEach { entry ->
            // Write all entries in the same order of the master file.
            val key = entry.key
            val value = entries.find { it.key == key }?.value
            if (value == null) {
              // This entry of the masterFile is not part of this lang file.
              missedKeyInLang.add(key)
              out.println("### not translated: $key=${entry.value}")
            } else {
              // This entry is part of masterFile as well as of this lang file.
              writtenKeys.add(key)
              out.println("$key=$value")
            }
          }
        }
      }
      // Determine the translations of this lang file not defined in masterFile:
      val missedEntriesInMaster = entries.minus(masterFile.entries)
      if (!missedKeyInLang.isNullOrEmpty() || !missedEntriesInMaster.isNullOrEmpty()) {
        // Differences between master and lang file are detected. So write the lang file again including
        // the detected differences.
        val content = File(filename).readText(ENCODING)
        val mainClass = SortAndCheckI18nPropertiesMain::class.java
        File(filename).printWriter(ENCODING).use { out ->
          // Some header information:
          out.println("# Processed output by ${mainClass.name}.kt")
          out.println("#")
          out.println("# You may correct the entries wherever you want without taking care of any sort order.")
          out.println("# Make any correction you want and re-run ${mainClass.simpleName}.kt again.")
          out.println("# This main function sorts all entries in default properties and ensures the same output in this lang properties.")
          out.println("#")
          out.println("# Any comment or blank line of this file will be ignored and replaced by such lines from default properties.")
          out.println("")
          // Start with all translations missed in masterFile:
          if (!missedEntriesInMaster.isNullOrEmpty()) {
            out.println("# ******** Entries in '${File(filename).name}' MISSED in default '${File(masterFile.filename).name}':")
            out.println("#")
            missedEntriesInMaster.forEach {
              out.println("${it.key}=${it.value}")
            }
            out.println()
          }
          // Adds now all translations of masterFile missed in this language file:
          if (!missedKeyInLang.isNullOrEmpty()) {
            out.println("# Missed translations from default '${File(masterFile.filename).name}' (might be OK):")
            out.println("#")
            missedKeyInLang.forEach { key ->
              val entry = masterFile.entries.find { it.key == key }
              if (entry != null) {
                out.println("# ${entry.key}=${entry.value}")
              }
            }
            out.println()
          }
          out.println()
          out.write(content)
        }
      }
    }
  }

  /**
   * Represents a translation entry (key-value).
   */
  data class Entry(val key: String) { // key as prop of this data class for equals/hashCode
    var value: String = ""          // value isn't a part of equals/hashcCde

    companion object {
      fun from(line: String): Entry {
        val pos = line.indexOf('=')
        val key = line.substring(0, pos)
        val value = line.substring(pos + 1)
        val entry = Entry(key)
        entry.value = fixApostrophCharsAndReplaceUTFChars(value) ?: ""
        return entry
      }
    }
  }

  /**
   * Block of properties file containing head lines (comment/blank lines) and all translation entries.
   */
  class Block() {
    val headLines = mutableListOf<String>()
    val entries = mutableListOf<Entry>()

    /**
     * Sort all translation entries by [Entry.key].
     */
    fun sort() {
      entries.sortBy { it.key.toLowerCase() }
    }
  }

  /**
   * Single apostrophs of i18n messages (containg params) will be replaced (if not used as escape chars).
   * "Don't believe the hype." -> "Don't believe the hype."
   * "Don''t believe {0}." -> "Don't believe (0})."
   * "Don''t escape '{0}'." -> "Don''t escape '{0}'."
   */
  internal fun fixApostrophCharsAndReplaceUTFChars(str: String?): String? {
    str ?: return null
    val result = str.replace("Ä", "\\u00C4")
      .replace("ä", "\\u00E4")
      .replace("Ö", "\\u00D6")
      .replace("ö", "\\u00F6")
      .replace("Ü", "\\u00DC")
      .replace("ü", "\\u00FC")
      .replace("ß", "\\u00DF")
    if (!result.contains("{0}") && !result.contains("{1}") && !result.contains("\${")) {
      return result
    }
    val sb = StringBuilder()
    var lastButOneChar = 'x'
    var lastChar = 'x'
    result.forEach { char ->
      if (lastChar == '\'' && !"'{}$".contains(char) && !"'{}$".contains(lastButOneChar)) {
        // Single quote was no escape quote, so double it:
        sb.append("'")
        // reset:
        lastButOneChar = 'x'
        lastChar = 'x'
      } else {
        lastButOneChar = lastChar
        lastChar = char
      }
      sb.append(char)
    }
    if (lastChar == '\'' && !"'{}$".contains(lastButOneChar)) {
      // Double trailing single quote.
      sb.append("'")
    }
    return sb.toString()
  }
}
