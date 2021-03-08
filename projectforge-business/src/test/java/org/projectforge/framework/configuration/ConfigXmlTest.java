/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.configuration;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.business.configuration.ConfigurationServiceAccessor;
import org.projectforge.common.JiraUtilsTest;
import org.projectforge.framework.calendar.ConfigureHoliday;
import org.projectforge.framework.calendar.HolidayDefinition;
import org.projectforge.framework.calendar.Holidays;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.xstream.XmlHelper;
import org.projectforge.test.TestSetup;

import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigXmlTest {
  private final static String xml = XmlHelper.replaceQuotes(XmlHelper.XML_HEADER
          + "\n"
          + "<config>\n"
          + "  <jiraBrowseBaseUrl>"
          + JiraUtilsTest.JIRA_BASE_URL
          + "</jiraBrowseBaseUrl>\n"
          + "  <holidays>\n"
          + "    <holiday label='Erster Mai' month='5' dayOfMonth='1' workingDay='false' />\n"
          + "    <holiday label='Dritter Oktober' month='10' dayOfMonth='3' workingDay='false' />\n"
          + "    <holiday id='XMAS_EVE' workingDay='true' workFraction='0.5' />\n"
          + "    <holiday id='SHROVE_TUESDAY' ignore='true' />\n"
          + "    <holiday id='SYLVESTER' workingDay='true' workFraction='0.5' />\n"
          + "  </holidays>\n"
          + "</config>\n");

  private final static String exportXml = XmlHelper.replaceQuotes(XmlHelper.XML_HEADER
          + "\n"
          + "<config>\n"
          + "  <jiraBrowseBaseUrl>"
          + JiraUtilsTest.JIRA_BASE_URL
          + "</jiraBrowseBaseUrl>\n"
          + "  <holidays>\n"
          + "    <holiday label='Erster Mai' month='5' dayOfMonth='1'/>\n"
          + "    <holiday label='Dritter Oktober' month='10' dayOfMonth='3'/>\n"
          + "    <holiday id='XMAS_EVE' workingDay='true' workFraction='0.5'/>\n"
          + "    <holiday id='SHROVE_TUESDAY' ignore='true'/>\n"
          + "    <holiday id='SYLVESTER' workingDay='true' workFraction='0.5'/>\n"
          + "  </holidays>\n"
          + "  <databaseDirectory>database</databaseDirectory>\n"
          + "  <ehcacheDirectory>ehcache</ehcacheDirectory>\n"
          + "  <loggingDirectory>logs</loggingDirectory>\n"
          + "  <jcrDirectory>jcr</jcrDirectory>\n"
          + "  <workingDirectory>work</workingDirectory>\n"
          + "  <backupDirectory>backup</backupDirectory>\n"
          + "  <tempDirectory>tmp</tempDirectory>\n"
          + "  <accountingConfig/>\n"
          + "</config>");

  /**
   * Creates a test configuration if no configuration does already exists.
   */
  public static ConfigXml createTestConfiguration() {
    ConfigurationServiceAccessor.internalInitJunitTestMode();
    ConfigXml.internalSetInstance(xml);
    return ConfigXml.getInstance();
  }

  @Test
  public void testHolidayDefinition() {
    createTestConfiguration();
    final ConfigXml config = ConfigXml.getInstance();
    assertEquals(5, config.getHolidays().size());
    ConfigureHoliday holiday = config.getHolidays().get(0);
    assertEquals(Month.MAY.getValue(), (int) holiday.getMonth());
    holiday = config.getHolidays().get(2);
    assertEquals(HolidayDefinition.XMAS_EVE, holiday.getId());
    holiday = config.getHolidays().get(3);
    assertEquals(HolidayDefinition.SHROVE_TUESDAY, holiday.getId());
    assertTrue(holiday.isIgnore());

    final Holidays holidays = Holidays.getInstance();
    PFDateTime dateTime = PFDateTime.withDate(2009, Month.MAY, 1);
    assertTrue(holidays.isHoliday(2009, dateTime.getDayOfYear()), "Should be there.");
    dateTime = dateTime.withMonth(Month.FEBRUARY).withDayOfMonth(23);
    assertTrue(holidays.isHoliday(2009, dateTime.getDayOfYear()), "Should be there.");
    dateTime = dateTime.withDayOfMonth(24);
    assertFalse(holidays.isHoliday(2009, dateTime.getDayOfYear()), "Should be ignored.");
  }

  @Test
  public void testExport() {
    createTestConfiguration();
    String exported_config = ConfigXml.getInstance().exportConfiguration();
    // on windows other pathes may be used.
    String expected_config = StringUtils.replace(exportXml, "\\", "/");
    exported_config = StringUtils.replace(exported_config, "\\", "/");
    assertEquals(expected_config, exported_config);
  }

  @BeforeAll
  static void setup() {
    TestSetup.init();
  }
}
