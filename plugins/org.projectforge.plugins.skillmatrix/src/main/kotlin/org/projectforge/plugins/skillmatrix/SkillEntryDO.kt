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

package org.projectforge.plugins.skillmatrix

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.Constants
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.framework.json.IdOnlySerializer

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
  name = "T_PLUGIN_SKILLMATRIX_ENTRY",
  indexes = [jakarta.persistence.Index(name = "idx_fk_t_plugin_skillmatrix_entry_owner_fk", columnList = "owner_fk")]
)
@NamedQueries(
  NamedQuery(
    name = SkillEntryDO.FIND_OF_OWNER,
    query = "from SkillEntryDO where owner.id=:ownerId and deleted=false"
  ),
  NamedQuery(
    name = SkillEntryDO.DELETE_ALL_OF_USER,
    query = "delete from SkillEntryDO where owner.id=:userId"
  ),
)
open class SkillEntryDO : AbstractBaseDO<Long>() {

  @get:Column(name = "pk")
  @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
  @get:Id
  @PropertyInfo(i18nKey = "id")
  override var id: Long? = null

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill")
  @FullTextField
  @get:Column(length = 255, nullable = false)
  open var skill: String? = null

  @get:Transient
  val normalizedSkill: String
    get() = getNormalizedSkill(skill)

  /**
   * Don't index this field (due to privacy protection). No one should filter all skills of one user by simply entering user's name into the
   * search field.
   */
  @PropertyInfo(i18nKey = "plugins.skillmatrix.owner")
  @IndexedEmbedded(includeDepth = 1)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "owner_fk")
  @JsonSerialize(using = IdOnlySerializer::class)
  open var owner: PFUserDO? = null

  /**
   * 1 - basic knowledge, 2 - established knowledge, 3 - expert knowledge
   */
  @PropertyInfo(i18nKey = "plugins.skillmatrix.rating")
  @GenericField
  @get:Column
  open var rating: Int? = null

  /**
   * 1 - interested, 2 - vested interest, 3 - going crazy
   */
  @PropertyInfo(i18nKey = "plugins.skillmatrix.interest")
  @GenericField
  @get:Column
  open var interest: Int? = null

  @PropertyInfo(i18nKey = "comment")
  @FullTextField
  @get:Column(length = Constants.LENGTH_COMMENT)
  open var comment: String? = null

  val ownerId: Long?
    @Transient
    get() = owner?.id

  companion object {
    const val FIND_OF_OWNER = "SkillEntryDO_FindSkillsOfOwner"

    internal const val DELETE_ALL_OF_USER = "SkillEntryDO_DeleteAllOfUser"

    const val MIN_VAL_RATING = 0

    const val MAX_VAL_RATING = 3

    const val MIN_VAL_INTEREST = 0

    const val MAX_VAL_INTEREST = 3

    fun getNormalizedSkill(skill: String?): String {
      return StringHelper.normalize(skill, true)!!
    }
  }
}
