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

package org.projectforge.web.session;

import org.apache.commons.lang3.StringUtils;

public enum UserAgentBrowser
{
  IE, MOZILLA, SAFARI, KONQUEROR, OPERA, FIREFOX, CHROME, OMNIWEB, ICAB, CAMINO, NETSCAPE, UNKNOWN;

  public boolean isIn(final UserAgentBrowser... browser)
  {
    for (final UserAgentBrowser br : browser) {
      if (this == br) {
        return true;
      }
    }
    return false;
  }

  public static UserAgentBrowser getBrowserFromUserAgentString(String userAgent)
  {
    if (StringUtils.isNotEmpty(userAgent)) {
      //TODO: Add mor browsers for user agent
      if (userAgent.contains("Version")) {
        return UserAgentBrowser.SAFARI;
      }
      if (userAgent.contains("Chrome")) {
        return UserAgentBrowser.CHROME;
      }
    }
    return UserAgentBrowser.UNKNOWN;
  }
}
