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

import mu.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList

private val log = KotlinLogging.logger {}

/**
 * This class is useful, if the stored object of derived classes has to be cached. After reaching expireTime during a
 * request, the method refresh will be called.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractCache {
    private var cacheListeners: CopyOnWriteArrayList<CacheListener>? = null

    protected var expireTime: Long = 60 * TICKS_PER_MINUTE

    @Transient
    private var timeOfLastRefresh: Long = -1

    @Transient
    private var isExpired = true

    /**
     * @return true if currently a cache refresh is running, otherwise false.
     */
    @Transient
    var isRefreshInProgress: Boolean = false
        private set

    /**
     * @return true if the cache is initialized, otherwise false (no refresh has been made yet).
     */
    val initialized: Boolean
        get() = timeOfLastRefresh != -1L

    protected constructor()

    /**
     * @param expireTime in milliseconds.
     */
    protected constructor(expireTime: Long) {
        this.expireTime = expireTime
    }

    open fun setExpireTimeInMinutes(expireTime: Long) {
        this.expireTime = expireTime * TICKS_PER_MINUTE
    }

    fun setExpireTimeInSeconds(expireTime: Long) {
        this.expireTime = expireTime * TICKS_PER_SECOND
    }

    fun setExpireTimeInHours(expireTime: Long) {
        this.expireTime = expireTime * TICKS_PER_HOUR
    }

    /**
     * Cache will be refreshed before next use.
     */
    open fun setExpired() {
        this.isExpired = true
    }

    /**
     * Sets the cache to expired and calls checkRefresh, which forces refresh.
     */
    fun forceReload() {
        setExpired()
        checkRefresh()
    }

    fun register(listener: CacheListener) {
        cacheListeners = cacheListeners ?: CopyOnWriteArrayList()
        cacheListeners!!.add(listener)
    }

    fun unregister(listener: CacheListener) {
        cacheListeners?.remove(listener)
    }

    /**
     * Checks the expired time and calls refresh, if cache is expired.
     */
    protected fun checkRefresh() {
        cacheListeners?.forEach { listener -> listener.onBeforeCacheRefresh() }
        synchronized(this) {
            if (isRefreshInProgress) {
                // Do nothing because refreshing is already in progress.
                return
            }
            if (this.isExpired || System.currentTimeMillis() - this.timeOfLastRefresh > this.expireTime) {
                try {
                    isRefreshInProgress = true
                    this.timeOfLastRefresh = System.currentTimeMillis()
                    try {
                        this.refresh()
                    } catch (ex: Throwable) {
                        log.error(ex.message, ex)
                    }
                    this.isExpired = false
                } finally {
                    isRefreshInProgress = false
                }
            }
        }
        cacheListeners?.let {
            synchronized(it) {
                it.forEach { listener -> listener.onAfterCacheRefresh() }
            }
        }
    }

    /**
     * Please implement this method refreshing the stored object _data. Do not forget to call checkRefresh in your cache
     * methods.
     *
     * @see .checkRefresh
     */
    protected abstract fun refresh()

    companion object {
        /**
         * Milliseconds.
         */
        const val TICKS_PER_SECOND: Long = 1000

        /**
         * Milliseconds.
         */
        const val TICKS_PER_MINUTE: Long = TICKS_PER_SECOND * 60

        /**
         * Milliseconds.
         */
        const val TICKS_PER_HOUR: Long = TICKS_PER_MINUTE * 60

        /**
         * Milliseconds.
         */
        const val TICKS_PER_DAY: Long = 24 * TICKS_PER_HOUR
    }
}
