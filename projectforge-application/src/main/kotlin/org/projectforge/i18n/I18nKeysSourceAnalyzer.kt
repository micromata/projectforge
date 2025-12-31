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
 * Tries to get all used i18n keys from the sources (java, kotlin, html, tsx, jsx, js). As result a file is written which will be checked
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
            parseReact(path)
        }
        // Also parse projectforge-webapp/src directory (not following src/main structure)
        val webappSrcDir = File(SourcesUtils.getBasePath().toFile(), "projectforge-webapp/src")
        if (webappSrcDir.exists() && webappSrcDir.isDirectory) {
            log.info { "Parsing projectforge-webapp/src for React files..." }
            parseReact(webappSrcDir)
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
            if (shouldExcludeFile(file)) continue
            val content = getContent(file)
            find(file, content, "<wicket:message\\s+key=\"([a-zA-Z0-9\\.]+)\"\\s/>")
        }
    }

    @Throws(IOException::class)
    private fun parseHtmlMailTemplates(path: File) {
        val files = SourcesUtils.listFiles(path, "html")
        for (file in files) {
            if (shouldExcludeFile(file)) continue
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
            if (shouldExcludeFile(file)) continue
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
            if (shouldExcludeFile(file)) continue
            val content = getContent(file)
            parseStringConstants(content, file)
            find(file, content, "translate\\(\"([a-zA-Z0-9\\.]+)\"") // translate("i18nKey") or translate("i18nKey", params...)
            find(file, content, "translate\\([a-zA-Z_]+,\\s*\"([a-zA-Z0-9\\.]+)\"") // translate(locale, "i18nKey") or translate(locale, "i18nKey", params...)
            find(file, content, "translateMsg\\(\"([a-zA-Z0-9\\.]+)\"") // translateMsg("i18nKey") or translateMsg("i18nKey", params...)
            find(file, content, "translateMsg\\([a-zA-Z_]+,\\s*\"([a-zA-Z0-9\\.]+)\"") // translateMsg(locale, "i18nKey") or translateMsg(locale, "i18nKey", params...)
            // UI component constructors with i18n keys
            // Note: Patterns require at least one dot to avoid matching simple strings like "Merlin", "Projects", etc.
            find(file, content, "UILayout\\(\"([a-zA-Z0-9]+\\.[a-zA-Z0-9\\.]+)\"") // UILayout("i18nKey") - requires at least one dot
            find(file, content, "title\\s*=\\s*\"([a-zA-Z0-9]+\\.[a-zA-Z0-9\\.]+)\"") // title = "i18nKey" in UI components - requires at least one dot
            find(file, content, "UIFieldset\\(\\d+,\\s*\"([a-zA-Z0-9]+\\.[a-zA-Z0-9\\.]+)\"") // UIFieldset(12, "i18nKey") - requires at least one dot
            find(file, content, "UIAlert\\(\"([a-zA-Z0-9]+\\.[a-zA-Z0-9\\.]+)\"") // UIAlert("i18nKey") - requires at least one dot
            // Property assignments with i18n keys (not in annotations, those are handled by getPropertyInfos())
            find(file, content, "\\bi18nKey\\s*=\\s*\"([a-zA-Z0-9]+\\.[a-zA-Z0-9\\.]+)\"") // i18nKey = "i18nKey"
            // Note: addTranslations("key1", "key2", ...) keys are already captured by parseStringConstants() above

            // Abstract*PagesRest constructors that use i18nKeyPrefix parameter
            // These constructors automatically generate derived keys with suffixes: .add, .edit, .list, .heading
            findAbstractPagesRestKeys(file, content)
        }
    }

    @Throws(IOException::class)
    private fun parseReact(path: File) {
        // Parse TypeScript React files (*.tsx)
        val tsxFiles = SourcesUtils.listFiles(path, "tsx")
        for (file in tsxFiles) {
            if (shouldExcludeFile(file)) continue
            val content = getContent(file)
            // Note: No parseStringConstants() for React files - we search for specific patterns only
            find(file, content, "translations\\[['\"]([a-zA-Z0-9\\.]+)['\"]\\]") // translations['i18nKey'] or translations["i18nKey"]
            find(file, content, "ui\\.translations\\[['\"]([a-zA-Z0-9\\.]+)['\"]\\]") // ui.translations['i18nKey'] or ui.translations["i18nKey"]
        }
        // Parse JSX files (*.jsx)
        val jsxFiles = SourcesUtils.listFiles(path, "jsx")
        for (file in jsxFiles) {
            if (shouldExcludeFile(file)) continue
            val content = getContent(file)
            // Note: No parseStringConstants() for React files - we search for specific patterns only
            find(file, content, "translations\\[['\"]([a-zA-Z0-9\\.]+)['\"]\\]") // translations['i18nKey'] or translations["i18nKey"]
            find(file, content, "ui\\.translations\\[['\"]([a-zA-Z0-9\\.]+)['\"]\\]") // ui.translations['i18nKey'] or ui.translations["i18nKey"]
        }
        // Parse JavaScript files (*.js)
        val jsFiles = SourcesUtils.listFiles(path, "js")
        for (file in jsFiles) {
            if (shouldExcludeFile(file)) continue
            val content = getContent(file)
            // Note: No parseStringConstants() for React files - we search for specific patterns only
            find(file, content, "translations\\[['\"]([a-zA-Z0-9\\.]+)['\"]\\]") // translations['i18nKey'] or translations["i18nKey"]
            find(file, content, "ui\\.translations\\[['\"]([a-zA-Z0-9\\.]+)['\"]\\]") // ui.translations['i18nKey'] or ui.translations["i18nKey"]
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

    /**
     * Finds i18nKeyPrefix parameters passed to Abstract*PagesRest constructors.
     * These constructors automatically generate derived keys with suffixes: .add, .edit, .list, .heading
     * Example: AbstractDTOPagesRest(EmployeeDao::class.java, "fibu.employee.title")
     * -> registers usage for fibu.employee.title.add, .edit, .list, .heading
     */
    private fun findAbstractPagesRestKeys(file: File, content: String) {
        // Pattern matches: AbstractDTOPagesRest<...>(..., "i18nKeyPrefix") or AbstractDOPagesRest or AbstractPagesRest
        val pattern = "Abstract(?:DTO|DO)?PagesRest<[^>]+>\\s*\\([^,]+,\\s*\"([a-zA-Z0-9\\.]+)\""
        val baseKeys = find(content, pattern)

        // For each base key, register the 4 derived keys
        val suffixes = listOf(".add", ".edit", ".list", ".heading")
        for (baseKey in baseKeys) {
            for (suffix in suffixes) {
                add(baseKey + suffix, file)
            }
        }
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

    /**
     * Checks if a file should be excluded from i18n key analysis.
     * This filters out third-party libraries and build artifacts.
     */
    private fun shouldExcludeFile(file: File): Boolean {
        val path = file.absolutePath

        // Exclude directories with third-party libraries
        val excludedDirs = listOf(
            "/scripts/",           // Third-party scripts (jquery, ace, etc.)
            "/include/",           // Third-party includes (bootstrap, etc.)
            "/node_modules/",      // Node modules
            "/webapp/scripts/",    // Webapp third-party scripts
            "/lesscss/",          // LESS CSS compiler
            "/build/",            // Build artifacts
            "/dist/",             // Distribution artifacts
            "/buildSrc/",         // Gradle build source (not in runtime classpath)
        )

        if (excludedDirs.any { path.contains(it) }) {
            return true
        }

        // Exclude specific file patterns
        val fileName = file.name
        val excludedPatterns = listOf(
            "jquery",             // jQuery library files
            "bootstrap",          // Bootstrap library files
            "ace.js",            // ACE editor
            "ace-",              // ACE editor modules (ace-*.js)
            ".min.js",           // Minified JavaScript files
        )

        if (excludedPatterns.any { fileName.contains(it, ignoreCase = true) }) {
            return true
        }

        return false
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
