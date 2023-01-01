/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.jupiter.api.Test;
import org.projectforge.business.systeminfo.SystemInfoCache;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.address.AddressEditPage;
import org.projectforge.web.address.AddressListPage;
import org.projectforge.web.admin.SetupPage;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.registry.WebRegistry;
import org.projectforge.web.wicket.WicketPageTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class CallAllPagesTest extends WicketPageTestBase {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CallAllPagesTest.class);

  static int counter;

  @Autowired
  private SystemInfoCache systemInfoCache;

  @Autowired
  private MenuCreator menuCreator;

  @SuppressWarnings("unchecked")
  private final Class<? extends WebPage>[] skipPages = new Class[]{ //
      // Not yet checked:
      AddressEditPage.class, // Shouldn't be used anymore (only available for compilation of AddressListPage).
      AddressListPage.class, // Shouldn't be used anymore (AddressListPage.filter used in Marketing plugin).
      SetupPage.class,       // Tested separately (works only on empty data bas
  };

  @Override
  protected void afterAll() {
    log.info("Number of tested Wicket pages: " + counter);
    recreateDataBase();
  }

  @Test
  public void testAllMountedPages() {
    MenuCreator.setTestCase(true);
    _testAllMountedPages();
    testPage(SetupPage.class, CalendarPage.class); // Data base isn't empty.
    // clearDatabase();
    // testPage(SetupPage.class); // Doesn't work (table t_pf_user exists).
  }

  private void _testAllMountedPages() {
    log.info("Test all web pages with.");
    login(AbstractTestBase.TEST_FULL_ACCESS_USER, AbstractTestBase.TEST_FULL_ACCESS_USER_PASSWORD);
    SystemInfoCache.internalInitialize(systemInfoCache);
    final Map<String, Class<? extends WebPage>> pages = WebRegistry.getInstance().getMountPages();
    counter = 0;
    for (final Map.Entry<String, Class<? extends WebPage>> entry : pages.entrySet()) {
      boolean skip = false;
      for (final Class<?> clazz : skipPages) {
        if (clazz.equals(entry.getValue()) == true) {
          log.info("Skipping page: " + entry.getValue());
          skip = true;
        }
      }
      if (skip == true) {
        continue;
      }
      testPage(entry.getValue());
    }
    logout();
  }

  private void testPage(final Class<? extends WebPage> pageClass) {
    testPage(pageClass, null, pageClass);
  }

  @SuppressWarnings("unused")
  private void testPage(final Class<? extends WebPage> pageClass, final PageParameters params) {
    testPage(pageClass, params, pageClass);
  }

  private void testPage(final Class<? extends WebPage> pageClass, final Class<? extends WebPage> expectedRenderedPage) {
    testPage(pageClass, null, expectedRenderedPage);
  }

  private void testPage(final Class<? extends WebPage> pageClass, final PageParameters params,
                        final Class<? extends WebPage> expectedRenderedPage) {
    log.info("Calling page: " + pageClass.getName());
    if (params != null) {
      tester.startPage(pageClass, params);
    } else {
      tester.startPage(pageClass);
    }
    tester.assertRenderedPage(expectedRenderedPage);
    ++counter;
  }
}
