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

package org.projectforge.framework.json

import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.PFUserDO

class JsonUtilsTest : AbstractTestBase() {
    @Test
    fun test() {
        val userList = persistenceService.runIsolatedReadOnly { context ->
            val em = context.em
            em.createQuery("SELECT u FROM PFUserDO u", PFUserDO::class.java).resultList
        }
        JsonUtils.toJson(userList) // Shouldn't throw exception as it did before fixing the IdsOnlySerializer.
    }
}
