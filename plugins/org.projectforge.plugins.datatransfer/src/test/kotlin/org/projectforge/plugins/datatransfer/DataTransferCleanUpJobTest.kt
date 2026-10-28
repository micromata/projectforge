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

package org.projectforge.plugins.datatransfer

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.files.FileObject
import org.projectforge.files.store.FileStore
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaEntityRest
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferCleanUpJob
import org.springframework.beans.factory.annotation.Autowired

const val MODUL_NAME = "org.projectforge.plugins.datatransfer"

class DataTransferCleanUpJobTest : AbstractTestBase() {
  @Autowired
  private lateinit var fileStore: FileStore

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaEntityRest: DataTransferAreaEntityRest

  @Autowired
  private lateinit var dataTransferCleanUpJob: DataTransferCleanUpJob

  @Autowired
  private lateinit var dataTransferTestService: DataTransferTestService

  init {
    DataTransferTestService.addPluginEntitiesForTestMode()
  }

  @PostConstruct
  private fun postConstruct() {
    initTestFileStore()
  }

  @Test
  fun cleanUpTest() {
    logon(TEST_USER)
    createArea("emptyTestArea")
    val area = createArea("testArea")
    val deletedArea = createArea("deletedArea")
    val file31 = createFile(area, 31)
    val file1 = createFile(area, 1)
    val file31OfDeletedArea = createFile(deletedArea, 31, "deleted")
    val file1OfDeletedArea = createFile(deletedArea, 1, "deleted")
    dataTransferAreaDao.delete(deletedArea, checkAccess = false)
    Assertions.assertTrue(fileStore.retrieveFile(file31))
    Assertions.assertTrue(fileStore.retrieveFile(file1))
    Assertions.assertEquals(3, dataTransferCleanUpJob.execute(), "Number of files, deleted by cleanup job")
    Assertions.assertFalse(fileStore.retrieveFile(file31), "File of area is expired and should be deleted.")
    Assertions.assertTrue(fileStore.retrieveFile(file1), "File of area isn't expired and should still exist.")
    Assertions.assertFalse(
      fileStore.retrieveFile(file31OfDeletedArea),
      "File of deleted (non existing) area should be deleted."
    )
    Assertions.assertFalse(
      fileStore.retrieveFile(file1OfDeletedArea),
      "File of deleted (non existing) area should be deleted."
    )
  }

  private fun createArea(areaName: String): DataTransferAreaDO {
    return dataTransferTestService.createArea(areaName)
  }

  private fun createFile(area: DataTransferAreaDO, ageInDays: Int, fileSuffix: String? = null): FileObject {
    return dataTransferTestService.createFile(area, ageInDays = ageInDays, fileSuffix = fileSuffix)!!
  }
}
