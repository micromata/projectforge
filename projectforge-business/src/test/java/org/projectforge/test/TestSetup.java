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

package org.projectforge.test;

import org.projectforge.business.configuration.ConfigurationServiceAccessor;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TestSetup {
  /**
   * Puts a context user in ThreadLocaleUserContext and creates a minimal ConfigXml configuration.
   * This is use-full for tests needing the user's locale, timezone etc, but not the time consuming
   * database and Spring setup.
   */
  public static void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    PFUserDO user = new PFUserDO();
    user.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
    user.setExcelDateFormat("YYYY-MM-DD");
    user.setDateFormat("dd.MM.yyyy");
    user.setLocale(new Locale("de", "DE"));
    user.setFirstDayOfWeek(Calendar.MONDAY);
    ThreadLocalUserContext.setUserContext(new UserContext(user, null));
    ConfigXmlTest.createTestConfiguration();
    ConfigurationServiceAccessor.internalInitJunitTestMode();
  }
}
