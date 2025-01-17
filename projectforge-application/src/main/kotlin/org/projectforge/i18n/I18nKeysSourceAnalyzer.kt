/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.i18n

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.projectforge.Constants
import org.projectforge.business.book.BookStatus
import org.projectforge.business.user.UserRightId
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.calendar.MonthHolder
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.time.DayHolder
import org.projectforge.framework.utils.SourcesUtils
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import kotlin.io.path.Path


private val log = KotlinLogging.logger {}

/**
 * Tries to get all used i18n keys from the sources (java and html). As result a file is written which will be checked
 * by AdminAction.checkI18nProperties. Unused i18n keys should be detected.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
internal class I18nKeysSourceAnalyzer {
    private val i18nKeyMap = mutableMapOf<String, I18nKeyUsageEntry>()

    // All string constants of form "..." are stored here with their occurrences in files.
    private val stringConstantsMap = mutableMapOf<String, MutableSet<File>>()

    private val resourceBundleNames = mutableListOf<String>()
    private val resourceBundles = mutableListOf<ResourceBundle>()
    private val resourceBundlesDE = mutableListOf<ResourceBundle>()
    private lateinit var reflections: Reflections

    private var htmlTemplatesCounter = 0

    fun run(createTmpFile: Boolean = false): Map<String, I18nKeyUsageEntry> {
        log.info("Create file with all detected i18n keys: ${getJsonFile(createTmpFile).absolutePath}")
        reflections = Reflections("org.projectforge", Scanners.entries)

        val srcMainDirs = SourcesUtils.getSrcMainDirs()
        srcMainDirs.forEach { main ->
            val resourcesDir = SourcesUtils.getSubDir(main, "resources")
            // println("$resourcesDir")
            if (resourcesDir != null) {
                val propertiesFiles = SourcesUtils.listFiles(resourcesDir, "properties")
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
                val usage = ensureI18nKeyUsage(key)
                usage.translation = bundle.getString(key)
                usage.bundleName = bundle.baseBundleName
            }
        }
        resourceBundlesDE.forEach { bundle ->
            bundle.keySet().forEach { key ->
                ensureI18nKeyUsage(key).translationDE = bundle.getString(key)
            }
        }
        srcMainDirs.forEach { path ->
            parseJava(path)
            parseKotlin(path)
            parseWicketHtml(path)
            parseHtmlMailTemplates(path)
        }
        getI18nEnums()
        getPropertyInfos()
        // Now, add all found occurrences of all i18n keys detected:
        i18nKeyMap.values.forEach { entry ->
            stringConstantsMap[entry.i18nKey]?.forEach { file ->
                entry.addUsage(file)
            }
        }
        writeJson(createTmpFile)
        println("Matching html mail templates: $htmlTemplatesCounter")
        return i18nKeyMap
    }

    private fun writeJson(createTmpFile: Boolean) {
        val file = getJsonFile(createTmpFile)
        log.info { "Writing (pretty) json formatted file: ${file.absolutePath}..." }
        // jsonFile.writeText(JsonUtils.toJson(orderedEntries))
        file.printWriter().use { out ->
            out.println("[")
            orderedEntries.forEachIndexed { index, entry ->
                if (index > 0) {
                    out.println(",")
                }
                out.print("  ")
                out.print(entry)
            }
            out.println("")
            out.println("]")
        }
    }

    private val orderedEntries: List<I18nKeyUsageEntry>
        get() = I18nKeysUsage.getOrderedEntries(i18nKeyMap.values)

    @Throws(IOException::class)
    private fun parseWicketHtml(path: File) {
        val files = SourcesUtils.listFiles(path, "html")
        for (file in files) {
            val content = getContent(file)
            find(file, content, "<wicket:message\\s+key=\"([a-zA-Z0-9\\.]+)\"\\s/>")
        }
    }

    @Throws(IOException::class)
    private fun parseHtmlMailTemplates(path: File) {
        val files = SourcesUtils.listFiles(path, "html")
        for (file in files) {
            val content = getContent(file)
            parseStringConstants(content, file)
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
            add(i18nKey, declaringClass)
        }
        if (!additionalI18nKey.isNullOrBlank()) {
            add(additionalI18nKey, declaringClass)
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
                                add("${enum.i18nKey}", it)
                            }
                        }
                    } else {
                        println("******************* Oups, enumConstants are null: ${it.name}")
                    }
                }
            }
        }
        UserRightId.entries.forEach {
            add(it.i18nKey, UserRightId::class.java)
        }
    }

    /**
     * Parses the given file for all "..." string constants for getting all possible occurrences
     * of all i18n keys of the resource bundles.
     */
    private fun parseStringConstants(fileContent: String, file: File) {
        find(fileContent, "\"([a-zA-Z0-9\\.]+)\"").forEach { stringConstant ->
            var fileSet = stringConstantsMap[stringConstant]
            if (fileSet == null) {
                fileSet = mutableSetOf()
                stringConstantsMap[stringConstant] = fileSet
            }
            fileSet.add(file)
        }
    }

    @Throws(IOException::class)
    private fun parseJava(path: File) {
        val files = SourcesUtils.listFiles(path, "java")
        for (file in files) {
            val content = getContent(file)
            parseStringConstants(content, file)
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
            if (file.path.endsWith(PATH_DAYHOLDER)) {
                for (key in DayHolder.DAY_KEYS) {
                    add("calendar.day.$key", file)
                    add("calendar.shortday.$key", file)
                }
            } else if (file.path.endsWith(PATH_MONTHHOLDER)) {
                for (key in MonthHolder.MONTH_KEYS) {
                    add("calendar.month.$key", file)
                }
            } else if (file.path.endsWith(PATH_MENU_ITEM_DEF)) {
                for (menuItem in MenuItemDefId.entries) {
                    add(menuItem.i18nKey, file)
                }
            } else if (file.name.endsWith("Page.java")
                && (content.contains("extends AbstractListPage")
                        || content.contains("extends AbstractEditPage"))
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
    private fun parseKotlin(path: File) {
        val files = SourcesUtils.listFiles(path, "kt")
        for (file in files) {
            val content = getContent(file)
            parseStringConstants(content, file)
            find(file, content, "translate\\(\"([a-zA-Z0-9\\.]+)\"\\)") // translate("i18nKey")
            //find(file, content, "translateMsg\\(\"([a-zA-Z0-9\\.]+,") // translateMst("i18nKey",...)
            //find(file, content, "addTranslations\\(\"([a-zA-Z0-9\\.]+)\"\\)") // addError("i18nkey")
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
        val info = ensureI18nKeyUsage(key)
        info.addUsage(file)
    }

    private fun add(key: String, clazz: Class<*>) {
        val info = ensureI18nKeyUsage(key)
        //println("$key: $location, en=${info.translation}, de=${info.translationDE}")
        info.addUsage(clazz)
    }

    private fun ensureI18nKeyUsage(key: String): I18nKeyUsageEntry {
        var info = i18nKeyMap[key]
        if (info == null) {
            info = I18nKeyUsageEntry(key)
            i18nKeyMap[key] = info
        }
        return info
    }

    @Throws(IOException::class)
    private fun getContent(file: File): String {
        return FileUtils.readFileToString(file, "UTF-8")
    }

    companion object {
        internal fun readJson(useFileSystem: Boolean, createTmpFile: Boolean): MutableMap<String, I18nKeyUsageEntry> {
            val map = mutableMapOf<String, I18nKeyUsageEntry>()
            val json: String
            val jsonFile = getJsonFile(createTmpFile)
            if (useFileSystem && jsonFile.exists() && jsonFile.canRead()) {
                log.info { "Reading i18nKeys from '${jsonFile.absolutePath}'" }
                json = jsonFile.readText()
            } else {
                log.info { "Reading i18nKeys from classpath: '$JSON_FILENAME'" }
                json = this::class.java.classLoader.getResource(JSON_FILENAME)?.readText() ?: ""
            }
            val array = JsonUtils.fromJson(json, Array<I18nKeyUsageEntry>::class.java)
            log.info { "${map.size} i18nKeys read." }
            array?.forEach {
                map[it.i18nKey] = it
            }
            return map
        }

        internal val basePath: Path
            get() = SourcesUtils.getBasePath()

        private const val JSON_FILENAME = "i18nKeys.json"
        internal val jsonResourceFile = File(
            Path(basePath.toString(), "projectforge-application", "src", "main", "resources").toFile(),
            JSON_FILENAME
        )

        internal val jsonTmpFile = File(
            Path(basePath.toString(), "projectforge-application", Constants.BUILD_DIR).toFile(),
            JSON_FILENAME
        )

        private fun getJsonFile(userTmpFile: Boolean = false): File {
            return if (userTmpFile) jsonTmpFile else jsonResourceFile
        }

        private val PATH_DAYHOLDER = getPathForClass(DayHolder::class.java)
        private val PATH_MONTHHOLDER = getPathForClass(MonthHolder::class.java)
        private val PATH_MENU_ITEM_DEF = getPathForClass(MenuItemDef::class.java)

        private fun getPathForClass(clazz: Class<*>): String {
            return clazz.name.replace(".", "/") + ".java"
        }
    }
}
