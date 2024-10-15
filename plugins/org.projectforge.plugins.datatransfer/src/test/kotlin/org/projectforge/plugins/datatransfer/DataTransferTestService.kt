/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.mockito.Mockito
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileObject
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.rest.AttachmentsServicesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.zip.ZipInputStream
import jakarta.annotation.PostConstruct
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession


@Service
class DataTransferTestService {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var attachmentsServicesRest: AttachmentsServicesRest

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var pluginAdminService: PluginAdminService

  init {
    addPluginEntitiesForTestMode()
  }

  @PostConstruct
  private fun postConstruct() {
    pluginAdminService.initializeAllPluginsForUnitTest()
  }

  internal fun createPersonalBox(user: PFUserDO): DataTransferAreaDO {
    return dataTransferAreaDao.ensurePersonalBox(user.id!!)!!
  }

  internal fun createArea(
    areaName: String,
    accessUsers: List<PFUserDO>? = null,
    accessGroups: List<GroupDO>? = null,
    externalDownloadEnabled: Boolean? = null,
    externalUploadEnabled: Boolean? = null,
  ): DataTransferAreaDO {
    val dbo = DataTransferAreaDO()
    dbo.areaName = areaName
    dbo.adminIds = "${ThreadLocalUserContext.loggedInUserId}"
    dbo.accessUserIds = PFUserDO.toLongList(accessUsers)
    dbo.accessGroupIds = GroupDO.toLongList(accessGroups)
    dbo.externalDownloadEnabled = externalDownloadEnabled
    dbo.externalUploadEnabled = externalUploadEnabled
    dataTransferAreaDao.save(dbo, checkAccess = false)
    return dbo
  }

  internal fun createFile(
    area: DataTransferAreaDO,
    filename: String? = null,
    ageInDays: Int = 0,
    fileSuffix: String? = null
  ): FileObject? {
    val path = "/ProjectForge/${dataTransferAreaPagesRest.jcrPath}"
    val file = FileObject()
    file.fileName = filename ?: "pom-$ageInDays${fileSuffix ?: ""}.xml"
    file.description = "This is the maven pom file."
    file.parentNodePath = path
    file.relPath = "${area.id}/attachments"
    file.created = Date(System.currentTimeMillis() - ageInDays * DataTransferJCRCleanUpJob.MILLIS_PER_DAY)
    file.createdByUser = "fin"
    file.lastUpdate = file.created
    file.lastUpdateByUser = "kai"
    try {
      val attachment = attachmentsService.addAttachment(
        dataTransferAreaPagesRest.jcrPath!!,
        fileInfo = file,
        content = File("pom.xml").readBytes(),
        baseDao = dataTransferAreaDao,
        obj = area,
        accessChecker = DataTransferAccessChecker(dataTransferAreaDao)
      )
      file.fileId = attachment.fileId
    } catch (ex: Exception) {
      return null
    }
    return file
  }

  internal fun downloadFile(
    dataTransferArea: DataTransferAreaDO,
    fileObject: FileObject
  ): ByteArray? {
    val response = try {
      attachmentsServicesRest.download(
        DataTransferPlugin.ID,
        dataTransferArea.id!!,
        fileObject.fileId!!,
        null
      )
    } catch (ex: Exception) {
      // Not found or no access
      return null
    }
    response.body.inputStream.use {
      return IOUtils.toByteArray(it)
    }
  }

  companion object {
    internal fun addPluginEntitiesForTestMode() {
      MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(
        DataTransferAreaDO::class.java.canonicalName,
        DataTransferAuditDO::class.java.canonicalName,
      )
    }

    internal fun checkZipArchive(byteArray: ByteArray): List<String> {
      val result = mutableListOf<String>()
      ZipInputStream(ByteArrayInputStream(byteArray)).use {
        var zipEntry = it.nextEntry
        while (zipEntry != null) {
          if (zipEntry.isDirectory) {
            zipEntry = it.nextEntry
            continue
          }
          result.add(FilenameUtils.getName(zipEntry.name))
          zipEntry = it.nextEntry
        }
      }
      return result
    }

    internal fun mockHttpServletRequest(): HttpServletRequest {
      val request = Mockito.mock(HttpServletRequest::class.java)
      val mockSession = MockSession()
      Mockito.`when`(request.getSession(Mockito.anyBoolean())).thenReturn(mockSession.session)
      Mockito.`when`(request.session).thenReturn(mockSession.session)
      return request
    }
  }

  internal class MyServletOutputStream : ServletOutputStream() {
    private val baos = ByteArrayOutputStream()
    val byteArray: ByteArray
      get() = baos.toByteArray()

    override fun write(b: Int) {
      baos.write(b)
    }

    override fun isReady(): Boolean {
      return true
    }

    override fun setWriteListener(p0: WriteListener?) {
    }
  }

  private class MockSession {
    val session = Mockito.mock(HttpSession::class.java)
    private val attributes = mutableMapOf<String, Any>()

    init {
      Mockito.`when`(session.getAttribute(Mockito.anyString())).thenAnswer { invocationOnMock ->
        val attribute = invocationOnMock.getArgument<String>(0)
        return@thenAnswer attributes[attribute]
      }
      Mockito.`when`(session.setAttribute(Mockito.anyString(), Mockito.any())).thenAnswer { invocationOnMock ->
        val attribute = invocationOnMock.getArgument<String>(0)
        val value = invocationOnMock.getArgument<Any>(1)
        attributes.put(attribute, value)
      }
    }

    fun clearSession() {
      attributes.clear()
    }
  }
}
