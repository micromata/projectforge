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

import static org.testng.AssertJUnit.assertEquals;

import org.projectforge.web.session.UserAgentBrowser;
import org.projectforge.web.session.UserAgentDetection;
import org.projectforge.web.session.UserAgentDevice;
import org.projectforge.web.session.UserAgentOS;
import org.testng.annotations.Test;

public class UserAgentDetectionTest
{
  // Test user agent strings.
  private String[] strs = {
      "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; de-de) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4", // 0
      "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)", // 1
      "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13", // 2
      "Mozilla/5.0 (iPod; U; CPU iPhone OS 4_2_1 like Mac OS X; de-de) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8C148 Safari/6533.18.5", // 3
      "Mozilla/5.0 (iPad; U; CPU OS 4_2_1 like Mac OS X; es-es) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8C148 Safari/6533.18.5", // 4
      "Opera/9.80 (Windows NT 5.1; U; zh-cn) Presto/2.7.62 Version/11.01", // 5
      "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.642.2 Safari/534", // 6
      "Mozilla/5.0 (compatible; Konqueror/4.3) KHTML/4.3.1 (like Gecko) SUSE", // 7
      "Mozilla/5.0 (compatible; Konqueror/4.4; Linux) KHTML/4.4.5 (like Gecko) Kubuntu", // 8
      "Mozilla/5.0 (Windows NT 6.1; rv:2.0b4) Gecko/20100818 Firefox/4.0b4 (.NET CLR 3.5.30729) QQDownload/1.7" // 9
  };

  @Test
  public void testOSDetection()
  {
    assertEquals(UserAgentOS.UNKNOWN, UserAgentOS.getUserAgentOS(null));
    assertEquals(UserAgentOS.MAC, UserAgentOS.getUserAgentOS(strs[0]));
    assertEquals(UserAgentOS.WINDOWS, UserAgentOS.getUserAgentOS(strs[1]));
    assertEquals(UserAgentOS.WINDOWS, UserAgentOS.getUserAgentOS(strs[2]));
    assertEquals(UserAgentOS.MAC, UserAgentOS.getUserAgentOS(strs[3]));
    assertEquals(UserAgentOS.MAC, UserAgentOS.getUserAgentOS(strs[4]));
    assertEquals(UserAgentOS.WINDOWS, UserAgentOS.getUserAgentOS(strs[5]));
    assertEquals(UserAgentOS.WINDOWS, UserAgentOS.getUserAgentOS(strs[6]));
    assertEquals(UserAgentOS.LINUX, UserAgentOS.getUserAgentOS(strs[7]));
    assertEquals(UserAgentOS.LINUX, UserAgentOS.getUserAgentOS(strs[8]));
    assertEquals(UserAgentOS.WINDOWS, UserAgentOS.getUserAgentOS(strs[9]));
  }

  @Test
  public void testDeviceDetection()
  {
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(null));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[0]));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[1]));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[2]));
    assertEquals(UserAgentDevice.IPHONE, UserAgentDevice.getUserAgentDevice(strs[3]));
    assertEquals(UserAgentDevice.IPOD, UserAgentDevice.getUserAgentDevice(strs[4]));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[5]));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[6]));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[7]));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[8]));
    assertEquals(UserAgentDevice.UNKNOWN, UserAgentDevice.getUserAgentDevice(strs[9]));
  }

  @Test
  public void testBrowserDetection()
  {
    detectAndAssert(null, UserAgentBrowser.UNKNOWN, null);
    detectAndAssert(strs[0], UserAgentBrowser.SAFARI, "5.0.3");
    detectAndAssert(strs[1], UserAgentBrowser.IE, "6.0");
    detectAndAssert(strs[2], UserAgentBrowser.FIREFOX, "3.6.13");
    detectAndAssert(strs[3], UserAgentBrowser.SAFARI, "5.0.2");
    detectAndAssert(strs[4], UserAgentBrowser.SAFARI, "5.0.2");
    detectAndAssert(strs[5], UserAgentBrowser.OPERA, "9.80");
    detectAndAssert(strs[6], UserAgentBrowser.CHROME, "10.0.642.2");
    detectAndAssert(strs[7], UserAgentBrowser.KONQUEROR, "4.3");
    detectAndAssert(strs[8], UserAgentBrowser.KONQUEROR, "4.4");
    detectAndAssert(strs[9], UserAgentBrowser.FIREFOX, "4.0b4");
  }

  private void detectAndAssert(final String userAgentString, final UserAgentBrowser expectedBrowser,
      final String expectedVersion)
  {
    final UserAgentDetection detection = UserAgentDetection.browserDetect(userAgentString);
    assertEquals(expectedBrowser, detection.getUserAgentBrowser());
    assertEquals(expectedVersion, detection.getUserAgentBrowserVersion());
  }
}
