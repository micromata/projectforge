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

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Warms the mutually dependent fibu caches once, in a fixed order, on a single thread at startup.
 *
 * [RechnungCache], [AuftragsCache] and [AuftragsRechnungCache] use each other (an order knows its invoiced sum,
 * an invoice knows its orders), so none of them can be filled correctly on its own: whichever refreshes first sees
 * the others empty. Left to the first requests after a start, that first fill happens in whatever order the requests
 * arrive (invoice list, order list and the menu badges all fire at once) and while they compete for the few database
 * connections of the pool - so a refresh may read a counterpart that is still filling on another thread and answer
 * with incomplete data ([org.projectforge.framework.cache.AbstractCache.performRefresh] returns the half-built map
 * after its lock timeout rather than blocking). The result then depends on that interleaving: sometimes the invoiced
 * amounts and the assigned invoices are there, sometimes not, until the caches are refreshed by hand.
 *
 * Doing the first fill here, in the same order [org.projectforge.business.system.SystemService.refreshCaches] uses
 * and on one thread, removes that race: `RechnungCache` is complete before `AuftragsCache` reads it, which is complete
 * before `AuftragsRechnungCache` links the two. The upstream caches (kost, projekt, kunde) `AuftragsCache` needs are
 * pulled in by its own refresh on this same thread. [AuftragsRechnungCache] additionally heals itself should a later
 * invalidation under load still catch `RechnungCache` incomplete (see its `invoiceResolutionIncomplete`).
 *
 * [ApplicationReadyEvent] rather than `@PostConstruct`: the database is migrated and the caches are usable by then,
 * and a failure here only costs a slower first request, never the whole start.
 */
@Service
class FibuCacheInitializer {
    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var auftragsRechnungCache: AuftragsRechnungCache

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        try {
            log.info { "Warming the fibu caches (RechnungCache, AuftragsCache, AuftragsRechnungCache) in order..." }
            rechnungCache.forceReload()
            auftragsCache.forceReload()
            auftragsRechnungCache.forceReload()
            log.info { "Warming the fibu caches done." }
        } catch (ex: Exception) {
            // Logged, not rethrown: a failed warm-up only means the first request fills the caches lazily, as before.
            log.error(ex) { "Can't warm the fibu caches on startup: ${ex.message}" }
        }
    }
}
