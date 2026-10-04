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
import org.junit.jupiter.api.Test

class SystemStatusTest {
    @Test
    fun `only a plain css colour is taken as the test-system colour`() {
        Assertions.assertEquals("#ff6868", SystemStatus.sanitizeColor(null))
        Assertions.assertEquals("#0f0", SystemStatus.sanitizeColor("#0f0"))
        Assertions.assertEquals("#00ff0080", SystemStatus.sanitizeColor(" #00ff0080 "))
        Assertions.assertEquals("orange", SystemStatus.sanitizeColor("orange"))
        Assertions.assertEquals("#ff6868", SystemStatus.sanitizeColor(""))
        Assertions.assertEquals("#ff6868", SystemStatus.sanitizeColor("red;background:url(x)"))
        Assertions.assertEquals("#ff6868", SystemStatus.sanitizeColor("#ggg"))
    }
}
