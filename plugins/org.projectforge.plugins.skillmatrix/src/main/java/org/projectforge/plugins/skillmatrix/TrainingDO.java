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

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.UserPrefParameter;

/**
 * This data object is the Java representation of a data-base entry of a training.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL_TRAINING",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_skill_fk", columnList = "skill_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_tenant_id", columnList = "tenant_id")
    })
public class TrainingDO extends DefaultBaseDO implements ShortDisplayNameCapable
{

  private static final long serialVersionUID = -3671964174762366962L;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.title")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skillrating.skill")
  @IndexedEmbedded(depth = 1)
  private SkillDO skill;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skill.description")
  @UserPrefParameter(i18nKey = "description", multiline = true)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.rating")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String rating;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.certificate")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String certificate;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.startDate")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date startDate;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.endDate")
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date endDate;

  private String fullAccessGroupIds, readOnlyAccessGroupIds;

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return this.getTitle() + " (#" + this.getId() + ")";
  }

  @Column(length = 255)
  public String getTitle()
  {
    return title;
  }

  /**
   * @param title
   * @return this for chaining.
   */
  public TrainingDO setTitle(final String title)
  {
    this.title = title;
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
   * @param skill
   * @return this for chaining.
   */
  public TrainingDO setSkill(final SkillDO skill)
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
  public TrainingDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * @return the rating
   */
  @Column(length = 255)
  public String getRating()
  {
    return rating;
  }

  /**
   * @param rating the rating to set
   * @return this for chaining.
   */
  public TrainingDO setRating(final String rating)
  {
    this.rating = rating;
    return this;
  }

  /**
   * @return the certificate
   */
  @Column(length = 4000)
  public String getCertificate()
  {
    return certificate;
  }

  /**
   * @param certificate the certificate to set
   * @return this for chaining.
   */
  public TrainingDO setCertificate(final String certificate)
  {
    this.certificate = certificate;
    return this;
  }

  @Column(name = "start_date")
  public Date getStartDate()
  {
    return startDate;
  }

  /**
   * @return this for chaining.
   */
  public TrainingDO setStartDate(final Date startDate)
  {
    this.startDate = startDate;
    return this;
  }

  @Column(name = "end_date")
  public Date getEndDate()
  {
    return endDate;
  }

  /**
   * @return this for chaining.
   */
  public TrainingDO setEndDate(final Date endDate)
  {
    this.endDate = endDate;
    return this;
  }

  @Transient
  protected static String[] getValuesArray(final String values)
  {
    if (StringUtils.isBlank(values) == true) {
      return null;
    }
    final String[] sar = StringUtils.split(values, ";");
    for (int i = 0; i < sar.length; i++) {
      sar[i] = StringUtils.trim(sar[i]);
    }
    return sar;
  }

  @Transient
  public String[] getRatingArray()
  {
    return getValuesArray(getRating());
  }

  @Transient
  public String[] getCertificateArray()
  {
    return getValuesArray(getCertificate());
  }

  /**
   * Members of these groups have full read/write access to this training.
   * 
   * @return the fullAccessGroupIds
   */
  @Column(name = "full_access_group_ids", length = 4000, nullable = true)
  public String getFullAccessGroupIds()
  {
    return fullAccessGroupIds;
  }

  /**
   * These users have full read/write access to this training.
   * 
   * @param fullAccessGroupIds the fullAccessGroupIds to set
   * @return this for chaining.
   */
  public TrainingDO setFullAccessGroupIds(final String fullAccessGroupIds)
  {
    this.fullAccessGroupIds = fullAccessGroupIds;
    return this;
  }

  /**
   * Members of these groups have full read-only access to this training.
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
  public TrainingDO setReadOnlyAccessGroupIds(final String readOnlyAccessGroupIds)
  {
    this.readOnlyAccessGroupIds = readOnlyAccessGroupIds;
    return this;
  }
}
