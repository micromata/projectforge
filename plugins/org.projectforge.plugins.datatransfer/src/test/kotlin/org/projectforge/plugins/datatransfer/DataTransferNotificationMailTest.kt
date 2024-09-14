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

import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.configuration.DomainService
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.datatransfer.rest.DataTransferPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class DataTransferNotificationMailTest : AbstractTestBase() {
  @Autowired
  private lateinit var dataTransferAuditDao: DataTransferAuditDao

  @Autowired
  private lateinit var domainService: DomainService

  @Autowired
  private lateinit var dataTransferNotificationMailService: DataTransferNotificationMailService

  init {
    DataTransferTestService.addPluginEntitiesForTestMode()
  }

  @Test
  fun mailTest() {
    val recipient = getUser(TEST_USER)
    val byUser = getUser(TEST_USER2)
    val area = createDataTransferArea(42, "Area")
    area.adminIds = "${recipient.id}"
    val link = createLink(area.id)
    val timestamp = PFDateTime.now().minusDays(1)
    dataTransferAuditDao.insertAudit(
      AttachmentsEventType.UPLOAD,
      area,
      recipient,
      null,
      FileInfo("ownFile.txt"),
      timestamp,
    )
    dataTransferAuditDao.insertAudit(
      AttachmentsEventType.UPLOAD,
      area,
      null,
      "External user 123.123.123.123",
      FileInfo("externalFile.txt"),
      timestamp,
    )
    dataTransferAuditDao.insertAudit(
      AttachmentsEventType.DOWNLOAD_ALL,
      area,
      byUser,
      null,
      timestamp4TestCase = timestamp,
    )
    dataTransferAuditDao.insertAudit(
      AttachmentsEventType.DOWNLOAD_MULTI,
      area,
      byUser,
      null,
      timestamp4TestCase = timestamp,
    )
    dataTransferAuditDao.insertAudit(
      AttachmentsEventType.DOWNLOAD,
      area,
      null,
      "externalUser: 127.0.0.1",
      FileInfo("externalFile.txt"),
      timestamp4TestCase = timestamp,
    )
    dataTransferAuditDao.insertAudit(
      AttachmentsEventType.DELETE,
      area,
      byUser,
      null,
      FileInfo("externalFile.txt"),
      timestamp4TestCase = timestamp,
    )
    val mail = dataTransferNotificationMailService.prepareMail(
      recipient,
      area,
      link,
      dataTransferAuditDao.internalGetQueuedEntriesByAreaId(area.id)!!,
      dataTransferAuditDao.internalGetDownloadEntriesByAreaId(area.id),
    )
    Assertions.assertNotNull(mail)
  }

  @Test
  fun notificationMailTest() {
    val recipient = createUser()
    recipient.locale = Locale.GERMAN
    val notificationInfoList = mutableListOf<DataTransferNotificationMailService.AttachmentNotificationInfo>()
    val area1 = createDataTransferArea(42, "Area", 10)
    notificationInfoList.add(createNotificationInfo("File 1.pdf", 1_200, area1, 5))
    notificationInfoList.add(createNotificationInfo("File 2.pdf", 1_200_000, area1, 1))
    val area2 = createDataTransferArea(2, "Area 2", 30)
    notificationInfoList.add(createNotificationInfo("File 1.pdf", 1_200, area2, 5))
    notificationInfoList.add(createNotificationInfo("File 2.pdf", 1_200_000, area2, 1))
    var mail = dataTransferNotificationMailService.prepareMail(recipient, notificationInfoList)
    Assertions.assertNull(mail, "Recipient has no access, so don't send an e-mail.")
    area1.adminIds = "${recipient.id}"
    area2.accessUserIds = "${recipient.id}"
    mail = dataTransferNotificationMailService.prepareMail(recipient, notificationInfoList)
    Assertions.assertNotNull(mail)
    Assertions.assertEquals(
      4,
      StringUtils.countMatches(mail!!.content, "http://localhost:8080/react/datatransferfiles/dynamic/42")
    )
    Assertions.assertEquals(
      4,
      StringUtils.countMatches(mail.content, "http://localhost:8080/react/datatransferfiles/dynamic/2")
    )
  }

  private fun createUser(
    firstname: String = "Kai",
    lastname: String = "Reinhard",
    email: String = "k.reinhard@acme.com",
    id: Long = 1
  ): PFUserDO {
    val recipient = PFUserDO()
    recipient.email = email
    recipient.firstname = firstname
    recipient.lastname = lastname
    recipient.id = id
    return recipient
  }

  private fun createDataTransferArea(id: Long, areaName: String, expireDays: Int = 30): DataTransferAreaDO {
    val area = DataTransferAreaDO()
    area.areaName = areaName
    area.id = id
    area.expiryDays = expireDays
    return area
  }

  private fun createNotificationInfo(
    name: String,
    size: Long,
    area: DataTransferAreaDO,
    expiresInDays: Int,
  ): DataTransferNotificationMailService.AttachmentNotificationInfo {
    val attachment = Attachment()
    attachment.name = name
    attachment.created =
      PFDateTime.now().plusDays(expiresInDays.toLong()).minusDays(area.expiryDays!!.toLong()).utilDate
    attachment.lastUpdate = attachment.created
    attachment.size = size
    val info = DataTransferNotificationMailService.AttachmentNotificationInfo(
      attachment,
      area,
      PFDateTime.now().plusDays(expiresInDays.toLong()).utilDate,
      expiresInDays * DataTransferJCRCleanUpJob.MILLIS_PER_DAY,
      Locale.GERMAN,
    )
    return info
  }

  private fun createLink(areaId: Long?): String {
    return domainService.getDomain(PagesResolver.getDynamicPageUrl(DataTransferPageRest::class.java, id = areaId ?: 0))
  }
}
