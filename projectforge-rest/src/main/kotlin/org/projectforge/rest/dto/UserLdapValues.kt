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

package org.projectforge.rest.dto

import org.projectforge.business.ldap.LdapUserValues

class UserLdapValues(
  var asString: String? = null,
  var uidNumber: Int? = null,
  var gidNumber: Int? = null,
  var homeDirectory: String? = null,
  var loginShell: String? = null,
  var sambaPrimaryGroupSIDNumber: Int? = null,
  var sambaSIDNumber: Int? = null,
  var sambaNTPassword: String? = "******",
) {
  fun convert(): LdapUserValues {
    val obj = LdapUserValues()
    obj.uidNumber = uidNumber
    obj.gidNumber = gidNumber
    obj.homeDirectory = homeDirectory
    obj.loginShell = loginShell
    obj.sambaPrimaryGroupSIDNumber = sambaPrimaryGroupSIDNumber
    obj.sambaSIDNumber = sambaSIDNumber
    return obj
  }

  fun copyFrom(src: LdapUserValues) {
    uidNumber = src.uidNumber
    gidNumber = src.gidNumber
    homeDirectory = src.homeDirectory
    loginShell = src.loginShell
    sambaPrimaryGroupSIDNumber = src.sambaPrimaryGroupSIDNumber
    sambaSIDNumber = src.sambaSIDNumber
  }

  val isValuesEmpty: Boolean
    get() = isPosixValuesEmpty && isSambaValuesEmpty

  val isPosixValuesEmpty: Boolean
    get() = uidNumber == null &&
        homeDirectory.isNullOrBlank() &&
        loginShell.isNullOrBlank() &&
        gidNumber == null

  val isSambaValuesEmpty: Boolean
    get() = sambaSIDNumber == null && sambaPrimaryGroupSIDNumber == null

  companion object {
    fun create(src: LdapUserValues): UserLdapValues {
      val dto = UserLdapValues()
      dto.uidNumber = src.uidNumber
      dto.gidNumber = src.gidNumber
      dto.homeDirectory = src.homeDirectory
      dto.loginShell = src.loginShell
      dto.sambaPrimaryGroupSIDNumber = src.sambaPrimaryGroupSIDNumber
      dto.sambaSIDNumber = src.sambaSIDNumber
      val sb = StringBuilder()
      dto.uidNumber?.let { sb.append("uid=$it, ") }
      dto.gidNumber?.let { sb.append("gid=$it, ") }
      dto.homeDirectory?.let {
        if (it.isNotBlank()) {
          sb.append("homeDirectory=$it, ")
        }
      }
      dto.loginShell?.let {
        if (it.isNotBlank()) {
          sb.append("loginShell=$it, ")
        }
      }
      dto.sambaPrimaryGroupSIDNumber?.let { sb.append("sambaPrimaryGroupSIDNumber=$it, ") }
      dto.sambaSIDNumber?.let { sb.append("sambaSIDNumber=$it, ") }
      dto.asString = sb.toString().removeSuffix(", ")
      return dto
    }
  }
}
