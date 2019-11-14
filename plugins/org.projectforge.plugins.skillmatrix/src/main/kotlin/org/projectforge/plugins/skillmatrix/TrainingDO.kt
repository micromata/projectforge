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

import org.apache.commons.lang3.StringUtils
import org.hibernate.search.annotations.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import java.sql.Date
import javax.persistence.*

/**
 * This data object is the Java representation of a data-base entry of a training.
 *
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL_TRAINING",
        indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_skill_fk", columnList = "skill_fk"),
            javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_tenant_id", columnList = "tenant_id")])
@NamedQueries(
        NamedQuery(name = TrainingDO.FIND_BY_TITLE, query = "from TrainingDO where title=:title"))
open class TrainingDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.title")
    @Field
    @get:Column(length = 255)
    open var title: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.skill")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "skill_fk")
    open var skill: SkillDO? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.description")
    @UserPrefParameter(i18nKey = "description", multiline = true)
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.rating")
    @Field
    @get:Column(length = 255)
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

    /**
     * Members of these groups have full read/write access to this training.
     */
    @get:Column(name = "full_access_group_ids", length = 4000, nullable = true)
    open var fullAccessGroupIds: String? = null

    /**
     * Members of these groups have full read-only access to this training.
     */
    @get:Column(name = "readonly_access_group_ids", length = 4000, nullable = true)
    open var readOnlyAccessGroupIds: String? = null

    val skillId: Int?
        @Transient
        get() = if (skill != null) skill!!.id else null

    val ratingArray: Array<String>?
        @Transient
        get() = getValuesArray(rating)

    val certificateArray: Array<String>?
        @Transient
        get() = getValuesArray(certificate)

    @Transient
    override fun getShortDisplayName(): String {
        return this.title + " (#" + this.id + ")"
    }

    companion object {
        internal const val FIND_BY_TITLE = "TrainingDO_FindByTitle"

        @Transient
        fun getValuesArray(values: String?): Array<String>? {
            if (StringUtils.isBlank(values)) {
                return null
            }
            val sar = StringUtils.split(values, ";")
            for (i in sar.indices) {
                sar[i] = StringUtils.trim(sar[i])
            }
            return sar
        }
    }
}
