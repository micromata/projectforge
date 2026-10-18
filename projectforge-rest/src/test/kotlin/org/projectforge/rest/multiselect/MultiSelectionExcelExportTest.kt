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

package org.projectforge.rest.multiselect

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MultiSelectionExcelExportTest {
    class Row(var value: String? = null, var comment: String? = null)

    private fun context(vararg params: String) =
        object : MassUpdateContext<Row>(params.associateWith { MassUpdateParameter(it) }.toMutableMap()) {
            override fun getId(obj: Row): Long = 1
        }.also { it.ignoreFieldsForModificationCheck = listOf("adopt") }

    @Test
    fun `a field recorded without a param of its own is a column after the params' fields`() {
        val context = context("adopt", "comment")
        val row = Row(comment = "old")
        context.startUpdate(row)
        val modified = Row(value = "yes", comment = "new")
        context.commitUpdate("row", modified) {
            context.recordCurrentModification("value", null, "yes")
        }
        assertEquals(listOf("comment", "value"), MultiSelectionExcelExport.columnFields(context))
    }

    @Test
    fun `unmodified fields are no columns`() {
        val context = context("comment")
        context.startUpdate(Row(comment = "same"))
        context.commitUpdate("row", Row(comment = "same")) {
            context.recordCurrentModification("value", "yes", "yes")
        }
        assertEquals(emptyList<String>(), MultiSelectionExcelExport.columnFields(context))
    }
}
