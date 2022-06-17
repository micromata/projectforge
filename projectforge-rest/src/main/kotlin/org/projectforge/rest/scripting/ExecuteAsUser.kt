/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.scripting

import mu.KotlinLogging
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.User

private val log = KotlinLogging.logger {}

/**
 * If a script is marked as "sudo", this object is handed over to the script, so the further execution may
 * be proceeded with super users.
 */
class ExecuteAsUser internal constructor(private val _executeAsUser: PFUserDO, val scriptDO: ScriptDO) {
  val loggedInUser = User()
  val executeAsUser = User()

  init {
    executeAsUser.copyFrom(_executeAsUser)
    loggedInUser.copyFrom(ThreadLocalUserContext.getUser())
  }

  /**
   * Used by scripts to proceed script execution as executeUser.
   * @param securityQuestion Should be [I_CONFIRM_ALL_USERS_HAVE_NOW_FULL_ACCESS]. If not, no sudo is done.
   */
  @Suppress("unused")
  fun proceedAsSuperUser(securityQuestion: String) {
    if (securityQuestion != I_CONFIRM_ALL_USERS_HAVE_NOW_FULL_ACCESS) {
      throw IllegalArgumentException("Can't enter super mode. Param of proceedAsSuperUser must be SudoExecution.I_CONFIRM_ALL_USERS_HAVE_NOW_FULL_ACCESS.")
    }
    val loggedInUser = ThreadLocalUserContext.getUser()!!
    val user = PFUserDO.createCopy(_executeAsUser)!!
    user.username = "${loggedInUser.username} (executed as ${user.username})"
    log.info { "Script ${scriptDO.name} (#${scriptDO.id}) is now executed by ${user.username}." }
    ThreadLocalUserContext.setUser(_executeAsUser)
  }

  companion object {
    const val I_CONFIRM_ALL_USERS_HAVE_NOW_FULL_ACCESS =
      "I CONFIRM: All execute users have full access inside this script!!!"
  }
}
