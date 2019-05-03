package org.projectforge.rest.dto

import org.projectforge.business.address.AddressbookDO
import org.projectforge.common.StringHelper

class Addressbook(var title: String? = null,
                  var owner: User? = null,
                  var description: String? = null,
                  var fullAccessGroupIds: MutableList<Int>? = null,
                  var fullAccessUserIds: MutableList<Int>? = null,
                  var readonlyAccessGroupIds: MutableList<Int>? = null,
                  var readonlyAccessUserIds: MutableList<Int>? = null
) : BaseObject<AddressbookDO>() {

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyFrom(src: AddressbookDO) {
        super.copyFrom(src)
        fullAccessGroupIds = toIntList(src.fullAccessGroupIds)
        fullAccessUserIds = toIntList(src.fullAccessUserIds)
        readonlyAccessGroupIds = toIntList(src.readonlyAccessGroupIds)
        readonlyAccessUserIds = toIntList(src.readonlyAccessUserIds)
    }

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyTo(dest: AddressbookDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = toString(fullAccessGroupIds)
        dest.fullAccessUserIds = toString(fullAccessUserIds)
        dest.readonlyAccessGroupIds = toString(readonlyAccessGroupIds)
        dest.readonlyAccessUserIds = toString(readonlyAccessUserIds)
    }

    private fun toString(intList: List<Int>?): String? {
        return intList?.joinToString { "$it" } ?: null
    }

    private fun toIntList(str: String?): MutableList<Int>? {
        if (str.isNullOrBlank()) {
            return null
        }
        return StringHelper.splitToInts(str, ",", false).toMutableList()
    }
}
