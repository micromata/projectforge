package org.projectforge.rest.dto

import org.projectforge.business.address.AddressbookDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class Addressbook(var title: String? = null,
                  var owner: User? = null,
                  var description: String? = null) : BaseObject<AddressbookDO>() {
    override fun copyFromMinimal(src: AddressbookDO) {
        super.copyFromMinimal(src)
        title = src.title
        val srcOwner = src.owner
        if (srcOwner != null) {
            owner = User()
            owner?.copyFromMinimal(srcOwner)
        }
    }

    override fun copyTo(dest: AddressbookDO) {
        super.copyTo(dest)
        dest.title = title
        if (owner != null) {
            dest.owner = PFUserDO()
            owner?.copyTo(dest.owner!!)
        }
    }
}
