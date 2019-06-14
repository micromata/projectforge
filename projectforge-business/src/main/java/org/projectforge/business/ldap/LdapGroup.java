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

package org.projectforge.business.ldap;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LdapGroup extends LdapObject<String>
{
  private Integer gidNumber; // POSIX gid

  private String description, organization, businessCategory;

  private final Set<String> members = new HashSet<String>();

  /**
   * @see org.projectforge.business.ldap.LdapObject#getId()
   */
  @Override
  public String getId()
  {
    return getCommonName();
  }

  public LdapGroup addMember(final String dn, final String baseDN)
  {
    if (dn.endsWith(baseDN) == true) {
      members.add(dn);
    } else {
      members.add(dn + "," + baseDN);
    }
    return this;
  }

  public LdapGroup addMember(final LdapObject< ? > member, final String baseDN)
  {
    return addMember(member.getDn(), baseDN);
  }

  /**
   * @return the members
   */
  public Set<String> getMembers()
  {
    return members;
  }

  public void clearMembers()
  {
    members.clear();
  }

  public void addAllMembers(final Set<String> newMembers)
  {
    this.members.addAll(newMembers);
  }

  /**
   * Field businessCategory is used for storing ProjectForge's group id (in LDAP master mode).
   * @return the id.
   */
  public String getBusinessCategory()
  {
    return businessCategory;
  }

  /**
   * @param businessCategory the businessCategory to set
   * @return this for chaining.
   */
  public LdapGroup setBusinessCategory(final String businessCategory)
  {
    this.businessCategory = businessCategory;
    return this;
  }

  /**
   * @return the organization
   */
  public String getOrganization()
  {
    return organization;
  }

  /**
   * @param organization the organization to set
   * @return this for chaining.
   */
  public LdapGroup setOrganization(final String organization)
  {
    this.organization = organization;
    return this;
  }

  public String getDescription()
  {
    return description;
  }

  public LdapGroup setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * @return The gid number of object class posixAccount.
   */
  public Integer getGidNumber()
  {
    return gidNumber;
  }

  public LdapGroup setGidNumber(final Integer gidNumber)
  {
    this.gidNumber = gidNumber;
    return this;
  }
}
