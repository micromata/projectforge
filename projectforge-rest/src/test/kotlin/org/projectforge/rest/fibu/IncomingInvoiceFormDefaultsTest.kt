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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/**
 * The one read the hand built incoming-invoice form does for its own defaults:
 * [IncomingInvoiceEntityRest.getFormDefaults].
 *
 * It writes nothing, so what has to be tested is that it checks the read access itself — a `@GetMapping` on an
 * entity rest class gets none of `AbstractPagesRest`'s checks for free. Unlike the outgoing invoice there is
 * no `activeKost2`/`kost2Check` pair: the incoming invoice has no project or customer, so there is nothing a
 * cost unit could be compared against.
 */
class IncomingInvoiceFormDefaultsTest : AbstractTestBase() {

    @Autowired
    private lateinit var incomingInvoiceEntityRest: IncomingInvoiceEntityRest

    @Test
    fun `the form defaults are readable with the finance right`() {
        logon(TEST_FINANCE_USER)
        // The only value is `fibu.defaultVAT`, which a test database need not carry - the assertion is that
        // the call goes through, not what number comes back. It may well be null, in which case the form's
        // VAT field simply starts empty.
        incomingInvoiceEntityRest.getFormDefaults()
    }

    @Test
    fun `a user without the finance right may not read them`() {
        logon(TEST_USER)
        // The whole reason for the explicit check in the endpoint: it is a plain GET, so nothing else
        // would ask.
        org.junit.jupiter.api.assertThrows<AccessException> { incomingInvoiceEntityRest.getFormDefaults() }
    }
}
