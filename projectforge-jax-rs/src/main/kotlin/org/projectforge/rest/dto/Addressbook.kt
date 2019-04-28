package org.projectforge.rest.dto

import org.projectforge.business.address.AddressbookDO

class Addressbook(var title: String? = null,
                  var owner: User? = null,
                  var description: String? = null) : BaseObject<AddressbookDO>() {
    override fun copyFromMinimal(src: AddressbookDO) {
        super.copyFromMinimal(src)
        id = src.id
        title = src.title
        val srcOwner = src.owner
        if (srcOwner != null) {
            owner = User()
            owner?.copyFromMinimal(srcOwner)
        }
    }
}
