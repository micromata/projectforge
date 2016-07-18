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

package org.projectforge.fibu.kost.reporting;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.fibu.kost.reporting.Report;
import org.projectforge.business.fibu.kost.reporting.ReportDao;
import org.projectforge.business.fibu.kost.reporting.ReportObjective;
import org.projectforge.common.i18n.Priority;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class ReportTest extends AbstractTestBase
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ReportTest.class);

  @Autowired
  private ReportDao reportDao;

  @Test
  public void saveXml()
  {
    ReportObjective reportObjective = new ReportObjective();
    reportObjective.setTitle("Customer ACME");
    reportObjective.setId("ACME");

    reportObjective.addKost1ExcludeRegExp("$*.01");
    reportObjective.addKost1ExcludeRegExp("$*.12");
    reportObjective.addKost1IncludeRegExp("^3.*");
    reportObjective.addKost2ExcludeRegExp("$*.02");
    reportObjective.addKost2ExcludeRegExp("$*.11");
    reportObjective.addKost2IncludeRegExp("^5.*");

    ReportObjective subReportObjective1 = new ReportObjective();
    subReportObjective1.setTitle("Project ACME-WEB-Portal");
    subReportObjective1.setId("ACME-WEB-Portal");
    subReportObjective1.addKost2IncludeRegExp("^5.020.01.*");
    reportObjective.addChildReportObjective(subReportObjective1);

    ReportObjective subReportObjective2 = new ReportObjective();
    subReportObjective2.setTitle("Project ACME-Java-Migration");
    subReportObjective2.setId("ACME-Java-Migration");
    subReportObjective2.addKost2IncludeRegExp("^5.020.02.*");
    reportObjective.addChildReportObjective(subReportObjective2);

    String xml = reportDao.serializeToXML(reportObjective);
    log.info(xml);
    logon(TEST_CONTROLLING_USER);
    Report report = reportDao.createReport(xml);
    assertEquals(report.getReportObjective().getId(), reportObjective.getId());
  }

  @Test
  public void testPriority()
  {
    assertTrue(Priority.LOW.getOrdinal() < Priority.MIDDLE.getOrdinal());
    assertTrue(Priority.MIDDLE.getOrdinal() < Priority.HIGH.getOrdinal());
  }

  @Test
  public void test()
  {
    assertEquals("5\\.100\\..*", Report.modifyRegExp("5.100.*"));
    assertEquals(".*\\.10\\..*", Report.modifyRegExp("*.10.*"));
    assertEquals("5.100.*", Report.modifyRegExp("'5.100.*"));
    assertEquals("*.10.*", Report.modifyRegExp("'*.10.*"));
    List<String> regExpList = new ArrayList<String>();
    regExpList.add("5.1*");
    assertFalse(Report.match(regExpList, "5.200.01.02", true));
    assertTrue(Report.match(regExpList, "5.190.01.02", true));
    regExpList = new ArrayList<String>();
    regExpList.add("*.02");
    assertFalse(Report.match(regExpList, "5.200.01.03", true));
    assertTrue(Report.match(regExpList, "5.190.01.02", true));
  }
}
