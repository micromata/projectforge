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


package org.projectforge.ui.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.ui.UILabelledElement

class AddLeadingTest {
    @Test
    fun `the leading fields come first, in the order added`() {
        val elements = mutableListOf<UILabelledElement>(
            UIFilterElement("deleted", label = "Gelöscht"),
            UIFilterElement("status", label = "Status"),
            UIFilterElement("amount", label = "Betrag"),
        )
        elements.addLeading(
            UIFilterListElement("businessUnit", label = "Business Unit"),
            null, // e.g. no business unit configured
            UIFilterListElement("customer", label = "Kunde"),
            UIFilterListElement("project", label = "Projekt"),
        )
        LayoutListFilterUtils.sortElements(elements, compareBy { it as String? })
        assertEquals(
            listOf("businessUnit", "customer", "project", "amount", "deleted", "status"),
            elements.map { (it as UIFilterElement).id },
        )
    }
}
