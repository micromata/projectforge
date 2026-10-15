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


package org.projectforge.business.lanesandplanes

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationJsonValidators
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.json.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The settings of the Lanes & Planes push (configuration parameter `lanesAndPlanes`). Always available (unlike
 * [LanesAndPlanesSyncService]), so the parameter is validated on save, even if the push isn't enabled.
 */
@Service
open class LanesAndPlanesSettingsService {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var kostCache: KostCache

    @PostConstruct
    private fun postConstruct() {
        ConfigurationJsonValidators.register(ConfigurationParam.LANES_AND_PLANES) { json ->
            validate(LanesAndPlanesSettings.parse(json))
        }
    }

    /** Read on every push: changes are sent by the next run. */
    open fun settings(): LanesAndPlanesSettings {
        return LanesAndPlanesSettings.parse(Configuration.instance.getStringValue(ConfigurationParam.LANES_AND_PLANES))
    }

    /**
     * Adds the given emails (by user id) to [LanesAndPlanesSettings.userEmails], after a successful push: from now
     * on these users are sent with this email, even if it changes in ProjectForge. Users already having an entry
     * are left untouched, so a change in the editor meanwhile isn't overwritten. Nothing is written, if there is
     * nothing new.
     */
    open fun addUserEmails(newEmails: Map<Long, String>) {
        if (newEmails.isEmpty()) {
            return
        }
        synchronized(this) {
            val entry = configurationDao.getEntry(ConfigurationParam.LANES_AND_PLANES) ?: run {
                configurationDao.checkAndUpdateDatabaseEntries()
                configurationDao.getEntry(ConfigurationParam.LANES_AND_PLANES)
            } ?: throw IllegalStateException("Configuration parameter ${ConfigurationParam.LANES_AND_PLANES.key} missing.")
            val settings = LanesAndPlanesSettings.parse(entry.stringValue)
            val known = settings.userEmails.mapNotNull { it.userId }.toSet()
            val added = newEmails.filterKeys { it !in known }.map { (userId, email) ->
                LanesAndPlanesSettings.UserEmail(userId, email)
            }
            if (added.isEmpty()) {
                return
            }
            settings.userEmails = settings.userEmails.filter { !it.isEmpty } + added.sortedBy { it.userId }
            entry.stringValue = JsonUtils.toJson(settings)
            configurationDao.update(entry, checkAccess = false)
            log.info { "Lanes & Planes: ${added.size} emails of pushed users stored in the configuration (users #${added.joinToString(", #") { "${it.userId}" }})." }
        }
        Configuration.instance.forceReload()
    }

    /** The translated errors of the given settings, each prefixed by the label of its field. */
    private fun validate(settings: LanesAndPlanesSettings): List<String> {
        return settings.validate({ kostCache.getKost1(it) }, { kostCache.getKost2(it) }).map { error ->
            val label = translate("${LanesAndPlanesSettings.I18N_PREFIX}.${error.field}")
            val prefix = error.index?.let { "$label ${it + 1}" } ?: label
            "$prefix: ${translateMsg(error.i18nKey, error.param)}"
        }
    }
}
