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

package org.projectforge.web.session;

public enum UserAgentOS
{
  WINDOWS, MAC, LINUX, UNKNOWN;

  /**
   * @param userAgent The user agent string from the http request.
   */
  public static UserAgentOS getUserAgentOS(final String userAgent)
  {
    if (userAgent == null) {
      return UNKNOWN;
    }
    final String str = userAgent.toLowerCase();
    if (str.contains("windows") == true) {
      return WINDOWS;
    } else if (str.contains("mac") == true) {
      return MAC;
    } else if (str.contains("linux") == true || str.contains("suse") == true || str.contains("ubuntu") == true) {
      return LINUX;
    }
    return UNKNOWN;
  }

  public boolean isIn(final UserAgentOS... operationsystems)
  {
    for (final UserAgentOS os : operationsystems) {
      if (this == os) {
        return true;
      }
    }
    return false;
  }
}
