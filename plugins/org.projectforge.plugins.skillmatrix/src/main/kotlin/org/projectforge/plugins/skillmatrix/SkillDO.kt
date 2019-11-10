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

import de.micromata.genome.jpa.impl.ATableTruncater
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import javax.persistence.*

/**
 * A skill usable for a skill-matrix.
 *
 * @author Billy Duong (b.duong@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL",
        indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_skill_parent_fk", columnList = "parent_fk"),
            javax.persistence.Index(name = "idx_fk_t_plugin_skill_tenant_id", columnList = "tenant_id")])
@ATableTruncater(value = org.projectforge.plugins.skillmatrix.SkillTableTruncater::class)
@AUserRightId(value = "SKILL", checkAccess = false)
@NamedQueries(
        NamedQuery(name = SkillDO.FIND_BY_TITLE,
                query = "from SkillDO where title=:title and deleted=false"),
        NamedQuery(name = SkillDO.FIND_BY_TITLE_AND_PARENT,
                query = "from SkillDO where title=:title and deleted=false and parent.id=:parentId"),
        NamedQuery(name = SkillDO.FIND_OTHER_BY_TITLE_AND_PARENT,
                query = "from SkillDO where title=:title and deleted=false and parent.id=:parentId and id=:id"),
        NamedQuery(name = SkillDO.FIND_BY_TITLE_ON_TOPLEVEL,
                query = "from SkillDO where title=:title and deleted=false and parent.id is null"),
        NamedQuery(name = SkillDO.FIND_OTHER_BY_TITLE_ON_TOPLEVEL,
                query = "from SkillDO where title=:title and deleted=false and parent.id is null and id=:id"))
open class SkillDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.title")
    @Field
    @get:Column(length = 255)
    open var title: String? = null

    // Null if this skill is a top level skill.
    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.parent")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "parent_fk")
    open var parent: SkillDO? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.description")
    @UserPrefParameter(i18nKey = "description", multiline = true)
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.comment")
    @UserPrefParameter(i18nKey = "comment", multiline = true)
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    /**
     * This value should be false for skills which should be used as categories or sub categories for which a rating isn't
     * useful. But for some categories is it useful to define them as rateable (e. g. for Programming languages -> Java ->
     * J2EE the skill Java should be rateable).
     */
    @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.rateable")
    @get:Column
    open var rateable = true

    /**
     * Members of these groups have full read/write access to this skill and related trainings.
     */
    @get:Column(name = "full_access_group_ids", length = 4000, nullable = true)
    open var fullAccessGroupIds: String? = null

    /**
     * Members of these groups have full read-only access to this skill.
     */
    @get:Column(name = "readonly_access_group_ids", length = 4000, nullable = true)
    open var readOnlyAccessGroupIds: String? = null

    /**
     * Members of these groups have full read/write access to trainings of this skill.
     */
    @get:Column(name = "training_access_group_ids", length = 4000, nullable = true)
    open var trainingGroupsIds: String? = null

    val parentId: Int?
        @Transient
        get() = if (parent != null) parent!!.id else null

    companion object {
        internal const val FIND_BY_TITLE = "SkillDO_FindByTitle"
        internal const val FIND_BY_TITLE_AND_PARENT = "SkillDO_FindByTitleAndParent"
        internal const val FIND_OTHER_BY_TITLE_AND_PARENT = "SkillDO_FindOtherByTitleAndParent"
        internal const val FIND_BY_TITLE_ON_TOPLEVEL = "SkillDO_FindByTitleOnToplevel"
        internal const val FIND_OTHER_BY_TITLE_ON_TOPLEVEL = "SkillDO_FindOtherByTitleOnToplevel"
    }
}
