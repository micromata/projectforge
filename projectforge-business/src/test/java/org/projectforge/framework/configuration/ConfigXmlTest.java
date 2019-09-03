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

package org.projectforge.framework.configuration;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.projectforge.business.configuration.ConfigurationServiceAccessor;
import org.projectforge.common.JiraUtilsTest;
import org.projectforge.framework.calendar.ConfigureHoliday;
import org.projectforge.framework.calendar.HolidayDefinition;
import org.projectforge.framework.calendar.Holidays;
import org.projectforge.framework.xstream.XmlHelper;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigXmlTest {
  private final static String xml = XmlHelper.replaceQuotes(XmlHelper.XML_HEADER
          + "\n"
          + "<config>\n"
          + "  <jiraBrowseBaseUrl>"
          + JiraUtilsTest.JIRA_BASE_URL
          + "</jiraBrowseBaseUrl>\n"
          + "  <holidays>\n"
          + "    <holiday label='Erster Mai' month='4' dayOfMonth='1' workingDay='false' />\n"
          + "    <holiday label='Dritter Oktober' month='9' dayOfMonth='3' workingDay='false' />\n"
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
          + "    <holiday label='Erster Mai' month='4' dayOfMonth='1'/>\n"
          + "    <holiday label='Dritter Oktober' month='9' dayOfMonth='3'/>\n"
          + "    <holiday id='XMAS_EVE' workingDay='true' workFraction='0.5'/>\n"
          + "    <holiday id='SHROVE_TUESDAY' ignore='true'/>\n"
          + "    <holiday id='SYLVESTER' workingDay='true' workFraction='0.5'/>\n"
          + "  </holidays>\n"
          + "  <databaseDirectory>database</databaseDirectory>\n"
          + "  <ehcacheDirectory>ehcache</ehcacheDirectory>\n"
          + "  <loggingDirectory>logs</loggingDirectory>\n"
          + "  <workingDirectory>work</workingDirectory>\n"
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
    assertEquals(Calendar.MAY, (int) holiday.getMonth());
    holiday = config.getHolidays().get(2);
    assertEquals(HolidayDefinition.XMAS_EVE, holiday.getId());
    holiday = config.getHolidays().get(3);
    assertEquals(HolidayDefinition.SHROVE_TUESDAY, holiday.getId());
    assertEquals(true, holiday.isIgnore());

    final Holidays holidays = Holidays.getInstance();
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2009);
    cal.set(Calendar.MONTH, Calendar.MAY);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    assertEquals(true, holidays.isHoliday(2009, cal.get(Calendar.DAY_OF_YEAR)), "Should be there.");
    cal.set(Calendar.MONTH, Calendar.FEBRUARY);
    cal.set(Calendar.DAY_OF_MONTH, 23);
    assertEquals(true, holidays.isHoliday(2009, cal.get(Calendar.DAY_OF_YEAR)), "Should be there.");
    cal.set(Calendar.DAY_OF_MONTH, 24);
    assertEquals(false, holidays.isHoliday(2009, cal.get(Calendar.DAY_OF_YEAR)), "Should be ignored.");
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

  @Test
  public void testPluginMainClasses() {
    final ConfigXml configuration = new ConfigXml();
    configuration.pluginMainClasses = "\n org.projectforge.plugins.todo.ToDoPlugin,\n  org.projectforge.plugins.software.SoftwarePlugin\n org.projectforge.plugins.ical.ICalPlugin";
    final String[] sa = configuration.getPluginMainClasses();
    assertEquals(3, sa.length);
    assertEquals("org.projectforge.plugins.todo.ToDoPlugin", sa[0]);
    assertEquals("org.projectforge.plugins.software.SoftwarePlugin", sa[1]);
    assertEquals("org.projectforge.plugins.ical.ICalPlugin", sa[2]);
  }
}
