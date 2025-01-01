/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.common.extensions.formatMillis
import org.projectforge.common.logging.LogDuration

/**
 * Mustn't be thread-safe, because it is used in a thread-local context.
 * If you want to use it in a multithreaded context, you have to synchronize it.
 * Usage:
 *   val saved = persistenceService.saveStatsState()
 *   // Do something...
 *   log.info("Processing done. stats=${persistenceService.formatStats(saved)}")
 *
 */
class PersistenceConnectionStats {
    var createdTransactions = 0
        private set
    var createdReadonlies = 0
        private set

    /**
     * Active readonlies are all readonlies that are currently open.
     */
    var activeReadonlies = 0
        private set

    /**
     * Active transactions are all transactions that are currently open.
     */
    var activeTransactions = 0
        private set

    /**
     * Used for performance measurement.
     */
    var logDuration: LogDuration? = null
        private set

    /**
     * Only the readonlies that were created since the last save.
     * Only set by [getActivities].
     */
    var activeReadonliesSinceLastSave = 0
        private set

    /**
     * Only the transactions that were created since the last save.
     * Only set by [getActivities].
     */
    var activeTransactionsSinceLastSave = 0
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

    fun getCopyOfCurrentState(): PersistenceConnectionStats {
        val copy = PersistenceConnectionStats()
        copy.createdTransactions = createdTransactions
        copy.createdReadonlies = createdReadonlies
        copy.activeReadonlies = activeReadonlies
        copy.activeTransactions = activeTransactions
        copy.logDuration = LogDuration()
        return copy
    }

    internal fun getActivities(oldState: PersistenceConnectionStats): PersistenceConnectionStats {
        val copy = PersistenceConnectionStats()
        copy.createdTransactions = createdTransactions - oldState.createdTransactions
        copy.createdReadonlies = createdReadonlies - oldState.createdReadonlies
        copy.activeReadonliesSinceLastSave = activeReadonlies - oldState.activeReadonlies
        copy.activeTransactionsSinceLastSave = activeTransactions - oldState.activeTransactions
        copy.activeTransactions = activeTransactions
        copy.activeReadonlies = activeReadonlies
        copy.logDuration = oldState.logDuration
        return copy
    }

    fun reset() {
        createdTransactions = 0
        createdReadonlies = 0
        activeReadonlies = 0
        activeTransactions = 0
    }

    /**
     * Returns a string representation of this object.
     * @param withDuration If true, the duration since the last save is included. Duration is formatted as "[HH:]mm:ss.SSS".
     * @return The string representation.
     */
    fun asString(withDuration: Boolean = true): String {
        val sb = StringBuilder()
        asString(sb, withDuration)
        return sb.toString()
    }

    private fun asString(sb: StringBuilder, withDuration: Boolean) {
        sb.append("[transactions=[created=").append(createdTransactions)
            .append(",active=").append(activeTransactions)
        if (activeTransactionsSinceLastSave != activeTransactions) {
            sb.append(",sinceLastSave=").append(activeTransactionsSinceLastSave)
        }
        sb.append("],readonlies=[created=").append(createdReadonlies)
            .append(",active=").append(activeReadonlies)
        if (activeReadonliesSinceLastSave != activeReadonlies) {
            sb.append(",sinceLastSave=").append(activeReadonliesSinceLastSave)
        }
        sb.append("]")
        if (withDuration) {
            logDuration?.let {
                sb.append(",duration=").append(it)
            }
        }
        sb.append("]")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        asString(sb, true)
        return sb.toString()
    }

    companion object {
        internal fun create(
            createdReadonlies: Int = 0,
            createdTransactions: Int = 0,
            activeReadonlies: Int = 0,
            activeTransactions: Int = 0,
            activeReadonliesSinceLastSave: Int = 0,
            activeTransactionsSinceLastSave: Int = 0,
        ): PersistenceConnectionStats {
            return PersistenceConnectionStats().also {
                it.logDuration = LogDuration()
                it.createdReadonlies = createdReadonlies
                it.createdTransactions = createdTransactions
                it.activeReadonlies = activeReadonlies
                it.activeTransactions = activeTransactions
                it.activeReadonliesSinceLastSave = activeReadonliesSinceLastSave
                it.activeTransactionsSinceLastSave = activeTransactionsSinceLastSave
            }
        }
    }
}
