/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.projectforge.business.book.BookStatus
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.i18n.Priority
import org.projectforge.framework.calendar.MonthHolder
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.time.DayHolder
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.web.wicket.WebConstants
import org.reflections.Reflections
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern
import kotlin.io.path.name
import kotlin.streams.toList


fun main() {
  CreateI18nKeys().run()
}

private val log = KotlinLogging.logger {}

/**
 * Tries to get all used i18n keys from the sources (java and html). As result a file is written which will be checked
 * by AdminAction.checkI18nProperties. Unused i18n keys should be detected.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class CreateI18nKeys {
  private val i18nKeyUsage = mutableMapOf<String, MutableSet<String>>()

  @Throws(IOException::class)
  fun run() {
    log.info("Create file with all detected i18n keys.")
    val srcDirs = getMainSourceDirs(basePath)
    srcDirs.forEach { path ->
      parseJava(path)
      parseKotlin(path)
      parseWicketHtml(path)
    }
    //getI18nEnums()
    val writer = FileWriter(I18N_KEYS_FILE)
    writer
      .append(
        "# Don't edit this file. This file is only for developers for checking i18n keys and detecting missed and unused ones.\n"
      )
    val i18nKeys: Set<String> = TreeSet(i18nKeyUsage.keys)
    for (i18nKey in i18nKeys) {
      writer.append(i18nKey).append("=")
      val set: Set<String> = i18nKeyUsage[i18nKey]!!
      var first = true
      for (filename in set) {
        if (first == false) {
          writer.append(',')
        } else {
          first = false
        }
        writer.append(filename)
      }
      writer.append("\n")
    }
    IOUtils.closeQuietly(writer)
    log.info("Creation of file of found i18n keys done: " + I18N_KEYS_FILE)
  }

  @Throws(IOException::class)
  private fun parseWicketHtml(path: Path) {
    val files = listFiles(path, "html")
    for (file in files) {
      val content = getContent(file)
      find(file, content, "<wicket:message\\s+key=\"([a-zA-Z0-9\\.]+)\"\\s/>")
    }
  }

  private fun getI18nEnums() {
    val reflections = Reflections("org.projectforge")
    val subTypes = reflections.getSubTypesOf(I18nEnum::class.java)
    subTypes.forEach {
      if (IUserRightId::class.java.isAssignableFrom(it)) {
        // OK, ignore IUserRightId
      } else {
        it.enumConstants.let { enumConstants ->
          if (enumConstants != null) {
            val instance = it.enumConstants.first()
            BookStatus.DISPOSED.i18nKey
            if (it.isEnum) {
              it.enumConstants.forEach { enum ->
                add("${instance.i18nKey}.$enum", it.simpleName)
              }
            }
          } else {
            println("******************* Oups, enumConstants are null: ${it.name}")
          }
        }
      }
    }
  }

  @Throws(IOException::class)
  private fun parseJava(path: Path) {
    val files = listFiles(path, "java")
    for (file in files) {
      val content = getContent(file)
      find(file, content, "getString\\(\"([a-zA-Z0-9\\.]+)\"\\)") // getString("i18nKey")
      find(
        file,
        content,
        "getLocalizedString\\(\"([a-zA-Z0-9\\.]+)\"\\)"
      ) // getLocalizedString("i18nkey")
      find(
        file,
        content,
        "getLocalizedMessage\\(\"([a-zA-Z0-9\\.]+)\""
      ) // getLocalizedMessage("i18nkey"
      find(file, content, "addError\\(\"([a-zA-Z0-9\\.]+)\"\\)") // addError("i18nkey")
      find(
        file,
        content,
        "addError\\([a-zA-Z0-9_\"\\.]+,\\s+\"([a-zA-Z0-9\\.]+)\""
      ) // addError(..., "i18nkey"
      find(file, content, "addGlobalError\\(\"([a-zA-Z0-9\\.]+)\"\\)") // addGlobalError("i18nkey")
      find(file, content, "resolveMessage\\(\"([a-zA-Z0-9\\.]+)\"\\)") // resolveMessage("i18nkey")
      find(
        file,
        content,
        "throw new UserException\\(\"([a-zA-Z0-9\\.]+)\""
      ) // throw new UserException("i18nkey"
      find(
        file,
        content,
        "throw new AccessException\\(\"([a-zA-Z0-9\\.]+)\""
      ) // throw new AccessException("i18nkey"
      find(file, content, "I18N_KEY_[A-Z0-9_]+ = \"([a-zA-Z0-9\\.]+)\"") // I18N_KEY_... = "i18nKey"
      find(file, content, "new Holiday\\(\"([a-zA-Z0-9\\.]+)\"") // new Holiday("i18nKey"
      find(
        file, content,
        "MessageAction.getForwardResolution\\([a-zA-Z0-9_\"\\.]+,\\s+\"([a-zA-Z0-9\\.]+)\"\\)"
      ) // MessageAction.getForwardResolution(...,
      // "i18nkey");
      if (file.path.endsWith(PATH_DAYHOLDER) == true) {
        for (key in DayHolder.DAY_KEYS) {
          add("calendar.day.$key", file)
          add("calendar.shortday.$key", file)
        }
      } else if (file.path.endsWith(PATH_MONTHHOLDER) == true) {
        for (key in MonthHolder.MONTH_KEYS) {
          add("calendar.month.$key", file)
        }
      } else if (file.path.endsWith(PATH_MENU_ITEM_DEF) == true) {
        for (menuItem in MenuItemDefId.values()) {
          add(menuItem.i18nKey, file)
        }
      } else if (file.name.endsWith("Page.java") == true
        && (content.contains("extends AbstractListPage") == true
            || content.contains("extends AbstractEditPage") == true)
      ) { // Wicket
        // Page
        var list =
          find(content, "super\\(parameters, \"([a-zA-Z0-9\\.]+)\"\\);") // super(parameters, "i18nKey");
        for (entry in list) {
          add("$entry.title.edit", file)
          add("$entry.title.list", file)
          add("$entry.title.list.select", file)
        }
        list = find(
          content,
          "super\\(caller, selectProperty, \"([a-zA-Z0-9\\.]+)\"\\);"
        ) // super(caller, selectProperty,
        // "i18nKey");
        for (entry in list) {
          add("$entry.title.edit", file)
          add("$entry.title.list", file)
          add("$entry.title.list.select", file)
        }
      }
    }
  }

  @Throws(IOException::class)
  private fun parseKotlin(path: Path) {
    val files = listFiles(path, "kt")
    for (file in files) {
      val content = getContent(file)
      find(file, content, "translate\\(\"([a-zA-Z0-9\\.]+)\"\\)") // translate("i18nKey")
      //find(file, content, "translateMsg\\(\"([a-zA-Z0-9\\.]+,") // translateMst("i18nKey",...)
      //find(file, content, "addTranslations\\(\"([a-zA-Z0-9\\.]+)\"\\)") // addError("i18nkey")
    }
  }

  private fun find(
    file: File, content: String,
    regexp: String,
    prefix: String? = null
  ) {
    val list = find(content, regexp)
    for (entry in list) {
      val key = if (prefix != null) prefix + entry else entry
      add(key, file)
    }
  }

  private fun find(file: File, content: String, regexp: String) {
    find(content, regexp).forEach {
      add(it, file)
    }
  }

  private fun find(content: String, regexp: String): List<String> {
    val result: MutableList<String> = ArrayList()
    val p = Pattern.compile(regexp, Pattern.MULTILINE) // Compiles regular expression into Pattern.
    val m = p.matcher(content)
    while (m.find()) {
      result.add(m.group(1))
    }
    return result
  }


  private fun add(key: String, file: File) {
    add(key, file.name)
  }

  private fun add(key: String, location: String) {
    var set = i18nKeyUsage[key]
    if (set == null) {
      set = TreeSet()
      i18nKeyUsage[key] = set
    }
    println("$key: $location")
    set.add(location)
  }

  private fun listFiles(path: String, suffix: String): Collection<File> {
    return FileUtils.listFiles(File(path), arrayOf(suffix), true)
  }

  private fun listFiles(path: Path, suffix: String): Collection<File> {
    return FileUtils.listFiles(path.toFile(), arrayOf(suffix), true)
  }

  private fun getMainSourceDirs(path: String): List<Path> {
    val dirs = Files.walk(Paths.get(basePath), 4) // plugins has depth 4
      .filter { Files.isDirectory(it) && it.name == "main" && it.parent?.name == "src" }.toList()
    println(dirs)
    return dirs
  }

  @Throws(IOException::class)
  private fun getContent(file: File): String {
    return FileUtils.readFileToString(file, "UTF-8")
  }

  private val basePath: String
    get() {
      return System.getProperty("user.dir")
    }

  companion object {
    private const val PATH = "src/main/"
    private val PATH_DAYHOLDER = getPathForClass(DayHolder::class.java)
    private val PATH_MONTHHOLDER = getPathForClass(MonthHolder::class.java)
    private val PATH_MENU_ITEM_DEF = getPathForClass(MenuItemDef::class.java)
    private val PATH_PRIORITY = getPathForClass(Priority::class.java)
    private const val I18N_KEYS_FILE = "src/main/resources/" + WebConstants.FILE_I18N_KEYS

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
      CreateI18nKeys().run()
    }

    private fun getPathForClass(clazz: Class<*>): String {
      return clazz.name.replace(".", "/") + ".java"
    }
  }
}
