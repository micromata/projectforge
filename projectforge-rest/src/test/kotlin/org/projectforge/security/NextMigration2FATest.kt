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
 * The legacy frontends are gated by the url of the page itself: `/wa/orderBookEdit` for Wicket
 * (`WicketUserFilter`), `/react/...` for the legacy React app. A page of projectforge-next cannot be gated that way
 * at all - it is a static file of the export, served by a resource handler (see `WebApplicationConfig`), so no
 * filter sees its url, and a client side navigation inside the app doesn't even reach the server. What is left is
 * the rest call the page makes, which is why every migrated page needs its rest url registered in
 * [ProjectForge2FAInitialization] - as a `WRITE:<category>` entry for the writing shortcuts and as the
 * `*Rest` class for the reading ones.
 *
 * That is easy to forget while migrating the next page, and nothing fails visibly: the page simply works without
 * a second factor. Hence this test, which asks for every category of [NextMigration] whether its rest url is
 * gated wherever its legacy url was.
 *
 * Both shortcut sets are configured alone on purpose. Together they hide exactly the gap this test is about: the
 * reading shortcut covers the whole path of a category (`^/rs/order.*`), so a missing `WRITE:order` would still
 * look gated as long as FINANCE is configured too - which an installation is free not to do.
 *
 * @author Kai Reinhard
 */
class NextMigration2FATest {
  @Test
  fun `reading a migrated page requires the second factor of its legacy list page`() {
    val handler = handler("ADMIN;FINANCE;HR;ORGA;SCRIPT")
    NextMigration.categories.forEach { category ->
      val legacyUrl = uriOf(NextMigration.legacyListUrl(category)) ?: return@forEach
      if (handler.getRemainingPeriod(legacyUrl) == null) {
        return@forEach // The legacy list page needs no second factor, so the migrated one doesn't either.
      }
      Assertions.assertNotNull(
        handler.getRemainingPeriod(restUrl(category)),
        "$legacyUrl requires a 2FA, so ${restUrl(category)} of the migrated page has to require one as well: " +
            "register its rest class in ProjectForge2FAInitialization.",
      )
    }
  }

  @Test
  fun `writing on a migrated page requires the second factor of its legacy form`() {
    val handler = handler("ADMIN_WRITE;FINANCE_WRITE;HR_WRITE;ORGA_WRITE;SCRIPT_WRITE")
    NextMigration.categories.forEach { category ->
      val legacyUrl = uriOf(NextMigration.legacyEditPage(category)) ?: return@forEach
      if (handler.getRemainingPeriod(legacyUrl) == null) {
        return@forEach // The legacy form needs no second factor, so the migrated one doesn't either.
      }
      // The save of a hand built page as well as of a UILayout page (see lib/rs/entity.ts):
      val saveUrl = "${restUrl(category)}/${RestPaths.SAVE_OR_UDATE}"
      Assertions.assertNotNull(
        handler.getRemainingPeriod(saveUrl),
        "$legacyUrl requires a 2FA, so $saveUrl of the migrated page has to require one as well: " +
            "add WRITE:$category to the matching shortcut in ProjectForge2FAInitialization.",
      )
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
   * @return The url as a request uri would carry it (leading slash, without query string), or null if the page has
   * no legacy counterpart any more - then there is nothing to keep.
   */
  private fun uriOf(url: String?): String? {
    return url?.let { "/${it.substringBefore('?')}" }
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
