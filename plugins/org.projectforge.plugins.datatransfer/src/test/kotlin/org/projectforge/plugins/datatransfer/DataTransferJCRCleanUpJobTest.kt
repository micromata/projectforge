/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.RepoService
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.util.*
import javax.annotation.PostConstruct

const val MODUL_NAME = "org.projectforge.plugins.datatransfer"

class DataTransferJCRCleanUpJobTest : AbstractTestBase() {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var repoService: RepoService

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var dataTransferJCRCleanUpJob: DataTransferJCRCleanUpJob

  @Autowired
  private lateinit var pluginAdminService: PluginAdminService

  init {
    MyJpaWithExtLibrariesScanner.setPluginEntitiesForTestMode(DataTransferAreaDO::class.java.canonicalName)
  }

  @PostConstruct
  private fun postConstruct() {
    pluginAdminService.initializeAllPluginsForUnitTest()
    initJCRTestRepo(MODUL_NAME, "cleanUpTestRepo")
  }

  @Test
  fun cleanUpTest() {
    val user = logon(TEST_USER)
    repoService.ensureNode(null, "${dataTransferAreaPagesRest.jcrPath}")
    createArea("emptyTestArea", user)
    val area = createArea("testArea", user)
    val deletedArea = createArea("deletedArea", user)
    val file31 = createFile(area, 31)
    val file1 = createFile(area, 1)
    val file31OfDeletedArea = createFile(deletedArea, 31, "deleted")
    val file1OfDeletedArea = createFile(deletedArea, 1, "deleted")
    dataTransferAreaDao.internalDelete(deletedArea)
    Assertions.assertTrue(repoService.retrieveFile(file31))
    Assertions.assertTrue(repoService.retrieveFile(file1))
    println(ToStringUtil.toJsonString(repoService.getNodeInfo("/ProjectForge", true)))
    dataTransferJCRCleanUpJob.execute()
    println(ToStringUtil.toJsonString(repoService.getNodeInfo("/ProjectForge", true)))
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

  private fun createArea(areaName: String, admin: PFUserDO): DataTransferAreaDO {
    val dbo = DataTransferAreaDO()
    dbo.areaName = areaName
    dbo.adminIds = "${admin.id}"
    dataTransferAreaDao.internalSave(dbo)
    return dbo
  }

  private fun createFile(area: DataTransferAreaDO, ageInDays: Int, fileSuffix: String? = null): FileObject {
    val path = "/ProjectForge/${dataTransferAreaPagesRest.jcrPath}"
    val file = FileObject()
    file.fileName = "pom-$ageInDays${fileSuffix ?: ""}.xml"
    file.description = "This is the maven pom file."
    file.parentNodePath = path
    file.relPath = "${area.id}/attachments"
    file.created = Date(System.currentTimeMillis() - ageInDays * DataTransferJCRCleanUpJob.MILLIS_PER_DAY)
    file.createdByUser = "fin"
    file.lastUpdate = file.created
    file.lastUpdateByUser = "kai"
    attachmentsService.addAttachment(
      dataTransferAreaPagesRest.jcrPath!!,
      fileInfo = file,
      content = File("pom.xml").readBytes(),
      baseDao = dataTransferAreaDao,
      obj = area,
      accessChecker = DataTransferAccessChecker(100000L, "none", dataTransferAreaDao)
    )
    return file
  }
}
