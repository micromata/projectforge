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

package org.projectforge.plugins.marketing

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class AddressCampaignDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressCampaignDao: AddressCampaignDao

    @Test
    fun `a title may be taken by one campaign only, ignoring case, blanks and deleted campaigns`() {
        val campaign = insert("Duplicate Title Test")
        assertTrue(addressCampaignDao.doesTitleAlreadyExist("Duplicate Title Test", null), "new campaign, same title")
        assertTrue(addressCampaignDao.doesTitleAlreadyExist("  duplicate title TEST ", null), "case and blanks ignored")
        assertFalse(addressCampaignDao.doesTitleAlreadyExist("Duplicate Title Test", campaign.id), "the campaign itself")
        assertFalse(addressCampaignDao.doesTitleAlreadyExist("Other Title Test", null), "another title")
        assertFalse(addressCampaignDao.doesTitleAlreadyExist(" ", null), "no title")

        val other = insert("Other Title Test")
        assertTrue(addressCampaignDao.doesTitleAlreadyExist("Duplicate Title Test", other.id), "renaming onto a taken title")

        addressCampaignDao.markAsDeleted(campaign, checkAccess = false)
        assertFalse(addressCampaignDao.doesTitleAlreadyExist("Duplicate Title Test", null), "deleted campaigns don't count")
    }

    private fun insert(title: String): AddressCampaignDO {
        val campaign = AddressCampaignDO()
        campaign.title = title
        campaign.values = "Yes; No"
        addressCampaignDao.insert(campaign, checkAccess = false)
        return campaign
    }
}
