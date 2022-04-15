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
import org.projectforge.business.configuration.DomainService
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.plugins.datatransfer.rest.DataTransferPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class NotificationMailTest : AbstractTestBase() {
  @Autowired
  private lateinit var domainService: DomainService

  @Autowired
  private lateinit var notificationMailService: NotificationMailService

  init {
    MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(DataTransferAreaDO::class.java.canonicalName)
  }

  @Test
  fun mailTest() {
    val recipient = createUser()
    val area = createDataTransferArea(42, "Area")
    val link = createLink(area.id)
    val byUser = createUser(firstname = "Mr.", lastname = "Modifier", id = 2)
    var mail = notificationMailService.prepareMail(
      recipient,
      AttachmentsEventType.UPLOAD,
      "Mail.kt",
      area,
      link,
      recipient,
      null
    )
    Assertions.assertNull(mail, "Don't send the recipient his own notification.")
    mail =
      notificationMailService.prepareMail(recipient, AttachmentsEventType.UPLOAD, "Mail.kt", area, link, byUser, null)
    Assertions.assertNotNull(mail)
    mail = notificationMailService.prepareMail(
      recipient,
      AttachmentsEventType.UPLOAD,
      "Mail.kt",
      area,
      link,
      null,
      "External: 127.0.0.1"
    )
    Assertions.assertNotNull(mail)
  }

  @Test
  fun motificationMailTest() {
    val recipient = createUser()
    val notificationInfoList = mutableListOf<NotificationMailService.AttachmentNotificationInfo>()
    val area1 = createDataTransferArea(42, "Area", 10)
    notificationInfoList.add(createNotificationInfo("File 1.pdf", 1_200, area1, 5))
    notificationInfoList.add(createNotificationInfo("File 2.pdf", 1_200_000, area1, 1))
    val area2 = createDataTransferArea(2, "Area 2", 30)
    notificationInfoList.add(createNotificationInfo("File 1.pdf", 1_200, area2, 5))
    notificationInfoList.add(createNotificationInfo("File 2.pdf", 1_200_000, area2, 1))
    val mail = notificationMailService.prepareMail(recipient, notificationInfoList)
    println(mail?.content)
  }

  private fun createUser(
    firstname: String = "Kai",
    lastname: String = "Reinhard",
    email: String = "k.reinhard@acme.com",
    id: Int = 1
  ): PFUserDO {
    val recipient = PFUserDO()
    recipient.email = email
    recipient.firstname = firstname
    recipient.lastname = lastname
    recipient.id = id
    return recipient
  }

  private fun createDataTransferArea(id: Int, areaName: String, expireDays: Int = 30): DataTransferAreaDO {
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
  ): NotificationMailService.AttachmentNotificationInfo {
    val attachment = Attachment()
    attachment.name = name
    attachment.created = PFDateTime.now().plusDays(expiresInDays.toLong()).minusDays(area.expiryDays!!.toLong()).utilDate
    attachment.lastUpdate = attachment.created
    attachment.size = size
    val info = NotificationMailService.AttachmentNotificationInfo(
      attachment,
      area,
      PFDateTime.now().plusDays(expiresInDays.toLong()).utilDate,
      expiresInDays * DataTransferJCRCleanUpJob.MILLIS_PER_DAY,
      Locale.GERMAN,
    )
    return info
  }

  private fun createLink(areaId: Int?): String {
    return domainService.getDomain(PagesResolver.getDynamicPageUrl(DataTransferPageRest::class.java, id = areaId ?: 0))
  }
}
