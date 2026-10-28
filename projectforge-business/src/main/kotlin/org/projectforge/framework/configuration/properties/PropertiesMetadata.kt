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

package org.projectforge.framework.configuration.properties

/**
 * The known parameters of ProjectForge's configuration, grouped by topic: parsed from the bundled
 * `application.properties`, whose format is described in its header (`### Group`, blocks, commented-out optional
 * parameters, `@map` prefixes).
 */
class PropertiesMetadata(
    val groups: List<Group>,
    /** Prefixes declared by `@map <prefix>`: every key `<prefix>.<name>` is valid (e.g. attribute mappings). */
    val mapPrefixes: List<String> = emptyList(),
) {
    enum class Type { STRING, BOOLEAN }

    /** What ProjectForge knows about a key of projectforge.properties, see [statusOf]. */
    enum class KeyStatus {
        /** Documented in application.properties. */
        KNOWN,

        /** Another index of a documented list key (`bankAccounts[2].iban`) or an entry of a declared map. */
        VARIANT,

        /** Not documented, in ProjectForge's own namespace (`projectforge.`, `pf.`): no longer used, or a typo. */
        OBSOLETE,

        /** Not documented, e.g. a property of Spring Boot or of a library. */
        UNKNOWN,
    }

    class Group(
        /** Stable id derived from the title, e.g. `server-web`. */
        val id: String,
        val title: String,
        /** Internal settings (`### Title [expert]`), shown collapsed and with a warning. */
        val expert: Boolean,
        val blocks: List<Block>,
    ) {
        val params: List<Param>
            get() = blocks.flatMap { it.params }
    }

    /** Lines without a blank line between them. [description] applies to all params of the block. */
    class Block(val description: String?, val params: List<Param>)

    class Param(
        val key: String,
        /** The default value, null for an optional (commented-out) parameter. */
        val defaultValue: String?,
        /** The example value of an optional parameter. */
        val example: String?,
        val description: String?,
        val type: Type,
        val secret: Boolean,
    ) {
        val optional: Boolean
            get() = defaultValue == null
    }

    private val paramByKey: Map<String, Param> = groups.flatMap { it.params }.associateBy { it.key }

    /** Position of every key in the metadata order (group by group). */
    private val positionByKey: Map<String, Int> =
        groups.flatMap { it.params }.mapIndexed { index, param -> param.key to index }.toMap()

    private val groupByKey: Map<String, Group> =
        groups.flatMap { group -> group.params.map { it.key to group } }.toMap()

    fun getParam(key: String): Param? = paramByKey[key]

    fun contains(key: String): Boolean = paramByKey.containsKey(key)

    fun getGroup(key: String): Group? = groupByKey[key]

    fun getPosition(key: String): Int? = positionByKey[key]

    fun statusOf(key: String): KeyStatus = when {
        contains(key) -> KeyStatus.KNOWN
        findIndexVariant(key) != null || findMapPrefix(key) != null -> KeyStatus.VARIANT
        OWN_NAMESPACES.any { key.startsWith(it) } -> KeyStatus.OBSOLETE
        else -> KeyStatus.UNKNOWN
    }

    /** The documented key that [key] is another index of, e.g. `bankAccounts[2].iban` → `bankAccounts[0].iban`. */
    private fun findIndexVariant(key: String): String? {
        val withoutIndices = key.replace(indexRegex, "[]")
        return paramByKey.keys.lastOrNull { it.replace(indexRegex, "[]") == withoutIndices }
    }

    private fun findMapPrefix(key: String): String? = mapPrefixes.firstOrNull { key.startsWith("$it.") }

    /**
     * The known key an unknown key belongs to, for sorting it into the right group, e.g.
     * `projectforge.einvoice.seller.bankAccounts[2].iban` → `projectforge.einvoice.seller.bankAccounts[0].iban` (same key
     * without indices), or the last known key with the longest common prefix of at least three segments
     * (`projectforge.einvoice.seller.foo` → `projectforge.einvoice.seller.contactName`).
     */
    fun findRelatedKey(key: String): String? {
        if (contains(key)) {
            return key
        }
        findIndexVariant(key)?.let { return it }
        findMapPrefix(key)?.let { prefix -> paramByKey.keys.lastOrNull { it.startsWith("$prefix.") }?.let { return it } }
        val segments = key.split('.')
        for (length in segments.size - 1 downTo 3) {
            val prefix = segments.take(length).joinToString(".") + "."
            paramByKey.keys.lastOrNull { it.startsWith(prefix) }?.let { return it }
        }
        return null
    }

    companion object {
        private val indexRegex = Regex("""\[\d+]""")

        private const val EXPERT_MARKER = "[expert]"

        private const val MAP_MARKER = "@map "

        private val OWN_NAMESPACES = listOf("projectforge.", "pf.")

        /**
         * Last segment of keys whose values must never leave the server (e.g. `managerPassword`, `apiKey`). Any
         * `…key` counts (`receiveSmsKey`, `fileSystemEncryptionKey`, `licensemanagement.key`): better a value hidden
         * too many than a credential shown.
         */
        private val secretRegex =
            Regex("(password|secret|token|pepper|key|passphrase)$", RegexOption.IGNORE_CASE)

        fun isSecret(key: String): Boolean = secretRegex.containsMatchIn(key.substringAfterLast('.'))

        /** A value given only by another key with a default, e.g. `${projectforge.jcr.maxDefaultFileSize:100MB}`. */
        private val fallbackRegex = Regex("""^\$\{[^:{}]+:([^{}]*)}$""")

        /**
         * The default shown: the default of a value given only by another key (a renamed key, which is still read), the
         * value otherwise.
         */
        internal fun defaultOf(value: String): String = fallbackRegex.matchEntire(value.trim())?.groupValues?.get(1) ?: value

        fun typeOf(value: String?): Type =
            if (value != null && (value.equals("true", true) || value.equals("false", true))) Type.BOOLEAN else Type.STRING

        /** The parsed bundled `application.properties`. */
        val instance: PropertiesMetadata by lazy {
            val content = PropertiesMetadata::class.java.getResourceAsStream("/application.properties")
                ?.use { String(it.readBytes(), Charsets.ISO_8859_1) }
                ?: throw IllegalStateException("application.properties not found in the class path.")
            parse(content)
        }

        fun parse(content: String): PropertiesMetadata {
            val groups = mutableListOf<Group>()
            val mapPrefixes = mutableListOf<String>()
            var groupTitle: String? = null
            var groupBlocks = mutableListOf<Block>()
            // State of the current block:
            var blockDescription: String? = null
            var blockParams = mutableListOf<Param>()
            val pendingComments = mutableListOf<String>()

            fun finishBlock() {
                if (blockParams.size == 1 && blockParams[0].description == null && blockDescription != null) {
                    // A block of one key: the block's description is the key's one.
                    val p = blockParams[0]
                    blockParams[0] = Param(p.key, p.defaultValue, p.example, blockDescription, p.type, p.secret)
                    blockDescription = null
                }
                if (blockParams.isNotEmpty()) {
                    groupBlocks.add(Block(blockDescription, blockParams))
                }
                blockDescription = null
                blockParams = mutableListOf()
                pendingComments.clear()
            }

            fun finishGroup() {
                finishBlock()
                val title = groupTitle
                if (title != null && groupBlocks.isNotEmpty()) {
                    val expert = title.endsWith(EXPERT_MARKER)
                    val cleanTitle = title.removeSuffix(EXPERT_MARKER).trim()
                    groups.add(Group(toId(cleanTitle), cleanTitle, expert, groupBlocks))
                }
                groupBlocks = mutableListOf()
            }

            PropertiesFileParser.parse(content).forEach { line ->
                when (line) {
                    is PropertiesFileLine.Blank -> finishBlock()
                    is PropertiesFileLine.Comment -> {
                        val title = line.groupTitle
                        if (title != null) {
                            finishGroup()
                            groupTitle = title
                        } else if (line.text.startsWith(MAP_MARKER)) {
                            mapPrefixes.add(line.text.removePrefix(MAP_MARKER).trim())
                        } else {
                            pendingComments.add(line.text)
                        }
                    }

                    is PropertiesFileLine.Entry -> {
                        if (groupTitle == null) {
                            return@forEach // The file's header (and keys before the first group) are ignored.
                        }
                        val comment = pendingComments.joinToString("\n").trimEnd().ifEmpty { null }
                        pendingComments.clear()
                        val description = if (blockParams.isEmpty() && blockDescription == null) {
                            blockDescription = comment
                            null
                        } else {
                            comment
                        }
                        val defaultValue = if (line.commentedOut) null else defaultOf(line.value)
                        val example = if (line.commentedOut) line.value else null
                        blockParams.add(
                            Param(
                                key = line.key,
                                defaultValue = defaultValue,
                                example = example,
                                description = description,
                                type = typeOf(defaultValue ?: example),
                                secret = isSecret(line.key),
                            )
                        )
                    }
                }
            }
            finishGroup()
            return PropertiesMetadata(groups, mapPrefixes)
        }

        private fun toId(title: String): String =
            title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }
}
