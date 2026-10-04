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

package org.projectforge.plugins.datatransfer.rest

import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The personal box of any user, for the user picker of the projectforge-next page `datatransfer/personal-box`.
 * Replaces the retired dynamic `DataTransferPersonalBoxPageRest` (`UILayout`).
 *
 * Everybody may put files into the personal box of any other user, that is what it is for: every user has
 * select access to every personal box (see `DataTransferAreaDao.hasAccess`).
 */
@RestController
@RequestMapping("${Rest.URL}/datatransferpersonalfiles")
class DataTransferPersonalBoxRest {
    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    /**
     * @param boxId The id of the personal box of [user], null if no user is given (or found).
     * @param user The user, at default the one chosen last time.
     */
    class PersonalBox(val boxId: Long? = null, val user: User? = null)

    /**
     * @param userId The user whose personal box is wanted, remembered for the next visit. Without it, the user
     * chosen last time is answered, but no box is resolved.
     */
    @AccessChecked("Any logged-in user: every user's personal box is open for uploads; own user pref")
    @GetMapping("box")
    fun getPersonalBox(@RequestParam("userId", required = false) userId: Long?): PersonalBox {
        if (userId == null) {
            val lastUser = getUserPref().userId?.let { toUser(it) }
            return PersonalBox(user = lastUser)
        }
        val user = toUser(userId) ?: return PersonalBox()
        getUserPref().userId = userId
        return PersonalBox(boxId = dataTransferAreaDao.ensurePersonalBox(userId)?.id, user = user)
    }

    private fun toUser(userId: Long): User? {
        val pfUser = userGroupCache.getUser(userId) ?: return null
        val user = User()
        user.copyFromMinimal(pfUser)
        return user
    }

    class PersonalBoxUserPref(var userId: Long? = null)

    private fun getUserPref(): PersonalBoxUserPref {
        return userPrefService.ensureEntry("datatransfer", "personalbox", PersonalBoxUserPref())
    }
}
