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

package org.projectforge.continuousdb.demo.entities;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Table(name = "T_GROUP", uniqueConstraints = { @UniqueConstraint(columnNames = { "name"})})
public class GroupDO extends DefaultBaseDO 
{
  // private static final Logger log = Logger.getLogger(GroupDO.class);

  private String name;

  private String organization;

  private String description;

  private Set<UserDO> assignedUsers;

  @ManyToMany(targetEntity = UserDO.class, cascade = { CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
  @JoinTable(name = "T_GROUP_USER", joinColumns = @JoinColumn(name = "GROUP_ID"), inverseJoinColumns = @JoinColumn(name = "USER_ID"))
  public Set<UserDO> getAssignedUsers()
  {
    return assignedUsers;
  }

  public void setAssignedUsers(final Set<UserDO> assignedUsers)
  {
    this.assignedUsers = assignedUsers;
  }

  @Column(length = 1000)
  public String getDescription()
  {
    return description;
  }

  /**
   * @param description
   * @return this for chaining.
   */
  public GroupDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  @Column(length = 100)
  public String getName()
  {
    return name;
  }

  /**
   * @param name
   * @return this for chaining.
   */
  public GroupDO setName(final String name)
  {
    this.name = name;
    return this;
  }

  @Column(length = 100)
  public String getOrganization()
  {
    return organization;
  }

  public void setOrganization(final String organization)
  {
    this.organization = organization;
  }

  @Override
  public String toString()
  {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", getName());
    builder.append("organization", getOrganization());
    builder.append("description", getDescription());
    return builder.toString();
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof GroupDO) {
      final GroupDO other = (GroupDO) o;
      if (ObjectUtils.equals(this.getName(), other.getName()) == false)
        return false;
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.getName());
    return hcb.toHashCode();
  }
}
