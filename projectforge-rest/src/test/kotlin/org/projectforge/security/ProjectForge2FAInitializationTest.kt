/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.security

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.SystemStatus
import org.projectforge.rest.ChangePasswordPageRest
import org.projectforge.rest.ChangeWlanPasswordPageRest
import org.projectforge.rest.admin.AdminLogViewerPageRest
import org.projectforge.ui.AutoCompletion

class ProjectForge2FAInitializationTest {
  @Test
  fun configTest() {
    SystemStatus.internalSet4JunitTests(true) // For receiving exceptions on failure instead of log error messages.
    val initialization = ProjectForge2FAInitialization()
    val my2FARequestHandler = My2FARequestHandler()
    initialization.my2FARequestHandler = my2FARequestHandler
    val config = My2FARequestConfiguration()
    config.internalSet4TestCases(expiryPeriodHours8 = "ADMIN;SCRIPT;FINANCE_WRITE;ORGA_WRITE;HR_WRITE")
    initialization.my2FARequestHandler.internalSet4UnitTests(config)
    initialization.init()
    initialization.registerShortCutClasses(My2FAShortCut.INTERNAL_TEST, ChangePasswordPageRest::class.java)
    initialization.registerShortCutClasses(My2FAShortCut.INTERNAL_TEST, ChangeWlanPasswordPageRest::class.java)
    Assertions.assertEquals("/rs/changePassword;/rs/changeWlanPassword;", my2FARequestHandler.getShortCutResolved(
      My2FAShortCut.INTERNAL_TEST
    ))
    my2FARequestHandler.registerShortCutValues(My2FAShortCut.INTERNAL_TEST2, "/rs/abc", "/rs/cde;")
    try {
      initialization.registerShortCutMethods(My2FAShortCut.INTERNAL_TEST2, ChangePasswordPageRest::class.java)
      Assertions.fail("IllegalArgumentException expected, because methods not given.")
    } catch (ex: IllegalArgumentException) {
      // OK
    }
    initialization.registerShortCutClasses(My2FAShortCut.INTERNAL_TEST2, ChangePasswordPageRest::class.java)
    Assertions.assertEquals("/rs/abc;/rs/cde;/rs/changePassword;", my2FARequestHandler.getShortCutResolved(My2FAShortCut.INTERNAL_TEST2))
    initialization.registerShortCutMethods(My2FAShortCut.INTERNAL_TEST3, AdminLogViewerPageRest::class.java, AdminLogViewerPageRest::search, AdminLogViewerPageRest::refresh)
    Assertions.assertEquals("/rs/adminLogViewer/search;/rs/adminLogViewer/refresh;", my2FARequestHandler.getShortCutResolved(
      My2FAShortCut.INTERNAL_TEST3
    ))

    Assertions.assertEquals("/rs/myAccount;/rs/tokenInfo;/rs/user/renewToken;", my2FARequestHandler.getShortCutResolved(
      My2FAShortCut.MY_ACCOUNT
    ))

    Assertions.assertNull(my2FARequestHandler.getRemainingPeriod("/rs/user/${AutoCompletion.AUTOCOMPLETE_TEXT}"))
    Assertions.assertEquals(0L, my2FARequestHandler.getRemainingPeriod("/rs/user/notautosearch"), "New 2FA requested for /rs/user*.")
    Assertions.assertNull(my2FARequestHandler.getRemainingPeriod("/rs/user/${AutoCompletion.AUTOCOMPLETE_TEXT}?search=abc"))
    Assertions.assertNull(my2FARequestHandler.getRemainingPeriod("/rs/user/${AutoCompletion.AUTOCOMPLETE_OBJECT}"))
    Assertions.assertNull(my2FARequestHandler.getRemainingPeriod("/rs/user/${AutoCompletion.AUTOCOMPLETE_OBJECT}?search=abc"))
  }
}
