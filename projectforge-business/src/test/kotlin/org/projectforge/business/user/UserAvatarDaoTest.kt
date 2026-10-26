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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class UserAvatarDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var userAvatarDao: UserAvatarDao

    @Test
    fun `a user changes the own avatar, not the one of others`() {
        val testUserId = getUserId(TEST_USER)
        val otherId = getUserId(TEST_USER2)
        logon(TEST_USER)
        Assertions.assertTrue(userAvatarDao.hasWriteAccess(testUserId))
        Assertions.assertFalse(userAvatarDao.hasWriteAccess(otherId))
        val version = userAvatarDao.saveOrUpdate(testUserId, image(800))
        Assertions.assertEquals(version, userAvatarDao.getVersion(testUserId))
        assertThrows<AccessException> { userAvatarDao.saveOrUpdate(otherId, image(100)) }
        assertThrows<AccessException> { userAvatarDao.delete(otherId) }
        Assertions.assertTrue(userAvatarDao.delete(testUserId))
        Assertions.assertNull(userAvatarDao.getVersion(testUserId))
    }

    @Test
    fun `an admin changes the avatar of anybody`() {
        val otherId = getUserId("user3")
        logon(TEST_ADMIN_USER)
        Assertions.assertTrue(userAvatarDao.hasWriteAccess(otherId))
        userAvatarDao.saveOrUpdate(otherId, image(100))
        Assertions.assertNotNull(userAvatarDao.getImage(otherId))
        Assertions.assertTrue(userAvatarDao.delete(otherId))
    }

    @Test
    fun `an avatar is seen by the users sharing a group only`() {
        val ownerId = getUserId(TEST_USER2)
        logon(TEST_USER2)
        userAvatarDao.saveOrUpdate(ownerId, image(400))
        try {
            // TEST_USER shares TEST_GROUP with the owner:
            logon(TEST_USER)
            Assertions.assertNotNull(userAvatarDao.getVersion(ownerId))
            val preview = userAvatarDao.getImage(ownerId, preview = true)!!
            val previewImage = ImageIO.read(preview.bytes.inputStream())
            Assertions.assertTrue(previewImage.width <= UserAvatarDao.PREVIEW_SIZE)
            // user2 shares no group with the owner:
            logon("user2")
            Assertions.assertNull(userAvatarDao.getVersion(ownerId))
            Assertions.assertNull(userAvatarDao.getImage(ownerId))
            Assertions.assertNull(userAvatarDao.getImage(ownerId, preview = true))
        } finally {
            logon(TEST_USER2)
            userAvatarDao.delete(ownerId)
        }
    }

    @Test
    fun `only readable images are stored`() {
        val userId = getUserId(TEST_USER)
        logon(TEST_USER)
        Assertions.assertFalse(userAvatarDao.isImage("<svg onload=alert(1)>".toByteArray()))
        assertThrows<IllegalArgumentException> { userAvatarDao.saveOrUpdate(userId, "no image".toByteArray()) }
    }

    private fun image(size: Int): ByteArray {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until size) {
            for (y in 0 until size) {
                image.setRGB(x, y, (x * 31 + y * 17) and 0xffffff)
            }
        }
        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }
}
