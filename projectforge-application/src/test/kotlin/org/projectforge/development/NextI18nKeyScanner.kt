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

import org.projectforge.framework.utils.SourcesUtils
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Collects the i18n keys the sources of projectforge-next name, so [GenerateNextI18nMessagesMain] exports
 * the texts the frontend asks for instead of a hand-kept list of key prefixes.
 *
 * Scanning sources for i18n keys is how ProjectForge answers "which key is still in use" everywhere else
 * (see `I18nKeysSourceAnalyzer`, which covers java, kotlin, html and the legacy React app); this is the
 * same idea for the next module, reduced to what a static scan can honestly find.
 *
 * The result is a set of *candidates*, not a set of keys: every dotted string literal is collected and the
 * caller keeps whatever the resource bundle knows. That is deliberate - a frontend key can be spelled in a
 * `t("…")` call, in a `labelKey` of a page definition or in a namespace of `useTranslations`, and the price
 * of a false positive is one unused line in the catalog while the price of a false negative is a raw key on
 * the screen.
 *
 * Keys built at runtime are out of reach of any scan (`t(zipMode ?? …)`, the markers of
 * `lib/validation/markers.ts`, the operation names the backend puts into a history entry). Those stay in
 * `GenerateNextI18nMessagesMain.PREFIXES`.
 */
internal object NextI18nKeyScanner {
    private const val MODULE_DIR = "projectforge-next"

    /**
     * Source directories of the module, named one by one rather than walking [MODULE_DIR]: that keeps
     * `node_modules`, `.next`, `out`, `build` and the playwright output out by construction instead of by a
     * list of exclusions that goes stale with the next tool.
     */
    private val SOURCE_DIRS = listOf("app", "components", "lib", "hooks", "store", "i18n")

    private val ENCODING = StandardCharsets.UTF_8

    /**
     * A key in the shape a bundle spells it: at least one dot, or a single word (`save`, `cancel`). Both
     * occur, so the shape alone can't tell a key from any other string - see the class comment.
     */
    private const val KEY = "[a-zA-Z0-9_.]+"

    /**
     * Patterns whose first group is certainly meant as an i18n key. Beyond the plain `t("…")` call these are
     * the indirections the next pages are built on: a page definition names its texts in `titleKey`,
     * `labelKey`, `categoryKey` … and the component then calls `t(page.titleKey)`, so the string in the
     * definition *is* the key (see `lib/page-def/types.ts`).
     *
     * Only these are reported as unresolved — for them, "the bundle doesn't know it" means something.
     */
    private fun keyPatterns(translateFunctions: Set<String>): List<String> {
        val functions = translateFunctions.joinToString("|")
        return listOf(
            // f("key"), f("key", {…})
            """\b(?:$functions)\(\s*"($KEY)"""",
            // f.has("key"), f.rich("key"), f.markup("key")
            """\b(?:$functions)\.(?:has|rich|markup)\(\s*"($KEY)"""",
            // Every property whose name ends in Key: labelKey, titleKey, tabTitleKey, savedMessageKey,
            // searchPlaceholderKey, i18nKey, tooltipI18nKey …
            """\b[a-zA-Z0-9]*[Kk]ey:\s*"($KEY)"""",
        )
    }

    /**
     * A translate call whose key is chosen at the call site - `t(lendOut ? "loaned" : "available")`,
     * `t(zipMode ?? "attachment.zip.standard")`. Every literal of the argument list is a candidate, including
     * the one the condition compares against (`status === "OPENED"`), which is no key: that is why these are
     * candidates and not declared keys.
     */
    private fun conditionalCallPattern(translateFunctions: Set<String>) =
        Regex("""\b(?:${translateFunctions.joinToString("|")})\(([^)\n]*[?][^)\n]*)\)""")

    /**
     * The names a file binds a translate function to, so a component that keeps two of them
     * (`const t = useTranslations("login")`, `const tb = useTranslations()`) is read as well as the plain `t`.
     *
     * `translate` is always one of them: that is the `Translate` function the list and edit components pass
     * around instead of the hook (see `components/shared/list/use-declared-columns.tsx`).
     */
    private val TRANSLATE_BINDING_PATTERN = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*useTranslations\(""")

    private fun translateFunctions(content: String): Set<String> =
        TRANSLATE_BINDING_PATTERN.findAll(content).mapTo(mutableSetOf("t", "translate")) { it.groupValues[1] }

    /**
     * Catch-all for a key handed on in a shape [KEY_PATTERNS] doesn't cover (an array of keys, a ternary, a
     * constant). Needs the dot: a bare word would collect every identifier-like string in the module.
     *
     * Its hits are exported if the bundle knows them and dropped silently otherwise - most of them are no
     * keys at all (a date format, an import path, a version number), so reporting them would bury the hits
     * that matter.
     */
    private val CANDIDATE_PATTERN = Regex(""""([a-zA-Z0-9_]+(?:\.[a-zA-Z0-9_]+)+)"""")

    /** The namespace of a `useTranslations("…")` call; `t("x")` in that file means `<namespace>.x`. */
    private val NAMESPACE_PATTERN = Regex("""useTranslations\(\s*"($KEY)"\s*\)""")

    /**
     * @param rootDir The repository root (see [GenerateNextI18nMessagesMain.resolveRootDir]).
     * @return Every key candidate the module names, including `<namespace>.<key>` for each namespace a file
     * declares. Never empty - an empty result means the module wasn't found, which the caller treats as an
     * error.
     */
    fun scan(rootDir: File): Set<String> {
        val keys = mutableSetOf<String>()
        forEachFile(rootDir) { content, fileKeys, namespaces ->
            keys.addAll(fileKeys)
            keys.addAll(namespaces)
            // The namespaces of this file, applied to all its keys as well. Pairing a t(…) call with the very
            // useTranslations it belongs to would need a parser; a next file holds one component and thus
            // normally one namespace, and an extra combination only costs a line in the catalog.
            namespaces.forEach { namespace ->
                fileKeys.forEach { key -> keys.add("$namespace.$key") }
            }
            CANDIDATE_PATTERN.findAll(content).forEach { keys.add(normalize(it.groupValues[1])) }
            val functions = translateFunctions(content)
            conditionalCallPattern(functions).findAll(content).forEach { call ->
                Regex(""""($KEY)"""").findAll(call.groupValues[1]).forEach { keys.add(normalize(it.groupValues[1])) }
            }
        }
        require(keys.isNotEmpty()) { "No i18n keys found in '$MODULE_DIR' — is the module present?" }
        return keys
    }

    /**
     * The keys the module asks for as `<key>._`, i.e. those it expects to be a namespace *and* a leaf in the
     * catalog. That shape only exists if the key has children ([GenerateNextI18nMessagesMain.JsonNode.put]
     * moves the leaf under `_` when a subtree claims the name), so the caller has to export the subtree as
     * well - otherwise the frontend asks for `fibu.kost1.title.list._` and finds a plain string at
     * `fibu.kost1.title.list`.
     *
     * @return The prefix of each such subtree, dot included (`fibu.kost1.title.list.`).
     */
    fun scanSubtreePrefixes(rootDir: File): Set<String> {
        val prefixes = mutableSetOf<String>()
        forEachFile(rootDir) { content, _, _ ->
            Regex(""""($KEY)\._"""").findAll(content).forEach { prefixes.add("${it.groupValues[1]}.") }
        }
        return prefixes
    }

    /**
     * The keys the module spells out as keys ([KEY_PATTERNS]) and that [isKnown] answers false for - neither
     * the key itself nor, since a `t("…")` inside a `useTranslations("N")` names `N.<key>`, any of its file's
     * namespaces in front of it. The catch-all candidates are left out, so the result holds only strings that
     * were meant to be keys and reads as the typo report it is.
     */
    fun scanUnknownKeys(rootDir: File, isKnown: (String) -> Boolean): Set<String> {
        val declared = mutableSetOf<String>()
        val namespaces = mutableSetOf<String>()
        forEachFile(rootDir) { _, fileKeys, fileNamespaces ->
            declared.addAll(fileKeys)
            namespaces.addAll(fileNamespaces)
        }
        // All namespaces of the module, not only those of the file the key was found in: a key is regularly
        // declared where the data is (the presets of components/data-table/history-interval-presets.ts) and
        // translated where the component is (`useTranslations("search")`), which is another file.
        return declared.filterNotTo(mutableSetOf()) { key ->
            isKnown(key) || namespaces.any { isKnown("$it.$key") }
        }
    }

    /**
     * Reads every source file once and hands the callback its content, the keys of [KEY_PATTERNS] and the
     * namespaces of its `useTranslations` calls.
     */
    private fun forEachFile(rootDir: File, callback: (String, Set<String>, Set<String>) -> Unit) {
        sourceFiles(rootDir).forEach { file ->
            val content = file.readText(ENCODING)
            val fileKeys = mutableSetOf<String>()
            keyPatterns(translateFunctions(content)).forEach { pattern ->
                Regex(pattern).findAll(content).forEach { match ->
                    fileKeys.add(normalize(match.groupValues[1]))
                }
            }
            val namespaces = NAMESPACE_PATTERN.findAll(content).map { normalize(it.groupValues[1]) }.toSet()
            callback(content, fileKeys, namespaces)
        }
    }

    private fun sourceFiles(rootDir: File): List<File> {
        return SOURCE_DIRS.map { File(rootDir, "$MODULE_DIR/$it") }
            .filter { it.isDirectory }
            .flatMap { SourcesUtils.listFiles(it, "ts", "tsx") }
    }

    /**
     * Drops the reserved `_` of the catalog: a key that is both a leaf and a namespace is written as
     * `<key>._` (see [GenerateNextI18nMessagesMain.JsonNode.put]), and the frontend asks for it that way -
     * spelled out in a page definition (`fibu.kost1.title.list._`) or appended at runtime by `labelKeyFor`.
     * The bundle knows the key without the suffix.
     */
    private fun normalize(key: String) = key.removeSuffix("._")
}
