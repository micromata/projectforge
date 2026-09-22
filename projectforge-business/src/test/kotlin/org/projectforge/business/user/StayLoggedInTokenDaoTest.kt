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
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.time.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

class StayLoggedInTokenDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

    /**
     * The point of one row per device: two logins of the same user are two independent credentials, and
     * invalidating one mustn't touch the other.
     */
    @Test
    fun oneRowPerDeviceTest() {
        val user = getUser(TEST_USER)
        val userId = user.id!!
        stayLoggedInTokenDao.deleteAll(userId)
        val firstDevice = stayLoggedInTokenDao.createToken(user, null)
        val secondDevice = stayLoggedInTokenDao.createToken(user, null)
        Assertions.assertNotEquals(firstDevice, secondDevice, "Every device gets its own token.")
        Assertions.assertEquals(2, stayLoggedInTokenDao.getEntries(userId).size)

        // The clear text token is only in the cookie, the database holds its hash:
        val entry = stayLoggedInTokenDao.getValidToken(firstDevice)!!
        Assertions.assertEquals(userId, entry.user?.id)
        Assertions.assertNotEquals(firstDevice, entry.tokenHash, "Stored as a hash, not in clear text.")
        Assertions.assertEquals(StayLoggedInTokenDao.hash(firstDevice), entry.tokenHash)

        Assertions.assertEquals(1, stayLoggedInTokenDao.deleteByToken(firstDevice), "One device, not both.")
        Assertions.assertNull(stayLoggedInTokenDao.getValidToken(firstDevice), "Logged out on this device.")
        Assertions.assertNotNull(stayLoggedInTokenDao.getValidToken(secondDevice), "Other device stays logged in.")

        stayLoggedInTokenDao.deleteAll(userId)
        Assertions.assertNull(stayLoggedInTokenDao.getValidToken(secondDevice), "Logged out everywhere.")
        Assertions.assertTrue(stayLoggedInTokenDao.getEntries(userId).isEmpty())
    }

    @Test
    fun unknownTokenTest() {
        Assertions.assertNull(stayLoggedInTokenDao.getValidToken(null))
        Assertions.assertNull(stayLoggedInTokenDao.getValidToken(""))
        Assertions.assertNull(stayLoggedInTokenDao.getValidToken("tooShort"))
        Assertions.assertNull(stayLoggedInTokenDao.getValidToken("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
        Assertions.assertEquals(0, stayLoggedInTokenDao.deleteByToken("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
    }

    /**
     * The expiry is enforced on every check, not only by the nightly purge job: a token whose last access is
     * older than the cookie's max age is dead, and its row is gone right away.
     */
    @Test
    fun expiredTokenTest() {
        val user = getUser(TEST_USER)
        val userId = user.id!!
        stayLoggedInTokenDao.deleteAll(userId)
        val token = stayLoggedInTokenDao.createToken(user, null)
        val entry = stayLoggedInTokenDao.getValidToken(token)!!
        setLastAccess(entry.id!!, Date(System.currentTimeMillis() - 31 * TimeUnit.DAY.millis))
        Assertions.assertNull(stayLoggedInTokenDao.getValidToken(token), "Older than 30 days.")
        Assertions.assertTrue(stayLoggedInTokenDao.getEntries(userId).isEmpty(), "The row is deleted, not only rejected.")
    }

    /**
     * A page load is a wave of requests, each carrying the cookie: lastAccess must not be written per request.
     */
    @Test
    fun updateLastAccessIfDueTest() {
        val user = getUser(TEST_USER)
        val userId = user.id!!
        stayLoggedInTokenDao.deleteAll(userId)
        val token = stayLoggedInTokenDao.createToken(user, null)
        val entry = stayLoggedInTokenDao.getValidToken(token)!!
        val lastAccess = entry.lastAccess
        stayLoggedInTokenDao.updateLastAccessIfDue(entry, null)
        Assertions.assertEquals(lastAccess, stayLoggedInTokenDao.getValidToken(token)?.lastAccess, "Not due yet.")

        val twoHoursAgo = Date(System.currentTimeMillis() - 2 * TimeUnit.HOUR.millis)
        setLastAccess(entry.id!!, twoHoursAgo)
        val stale = stayLoggedInTokenDao.getValidToken(token)!!
        stayLoggedInTokenDao.updateLastAccessIfDue(stale, null)
        Assertions.assertTrue(
            stayLoggedInTokenDao.getValidToken(token)!!.lastAccess!!.after(twoHoursAgo),
            "Refreshed after an hour.",
        )
        stayLoggedInTokenDao.deleteAll(userId)
    }

    private fun setLastAccess(id: Long, lastAccess: Date) {
        persistenceService.runInTransaction { context ->
            context.em.find(StayLoggedInTokenDO::class.java, id).let { entry ->
                entry.lastAccess = lastAccess
                context.em.flush()
            }
        }
    }
}
