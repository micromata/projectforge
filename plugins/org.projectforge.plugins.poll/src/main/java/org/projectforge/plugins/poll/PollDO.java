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

package org.projectforge.plugins.poll;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

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
@Table(name = "T_PLUGIN_POLL", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_plugin_poll_tenant_id", columnList = "tenant_id")
})
public class PollDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 1L;

  @IndexedEmbedded(depth = 1)
  private PFUserDO owner;

  private String title;

  private String location;

  private String description;

  private boolean active;

  public PollDO()
  {

  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_fk")
  /**
   * @return the owner
   */
  public PFUserDO getOwner()
  {
    return owner;
  }

  /**
   * @param owner the owner to set
   * @return this for chaining.
   */
  public PollDO setOwner(final PFUserDO owner)
  {
    this.owner = owner;
    return this;
  }

  @Column
  /**
   * @return the title
   */
  public String getTitle()
  {
    return title;
  }

  /**
   * @param title the title to set
   * @return this for chaining.
   */
  public PollDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  @Column
  /**
   * @return the location
   */
  public String getLocation()
  {
    return location;
  }

  /**
   * @param location the location to set
   * @return this for chaining.
   */
  public PollDO setLocation(final String location)
  {
    this.location = location;
    return this;
  }

  @Column
  /**
   * @return the description
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * @param description the description to set
   * @return this for chaining.
   */
  public PollDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  @Column
  /**
   * @return the active
   */
  public boolean isActive()
  {
    return active;
  }

  /**
   * @param active the active to set
   * @return this for chaining.
   */
  public PollDO setActive(final boolean active)
  {
    this.active = active;
    return this;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
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
    final PollDO other = (PollDO) obj;
    if (getId() == null) {
      if (other.getId() != null)
        return false;
      else
        return super.equals(obj);
    } else if (!getId().equals(other.getId()))
      return false;
    return true;
  }

}
