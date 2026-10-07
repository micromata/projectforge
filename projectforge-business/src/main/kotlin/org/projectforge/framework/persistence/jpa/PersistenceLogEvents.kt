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

package org.projectforge.framework.persistence.jpa

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

object PersistenceLogEvents {
    @JvmField
    val TRANSACTION_ROLLBACK = LogEvent(
        code = "persistence.transactionRollback",
        category = LogCategory.BUG,
        explanation = "A transaction was rolled back because of an unexpected exception; nothing of it was saved. " +
                "The exception is rethrown, so a request shows the user an error.",
        action = "Analyze the stack trace. Constraint violations or lock timeouts may also point to inconsistent data or a database problem.",
    )

    @JvmField
    val FULLTEXT_QUERY_INVALID = LogEvent(
        code = "persistence.fulltextQueryInvalid",
        category = LogCategory.CLIENT,
        explanation = "A search term the full text query parser can't parse (e.g. a trailing '/' or a dangling AND): " +
                "the user gets no result for it.",
    )
}
