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

package org.projectforge.framework.persistence.user.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.*;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Indexed
@Table(name = "T_USER_RIGHT",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_fk", "right_id", "tenant_id"})
        },
        indexes = {
                @javax.persistence.Index(name = "idx_fk_t_user_right_user_fk", columnList = "user_fk"),
                @javax.persistence.Index(name = "idx_fk_t_user_right_tenant_id", columnList = "tenant_id")
        })
@NamedQueries(
        @NamedQuery(name = UserRightDO.FIND_ALL_ORDERED, query = "from UserRightDO order by user.id, rightIdString"))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class UserRightDO extends DefaultBaseDO implements Comparable<UserRightDO>, Serializable, ShortDisplayNameCapable {
  private static final long serialVersionUID = 6703048743393453733L;

  @Field(index = Index.YES, analyze = Analyze.NO)
  private String rightIdString;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  protected UserRightValue value;

  @IndexedEmbedded(depth = 1)
  protected PFUserDO user;

  public UserRightDO() {

  }

  public UserRightDO(final UserRightId rightId) {
    this(null, rightId, null);
  }

  public UserRightDO(final IUserRightId rightId, final UserRightValue value) {
    this(null, rightId, value);
  }

  public UserRightDO(final PFUserDO user, final IUserRightId rightId) {
    this(user, rightId, null);
  }

  public UserRightDO(final PFUserDO user, final IUserRightId rightId, final UserRightValue value) {
    this.user = user;
    this.rightIdString = rightId == null ? null : rightId.getId();
    this.value = value;
  }

  /**
   * Only for storing the right id in the data base.
   */
  @Column(name = "right_id", length = 40, nullable = false)
  public String getRightIdString() {
    return rightIdString;
  }

  public void setRightIdString(String rightIdString) {
    this.rightIdString = rightIdString;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  public UserRightValue getValue() {
    return value;
  }

  public UserRightDO setValue(final UserRightValue value) {
    this.value = value;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_fk", nullable = false)
  public PFUserDO getUser() {
    return user;
  }

  @Transient
  public Integer getUserId() {
    if (this.user == null) {
      return null;
    }
    return user.getId();
  }

  public UserRightDO setUser(final PFUserDO user) {
    this.user = user;
    return this;
  }

  /*
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final UserRightDO o) {
    return this.rightIdString.compareTo(o.rightIdString);
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof UserRightDO) {
      final UserRightDO other = (UserRightDO) o;
      if (!Objects.equals(this.getRightIdString(), other.getRightIdString())) {
        return false;
      }
      if (!Objects.equals(this.getId(), other.getId())) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    if (getRightIdString() != null) {
      hcb.append(getRightIdString().hashCode());
    }
    hcb.append(getId());
    return hcb.toHashCode();
  }

  /**
   * @see org.projectforge.framework.persistence.api.ShortDisplayNameCapable#getShortDisplayName()
   */
  @Transient
  @Override
  public String getShortDisplayName() {
    return String.valueOf(this.rightIdString);
  }

  @Override
  public String toString() {
    final ToStringBuilder sb = new ToStringBuilder(this);
    sb.append("id", getId());
    sb.append("userId", this.getUserId());
    sb.append("rightId", this.rightIdString);
    sb.append("value", this.value);
    return sb.toString();
  }

  public static final String FIND_ALL_ORDERED = "UserRightDO_FindAllOrdered";
}
