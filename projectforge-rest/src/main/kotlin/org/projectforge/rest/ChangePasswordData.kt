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

import org.projectforge.business.login.LoginHandler

class ChangePasswordData(
  var userId: Long? = null,
  var userDisplayName: String? = null,
  var loginPassword: CharArray? = null,
  var newPassword: CharArray? = null,
  var passwordRepeat: CharArray? = null,
) {
  fun clear() {
    loginPassword?.let {
      LoginHandler.clearPassword(it)
    }
    newPassword?.let {
      LoginHandler.clearPassword(it)
    }
    passwordRepeat?.let {
      LoginHandler.clearPassword(it)
    }
  }
}
