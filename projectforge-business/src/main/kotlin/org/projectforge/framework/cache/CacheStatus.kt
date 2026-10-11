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


package org.projectforge.framework.cache

/**
 * A snapshot of the state of a cache, see [AbstractCache.status]. Times as epoch millis.
 * @param name The simple class name, shown as title.
 * @param qualifiedName The class name, the id of the cache.
 * @param refreshRunningSince Start of the running refresh, if any.
 * @param stuck The running refresh has been running longer than allowed and is (or will be) interrupted by the
 * watchdog.
 * @param expired Invalidated (or never filled): the next access refreshes it.
 * @param everFilled At least one refresh completed successfully.
 * @param lastRefreshStart Start of the last refresh, successful or not.
 * @param lastRefreshError Message of the exception of the last refresh, null if it succeeded.
 */
class CacheStatus(
    val name: String,
    val qualifiedName: String,
    val refreshInProgress: Boolean,
    val refreshRunningSince: Long?,
    val stuck: Boolean,
    val expired: Boolean,
    val everFilled: Boolean,
    val expireTimeMs: Long,
    val lastRefreshStart: Long?,
    val lastRefreshEnd: Long?,
    val lastRefreshDurationMs: Long?,
    val lastSuccess: Long?,
    val lastRefreshError: String?,
    val refreshCount: Long,
    val failureCount: Long,
)
