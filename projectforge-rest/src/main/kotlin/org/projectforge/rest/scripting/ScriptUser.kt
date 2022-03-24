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

import org.projectforge.business.fibu.ProjectUtils
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.dto.Project
import org.projectforge.rest.dto.User

/**
 * ScriptUser is accessible from any script and contains info about the logged-in-user as well as some
 * useful methods.
 */
@Suppress("unused")
class ScriptUser internal constructor() {
  private val loggedInUser = ThreadLocalUserContext.getUser()
  private val userGroupCache = UserGroupCache.getInstance()

  val user = User()

  fun getProjects(): List<Project> {
    return ProjectUtils.getProjectsOfManager(loggedInUser).map {
      val project = Project()
      project.copyFrom(it)
      project
    }
  }

  fun isMemberOfGroup(groupName: String): Boolean {
    userGroupCache.allGroups.find { it.name == groupName }?.let { group ->
      return userGroupCache.isUserMemberOfGroup(loggedInUser.id, group.id)
    }
    return false
  }

  fun isProjectManager(): Boolean {
    return userGroupCache.isUserMemberOfProjectManagers(loggedInUser.id)
  }

  fun isProjectAssistant(): Boolean {
    return userGroupCache.isUserMemberOfProjectAssistant(loggedInUser.id)
  }

  fun isControllingOrFinanceStaffMember(): Boolean {
    return userGroupCache.isUserMemberOfControllingGroup(loggedInUser.id) ||
        userGroupCache.isUserMemberOfFinanceGroup(loggedInUser.id)
  }

  fun isControllingStaffMember(): Boolean {
    return userGroupCache.isUserMemberOfControllingGroup(loggedInUser.id)
  }

  fun isFinanceStaffMember(): Boolean {
    return userGroupCache.isUserMemberOfFinanceGroup(loggedInUser.id)
  }

  fun isAdmin(): Boolean {
    return userGroupCache.isUserMemberOfAdminGroup(loggedInUser.id)
  }

  fun isHRStaffMember(): Boolean {
    return userGroupCache.isUserMemberOfHRGroup(loggedInUser.id)
  }

  init {
    user.copyFrom(ThreadLocalUserContext.getUser())
  }
}
