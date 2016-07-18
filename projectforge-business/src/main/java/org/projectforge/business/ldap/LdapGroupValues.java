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

package org.projectforge.business.ldap;

import java.io.Serializable;

import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.xstream.XmlObject;

/**
 * Bean used for serialization and deserialization of the ldap values as xml string in {@link GroupDO#getLdapValues()} ConfigXML
 * (config.xml).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XmlObject(alias = "values")
public class LdapGroupValues implements Serializable
{
  private static final long serialVersionUID = 812898869519604597L;

  private Integer gidNumber = null;

  public boolean isValuesEmpty()
  {
    return isPosixValuesEmpty() == true;
  }

  public boolean isPosixValuesEmpty()
  {
    return getGidNumber() == null;
  }

  public Integer getGidNumber()
  {
    return gidNumber;
  }

  public LdapGroupValues setGidNumber(final Integer gidNumber)
  {
    this.gidNumber = gidNumber;
    return this;
  }
}
