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

/**
 * See: http://www.quirksmode.org/js/detect.html
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UserAgentDetection
{
  private static UserAgentDetectionBrowser[] browsers;

  private UserAgentBrowser userAgentBrowser = UserAgentBrowser.UNKNOWN;

  private String version;

  static {
    browsers = new UserAgentDetectionBrowser[12];
    UserAgentDetectionBrowser browser;
    int i = 0;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "MSIE";
    browser.browser = UserAgentBrowser.IE;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "Firefox";
    browser.browser = UserAgentBrowser.FIREFOX;

    // Chrome must be listed before Safari:
    // Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.642.2 Safari/534
    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "Chrome";
    browser.browser = UserAgentBrowser.CHROME;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    // for newer Netscapes (6+)
    browser.identity = "Netscape";
    browser.browser = UserAgentBrowser.NETSCAPE;

    // Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; de-de) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4
    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "Safari";
    browser.versionSearch = "Version";
    browser.browser = UserAgentBrowser.SAFARI;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "OmniWeb";
    browser.versionSearch = "OmniWeb/";
    browser.browser = UserAgentBrowser.OMNIWEB;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "Opera";
    browser.browser = UserAgentBrowser.OPERA;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "iCab";
    browser.browser = UserAgentBrowser.ICAB;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "Konqueror";
    browser.browser = UserAgentBrowser.KONQUEROR;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "Camino";
    browser.browser = UserAgentBrowser.CAMINO;

    browsers[i++] = browser = new UserAgentDetectionBrowser();
    // for older Netscapes (4-)
    browser.identity = "Netscape";
    browser.versionSearch = "Mozilla";
    browser.browser = UserAgentBrowser.NETSCAPE;

    // Must be tested last because most of the browsers has the string Mozilla in the user agent string.
    browsers[i++] = browser = new UserAgentDetectionBrowser();
    browser.identity = "Mozilla";
    browser.versionSearch = "rv";
    browser.browser = UserAgentBrowser.MOZILLA;
  }

  public static UserAgentDetection browserDetect(final String userAgentString)
  {
    final UserAgentDetection detection = new UserAgentDetection();
    if (userAgentString == null) {
      return detection;
    }
    for (final UserAgentDetectionBrowser browser : browsers) {
      if (userAgentString.contains(browser.identity) == false) {
        continue;
      }
      detection.userAgentBrowser = browser.browser;
      String version = browser.versionSearch;
      if (version == null) {
        // Version is given direct after identity string (separated by '/')
        version = browser.identity;
      }
      int pos = userAgentString.indexOf(version);
      if (pos >= 0) {
        pos += version.length() + 1;
        final StringBuffer buf = new StringBuffer();
        int i = pos;
        while (true) {
          final char ch = userAgentString.charAt(i);
          if (Character.isWhitespace(ch) == true || ch == '/') {
            ++i;
          } else {
            break;
          }
        }
        while (true) {
          if (i >= userAgentString.length()) {
            break;
          }
          final char ch = userAgentString.charAt(i++);
          if (Character.isDigit(ch) == true || ch == '.' || ch == 'b') { // b for 4.0b4
            buf.append(ch);
          } else {
            break;
          }
        }
        detection.version = buf.toString();
      }
      break;
    }
    return detection;
  }

  public UserAgentBrowser getUserAgentBrowser()
  {
    return userAgentBrowser;
  }

  public String getUserAgentBrowserVersion()
  {
    return version;
  }
}
