/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address;

import org.apache.commons.lang3.StringUtils;

public enum InstantMessagingType
{
  SKYPE("skype"), AIM("aim"), GOOGLE_TALK("googletalk"), ICQ("icq"), MSN("msn"), YAHOO("yahoo"), JABBER("jabber");

  /**
   * List of all available values.
   */
  public static final InstantMessagingType[] LIST = new InstantMessagingType[] { SKYPE, ICQ, MSN, YAHOO, JABBER, AIM, GOOGLE_TALK};

  private String key;

  public static InstantMessagingType get(final String s)
  {
    if (StringUtils.isEmpty(s)) {
      return null;
    }
    if ("SKYPE".equals(s)) {
      return SKYPE;
    } else if ("ICQ".equals(s)) {
      return ICQ;
    } else if ("MSN".equals(s)) {
      return MSN;
    } else if ("YAHOO".equals(s)) {
      return YAHOO;
    } else if ("JABBER".equals(s)) {
      return JABBER;
    } else if ("AIM".equals(s)) {
      return AIM;
    } else if ("GOOGLE_TALK".equals(s)) {
      return GOOGLE_TALK;
    }
    throw new UnsupportedOperationException("Unknown InstantMessagingType: '" + s + "'");
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  InstantMessagingType(final String key)
  {
    this.key = key;
  }
}
