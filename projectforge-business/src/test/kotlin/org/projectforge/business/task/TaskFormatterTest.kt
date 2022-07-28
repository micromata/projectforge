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

package org.projectforge.business.task

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TaskFormatterTest {
  @Test
  fun taskPathShortFormTest() {
    Assertions.assertEquals("M", TaskFormatter.asShortForm("Micromata"))
    Assertions.assertEquals("BUG", TaskFormatter.asShortForm("Business Unit Green"))
    Assertions.assertEquals("P", TaskFormatter.asShortForm("ProjectForge"))
    Assertions.assertEquals("R13", TaskFormatter.asShortForm("Release 1.3"))
    Assertions.assertEquals("R5", TaskFormatter.asShortForm("-/:Release .: 5"))
    var array = arrayOf<String?>("Micromata", "Business Unit Green", "ProjectForge", "Development", "Release 1.3")
    Assertions.assertEquals("M..BUG..P..D..Release 1.3", TaskFormatter.asShortForm(5, array))
    Assertions.assertEquals("M..BUG..P..D..Release 1.3", TaskFormatter.asShortForm(34, array))
    Assertions.assertEquals("M..BUG..P..Development..Release 1.3", TaskFormatter.asShortForm(35, array))

    array = arrayOf("Alpha", "Bravo", "Charlie")
    Assertions.assertEquals("A..B..Charlie", TaskFormatter.asShortForm(1, array))
    Assertions.assertEquals("A..B..Charlie", TaskFormatter.asShortForm(16, array))
    Assertions.assertEquals("A..Bravo..Charlie", TaskFormatter.asShortForm(17, array))
    Assertions.assertEquals("A..Bravo..Charlie", TaskFormatter.asShortForm(20, array))
    Assertions.assertEquals("Alpha..Bravo..Charlie", TaskFormatter.asShortForm(21, array))

    array = arrayOf("Alpha")
    Assertions.assertEquals("Alpha", TaskFormatter.asShortForm(1, array))
    Assertions.assertEquals("", TaskFormatter.asShortForm(1, null))
    Assertions.assertEquals("", TaskFormatter.asShortForm(1, arrayOf("")))
    Assertions.assertEquals("", TaskFormatter.asShortForm(1, arrayOf("")))
    array = arrayOf("Alpha", null, "Bravo")
    Assertions.assertEquals("A..?..Bravo", TaskFormatter.asShortForm(1, array))
    Assertions.assertEquals("A..?..Bravo", TaskFormatter.asShortForm(14, array))
    Assertions.assertEquals("Alpha..?..Bravo", TaskFormatter.asShortForm(15, array))
  }
}
