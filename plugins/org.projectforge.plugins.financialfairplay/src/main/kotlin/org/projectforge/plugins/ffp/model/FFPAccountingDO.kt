/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.ffp.model

import de.micromata.genome.db.jpa.history.api.WithHistory
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import javax.persistence.*

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_ACCOUNTING")
@WithHistory
open class FFPAccountingDO : DefaultBaseDO() {

    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "EVENT_ID")
    open var event: FFPEventDO? = null

    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "ATTENDEE_USER_ID")
    open var attendee: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.ffp.value")
    @get:Column(nullable = false)
    open var value: BigDecimal? = null

    @PropertyInfo(i18nKey = "plugins.ffp.weighting")
    @get:Column(nullable = false)
    open var weighting: BigDecimal? = null

    @PropertyInfo(i18nKey = "plugins.ffp.comment")
    @get:Column
    open var comment: String? = null

    override fun equals(other: Any?): Boolean {
        if (other !is FFPAccountingDO) {
            return false
        }
        val o = other as FFPAccountingDO?
        if (this.id != null && o!!.id != null) {
            return this.id == o.id
        }
        if (this.event != null && this.event!!.id != null && o!!.event != null && o.event!!.id != null
                && this.attendee != null && o.attendee != null) {
            return this.event!!.id == o.event!!.id && this.attendee!!.id == o.attendee!!.id
        }
        //Case new event
        return if (this.attendee != null && o!!.attendee != null) {
            this.attendee!!.id == o.attendee!!.id
        } else false
    }

    override fun hashCode(): Int {
        var result = if (id != null) id!!.hashCode() else 0
        result = 31 * result + if (event != null && event!!.id != null) event!!.id!!.hashCode() else 0
        result = 31 * result + if (attendee != null) attendee!!.id!!.hashCode() else 0
        return result
    }
}
