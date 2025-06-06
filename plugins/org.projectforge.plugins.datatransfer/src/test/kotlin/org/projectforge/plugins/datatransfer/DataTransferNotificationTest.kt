/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.user.entities.PFUserDO

class DataTransferNotificationTest {
  @Test
  fun registerRecipientsTest() {
    val observerIds = longArrayOf(1, 2, 3)
    testAudit(
      uploadUserId = 1,
      byUserId = 1,
      AttachmentsEventType.UPLOAD,
      observerIds,
      expected = setOf(2, 3),
      "only 2 and 3 should be notified. Action done by 1.",
    )
    testAudit(
      uploadUserId = 1,
      byUserId = 4,
      AttachmentsEventType.DELETE,
      observerIds,
      expected = setOf(1, 2, 3),
      "1, 2 and 3 should be notified. Action done by 4.",
    )
    testAudit(
      uploadUserId = 4,
      byUserId = 4,
      AttachmentsEventType.DELETE,
      observerIds,
      expected = setOf(1, 2, 3),
      "1, 2 and 3 should be notified. Action done by 4.",
    )
    testAudit(
      uploadUserId = 4,
      byUserId = 4,
      AttachmentsEventType.UPLOAD,
      observerIds,
      expected = setOf(1, 2, 3),
      "1, 2 and 3 should be notified. Action done by 4.",
    )
    testAudit(
      uploadUserId = 4,
      byUserId = 2,
      AttachmentsEventType.MODIFICATION,
      observerIds,
      expected = setOf(1, 3, 4),
      "1, 3 and 4 should be notified. Action done by 2, file uploaded by 4.",
    )
  }

  private fun testAudit(
    uploadUserId: Long,
    byUserId: Long,
    eventType: AttachmentsEventType,
    observerIds: LongArray,
    expected: Set<Long>,
    msg: String,
  ) {
    val audit = DataTransferAuditDO()
    audit.eventType = eventType
    PFUserDO().let {
      it.id = byUserId
      audit.byUser = it
    }
    PFUserDO().let {
      it.id = uploadUserId
      audit.uploadByUser = it
    }
    val recipients = mutableSetOf<Long>()
    DataTransferNotificationMailService.registerRecipients(audit, observerIds, recipients)
    assertSet(expected, recipients, msg)
  }


  private fun assertSet(expected: Set<Long>, actual: Set<Long>, msg: String) {
    val message = "$msg expected=${expected.joinToString()}, actual=${actual.joinToString()}"
    Assertions.assertEquals(expected.size, actual.size, message)
    expected.forEach {
      Assertions.assertTrue(actual.contains(it), message)
    }
  }
}
