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


package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.springframework.beans.factory.annotation.Autowired

/**
 * The number of a project is fixed once it has cost 2 units, which carry a copy of it
 * ([ProjektDao.onUpdate]).
 */
class ProjektDaoNumberLockTest : AbstractTestBase() {
    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Test
    fun `number is changeable until the project has cost 2 units`() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val id = projektDao.insert(ProjektDO().also {
                it.name = "ProjektDaoNumberLockTest"
                it.internKost2_4 = 987
                it.nummer = 41
            })
            // No cost 2 unit yet: a typo in the number may still be fixed.
            Assertions.assertFalse(projektDao.isNumberLocked(id))
            projektDao.update(projektDao.find(id)!!.also { it.nummer = 42 })

            val kost2 = Kost2DO().also { it.kost2Art = Kost2ArtDO().withId(1L) }
            kost2Dao.setProjekt(kost2, id)
            kost2Dao.insert(kost2)
            Assertions.assertTrue(projektDao.isNumberLocked(id))

            val ex = assertThrows<UserException> {
                projektDao.update(projektDao.find(id)!!.also { it.nummer = 43 })
            }
            Assertions.assertEquals(ProjektDao.NUMBER_LOCKED_I18N_KEY, ex.i18nKey)
            assertThrows<UserException> {
                projektDao.update(projektDao.find(id)!!.also { it.internKost2_4 = 986 })
            }
            // Everything else stays editable.
            projektDao.update(projektDao.find(id)!!.also { it.description = "still editable" })
            Assertions.assertEquals(42, projektDao.find(id)!!.nummer)
            null
        }
    }
}
