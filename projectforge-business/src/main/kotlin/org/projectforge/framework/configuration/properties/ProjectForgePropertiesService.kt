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

import org.projectforge.business.configuration.ConfigurationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.stereotype.Service
import java.io.File
import java.time.ZoneId

/**
 * The properties editor (Administration → Properties editor): the known parameters of [PropertiesMetadata] with
 * the values of the admin's projectforge.properties and the values the running system uses. Changes take effect
 * only after a restart. Values of secrets ([PropertiesMetadata.isSecret]) never leave this service.
 */
@Service
class ProjectForgePropertiesService {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var environment: ConfigurableEnvironment

    class Data(
        val filePath: String,
        val fileExists: Boolean,
        val writable: Boolean,
        val fileVersion: String,
        val groups: List<GroupData>,
    )

    class GroupData(val id: String, val title: String, val expert: Boolean, val blocks: List<BlockData>)

    class BlockData(val description: String?, val params: List<ParamData>)

    class ParamData(
        val key: String,
        val description: String?,
        /** Null for optional parameters and secrets. */
        val defaultValue: String?,
        val example: String?,
        val optional: Boolean,
        val known: Boolean,
        val status: PropertiesMetadata.KeyStatus,
        val type: PropertiesMetadata.Type,
        val secret: Boolean,
        /** Whether the key is set (active) in projectforge.properties. */
        val inFile: Boolean,
        /** The value in projectforge.properties, null if not set or a secret. */
        val fileValue: String?,
        /** The admin's comment above the key in projectforge.properties (values of secrets masked). */
        val fileComment: String?,
        /** The value of the running system, null if not set or a secret. */
        val activeValue: String?,
        /** Where the active value comes from, if not from projectforge.properties or application.properties. */
        val activeSource: String?,
        /**
         * The running system uses another value than projectforge.properties (or application.properties) gives:
         * a restart is pending or the value is overridden, e.g. by an environment variable.
         */
        val activeDiffers: Boolean,
    )

    private val metadata: PropertiesMetadata
        get() = PropertiesMetadata.instance

    val propertiesFile: ProjectForgePropertiesFile
        get() = ProjectForgePropertiesFile(
            File(configurationService.applicationHomeDir, ProjectForgePropertiesFile.FILENAME),
            metadata,
        )

    fun read(): Data {
        val file = propertiesFile
        val snapshot = file.read()
        val comments = collectComments(snapshot.lines)
        val undocumented = snapshot.values.keys.filter { !metadata.contains(it) }.sorted()
            .groupBy { metadata.statusOf(it) }
        // Variants (other list indices, map entries) follow the documented key they belong to.
        val variantsByKey = undocumented[PropertiesMetadata.KeyStatus.VARIANT].orEmpty()
            .groupBy { metadata.findRelatedKey(it) }
        val groups = metadata.groups.map { group ->
            GroupData(group.id, group.title, group.expert, group.blocks.map { block ->
                BlockData(block.description, block.params.flatMap { param ->
                    listOf(toParamData(param.key, param, snapshot, comments[param.key])) +
                        variantsByKey[param.key].orEmpty().map { toParamData(it, null, snapshot, comments[it]) }
                })
            })
        }.toMutableList()
        listOf(
            OBSOLETE_GROUP_ID to PropertiesMetadata.KeyStatus.OBSOLETE,
            OTHER_GROUP_ID to PropertiesMetadata.KeyStatus.UNKNOWN,
        ).forEach { (groupId, status) ->
            // A variant without a documented key to follow (a map without an example) is shown as unknown.
            val keys = undocumented[status].orEmpty() +
                if (status == PropertiesMetadata.KeyStatus.UNKNOWN) variantsByKey[null].orEmpty() else emptyList()
            if (keys.isNotEmpty()) {
                // The title is translated by the client (by group id).
                groups.add(GroupData(groupId, groupId, false, listOf(
                    BlockData(null, keys.map { toParamData(it, null, snapshot, comments[it]) })
                )))
            }
        }
        return Data(file.file.absolutePath, snapshot.exists, snapshot.writable, snapshot.version, groups)
    }

    /**
     * @return The backup file.
     * @throws ProjectForgePropertiesFile.ConflictException if the file was modified since [fileVersion] was read.
     */
    fun save(changes: List<PropertyChange>, fileVersion: String): File? {
        changes.forEach {
            require(keyRegex.matches(it.key)) { "Invalid key: '${it.key}'." }
            require(it.remove || it.value != null) { "No value given for '${it.key}'." }
        }
        require(changes.map { it.key }.toSet().size == changes.size) { "Duplicate keys in changes." }
        return propertiesFile.save(changes, fileVersion)
    }

    /** A backup of projectforge.properties for the diff: [key] is its file name, [timestamp] in epoch millis. */
    class BackupData(val key: String, val timestamp: Long)

    /** The backups of projectforge.properties, newest first. */
    fun backups(): List<BackupData> = propertiesFile.backups().map { backup ->
        BackupData(backup.file.name, backup.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    /**
     * The text of projectforge.properties ([key] null) or of one of its backups for the diff ([PropertiesDiffText]),
     * values of secrets masked.
     */
    fun diffText(key: String?, ignoreOrder: Boolean): String {
        val file = propertiesFile
        val source = if (key.isNullOrEmpty()) {
            file.file
        } else {
            // Only a listed backup, never a path given by the client.
            file.backups().firstOrNull { it.file.name == key }?.file
                ?: throw IllegalArgumentException("Unknown backup: '$key'.")
        }
        val content = if (source.exists()) String(source.readBytes(), Charsets.ISO_8859_1) else ""
        return PropertiesDiffText.build(content, metadata, ignoreOrder)
    }

    private fun toParamData(
        key: String,
        param: PropertiesMetadata.Param?,
        snapshot: ProjectForgePropertiesFile.Snapshot,
        comment: String?,
    ): ParamData {
        val secret = param?.secret ?: PropertiesMetadata.isSecret(key)
        val fileValue = snapshot.values[key]
        val activeValue = runCatching { environment.getProperty(key) }.getOrNull()
        val configuredValue = fileValue ?: param?.defaultValue
        val expectedActive = configuredValue?.let { runCatching { environment.resolvePlaceholders(it) }.getOrDefault(it) }
        val activeSource = if (activeValue != null) findSource(key) else null
        return ParamData(
            key = key,
            description = param?.description,
            defaultValue = if (secret) null else param?.defaultValue,
            example = if (secret) null else param?.example,
            optional = param?.optional ?: true,
            known = param != null,
            status = metadata.statusOf(key),
            type = param?.type ?: PropertiesMetadata.typeOf(fileValue),
            secret = secret,
            inFile = fileValue != null,
            fileValue = if (secret) null else fileValue,
            fileComment = comment,
            activeValue = if (secret) null else activeValue,
            activeSource = activeSource,
            activeDiffers = (activeValue ?: "") != (expectedActive ?: ""),
        )
    }

    /** The name of the property source the running value comes from, null for the configuration files. */
    private fun findSource(key: String): String? {
        val source = environment.propertySources.firstOrNull {
            it.name != "configurationProperties" && runCatching { it.containsProperty(key) }.getOrDefault(false)
        } ?: return null
        val name = source.name
        return when {
            name.contains(ProjectForgePropertiesFile.FILENAME) || name.contains("application.properties") -> null
            name == "systemEnvironment" -> "environment variable"
            name == "systemProperties" -> "system property"
            name == "commandLineArgs" -> "command line"
            else -> name
        }
    }

    /** The admin's comment lines above each key, commented-out values of secrets masked. */
    private fun collectComments(lines: List<PropertiesFileLine>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pending = mutableListOf<String>()
        lines.forEach { line ->
            when (line) {
                is PropertiesFileLine.Blank -> pending.clear()
                is PropertiesFileLine.Comment -> if (line.groupTitle == null) pending.add(line.text)
                is PropertiesFileLine.Entry -> {
                    if (line.commentedOut) {
                        pending.add(PropertiesFileParser.formatEntry(line.key, if (PropertiesMetadata.isSecret(line.key)) SECRET_MASK else line.value))
                    } else {
                        if (pending.isNotEmpty()) {
                            result[line.key] = pending.joinToString("\n")
                        }
                        pending.clear()
                    }
                }
            }
        }
        return result
    }

    companion object {
        /** Keys of projectforge.properties unknown to ProjectForge, e.g. own Spring Boot settings. */
        const val OTHER_GROUP_ID = "other"

        /** Keys in ProjectForge's namespace unknown to application.properties: no longer used (or typos). */
        const val OBSOLETE_GROUP_ID = "obsolete"

        private const val SECRET_MASK = "******"

        private val keyRegex = Regex("""^[A-Za-z0-9][A-Za-z0-9._\-\[\]]*$""")
    }
}
