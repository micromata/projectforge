/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
 */
object SortAndCheckI18nPropertiesMain {
    val ENCODING = StandardCharsets.ISO_8859_1
    val LANGUAGES = listOf("de")
    val FILES = listOf("projectforge-business/src/main/resources/I18nResources")

    @JvmStatic
    fun main(args: Array<String>) {
        FILES.forEach { basename ->
            val defaultFile = FileContent(basename, "")
            defaultFile.sortAndWrite()
            val deFile = FileContent(basename, "_de")
            deFile.write(defaultFile)
        }
    }

    class FileContent(basename: String, lang: String) {
        val blocks = mutableListOf<Block>()
        val entries = mutableListOf<Entry>()
        val filename: String = "$basename$lang.properties"
        lateinit var currentBlock: Block

        init {
            newBlock()
            println("Reading file '$basename$lang.properties'...")
            File("$basename$lang.properties").forEachLine(ENCODING) { line ->
                if (line.indexOf('=') > 0 && line.trim()[0].isLetter()) {
                    add(Entry(line))
                } else {
                    add(line)
                }
            }
        }

        fun add(entry: Entry) {
            entries.add(entry)
            currentBlock.entries.add(entry)
        }

        fun add(line: String) {
            if (currentBlock.entries.size > 0) {
                // Block finished. Create a new one.
                newBlock()
            }
            currentBlock.headLines.add(line)
        }

        fun newBlock() {
            currentBlock = Block()
            blocks.add(currentBlock)
        }

        fun sortAndWrite() {
            println("Sorting and writing file '$filename'...")
            blocks.forEach { it.sort() }
            File(filename).printWriter(ENCODING).use { out ->
                blocks.forEach { block ->
                    block.headLines.forEach { line ->
                        out.println(line)
                    }
                    block.entries.forEach { entry ->
                        out.println("${entry.key}=${entry.value}")
                    }
                }
            }
        }

        fun write(masterFile: FileContent) {
            println("Writing file '$filename' by using master file...")
            blocks.forEach { it.sort() }
            val writtenKeys = mutableSetOf<String>()
            val missedKeyInLang = mutableSetOf<String>()
            File(filename).printWriter(ENCODING).use { out ->
                masterFile.blocks.forEach { block ->
                    block.headLines.forEach { line ->
                        out.println(line)
                    }
                    block.entries.forEach { entry ->
                        val key = entry.key
                        val value = entries.find { it.key == key }?.value
                        if (value == null) {
                            missedKeyInLang.add(key)
                            out.println("### not translated: $key=${entry.value}")
                        } else {
                            writtenKeys.add(key)
                            out.println("$key=$value")
                        }
                    }
                }
            }
            val missedEntriesInMaster = entries.minus(masterFile.entries)
            if (!missedKeyInLang.isNullOrEmpty() || !missedEntriesInMaster.isNullOrEmpty()) {
                val content = File(filename).readText(ENCODING)
                val mainClass = SortAndCheckI18nPropertiesMain::class.java
                File(filename).printWriter(ENCODING).use { out ->
                    out.println("# Processed output by ${mainClass.name}.kt")
                    out.println("#")
                    out.println("# You may correct the entries wherever you want without taking care of any sort order.")
                    out.println("# Make any correction you want and re-run ${mainClass.simpleName}.kt again.")
                    out.println("# This main function sorts all entries in default properties and ensures the same output in this lang properties.")
                    out.println("")
                    if (!missedEntriesInMaster.isNullOrEmpty()) {
                        out.println("# ******** Entries in '${File(filename).name}' MISSED in default '${File(masterFile.filename).name}':")
                        missedEntriesInMaster.forEach {
                            out.println("${it.key}=${it.value}")
                        }
                        out.println()
                    }
                    if (!missedKeyInLang.isNullOrEmpty()) {
                        out.println("# Missed translations from default '${File(masterFile.filename).name}' (might be OK):")
                        missedKeyInLang.forEach {key ->
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

    class Entry(line: String) {
        val key: String
        val value: String

        override fun equals(other: Any?): Boolean {
            return key == (other as? Entry)?.key
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }

        init {
            val pos = line.indexOf('=')
            key = line.substring(0, pos)
            value = line.substring(pos + 1)
        }
    }

    class Block() {
        val headLines = mutableListOf<String>()
        val entries = mutableListOf<Entry>()
        fun sort() {
            entries.sortBy { it.key?.toLowerCase() }
        }
    }
}
