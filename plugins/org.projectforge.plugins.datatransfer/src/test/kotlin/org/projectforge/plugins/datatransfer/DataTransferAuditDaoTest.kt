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
    val size = 60
    for (i in 1..size) {
      dataTransferAuditDao.insert(create(1, PFDateTime.now().minusDays(10).utilDate))
    }
    for (i in 1..size) {
      dataTransferAuditDao.insert(create(2, PFDateTime.now().utilDate))
    }
    dataTransferAuditDao.getEntriesByAreaId(1).let { entries ->
      Assertions.assertEquals(size, entries.size)
      dataTransferAuditDao.notificationsSentFor(entries)
    }
    dataTransferAuditDao.getEntriesByAreaId(1).let { entries ->
      Assertions.assertEquals(size, entries.size)
      entries.forEach {
        Assertions.assertTrue(it.notificationsSent == true)
      }
    }
    Assertions.assertEquals(size, dataTransferAuditDao.getEntriesByAreaId(2).size)
    dataTransferAuditDao.deleteOldEntries(PFDateTime.now().minusDays(1).utilDate)
    dataTransferAuditDao.getEntriesByAreaId(1).let { entries ->
      Assertions.assertEquals(0, entries.size, "All entries of area 1 should be deleted now")
    }
    dataTransferAuditDao.getEntriesByAreaId(2).let { entries ->
      Assertions.assertEquals(size, entries.size, "All entries of area 2 should exist")
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
