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
import org.projectforge.business.fibu.KundeStatus
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.fibu.CustomerPagesRest
import org.projectforge.rest.fibu.ProjectEntityRest
import org.projectforge.ui.AutoCompletion
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
     * A group named `acme-pop`: the index holds `acme` and `pop` (the default analyzer separates at the hyphen),
     * so the query `+acme-pop*` of the combo box asked for a term that cannot exist - Lucene doesn't tokenize a
     * wildcard term. Only `acme pop` used to find the group.
     */
    @Test
    fun groupNameWithHyphenTest() {
        logon(ADMIN_USER)
        val group = GroupDO()
        group.name = "acme-pop.$PREFIX"
        groupDao.insert(group)
        listOf("acme-pop", "acme-po", "acme-", "acme", "pop", "acme pop").forEach { searchString ->
            val result = autoComplete(groupEntityRest, searchString)
            Assertions.assertTrue(
                result.any { it.id == group.id },
                "Group '${group.name}' not offered for '$searchString': $result",
            )
        }
        Assertions.assertTrue(
            autoComplete(groupEntityRest, "axme-pop").none { it.id == group.id },
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

    /**
     * Accents and German umlaut spellings are ignored, in both directions: "Andre" finds "André", "Mueller"
     * finds "Müller" and vice versa (default analyzer of the user names, see MyAnalysisConfigurer).
     */
    @Test
    fun userNameWithAccentsTest() {
        logon(ADMIN_USER)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "accents-$PREFIX"
            it.firstname = "André"
            it.lastname = "Müller"
        })
        listOf("Andre", "andré", "ANDRE", "Andre Mueller", "Muller", "Müller", "Mueller", "Mue", "Mül").forEach {
            Assertions.assertTrue(
                autoComplete(userEntityRest, it).any { hit -> hit.id == user.id },
                "User 'André Müller' not offered for '$it'.",
            )
        }
        val anotherUser = initTestDB.addUser(PFUserDO().also {
            it.username = "accents2-$PREFIX"
            it.firstname = "Andrea"
            it.lastname = "Mueller"
        })
        Assertions.assertTrue(autoComplete(userEntityRest, "Müller").any { it.id == anotherUser.id })
    }

    /** Customer names (customAnalyzer) and project names (wordPartsAnalyzer) ignore accents as well. */
    @Test
    fun customerAndProjectWithAccentsTest() {
        logon(TEST_FINANCE_USER)
        val customer = KundeDO().also {
            it.name = "Société Générale $PREFIX"
            it.id = 4714L
        }
        kundeDao.insert(customer)
        listOf("societe", "Société", "generale", "Gene").forEach {
            Assertions.assertTrue(
                autoComplete(customerPagesRest, it).any { hit -> hit.id == customer.id },
                "Customer '${customer.name}' not offered for '$it'.",
            )
        }
        val projekt = ProjektDO().also {
            it.name = "Größenmessung_$PREFIX"
            it.internKost2_4 = 994
            it.nummer = 98
        }
        projektDao.insert(projekt)
        listOf("groessenmessung", "grossen", "Größen", "Größenmessung_").forEach {
            Assertions.assertTrue(
                autoComplete(projectEntityRest, it).any { hit -> hit.id == projekt.id },
                "Project '${projekt.name}' not offered for '$it'.",
            )
        }
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
     * A project is found by its number as the label shows it (`7.002.00`, or a prefix of it) and by its
     * customer; projects named after the word come before those found only by their customer.
     */
    @Test
    fun projectByNumberAndCustomerTest() {
        logon(TEST_FINANCE_USER)
        val word = "numcust${PREFIX!!.lowercase()}"
        val customer = KundeDO().also {
            it.name = "$word gmbh"
            it.id = 4713L
        }
        kundeDao.insert(customer)
        val byCustomer = ProjektDO().also {
            it.name = "Allgemeine Softwareunterstuetzung"
            it.kunde = customer
            it.nummer = 3
        }
        projektDao.insert(byCustomer)
        val byName = ProjektDO().also {
            it.name = "$word portal"
            it.internKost2_4 = 994
            it.nummer = 94
        }
        projektDao.insert(byName)
        val kost = projektDao.find(byCustomer.id)!!.kost
        Assertions.assertFalse(kost.contains('?'), "Project without number: $kost")
        listOf(kost, kost.dropLast(1), kost.substringBeforeLast('.'), word.take(6)).forEach { searchString ->
            Assertions.assertTrue(
                autoComplete(projectEntityRest, searchString).any { it.id == byCustomer.id },
                "Project '$kost' not offered for '$searchString'.",
            )
        }
        val ids = autoComplete(projectEntityRest, word).map { it.id }
        Assertions.assertEquals(listOf(byName.id, byCustomer.id), ids)
        // Saved in the next app, the customer is an id-only stub (Project.copyTo): still found by it.
        projektDao.update(projektDao.find(byCustomer.id)!!.also {
            it.kunde = KundeDO().also { k -> k.nummer = customer.nummer }
            it.description = "edited"
        })
        Assertions.assertTrue(
            autoComplete(projectEntityRest, word.take(6)).any { it.id == byCustomer.id },
            "Project not offered by its customer after an update with a customer stub.",
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

    /**
     * A deactivated user is offered only on request ([AutoCompletion.SHOW_INACTIVE_PARAM]), and only to the
     * members of [AutoCompletion.SHOW_INACTIVE_PERSONS_GROUPS] - never to anybody else.
     */
    @Test
    fun deactivatedUserTest() {
        logon(ADMIN_USER)
        val word = "deactquux${PREFIX!!.lowercase()}"
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "deact-$PREFIX"
            it.lastname = word
            it.deactivated = true
        })
        // Controlling may select deactivated users (UserDao.hasUserSelectAccess), but isn't privileged here:
        logon(TEST_CONTROLLING_USER)
        Assertions.assertTrue(autoComplete(userEntityRest, word).none { it.id == user.id })
        Assertions.assertTrue(
            autoComplete(userEntityRest, word, showInactive = true).none { it.id == user.id },
            "Not for a user outside the privileged groups, whatever the request asks for.",
        )
        logon(TEST_FINANCE_USER)
        Assertions.assertTrue(autoComplete(userEntityRest, word).none { it.id == user.id }, "Hidden by default.")
        val hit = autoComplete(userEntityRest, word, showInactive = true).single { it.id == user.id }
        Assertions.assertEquals(true, hit.deactivated)
    }

    /** An ended project is offered only on request, and marked as inactive then. */
    @Test
    fun endedProjectTest() {
        logon(TEST_FINANCE_USER)
        val word = "endedprj${PREFIX!!.lowercase()}"
        val ended = ProjektDO().also {
            it.name = "$word ended"
            it.internKost2_4 = 996
            it.nummer = 96
            it.status = ProjektStatus.ENDED
        }
        projektDao.insert(ended)
        val running = ProjektDO().also {
            it.name = "$word running"
            it.internKost2_4 = 995
            it.nummer = 95
            it.status = ProjektStatus.ACTIVE
        }
        projektDao.insert(running)
        // Only the own projects: the name parts of other tests' projects (the PREFIX) match the word as well.
        val own = setOf(running.id, ended.id)
        Assertions.assertEquals(
            listOf(running.id),
            autoComplete(projectEntityRest, word).map { it.id }.filter { it in own },
        )
        val all = autoComplete(projectEntityRest, word, showInactive = true).filter { it.id in own }
        Assertions.assertEquals(own, all.map { it.id }.toSet())
        Assertions.assertEquals(true, all.single { it.id == ended.id }.deactivated)
        Assertions.assertNull(all.single { it.id == running.id }.deactivated)
    }

    /** A customer no longer in business is offered only on request. */
    @Test
    fun endedCustomerTest() {
        logon(TEST_FINANCE_USER)
        val word = "endedcust${PREFIX!!.lowercase()}"
        val customer = KundeDO().also {
            it.name = "$word gmbh"
            it.id = 4712L
            it.status = KundeStatus.ENDED
        }
        kundeDao.insert(customer)
        Assertions.assertTrue(autoComplete(customerPagesRest, word).none { it.id == customer.id })
        Assertions.assertEquals(
            true,
            autoComplete(customerPagesRest, word, showInactive = true).single { it.id == customer.id }.deactivated,
        )
    }

    private fun autoComplete(
        rest: AbstractEntityRest<*, *, *>,
        searchString: String,
        showInactive: Boolean = false,
    ): List<AbstractEntityRest.DisplayObject> {
        return rest.getAutoCompleteObjects(
            MockHttpServletRequest().also {
                it.setSession(MockHttpSession())
                if (showInactive) it.setParameter(AutoCompletion.SHOW_INACTIVE_PARAM, "true")
            },
            searchString,
            30,
        )
    }

    companion object {
        private val PREFIX = AutoCompleteObjectsTest::class.simpleName
    }
}
