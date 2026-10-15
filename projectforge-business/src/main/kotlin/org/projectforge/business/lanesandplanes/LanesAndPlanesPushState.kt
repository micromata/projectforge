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
import org.projectforge.framework.json.JsonUtils
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * The state of the last successful push (in the work directory, so it survives a restart): the number of users
 * sent, see LanesAndPlanesSyncService.refuseReason. The emails known to Lanes & Planes are kept in the settings,
 * see [LanesAndPlanesSettings.userEmails].
 *
 * @param users The number of users sent (additional users included).
 */
class LanesAndPlanesPushState(var users: Int? = null) {
    companion object {
        /** The state of the given file, null if missing or not readable. Older files (with emails) are read too. */
        fun load(file: File): LanesAndPlanesPushState? {
            if (!file.exists()) {
                return null
            }
            return try {
                JsonUtils.fromJson(file.readText(), LanesAndPlanesPushState::class.java, failOnUnknownProps = false)
            } catch (ex: Exception) {
                log.warn { "Lanes & Planes: push state ${file.absolutePath} not readable, ignored: ${ex.message}" }
                null
            }
        }

        fun save(file: File, users: Int) {
            file.writeText(JsonUtils.toJson(LanesAndPlanesPushState(users)))
        }
    }
}
