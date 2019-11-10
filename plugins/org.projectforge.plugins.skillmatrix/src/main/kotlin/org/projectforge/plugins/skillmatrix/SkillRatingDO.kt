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

import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.*

/**
 * A skill usable for a skill matrix.
 *
 * @author Billy Duong (b.duong@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL_RATING",
        indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_skill_rating_skill_fk", columnList = "skill_fk"),
            javax.persistence.Index(name = "idx_fk_t_plugin_skill_rating_user_fk", columnList = "user_fk"),
            javax.persistence.Index(name = "idx_fk_t_plugin_skill_rating_tenant_id", columnList = "tenant_id")])
@NamedQueries(
        NamedQuery(name = SkillRatingDO.FIND_BY_USER_AND_SKILL,
                query = "from SkillRatingDO where user.id=:userId and skill.id=:skillId"),
        NamedQuery(name = SkillRatingDO.FIND_OTHER_BY_USER_AND_SKILL,
                query = "from SkillRatingDO where user.id=:userId and skill.id=:skillId and id!=:id"))
open class SkillRatingDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.user")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk")
    open var user: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.skill")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "skill_fk")
    open var skill: SkillDO? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.sinceyear")
    @Field(analyze = Analyze.NO)
    @get:Column(name = "since_year")
    open var sinceYear: Int? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.rating")
    @Enumerated(EnumType.STRING)
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 15, name = "skill_rating")
    open var skillRating: SkillRating? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.certificates")
    @Field
    @get:Column(length = 4000)
    open var certificates: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.trainingcourses")
    @Field
    @get:Column(length = 4000)
    open var trainingCourses: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.description")
    @UserPrefParameter(i18nKey = "description", multiline = true)
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.comment")
    @UserPrefParameter(i18nKey = "comment", multiline = true)
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    val userId: Int?
        @Transient
        get() = if (user != null) user!!.id else null

    val skillId: Int?
        @Transient
        get() = if (skill != null) skill!!.id else null

    companion object {
        internal const val FIND_BY_USER_AND_SKILL = "SkillRatingDO_FindByUserAndSkill"
        internal const val FIND_OTHER_BY_USER_AND_SKILL = "SkillRatingDO_FindOtherByUserAndSkill"
    }
}
