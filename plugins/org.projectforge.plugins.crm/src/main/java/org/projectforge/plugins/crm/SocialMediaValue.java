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

package org.projectforge.plugins.crm;

import java.io.Serializable;

import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@XmlObject(alias = "value")
public class SocialMediaValue implements Serializable
{
  private static final long serialVersionUID = 5659903071636285902L;

  @XmlField
  @PropertyInfo(i18nKey = "contactType")
  private ContactType contactType;

  @XmlField
  @PropertyInfo(i18nKey = "imType")
  private SocialMediaType socialMediaType;

  @XmlField
  @PropertyInfo(i18nKey = "user")
  private String user;

  public ContactType getContactType()
  {
    return contactType;
  }

  public SocialMediaValue setContactType(final ContactType contactType)
  {
    this.contactType = contactType;
    return this;
  }

  public SocialMediaType getSocialMediaType()
  {
    return socialMediaType;
  }

  public SocialMediaValue setSocialMediaType(final SocialMediaType socialMediaType)
  {
    this.socialMediaType = socialMediaType;
    return this;
  }

  public String getUser()
  {
    return user;
  }

  public SocialMediaValue setUser(final String user)
  {
    this.user = user;
    return this;
  }

}
