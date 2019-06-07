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

package org.projectforge.business.teamcal.event.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.annotations.Indexed;
import org.projectforge.business.address.AddressDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_CALENDAR_EVENT_ATTENDEE",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_team_event_fk",
            columnList = "team_event_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_address_id",
            columnList = "address_id"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_user_id", columnList = "user_id"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attendee_tenant_id", columnList = "tenant_id")
    })
@WithHistory(noHistoryProperties = "loginToken")
public class TeamEventAttendeeDO extends DefaultBaseDO implements Comparable<TeamEventAttendeeDO>
{
  private static final long serialVersionUID = -3293247578185393730L;

  private Short number;

  private String url;

  private AddressDO address;

  private PFUserDO user;

  private String loginToken;

  private TeamEventAttendeeStatus status = TeamEventAttendeeStatus.NEEDS_ACTION;

  private String comment;

  private String commentOfAttendee;

  private String commonName;
  private String cuType;
  private Boolean rsvp;
  private String role;
  private String additionalParams;

  private static final Set<String> NON_HISTORIZABLE_ATTRIBUTES;

  public static final int URL_MAX_LENGTH = 255;

  static {
    NON_HISTORIZABLE_ATTRIBUTES = new HashSet<String>();
    NON_HISTORIZABLE_ATTRIBUTES.add("loginToken");
  }

  @Transient
  public String getEMailAddress()
  {
    if (address != null) {
      return address.getEmail();
    } else if (user != null) {
      return user.getEmail();
    }
    return null;
  }

  /**
   * Is set if the attendee is a ProjectForge user.
   *
   * @return the userId
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "address_id")
  public AddressDO getAddress()
  {
    return address;
  }

  /**
   * @param address the address to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setAddress(final AddressDO address)
  {
    this.address = address;
    return this;
  }

  @Transient
  public Integer getAddressId()
  {
    if (this.address == null) {
      return null;
    }
    return address.getId();
  }

  /**
   * Is set if the attendee is a ProjectForge user.
   *
   * @return the userId
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  @Transient
  public Integer getUserId()
  {
    if (this.user == null) {
      return null;
    }
    return user.getId();
  }

  /**
   * Is used if the attendee isn't a ProjectForge user for authentication.
   *
   * @return the loginToken
   */
  @Column(name = "login_token", length = 255)
  public String getLoginToken()
  {
    return loginToken;
  }

  /**
   * @param loginToken the loginToken to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setLoginToken(final String loginToken)
  {
    this.loginToken = loginToken;
    return this;
  }

  /**
   * The url (mail) of the attendee. Isn't used if the attendee is a ProjectForge user.
   *
   * @return the url
   */
  @Column(length = URL_MAX_LENGTH)
  public String getUrl()
  {
    return url;
  }

  /**
   * @param url the url to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setUrl(final String url)
  {
    this.url = url;
    return this;
  }

  /**
   * @return the status
   */
  @Enumerated(EnumType.STRING)
  @Column(length = 100)
  public TeamEventAttendeeStatus getStatus()
  {
    return status;
  }

  /**
   * @param status the status to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setStatus(final TeamEventAttendeeStatus status)
  {
    this.status = status;
    return this;
  }

  /**
   * @return the comment
   */
  @Column(length = 4000)
  public String getComment()
  {
    return comment;
  }

  /**
   * @param comment the comment to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  /**
   * @return the commentOfAttendee
   */
  @Column(length = 4000, name = "comment_of_attendee")
  public String getCommentOfAttendee()
  {
    return commentOfAttendee;
  }

  /**
   * @param commentOfAttendee the commentOfAttendee to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setCommentOfAttendee(final String commentOfAttendee)
  {
    this.commentOfAttendee = commentOfAttendee;
    return this;
  }

  /**
   * @return the number
   */
  @Column
  public Short getNumber()
  {
    return number;
  }

  /**
   * @param number the number to set
   * @return this for chaining.
   */
  public TeamEventAttendeeDO setNumber(final Short number)
  {
    this.number = number;
    return this;
  }

  @Column(length = 256, name = "common_name")
  public String getCommonName()
  {
    return commonName;
  }

  public TeamEventAttendeeDO setCommonName(final String commonName)
  {
    this.commonName = commonName;
    return this;
  }

  @Column(length = 20, name = "cu_type")
  public String getCuType()
  {
    return cuType;
  }

  public TeamEventAttendeeDO setCuType(final String cuType)
  {
    this.cuType = cuType;
    return this;
  }

  @Column
  public Boolean getRsvp()
  {
    return rsvp;
  }

  public TeamEventAttendeeDO setRsvp(final Boolean rscp)
  {
    this.rsvp = rscp;
    return this;
  }

  @Column
  public String getRole()
  {
    return role;
  }

  public TeamEventAttendeeDO setRole(final String role)
  {
    this.role = role;

    return this;
  }

  @Column(length = 1000, name = "additional_params")
  public String getAdditionalParams()
  {
    return additionalParams;
  }

  public TeamEventAttendeeDO setAdditionalParams(final String additionalParams)
  {
    this.additionalParams = additionalParams;
    return this;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final TeamEventAttendeeDO arg0)
  {
    if (this.getId() != null && Objects.equals(this.getId(), arg0.getId()) == true) {
      return 0;
    }
    return this.toString().toLowerCase().compareTo(arg0.toString().toLowerCase());
  }

  /**
   * Equals / HashCode contract is broken because of technical requirements
   *
   * @return
   */
  @Override
  public int hashCode()
  {
    int result = 0;
    if (url != null) {
      return 31 * url.hashCode();
    }
    if (address != null && address.getPk() != null) {
      return 31 * address.getPk().hashCode();
    }
    if (user != null && user.getPk() != null) {
      return 31 * user.getPk().hashCode();
    }
    if (getPk() != null) {
      return 31 * getPk().hashCode();
    }
    return result;
  }

  /**
   * Equals / HashCode contract is broken because of technical requirements
   *
   * @return
   */
  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof TeamEventAttendeeDO == false) {
      return false;
    }
    final TeamEventAttendeeDO other = (TeamEventAttendeeDO) o;
    if (this.getUrl() != null && other.getUrl() != null && StringUtils.equals(this.getUrl(), other.getUrl())) {
      return true;
    }
    if (this.getAddressId() != null && other.getAddressId() != null && Objects.equals(this.getAddressId(), other.getAddressId())) {
      return true;
    }
    if (this.getUserId() != null && other.getUserId() != null && Objects.equals(this.getUserId(), other.getUserId())) {
      return true;
    }
    if (this.getPk() != null && other.getPk() != null && Objects.equals(this.getPk(), other.getPk())) {
      return true;
    }
    if (this.getUrl() == null && other.getUrl() == null && this.getAddressId() == null && other.getAddressId() == null && this.getUserId() == null
        && other.getUserId() == null && this.getPk() == null && other.getPk() == null) {
      return true;
    }
    return false;
  }

  @Override
  public String toString()
  {
    if (this.getUser() != null) {
      return this.getUser().getFullname() + " (" + this.getUser().getEmail() + ")";
    }
    if (this.getAddress() != null) {
      return this.getAddress().getFullName() + " (" + this.getAddress().getEmail() + ")";
    }
    if (this.getUrl() != null) {
      return this.getUrl();
    }
    return super.toString();
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public TeamEventAttendeeDO clone()
  {
    TeamEventAttendeeDO cloneAttendee = new TeamEventAttendeeDO();
    cloneAttendee.setAddress(this.getAddress());
    cloneAttendee.setComment(this.getComment());
    cloneAttendee.setCommentOfAttendee(this.getCommentOfAttendee());
    cloneAttendee.setLoginToken(this.getLoginToken());
    cloneAttendee.setNumber(this.getNumber());
    cloneAttendee.setStatus(this.getStatus());
    cloneAttendee.setUrl(this.getUrl());
    cloneAttendee.setUser(this.getUser());
    cloneAttendee.setCommonName(this.getCommonName());
    cloneAttendee.setCuType(this.getCuType());
    cloneAttendee.setRsvp(this.getRsvp());
    cloneAttendee.setAdditionalParams(this.getAdditionalParams());
    cloneAttendee.setRole(this.getRole());

    return cloneAttendee;
  }
}
