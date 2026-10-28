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

package org.projectforge.rest

import org.projectforge.Constants
import org.projectforge.business.user.UserAvatarDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

/**
 * The avatars of the users (see [UserAvatarDao] for who may see and change them). A change takes effect at once,
 * not with the save of a form.
 */
@RestController
@RequestMapping("${Rest.URL}/userAvatar")
class UserAvatarRest {
    class Result(val avatarVersion: Long?)

    @Autowired
    private lateinit var userAvatarDao: UserAvatarDao

    /**
     * The avatar of the given user. The clients add its version (`?v=`), so it may be cached.
     */
    @AccessChecked("UserAvatarDao.hasSelectAccess: own, admin or users sharing a group")
    @GetMapping("{userId}")
    fun getImage(@PathVariable("userId") userId: Long): ResponseEntity<ByteArray> {
        return image(userId, preview = false)
    }

    /**
     * The small preview of the avatar of the given user (chat, user menu).
     */
    @AccessChecked("UserAvatarDao.hasSelectAccess: own, admin or users sharing a group")
    @GetMapping("{userId}/preview")
    fun getPreview(@PathVariable("userId") userId: Long): ResponseEntity<ByteArray> {
        return image(userId, preview = true)
    }

    private fun image(userId: Long, preview: Boolean): ResponseEntity<ByteArray> {
        val image = userAvatarDao.getImage(userId, preview) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.imageType.mimeType))
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePrivate())
            .header("X-Content-Type-Options", "nosniff")
            .body(image.bytes)
    }

    /**
     * Stores the uploaded image (already cut square and scaled down by the image editor) as avatar of the given user.
     */
    @AccessChecked("UserAvatarDao.hasWriteAccess: own user or admin")
    @PostMapping("{userId}")
    fun upload(
        @PathVariable("userId") userId: Long,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<*> {
        if (!userAvatarDao.hasWriteAccess(userId)) {
            throw AccessException("access.exception.userHasNotRight")
        }
        if (file.size > MAX_UPLOAD_SIZE_BYTES) {
            return ResponseEntity(
                translateMsg("file.upload.error.maxSizeOfExceeded", "${MAX_UPLOAD_SIZE_BYTES / Constants.MB} MB"),
                HttpStatus.BAD_REQUEST,
            )
        }
        val bytes = file.bytes
        if (userAvatarDao.getSupportedImageType(file.originalFilename) == null || !userAvatarDao.isImage(bytes)) {
            return ResponseEntity(translate("user.avatar.error.unsupportedImage"), HttpStatus.BAD_REQUEST)
        }
        return ResponseEntity.ok(Result(userAvatarDao.saveOrUpdate(userId, bytes)))
    }

    @AccessChecked("UserAvatarDao.hasWriteAccess: own user or admin")
    @DeleteMapping("{userId}")
    fun delete(@PathVariable("userId") userId: Long): Result {
        userAvatarDao.delete(userId)
        return Result(null)
    }

    companion object {
        /** The image editor uploads at most 400x400 px, the limit only keeps out other uploads. */
        private const val MAX_UPLOAD_SIZE_BYTES = 2L * Constants.MB
    }
}
