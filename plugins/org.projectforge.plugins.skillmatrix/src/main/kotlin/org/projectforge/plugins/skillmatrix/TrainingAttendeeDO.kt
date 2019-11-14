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

package org.projectforge.plugins.skillmatrix

import org.hibernate.search.annotations.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.sql.Date
import javax.persistence.*

/**
 * This data object is the Java representation of a data-base entry of attendee.
 *
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL_TRAINING_ATTENDEE", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_attendee_attendee_fk", columnList = "attendee_fk"), javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_attendee_training_fk", columnList = "training_fk"), javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_attendee_tenant_id", columnList = "tenant_id")])
open class TrainingAttendeeDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.attendee.menu")
    @UserPrefParameter(i18nKey = "plugins.skillmatrix.skilltraining.attendee.menu")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "attendee_fk")
    open var attendee: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.training")
    @UserPrefParameter(i18nKey = "plugins.skillmatrix.skilltraining.training")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "training_fk")
    open var training: TrainingDO? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.description")
    @UserPrefParameter(i18nKey = "description", multiline = true)
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.rating")
    @Field
    @get:Column(length = 1000)
    open var rating: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.certificate")
    @Field
    @get:Column(length = 4000)
    open var certificate: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.startDate")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "start_date")
    open var startDate: Date? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.endDate")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "end_date")
    open var endDate: Date? = null

    val attendeeId: Int?
        @Transient
        get() = if (attendee != null) attendee!!.id else null

    val trainingId: Int?
        @Transient
        get() = if (training != null) training!!.id else null

    @Transient
    override fun getShortDisplayName(): String {
        return if (training != null) training!!.title + " (#" + this.id + ")" else " (#" + this.id + ")"
    }
}
