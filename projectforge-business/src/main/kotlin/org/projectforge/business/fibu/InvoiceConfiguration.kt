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

package org.projectforge.business.fibu

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * What an installation expects of an invoice beyond what the database enforces - the rules of its own
 * bookkeeping, which differ from installation to installation.
 *
 * Example configuration in application.properties:
 * ```
 * projectforge.fibu.invoices.accountRequired=true
 * ```
 *
 * @author Kai Reinhard
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.fibu.invoices")
open class InvoiceConfiguration {
    /**
     * Whether an invoice is expected to name a DATEV account ([AbstractRechnungDO.konto]).
     *
     * Not a validation: an invoice without one still saves. It is what makes it *incomplete*, which is
     * the question the invoice list's filter asks (`IncompleteInvoiceFilter`) - an installation that
     * books its invoices into DATEV finds the ones nobody has assigned an account to yet, and one that
     * doesn't is not asked about accounts at all.
     *
     * For an outgoing invoice an account inherited from the project or the customer counts as given
     * (`KontoCache.getKonto(invoice)`), as that is the account the export uses.
     */
    var accountRequired: Boolean = false
}
