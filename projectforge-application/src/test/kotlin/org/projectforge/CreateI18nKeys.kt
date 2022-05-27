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

import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.projectforge.business.book.BookStatus
import org.projectforge.business.user.UserRightId
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.calendar.MonthHolder
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.time.DayHolder
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.web.wicket.WebConstants
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.io.File
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
  private class I18nKeyInfo(val i18nKey: String) {
    var translation: String? = null
    var translationDE: String? = null
    val locationList = mutableListOf<String>()
    fun addLocation(location: String) {
      locationList.add(location)
    }

    val locations: String
      get() = locationList.sorted().joinToString()
  }

  private val i18nKeyMap = mutableMapOf<String, I18nKeyInfo>()

  private val srcMainDirs: List<Path>
  private val resourceBundleNames = mutableListOf<String>()
  private val resourceBundles = mutableListOf<ResourceBundle>()
  private val resourceBundlesDE = mutableListOf<ResourceBundle>()
  private val reflections = Reflections("org.projectforge", Scanners.values())

  private var htmlTemplatesCounter = 0

  init {
    srcMainDirs = Files.walk(Paths.get(basePath), 4) // plugins has depth 4
      .filter { Files.isDirectory(it) && it.name == "main" && it.parent?.name == "src" }.toList()
    srcMainDirs.forEach { main ->
      val resourcesDir = Files.walk(main, 1) // src/main/resourcres/***I18n*.properties
        .filter { Files.isDirectory(it) && it.name == "resources" }.toList()
      if (resourcesDir.isNotEmpty()) {
        val propertiesFiles = listFiles(resourcesDir[0], "properties")
        propertiesFiles.filter { it.name.endsWith("_en.properties") }.forEach {
          resourceBundleNames.add(it.name.removeSuffix("_en.properties"))
        }
      }
    }
    resourceBundleNames.sort()
    resourceBundleNames.forEach {
      resourceBundles.add(ResourceBundle.getBundle(it, Locale.ENGLISH))
      resourceBundlesDE.add(ResourceBundle.getBundle(it, Locale.GERMAN))
    }
    resourceBundles.forEach { bundle ->
      bundle.keySet().forEach { key ->
        ensureI18nKeyInfo(key).translation = bundle.getString(key)
      }
    }
    resourceBundlesDE.forEach { bundle ->
      bundle.keySet().forEach { key ->
        ensureI18nKeyInfo(key).translationDE = bundle.getString(key)
      }
    }
    println(resourceBundleNames)
  }

  @Throws(IOException::class)
  fun run() {
    log.info("Create file with all detected i18n keys.")
    srcMainDirs.forEach { path ->
      parseJava(path)
      parseKotlin(path)
      parseWicketHtml(path)
      parseHtmlMailTemplates(path)
    }
    getI18nEnums()
    getPropertyInfos()
    val workbook = ExcelWorkbook.createEmptyWorkbook()
    val sheet = workbook.createOrGetSheet("I18n keys")
    sheet.registerColumn("I18n key|60", "i18nKey")
    sheet.registerColumn("English|60", "translation")
    sheet.registerColumn("German|60", "translationDE")
    sheet.registerColumn("Locations|60")
    sheet.createRow().fillHeadRow()
    i18nKeyMap.keys.sorted().forEach { key ->
      val row = sheet.createRow()
      row.autoFillFromObject(i18nKeyMap[key])
    }
    sheet.setAutoFilter()
    val file = File("i18nKeys.xlsx")
    workbook.asByteArrayOutputStream.use { baos ->
      file.writeBytes(baos.toByteArray())
    }
    println("File '${file.absolutePath}' written.")
    println("Matching html mail templates: $htmlTemplatesCounter")
  }

  @Throws(IOException::class)
  private fun parseWicketHtml(path: Path) {
    val files = listFiles(path, "html")
    for (file in files) {
      val content = getContent(file)
      find(file, content, "<wicket:message\\s+key=\"([a-zA-Z0-9\\.]+)\"\\s/>")
    }
  }

  @Throws(IOException::class)
  private fun parseHtmlMailTemplates(path: Path) {
    val files = listFiles(path, "html")
    for (file in files) {
      val content = getContent(file)
      find(content, "pf.getI18nString\\(\"([a-zA-Z0-9\\.]+)\"\\)").forEach {
        ++htmlTemplatesCounter
        add(it, file)
      }
    }
  }

  private fun getPropertyInfos() {
    // val annotations = reflections.getTypesAnnotatedWith(PropertyInfo::class.java)
    reflections.getFieldsAnnotatedWith(PropertyInfo::class.java)?.forEach { field ->
      val ann = field.getAnnotation(PropertyInfo::class.java)
      addAnnotationKeys(ann.i18nKey, ann.additionalI18nKey, field.declaringClass)
    }
    reflections.getMethodsAnnotatedWith(PropertyInfo::class.java)?.forEach { method ->
      val ann = method.getAnnotation(PropertyInfo::class.java)
      addAnnotationKeys(ann.i18nKey, ann.additionalI18nKey, method.declaringClass)
    }
  }

  private fun addAnnotationKeys(i18nKey: String?, additionalI18nKey: String?, declaringClass: Class<*>) {
    if (!i18nKey.isNullOrBlank()) {
      add(i18nKey, declaringClass.simpleName)
    }
    if (!additionalI18nKey.isNullOrBlank()) {
      add(additionalI18nKey, declaringClass.simpleName)
    }
  }

  private fun getI18nEnums() {
    val subTypes = reflections.getSubTypesOf(I18nEnum::class.java)
    subTypes.forEach {
      if (IUserRightId::class.java.isAssignableFrom(it)) {
        // OK, ignore IUserRightId
      } else {
        it.enumConstants.let { enumConstants ->
          if (enumConstants != null) {
            BookStatus.DISPOSED.i18nKey
            if (it.isEnum) {
              it.enumConstants.forEach { enum ->
                add("${enum.i18nKey}", it.simpleName)
              }
            }
          } else {
            println("******************* Oups, enumConstants are null: ${it.name}")
          }
        }
      }
    }
    UserRightId.values().forEach {
      add("${it.i18nKey}", UserRightId::class.java.simpleName)
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
    val info = ensureI18nKeyInfo(key)
    //println("$key: $location, en=${info.translation}, de=${info.translationDE}")
    info.addLocation(location)
  }

  private fun ensureI18nKeyInfo(key: String): I18nKeyInfo {
    var info = i18nKeyMap[key]
    if (info == null) {
      info = I18nKeyInfo(key)
      i18nKeyMap[key] = info
    }
    return info
  }

  private fun listFiles(path: String, suffix: String): Collection<File> {
    return FileUtils.listFiles(File(path), arrayOf(suffix), true)
  }

  private fun listFiles(path: Path, suffix: String): Collection<File> {
    return FileUtils.listFiles(path.toFile(), arrayOf(suffix), true)
  }

  @Throws(IOException::class)
  private fun getContent(file: File): String {
    return FileUtils.readFileToString(file, "UTF-8")
  }

  private val basePath: String
    get() {
      return System.getProperty("user.dir")
    }

  private fun getResourceBundle(bundleName: String, locale: Locale?): ResourceBundle {
    return if (locale != null) ResourceBundle.getBundle(bundleName, locale) else ResourceBundle.getBundle(bundleName)
  }

  companion object {
    private val PATH_DAYHOLDER = getPathForClass(DayHolder::class.java)
    private val PATH_MONTHHOLDER = getPathForClass(MonthHolder::class.java)
    private val PATH_MENU_ITEM_DEF = getPathForClass(MenuItemDef::class.java)
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
