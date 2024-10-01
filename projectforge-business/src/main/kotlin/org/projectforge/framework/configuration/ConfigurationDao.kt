/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.Configuration.Companion.instance
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

/**
 * Configuration values persistet in the data base. Please access the configuration parameters via
 * [Configuration].
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class ConfigurationDao : BaseDao<ConfigurationDO>(ConfigurationDO::class.java) {
    @JvmField
    @Autowired
    val applicationContext: ApplicationContext? = null

    @Autowired
    private lateinit var pfPersistenceService: PfPersistenceService

    /**
     * Force reload of the Configuration cache.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.afterSaveOrModify
     * @see Configuration.setExpired
     */
    override fun afterSaveOrModify(obj: ConfigurationDO, context: PfPersistenceContext) {
        instance.setExpired()
    }

    /**
     * Checks and creates missing database entries. Updates also out-dated descriptions.
     */
    fun checkAndUpdateDatabaseEntriesNewTrans() {
        return persistenceService.runInTransaction { context ->
            checkAndUpdateDatabaseEntries(context)
        }
    }

    /**
     * Checks and creates missing database entries. Updates also out-dated descriptions.
     */
    fun checkAndUpdateDatabaseEntries(context: PfPersistenceContext) {
        val list = internalLoadAll(context)
        val params: MutableSet<String?> = HashSet()
        for (param in ConfigurationParam.entries) {
            checkAndUpdateDatabaseEntry(param, list, params, context)
        }
        for (entry in list) {
            if (!params.contains(entry.parameter)) {
                log.error("Unknown configuration entry. Mark as deleted: " + entry.parameter)
                internalMarkAsDeleted(entry, context)
            }
        }
    }

    fun getEntry(param: IConfigurationParam): ConfigurationDO {
        return persistenceService.runReadOnly { context ->
            getEntry(param, context)
        }
    }

    fun getEntry(param: IConfigurationParam, context: PfPersistenceContext): ConfigurationDO {
        return context.selectNamedSingleResult(
            ConfigurationDO.FIND_BY_PARAMETER,
            ConfigurationDO::class.java,
            Pair("parameter", param.key),
            nullAllowed = false,
        )!!
    }

    fun getValue(parameter: IConfigurationParam): Any? {
        return getValue(parameter, getEntry(parameter))
    }

    fun getValue(parameter: IConfigurationParam, configurationDO: ConfigurationDO?): Any? {
        if (parameter.type.isIn(ConfigurationType.STRING, ConfigurationType.TEXT)) {
            if (configurationDO == null) {
                return parameter.defaultStringValue
            }
            val result = configurationDO.stringValue
            return result ?: parameter.defaultStringValue
        } else if (parameter.type.isIn(ConfigurationType.FLOAT, ConfigurationType.PERCENT)) {
            if (configurationDO == null) {
                return BigDecimal.ZERO
            }
            return configurationDO.floatValue
        } else if (parameter.type == ConfigurationType.LONG) {
            if (configurationDO == null) {
                return 0
            }
            return configurationDO.longValue
        } else if (parameter.type == ConfigurationType.BOOLEAN) {
            if (configurationDO == null) {
                return null
            }
            return configurationDO.booleanValue
        } else if (parameter.type == ConfigurationType.CALENDAR) {
            if (configurationDO == null) {
                return null
            }
            val calendarId = configurationDO.calendarId
            return calendarId
        } else if (parameter.type == ConfigurationType.TIME_ZONE) {
            var timezoneId = configurationDO?.timeZoneId
            if (timezoneId == null) {
                timezoneId = parameter.defaultStringValue
            }
            if (timezoneId != null) {
                return TimeZone.getTimeZone(timezoneId)
            }
            return null
        }
        throw UnsupportedOperationException("Type unsupported: " + parameter.type)
    }

    override fun hasAccess(
        user: PFUserDO, obj: ConfigurationDO?, oldObj: ConfigurationDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user, throwException)
    }

    override fun newInstance(): ConfigurationDO {
        throw UnsupportedOperationException()
    }

    private fun checkAndUpdateDatabaseEntry(
        param: IConfigurationParam,
        list: List<ConfigurationDO>,
        params: MutableSet<String?>,
        context: PfPersistenceContext,
    ) {
        params.add(param.key)

        // find the entry and update it
        for (configuration in list) {
            if (param.key == configuration.parameter) {
                var modified = false
                if (configuration.configurationType != param.type) {
                    log.info("Updating configuration type of configuration entry: $param")
                    configuration.internalSetConfigurationType(param.type)
                    modified = true
                }
                if (configuration.deleted) {
                    log.info("Restore deleted configuration entry: $param")
                    configuration.deleted = false
                    modified = true
                }
                if (modified) {
                    internalUpdate(configuration, context)
                }
                return
            }
        }

        // Entry does not exist: Create entry:
        log.info("Entry does not exist. Creating parameter '" + param.key + "'.")
        val configuration = ConfigurationDO()
        configuration.parameter = param.key
        configuration.configurationType = param.type
        if (param.type.isIn(ConfigurationType.STRING, ConfigurationType.TEXT)) {
            configuration.value = param.defaultStringValue
        }
        if (param.type.isIn(ConfigurationType.LONG)) {
            configuration.longValue = param.defaultLongValue
        }
        if (param.type.isIn(ConfigurationType.BOOLEAN)) {
            configuration.stringValue = param.defaultBooleanValue.toString()
        }
        internalSave(configuration, context)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ConfigurationDao::class.java)
    }
}
