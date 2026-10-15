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

/**
 * Database timeouts for background maintenance work: cache refreshes and scheduler runs. Without them a statement
 * blocked on a database lock (e.g. a row locked by a transaction that never commits) waits forever, and the refresh or
 * job hangs until a restart: the JDBC driver doesn't react to [Thread.interrupt] while it waits for the database.
 *
 * Inside [run] every persistence context newly created by [PfPersistenceService] sets the timeouts, limited to that
 * context (PostgreSQL: transaction-local `set_config(..., true)`; HSQLDB: JPA query timeout of the session, see
 * [queryTimeoutMillis]), so pooled connections are never affected beyond it. A statement exceeding them fails with an
 * exception, which ends the refresh or run normally (lock and running flag are released). A context reused from an
 * outer caller (e.g. the request's transaction) is left untouched, as are plain JDBC accesses not going through
 * [PfPersistenceService] (e.g. `JdbcTemplate`).
 *
 * Configured by `projectforge.maintenance.db.lockTimeoutSeconds` (default 60) and
 * `projectforge.maintenance.db.statementTimeoutSeconds` (default 0); 0 switches a timeout off. The statement timeout
 * is off by default, as some jobs legitimately run long statements (exports, backups).
 */
object MaintenanceDbTimeouts {
    const val DEFAULT_LOCK_TIMEOUT_SECONDS = 60

    @Volatile
    var lockTimeoutSeconds: Int = DEFAULT_LOCK_TIMEOUT_SECONDS
        internal set

    @Volatile
    var statementTimeoutSeconds: Int = 0
        internal set

    private val depth = ThreadLocal.withInitial { 0 }

    /** True inside [run] on the current thread. */
    val isActive: Boolean
        get() = depth.get() > 0

    /** Runs the block with the maintenance timeouts. Nestable. */
    fun <T> run(block: () -> T): T {
        depth.set(depth.get() + 1)
        try {
            return block()
        } finally {
            val remaining = depth.get() - 1
            if (remaining <= 0) depth.remove() else depth.set(remaining)
        }
    }

    /** The PostgreSQL settings to apply (name to value), empty if all timeouts are switched off. */
    internal fun settings(): List<Pair<String, String>> = buildList {
        lockTimeoutSeconds.takeIf { it > 0 }?.let { add("lock_timeout" to "${it}s") }
        statementTimeoutSeconds.takeIf { it > 0 }?.let { add("statement_timeout" to "${it}s") }
    }

    /**
     * HSQLDB: its query timeout covers the lock wait and the statement, so the smaller of the two timeouts switched
     * on, in milliseconds. Null, if both are off.
     */
    internal fun queryTimeoutMillis(): Int? =
        listOf(lockTimeoutSeconds, statementTimeoutSeconds).filter { it > 0 }.minOrNull()?.let { it * 1000 }
}
