/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.RepoService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

const val MODUL_NAME = "org.projectforge.plugins.datatransfer"

class DataTransferJCRCleanUpJobTest : AbstractTestBase() {
  @Autowired
  private lateinit var repoService: RepoService

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var dataTransferJCRCleanUpJob: DataTransferJCRCleanUpJob

  @Autowired
  private lateinit var dataTransferTestService: DataTransferTestService

  init {
    DataTransferTestService.addPluginEntitiesForTestMode()
  }

  @Test
  fun cleanUpTest() {
    logon(TEST_USER)
    repoService.ensureNode(null, "${dataTransferAreaPagesRest.jcrPath}")
    createArea("emptyTestArea")
    val area = createArea("testArea")
    val deletedArea = createArea("deletedArea")
    val file31 = createFile(area, 31)
    val file1 = createFile(area, 1)
    val file31OfDeletedArea = createFile(deletedArea, 31, "deleted")
    val file1OfDeletedArea = createFile(deletedArea, 1, "deleted")
    dataTransferAreaDao.internalDelete(deletedArea)
    Assertions.assertTrue(repoService.retrieveFile(file31))
    Assertions.assertTrue(repoService.retrieveFile(file1))
    Assertions.assertEquals(3, dataTransferJCRCleanUpJob.execute(), "Number of files, deleted by cleanup job")
    Assertions.assertFalse(repoService.retrieveFile(file31), "File of area is expired and should be deleted.")
    Assertions.assertTrue(repoService.retrieveFile(file1), "File of area isn't expired and should still exist.")
    Assertions.assertFalse(
      repoService.retrieveFile(file31OfDeletedArea),
      "File of deleted (non existing) area should be deleted."
    )
    Assertions.assertFalse(
      repoService.retrieveFile(file1OfDeletedArea),
      "File of deleted (non existing) area should be deleted."
    )
    repoService.shutdown()
  }

  private fun createArea(areaName: String): DataTransferAreaDO {
    return dataTransferTestService.createArea(areaName)
  }

  private fun createFile(area: DataTransferAreaDO, ageInDays: Int, fileSuffix: String? = null): FileObject {
    return dataTransferTestService.createFile(area, ageInDays = ageInDays, fileSuffix = fileSuffix)!!
  }
}
