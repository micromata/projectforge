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
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.warn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Properties of projectforge.properties renamed or removed by an update. A renamed property is still read (its new key
 * defaults to the former one in application.properties, e.g. `projectforge.files.maxDefaultFileSize=${projectforge.jcr.maxDefaultFileSize:100MB}`),
 * a removed one has no effect (the keys of [LEGACY_JCR_GUARD] are still read by the start-up guard). Both are reported on every start ([ConfigurationLogEvents.RENAMED_PROPERTY_SET],
 * [ConfigurationLogEvents.OBSOLETE_PROPERTY_SET], problems of the system dashboard), as long as they are set.
 */
@Service
open class ConfigurationRenamedProperties {
    @Autowired
    private lateinit var environment: Environment

    @EventListener(ApplicationReadyEvent::class)
    open fun check() {
        try {
            warnings { key -> environment.getProperty(key) }.forEach { (event, message) ->
                log.warn(event) { message }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Couldn't check the renamed and obsolete properties: ${ex.message}" }
        }
    }

    companion object {
        /** Former key to new key. The new key has to default to the former one in application.properties. */
        internal val RENAMED = mapOf(
            "projectforge.jcr.maxDefaultFileSize" to "projectforge.files.maxDefaultFileSize",
            "projectforge.jcr.cron.sanityCheck" to "projectforge.files.cron.sanityCheck",
        )

        /** Removed keys. */
        internal val OBSOLETE = listOf(
            "projectforge.files.store",
            "projectforge.jcr.backupDir",
            "projectforge.jcr.cron.backup",
            "projectforge.jcr.cron.purgeBackupKeepDailyBackups",
            "projectforge.jcr.cron.purgeBackupKeepWeeklyBackups",
            "oak.datasource.username",
            "oak.datasource.password",
            "oak.datasource.driver-class-name",
            "oak.datasource.hikari.maximum-pool-size",
            "oak.datasource.hikari.minimum-idle",
            "oak.datasource.hikari.connection-timeout",
            "oak.datasource.hikari.idle-timeout",
        )

        /**
         * Keys of the former JCR, which are only read by the start-up guard for files of the JCR, which weren't migrated
         * (LegacyJcrGuard). Removing them before the JCR is deleted would hide a JCR at a custom location from the guard.
         */
        internal val LEGACY_JCR_GUARD = listOf("projectforge.dir.jcr", "oak.datasource.url")

        /**
         * @param lookup The value of a property, null if not set.
         * @return The warnings for all renamed and obsolete properties still set (not blank).
         */
        internal fun warnings(lookup: (String) -> String?): List<Pair<LogEvent, String>> {
            fun valueOf(key: String) = lookup(key)?.trim()?.ifEmpty { null }
            val result = mutableListOf<Pair<LogEvent, String>>()
            RENAMED.forEach { (former, new) ->
                val value = valueOf(former) ?: return@forEach
                // The new key defaults to the former one, so it is given in both cases: set explicitly, if it differs.
                val newValue = valueOf(new)
                val state = if (newValue == null || newValue == value) {
                    "still read"
                } else {
                    "ignored, because $new='$newValue' is given"
                }
                result.add(
                    ConfigurationLogEvents.RENAMED_PROPERTY_SET to
                            "Property $former='$value' was renamed to $new ($state). Please rename it in projectforge.properties."
                )
            }
            OBSOLETE.forEach { key ->
                valueOf(key) ?: return@forEach
                result.add(
                    ConfigurationLogEvents.OBSOLETE_PROPERTY_SET to
                            "Property $key has no effect any more. Please remove it from projectforge.properties."
                )
            }
            LEGACY_JCR_GUARD.forEach { key ->
                valueOf(key) ?: return@forEach
                result.add(
                    ConfigurationLogEvents.OBSOLETE_PROPERTY_SET to
                            "Property $key is only used by the start-up check for files of the former JCR, which " +
                            "weren't migrated (LegacyJcrGuard). Please remove it from projectforge.properties after " +
                            "deleting the former JCR, not before."
                )
            }
            return result
        }
    }
}
