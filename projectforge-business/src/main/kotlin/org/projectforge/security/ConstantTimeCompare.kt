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

package org.projectforge.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Comparison of secrets (access keys, tokens, ...) without leaking how many characters of a guess were correct:
 * `String.equals` returns on the first differing character, so the time it takes tells an attacker whether a prefix
 * was right.
 *
 * Note that the length of the given values isn't hidden (only their content), which is fine for our secrets: they
 * are of a fixed, publicly known length.
 */
object ConstantTimeCompare {
    /**
     * @return true if both values are given (not null) and equal, otherwise false. A null value never equals
     * anything, not even another null: a missing secret must not authenticate a caller who also sends none.
     */
    @JvmStatic
    fun equals(expected: String?, given: String?): Boolean {
        if (expected == null || given == null) {
            return false
        }
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            given.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
