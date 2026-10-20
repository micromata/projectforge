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

package org.projectforge.rest.admin

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.properties.ProjectForgePropertiesFile
import org.projectforge.framework.configuration.properties.ProjectForgePropertiesService
import org.projectforge.framework.configuration.properties.PropertyChange
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The **properties editor** (`/next/propertiesEditor`): edits the admin's projectforge.properties with the
 * parameters, groups and descriptions of the bundled application.properties ([ProjectForgePropertiesService]).
 * A backup is written before every save; changes take effect only after a restart.
 *
 * **Access:** admin group; saving also checks that the user is neither restricted nor a demo user. The whole path is
 * gated by the ADMIN_WRITE 2FA period (see ProjectForge2FAInitialization), because the file contains the database
 * credentials and the security keys.
 */
@RestController
@RequestMapping("${Rest.URL}/propertiesEditor")
class PropertiesEditorRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var propertiesService: ProjectForgePropertiesService

    class Change(var key: String? = null, var value: String? = null, var remove: Boolean = false)

    class SaveRequest(var fileVersion: String? = null, var changes: List<Change>? = null)

    /** [success] false: [message] is shown as an error (e.g. the file was modified in the meantime). */
    class SaveResponse(val success: Boolean, val message: String, val conflict: Boolean = false)

    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @GetMapping
    fun getData(): ProjectForgePropertiesService.Data {
        checkAdminAccess()
        return propertiesService.read()
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("save")
    fun save(@RequestBody request: SaveRequest): SaveResponse {
        checkWriteAccess()
        val fileVersion = request.fileVersion
        require(!fileVersion.isNullOrBlank()) { "fileVersion must be given." }
        val changes = request.changes.orEmpty().map { change ->
            val key = change.key?.trim()
            require(!key.isNullOrEmpty()) { "Key must be given." }
            PropertyChange(key, if (change.remove) null else change.value ?: "", change.remove)
        }
        if (changes.isEmpty()) {
            return SaveResponse(false, translate("system.propertiesEditor.nothingChanged"))
        }
        return try {
            val backup = propertiesService.save(changes, fileVersion)
            log.info { "Admin user changed projectforge.properties: ${changes.joinToString { it.key }}." }
            val message = if (backup != null) {
                translateMsg("system.propertiesEditor.saved", backup.name)
            } else {
                translate("system.propertiesEditor.savedWithoutBackup")
            }
            SaveResponse(true, "$message ${translate("system.propertiesEditor.restartRequired")}")
        } catch (_: ProjectForgePropertiesFile.ConflictException) {
            SaveResponse(false, translate("system.propertiesEditor.conflict"), conflict = true)
        } catch (ex: Exception) {
            log.error(ex) { "Error while writing projectforge.properties: ${ex.message}" }
            SaveResponse(false, translateMsg("system.propertiesEditor.saveFailed", ex.message ?: ex::class.simpleName))
        }
    }

    private fun checkAdminAccess() {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    }

    private fun checkWriteAccess() {
        checkAdminAccess()
        accessChecker.checkRestrictedOrDemoUser()
    }
}
