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

  // Needed to initialize dataTransferAuditDao.dataTransferAreaDao
  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  init {
    MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(
      DataTransferAreaDO::class.java.canonicalName,
      DataTransferAuditDO::class.java.canonicalName,
    )
  }

  @Test
  fun removeFromQueueTest() {
    val areaId = 1
    logon(TEST_USER)
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minusDays(10).utilDate))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(62, ChronoUnit.MINUTES).utilDate))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(11, ChronoUnit.MINUTES).utilDate))
    Assertions.assertEquals(3, dataTransferAuditDao.internalGetEntriesByAreaId(areaId)!!.size)
    dataTransferAuditDao.internalGetQueuedEntriesByAreaId(areaId).let { entries ->
      Assertions.assertEquals(3, entries!!.size, "3 entries queued.")
      dataTransferAuditDao.removeFromQueue(entries)
    }
    Assertions.assertEquals(0, dataTransferAuditDao.internalGetQueuedEntriesByAreaId(areaId)!!.size, "No more queued entries. 3 were processed.")
  }

  @Test
  fun deleteOldTest() {
    val size = 60
    val areaId = 2
    for (i in 1..size) {
      dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minusMonths(10).utilDate))
    }
    dataTransferAuditDao.internalGetEntriesByAreaId(areaId).let { entries ->
      Assertions.assertEquals(size, entries!!.size)
    }
    Assertions.assertEquals(size, dataTransferAuditDao.deleteOldEntries(PFDateTime.now().minusMonths(9)))
    dataTransferAuditDao.internalGetEntriesByAreaId(areaId).let { entries ->
      Assertions.assertEquals(0, entries!!.size, "All entries of area 1 should be deleted now")
    }
  }

  @Test
  fun queueTest() {
    val areaId = 3
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minusDays(10).utilDate))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(62, ChronoUnit.MINUTES).utilDate))
    Assertions.assertEquals(2, dataTransferAuditDao.internalGetQueuedEntriesByAreaId(areaId)!!.size)
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(1, ChronoUnit.MINUTES).utilDate, AttachmentsEventType.DOWNLOAD_ALL))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(2, ChronoUnit.MINUTES).utilDate, AttachmentsEventType.DOWNLOAD))
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(3, ChronoUnit.MINUTES).utilDate, AttachmentsEventType.DOWNLOAD_MULTI))
    Assertions.assertEquals(2, dataTransferAuditDao.internalGetQueuedEntriesByAreaId(areaId)!!.size, "Download events should be ignored.")
    dataTransferAuditDao.insert(create(areaId, PFDateTime.now().minus(2, ChronoUnit.MINUTES).utilDate, AttachmentsEventType.MODIFICATION))
    Assertions.assertNull(dataTransferAuditDao.internalGetQueuedEntriesByAreaId(areaId), "An audit entry newer than 5 minutes found. Queue should return nothing.")

    dataTransferAuditDao.internalGetDownloadEntriesByAreaId(areaId).let { result ->
      Assertions.assertEquals(3, result.size, "2 download events should be there.")
      Assertions.assertEquals(result[0].eventType, AttachmentsEventType.DOWNLOAD_ALL)
      Assertions.assertEquals(result[1].eventType, AttachmentsEventType.DOWNLOAD)
      Assertions.assertEquals(result[2].eventType, AttachmentsEventType.DOWNLOAD_MULTI)
    }
  }

  private fun create(areaId: Int, timestamp: Date? = null, eventType: AttachmentsEventType? = AttachmentsEventType.UPLOAD): DataTransferAuditDO {
    val user = getUser(TEST_USER)
    val obj = DataTransferAuditDO()
    obj.byUser = user
    obj.areaId = areaId
    obj.timestamp = timestamp
    obj.eventType = eventType
    return obj
  }
}
