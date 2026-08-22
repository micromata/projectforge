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

package org.projectforge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Asserts that the two languages naming the migrated pages agree.
 *
 * The frontend choice per page is declared twice: [NextMigration.categories] decides which url the
 * menu and every server side redirect point to, and `HAND_BUILT_CATEGORIES` in projectforge-next
 * decides whether that url is served by a hand built page or by the generic UILayout renderer. A
 * category present in only one of them fails silently - the user lands on the generic page of an
 * entity that has a hand built one, or on a route that doesn't exist - so it is asserted here
 * instead of found in production.
 *
 * @author Kai Reinhard
 */
class NextMigrationTest {
    @Test
    fun `migrated categories match the hand built categories of projectforge-next`() {
        val file = handBuiltCategoriesFile()
        // Not part of the Java build, so a build without the frontend sources is no failure here.
        Assumptions.assumeTrue(file != null, "projectforge-next/lib/hand-built-categories.ts not found.")
        val content = file!!.readText()
        val array = Regex("""HAND_BUILT_CATEGORIES\s*=\s*\[([^]]*)]""").find(content)?.groupValues?.get(1)
        Assertions.assertNotNull(array, "Can't parse HAND_BUILT_CATEGORIES in ${file.absolutePath}.")
        val categories = Regex("""["']([^"']+)["']""").findAll(array!!).map { it.groupValues[1] }.toSet()
        Assertions.assertEquals(
            NextMigration.categories,
            categories,
            "NextMigration.MIGRATED and HAND_BUILT_CATEGORIES (${file.absolutePath}) disagree. " +
                    "A migrated page has to be listed in both.",
        )
    }

    @Test
    fun `isNextUrl recognizes the urls the menu has to leave Wicket for`() {
        // What WicketMenuBuilder decides on: MenuItemDefId carries the url, not the category, and the
        // menu links are built with and without leading slash.
        Assertions.assertTrue(NextMigration.isNextUrl(NextMigration.listUrl("order")))
        Assertions.assertTrue(NextMigration.isNextUrl("/${NextMigration.listUrl("cost1")}"))
        Assertions.assertFalse(NextMigration.isNextUrl(NextMigration.listUrl("address")))
        Assertions.assertFalse(NextMigration.isNextUrl("wa/cost2List"))
        Assertions.assertFalse(NextMigration.isNextUrl(null))
    }

    /**
     * The invoice was the last list whose form stayed in Wicket, and its release is what these two
     * assertions guard: a row click leads to the next form now, and `RechnungEditForm`'s attachment link
     * ([NextMigration.nextEditPage]) names the same page, so the escape hatch back to Wicket doesn't send
     * anyone in a circle.
     */
    @Test
    fun `the invoice form is next's, and both answers name it`() {
        Assertions.assertEquals("next/invoice/:id", NextMigration.nextEditPage("outgoingInvoice"))
        Assertions.assertEquals("next/invoice/:id", NextMigration.standardEditPage("outgoingInvoice"))
        Assertions.assertEquals("next/invoice/new", NextMigration.newEntryUrl("outgoingInvoice"))
        // For a fully migrated page the two agree - there is only one form.
        Assertions.assertEquals(
            NextMigration.standardEditPage("book"),
            NextMigration.nextEditPage("book"),
        )
        // Not migrated at all, so there is no next form to name.
        Assertions.assertNull(NextMigration.nextEditPage("address"))
    }

    /**
     * The group is the page the task wizard needs: it creates a group in a dialog around the *next* form
     * (see `wizard-group-step.tsx`), so the routes of that form are part of the contract. Migrated from the
     * React app, which is where the escape hatch leads.
     */
    @Test
    fun `the group page is next's, and its way back leads to the React app`() {
        Assertions.assertEquals("next/group", NextMigration.listUrl("group"))
        Assertions.assertEquals("next/group/:id", NextMigration.nextEditPage("group"))
        Assertions.assertEquals("next/group/:id", NextMigration.standardEditPage("group"))
        Assertions.assertEquals("next/group/new", NextMigration.newEntryUrl("group"))
        Assertions.assertEquals("react/group", NextMigration.legacyListUrl("group"))
        Assertions.assertEquals("react/group/edit/:id", NextMigration.legacyEditPage("group"))
    }

    /**
     * The working directory of a test run is the module, not the repository, and both are valid
     * (Gradle vs. IDE), so the project root is searched upwards instead of assumed.
     */
    private fun handBuiltCategoriesFile(): File? {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val file = File(dir, "projectforge-next/lib/hand-built-categories.ts")
            if (file.exists()) {
                return file
            }
            dir = dir.parentFile
        }
        return null
    }
}
