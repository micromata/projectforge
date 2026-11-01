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


package org.projectforge.business.fibu.kost

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import kotlin.concurrent.thread

/**
 * The Kost2 of a project are found by the project reference of the cached Kost2 ([KostCache.getKost2ForProjekt]).
 * That reference must neither be wiped by a cache miss nor decide by an outdated copy of the project, and a Kost2
 * changed while the cache is refreshed must not get lost: the Kost2 would be missing in the task tree, the project
 * list or the cost unit selection of a time sheet until the next refresh.
 */
class KostCacheStaleProjectTest : AbstractTestBase() {
    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var projektKost2Service: ProjektKost2Service

    @Test
    fun `kost2 of a reactivated project are active again without a refresh of the KostCache`() {
        logon(TEST_FINANCE_USER)
        val projektId = projektDao.insert(ProjektDO().also {
            it.name = "KostCacheStaleProjectTest reactivated"
            it.internKost2_4 = 976
            it.nummer = 14
        })
        projektKost2Service.activate(projektId, listOf(1L))
        Assertions.assertEquals(1, kostCache.getActiveKost2ForProjekt(projektId).size)

        setStatus(projektId, ProjektStatus.ENDED)
        // The time sheet list, the calendar etc. bind the then current (ended) project to the cached Kost2:
        kostCache.getKost2ForProjekt(projektId).forEach { PfCaches.instance.getKost2(it.id) }
        Assertions.assertEquals(0, kostCache.getActiveKost2ForProjekt(projektId).size)

        setStatus(projektId, ProjektStatus.ACTIVE)
        Assertions.assertEquals(
            1, kostCache.getActiveKost2ForProjekt(projektId).size,
            "The Kost2 of the reactivated project must not count as ended by the outdated project copy.",
        )
    }

    @Test
    fun `a project missed by the ProjektCache doesn't wipe the project of the cached kost2`() {
        val projekt = ProjektDO().also { it.id = 999_999_997L }
        val kost2 = Kost2DO().also {
            it.id = 999_999_998L
            it.projekt = projekt
        }
        try {
            kostCache.updateKost2(kost2)
            PfCaches.instance.initialize(kostCache.getKost2(kost2.id)!!)
            Assertions.assertEquals(
                listOf(kost2.id), kostCache.getKost2ForProjekt(projekt.id).map { it.id },
                "The Kost2 must still be found by its project.",
            )
        } finally {
            kostCache.forceReload()
        }
    }

    @Test
    fun `kost2 changed during a refresh is in the refreshed cache`() {
        val kost2 = Kost2DO().also {
            it.id = 999_999_996L
            it.projekt = ProjektDO().also { p -> p.id = 999_999_995L }
        }
        kostCache.getKost2(1L) // Initializes the cache, so the refresh below is the only one.
        var error: Throwable? = null
        kostCache.beforePublishHook = {
            kostCache.beforePublishHook = null
            // As a concurrent request, after the refresh has read the database:
            thread {
                try {
                    kostCache.updateKost2(kost2)
                } catch (ex: Throwable) {
                    error = ex
                }
            }.join()
        }
        try {
            kostCache.forceReload()
            error?.let { throw it }
            Assertions.assertSame(kost2, kostCache.getKost2(kost2.id), "Kost2 changed during the refresh must not get lost.")
        } finally {
            kostCache.beforePublishHook = null
            kostCache.forceReload()
        }
    }

    private fun setStatus(projektId: Long, status: ProjektStatus) {
        val projekt = projektDao.find(projektId)!!
        projekt.status = status
        projektDao.update(projekt)
    }
}
