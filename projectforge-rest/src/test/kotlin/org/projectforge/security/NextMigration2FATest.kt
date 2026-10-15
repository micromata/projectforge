/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.NextMigration
import org.projectforge.SystemStatus
import org.projectforge.model.rest.RestPaths

/**
 * A page migrated to projectforge-next has to keep the second factor its legacy page required.
 *
 * The Wicket pages were gated by their own urls (`/wa/orderBookEdit`, `/wa/datev`, ...). A page of projectforge-next
 * cannot be gated that way at all - it is a static file of the export, served by a resource handler (see
 * `WebApplicationConfig`), so no filter sees its url, and a client side navigation inside the app doesn't even reach
 * the server. What is left is the rest call the page makes, which is why every migrated page needs its rest url
 * registered in [ProjectForge2FAInitialization] - as a `WRITE:<category>` entry for the writing shortcuts and as the
 * `*Rest` class for the reading ones.
 *
 * Wicket is gone (every `/wa` url is redirected to next, see `OrphanedLinkFilter`), and so are the `/wa` values of
 * the shortcuts. What they gated is kept here as the list of rest urls that have to require a second factor in their
 * stead: the categories whose Wicket list or form required one, and the standalone pages.
 *
 * Both shortcut sets are configured alone on purpose. Together they hide exactly the gap this test is about: the
 * reading shortcut covers the whole path of a category (`^/rs/order.*`), so a missing `WRITE:order` would still
 * look gated as long as FINANCE is configured too - which an installation is free not to do.
 *
 * @author Kai Reinhard
 */
class NextMigration2FATest {
  /** The categories whose Wicket list page required a second factor (ADMIN, FINANCE, HR). */
  private val gatedOnRead = listOf(
    "access", "account", "accountingRecord", "cost1", "cost2", "cost2Type", "customer", "hrPlanning", "order",
    "outgoingInvoice", "incomingInvoice", "project",
  )

  /** The categories whose Wicket form required a second factor (ADMIN_WRITE, FINANCE_WRITE). */
  private val gatedOnWrite = listOf(
    "access", "account", "configuration", "cost1", "cost2", "cost2Type", "customer", "order", "outgoingInvoice",
    "incomingInvoice", "project",
  )

  /**
   * The standalone pages (no category of [NextMigration]): rest urls their next pages call, successors of Wicket
   * pages that required a second factor on reading.
   */
  private val standaloneOnRead = listOf(
    "/rs/system/reindex", // wa/admin
    "/rs/pluginList/setActivated", // wa/wicket/bookmarkable/org.projectforge.web.admin.PluginListPage
    "/rs/user", "/rs/group", // wa/user*, wa/group*
    "/rs/hrView", // wa/hrList
    "/rs/reportObjectives", // wa/reportObjectives
    "/rs/datevRecordImport", "/rs/datevAccountImport", // wa/datevImport
  )

  /** As [standaloneOnRead], successors of Wicket pages that required a second factor on writing. */
  private val standaloneOnWrite = listOf(
    "/rs/system/reindex",
    "/rs/pluginList/setActivated",
    "/rs/user/${RestPaths.SAVE_OR_UDATE}", "/rs/group/${RestPaths.SAVE_OR_UDATE}", // wa/userEdit, wa/groupEdit
    "/rs/reportObjectives/upload", "/rs/reportObjectives/paste", // wa/reportEdit
    "/rs/datevRecordImport", "/rs/datevAccountImport", // wa/datevImport
  )

  @Test
  fun `reading a migrated page requires the second factor of its legacy page`() {
    val handler = handler("ADMIN;FINANCE;HR;ORGA;SCRIPT")
    (gatedOnRead.map { restUrl(it) } + standaloneOnRead).forEach { url ->
      Assertions.assertNotNull(
        handler.getRemainingPeriod(url),
        "The legacy page of $url required a 2FA, so $url has to require one as well: register its rest class " +
            "in ProjectForge2FAInitialization.",
      )
    }
  }

  @Test
  fun `writing on a migrated page requires the second factor of its legacy form`() {
    val handler = handler("ADMIN_WRITE;FINANCE_WRITE;HR_WRITE;ORGA_WRITE;SCRIPT_WRITE")
    // The save of a hand built page as well as of a UILayout page (see lib/rs/entity.ts):
    (gatedOnWrite.map { "${restUrl(it)}/${RestPaths.SAVE_OR_UDATE}" } + standaloneOnWrite).forEach { url ->
      Assertions.assertNotNull(
        handler.getRemainingPeriod(url),
        "The legacy form of $url required a 2FA, so $url has to require one as well: add WRITE:<category> or " +
            "the rest class to the matching shortcut in ProjectForge2FAInitialization.",
      )
    }
  }

  /** The lists name migrated categories: a renamed category would otherwise silently drop out of the test. */
  @Test
  fun `the gated categories are migrated ones`() {
    (gatedOnRead + gatedOnWrite).forEach { category ->
      Assertions.assertTrue(NextMigration.isMigrated(category), category)
    }
  }

  /**
   * No `/wa` value is left in the shortcuts: Wicket is gone, so they would gate nothing.
   */
  @Test
  fun `no shortcut names a wicket url`() {
    val handler = handler("")
    My2FAShortCut.entries.forEach { shortCut ->
      val resolved = handler.getShortCutResolved(shortCut) ?: return@forEach
      Assertions.assertFalse(resolved.contains("/wa"), "$shortCut: $resolved")
    }
  }

  /**
   * The rest url of a category: [NextMigration] is keyed by the rest category, which is the path of its
   * `*Rest` class (`/rs/order`), so no lookup is needed.
   */
  private fun restUrl(category: String): String {
    return "/rs/$category"
  }

  /**
   * @param shortCuts The shortcuts to configure for one expiry period, e.g. `FINANCE_WRITE`. All others are left
   * unconfigured, i.e. require no second factor.
   */
  private fun handler(shortCuts: String): My2FARequestHandler {
    SystemStatus.internalSet4JunitTests(true) // For receiving exceptions on failure instead of log error messages.
    val initialization = ProjectForge2FAInitialization()
    val handler = My2FARequestHandler()
    initialization.my2FARequestHandler = handler
    val configuration = My2FARequestConfiguration()
    configuration.internalSet4TestCases(expiryPeriodHours8 = shortCuts)
    handler.internalSet4UnitTests(configuration)
    initialization.init()
    return handler
  }
}
