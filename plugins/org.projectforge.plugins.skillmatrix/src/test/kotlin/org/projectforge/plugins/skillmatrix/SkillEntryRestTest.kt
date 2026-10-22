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


package org.projectforge.plugins.skillmatrix

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.dto.SkillEntry
import org.projectforge.rest.dto.User
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest

/**
 * The skill matrix behind the next page: [SkillEntryEntityRest] (owner of new and cloned skills, filter
 * "my skills") and the mass update of [SkillEntryMassUpdateRest].
 */
class SkillEntryRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var skillEntryDao: SkillEntryDao

    @Autowired
    private lateinit var skillEntryEntityRest: SkillEntryEntityRest

    @Autowired
    private lateinit var skillEntryMassUpdateRest: SkillEntryMassUpdateRest

    init {
        MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(SkillEntryDO::class.java.canonicalName)
    }

    @Test
    fun `a new and a cloned skill belong to the logged-in user`() {
        val own = logon(TEST_USER)
        assertEquals(own.id, skillEntryEntityRest.newBaseDO(null).owner?.id)
        val colleagues = SkillEntry(id = 42, skill = "Kotlin", owner = User(getUser(TEST_USER2)), rating = 2)
        val clone = skillEntryEntityRest.prepareClone(colleagues)
        assertEquals(own.id, clone.owner?.id, "The copy of a colleague's skill is the own one.")
        assertNull(clone.id)
        assertEquals("Kotlin", clone.skill)
    }

    @Test
    fun `the filter my skills shows only the own skills, without it everybody's`() {
        val own = getUser(TEST_USER)
        val other = getUser(TEST_USER2)
        val ownSkill = insert(own, "filter-own")
        val otherSkill = insert(other, "filter-other")
        logon(own)
        val mine = list(MagicFilter().also { it.entries.add(MagicFilterEntry("owner", "true")) })
        assertTrue(mine.contains(ownSkill.id))
        assertFalse(mine.contains(otherSkill.id))
        val all = list(MagicFilter())
        assertTrue(all.containsAll(listOf(ownSkill.id, otherSkill.id)), "Everybody reads the skills of all.")
    }

    @Test
    fun `the mass update offers the levels by their texts, without a delete option`() {
        logon(TEST_USER)
        val fields = skillEntryMassUpdateRest.requestMeta(MockHttpServletRequest()).fields.associateBy { it.field }
        listOf("rating", "interest").forEach { name ->
            val field = fields[name]!!
            assertEquals(listOf("0", "1", "2", "3"), field.values?.map { it.id }, name)
            assertEquals("textValue", field.valueProperty, name)
            assertFalse(field.deleteOption, name)
        }
        assertTrue(fields["comment"]!!.appendOption)
        assertNull(fields["skill"], "One skill per owner: setting it on several entries makes doublets.")
        assertNull(fields["owner"])
    }

    @Test
    fun `the mass update sets the levels and the comment of the own skills, the others' fail per entry`() {
        val own = getUser(TEST_USER)
        val other = getUser(TEST_USER2)
        val first = insert(own, "mass-first", rating = 1, interest = 3, comment = "first")
        val second = insert(own, "mass-second", rating = 3, interest = 3)
        val colleagues = insert(other, "mass-other", rating = 1, interest = 1)
        logon(own)
        val params = mutableMapOf(
            // Level 0 ("none") is a value as any other, not an empty one.
            "rating" to MassUpdateParameter("rating").also { it.textValue = "0" },
            "interest" to MassUpdateParameter("interest").also { it.textValue = "2" },
            "comment" to MassUpdateParameter("comment").also { it.textValue = "mass"; it.append = true },
        )
        val context = object : MassUpdateContext<SkillEntryDO>(params) {
            override fun getId(obj: SkillEntryDO): Long = obj.id!!
        }
        val result = skillEntryMassUpdateRest.massUpdate(
            MockHttpServletRequest(),
            listOf(first.id!!, second.id!!, colleagues.id!!),
            context,
        )
        assertNull(result, "No validation error.")
        assertEquals(2, context.modifiedCounter)
        assertEquals(1, context.errorCounter, "Only readable, not writable.")
        assertTrue(context.errorMessages.single().identifier.startsWith("mass-other"))

        val updated = skillEntryDao.select(listOf(first.id!!, second.id!!), checkAccess = false)!!.associateBy { it.id }
        updated.values.forEach {
            assertEquals(0, it.rating, it.skill)
            assertEquals(2, it.interest, it.skill)
        }
        assertEquals("first\nmass", updated[first.id]!!.comment)
        assertEquals("mass", updated[second.id]!!.comment)
        val unchanged = skillEntryDao.find(colleagues.id, checkAccess = false)!!
        assertEquals(1, unchanged.rating)
        assertNull(unchanged.comment)
    }

    @Test
    fun `an update can't move the own skill to a colleague`() {
        val own = getUser(TEST_USER)
        val other = getUser(TEST_USER2)
        val entry = insert(own, "move")
        logon(own)
        val dto = SkillEntry(id = entry.id, skill = entry.skill, owner = User(other), rating = 3)
        assertThrows(AccessException::class.java) {
            skillEntryDao.update(skillEntryEntityRest.transformForDB(dto))
        }
        assertEquals(own.id, skillEntryDao.find(entry.id, checkAccess = false)!!.ownerId)
        // Without changing the owner, the own skill is updated:
        dto.owner = User(own)
        skillEntryDao.update(skillEntryEntityRest.transformForDB(dto))
        assertEquals(3, skillEntryDao.find(entry.id, checkAccess = false)!!.rating)
    }

    private fun list(filter: MagicFilter): List<Long?> = skillEntryEntityRest.listPage(
        MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = filter, refresh = true)
    ).resultSet.map { (it as SkillEntry).id }

    private fun insert(
        owner: PFUserDO,
        skill: String,
        rating: Int = 0,
        interest: Int = 0,
        comment: String? = null,
    ): SkillEntryDO {
        val entry = SkillEntryDO()
        entry.owner = owner
        entry.skill = "$skill-${System.nanoTime()}"
        entry.rating = rating
        entry.interest = interest
        entry.comment = comment
        skillEntryDao.insert(entry, checkAccess = false)
        return entry
    }
}
