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
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.fibu.CustomerPagesRest
import org.projectforge.rest.fibu.ProjectEntityRest
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
    private lateinit var groupEntityRest: GroupEntityRest

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var customerPagesRest: CustomerPagesRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var projectEntityRest: ProjectEntityRest

    @Autowired
    private lateinit var userEntityRest: UserEntityRest

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
            val result = autoComplete(groupEntityRest, searchString)
            Assertions.assertTrue(
                result.any { it.id == group.id },
                "Group '${group.name}' not offered for '$searchString': $result",
            )
        }
        Assertions.assertTrue(
            autoComplete(groupEntityRest, "dxl-pop").none { it.id == group.id },
            "A word the name doesn't hold offers nothing: the search is still a search.",
        )
    }

    /**
     * `PF_Controlling` is one term for the standard tokenizer (underscore is a word character), so `controlling`
     * found nothing. The `wordPartsAnalyzer` of the group name adds the word parts and keeps the original term.
     */
    @Test
    fun groupNameWithUnderscoreTest() {
        logon(ADMIN_USER)
        val group = GroupDO()
        group.name = "PF_Controlling$PREFIX"
        groupDao.insert(group)
        listOf("controlling", "pf_contr", "PF_Con", "PF_Con*", "PF_Controlling", "pf", "pf con").forEach { searchString ->
            Assertions.assertTrue(
                autoComplete(groupEntityRest, searchString).any { it.id == group.id },
                "Group '${group.name}' not offered for '$searchString'.",
            )
        }
        Assertions.assertTrue(
            autoComplete(groupEntityRest, "ontrolling").none { it.id == group.id },
            "No infix search.",
        )
    }

    /**
     * Searching `micromat` shall offer the group `micromaten` first, not the groups of the organization
     * `Micromata` - even if there are more of them than `maxResults`, which used to cut the name hit entirely.
     */
    @Test
    fun groupNameHitsFirstTest() {
        logon(ADMIN_USER)
        val word = "zorgoplex${PREFIX!!.lowercase()}"
        (1..35).forEach { i ->
            groupDao.insert(GroupDO().also {
                it.name = "aaa-$PREFIX-$i"
                it.organization = "${word}a gmbh"
            })
        }
        val named = GroupDO().also { it.name = "${word}en" }
        groupDao.insert(named)
        val inName = GroupDO().also { it.name = "aab ${word}team" }
        groupDao.insert(inName)
        val result = autoComplete(groupEntityRest, word)
        Assertions.assertEquals(30, result.size)
        Assertions.assertEquals(named.id, result[0].id, "Name starting with the term first: $result")
        Assertions.assertEquals(inName.id, result[1].id, "Then other name hits, before the organization hits: $result")
        Assertions.assertTrue(result.drop(2).all { it.displayName?.startsWith("aaa-") == true }, "$result")
    }

    /** A user found by the name comes before one found only by the email address. */
    @Test
    fun userNameHitsFirstTest() {
        logon(ADMIN_USER)
        val word = "quxbaz${PREFIX!!.lowercase()}"
        val byEmail = initTestDB.addUser(PFUserDO().also {
            it.username = "aaa-email-$PREFIX"
            it.lastname = "Aaa"
            it.email = "$word@example.org"
        })
        val byName = initTestDB.addUser(PFUserDO().also {
            it.username = "zzz-name-$PREFIX"
            it.lastname = word
        })
        val result = autoComplete(userEntityRest, word).map { it.id }
        // Other users of the shared test database may match as well: only the order of these two counts.
        Assertions.assertEquals(listOf(byName.id, byEmail.id), result.filter { it == byName.id || it == byEmail.id })
        Assertions.assertEquals(byName.id, result.first(), "$result")
    }

    /** Project names and identifiers are found by a part of an underscore name, as group names. */
    @Test
    fun projectNameWithUnderscoreTest() {
        logon(TEST_FINANCE_USER)
        val projekt = ProjektDO().also {
            it.name = "PF_Migration$PREFIX"
            it.identifier = "ACT_PFMIGR"
            it.internKost2_4 = 997
            it.nummer = 97
        }
        projektDao.insert(projekt)
        listOf("migration", "pf_mig", "PF_Migration", "pfmigr", "act_pf").forEach { searchString ->
            Assertions.assertTrue(
                autoComplete(projectEntityRest, searchString).any { it.id == projekt.id },
                "Project '${projekt.name}' not offered for '$searchString'.",
            )
        }
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
