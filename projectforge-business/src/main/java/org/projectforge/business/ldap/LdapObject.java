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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.projectforge.framework.ToStringUtil;

import java.io.Serializable;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class LdapObject<I extends Serializable>
{
  private String dn;

  protected String commonName;

  private String organizationalUnit;

  private String[] objectClasses;

  public abstract I getId();

  /**
   * @return the cn
   */
  public String getCommonName()
  {
    return commonName;
  }

  /**
   * cn
   * @param commonName the cn to set
   * @return this for chaining.
   */
  public LdapObject<I> setCommonName(final String commonName)
  {
    if (commonName.indexOf('\\') >= 0) {
      // Remove escape characters
      this.commonName = commonName.replace("\\", "");
    } else {
      this.commonName = commonName;
    }
    return this;
  }

  /**
   * @return the dn
   */
  public String getDn()
  {
    return dn;
  }

  /**
   * @param dn the dn to set
   * @return this for chaining.
   */
  public void setDn(final String dn)
  {
    this.dn = dn;
  }

  /**
   * @return the organizationalUnit
   */
  public String getOrganizationalUnit()
  {
    return organizationalUnit;
  }

  /**
   * @param organizationalUnit the organizationalUnit to set
   * @return this for chaining.
   */
  public void setOrganizationalUnit(final String organizationalUnit)
  {
    this.organizationalUnit = organizationalUnit;
  }

  /**
   * @return the objectClasses
   */
  public String[] getObjectClasses()
  {
    return objectClasses;
  }

  /**
   * @param objectClasses the objectClasses to set
   * @return this for chaining.
   */
  public void setObjectClasses(final String[] objectClasses)
  {
    this.objectClasses = objectClasses;
  }

  public void addObjectClass(final String objectClass)
  {
    if (this.objectClasses == null) {
      this.objectClasses = new String[] { objectClass};
    } else {
      this.objectClasses = (String[])ArrayUtils.add(this.objectClasses, objectClass);
    }
  }

  @Override
  public boolean equals(final Object obj)
  {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public int hashCode()
  {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString()
  {
    return ToStringUtil.toJsonString(this);
  }
}
