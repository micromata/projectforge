/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.user.UserXmlPreferencesDO
import org.projectforge.business.user.UserXmlPreferencesDao
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.GZIPHelper
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("${Rest.URL}/admin")
class AdminRest() {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    /**
     * Helper for reading compressed serialized settings of user's in the database.
     */
    @PostMapping("extractSerializedSettings")
    fun extractSerializedSettings(@RequestBody serializedSettings: String): String {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val userPref = UserXmlPreferencesDO()
        userPref.serializedValue = serializedSettings
        userPref.user = PFUserDO().apply { id = ThreadLocalUserContext.requiredLoggedInUserId }
        val result = userXmlPreferencesDao.deserialize(userPref)
        return JsonUtils.toJson(result)
    }
}

/**
 * Small helper class to show compressed user settings as xml. Later, a post method on the admin pages would be nice.
 */
fun main() {
    val str =
        "!rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHAAAAE9H4sIAAAAAAAAAN2XUWuDMBDH3/sppHuuxtOLBlKhFPq2p32CTJO2m1VJfOm3X7JOtsEoCKELvt15F8OP/10u4b0+xoPu32Q9KmvLWEszxrVoZdcIHe+/jJfx2spnMVSrKOLGOSaqW2HMdt2eu3fZbE7CnDYXMaxdik2S3aivN9t6526sUsgJZoQnzpkCc/afFtllr8d93/a6ehJN\n" +
                "U0rKk+nDd4qaUii1cfU7zpP5G/PkB9NffAVACplfPkLUXTil1GPggAEr0DdcSQgJhM8WJ1C/fIfd7gCBFOcnX+mXjxEp2X39HsiHLPWsnwqnOC2cZ/GCar4Sizxd7mRALLIcPQuolMBgBHSjHRZcoI7Pu36hHJ0Ume/uC2o0FGA7cLmnC0OG4Fm/f7h38uT2sKhW837kniUfuFG8ncgMAAA="
    val xml = GZIPHelper.uncompress(str)
    println("Length of str: ${str.length}, length of xml: ${xml.length}")
    println(xml)
}
