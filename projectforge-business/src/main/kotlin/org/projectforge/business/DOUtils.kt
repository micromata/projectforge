package org.projectforge.business

import org.projectforge.business.address.AddressbookDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.TenantDO

/**
 * Convenient classes for working with DO inside Kotlin code...
 */
class DOUtils {
    companion object {
        /**
         * Makes a minimal clone of tenant: name
         * @see copyMinimal
         */
        fun cloneMinimal(orig : TenantDO) : TenantDO {
            val newObj = TenantDO()
            copyMinimal(orig, newObj)
            newObj.name = orig.name
            return newObj
        }

        /**
         * Makes a minimal clone of addressbook.
         * @see copyMinimal
         */
        fun cloneMinimal(orig : AddressbookDO) : AddressbookDO {
            val newObj = AddressbookDO()
            copyMinimal(orig, newObj)
            newObj.title = orig.title
            return newObj
        }

        /**
         * Copies minimal properties: pk, isDeleted
         * @param orig
         * @param dest
         */
        fun copyMinimal(orig : DefaultBaseDO, dest: DefaultBaseDO) {
            dest.pk = orig.pk
            dest.isDeleted = orig.isDeleted
        }
    }
}