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

package org.projectforge.business.user

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.Tuple
import org.projectforge.Constants
import org.projectforge.business.address.ImageType
import org.projectforge.business.image.ImageService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.Date
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

/**
 * The avatars of the users. A user sees the own avatar and those of the users sharing at least one group, an admin
 * all of them; a user changes the own avatar, an admin the one of anybody.
 */
@Service
open class UserAvatarDao {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var imageService: ImageService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userAvatarCache: UserAvatarCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    class Image(val bytes: ByteArray, val imageType: ImageType)

    /**
     * The avatar (or its preview) of the given user, or null if the user has none or the logged-in user may not see
     * it ([hasSelectAccess]).
     */
    open fun getImage(userId: Long, preview: Boolean = false): Image? {
        if (!hasSelectAccess(userId)) {
            return null
        }
        val column = if (preview) "imagePreview" else "image"
        val tuple = persistenceService.selectSingleResult(
            "select $column as bytes,imageType as imageType from ${UserAvatarDO::class.java.name} t where t.user.id = :userId",
            Tuple::class.java,
            Pair("userId", userId),
        ) ?: return null
        val bytes = tuple.get("bytes", ByteArray::class.java)?.takeIf { it.isNotEmpty() } ?: return null
        return Image(bytes, tuple.get("imageType", ImageType::class.java) ?: ImageType.PNG)
    }

    /**
     * The version of the avatar of the given user (see [UserAvatarCache.getVersion]) for the logged-in user: null
     * if the user has none or the logged-in user may not see it, so the clients show the initials instead.
     */
    open fun getVersion(userId: Long?): Long? {
        val version = userAvatarCache.getVersion(userId) ?: return null
        return if (hasSelectAccess(userId)) version else null
    }

    /**
     * May the logged-in user see the avatar of the given user? The own one, as admin any, otherwise only of users
     * sharing at least one group with the logged-in user.
     */
    open fun hasSelectAccess(userId: Long?): Boolean {
        userId ?: return false
        val loggedInUser = ThreadLocalUserContext.loggedInUser ?: return false
        if (userId == loggedInUser.id || accessChecker.isUserMemberOfAdminGroup(loggedInUser)) {
            return true
        }
        val ownGroups = userGroupCache.getUserGroups(loggedInUser)?.takeIf { it.isNotEmpty() } ?: return false
        val otherGroups = userGroupCache.getUserGroups(userGroupCache.getUser(userId)) ?: return false
        return otherGroups.any { it in ownGroups }
    }

    /**
     * May the logged-in user change the avatar of the given user? Only the own one (not as restricted or demo
     * user), or as admin any.
     */
    open fun hasWriteAccess(userId: Long?): Boolean {
        val loggedInUser = ThreadLocalUserContext.loggedInUser ?: return false
        if (accessChecker.isUserMemberOfAdminGroup(loggedInUser)) {
            return true
        }
        return userId != null && userId == loggedInUser.id && !loggedInUser.restrictedUser &&
                !accessChecker.isDemoUser()
    }

    private fun checkWriteAccess(userId: Long) {
        if (!hasWriteAccess(userId)) {
            throw AccessException("access.exception.userHasNotRight")
        }
    }

    /**
     * Stores the given image as avatar of the given user, shrunk to [MAX_SIZE_BYTES] and with a preview of
     * [PREVIEW_SIZE] px.
     * @return The version (see [UserAvatarCache.getVersion]) of the new avatar.
     * @throws AccessException if the logged-in user may not change the avatar of this user.
     */
    open fun saveOrUpdate(userId: Long, image: ByteArray): Long {
        checkWriteAccess(userId)
        require(isImage(image)) { "The avatar of user #$userId isn't a readable image." }
        val user = userGroupCache.getUser(userId) ?: throw IllegalArgumentException("User #$userId not found.")
        val imageResult = imageService.shrinkToMaxFileSize(image, MAX_SIZE_BYTES)
        val now = Date()
        persistenceService.runInTransaction { context ->
            val avatar = context.selectSingleResult(
                "from ${UserAvatarDO::class.java.name} t where t.user.id = :userId",
                UserAvatarDO::class.java,
                Pair("userId", userId),
                attached = true,
            ) ?: UserAvatarDO().also { it.user = context.getReference(PFUserDO::class.java, userId) }
            avatar.image = imageResult.bytes
            avatar.imagePreview = imageService.resizeImagePreserveRatio(
                imageResult.bytes,
                PREVIEW_SIZE,
                PREVIEW_SIZE,
                imageResult.imageType,
            )
            avatar.imageType = imageResult.imageType
            avatar.lastUpdate = now
            if (avatar.id != null) {
                context.update(avatar)
            } else {
                context.insert(avatar)
            }
        }
        log.info { "New avatar of user #$userId (${user.username}) saved." }
        userAvatarCache.setExpired()
        return now.time
    }

    /**
     * Removes the avatar of the given user.
     * @return true if there was one.
     * @throws AccessException if the logged-in user may not change the avatar of this user.
     */
    open fun delete(userId: Long): Boolean {
        checkWriteAccess(userId)
        var deleted = false
        persistenceService.runInTransaction { context ->
            context.executeQuery(
                "from ${UserAvatarDO::class.java.name} t where t.user.id = :userId",
                UserAvatarDO::class.java,
                Pair("userId", userId),
                attached = true,
            ).forEach {
                context.delete(it)
                deleted = true
            }
        }
        if (deleted) {
            log.info { "Avatar of user #$userId deleted." }
        }
        userAvatarCache.setExpired()
        return deleted
    }

    fun getSupportedImageType(filename: String?): ImageType? = ImageType.fromExtension(filename)

    /** Only readable images are stored: they are encoded anew, so nothing but the picture reaches the browsers. */
    fun isImage(bytes: ByteArray): Boolean {
        return runCatching { ImageIO.read(ByteArrayInputStream(bytes)) != null }.getOrDefault(false)
    }

    companion object {
        /** The avatars are cut to 400 px in the image editor, which practically always fits. */
        private const val MAX_SIZE_BYTES = 100L * Constants.KB

        /** The chat shows avatars with 28 px, on a retina display that's 56 pixels. */
        const val PREVIEW_SIZE = 64
    }
}
