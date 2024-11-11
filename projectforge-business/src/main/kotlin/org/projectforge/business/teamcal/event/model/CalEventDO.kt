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

package org.projectforge.business.teamcal.event.model

import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.Constants
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.util.*

@Entity
@Indexed
@Table(
    name = "T_CALENDAR_EVENT", uniqueConstraints = [UniqueConstraint(
        name = "unique_t_calendar_event_uid_calendar_fk", columnNames = ["uid", "calendar_fk"]
    )]
) //@WithHistory(noHistoryProperties = {"lastUpdate", "created"}, nestedEntities = {TeamEventAttendeeDO.class})
@AUserRightId(value = "PLUGIN_CALENDAR_EVENT")
open class CalEventDO : DefaultBaseDO(), ICalendarEvent {
    /**
     * @return the calendar
     */
    /**
     * @param calendar the calendar to set
     * @return this for chaining.
     */
    @get:JoinColumn(name = "calendar_fk", nullable = false)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    var calendar: TeamCalDO? = null

    /**
     * @return the startDate
     */
    /**
     * @param startDate the startDate to set
     */
    @get:Column(name = "start_date")
    @PropertyInfo(i18nKey = "plugins.teamcal.event.beginDate")
    @GenericField
    override // was: @Field(index = Index.YES, analyze = Analyze.NO) @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    var startDate: Date? = null

    /**
     * @return the endDate
     */
    /**
     * @param endDate the endDate to set
     */
    @get:Column(name = "end_date")
    @PropertyInfo(i18nKey = "plugins.teamcal.event.endDate")
    @GenericField
    override // was: @Field(index = Index.YES, analyze = Analyze.NO) @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    var endDate: Date? = null

    /**
     * Loads or creates the team event uid. Its very important that the uid is always the same in every ics file, which is
     * created. So only one time creation.
     */
    /**
     * @param uid
     */
    @get:Column(nullable = false)
    override var uid: String? = null

    /**
     * @param subject
     */
    @get:Column(length = Constants.LENGTH_SUBJECT)
    @PropertyInfo(i18nKey = "plugins.teamcal.event.subject")
    @FullTextField
    override var subject: String? = null

    /**
     * @return the location
     */
    /**
     * @param location the location to set
     */
    @get:Column(length = Constants.LENGTH_SUBJECT)
    @PropertyInfo(i18nKey = "plugins.teamcal.event.location")
    @FullTextField
    override var location: String? = null

    /**
     * @param note the note to set
     * @return this for chaining.
     */
    @get:Column(length = 4000)
    @PropertyInfo(i18nKey = "plugins.teamcal.event.note")
    @FullTextField
    override var note: String? = null

    /**
     * @return the ics
     */
    /**
     * @param icsData the icsData to set
     */
    @get:Column(name = "ics", length = 10000)
    var icsData: String? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.event.allDay")
    override var allDay: Boolean = false

    private val recurrence = false

    fun hasRecurrence(): Boolean {
        return false
    }
}
