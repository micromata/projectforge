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

package org.projectforge.business.sipgate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class SipgateDeleteContactServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressDao: AddressDao

    /**
     * A force deleted address keeps its sync entry as tombstone, otherwise the next sync would import the remote
     * contact again as a new address.
     */
    @Test
    fun forceDeleteKeepsTombstone() {
        logon(ADMIN)
        val address = AddressDO()
        address.name = "Sipgate tombstone"
        addressDao.insert(address, checkAccess = false)
        val contactId = "sipgate-tombstone-test"
        persistenceService.runInTransaction { context ->
            context.em.persist(SipgateContactSyncDO.create(null, address).also { it.sipgateContactId = contactId })
        }
        addressDao.forceDelete(address, checkAccess = false)
        Assertions.assertNull(addressDao.find(address.id, checkAccess = false))
        val syncDO = persistenceService.runReadOnly { context ->
            context.em.find(SipgateContactSyncDO::class.java, contactId)
        }
        Assertions.assertNotNull(syncDO, "sync entry kept")
        Assertions.assertNull(syncDO!!.addressId)
        Assertions.assertEquals(SipgateContactSyncDO.RemoteStatus.DELETED_BY_LOCAL, syncDO.remoteStatus)
    }
}
