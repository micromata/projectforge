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

package org.projectforge.web.session;

/**
 * The users client agent device (stored in MySession).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum UserAgentDevice
{
  ANDROID, BLACKBERRY, IPHONE, IPAD, IPOD, SMARTPHONE, SYMBIAN, WAP, WINDOWS_MOBILE, UNKNOWN;

  /**
   * @param userAgent The user agent string from the http request.
   */
  public static UserAgentDevice getUserAgentDevice(final String userAgent)
  {
    if (userAgent == null) {
      return UNKNOWN;
    }
    final String str = userAgent.toLowerCase();
    if (str.contains("android")) {
      return ANDROID;
    } else if (str.contains("blackberry")) {
      return BLACKBERRY;
    } else if (str.contains("iphone")) {
      return IPHONE;
    } else if (str.contains("ipad")) {
      return IPOD;
    } else if (str.contains("ipod")) {
      return IPOD;
    } else if (str.contains("smartphone")) {
      return SMARTPHONE;
    } else if (str.contains("symbian")) {
      return SYMBIAN;
    } else if (str.contains("wap")) {
      return WAP;
    } else if (str.contains("phone") && str.contains("windows")) {
      return WINDOWS_MOBILE;
    } else if (str.contains("windows ce")) {
      return WINDOWS_MOBILE;
    }
    return UNKNOWN;
  }

  public boolean isIn(final UserAgentDevice... device)
  {
    for (final UserAgentDevice dev : device) {
      if (this == dev) {
        return true;
      }
    }
    return false;
  }

  public boolean isMobile()
  {
    return isIn(ANDROID, BLACKBERRY, IPHONE, IPOD, SMARTPHONE, SYMBIAN, WAP, WINDOWS_MOBILE);
  }
}
