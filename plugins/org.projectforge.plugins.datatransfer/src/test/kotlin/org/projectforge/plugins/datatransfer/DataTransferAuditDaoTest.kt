/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.datatransfer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.time.PFDateTime
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit
import java.util.*


class DataTransferAuditDaoTest : AbstractTestBase() {
  @Autowired
  private lateinit var dataTransferAuditDao: DataTransferAuditDao

  init {
    MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(
      DataTransferAreaDO::class.java.canonicalName,
      DataTransferAuditDO::class.java.canonicalName,
    )
  }

  @Test
  fun daoTest() {
    val areaId = 1
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minusDays(10).utilDate))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(62, ChronoUnit.MINUTES).utilDate))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(5, ChronoUnit.MINUTES).utilDate))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().utilDate))
    Assertions.assertEquals(4, dataTransferAuditDao.getEntriesByAreaId(areaId)!!.size)
    dataTransferAuditDao.getQueuedEntriesByAreaId(areaId).let { entries ->
      Assertions.assertEquals(2, entries!!.size, "2 entries older than 1 hour queued.")
      dataTransferAuditDao.removeFromQueue(entries)
    }
    Assertions.assertEquals(0, dataTransferAuditDao.getQueuedEntriesByAreaId(areaId)!!.size, "No more queued entries. 2 were processed and 2 are newer")
    Assertions.assertEquals(1, dataTransferAuditDao.deleteOldEntries(PFDateTime.now().minusDays(1)))
    dataTransferAuditDao.getEntriesByAreaId(areaId).let { entries ->
      Assertions.assertEquals(3, entries!!.size, "1 entry older than 1 day should be deleted.")
    }
  }

  @Test
  fun daoDeleteOldTest() {
    val size = 60
    val areaId = 2
    for (i in 1..size) {
      dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minusDays(10).utilDate))
    }
    dataTransferAuditDao.getEntriesByAreaId(areaId).let { entries ->
      Assertions.assertEquals(size, entries!!.size)
    }
    Assertions.assertEquals(size, dataTransferAuditDao.deleteOldEntries(PFDateTime.now().minusDays(1)))
    dataTransferAuditDao.getEntriesByAreaId(areaId).let { entries ->
      Assertions.assertEquals(0, entries!!.size, "All entries of area 1 should be deleted now")
    }
  }

  private fun create(areaId: Int, timestamp: Date? = null): DataTransferAuditDO {
    val user = getUser(TEST_USER)
    val obj = DataTransferAuditDO()
    obj.byUser = user
    obj.areaId = areaId
    obj.timestamp = timestamp
    obj.eventType = AttachmentsEventType.UPLOAD
    return obj
  }
}
