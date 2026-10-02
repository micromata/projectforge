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

package org.projectforge.plugins.licensemanagement.rest

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.FileCheck
import org.projectforge.plugins.licensemanagement.LicenseDO
import org.projectforge.plugins.licensemanagement.LicenseDao
import org.projectforge.plugins.licensemanagement.LicenseManagementRight
import org.projectforge.plugins.licensemanagement.LicensemanagementPluginUserRightsId
import org.projectforge.plugins.licensemanagement.dto.License
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

/**
 * The layout-free REST endpoint of the licenses (menu "Licenses / Hardware"), serving the hand-built
 * projectforge-next page (see components/features/license). Replaces the retired `LicensePagesRest`, whose
 * server-side `UILayout` moved onto the frontend, and the removed Wicket pages.
 *
 * The license key and the two stored files are only for administrators and the owners of a license
 * ([LicenseManagementRight.isLicenseKeyVisible]): the key travels only to them, the files are never part of the
 * DTO but have endpoints of their own ([FILE_PATH]), and a save keeps what the client could not send.
 */
@RestController
@RequestMapping("${Rest.URL}/license")
class LicenseEntityRest : AbstractDTOEntityRest<LicenseDO, License, LicenseDao>(
    baseDaoClazz = LicenseDao::class.java,
    i18nKeyPrefix = "plugins.licensemanagement.title",
    // The copy is offered for editing and saved by the user, as Wicket's clone button did.
    cloneSupport = CloneSupport.CLONE,
) {
    @Autowired
    private lateinit var userRightService: UserRightService

    private val right: LicenseManagementRight
        get() = userRightService.getRight(LicensemanagementPluginUserRightsId.PLUGIN_LICENSE_MANAGEMENT) as LicenseManagementRight

    private fun isKeyVisible(license: LicenseDO): Boolean {
        return right.isLicenseKeyVisible(ThreadLocalUserContext.loggedInUser, license)
    }

    /**
     * Takes over what the DTO doesn't carry from the stored license: the files (written by the file endpoints
     * only) and, for a user who may not see it, the key. Necessary because an update copies every value of the
     * posted object, null values included, onto the stored one.
     *
     * Whether the key is visible is decided on the *stored* license: a user adding themselves as owner gets to
     * see the key after the save, not in it.
     */
    override fun transformForDB(dto: License): LicenseDO {
        val license = LicenseDO()
        dto.copyTo(license)
        license.file1 = null
        license.filename1 = null
        license.file2 = null
        license.filename2 = null
        val dbObj = dto.id?.let { baseDao.find(it, checkAccess = false) } ?: return license
        license.file1 = dbObj.file1
        license.filename1 = dbObj.filename1
        license.file2 = dbObj.file2
        license.filename2 = dbObj.filename2
        if (!isKeyVisible(dbObj)) {
            license.key = dbObj.key
        }
        return license
    }

    override fun transformFromDB(obj: LicenseDO, editMode: Boolean): License {
        val license = License()
        license.copyFrom(obj)
        license.keyVisible = isKeyVisible(obj)
        // A name without its file is no file (see LicenseFilesState).
        val files = LicenseFilesState(obj)
        license.numberOfFiles = listOfNotNull(files.filename1, files.filename2).size.takeIf { it > 0 }
        if (license.keyVisible) {
            license.filename1 = files.filename1
            license.filename2 = files.filename2
        } else {
            license.key = null
            license.filename1 = null
            license.filename2 = null
        }
        return license
    }

    /** One license is what a new entry is about, as the removed Wicket `LicenseEditPage` preset it. */
    override fun newBaseDO(request: HttpServletRequest?): LicenseDO {
        return LicenseDO().also { it.numberOfLicenses = 1 }
    }

    /** The copy has none of the files (it is a new license), and its creator sees what they type in. */
    override fun prepareClone(dto: License): License {
        super.prepareClone(dto)
        dto.filename1 = null
        dto.filename2 = null
        dto.keyVisible = true
        return dto
    }

    /** The key column is for administrators only, as on the removed Wicket `LicenseListPage`. */
    override fun addVariablesForListPage(): Map<String, Any> {
        return mapOf("keyColumnVisible" to accessChecker.isLoggedInUserMemberOfAdminGroup)
    }

    // ------------------------------------------------------------------------------------------
    // The two stored files
    // ------------------------------------------------------------------------------------------

    /** The names of the stored files. */
    @GetMapping("$FILE_PATH/{id}")
    fun getFiles(@PathVariable("id") id: Long): LicenseFilesState {
        return LicenseFilesState(checkFileAccess(id, write = false))
    }

    /** Downloads the file of the given slot (1 or 2). */
    @GetMapping("$FILE_PATH/{id}/{slot}")
    fun downloadFile(@PathVariable("id") id: Long, @PathVariable("slot") slot: Int): ResponseEntity<*> {
        val license = checkFileAccess(id, write = false)
        val bytes = if (slot == 1) license.file1 else if (slot == 2) license.file2 else null
        val filename = if (slot == 1) license.filename1 else if (slot == 2) license.filename2 else null
        if (bytes == null) {
            return ResponseEntity.notFound().build<Any>()
        }
        log.info { "Downloading file #$slot '$filename' of license #$id." }
        return RestUtils.downloadFile(filename ?: "license-$id-file$slot", bytes)
    }

    /**
     * Stores the given file in the given slot (1 or 2), replacing the one that was there. A refusal (too
     * large) is answered as 400 with the translated text of [FileCheck].
     */
    @PostMapping("$FILE_PATH/{id}/{slot}")
    fun uploadFile(
        @PathVariable("id") id: Long,
        @PathVariable("slot") slot: Int,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<*> {
        checkSlot(slot)
        val license = checkFileAccess(id, write = true)
        val filename = file.originalFilename ?: "unknown"
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(translate("file.upload.error.noFileSelected"))
        }
        FileCheck.checkFile(filename, file.size, megaBytes = MAX_FILE_MEGA_BYTES)?.let { error ->
            return ResponseEntity.badRequest().body(error)
        }
        log.info { "Uploading file #$slot '$filename' (${file.size} bytes) for license #$id." }
        val bytes = file.inputStream.use { it.readBytes() }
        if (slot == 1) {
            license.file1 = bytes
            license.filename1 = filename
        } else {
            license.file2 = bytes
            license.filename2 = filename
        }
        baseDao.update(license)
        return ResponseEntity.ok(LicenseFilesState(license))
    }

    /** Removes the file of the given slot (1 or 2); nothing to do where there is none. */
    @DeleteMapping("$FILE_PATH/{id}/{slot}")
    fun deleteFile(@PathVariable("id") id: Long, @PathVariable("slot") slot: Int): LicenseFilesState {
        checkSlot(slot)
        val license = checkFileAccess(id, write = true)
        log.info { "Deleting file #$slot of license #$id." }
        if (slot == 1) {
            license.file1 = null
            license.filename1 = null
        } else {
            license.file2 = null
            license.filename2 = null
        }
        baseDao.update(license)
        return LicenseFilesState(license)
    }

    /** Swaps the two files, as Wicket's swap button did. */
    @PostMapping("$FILE_PATH/{id}/swap")
    fun swapFiles(@PathVariable("id") id: Long): LicenseFilesState {
        val license = checkFileAccess(id, write = true)
        log.info { "Swapping the files of license #$id." }
        val file1 = license.file1
        val filename1 = license.filename1
        license.file1 = license.file2
        license.filename1 = license.filename2
        license.file2 = file1
        license.filename2 = filename1
        baseDao.update(license)
        return LicenseFilesState(license)
    }

    /**
     * The license, if the user may see its files (select access plus [isKeyVisible], the condition under which
     * Wicket offered them) and, for [write], update it. An unknown id is an [AccessException]: "there is none"
     * and "you may not see it" are the same answer here.
     */
    private fun checkFileAccess(id: Long, write: Boolean): LicenseDO {
        val license = baseDao.find(id) ?: throw AccessException("access.exception.userHasNotRight")
        if (!isKeyVisible(license)) {
            throw AccessException("access.exception.userHasNotRight")
        }
        if (write) {
            baseDao.hasLoggedInUserUpdateAccess(license, license, throwException = true)
        }
        return license
    }

    private fun checkSlot(slot: Int) {
        require(slot == 1 || slot == 2) { "Unknown file slot $slot, 1 or 2 expected." }
    }

    /** The names of the two files after a change, so the client needs no second call. */
    class LicenseFilesState(license: LicenseDO) {
        val filename1: String? = license.file1?.let { license.filename1 ?: "file1" }
        val filename2: String? = license.file2?.let { license.filename2 ?: "file2" }
    }

    companion object {
        const val FILE_PATH = "file"

        private const val MAX_FILE_MEGA_BYTES = 20L
    }
}
