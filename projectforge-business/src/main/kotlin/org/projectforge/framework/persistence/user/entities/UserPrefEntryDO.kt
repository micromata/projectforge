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

package org.projectforge.framework.persistence.user.entities

import jakarta.persistence.*
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.entities.AbstractBaseDO.Companion.copyValues
import org.projectforge.framework.utils.NumberHelper.parseInteger
import java.io.Serializable

/**
 * Represents a single generic user preference entry.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(
    name = "T_USER_PREF_ENTRY",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_pref_fk", "parameter"])],
    indexes = [Index(name = "idx_fk_t_user_pref_entry_user_pref_fk", columnList = "user_pref_fk")]
)
class UserPrefEntryDO : BaseDO<Int>, Serializable {
    @get:Column(length = 255)
    var parameter: String? = null // 255 not null

    @get:Column(name = "s_value", length = MAX_STRING_VALUE_LENGTH)
    var value: String? = null // MAX_STRING_VALUE_LENGTH

    /**
     * The entries will be ordered by this property. This field is not persisted.
     */
    @get:Transient
    var orderString: String? = null

    /**
     * For displaying paramter's localized label (if given). This field is not persisted.
     *
     * @see UserPrefParameter.i18nKey
     */
    @get:Transient
    var i18nKey: String? = null

    @get:Transient
    var tooltipI18nKey: String? = null

    @get:Transient
    var dependsOn: String? = null

    /**
     * Type of parameter value (if given). This field is not persisted.
     */
    @get:Transient
    var type: Class<*>? = null

    /**
     * Value as object, if given. This field is not persisted.
     */
    @get:Transient
    var valueAsObject: Any? = null

    /**
     * This field is not persisted.
     *
     * @see UserPrefParameter.required
     */
    @get:Transient
    var isRequired: Boolean = false

    /**
     * This field is not persisted.
     */
    @get:Transient
    var maxLength: Int? = null

    /**
     * This field is not persisted.
     *
     * @see UserPrefParameter.multiline
     */
    @get:Transient
    var isMultiline: Boolean = false

    @get:Column(name = "pk")
    @get:GeneratedValue
    @get:Id
    override var id: Int? = null

    @get:Transient
    val valueAsInteger: Int?
        get() = parseInteger(value)

    @get:Transient
    override var isMinorChange: Boolean
        /**
         * @return Always true.
         * @see org.projectforge.framework.persistence.api.BaseDO.isMinorChange
         */
        get() = false
        /**
         * Throws UnsupportedOperationException.
         *
         * @see org.projectforge.framework.persistence.api.BaseDO.setMinorChange
         */
        set(value) {
            throw UnsupportedOperationException()
        }

    override fun equals(o: Any?): Boolean {
        if (o is UserPrefEntryDO) {
            val other = o
            if (this.parameter != other.parameter) {
                return false
            }
            if (this.id != other.id) {
                return false
            }
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(parameter)
        hcb.append(id)
        return hcb.toHashCode()
    }

    override fun toString(): String {
        val sb = ToStringBuilder(this)
        sb.append("id", id)
        sb.append("parameter", this.parameter)
        sb.append("value", this.value)
        return sb.toString()
    }

    /**
     * @param src
     * @see AbstractBaseDO.copyValues
     */
    override fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): EntityCopyStatus {
        return copyValues(src, this, *ignoreFields)
    }

    override fun getTransientAttribute(key: String): Any? {
        throw UnsupportedOperationException()
    }

    override fun removeTransientAttribute(key: String): Any? {
        throw UnsupportedOperationException()
    }

    override fun setTransientAttribute(key: String, value: Any?) {
        throw UnsupportedOperationException()
    }

    companion object {
        private const val serialVersionUID = 7163902159871289059L

        const val MAX_STRING_VALUE_LENGTH: Int = 10000
    }
}
