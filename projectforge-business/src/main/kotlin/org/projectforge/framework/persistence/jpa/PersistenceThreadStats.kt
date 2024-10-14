/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * Mustn't be thread-safe, because it is used in a thread-local context.
 */
class PersistenceThreadStats {
    var createdTransactions = 0
        private set
    var createdReadonlies = 0
        private set

    var activeReadonlies = 0
        private set

    var activeTransactions = 0
        private set

    fun readonlyCreated() {
        createdReadonlies++
        activeReadonlies++
    }

    fun readonlyClosed() {
        activeReadonlies--
    }

    fun transactionCreated() {
        createdTransactions++
        activeTransactions++
    }

    fun transactionClosed() {
        activeTransactions--
    }

    fun saveCurrentState(): PersistenceThreadStats {
        val copy = PersistenceThreadStats()
        copy.createdTransactions = createdTransactions
        copy.createdReadonlies = createdReadonlies
        copy.activeReadonlies = activeReadonlies
        copy.activeTransactions = activeTransactions
        return copy
    }

    fun getActivities(oldState: PersistenceThreadStats): PersistenceThreadStats {
        val copy = PersistenceThreadStats()
        copy.createdTransactions = createdTransactions - oldState.createdTransactions
        copy.createdReadonlies = createdReadonlies - oldState.createdReadonlies
        copy.activeReadonlies = activeReadonlies - oldState.activeReadonlies
        copy.activeTransactions = activeTransactions - oldState.activeTransactions
        return copy
    }

    fun reset() {
        createdTransactions = 0
        createdReadonlies = 0
        activeReadonlies = 0
        activeTransactions = 0
    }

    override fun toString(): String {
        return "transactions:{created:$createdTransactions,active:$activeTransactions},readonlies:{created:$createdReadonlies,active:$activeReadonlies}"
    }
}
