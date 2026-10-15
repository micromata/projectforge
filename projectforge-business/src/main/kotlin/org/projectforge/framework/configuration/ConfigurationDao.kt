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

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.logging.PfLog
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.Configuration.Companion.instance
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.persistence.api.BaseDao
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
 * @author Kai Reinhard
 */
@Service
open class ConfigurationDao : BaseDao<ConfigurationDO>(ConfigurationDO::class.java) {
    @JvmField
    @Autowired
    val applicationContext: ApplicationContext? = null

    /**
     * Force reload of the Configuration cache.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.afterInsertOrModify
     * @see Configuration.setExpired
     */
    override fun afterInsertOrModify(obj: ConfigurationDO, operationType: OperationType) {
        instance.setExpired()
    }

    /**
     * Checks and creates missing database entries. Updates also out-dated descriptions.
     */
    fun checkAndUpdateDatabaseEntries() {
        persistenceService.runInTransaction { _ ->
            val list = selectAll(checkAccess = false)
            val params: MutableSet<String?> = HashSet()
            for (param in ConfigurationParam.entries) {
                checkAndUpdateDatabaseEntry(param, list, params)
            }
            for (entry in list) {
                // selectAll returns the deleted rows too, so guard on !deleted: an obsolete parameter is
                // marked (and logged) once on the transition, not again on every list load that re-runs this.
                if (!params.contains(entry.parameter) && !entry.deleted) {
                    PfLog.warn(log, ConfigurationLogEvents.UNKNOWN_ENTRY, "Unknown configuration entry. Mark as deleted: " + entry.parameter)
                    markAsDeleted(entry, checkAccess = false)
                }
            }
        }
    }

    fun getEntry(param: IConfigurationParam): ConfigurationDO? {
        return persistenceService.selectNamedSingleResult(
            ConfigurationDO.FIND_BY_PARAMETER,
            ConfigurationDO::class.java,
            Pair("parameter", param.key),
            nullAllowed = true,
        )
    }

    fun getValue(parameter: IConfigurationParam): Any? {
        return getValue(parameter, getEntry(parameter))
    }

    fun getValue(parameter: IConfigurationParam, configurationDO: ConfigurationDO?): Any? {
        if (parameter.type.isIn(ConfigurationType.STRING, ConfigurationType.TEXT, ConfigurationType.JSON)) {
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

    /**
     * The set of configuration parameters is fixed (see [checkAndUpdateDatabaseEntries] and
     * [newInstance], which throws): a parameter is only ever read or updated through the UI, never inserted
     * or deleted. INSERT and DELETE are therefore denied even for an admin, so the migrated next page
     * ([org.projectforge.rest.ConfigurationEntityRest]) offers no add and no delete and both are refused
     * server-side too. [checkAndUpdateDatabaseEntries] is unaffected: it runs with `checkAccess = false`,
     * bypassing this method.
     *
     * SELECT and UPDATE depend on who maintains the parameter ([ConfigurationParam.getEditors]):
     * - An admin sees every parameter except the [ConfigurationEditors.CONTROLLING] ones, but changes only the
     *   [ConfigurationEditors.ADMIN] ones.
     * - A member of PF_Finance or PF_Controlling sees and changes only the [ConfigurationEditors.FINANCE] ones.
     * - A member of PF_Finance or PF_Controlling sees the [ConfigurationEditors.CONTROLLING] ones, but only a member
     *   of both groups changes them.
     * - A parameter with a page of its own ([ConfigurationParam.getEditPage]) is never updated through this
     *   check: that page validates its structured value and saves it itself, after checking the editors.
     *
     * Without an object (the list as a whole), SELECT is granted to both sides; the rows are then filtered
     * per parameter.
     */
    override fun hasAccess(
        user: PFUserDO, obj: ConfigurationDO?, oldObj: ConfigurationDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        val allowed = when (operationType) {
            OperationType.INSERT, OperationType.DELETE, OperationType.UNDELETE -> false
            OperationType.SELECT -> {
                if (obj == null) {
                    accessChecker.isUserMemberOfAdminGroup(user) || isFinanceUser(user)
                } else {
                    val editors = paramOf(obj)?.editors ?: ConfigurationEditors.ADMIN
                    accessChecker.isUserMemberOfGroup(user, *editors.viewers.toTypedArray())
                }
            }

            OperationType.UPDATE -> {
                val param = paramOf(oldObj ?: obj)
                param != null && param.editPage == null && isEditor(user, param)
            }
        }
        if (!allowed && throwException) {
            throw AccessException(user, "access.exception.noAccess")
        }
        return allowed
    }

    /**
     * Whether the user maintains the given parameter ([ConfigurationParam.getEditors]). Also asked by the
     * pages a parameter has of its own ([ConfigurationParam.getEditPage]).
     */
    fun isEditor(user: PFUserDO, param: ConfigurationParam): Boolean {
        val editors = param.editors
        return if (editors.allEditorGroupsRequired) {
            editors.editors.all { accessChecker.isUserMemberOfGroup(user, it) }
        } else {
            accessChecker.isUserMemberOfGroup(user, *editors.editors.toTypedArray())
        }
    }

    private fun isFinanceUser(user: PFUserDO): Boolean {
        return accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)
    }

    private fun paramOf(obj: ConfigurationDO?): ConfigurationParam? {
        return obj?.parameter?.let { ConfigurationParam.ofKey(it) }
    }

    /**
     * The add button of the next list ([org.projectforge.rest.ConfigurationEntityRest]) routes through
     * [BaseDao.hasInsertAccess], which bypasses [hasAccess] and would otherwise grant an admin insert via
     * `@AUserRightId("ADMIN_CORE")`. The parameter set is fixed, so insert is always denied.
     */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return false
    }

    override fun newInstance(): ConfigurationDO {
        throw UnsupportedOperationException()
    }

    private fun checkAndUpdateDatabaseEntry(
        param: IConfigurationParam,
        list: List<ConfigurationDO>,
        params: MutableSet<String?>,
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
                    update(configuration, checkAccess = false)
                }
                return
            }
        }

        // Entry does not exist: Create entry:
        log.info("Entry does not exist. Creating parameter '" + param.key + "'.")
        val configuration = ConfigurationDO()
        configuration.parameter = param.key
        configuration.configurationType = param.type
        if (param.type.isIn(ConfigurationType.STRING, ConfigurationType.TEXT, ConfigurationType.JSON)) {
            configuration.value = param.defaultStringValue
        }
        // A LONG parameter without a default (0) starts empty rather than with 0, e.g. a task id not yet configured.
        if (param.type.isIn(ConfigurationType.LONG) && param.defaultLongValue != 0L) {
            configuration.longValue = param.defaultLongValue
        }
        if (param.type.isIn(ConfigurationType.BOOLEAN)) {
            configuration.stringValue = param.defaultBooleanValue.toString()
        }
        if (param is ConfigurationParam) {
            param.legacyProperty?.let { property ->
                takeOverLegacyProperty(configuration, param, applicationContext?.environment?.getProperty(property))
            }
        }
        insert(configuration, checkAccess = false)
    }

    /**
     * Sets the value of the property the new parameter was configured by before ([ConfigurationParam.getLegacyProperty])
     * as its initial value. Only on creating the row: afterwards the parameter alone applies, so a value cleared on the
     * configuration page isn't taken over again.
     *
     * @return true if taken over.
     */
    internal fun takeOverLegacyProperty(configuration: ConfigurationDO, param: ConfigurationParam, value: String?): Boolean {
        val property = param.legacyProperty ?: return false
        val trimmed = value?.trim()?.ifEmpty { null } ?: return false
        when (param.type) {
            ConfigurationType.LONG -> {
                val longValue = trimmed.toLongOrNull()
                if (longValue == null) {
                    log.warn("$property='$trimmed' is no number, not taken over into the configuration parameter '${param.key}'.")
                    return false
                }
                configuration.longValue = longValue
            }

            ConfigurationType.STRING -> configuration.stringValue = trimmed
            else -> throw UnsupportedOperationException("Taking over a property of type ${param.type} isn't supported.")
        }
        log.info(
            "$property='$trimmed' taken over into the new configuration parameter '${param.key}', which is maintained on " +
                    "the configuration page from now on. The property is ignored and may be removed from projectforge.properties."
        )
        return true
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ConfigurationDao::class.java)
    }
}
