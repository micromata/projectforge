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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.UserPrefParameter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * A skill usable for a skill matrix.
 * 
 * @author Billy Duong (b.duong@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL_RATING",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_rating_skill_fk", columnList = "skill_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_rating_user_fk", columnList = "user_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_rating_tenant_id", columnList = "tenant_id")
    })
public class SkillRatingDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 3049488664076249000L;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.user")
  @IndexedEmbedded(depth = 1)
  private PFUserDO user;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.skill")
  @IndexedEmbedded(depth = 1)
  private SkillDO skill;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.sinceyear")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  private Integer sinceYear;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.rating")
  @Enumerated(EnumType.STRING)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private SkillRating skillRating;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.certificates")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String certificates;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.trainingcourses")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String trainingCourses;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.description")
  @UserPrefParameter(i18nKey = "description", multiline = true)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.comment")
  @UserPrefParameter(i18nKey = "comment", multiline = true)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_fk")
  public PFUserDO getUser()
  {
    return user;
  }

  @Transient
  public Integer getUserId()
  {
    return user != null ? user.getId() : null;
  }

  /**
   * @param user
   * @return this for chaining.
   */
  public SkillRatingDO setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_fk")
  public SkillDO getSkill()
  {
    return skill;
  }

  @Transient
  public Integer getSkillId()
  {
    return skill != null ? skill.getId() : null;
  }

  /**
   * @return this for chaining.
   */
  public SkillRatingDO setSkill(final SkillDO skill)
  {
    this.skill = skill;
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
  public SkillRatingDO setDescription(final String description)
  {
    this.description = description;
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
  public SkillRatingDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  @Column(name = "since_year")
  public Integer getSinceYear()
  {
    return sinceYear;
  }

  /**
   * @return this for chaining.
   */
  public SkillRatingDO setSinceYear(final Integer sinceYear)
  {
    this.sinceYear = sinceYear;
    return this;
  }

  @Column(length = 4000)
  public String getCertificates()
  {
    return certificates;
  }

  /**
   * @return this for chaining.
   */
  public SkillRatingDO setCertificates(final String certificates)
  {
    this.certificates = certificates;
    return this;
  }

  @Column(length = 4000)
  public String getTrainingCourses()
  {
    return trainingCourses;
  }

  /**
   * @return this for chaining.
   */
  public SkillRatingDO setTrainingCourses(final String trainingCourses)
  {
    this.trainingCourses = trainingCourses;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 15, name = "skill_rating")
  public SkillRating getSkillRating()
  {
    return skillRating;
  }

  /**
   * @return this for chaining.
   */
  public SkillRatingDO setSkillRating(final SkillRating skillRating)
  {
    this.skillRating = skillRating;
    return this;
  }
}
