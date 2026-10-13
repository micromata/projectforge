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
import org.projectforge.common.logging.warn
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Properties of projectforge.properties replaced by configuration parameters ([ConfigurationParam.getLegacyProperty]):
 * a property is taken over once, as initial value of its parameter when the parameter's row is created
 * ([ConfigurationDao.takeOverLegacyProperty]). Afterwards it is ignored, so a property still set is reported on every
 * start ([ConfigurationLogEvents.LEGACY_PROPERTY_SET], a problem of the system dashboard): it should be removed.
 */
@Service
open class ConfigurationLegacyProperties {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var environment: Environment

    /**
     * Creates the missing rows of the parameters on startup, so properties of former installations are taken over
     * right away, and reports the properties still set. Failures are logged only: startup goes on.
     */
    @EventListener(ApplicationReadyEvent::class)
    open fun takeOverAndCheck() {
        try {
            configurationDao.checkAndUpdateDatabaseEntries()
            ConfigurationParam.entries.forEach { param ->
                val property = param.legacyProperty ?: return@forEach
                val message = legacyPropertyWarning(param, environment.getProperty(property), configurationDao.getEntry(param))
                    ?: return@forEach
                log.warn(ConfigurationLogEvents.LEGACY_PROPERTY_SET) { message }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Couldn't check the properties replaced by configuration parameters: ${ex.message}" }
        }
    }

    companion object {
        /**
         * @param propertyValue The value of the [ConfigurationParam.getLegacyProperty] in projectforge.properties.
         * @param entry The row of the parameter, null if it doesn't exist (yet).
         * @return The warning if the property is set (not blank), else null.
         */
        internal fun legacyPropertyWarning(param: ConfigurationParam, propertyValue: String?, entry: ConfigurationDO?): String? {
            val property = param.legacyProperty ?: return null
            val value = propertyValue?.trim()?.ifEmpty { null } ?: return null
            val paramValue = entry?.let { valueOf(param, it) }
            val state = if (paramValue == value) {
                "it was taken over into the configuration parameter '${param.key}', which applies alone since then"
            } else {
                "the configuration parameter '${param.key}' applies (currently '${paramValue ?: ""}')"
            }
            return "Property $property='$value' is ignored: $state. Please remove it from projectforge.properties."
        }

        /** The value as string, in the form of the property (types of [ConfigurationDao.takeOverLegacyProperty]). */
        private fun valueOf(param: ConfigurationParam, entry: ConfigurationDO): String? {
            val value = when (param.type) {
                ConfigurationType.LONG -> entry.longValue?.toString()
                else -> entry.stringValue
            }
            return value?.trim()?.ifEmpty { null }
        }
    }
}
