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
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.datatransfer.rest.DataTransferPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

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
    val recipient = PFUserDO()
    recipient.email = "k.reinhard@acme.com"
    recipient.firstname = "Kai"
    recipient.lastname = "Reinhard"
    recipient.id = 1
    val data = DataTransferAreaDO()
    data.areaName = "Area"
    data.id = 42
    val link = domainService.getDomain(PagesResolver.getDynamicPageUrl(DataTransferPageRest::class.java, id = data.id ?: 0))
    val byUser = PFUserDO()
    byUser.firstname = "Mr."
    byUser.lastname = "Modifier"
    byUser.id = 2
    var mail = notificationMailService.prepareMail(recipient, AttachmentsEventType.UPLOAD, "Mail.kt", data, link, recipient, null)
    Assertions.assertNull(mail, "Don't send the recipient his own notification.")
    mail = notificationMailService.prepareMail(recipient, AttachmentsEventType.UPLOAD, "Mail.kt", data, link, byUser, null)
    Assertions.assertNotNull(mail)
    mail = notificationMailService.prepareMail(recipient, AttachmentsEventType.UPLOAD, "Mail.kt", data, link, null, "External: 127.0.0.1")
    Assertions.assertNotNull(mail)
  }
}
