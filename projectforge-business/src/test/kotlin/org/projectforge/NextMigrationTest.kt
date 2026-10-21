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
 * Asserts the migration registry [NextMigration]: its routes, redirects and legacy urls.
 *
 * @author Kai Reinhard
 */
class NextMigrationTest {
    /**
     * The menu and every server side redirect of a migrated category point to its route in projectforge-next,
     * so a route without a page there would be a dead link - asserted here instead of found in production.
     */
    @Test
    fun `every migrated route has a page in projectforge-next`() {
        val appDir = nextAppDir()
        // Not part of the Java build, so a build without the frontend sources is no failure here.
        Assumptions.assumeTrue(appDir != null, "projectforge-next/app/(authenticated) not found.")
        NextMigration.categories.forEach { category ->
            val route = NextMigration.nextPage(category)!!.route
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
        Assertions.assertTrue(NextMigration.isNextUrl(NextMigration.listUrl("order")))
        Assertions.assertTrue(NextMigration.isNextUrl("/${NextMigration.listUrl("cost1")}"))
        // A category no page has (every React list is migrated by now): not next's.
        Assertions.assertFalse(NextMigration.isNextUrl(NextMigration.listUrl("notMigratedCategory")))
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
        // Not migrated at all (a category no page has), so there is no next form to name.
        Assertions.assertNull(NextMigration.nextEditPage("notMigratedCategory"))
    }

    /**
     * The address campaign values offer the way back to their React page, which is kept under a category of its
     * own for the running campaign; a new value has no React url of its own (it needs a row of the list).
     */
    @Test
    fun `the address campaign values lead back to the legacy category`() {
        Assertions.assertEquals("next/address-campaign-value", NextMigration.listUrl("addressCampaignValue"))
        Assertions.assertEquals("react/addressCampaignValueLegacy", NextMigration.legacyListUrl("addressCampaignValue"))
        Assertions.assertEquals(
            "react/addressCampaignValueLegacy/edit/:id",
            NextMigration.legacyEditPage("addressCampaignValue"),
        )
        Assertions.assertEquals("react/addressCampaignValueLegacy", NextMigration.legacyNewEntryUrl("addressCampaignValue"))
        Assertions.assertFalse(NextMigration.isMigrated("addressCampaignValueLegacy"))
        // The old urls are next's, the legacy category's pages show no hint (and aren't redirected, see
        // OrphanedLinkFilterTest).
        Assertions.assertTrue(NextMigration.hasNextVersion(NextMigration.LegacyApp.REACT, "addressCampaignValue"))
        Assertions.assertFalse(NextMigration.hasNextVersion(NextMigration.LegacyApp.REACT, "addressCampaignValueLegacy"))
        Assertions.assertFalse(
            NextMigration.hasNextVersion(NextMigration.LegacyApp.REACT, "addressCampaignValueLegacy/edit/42")
        )
        val link = NextMigration.orphanedLinks().single { it.category == "addressCampaignValue" }
        Assertions.assertEquals("react/addressCampaignValue", link.legacyListPath)
        Assertions.assertEquals("react/addressCampaignValue/edit", link.legacyEditPath)
    }

    /**
     * The invoices and timesheets no longer offer the way back (see NextPage.offerLegacyLink): neither the
     * creditor, the debtor nor the timesheet page shows a "classic version" link, so every escape-hatch
     * accessor answers null. The
     * legacyApp stays all the same, so a bookmarked Wicket link is still redirected onto next - asserted in
     * `orphaned links...` below.
     */
    @Test
    fun `the invoices and timesheets offer no way back`() {
        for (category in listOf("outgoingInvoice", "incomingInvoice", "timesheet")) {
            Assertions.assertNull(NextMigration.legacyListUrl(category), category)
            Assertions.assertNull(NextMigration.legacyEditPage(category), category)
            Assertions.assertNull(NextMigration.legacyNewEntryUrl(category), category)
            Assertions.assertFalse(NextMigration.legacyListInMenu(category), category)
        }
    }

    /**
     * Wicket is no way back any more (it is being removed): no page migrated from Wicket offers a "classic
     * version" link, and NextPage refuses one that would. Their legacyApp stays, so a bookmarked Wicket link is
     * still redirected onto next.
     */
    @Test
    fun `no page offers a way back to Wicket`() {
        val wicketLinks = NextMigration.orphanedLinks().filter { it.legacyApp == NextMigration.LegacyApp.WICKET }
        for (category in listOf("access", "account", "accountingRecord", "gantt", "task")) {
            Assertions.assertNull(NextMigration.legacyListUrl(category), category)
            Assertions.assertNull(NextMigration.legacyEditPage(category), category)
            Assertions.assertNull(NextMigration.legacyNewEntryUrl(category), category)
            Assertions.assertFalse(NextMigration.legacyListInMenu(category), category)
            Assertions.assertTrue(
                wicketLinks.any { it.nextListUrl == "/${NextMigration.listUrl(category)}" },
                "A bookmarked Wicket link of $category must still be redirected.",
            )
        }
        NextMigration.categories.forEach { category ->
            listOfNotNull(
                NextMigration.legacyListUrl(category),
                NextMigration.legacyEditPage(category),
                NextMigration.legacyNewEntryUrl(category),
            ).forEach { url ->
                Assertions.assertFalse(url.startsWith(Constants.WICKET_APPLICATION_PATH), "$category: $url")
            }
        }
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            NextMigration.NextPage(route = "x", legacyApp = NextMigration.LegacyApp.WICKET)
        }
    }

    /**
     * The data transfer areas: the row click leads to the file view (`datatransfer/:id`), the admin form is a
     * sub route of it. The React pages are gone, so there is no way back, but a bookmarked React link of the
     * list or the form is still redirected onto next.
     */
    @Test
    fun `the data transfer pages are next's and offer no way back`() {
        Assertions.assertEquals("next/datatransfer", NextMigration.listUrl("datatransfer"))
        Assertions.assertEquals("next/datatransfer/:id/edit", NextMigration.standardEditPage("datatransfer"))
        Assertions.assertEquals("next/datatransfer/new", NextMigration.newEntryUrl("datatransfer"))
        Assertions.assertNull(NextMigration.legacyListUrl("datatransfer"))
        Assertions.assertNull(NextMigration.legacyEditPage("datatransfer"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("datatransfer"))
        val link = NextMigration.orphanedLinks().single { it.category == "datatransfer" }
        Assertions.assertEquals("react/datatransfer", link.legacyListPath)
        Assertions.assertEquals("react/datatransfer/edit", link.legacyEditPath)
    }

    /**
     * The scripts: the row click leads to the execution (`script/:id`, `myscript/:id`), the form of a script
     * is a sub route of it, the user's own script list has none. The React pages are gone, so there is no
     * way back, but a bookmarked React link of the lists or the form is still redirected onto next.
     */
    @Test
    fun `the script pages are next's and offer no way back`() {
        Assertions.assertEquals("next/script", NextMigration.listUrl("script"))
        Assertions.assertEquals("next/script/:id/edit", NextMigration.standardEditPage("script"))
        Assertions.assertEquals("next/script/new", NextMigration.newEntryUrl("script"))
        Assertions.assertEquals("next/myscript", NextMigration.listUrl("myscript"))
        Assertions.assertEquals("next/myscript/:id", NextMigration.standardEditPage("myscript"))
        for (category in listOf("script", "myscript")) {
            Assertions.assertNull(NextMigration.legacyListUrl(category), category)
            Assertions.assertNull(NextMigration.legacyEditPage(category), category)
            Assertions.assertNull(NextMigration.legacyNewEntryUrl(category), category)
            Assertions.assertTrue(NextMigration.orphanedLinks().none { it.category == category })
        }
    }

    /**
     * The address books: list and form are next's, the React pages are gone, so there is no way back, but a
     * bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the address book pages are next's and offer no way back`() {
        Assertions.assertEquals("next/addressBook", NextMigration.listUrl("addressBook"))
        Assertions.assertEquals("next/addressBook/:id", NextMigration.standardEditPage("addressBook"))
        Assertions.assertEquals("next/addressBook/new", NextMigration.newEntryUrl("addressBook"))
        Assertions.assertNull(NextMigration.legacyListUrl("addressBook"))
        Assertions.assertNull(NextMigration.legacyEditPage("addressBook"))
        val link = NextMigration.orphanedLinks().single { it.category == "addressBook" }
        Assertions.assertEquals("react/addressBook", link.legacyListPath)
        Assertions.assertEquals("react/addressBook/edit", link.legacyEditPath)
    }

    /**
     * The addresses: list, view and form are next's, the form a sub route of the view. There is no way back (the
     * React form left for the address campaigns has a category of its own, `addressLegacy`), but a bookmarked React
     * link is still redirected onto next.
     */
    @Test
    fun `the address pages are next's and offer no way back`() {
        Assertions.assertEquals("next/address", NextMigration.listUrl("address"))
        Assertions.assertEquals("next/address/:id/edit", NextMigration.standardEditPage("address"))
        Assertions.assertEquals("next/address/new", NextMigration.newEntryUrl("address"))
        Assertions.assertNull(NextMigration.legacyListUrl("address"))
        Assertions.assertNull(NextMigration.legacyEditPage("address"))
        Assertions.assertFalse(NextMigration.isMigrated("addressLegacy"))
        Assertions.assertEquals("react/addressLegacy/edit/:id", NextMigration.standardEditPage("addressLegacy"))
        val link = NextMigration.orphanedLinks().single { it.category == "address" }
        Assertions.assertEquals("react/address", link.legacyListPath)
        Assertions.assertEquals("react/address/edit", link.legacyEditPath)
        Assertions.assertEquals("/next/address/:id/edit", link.nextEditUrl)
    }

    /**
     * The team calendars: list and form are next's, the React pages are gone, so there is no way back, but a
     * bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the team calendar pages are next's and offer no way back`() {
        Assertions.assertEquals("next/teamCal", NextMigration.listUrl("teamCal"))
        Assertions.assertEquals("next/teamCal/:id", NextMigration.standardEditPage("teamCal"))
        Assertions.assertEquals("next/teamCal/new", NextMigration.newEntryUrl("teamCal"))
        Assertions.assertNull(NextMigration.legacyListUrl("teamCal"))
        Assertions.assertNull(NextMigration.legacyEditPage("teamCal"))
        val link = NextMigration.orphanedLinks().single { it.category == "teamCal" }
        Assertions.assertEquals("react/teamCal", link.legacyListPath)
        Assertions.assertEquals("react/teamCal/edit", link.legacyEditPath)
    }

    /**
     * The users: list and form are next's, the React pages are gone, so there is no way back, but a bookmarked
     * React link is still redirected onto next.
     */
    @Test
    fun `the user pages are next's and offer no way back`() {
        Assertions.assertEquals("next/user", NextMigration.listUrl("user"))
        Assertions.assertEquals("next/user/:id", NextMigration.standardEditPage("user"))
        Assertions.assertEquals("next/user/new", NextMigration.newEntryUrl("user"))
        Assertions.assertNull(NextMigration.legacyListUrl("user"))
        Assertions.assertNull(NextMigration.legacyEditPage("user"))
        val link = NextMigration.orphanedLinks().single { it.category == "user" }
        Assertions.assertEquals("react/user", link.legacyListPath)
        Assertions.assertEquals("react/user/edit", link.legacyEditPath)
    }

    /**
     * The vacation entries: list and form are next's, the React pages are gone, so there is no way back, but a
     * bookmarked React link or one of an old notification mail is still redirected onto next.
     */
    @Test
    fun `the vacation pages are next's and offer no way back`() {
        Assertions.assertEquals("next/vacation", NextMigration.listUrl("vacation"))
        Assertions.assertEquals("next/vacation/:id", NextMigration.standardEditPage("vacation"))
        Assertions.assertEquals("next/vacation/new", NextMigration.newEntryUrl("vacation"))
        Assertions.assertNull(NextMigration.legacyListUrl("vacation"))
        Assertions.assertNull(NextMigration.legacyEditPage("vacation"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("vacation"))
        val link = NextMigration.orphanedLinks().single { it.category == "vacation" }
        Assertions.assertEquals("react/vacation", link.legacyListPath)
        Assertions.assertEquals("react/vacation/edit", link.legacyEditPath)
    }

    /**
     * The leave account entries: list and form are next's, the React pages are gone, so there is no way back, but a
     * bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the leave account entry pages are next's and offer no way back`() {
        Assertions.assertEquals("next/leaveAccountEntry", NextMigration.listUrl("leaveAccountEntry"))
        Assertions.assertEquals("next/leaveAccountEntry/:id", NextMigration.standardEditPage("leaveAccountEntry"))
        Assertions.assertEquals("next/leaveAccountEntry/new", NextMigration.newEntryUrl("leaveAccountEntry"))
        Assertions.assertNull(NextMigration.legacyListUrl("leaveAccountEntry"))
        Assertions.assertNull(NextMigration.legacyEditPage("leaveAccountEntry"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("leaveAccountEntry"))
        val link = NextMigration.orphanedLinks().single { it.category == "leaveAccountEntry" }
        Assertions.assertEquals("react/leaveAccountEntry", link.legacyListPath)
        Assertions.assertEquals("react/leaveAccountEntry/edit", link.legacyEditPath)
    }

    /**
     * The contracts: list and form are next's, the React pages are gone, so there is no way back, but a bookmarked
     * React link is still redirected onto next.
     */
    @Test
    fun `the contract pages are next's and offer no way back`() {
        Assertions.assertEquals("next/contract", NextMigration.listUrl("contract"))
        Assertions.assertEquals("next/contract/:id", NextMigration.standardEditPage("contract"))
        Assertions.assertEquals("next/contract/new", NextMigration.newEntryUrl("contract"))
        Assertions.assertNull(NextMigration.legacyListUrl("contract"))
        Assertions.assertNull(NextMigration.legacyEditPage("contract"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("contract"))
        val link = NextMigration.orphanedLinks().single { it.category == "contract" }
        Assertions.assertEquals("react/contract", link.legacyListPath)
        Assertions.assertEquals("react/contract/edit", link.legacyEditPath)
    }

    /**
     * The memos: list and form are next's, the React pages are gone, so there is no way back, but a bookmarked
     * React link is still redirected onto next.
     */
    @Test
    fun `the memo pages are next's and offer no way back`() {
        Assertions.assertEquals("next/memo", NextMigration.listUrl("memo"))
        Assertions.assertEquals("next/memo/:id", NextMigration.standardEditPage("memo"))
        Assertions.assertEquals("next/memo/new", NextMigration.newEntryUrl("memo"))
        Assertions.assertNull(NextMigration.legacyListUrl("memo"))
        Assertions.assertNull(NextMigration.legacyEditPage("memo"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("memo"))
        val link = NextMigration.orphanedLinks().single { it.category == "memo" }
        Assertions.assertEquals("react/memo", link.legacyListPath)
        Assertions.assertEquals("react/memo/edit", link.legacyEditPath)
    }

    /**
     * The skill matrix: list and form are next's, the React pages are gone, so there is no way back, but a
     * bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the skill matrix pages are next's and offer no way back`() {
        Assertions.assertEquals("next/skillentry", NextMigration.listUrl("skillentry"))
        Assertions.assertEquals("next/skillentry/:id", NextMigration.standardEditPage("skillentry"))
        Assertions.assertEquals("next/skillentry/new", NextMigration.newEntryUrl("skillentry"))
        Assertions.assertNull(NextMigration.legacyListUrl("skillentry"))
        Assertions.assertNull(NextMigration.legacyEditPage("skillentry"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("skillentry"))
        val link = NextMigration.orphanedLinks().single { it.category == "skillentry" }
        Assertions.assertEquals("react/skillentry", link.legacyListPath)
        Assertions.assertEquals("react/skillentry/edit", link.legacyEditPath)
    }

    /**
     * The bank accounts and their transactions (banking plugin): list and form are next's, the React pages are gone,
     * so there is no way back and no redirect of old links.
     */
    @Test
    fun `the banking pages are next's and offer no way back`() {
        listOf("bankAccount", "bankAccountRecord").forEach { category ->
            Assertions.assertEquals("next/$category", NextMigration.listUrl(category))
            Assertions.assertEquals("next/$category/:id", NextMigration.standardEditPage(category))
            Assertions.assertEquals("next/$category/new", NextMigration.newEntryUrl(category))
            Assertions.assertNull(NextMigration.legacyListUrl(category))
            Assertions.assertNull(NextMigration.legacyEditPage(category))
            Assertions.assertNull(NextMigration.legacyNewEntryUrl(category))
            Assertions.assertTrue(NextMigration.orphanedLinks().none { it.category == category })
        }
    }

    /**
     * The polls: list, form and answer page are next's, the React pages are gone, so there is no way back, but a
     * bookmarked React link is still redirected onto next (the answer page by OrphanedLinkFilter).
     */
    @Test
    fun `the poll pages are next's and offer no way back`() {
        Assertions.assertEquals("next/poll", NextMigration.listUrl("poll"))
        Assertions.assertEquals("next/poll/:id", NextMigration.standardEditPage("poll"))
        Assertions.assertEquals("next/poll/new", NextMigration.newEntryUrl("poll"))
        Assertions.assertNull(NextMigration.legacyListUrl("poll"))
        Assertions.assertNull(NextMigration.legacyEditPage("poll"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("poll"))
        val link = NextMigration.orphanedLinks().single { it.category == "poll" }
        Assertions.assertEquals("react/poll", link.legacyListPath)
        Assertions.assertEquals("react/poll/edit", link.legacyEditPath)
    }

    /**
     * The inbox and the outbox: list and form are next's, the React pages are gone, so there is no way back, but a
     * bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the inbox and outbox pages are next's and offer no way back`() {
        listOf("incomingMail", "outgoingMail").forEach { category ->
            Assertions.assertEquals("next/$category", NextMigration.listUrl(category))
            Assertions.assertEquals("next/$category/:id", NextMigration.standardEditPage(category))
            Assertions.assertEquals("next/$category/new", NextMigration.newEntryUrl(category))
            Assertions.assertNull(NextMigration.legacyListUrl(category))
            Assertions.assertNull(NextMigration.legacyEditPage(category))
            Assertions.assertNull(NextMigration.legacyNewEntryUrl(category))
            Assertions.assertTrue(NextMigration.orphanedLinks().none { it.category == category })
        }
    }

    /**
     * The visitor book: list and form (the visits included) are next's, the React pages are gone, so there is no
     * way back, but a bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the visitorbook pages are next's and offer no way back`() {
        Assertions.assertEquals("next/visitorbook", NextMigration.listUrl("visitorbook"))
        Assertions.assertEquals("next/visitorbook/:id", NextMigration.standardEditPage("visitorbook"))
        Assertions.assertEquals("next/visitorbook/new", NextMigration.newEntryUrl("visitorbook"))
        Assertions.assertNull(NextMigration.legacyListUrl("visitorbook"))
        Assertions.assertNull(NextMigration.legacyEditPage("visitorbook"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("visitorbook"))
        val link = NextMigration.orphanedLinks().single { it.category == "visitorbook" }
        Assertions.assertEquals("react/visitorbook", link.legacyListPath)
        Assertions.assertEquals("react/visitorbook/edit", link.legacyEditPath)
    }

    /**
     * The employees: list and form (time-dependent attributes included) are next's, the React pages are gone, so
     * there is no way back, but a bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the employee pages are next's and offer no way back`() {
        Assertions.assertEquals("next/employee", NextMigration.listUrl("employee"))
        Assertions.assertEquals("next/employee/:id", NextMigration.standardEditPage("employee"))
        Assertions.assertEquals("next/employee/new", NextMigration.newEntryUrl("employee"))
        Assertions.assertNull(NextMigration.legacyListUrl("employee"))
        Assertions.assertNull(NextMigration.legacyEditPage("employee"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("employee"))
        val link = NextMigration.orphanedLinks().single { it.category == "employee" }
        Assertions.assertEquals("react/employee", link.legacyListPath)
        Assertions.assertEquals("react/employee/edit", link.legacyEditPath)
    }

    /**
     * The currency pairs: list and form (conversion rates included) are next's, the React pages are gone, so there
     * is no way back, but a bookmarked React link is still redirected onto next.
     */
    @Test
    fun `the currency pair pages are next's and offer no way back`() {
        Assertions.assertEquals("next/currencyPair", NextMigration.listUrl("currencyPair"))
        Assertions.assertEquals("next/currencyPair/:id", NextMigration.standardEditPage("currencyPair"))
        Assertions.assertEquals("next/currencyPair/new", NextMigration.newEntryUrl("currencyPair"))
        Assertions.assertNull(NextMigration.legacyListUrl("currencyPair"))
        Assertions.assertNull(NextMigration.legacyEditPage("currencyPair"))
        Assertions.assertNull(NextMigration.legacyNewEntryUrl("currencyPair"))
        val link = NextMigration.orphanedLinks().single { it.category == "currencyPair" }
        Assertions.assertEquals("react/currencyPair", link.legacyListPath)
        Assertions.assertEquals("react/currencyPair/edit", link.legacyEditPath)
    }

    /**
     * The HR planning list serves its metadata under the category of its rows, `hrPlanningEntry`: a category
     * unknown to [NextMigration] would count as a React page and offer a dead link to `react/hrPlanningEntry`.
     */
    @Test
    fun `the HR planning offers no way back`() {
        for (category in listOf("hrPlanning", "hrPlanningEntry")) {
            Assertions.assertNull(NextMigration.legacyListUrl(category), category)
            Assertions.assertNull(NextMigration.legacyEditPage(category), category)
            Assertions.assertNull(NextMigration.legacyNewEntryUrl(category), category)
        }
        Assertions.assertEquals("next/hrPlanning", NextMigration.listUrl("hrPlanningEntry"))
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
     * The escape hatch shares its url with the orphaned link it must be let past, so it carries the
     * marker OrphanedLinkFilter looks for - appended as the first or a further query parameter as the
     * url already has one.
     */
    @Test
    fun `the escape hatch marker is appended as a query parameter`() {
        Assertions.assertEquals(
            "wa/orderBookList?${NextMigration.ESCAPE_HATCH_PARAM}",
            NextMigration.withEscapeHatchMarker("wa/orderBookList"),
        )
        Assertions.assertEquals(
            "wa/orderBookEdit?id=:id&${NextMigration.ESCAPE_HATCH_PARAM}",
            NextMigration.withEscapeHatchMarker("wa/orderBookEdit?id=:id"),
        )
        Assertions.assertNull(NextMigration.withEscapeHatchMarker(null))
    }

    /**
     * OrphanedLinkFilter redirects each migrated page in its own legacy app: a page migrated from Wicket
     * orphans its `wa/...` links, one from React its `react/...` links, and a page whose legacy form is
     * gone (`book`) has no legacy link to orphan.
     */
    @Test
    fun `orphaned links map each migrated legacy page to its next url`() {
        val byList = NextMigration.orphanedLinks().associateBy { it.legacyListPath }

        val order = byList["wa/orderBookList"]
        Assertions.assertNotNull(order, "The order page was migrated from Wicket and must be redirected.")
        Assertions.assertEquals(NextMigration.LegacyApp.WICKET, order!!.legacyApp)
        Assertions.assertEquals("wa/orderBookEdit", order.legacyEditPath) // Id is the `id` query parameter.
        Assertions.assertEquals("/next/order", order.nextListUrl)
        Assertions.assertEquals("/next/order/:id", order.nextEditUrl)
        Assertions.assertEquals("/next/order/new", order.nextNewEntryUrl)

        val group = byList["react/group"]
        Assertions.assertNotNull(group, "The group page was migrated from the React app and must be redirected.")
        Assertions.assertEquals(NextMigration.LegacyApp.REACT, group!!.legacyApp)
        Assertions.assertEquals("react/group/edit", group.legacyEditPath) // Id is a path segment.
        Assertions.assertEquals("/next/group", group.nextListUrl)

        // The invoices offer no way back any more (offerLegacyLink = false), but their legacyApp stays, so a
        // bookmarked Wicket link is still bent onto next.
        val incomingInvoice = byList["wa/incomingInvoiceList"]
        Assertions.assertNotNull(incomingInvoice, "A bookmarked incoming invoice link must still be redirected.")
        Assertions.assertEquals("/next/creditor-invoice", incomingInvoice!!.nextListUrl)
        Assertions.assertEquals("wa/incomingInvoiceEdit", incomingInvoice.legacyEditPath)

        // The book's legacy form was removed (legacyApp == null), so there is no legacy link to bend.
        Assertions.assertTrue(
            NextMigration.orphanedLinks().none { it.nextListUrl == "/next/book" },
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
