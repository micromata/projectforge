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

package org.projectforge.plugins.poll.attendee;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.poll.PollDO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_POLL_ATTENDEE", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_plugin_poll_attendee_tenant_id", columnList = "tenant_id")
})
public class PollAttendeeDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -7792408128536643950L;

  @IndexedEmbedded(depth = 1)
  private PFUserDO user;

  private String email;

  @IndexedEmbedded(depth = 1)
  private PollDO poll;

  private String secureKey;

  public PollAttendeeDO()
  {

  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_fk")
  /**
   * @return the user
   */
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   * @return this for chaining.
   */
  public PollAttendeeDO setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  @Column
  /**
   * @return the email
   */
  public String getEmail()
  {
    return email;
  }

  /**
   * @param email the email to set
   * @return this for chaining.
   */
  public PollAttendeeDO setEmail(final String email)
  {
    this.email = email;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "poll_fk")
  /**
   * @return the pollId
   */
  public PollDO getPoll()
  {
    return poll;
  }

  /**
   * @param poll the pollId to set
   * @return this for chaining.
   */
  public PollAttendeeDO setPoll(final PollDO poll)
  {
    this.poll = poll;
    return this;
  }

  @Column
  /**
   * @return the secureKey
   */
  public String getSecureKey()
  {
    return secureKey;
  }

  /**
   * @param secureKey the secureKey to set
   * @return this for chaining.
   */
  public PollAttendeeDO setSecureKey(final String secureKey)
  {
    this.secureKey = secureKey;
    return this;
  }

  /**
   * @see org.projectforge.framework.persistence.entities.AbstractBaseDO#toString()
   */
  @Override
  public String toString()
  {
    String result = null;
    if (user != null) {
      result = user.getFullname();
    } else {
      result = email;
    }
    return result;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((email == null) ? 0 : email.hashCode());
    result = prime * result + ((poll == null) ? 0 : poll.hashCode());
    result = prime * result + ((secureKey == null) ? 0 : secureKey.hashCode());
    result = prime * result + ((user == null) ? 0 : user.hashCode());
    return result;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final PollAttendeeDO other = (PollAttendeeDO) obj;
    if (email == null) {
      if (other.email != null)
        return false;
    } else if (!email.equals(other.email))
      return false;
    if (poll == null) {
      if (other.poll != null)
        return false;
    } else if (!poll.equals(other.poll))
      return false;
    if (secureKey == null) {
      if (other.secureKey != null)
        return false;
    } else if (!secureKey.equals(other.secureKey))
      return false;
    if (user == null) {
      if (other.user != null)
        return false;
    } else if (!user.equals(other.user))
      return false;
    return true;
  }
}
