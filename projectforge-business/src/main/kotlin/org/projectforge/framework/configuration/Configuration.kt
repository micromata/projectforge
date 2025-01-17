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

package org.projectforge.framework.configuration

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.time.DateFormats
import java.math.BigDecimal
import java.util.*


private val log = KotlinLogging.logger {}

/**
 * This class also provides the configuration of the parameters which are stored via ConfigurationDao. Those parameters
 * are cached. <br></br>
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
class Configuration(
    private val configurationService: ConfigurationService,
    /**
     * Only null for test cases. Used for running refresh in isolated transaction.
     */
    private val persistenceService: PfPersistenceService?,
) : AbstractCache(TICKS_PER_HOUR) {
    private var configurationParamMap: MutableMap<ConfigurationParam, Any?>? = null

    val defaultTimeZone
        get() = getValue(ConfigurationParam.DEFAULT_TIMEZONE) as TimeZone? ?: TimeZone.getDefault()

    /**
     * @return The first entry of [.getDateFormats] if exists, otherwise yyyy-MM-dd (ISO date format).
     */
    val defaultDateFormat: String
        get() {
            val sa: Array<String> = dateFormats
            return if (sa.size > 0) {
                sa[0]
            } else {
                DateFormats.ISO_DATE
            }
        }

    /**
     * Available date formats (configurable as parameter, see web dialogue with system parameters).
     *
     * @return
     */
    val dateFormats: Array<String>
        get() {
            val str = getStringValue(ConfigurationParam.DATE_FORMATS)
            return StringUtils.split(str, " \t\r\n,;") ?: emptyArray()
        }


    /**
     * Available excel date formats (configurable as parameter, see web dialogue with system parameters).
     *
     * @return
     */
    val excelDateFormats: Array<String>
        get() {
            val str = getStringValue(ConfigurationParam.EXCEL_DATE_FORMATS)
            return StringUtils.split(str, " \t\r\n,;") ?: emptyArray()
        }

    /**
     * @return The first entry of [.getExcelDateFormats] if exists, otherwise YYYY-MM-DD (ISO date format).
     */
    val defaultExcelDateFormat: String
        get() {
            val sa = excelDateFormats
            return if (sa.size > 0) {
                sa[0]
            } else {
                DateFormats.EXCEL_ISO_DATE
            }
        }

    val calendarDomain: String?
        get() = getValue(ConfigurationParam.CALENDAR_DOMAIN) as String?

    val isCalendarDomainValid: Boolean
        get() = isDomainValid(calendarDomain)


    fun putParameterManual(param: ConfigurationParam, value: Any) {
        checkRefresh()
        synchronized(this) {
            configurationParamMap!![param] = value
        }
    }

    /**
     * @param defaultValue If parameter not found or value is null, the default value is returned instead.
     * @return The string value of the given parameter stored as ConfigurationDO in the data base.
     * @throws ClassCastException if configuration parameter is from the wrong type.
     */
    @JvmOverloads
    fun getStringValue(parameter: IConfigurationParam, defaultValue: String? = null): String? {
        return getValue(parameter) as String? ?: defaultValue
    }

    /**
     * @return The boolean value of the given parameter stored as ConfigurationDO in the data base.
     * @throws ClassCastException if configuration parameter is from the wrong type.
     */
    fun getBooleanValue(parameter: IConfigurationParam): Boolean {
        val obj = getValue(parameter)
        return obj == java.lang.Boolean.TRUE
    }

    /**
     * @return The BigDecimal value of the given parameter stored as ConfigurationDO in the data base.
     * @throws ClassCastException if configuration parameter is from the wrong type.
     */
    fun getPercentValue(parameter: IConfigurationParam): BigDecimal? {
        return getValue(parameter) as BigDecimal?
    }

    val isCostConfigured: Boolean
        get() = getBooleanValue(ConfigurationParam.COST_CONFIGURED)

    protected fun getValue(parameter: IConfigurationParam?): Any? {
        checkRefresh()
        synchronized(this) {
            return configurationParamMap!![parameter]
        }
    }

    override fun refresh() {
        if (persistenceService == null) {
            // In test mode.
            doRefresh()
            return
        }
        persistenceService.runIsolatedReadOnly() { context ->
            doRefresh()
        }
    }

    private fun doRefresh() {
        val newMap: MutableMap<ConfigurationParam, Any?> = HashMap()
        log.info("Initializing (ConfigurationDO parameters) ...")
        val list = try {
            configurationService.daoInternalLoadAll()
        } catch (ex: Exception) {
            log.error(
                "******* Exception while getting configuration parameters from data-base (only OK for migration from older versions): "
                        + ex.message,
                ex
            )
            emptyList<ConfigurationDO>()
        }
        for (param in ConfigurationParam.values()) {
            var configuration: ConfigurationDO? = null
            for (entry in list) {
                if (StringUtils.equals(param.key, entry.parameter)) {
                    configuration = entry
                    break
                }
            }
            newMap[param] = configurationService.getDaoValue(param, configuration)
        }
        if (configurationParamMap == null) {
            for ((key, value1) in newMap) {
                val value = value1 ?: continue
                log.info(key.key + "=" + value)
            }
        }
        configurationParamMap = newMap
    }

    companion object {
        @JvmStatic
        lateinit var instance: Configuration
            private set

        private var instanceSet = false

        /**
         * Validates the domain.
         */
        @JvmStatic
        fun isDomainValid(domain: String?): Boolean {
            return (!domain.isNullOrBlank() && domain.matches("^[a-zA-Z]+[a-zA-Z0-9\\.\\-]*[a-zA-Z0-9]+$".toRegex()))
        }

        fun initializeForTestOnly(configurationService: ConfigurationService) {
            instance = Configuration(configurationService, null)
            instanceSet = true
        }
    }

    init {
        if (initialized) {
            log.warn("Oups, shouldn't initiate Configuration twice.")
        } else {
            instance = this
            instanceSet = true
        }
    }
}

