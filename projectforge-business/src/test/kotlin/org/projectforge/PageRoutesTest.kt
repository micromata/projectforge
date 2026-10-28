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

    @Test
    fun `routes of pages whose route differs from the category`() {
        Assertions.assertEquals("next/invoice/:id", PageRoutes.standardEditPage("outgoingInvoice"))
        Assertions.assertEquals("next/invoice/new", PageRoutes.newEntryUrl("outgoingInvoice"))
        Assertions.assertEquals("next/address-campaign-value", PageRoutes.listUrl("addressCampaignValue"))
        Assertions.assertEquals("next/group/:id", PageRoutes.standardEditPage("group"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "addressCampaignValue" })
    }

/**
 * Asserts the page registry [PageRoutes]: its routes and the redirects of old links.
 *
 * @author Kai Reinhard
 */
class PageRoutesTest {
    /**
     * The menu and every server side redirect of a migrated category point to its route in projectforge-next,
     * so a route without a page there would be a dead link - asserted here instead of found in production.
     */
    @Test
    fun `every migrated route has a page in projectforge-next`() {
        val appDir = nextAppDir()
        // Not part of the Java build, so a build without the frontend sources is no failure here.
        Assumptions.assumeTrue(appDir != null, "projectforge-next/app/(authenticated) not found.")
        PageRoutes.categories.forEach { category ->
            val route = PageRoutes.page(category)!!.route
            Assertions.assertTrue(
                File(appDir, route).isDirectory,
                "Route '$route' of the migrated category '$category' has no page in ${appDir!!.absolutePath}.",
            )
        }
    }

    @Test
    fun `isNextUrl recognizes the urls the menu has to leave Wicket for`() {
        // What WicketMenuBuilder decides on: MenuItemDefId carries the url, not the category, and the
        // menu links are built with and without leading slash.
        Assertions.assertTrue(PageRoutes.isNextUrl(PageRoutes.listUrl("order")))
        Assertions.assertTrue(PageRoutes.isNextUrl("/${PageRoutes.listUrl("cost1")}"))
        // A category no page has (every React list is migrated by now): not next's.
        Assertions.assertFalse(PageRoutes.isNextUrl(PageRoutes.listUrl("notMigratedCategory")))
        Assertions.assertFalse(PageRoutes.isNextUrl("wa/cost2List"))
        Assertions.assertFalse(PageRoutes.isNextUrl(null))
    }

    /**
     * The data transfer areas: the row click leads to the file view (`datatransfer/:id`), the admin form is a
     * sub route of it. The React pages are gone, so there is no way back, and as no link of the list or the form
     * ever left ProjectForge, none is redirected (the mailed links of the file view are, by OrphanedLinkFilter).
     */
    @Test
    fun `the data transfer pages are next's and offer no way back`() {
        Assertions.assertEquals("next/datatransfer", PageRoutes.listUrl("datatransfer"))
        Assertions.assertEquals("next/datatransfer/:id/edit", PageRoutes.standardEditPage("datatransfer"))
        Assertions.assertEquals("next/datatransfer/new", PageRoutes.newEntryUrl("datatransfer"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "datatransfer" })
    }

    /**
     * The scripts: the row click leads to the execution (`script/:id`, `myscript/:id`), the form of a script
     * is a sub route of it, the user's own script list has none. The React pages are gone, so there is no
     * way back, and as no link of them ever left ProjectForge (no mail, no data transfer), none is redirected.
     */
    @Test
    fun `the script pages are next's and offer no way back`() {
        Assertions.assertEquals("next/script", PageRoutes.listUrl("script"))
        Assertions.assertEquals("next/script/:id/edit", PageRoutes.standardEditPage("script"))
        Assertions.assertEquals("next/script/new", PageRoutes.newEntryUrl("script"))
        Assertions.assertEquals("next/myscript", PageRoutes.listUrl("myscript"))
        Assertions.assertEquals("next/myscript/:id", PageRoutes.standardEditPage("myscript"))
        for (category in listOf("script", "myscript")) {
            Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == category })
        }
    }

    /**
     * The address books: list and form are next's, the React pages are gone, so there is no way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the address book pages are next's and offer no way back`() {
        Assertions.assertEquals("next/addressBook", PageRoutes.listUrl("addressBook"))
        Assertions.assertEquals("next/addressBook/:id", PageRoutes.standardEditPage("addressBook"))
        Assertions.assertEquals("next/addressBook/new", PageRoutes.newEntryUrl("addressBook"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "addressBook" })
    }

    /**
     * The addresses: list, view and form are next's, the form a sub route of the view. There is no way back, and as
     * no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the address pages are next's and offer no way back`() {
        Assertions.assertEquals("next/address", PageRoutes.listUrl("address"))
        Assertions.assertEquals("next/address/:id/edit", PageRoutes.standardEditPage("address"))
        Assertions.assertEquals("next/address/new", PageRoutes.newEntryUrl("address"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "address" })
    }

    /**
     * The team calendars: list and form are next's, the React pages are gone, so there is no way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the team calendar pages are next's and offer no way back`() {
        Assertions.assertEquals("next/teamCal", PageRoutes.listUrl("teamCal"))
        Assertions.assertEquals("next/teamCal/:id", PageRoutes.standardEditPage("teamCal"))
        Assertions.assertEquals("next/teamCal/new", PageRoutes.newEntryUrl("teamCal"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "teamCal" })
    }

    /**
     * The users: list and form are next's, the React pages are gone, so there is no way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the user pages are next's and offer no way back`() {
        Assertions.assertEquals("next/user", PageRoutes.listUrl("user"))
        Assertions.assertEquals("next/user/:id", PageRoutes.standardEditPage("user"))
        Assertions.assertEquals("next/user/new", PageRoutes.newEntryUrl("user"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "user" })
    }

    /**
     * The vacation entries: list and form are next's, the React pages are gone, so there is no way back, but a
     * link of an old vacation mail (react/vacation/edit/<id>) is still redirected onto next.
     */
    @Test
    fun `the vacation pages are next's and offer no way back`() {
        Assertions.assertEquals("next/vacation", PageRoutes.listUrl("vacation"))
        Assertions.assertEquals("next/vacation/:id", PageRoutes.standardEditPage("vacation"))
        Assertions.assertEquals("next/vacation/new", PageRoutes.newEntryUrl("vacation"))
        val link = PageRoutes.orphanedLinks().single { it.category == "vacation" }
        Assertions.assertEquals("react/vacation", link.legacyListPath)
        Assertions.assertEquals("react/vacation/edit", link.legacyEditPath)
    }

    /**
     * The leave account entries: list and form are next's, the React pages are gone, so there is no way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the leave account entry pages are next's and offer no way back`() {
        Assertions.assertEquals("next/leaveAccountEntry", PageRoutes.listUrl("leaveAccountEntry"))
        Assertions.assertEquals("next/leaveAccountEntry/:id", PageRoutes.standardEditPage("leaveAccountEntry"))
        Assertions.assertEquals("next/leaveAccountEntry/new", PageRoutes.newEntryUrl("leaveAccountEntry"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "leaveAccountEntry" })
    }

    /**
     * The contracts: list and form are next's, the React pages are gone, so there is no way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the contract pages are next's and offer no way back`() {
        Assertions.assertEquals("next/contract", PageRoutes.listUrl("contract"))
        Assertions.assertEquals("next/contract/:id", PageRoutes.standardEditPage("contract"))
        Assertions.assertEquals("next/contract/new", PageRoutes.newEntryUrl("contract"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "contract" })
    }

    /**
     * The Word® templates of Merlin: the row click leads to the execution (`merlin/:id`), the form is a sub route
     * of it. The React pages are gone, so there is no way back, and as no link of them ever left ProjectForge, none
     * is redirected.
     */
    @Test
    fun `the merlin pages are next's and offer no way back`() {
        Assertions.assertEquals("next/merlin", PageRoutes.listUrl("merlin"))
        Assertions.assertEquals("next/merlin/:id/edit", PageRoutes.standardEditPage("merlin"))
        Assertions.assertEquals("next/merlin/new", PageRoutes.newEntryUrl("merlin"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "merlin" })
    }

    /**
     * The memos: list and form are next's, the React pages are gone, so there is no way back, and as no link of
     * them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the memo pages are next's and offer no way back`() {
        Assertions.assertEquals("next/memo", PageRoutes.listUrl("memo"))
        Assertions.assertEquals("next/memo/:id", PageRoutes.standardEditPage("memo"))
        Assertions.assertEquals("next/memo/new", PageRoutes.newEntryUrl("memo"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "memo" })
    }

    /**
     * The skill matrix: list and form are next's, the React pages are gone, so there is no way back, and as no
     * link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the skill matrix pages are next's and offer no way back`() {
        Assertions.assertEquals("next/skillentry", PageRoutes.listUrl("skillentry"))
        Assertions.assertEquals("next/skillentry/:id", PageRoutes.standardEditPage("skillentry"))
        Assertions.assertEquals("next/skillentry/new", PageRoutes.newEntryUrl("skillentry"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "skillentry" })
    }

    /**
     * The bank accounts and their transactions (banking plugin): list and form are next's, the React pages are gone,
     * so there is no way back and no redirect of old links.
     */
    @Test
    fun `the banking pages are next's and offer no way back`() {
        listOf("bankAccount", "bankAccountRecord").forEach { category ->
            Assertions.assertEquals("next/$category", PageRoutes.listUrl(category))
            Assertions.assertEquals("next/$category/:id", PageRoutes.standardEditPage(category))
            Assertions.assertEquals("next/$category/new", PageRoutes.newEntryUrl(category))
            Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == category })
        }
    }

    /**
     * The polls: list, form and answer page are next's, the React pages are gone, so there is no way back, and as no link of them ever left ProjectForge, none is redirected (the links of the poll mails to the answer page are, by OrphanedLinkFilter).
     */
    @Test
    fun `the poll pages are next's and offer no way back`() {
        Assertions.assertEquals("next/poll", PageRoutes.listUrl("poll"))
        Assertions.assertEquals("next/poll/:id", PageRoutes.standardEditPage("poll"))
        Assertions.assertEquals("next/poll/new", PageRoutes.newEntryUrl("poll"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "poll" })
    }

    /**
     * The inbox and the outbox: list and form are next's, the React pages are gone, so there is no way back, and as
     * no link of them ever left ProjectForge (no mail, no data transfer), none is redirected.
     */
    @Test
    fun `the inbox and outbox pages are next's and offer no way back`() {
        listOf("incomingMail", "outgoingMail").forEach { category ->
            Assertions.assertEquals("next/$category", PageRoutes.listUrl(category))
            Assertions.assertEquals("next/$category/:id", PageRoutes.standardEditPage(category))
            Assertions.assertEquals("next/$category/new", PageRoutes.newEntryUrl(category))
            Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == category })
        }
    }

    /**
     * The visitor book: list and form (the visits included) are next's, the React pages are gone, so there is no
     * way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the visitorbook pages are next's and offer no way back`() {
        Assertions.assertEquals("next/visitorbook", PageRoutes.listUrl("visitorbook"))
        Assertions.assertEquals("next/visitorbook/:id", PageRoutes.standardEditPage("visitorbook"))
        Assertions.assertEquals("next/visitorbook/new", PageRoutes.newEntryUrl("visitorbook"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "visitorbook" })
    }

    /**
     * The employees: list and form (time-dependent attributes included) are next's, the React pages are gone, so
     * there is no way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the employee pages are next's and offer no way back`() {
        Assertions.assertEquals("next/employee", PageRoutes.listUrl("employee"))
        Assertions.assertEquals("next/employee/:id", PageRoutes.standardEditPage("employee"))
        Assertions.assertEquals("next/employee/new", PageRoutes.newEntryUrl("employee"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "employee" })
    }

    /**
     * The currency pairs: list and form (conversion rates included) are next's, the React pages are gone, so there
     * is no way back, and as no link of them ever left ProjectForge, none is redirected.
     */
    @Test
    fun `the currency pair pages are next's and offer no way back`() {
        Assertions.assertEquals("next/currencyPair", PageRoutes.listUrl("currencyPair"))
        Assertions.assertEquals("next/currencyPair/:id", PageRoutes.standardEditPage("currencyPair"))
        Assertions.assertEquals("next/currencyPair/new", PageRoutes.newEntryUrl("currencyPair"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "currencyPair" })
    }

    /**
     * The HR planning list serves its metadata under the category of its rows, `hrPlanningEntry`: a category
     * unknown to [PageRoutes] would count as a React page and offer a dead link to `react/hrPlanningEntry`.
     */
    @Test
    fun `the HR planning offers no way back`() {
        for (category in listOf("hrPlanning", "hrPlanningEntry")) {
        }
        Assertions.assertEquals("next/hrPlanning", PageRoutes.listUrl("hrPlanningEntry"))
    }

    /** The team event form is next's, the React form is gone: no way back, and the invitation mails never linked it. */
    @Test
    fun `the team event form is next's, without a way back`() {
        Assertions.assertEquals("next/teamEvent/:id", PageRoutes.standardEditPage("teamEvent"))
        Assertions.assertEquals("next/teamEvent/new", PageRoutes.newEntryUrl("teamEvent"))
        Assertions.assertTrue(PageRoutes.orphanedLinks().none { it.category == "teamEvent" })
    }

    /**
     * Old links are only redirected where they have left ProjectForge (see Page.legacyApp): the order and to-do
     * change mails and the vacation mails linked the legacy forms. Next to them only the calendar, the former page
     * after login.
     */
    @Test
    fun `only links that left ProjectForge are redirected`() {
        Assertions.assertEquals(
            setOf("order", "todo", "vacation", "calendar"),
            PageRoutes.orphanedLinks().map { it.category }.toSet(),
        )
    }

    /**
     * OrphanedLinkFilter redirects each migrated page in its own legacy app: a page migrated from Wicket
     * orphans its `wa/...` links, one from React its `react/...` links, and a page whose legacy form is
     * gone (`book`) has no legacy link to orphan.
     */
    @Test
    fun `orphaned links map each migrated legacy page to its next url`() {
        val byList = PageRoutes.orphanedLinks().associateBy { it.legacyListPath }

        val order = byList["wa/orderBookList"]
        Assertions.assertNotNull(order, "The order page was migrated from Wicket and must be redirected.")
        Assertions.assertEquals(PageRoutes.LegacyApp.WICKET, order!!.legacyApp)
        Assertions.assertEquals("wa/orderBookEdit", order.legacyEditPath) // Id is the `id` query parameter.
        Assertions.assertEquals("/next/order", order.nextListUrl)
        Assertions.assertEquals("/next/order/:id", order.nextEditUrl)
        Assertions.assertEquals("/next/order/new", order.nextNewEntryUrl)

        val vacation = byList["react/vacation"]
        Assertions.assertNotNull(vacation, "The vacation page was migrated from the React app and must be redirected.")
        Assertions.assertEquals(PageRoutes.LegacyApp.REACT, vacation!!.legacyApp)
        Assertions.assertEquals("react/vacation/edit", vacation.legacyEditPath) // Id is a path segment.
        Assertions.assertEquals("/next/vacation", vacation.nextListUrl)

        // The to-dos: the bookmarkable url of the Wicket form, the link of the notification mails.
        val todo = PageRoutes.orphanedLinks().single { it.category == "todo" }
        Assertions.assertEquals("wa/wicket/bookmarkable/org.projectforge.plugins.todo.ToDoEditPage", todo.legacyEditPath)
        Assertions.assertEquals("/next/todo/:id", todo.nextEditUrl)

        // The book's legacy form was removed (legacyApp == null), so there is no legacy link to bend.
        Assertions.assertTrue(
            PageRoutes.orphanedLinks().none { it.nextListUrl == "/next/book" },
            "A page whose legacy implementation is gone has no orphaned link.",
        )
    }

    /**
     * The working directory of a test run is the module, not the repository, and both are valid
     * (Gradle vs. IDE), so the project root is searched upwards instead of assumed.
     */
    private fun nextAppDir(): File? {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val appDir = File(dir, "projectforge-next/app/(authenticated)")
            if (appDir.isDirectory) {
                return appDir
            }
            dir = dir.parentFile
        }
        return null
    }
}
