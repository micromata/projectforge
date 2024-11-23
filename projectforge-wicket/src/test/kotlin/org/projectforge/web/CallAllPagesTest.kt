/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web

import mu.KotlinLogging
import org.apache.wicket.markup.html.WebPage
import org.apache.wicket.request.mapper.parameter.PageParameters
import org.junit.jupiter.api.Test
import org.projectforge.business.systeminfo.SystemInfoCache
import org.projectforge.business.systeminfo.SystemInfoCache.Companion.internalInitialize
import org.projectforge.common.logging.LogSubscription
import org.projectforge.common.logging.LoggerMemoryAppender
import org.projectforge.menu.builder.MenuCreator.Companion.testCase
import org.projectforge.web.address.AddressEditPage
import org.projectforge.web.address.AddressListPage
import org.projectforge.web.admin.SetupPage
import org.projectforge.web.calendar.CalendarPage
import org.projectforge.web.registry.WebRegistry
import org.projectforge.web.wicket.WicketPageTestBase
import org.springframework.beans.factory.annotation.Autowired

private val log = KotlinLogging.logger {}

class CallAllPagesTest : WicketPageTestBase() {
    @Autowired
    private lateinit var systemInfoCache: SystemInfoCache

    private val skipPages: Array<Class<out WebPage>> = arrayOf(
        // Not yet checked:
        AddressEditPage::class.java,  // Shouldn't be used anymore (only available for compilation of AddressListPage).
        AddressListPage::class.java,  // Shouldn't be used anymore (AddressListPage.filter used in Marketing plugin).
        SetupPage::class.java,  // Tested separately (works only on empty database)
    )

    override fun afterAll() {
        log.info { "Number of tested Wicket pages: $counter" }
        recreateDataBase()
    }

    override fun beforeAll() {
        if (!LoggerMemoryAppender.isInitialized()) {
            LoggerMemoryAppender() // Needed by DatevImportPage
        }
        super.beforeAll()
    }

    @Test
    fun testAllMountedPages() {
        testCase = true
        _testAllMountedPages()
        suppressErrorLogs {
            testPage(SetupPage::class.java, CalendarPage::class.java) // Database isn't empty.
        }
        // clearDatabase();
        // testPage(SetupPage.class); // Doesn't work (table t_pf_user exists).
    }

    private fun _testAllMountedPages() {
        log.info("Test all web pages with.")
        login(TEST_FULL_ACCESS_USER, TEST_FULL_ACCESS_USER_PASSWORD)
        internalInitialize(systemInfoCache)
        val pages = WebRegistry.getInstance().mountPages
        counter = 0
        for ((_, value) in pages) {
            var skip = false
            for (clazz in skipPages) {
                if (clazz == value) {
                    log.info("Skipping page: $value")
                    skip = true
                }
            }
            if (skip) {
                continue
            }
            testPage(value)
        }
        logout()
    }

    @Suppress("unused")
    private fun testPage(pageClass: Class<out WebPage?>, params: PageParameters) {
        testPage(pageClass, params, pageClass)
    }

    private fun testPage(pageClass: Class<out WebPage?>, expectedRenderedPage: Class<out WebPage?>) {
        testPage(pageClass, null, expectedRenderedPage)
    }

    private fun testPage(
        pageClass: Class<out WebPage?>, params: PageParameters? = null,
        expectedRenderedPage: Class<out WebPage?> = pageClass
    ) {
        log.info("Calling page: " + pageClass.name)
        suppressErrorLogs {
            if (params != null) {
                tester.startPage(pageClass, params)
            } else {
                tester.startPage(pageClass) // suppress
            }
        }
        tester.assertRenderedPage(expectedRenderedPage)
        ++counter
    }

    companion object {
        var counter: Int = 0
    }
}
