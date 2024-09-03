/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.persistence.entities

import org.apache.commons.lang3.ClassUtils
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.ToStringUtil.Companion.toJsonString
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.projectforge.framework.persistence.history.NoHistory

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@MappedSuperclass
abstract class AbstractBaseDO<I : Serializable> : ExtendedBaseDO<I>, Serializable {
    @get:Basic
    @PropertyInfo(i18nKey = "deleted")
    override var deleted: Boolean = false

    @NoHistory
    @get:PropertyInfo(i18nKey = "created")
    @get:Column
    override var created: Date? = null

    @NoHistory
    @PropertyInfo(i18nKey = "modified")
    @get:Column(name = "last_update")
    override var lastUpdate: Date? = null

    /**
     * Default value is false.
     *
     * @see org.projectforge.framework.persistence.api.BaseDO.isMinorChange
     */
    @get:jakarta.persistence.Transient
    @Transient
    override var isMinorChange: Boolean = false

    @Transient
    private var attributeMap: MutableMap<String?, Any?>? = null

    /**
     * If any re-calculations have to be done before displaying, indexing etc. This method have an implementation if a
     * data object has transient fields which are calculated by other fields. This default implementation does nothing.
     */
    override fun recalculate() {
    }

    override fun getTransientAttribute(key: String): Any? {
        if (attributeMap == null) {
            return null
        }
        return attributeMap!![key]
    }

    override fun removeTransientAttribute(key: String): Any? {
        val obj = getTransientAttribute(key)
        if (obj != null) {
            attributeMap!!.remove(key)
        }
        return obj
    }

    override fun setTransientAttribute(key: String, value: Any?) {
        if (attributeMap == null) {
            attributeMap = HashMap()
        }
        attributeMap!![key] = value
    }

    /**
     * as json.
     */
    override fun toString(): String {
        return toJsonString(this)
    }

    /**
     * Copies all values from the given src object excluding the values created and lastUpdate. Do not overwrite created
     * and lastUpdate from the original database object.
     *
     * @param src
     * @param ignoreFields Does not copy these properties (by field name).
     * @return true, if any modifications are detected, otherwise false;
     */
    override fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): EntityCopyStatus {
        return copyValues(src, this, *ignoreFields)
    }

    companion object {
        private const val serialVersionUID = -2225460450662176301L

        private val log: Logger = LoggerFactory.getLogger(AbstractBaseDO::class.java)

        /**
         * Copies all values from the given src object excluding the values created and lastUpdate, if already existed
         * in the dest object. Do not overwrite created
         * and lastUpdate from the original database object.
         *
         * @param src
         * @param dest
         * @param ignoreFields Does not copy these properties (by field name).
         * @return true, if any modifications are detected, otherwise false;
         */
        @JvmStatic
        fun copyValues(src: BaseDO<*>, dest: BaseDO<*>, vararg ignoreFields: String?): EntityCopyStatus {
            if (dest is AbstractBaseDO<*> && src is AbstractBaseDO<*>) {
                val srcObj = src
                val destObj = dest
                val created = destObj.created
                val lastUpdate = destObj.lastUpdate
                destObj.created = srcObj.created
                destObj.lastUpdate = srcObj.lastUpdate
                val modificationStatus = BaseDaoJpaAdapter.copyValues(src, dest, *ignoreFields)
                // Preserve original dest values:
                if (created != null) {
                    destObj.created = created
                }
                if (lastUpdate != null) {
                    destObj.lastUpdate = lastUpdate
                }
                return modificationStatus
            }
            return BaseDaoJpaAdapter.copyValues(src, dest, *ignoreFields)
        }

        @JvmStatic
        @Deprecated("")
        fun getModificationStatus(
            currentStatus: EntityCopyStatus,
            status: EntityCopyStatus
        ): EntityCopyStatus {
            return currentStatus.combine(status)
        }

        /**
         * Returns whether or not to append the given `Field`.
         *
         *  * Ignore transient fields
         *  * Ignore static fields
         *  * Ignore inner class fields
         *
         *
         * @param field The Field to test.
         * @return Whether or not to consider the given `Field`.
         */
        protected fun accept(field: Field): Boolean {
            if (field.name.indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
                // Reject field from inner class.
                return false
            }
            if (Modifier.isTransient(field.modifiers)) {
                // transients.
                return false
            }
            if (Modifier.isStatic(field.modifiers)) {
                // transients.
                return false
            }
            return "created" == field.name != true && "lastUpdate" == field.name != true
        }
    }
}
