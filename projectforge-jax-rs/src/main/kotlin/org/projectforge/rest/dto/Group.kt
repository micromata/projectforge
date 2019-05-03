package org.projectforge.rest.dto

import org.projectforge.framework.persistence.user.entities.GroupDO

class Group(id: Int? = null,
            var name: String? = null
) : BaseObject<GroupDO>(id = id) {

    override fun copyFromMinimal(src: GroupDO) {
        super.copyFromMinimal(src)
        name = src.name
    }
}
