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

package org.projectforge.framework.integration

import java.net.URI

/** The state of a subsystem on the problem dashboard, ordered by severity. */
enum class SubsystemState {
    OK,

    /** No run of its syncs yet (e.g. right after the start). */
    UNKNOWN,

    /** The last run had errors, or errors occurred within the last 24 hours. */
    DEGRADED,

    /** The last run was aborted (e.g. unreachable). */
    DOWN,
}

/**
 * Which problems of the log aggregation belong to a subsystem: the code starts with a prefix, the location with a
 * class name prefix (`LdapMasterLoginHandler:42`), or the message is the failure of one of its syncs
 * (`Sync ldap-master errors in ...`, see [SyncStats]).
 * @param syncTypes Prefixes of the types of [SyncStats], e.g. `gateway-receive-` for all of them.
 */
class SubsystemProblemMatch(
    val codePrefixes: List<String> = emptyList(),
    val locationPrefixes: List<String> = emptyList(),
    val syncTypes: List<String> = emptyList(),
) {
    fun matches(code: String, location: String?, message: String?): Boolean =
        codePrefixes.any { code.startsWith(it) } ||
                (location != null && locationPrefixes.any { location.startsWith(it) }) ||
                (message != null && syncTypes.any { message.startsWith("Sync $it") })
}

/** The last run of a sync of a subsystem. Times as epoch millis. */
class SubsystemSync(
    val type: String,
    val runs: Int,
    val lastRun: Long?,
    val lastStatus: SyncStats.Status?,
    val lastDurationMs: Long?,
    val lastError: String?,
    val lastErrorDate: Long?,
)

/**
 * @param detail E.g. the server or the provider (Authentik, Keycloak).
 * @param syncs The syncs run since the start; a subsystem without syncs (e.g. mail) is OK by itself.
 */
class SubsystemStatus(
    val state: SubsystemState,
    val detail: String? = null,
    val syncs: List<SubsystemSync> = emptyList(),
) {
    companion object {
        /**
         * The worst state of the syncs run since the start. A sync never run is left out: its handler may not be the
         * active one (e.g. the LDAP slave of an LDAP master), all of them register their [SyncStats].
         */
        fun ofSyncs(syncTypes: List<String>, detail: String? = null): SubsystemStatus {
            val syncs = SyncStatsRegistry.all()
                .filter { stats -> syncTypes.any { stats.type.startsWith(it) } }
                .map { syncOf(it) }
                .filter { it.runs > 0 }
            val state = syncs.maxOfOrNull { stateOf(it.lastStatus) } ?: SubsystemState.UNKNOWN
            return SubsystemStatus(state, detail, syncs)
        }

        /** The host of a url (or the trimmed value, if it isn't one): a [detail] without credentials or path. */
        fun hostOf(url: String?): String? {
            val value = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return runCatching { URI(value).host }.getOrNull() ?: value
        }

        private fun stateOf(status: SyncStats.Status?): SubsystemState = when (status) {
            SyncStats.Status.SUCCESS -> SubsystemState.OK
            SyncStats.Status.ERRORS -> SubsystemState.DEGRADED
            SyncStats.Status.ABORTED -> SubsystemState.DOWN
            null -> SubsystemState.UNKNOWN
        }

        // Under the lock of SyncStats.record, so that the fields belong to the same run.
        private fun syncOf(stats: SyncStats): SubsystemSync = synchronized(stats) {
            SubsystemSync(
                type = stats.type,
                runs = stats.runs,
                lastRun = stats.lastRunStart?.time,
                lastStatus = stats.lastStatus,
                lastDurationMs = stats.lastDurationMs,
                lastError = stats.lastError,
                lastErrorDate = stats.lastErrorDate?.time,
            )
        }
    }
}

/**
 * A subsystem or interface shown as a tile on the problem dashboard (`LogGroupAdminService.subsystems`), if
 * active. A Spring bean in the module of the subsystem.
 */
interface SubsystemStatusProvider {
    /** Unique, e.g. `ldap`: the dashboard's filter value. */
    val id: String

    /** The i18n key of the tile's title. */
    val titleKey: String

    val problems: SubsystemProblemMatch

    /** Null if the subsystem isn't active (not configured, not the login handler etc.). */
    fun status(): SubsystemStatus?
}
