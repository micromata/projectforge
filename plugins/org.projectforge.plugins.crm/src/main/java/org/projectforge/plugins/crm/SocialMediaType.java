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

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.i18n.I18nEnum;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
public enum SocialMediaType implements I18nEnum
{
  AIM("aim"), FACEBOOK("facebook"), GADO("gado"), GOOGLE_TALK("googletalk"), ICQ("icq"), JABBER("jabber"), MSN("msn"), QQ("qq"), SKYPE("skype"), TWITTER("twitter"), YAHOO("yahoo"), OTHER("other") ;

  public static final String I18N_KEY_SOCIALMEDIATYPE_PREFIX = "socialmediatype.";

  /**
   * List of all available values.
   */
  public static final SocialMediaType[] LIST = new SocialMediaType[] { AIM, FACEBOOK, GADO, GOOGLE_TALK, ICQ, JABBER, MSN, QQ, SKYPE, TWITTER, YAHOO, OTHER };

  private String key;

  public static SocialMediaType get(final String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    if ("AIM".equals(s) == true) {
      return AIM;
    } else if ("FACEBOOK".equals(s) == true) {
      return FACEBOOK;
    } else if ("GADO".equals(s) == true) {
      return GADO;
    } else if ("GOOGLE_TALK".equals(s) == true) {
      return GOOGLE_TALK;
    } else if ("ICQ".equals(s) == true) {
      return ICQ;
    } else if ("JABBER".equals(s) == true) {
      return JABBER;
    } else if ("MSN".equals(s) == true) {
      return MSN;
    } else if ("QQ".equals(s) == true) {
      return QQ;
    } else if ("SKYPE".equals(s) == true) {
      return SKYPE;
    } else if ("TWITTER".equals(s) == true) {
      return TWITTER;
    }else if ("YAHOO".equals(s) == true) {
      return YAHOO;
    } else if ("OTHER".equals(s) == true) {
      return OTHER;
    }
    throw new UnsupportedOperationException("Unknown SocialMediaType: '" + s + "'");
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  SocialMediaType(final String key)
  {
    this.key = key;
  }

  /**
   * @see org.projectforge.common.i18n.I18nEnum#getI18nKey()
   */
  @Override
  public String getI18nKey()
  {
    return I18N_KEY_SOCIALMEDIATYPE_PREFIX + key;
  }
}
