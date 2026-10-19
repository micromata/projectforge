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


package org.projectforge.feature

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.menu.builder.MenuItemDefId

class FeatureTogglesTest {
    @Test
    fun `all is enabled by default`() {
        val toggles = FeatureToggles(FeatureToggles.parse("all"), emptySet())
        Assertions.assertFalse(toggles.isRestricted)
        Assertions.assertTrue(toggles.isEnabled(listOf("ADDRESS_LIST", "COMMON")))
    }

    @Test
    fun `allowlist enables the listed ids and their children`() {
        val toggles = FeatureToggles(FeatureToggles.parse("datatransfer, COMMON"), emptySet())
        Assertions.assertTrue(toggles.isRestricted)
        Assertions.assertTrue(toggles.isEnabled(listOf("datatransfer", "MISC")))
        Assertions.assertTrue(toggles.isEnabled(listOf("ADDRESS_LIST", "COMMON")), "Child of an enabled group.")
        Assertions.assertFalse(toggles.isEnabled(listOf("POLL", "MISC")))
        Assertions.assertFalse(toggles.isEnabled(listOf("MISC")))
    }

    @Test
    fun `denylist wins`() {
        val toggles = FeatureToggles(FeatureToggles.parse("datatransfer"), FeatureToggles.parse("DataTransferPersonalBox;MISC")!!)
        Assertions.assertFalse(toggles.isEnabled(listOf("DataTransferPersonalBox", "datatransfer")))
        Assertions.assertFalse(toggles.isEnabled(listOf("datatransfer", "MISC")), "Group switched off.")
        val all = FeatureToggles(null, setOf("TIMESHEET_LIST"))
        Assertions.assertFalse(all.isEnabled(listOf("TIMESHEET_LIST", "PROJECT_MANAGEMENT")))
        Assertions.assertTrue(all.isEnabled(listOf("TASK_TREE", "PROJECT_MANAGEMENT")))
    }

    @Test
    fun `parse ignores blanks`() {
        Assertions.assertEquals(setOf("a", "b"), FeatureToggles.parse(" a,, b "))
        Assertions.assertEquals(emptySet<String>(), FeatureToggles.parse(""))
        Assertions.assertNull(FeatureToggles.parse("a,ALL"))
    }
}

class FeatureDerivationTest {
    @Test
    fun `next url of a migrated page`() {
        val definition = FeatureDerivation.derive("ORDER", "next/invoice")
        // The route differs from the category (outgoingInvoice).
        Assertions.assertTrue("rs/outgoingInvoice" in definition.paths, "${definition.paths}")
        Assertions.assertTrue("rs/outgoingInvoiceSelected" in definition.paths)
        Assertions.assertTrue("next/invoice" in definition.paths)
        Assertions.assertTrue("next/invoice/new" in definition.paths)
    }

    @Test
    fun `next url of a page of its own`() {
        val definition = FeatureDerivation.derive("SEARCH", "next/search")
        Assertions.assertEquals(setOf("rs/search", "rs/searchSelected", "next/search"), definition.paths)
        // Two segments: no category of its own.
        Assertions.assertEquals(setOf("next/finance/statistics"), FeatureDerivation.derive("X", "next/finance/statistics").paths)
    }

    @Test
    fun `react url and extras`() {
        val definition = FeatureDerivation.derive(
            "MY_2FA",
            "react/2FA/dynamic",
            restPaths = listOf("task/tree", "rsPublic/foo"),
            sharedRestPaths = listOf("task/tree"),
        )
        Assertions.assertTrue("rs/2FA" in definition.paths)
        Assertions.assertTrue("next/2FA" in definition.paths, "The generic page of next.")
        Assertions.assertTrue("rs/task/tree" in definition.paths)
        Assertions.assertFalse("rs/task/treeSelected" in definition.paths)
        Assertions.assertTrue("rsPublic/foo" in definition.paths)
        Assertions.assertEquals(setOf("rs/task/tree"), definition.sharedPaths)
    }

    @Test
    fun `no paths without url`() {
        Assertions.assertTrue(FeatureDerivation.derive("LOGOUT", "logout").paths.isEmpty())
        Assertions.assertTrue(FeatureDerivation.derive("COMMON", null).paths.isEmpty())
    }

    @Test
    fun `core features`() {
        val ids = CoreFeatures.definitions.map { it.id }
        Assertions.assertFalse(MenuItemDefId.LOGOUT.id in ids, "Always on.")
        val taskTree = CoreFeatures.definitions.first { it.id == MenuItemDefId.TASK_TREE.id }
        Assertions.assertTrue("rs/task" in taskTree.paths)
        Assertions.assertTrue("rs/task/favorites" in taskTree.sharedPaths)
    }
}
