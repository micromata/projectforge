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

package org.projectforge.plugins.crm;

import java.io.Serializable;

import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@XmlObject(alias = "value")
public class EmailValue implements Serializable
{
  private static final long serialVersionUID = 3930937731653442004L;

  @XmlField
  @PropertyInfo(i18nKey = "contactType")
  private ContactType contactType;

  @XmlField
  @PropertyInfo(i18nKey = "email")
  private String email;

  public ContactType getContactType()
  {
    return contactType;
  }

  public EmailValue setContactType(final ContactType contactType)
  {
    this.contactType = contactType;
    return this;
  }

  public String getEmail()
  {
    return email;
  }

  public EmailValue setEmail(final String email)
  {
    this.email = email;
    return this;
  }

}
