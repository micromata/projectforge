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
import org.junit.jupiter.api.io.TempDir
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

/**
 * The test runs [E2ETestAccountsService.ensureAccounts] directly rather than through the
 * `ApplicationReadyEvent`, with a temporary home directory: what is worth asserting is that a second
 * run changes nothing and that a password the file no longer agrees with is replaced.
 *
 * Every case runs **without a logged-in user**, because that is the situation at startup — and one the
 * service has to arrange for itself: writing a history entry asks the access checker about the
 * logged-in user, and no user at all counts as a restricted one.
 */
class E2ETestAccountsServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var e2eTestAccountsService: E2ETestAccountsService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userPasswordDao: UserPasswordDao

    @Test
    fun `accounts are created once and repaired on every run`(@TempDir homeDir: File) {
        ThreadLocalUserContext.clear() // As at startup: nobody is logged in.
        val first = e2eTestAccountsService.ensureAccounts(homeDir)
        Assertions.assertEquals(4, first.size, "One account per role.")
        val file = File(homeDir, E2ETestAccountsService.FILENAME)
        Assertions.assertTrue(file.exists(), "The file with the generated passwords is written.")

        val fullAccess = first["full-access-user"]!!
        Assertions.assertEquals("e2e-full-access", fullAccess.username)
        val user = userService.getInternalByUsername(fullAccess.username)
        Assertions.assertNotNull(user, "The user exists after the first run.")
        Assertions.assertTrue(
            userPasswordDao.checkPassword(user!!, fullAccess.password.toCharArray())!!.isOK,
            "The password the file names is the password the user has.",
        )
        Assertions.assertTrue(
            userGroupCache.isUserMemberOfAdminGroup(user.id),
            "The full access account is in the admin group.",
        )
        Assertions.assertEquals(
            UserRightValue.READWRITE,
            userGroupCache.getUserRight(user.id, UserRightId.FIBU_AUSGANGSRECHNUNGEN)?.value,
            "...and holds the finance rights.",
        )

        // The second run must find everything in place: same users, same passwords, same file.
        val content = file.readText()
        val second = e2eTestAccountsService.ensureAccounts(homeDir)
        Assertions.assertEquals(first, second, "Nothing is created or changed twice.")
        Assertions.assertEquals(content, file.readText(), "An unchanged state doesn't rewrite the file.")
        Assertions.assertEquals(
            user.id,
            userService.getInternalByUsername(fullAccess.username)?.id,
            "No second user of the same name.",
        )
    }

    @Test
    fun `a password the file no longer agrees with is replaced`(@TempDir homeDir: File) {
        ThreadLocalUserContext.clear() // As at startup: nobody is logged in.
        val accounts = e2eTestAccountsService.ensureAccounts(homeDir)
        val file = File(homeDir, E2ETestAccountsService.FILENAME)
        val normalo = accounts["normalo-user"]!!
        // What happens when the file is stale — copied from another instance, or the password was
        // changed in the UI meanwhile.
        file.writeText("normalo-user=${normalo.username}/wrong-password\n")

        val repaired = e2eTestAccountsService.ensureAccounts(homeDir)
        Assertions.assertEquals(4, repaired.size, "The roles missing from the file are back.")
        val newNormalo = repaired["normalo-user"]!!
        Assertions.assertEquals(normalo.username, newNormalo.username)
        Assertions.assertNotEquals("wrong-password", newNormalo.password, "A new password is generated.")
        Assertions.assertTrue(
            userPasswordDao.checkPassword(
                userService.getInternalByUsername(newNormalo.username)!!,
                newNormalo.password.toCharArray(),
            )!!.isOK,
            "...and it is the one the user now has.",
        )
        Assertions.assertTrue(
            file.readText().contains("normalo-user=${newNormalo.username}/${newNormalo.password}"),
            "...and the one the file names.",
        )
    }

    @Test
    fun `a role pointed at another user by hand is left alone`(@TempDir homeDir: File) {
        ThreadLocalUserContext.clear() // As at startup: nobody is logged in.
        val file = File(homeDir, E2ETestAccountsService.FILENAME)
        // Any other user will do: the line is only checked for naming one of its own.
        file.writeText("finance-user=$TEST_FULL_ACCESS_USER/somePassword\n")

        val accounts = e2eTestAccountsService.ensureAccounts(homeDir)
        Assertions.assertEquals(
            E2ETestAccountsService.Account(TEST_FULL_ACCESS_USER, "somePassword"),
            accounts["finance-user"],
            "The developer's own account for that role survives, password and all.",
        )
        Assertions.assertEquals(
            "e2e-full-access",
            accounts["full-access-user"]?.username,
            "The roles the file doesn't override are created as usual.",
        )
        Assertions.assertTrue(
            file.readText().contains("finance-user=$TEST_FULL_ACCESS_USER/somePassword"),
            "...and the rewritten file still has that line.",
        )
    }
}
