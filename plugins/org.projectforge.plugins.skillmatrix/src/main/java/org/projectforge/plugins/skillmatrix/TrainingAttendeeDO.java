/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * This data object is the Java representation of a data-base entry of attendee.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_SKILL_TRAINING_ATTENDEE",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_attendee_attendee_fk",
            columnList = "attendee_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_attendee_training_fk",
            columnList = "training_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_skill_training_attendee_tenant_id", columnList = "tenant_id")
    })
public class TrainingAttendeeDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final long serialVersionUID = -3676402473986512186L;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.attendee.menu")
  @UserPrefParameter(i18nKey = "plugins.skillmatrix.skilltraining.attendee.menu")
  @IndexedEmbedded(depth = 1)
  private PFUserDO attendee;

  @PropertyInfo(i18nKey = "plugins.skillmatrix.skilltraining.training")
  @UserPrefParameter(i18nKey = "plugins.skillmatrix.skilltraining.training")
  @IndexedEmbedded(depth = 1)
  private TrainingDO training;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attendee_fk")
  public PFUserDO getAttendee()
  {
    return attendee;
  }

  @Transient
  public Integer getAttendeeId()
  {
    return attendee != null ? attendee.getId() : null;
  }

  /**
   * @param skill
   * @return this for chaining.
   */
  public TrainingAttendeeDO setAttendee(final PFUserDO attendee)
  {
    this.attendee = attendee;
    return this;
  }

  @Transient
  public Integer getTrainingId()
  {
    return training != null ? training.getId() : null;
  }

  /**
   * @param skill
   * @return this for chaining.
   */
  public TrainingAttendeeDO setTraining(final TrainingDO training)
  {
    this.training = training;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "training_fk")
  public TrainingDO getTraining()
  {
    return training;
  }

  @Column(length = Constants.LENGTH_TEXT)
  public String getDescription()
  {
    return description;
  }

  /**
   * @return this for chaining.
   */
  public TrainingAttendeeDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * @return the rating
   */
  @Column(length = 1000)
  public String getRating()
  {
    return rating;
  }

  /**
   * @param rating the rating to set
   * @return this for chaining.
   */
  public TrainingAttendeeDO setRating(final String rating)
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
  public TrainingAttendeeDO setCertificate(final String certificate)
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
  public TrainingAttendeeDO setStartDate(final Date startDate)
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
  public TrainingAttendeeDO setEndDate(final Date endDate)
  {
    this.endDate = endDate;
    return this;
  }

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return getTraining() != null ? getTraining().getTitle() + " (#" + this.getId() + ")" : " (#" + this.getId() + ")";
  }
}
