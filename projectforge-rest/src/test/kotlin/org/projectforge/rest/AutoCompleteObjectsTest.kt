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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.fibu.CustomerPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession

/**
 * The combo boxes of the web frontend ([AbstractEntityRest.getAutoCompleteObjects]): what the user types must
 * find an entry whose name holds a character the index tokenizes at.
 */
class AutoCompleteObjectsTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var groupPagesRest: GroupPagesRest

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var customerPagesRest: CustomerPagesRest

    /**
     * A group named `dhl-pop`: the index holds `dhl` and `pop` (the default analyzer separates at the hyphen),
     * so the query `+dhl-pop*` of the combo box asked for a term that cannot exist - Lucene doesn't tokenize a
     * wildcard term. Only `dhl pop` used to find the group.
     */
    @Test
    fun groupNameWithHyphenTest() {
        logon(ADMIN_USER)
        val group = GroupDO()
        group.name = "dhl-pop.$PREFIX"
        groupDao.insert(group)
        listOf("dhl-pop", "dhl-po", "dhl-", "dhl", "pop", "dhl pop").forEach { searchString ->
            val result = autoComplete(groupPagesRest, searchString)
            Assertions.assertTrue(
                result.any { it.id == group.id },
                "Group '${group.name}' not offered for '$searchString': $result",
            )
        }
        Assertions.assertTrue(
            autoComplete(groupPagesRest, "dxl-pop").none { it.id == group.id },
            "A word the name doesn't hold offers nothing: the search is still a search.",
        )
    }

    /**
     * `KundeDO.name` is indexed by `customAnalyzer` (a whitespace tokenizer), i.e. `acme-pop` stays one term
     * (only whitespace splits) - so here it is the word as typed that matches, and the terms of the standard
     * analyzer would not. Both readings are offered, which is why this still works.
     */
    @Test
    fun customerNameWithHyphenTest() {
        logon(TEST_FINANCE_USER)
        val customer = KundeDO()
        customer.name = "acme-pop $PREFIX gmbh"
        customer.id = 4711L
        kundeDao.insert(customer)
        listOf("acme-pop", "acme-po", "acme-", "acme").forEach { searchString ->
            val result = autoComplete(customerPagesRest, searchString)
            Assertions.assertTrue(
                result.any { it.id == customer.id },
                "Customer '${customer.name}' not offered for '$searchString': $result",
            )
        }
    }

    private fun autoComplete(
        rest: AbstractEntityRest<*, *, *>,
        searchString: String,
    ): List<AbstractEntityRest.DisplayObject> {
        return rest.getAutoCompleteObjects(
            MockHttpServletRequest().also { it.setSession(MockHttpSession()) },
            searchString,
            30,
        )
    }

    companion object {
        private val PREFIX = AutoCompleteObjectsTest::class.simpleName
    }
}
