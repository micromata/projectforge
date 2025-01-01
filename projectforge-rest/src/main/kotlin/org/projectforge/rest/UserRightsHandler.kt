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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.user.*
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.User
import org.projectforge.rest.dto.UserRightDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class UserRightsHandler {
  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @Autowired
  private lateinit var userRightDao: UserRightDao

  @Autowired
  private lateinit var userRightService: UserRightService

  fun getUserRights(user: User, userDO: PFUserDO): List<UserRightDto> {
    val list = mutableListOf<UserRightDto>()
    val dbList = userRightDao.select(userDO)
    val userGroups = User.getAssignedGroupDOs(user)
    for (right in userRightService.orderedRights) {
      if (!right.isAvailable(userDO, userGroups)) {
        continue
      }
      val rightDto = UserRightDto(rightId = right.id.toString())
      dbList.find { it.rightIdString == right.id.id }?.let {
        rightDto.value = it.value
      }
      list.add(rightDto)
    }
    return list
  }

  fun getUserRight(rightDto: UserRightDto, userDO: PFUserDO, userGroups: Collection<GroupDO>?): UserRight? {
    val right = userRightService.getRight(rightDto.rightId) ?: return null
    if (right.isConfigurable(userDO, userGroups)) {
      return right
    }
    return null
  }

  fun getUserRightVOs(user: User): List<UserRightVO> {
    val list = mutableListOf<UserRightVO>()
    user.userRights?.forEach { rightDto ->
      rightDto.rightId
      val right = userRightService.getRight(rightDto.rightId)
      if (right != null) {
        val rightVO = UserRightVO(right)
        if (right.isBooleanType) {
          rightVO.value = when (rightDto.booleanValue) {
            true -> UserRightValue.TRUE
            false -> UserRightValue.FALSE
            else -> null
          }
        } else {
          rightVO.value = rightDto.value
        }
        list.add(rightVO)
      } else {
        log.error("Oups, right with id '${rightDto.rightId}' not found. Will be ignored.")
      }
    }
    return list
  }
}
