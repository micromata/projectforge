package org.projectforge.rest.dto

import org.projectforge.framework.persistence.user.entities.PFUserDO

class User(id: Int? = null,
           var username: String? = null,
           var firstname: String? = null,
           var lastname: String? = null,
           /**
            * Only for displaying purposes. Will be ignored on save or update.
            */
           var fullname: String? = null,
           var description: String? = null,
           var email: String? = null
) : BaseObject<PFUserDO>(id = id) {

    override fun copyFromMinimal(src: PFUserDO) {
        super.copyFromMinimal(src)
        username = src.username
        fullname = src.getFullname()
    }
}
