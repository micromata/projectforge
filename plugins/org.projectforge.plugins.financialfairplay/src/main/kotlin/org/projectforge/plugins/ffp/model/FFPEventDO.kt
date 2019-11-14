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
import org.hibernate.search.annotations.DateBridge
import org.hibernate.search.annotations.IndexedEmbedded
import org.hibernate.search.annotations.Resolution
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "T_PLUGIN_FINANCIALFAIRPLAY_EVENT")
@WithHistory
@AUserRightId(value = "FFP_EVENT", checkAccess = false)
open class FFPEventDO : DefaultBaseDO() {

    /**
     * The organizer.
     *
     * @return the user
     */
    /**
     * @param organizer the organizer to set
     */
    @PropertyInfo(i18nKey = "plugins.ffp.organizer")
    @IndexedEmbedded(includePaths = ["firstname", "lastname"])
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "organizer_user_id")
    open var organizer: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.ffp.title")
    @get:Column(nullable = false, length = 1000)
    open var title: String? = null

    @PropertyInfo(i18nKey = "plugins.ffp.eventDate")
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Temporal(TemporalType.DATE)
    @get:Column(nullable = false)
    open var eventDate: Date? = null

    // TODO: Set not supported
    @PropertyInfo(i18nKey = "plugins.ffp.attendees")
    open var attendeeList: MutableSet<PFUserDO>? = null
        @ManyToMany
        @JoinTable(name = "T_PLUGIN_FINANCIALFAIRPLAY_EVENT_ATTENDEE", joinColumns = [JoinColumn(name = "EVENT_PK", referencedColumnName = "PK")], inverseJoinColumns = [JoinColumn(name = "ATTENDEE_USER_PK", referencedColumnName = "PK")])
        get() {
            if (field == null) {
                this.attendeeList = HashSet()
            }
            return field
        }

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    open var accountingList: Set<FFPAccountingDO>? = null
        @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, mappedBy = "event", orphanRemoval = true)
        get() {
            if (field == null) {
                this.accountingList = HashSet()
            }
            return field
        }

    @get:Column
    open var finished: Boolean = false

    @PropertyInfo(i18nKey = "plugins.ffp.commonDebtValue")
    @get:Column
    open var commonDebtValue: BigDecimal? = null

    val status: String
        @Transient
        get() = if (finished) {
            I18nHelper.getLocalizedMessage("plugins.ffp.status.closed")
        } else {
            I18nHelper.getLocalizedMessage("plugins.ffp.status.open")
        }

    @Transient
    fun addAttendee(attendee: PFUserDO) {
        attendeeList?.add(attendee)
    }
}
