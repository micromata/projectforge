package org.projectforge.rest.dto

import org.projectforge.business.address.AddressbookDO
import org.projectforge.common.StringHelper

class Addressbook(var title: String? = null,
                  var owner: User? = null,
                  var description: String? = null,
                  var fullAccessGroups: MutableList<Group>? = null,
                  var fullAccessUsers: MutableList<User>? = null,
                  var readonlyAccessGroups: MutableList<Group>? = null,
                  var readonlyAccessUsers: MutableList<User>? = null
) : BaseObject<AddressbookDO>() {

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyFrom(src: AddressbookDO) {
        super.copyFrom(src)
        fullAccessGroups = toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = toUserList(src.fullAccessUserIds)
        readonlyAccessGroups = toGroupList(src.readonlyAccessGroupIds)
        readonlyAccessUsers = toUserList(src.readonlyAccessUserIds)
    }

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyTo(dest: AddressbookDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = fullAccessGroups?.joinToString { "${it.id}" }
        dest.fullAccessUserIds = fullAccessUsers?.joinToString { "${it.id}" }
        dest.readonlyAccessGroupIds = readonlyAccessGroups?.joinToString { "${it.id}" }
        dest.readonlyAccessUserIds = readonlyAccessUsers?.joinToString { "${it.id}" }
    }

    private fun toUserList(str: String?): MutableList<User>? {
        if (str.isNullOrBlank()) return null
        val users = mutableListOf<User>()
        StringHelper.splitToInts(str, ",", false).forEach { users.add(User(it, fullname = "Kai Reinhard")) }
        return users
    }

    private fun toGroupList(str: String?): MutableList<Group>? {
        if (str.isNullOrBlank()) return null
        val groups = mutableListOf<Group>()
        StringHelper.splitToInts(str, ",", false).forEach { groups.add(Group(it, name = "Gruppe")) }
        return groups
    }
}
