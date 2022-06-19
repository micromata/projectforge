package org.projectforge.rest

import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRight
import org.projectforge.business.user.UserRightDao
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.UserRightDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserRightsHandler {
  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @Autowired
  private lateinit var userRightDao: UserRightDao

  @Autowired
  private lateinit var userRightService: UserRightService

  fun getUserRights(userDO: PFUserDO): List<UserRightDto> {
    val list = mutableListOf<UserRightDto>()
    val dbList = userRightDao.getList(userDO)
    for (right in userRightService.getOrderedRights()) {
      if (!right.isAvailable(userGroupCache, userDO)) {
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

  fun getUserRight(rightDto: UserRightDto, userDO: PFUserDO): UserRight? {
    val right = userRightService.getRight(rightDto.rightId) ?: return null
    if (right.isConfigurable(userGroupCache, userDO)) {
      return right
    }
    return null
  }
}
