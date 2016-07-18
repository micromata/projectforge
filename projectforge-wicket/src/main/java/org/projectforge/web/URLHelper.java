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

package org.projectforge.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLHelper
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(URLHelper.class);

  /**
   * Uses UTF-8
   * @param str
   * @see URLEncoder#encode(String, String)
   */
  public static String encode(final String str)
  {
    if (str == null) {
      return "";
    }
    try {
      return URLEncoder.encode(str, "UTF-8");
    } catch (final UnsupportedEncodingException ex) {
      log.info("Can't URL-encode '" + str + "': " + ex.getMessage());
      return "";
    }
  }

  /**
   * Removes the jsessionid parameter from the given url if exists.
   * @param url
   * @return
   */
  public static String removeJSessionId(final String url)
  {
    if (url == null) {
      return null;
    }
    final int pos = url.indexOf(";jsessionid=");
    if (pos < 0) {
      return url;
    }
    final int questionMark = url.indexOf('?');
    if (questionMark < 0) {
      // No parameters, so url ends with jsession id.
      return url.substring(0, pos);
    }
    if (questionMark < pos) {
      // What the hell?
      return url;
    }
    return url.substring(0, pos) + url.substring(questionMark);
  }
}
