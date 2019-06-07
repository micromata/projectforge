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

package org.projectforge.plugins.skillmatrix;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.UserPrefParameter;

import de.micromata.genome.jpa.impl.ATableTruncater;

/**
 * A skill usable for a skill-matrix.
 *
 * @author Billy Duong (b.duong@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_parent_fk", columnList = "parent_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_tenant_id", columnList = "tenant_id")
    })
@ATableTruncater(value = org.projectforge.plugins.skillmatrix.SkillTableTruncater.class)
@AUserRightId(value = "SKILL", checkAccess = false)
public class SkillDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 6102127905651011282L;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.title")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title;

  // Null if this skill is a top level skill.
  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.parent")
  @IndexedEmbedded(depth = 1)
  private SkillDO parent;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.description")
  @UserPrefParameter(i18nKey = "description", multiline = true)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.comment")
  @UserPrefParameter(i18nKey = "comment", multiline = true)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.rateable")
  private boolean rateable = true;

  private String fullAccessGroupIds;
  private String readOnlyAccessGroupIds;
  private String trainingGroupsIds;

  @Column(length = 255)
  public String getTitle()
  {
    return title;
  }

  /**
   * @param title
   * @return this for chaining.
   */
  public SkillDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_fk")
  public SkillDO getParent()
  {
    return parent;
  }

  @Transient
  public Integer getParentId()
  {
    return parent != null ? parent.getId() : null;
  }

  /**
   * @param parent
   * @return this for chaining.
   */
  public SkillDO setParent(final SkillDO parent)
  {
    this.parent = parent;
    return this;
  }

  @Column(length = Constants.LENGTH_TEXT)
  public String getComment()
  {
    return comment;
  }

  /**
   * @return this for chaining.
   */
  public SkillDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  /**
   * This value should be false for skills which should be used as categories or sub categories for which a rating isn't
   * useful. But for some categories is it useful to define them as rateable (e. g. for Programming languages -> Java ->
   * J2EE the skill Java should be rateable).
   */
  @Column
  public boolean isRateable()
  {
    return rateable;
  }

  /**
   * @return this for chaining.
   */
  public SkillDO setRateable(final boolean rateable)
  {
    this.rateable = rateable;
    return this;
  }

  @Column(length = Constants.LENGTH_TEXT)
  public String getDescription()
  {
    return description;
  }

  /**
   * @return this for chaining.
   */
  public SkillDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * Members of these groups have full read/write access to this skill and related trainings.
   *
   * @return the fullAccessGroupIds
   */
  @Column(name = "full_access_group_ids", length = 4000, nullable = true)
  public String getFullAccessGroupIds()
  {
    return fullAccessGroupIds;
  }

  /**
   * These users have full read/write access to this skill.
   *
   * @param fullAccessGroupIds the fullAccessGroupIds to set
   * @return this for chaining.
   */
  public SkillDO setFullAccessGroupIds(final String fullAccessGroupIds)
  {
    this.fullAccessGroupIds = fullAccessGroupIds;
    return this;
  }

  /**
   * Members of these groups have full read-only access to this skill.
   *
   * @return the readOnlyAccessGroupIds
   */
  @Column(name = "readonly_access_group_ids", length = 4000, nullable = true)
  public String getReadOnlyAccessGroupIds()
  {
    return readOnlyAccessGroupIds;
  }

  /**
   * @param readOnlyAccessGroupIds the readOnlyAccessGroupIds to set
   * @return this for chaining.
   */
  public SkillDO setReadOnlyAccessGroupIds(final String readonlyAccessGroupIds)
  {
    this.readOnlyAccessGroupIds = readonlyAccessGroupIds;
    return this;
  }

  /**
   * Members of these groups have full read/write access to trainings of this skill.
   *
   * @return the trainingGroupsIds
   */
  @Column(name = "training_access_group_ids", length = 4000, nullable = true)
  public String getTrainingAccessGroupIds()
  {
    return trainingGroupsIds;
  }

  /**
   * These users have full read/write access to trainings of this skill.
   *
   * @param trainingGroupsIds the trainingGroupsIds to set
   * @return this for chaining.
   */
  public SkillDO setTrainingAccessGroupIds(final String trainingGroupsIds)
  {
    this.trainingGroupsIds = trainingGroupsIds;
    return this;
  }
}
