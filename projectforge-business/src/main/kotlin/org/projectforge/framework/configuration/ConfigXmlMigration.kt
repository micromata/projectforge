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


package org.projectforge.framework.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.common.logging.error
import org.projectforge.common.logging.warn
import org.projectforge.framework.configuration.ApplicationDirectories.Dir
import org.projectforge.framework.configuration.properties.ProjectForgePropertiesFile
import org.projectforge.framework.configuration.properties.PropertiesMetadata
import org.projectforge.framework.configuration.properties.PropertyChange
import org.projectforge.framework.json.JsonUtils
import org.projectforge.jira.JiraConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Takes over the former config.xml of the base dir once:
 * - its holidays, jira servers, contract types and accounting settings into the configuration parameters
 *   ([ConfigurationParam.HOLIDAYS], [ConfigurationParam.JIRA], [ConfigurationParam.CONTRACT_TYPES],
 *   [ConfigurationParam.FIBU_ACCOUNTING]) still empty: as initial value of a parameter's new row
 *   ([ConfigurationDao]), or on startup ([migrate]) for a row without value,
 * - its directories differing from the defaults into the properties `projectforge.dir.*` not given
 *   ([takeOverDirectories], on startup of [org.projectforge.business.configuration.ConfigurationService]).
 *
 * Afterwards config.xml is renamed to config.xml.migrated, so it isn't applied again (e.g. to a parameter cleared on
 * purpose). If anything fails, the file is kept and the take-over is retried on the next start
 * ([ConfigurationLogEvents.CONFIG_XML_NOT_MIGRATED]).
 */
@Service
open class ConfigXmlMigration {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var systemStatus: SystemStatus

    /**
     * Fills the parameters without value and renames config.xml if everything was taken over. Skipped on an empty
     * database: the setup creates the rows (with the values of config.xml) and the file is renamed on the next start.
     */
    @EventListener(ApplicationReadyEvent::class)
    open fun migrate() {
        val legacy = legacyConfig ?: return
        if (systemStatus.setupRequiredFirst == true) {
            return
        }
        try {
            var complete = readFailure == null && !directoriesFailed
            configurationDao.checkAndUpdateDatabaseEntries()
            var modified = false
            jsonValues(legacy).forEach { (param, json) ->
                val entry = configurationDao.getEntry(param)
                if (entry == null) {
                    complete = false
                    return@forEach
                }
                if (entry.stringValue.isNullOrBlank()) {
                    entry.stringValue = json
                    configurationDao.update(entry, checkAccess = false)
                    logTakeOver(param)
                    modified = true
                }
            }
            if (modified) {
                Configuration.instance.forceReload()
            }
            if (complete) {
                markMigrated()
            } else {
                log.warn(ConfigurationLogEvents.CONFIG_XML_NOT_MIGRATED) {
                    "${configFile?.absolutePath} not taken over completely, kept for the next start" +
                            (readFailure?.let { ": $it" } ?: ".")
                }
            }
        } catch (ex: Exception) {
            log.error(ConfigurationLogEvents.CONFIG_XML_NOT_MIGRATED, ex) {
                "Couldn't take over ${configFile?.absolutePath}: ${ex.message}"
            }
        }
    }

    companion object {
        const val MIGRATED_SUFFIX = ".migrated"

        /** config.xml in the base dir, null if not given (or not readable). */
        private var configFile: File? = null

        /** The content of config.xml, null if not given. */
        private var legacyConfig: LegacyConfigXml? = null

        private var readFailure: String? = null

        private var directoriesFailed = false

        /** Reads config.xml of the given base dir, if exists. Called on startup by ConfigurationService. */
        @JvmStatic
        fun init(applicationHomeDir: String) {
            val file = File(applicationHomeDir, LegacyConfigXml.CONFIG_XML_FILE)
            configFile = null
            legacyConfig = null
            readFailure = null
            directoriesFailed = false
            if (!file.exists()) {
                return
            }
            configFile = file
            legacyConfig = try {
                LegacyConfigXml.read(file.readText()).also {
                    log.info { "${file.absolutePath} found: taking it over into configuration parameters and properties." }
                }
            } catch (ex: Exception) {
                readFailure = "${file.absolutePath} not readable: ${ex.message}"
                log.error(ConfigurationLogEvents.CONFIG_XML_NOT_MIGRATED, ex) { readFailure }
                null
            }
        }

        /** The value of config.xml for the new row of the given parameter, null if none is given there. */
        @JvmStatic
        fun getJsonValue(param: IConfigurationParam): String? {
            val legacy = legacyConfig ?: return null
            return (param as? ConfigurationParam)?.let { jsonValues(legacy)[it] }
        }

        internal fun logTakeOver(param: ConfigurationParam) {
            log.info {
                "Configuration of config.xml taken over into the configuration parameter '${param.key}', which is " +
                        "maintained on the configuration page from now on."
            }
        }

        /**
         * The directories given by config.xml for the properties not given: they apply and are written to
         * projectforge.properties.
         * @param properties The values of the properties `projectforge.dir.*`.
         * @return The values to use (property, else config.xml, else null for the default).
         */
        @JvmStatic
        fun takeOverDirectories(applicationHomeDir: String, properties: Map<Dir, String?>): Map<Dir, String?> {
            val result = properties.toMutableMap()
            val legacy = legacyConfig ?: return result
            val changes = directoryValues(legacy).filter { (dir, _) -> properties[dir].isNullOrBlank() }
                .map { (dir, value) ->
                    result[dir] = value
                    PropertyChange(dir.property, value)
                }
            if (changes.isEmpty()) {
                return result
            }
            try {
                val file = ProjectForgePropertiesFile(
                    File(applicationHomeDir, ProjectForgePropertiesFile.FILENAME),
                    PropertiesMetadata.instance,
                )
                file.save(changes, file.read().version)
                changes.forEach {
                    log.info { "Directory of config.xml taken over into ${file.file.absolutePath}: ${it.key}=${it.value}" }
                }
            } catch (ex: Exception) {
                directoriesFailed = true
                log.error(ConfigurationLogEvents.CONFIG_XML_NOT_MIGRATED, ex) {
                    "Couldn't write the directories of config.xml to projectforge.properties (used for this start " +
                            "only): ${changes.joinToString { "${it.key}=${it.value}" }}: ${ex.message}"
                }
            }
            return result
        }

        /** The sections of config.xml given, as JSON values of their parameters. */
        internal fun jsonValues(legacy: LegacyConfigXml): Map<ConfigurationParam, String> {
            val result = mutableMapOf<ConfigurationParam, String>()
            legacy.holidays?.takeIf { it.isNotEmpty() }?.let {
                result[ConfigurationParam.HOLIDAYS] = JsonUtils.toJson(it, ignoreNullableProps = true)
            }
            if (!legacy.jiraBrowseBaseUrl.isNullOrBlank() || !legacy.jiraServers.isNullOrEmpty()) {
                val jira = JiraConfig(legacy.jiraBrowseBaseUrl?.trim()?.ifEmpty { null }, legacy.jiraServers?.ifEmpty { null })
                result[ConfigurationParam.JIRA] = JsonUtils.toJson(jira, ignoreNullableProps = true)
            }
            legacy.contractTypes?.takeIf { it.isNotEmpty() }?.let {
                result[ConfigurationParam.CONTRACT_TYPES] = JsonUtils.toJson(it, ignoreNullableProps = true)
            }
            legacy.accountingConfig?.let { JsonUtils.toJson(it, ignoreNullableProps = true) }
                ?.takeIf { it != "{}" }
                ?.let { result[ConfigurationParam.FIBU_ACCOUNTING] = it }
            return result
        }

        /** The directories of config.xml differing from the defaults. */
        internal fun directoryValues(legacy: LegacyConfigXml): Map<Dir, String> {
            return mapOf(
                Dir.WORK to legacy.workingDirectory,
                Dir.TMP to legacy.tempDirectory,
                Dir.BACKUP to legacy.backupDirectory,
            ).mapNotNull { (dir, value) ->
                value?.trim()?.takeIf { it.isNotEmpty() && it != dir.defaultName }?.let { dir to it }
            }.toMap()
        }

        internal fun markMigrated() {
            val file = configFile ?: return
            val dest = File(file.parentFile, file.name + MIGRATED_SUFFIX).let { dest ->
                if (dest.exists()) File(file.parentFile, "${file.name}$MIGRATED_SUFFIX-${System.currentTimeMillis()}") else dest
            }
            if (file.renameTo(dest)) {
                log.info { "${file.absolutePath} taken over completely, renamed to ${dest.name}. It isn't used anymore." }
                configFile = null
                legacyConfig = null
            } else {
                log.warn(ConfigurationLogEvents.CONFIG_XML_NOT_MIGRATED) {
                    "${file.absolutePath} taken over completely, but couldn't be renamed to ${dest.name}. Please " +
                            "remove it, otherwise its values are taken over again into the parameters left empty."
                }
            }
        }
    }
}
