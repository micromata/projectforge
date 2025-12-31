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

import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Used by AddressDao if addresses are forced to be deleted.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class SipgateDeleteContactService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    fun deleteContact(addressId: Long) {
        persistenceService.runInTransaction { ctx ->
            ctx.executeNamedUpdate(
                SipgateContactSyncDO.DELETE_BY_ADDRESS_ID,
                Pair("addressId", addressId),
            )
        }
    }
}
