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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class UrlHelperTest {
  @Test
  public void removeJSessionId() {
    assertNull(URLHelper.removeJSessionId(null));
    assertEquals("", URLHelper.removeJSessionId(""));
    assertEquals("http://localhost:8080/ProjectForge/wa/addressEdit",
            URLHelper.removeJSessionId("http://localhost:8080/ProjectForge/wa/addressEdit"));
    assertEquals("http://localhost:8080/ProjectForge/wa/addressEdit",
            URLHelper.removeJSessionId(
                    "http://localhost:8080/ProjectForge/wa/addressEdit;jsessionid=hji8ysdreqlz19bipa3ccm2jj"));
    assertEquals("http://localhost:8080/ProjectForge/wa/addressEdit?4",
            URLHelper.removeJSessionId(
                    "http://localhost:8080/ProjectForge/wa/addressEdit;jsessionid=hji8ysdreqlz19bipa3ccm2jj?4"));
    assertEquals("https://localhost:8443/ProjectForge/wa/timesheetEdit/id/325295",
            URLHelper
                    .removeJSessionId(
                            "https://localhost:8443/ProjectForge/wa/timesheetEdit/id/325295;jsessionid=DF6F216F10DC6A27EBA0EB60A7254EAA"));
    assertEquals("https://localhost:8443/ProjectForge/wa/timesheetEdit/id/325295",
            URLHelper.removeJSessionId("https://localhost:8443/ProjectForge/wa/timesheetEdit/id/325295"));
    assertEquals("https://localhost:8443/?hurzel",
            URLHelper.removeJSessionId("https://localhost:8443/;jsessionid=376kjKJ224?hurzel"));
    assertEquals(
            "https://localhost:8443/ProjectForge/wa/?wicket:bookmarkablePage=:org.projectforge.web.timesheet.TimesheetMassUpdatePage",
            URLHelper
                    .removeJSessionId(
                            "https://localhost:8443/ProjectForge/wa/;jsessionid=B2BE03E5838FDAFCE3ED1AE235A78878?wicket:bookmarkablePage=:org.projectforge.web.timesheet.TimesheetMassUpdatePage"));
    assertEquals("https://localhost:8443/timesheetList/date=6546576/store=false?hurzel",
            URLHelper.removeJSessionId(
                    "https://localhost:8443/timesheetList/date=6546576/store=false;jsessionid=376kjKJ224?hurzel"));
    assertEquals("https://localhost:8443/timesheetList/?hurzel",
            URLHelper.removeJSessionId("https://localhost:8443/timesheetList/;jsessionid=376kjKJ224?hurzel"));
    assertEquals("https://localhost:8443/timesheetList/",
            URLHelper.removeJSessionId("https://localhost:8443/timesheetList/;jsessionid=376kjKJ224"));
  }
}
