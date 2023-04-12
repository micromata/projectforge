package org.projectforge.business.poll

import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class PollDao : BaseDao<PollDO>(PollDO::class.java){

    @Autowired
    private lateinit var userService: UserService

    override fun newInstance(): PollDO {
        return PollDO()
    }

    override fun hasAccess(
        user: PFUserDO?,
        obj: PollDO?,
        oldObj: PollDO?,
        operationType: OperationType?,
        throwException: Boolean
    ): Boolean {
        return true
    }

    /*override fun getList(
        filter: QueryFilter,
        customResultFilters: MutableList<CustomResultFilter<PollDO>>?
    ): List<PollDO> {
        val loggedInUserId = ThreadLocalUserContext.userId
        // Don't search for personal boxes of other users (they will be added afterwards):
        filter.add(
            DBPredicate.Or(
                // Either not a personal box,
                // or the personal box of the logged-in user:
                DBPredicate.Equal("adminIds", loggedInUserId.toString()),
            )
        )
        var result = super.getList(filter, customResultFilters)
        // searchString contains trailing %:
        val searchString = filter.fulltextSearchString?.replace("%", "")
        if (searchString == null || searchString.length < 2) { // Search string is given and has at least 2 chars:
            return result
        }
        result = result.toMutableList()
        userService.sortedUsers.filter { user ->
            user.username?.contains(searchString, ignoreCase = true) == true ||
                    user.getFullname().contains(searchString, ignoreCase = true)
        }.forEach { user ->
            // User name matches given string, so add personal box of this active user:
            result.add(internalLoadAll()[user.id])
        }
        return result
    }

     */

}