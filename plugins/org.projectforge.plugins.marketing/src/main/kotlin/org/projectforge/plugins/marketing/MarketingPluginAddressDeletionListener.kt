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

package org.projectforge.plugins.marketing

import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDeletionListener
import org.projectforge.framework.persistence.jpa.PfEmgr

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MarketingPluginAddressDeletionListener(val addressCampaignDao: AddressCampaignDao) : AddressDeletionListener {
  override fun onDelete(address: AddressDO) {
    addressCampaignDao.emgrFactory.runInTrans { emgr: PfEmgr ->
      val counter = emgr.entityManager
        .createNamedQuery(AddressCampaignValueDO.DELETE_BY_ADDRESS)
        .setParameter("addressId", address.getId())
        .executeUpdate()
      if (counter > 0) {
        log.info("Removed #$counter address campaign value entries of deleted address: $address")
      }
      true
    }
  }
}
