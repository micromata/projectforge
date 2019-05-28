package org.projectforge.rest.dto

import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class Group(id: Int? = null,
            var name: String? = null,
            var assignedUsers: MutableSet<PFUserDO>? = null
) : BaseObject<GroupDO>(id = id) {

    override fun copyFromMinimal(src: GroupDO) {
        super.copyFromMinimal(src)
        name = src.name
    }
}
