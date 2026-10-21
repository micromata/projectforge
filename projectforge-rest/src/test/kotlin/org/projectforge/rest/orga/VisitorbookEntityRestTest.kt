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

package org.projectforge.rest.orga

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.orga.VisitorType
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookDao
import org.projectforge.business.orga.VisitorbookService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Visitorbook
import org.projectforge.rest.dto.VisitorbookEntry
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import java.time.LocalDate

/**
 * The visitor book of the next pages ([VisitorbookEntityRest]): access, and the visits written through
 * `visitorbook/entry/...` - the visitor a visit belongs to and the checks of a visit.
 */
class VisitorbookEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var visitorbookDao: VisitorbookDao

    @Autowired
    private lateinit var visitorbookEntityRest: VisitorbookEntityRest

    @Autowired
    private lateinit var visitorbookService: VisitorbookService

    /** The visitors and their visits are for the users with the visitor book right only. */
    @Test
    fun `visitors and visits are for users with the visitor book right only`() {
        val orgaUser = createOrgaUser()
        logon(orgaUser)
        val visitorbookId = createVisitor("Access")
        val entry = entry(visitorbookId, LocalDate.of(2026, 10, 1))
        logon(TEST_USER)
        assertThrows<AccessException> { visitorbookEntityRest.getList(MockHttpServletRequest(), MagicFilter()) }
        assertThrows<AccessException> { visitorbookEntityRest.getNewEntry(MockHttpServletRequest()) }
        assertThrows<AccessException> { visitorbookEntityRest.insertEntry(post(entry)) }

        logon(orgaUser)
        val stored = entries(visitorbookEntityRest.insertEntry(post(entry))).single()
        assertEquals("09:00", stored.arrived)
        val detail = visitorbookEntityRest.getItem(visitorbookId).body as Visitorbook
        assertEquals(listOf(stored.id), detail.entries.map { it.id })
        assertEquals(1, detail.numberOfVisits)
        assertEquals(true, detail.writeAccess)

        stored.comment = "changed"
        logon(TEST_USER)
        assertThrows<AccessException> { visitorbookEntityRest.updateEntry(post(stored)) }
        assertThrows<AccessException> { visitorbookEntityRest.deleteEntry(post(stored)) }
        logon(orgaUser)
        assertEquals("changed", entries(visitorbookEntityRest.updateEntry(post(stored))).single().comment)
    }

    /** The visitor of a visit comes from the database: a posted visitor id doesn't move the visit. */
    @Test
    fun `an update keeps the stored visitor of the visit`() {
        logon(createOrgaUser())
        val visitorbookId = createVisitor("Owner")
        val otherVisitorbookId = createVisitor("Other")
        val stored = entries(
            visitorbookEntityRest.insertEntry(post(entry(visitorbookId, LocalDate.of(2026, 10, 1))))
        ).single()
        stored.visitorbookId = otherVisitorbookId
        stored.comment = "moved?"
        val entries = entries(visitorbookEntityRest.updateEntry(post(stored)))
        assertEquals(visitorbookId, entries.single().visitorbookId)
        assertEquals("moved?", entries.single().comment)
        assertTrue(visitorbookService.selectAllVisitorbookEntries(otherVisitorbookId, deleted = false).isEmpty())
    }

    /** Deleting the last visit leaves a visitor without visits (the info of the list emptied, no error). */
    @Test
    fun `the last visit can be deleted`() {
        logon(createOrgaUser())
        val visitorbookId = createVisitor("Last")
        val stored = entries(
            visitorbookEntityRest.insertEntry(post(entry(visitorbookId, LocalDate.of(2026, 10, 1))))
        ).single()
        assertEquals(LocalDate.of(2026, 10, 1), visitorbookService.getVisitorbookInfo(visitorbookId)?.lastDateOfVisit)
        assertTrue(entries(visitorbookEntityRest.deleteEntry(post(stored))).isEmpty())
        val info = visitorbookService.getVisitorbookInfo(visitorbookId)
        assertNull(info?.lastDateOfVisit)
        assertEquals(0, info?.numberOfVisits ?: 0)
    }

    /** A visit needs its date. */
    @Test
    fun `a visit without date is refused at its field`() {
        logon(createOrgaUser())
        val visitorbookId = createVisitor("Date")
        assertEquals(
            listOf("dateOfVisit"),
            validationFields(visitorbookEntityRest.insertEntry(post(entry(visitorbookId, null)))),
        )
    }

    /** A new visitor offers the stored ones with the typed name, so the visit goes to the existing one. */
    @Test
    fun `existing visitors are found by the start of their names`() {
        val orgaUser = createOrgaUser()
        logon(orgaUser)
        val id = createVisitor("Existingperson", firstname = "Maxi", company = "Existing Company GmbH")
        val deletedId = createVisitor("Existingperson", firstname = "Mara")
        visitorbookDao.markAsDeleted(visitorbookDao.find(deletedId)!!)
        visitorbookEntityRest.insertEntry(post(entry(id, LocalDate.of(2026, 10, 1))))

        val found = visitorbookEntityRest.getExisting(firstname = "ma", lastname = "existingp")
        assertEquals(listOf(id), found.map { it.id }, "Prefix of both names, case-insensitive, no deleted visitor.")
        assertEquals("Existing Company GmbH", found.first().company)
        assertEquals(LocalDate.of(2026, 10, 1), found.first().lastDateOfVisit)
        assertEquals(1, found.first().numberOfVisits)
        assertEquals(listOf(id), visitorbookEntityRest.getExisting(firstname = null, lastname = "Existingp").map { it.id })
        assertTrue(visitorbookEntityRest.getExisting(firstname = "Maxi", lastname = "Other").isEmpty())
        assertTrue(visitorbookEntityRest.getExisting(firstname = "M", lastname = "").isEmpty(), "Too short.")

        logon(TEST_USER)
        assertThrows<AccessException> { visitorbookEntityRest.getExisting(firstname = null, lastname = "Existingp") }
    }

    /** The company names in use are completed; no other property is. */
    @Test
    fun `company names are completed`() {
        logon(createOrgaUser())
        createVisitor("Companyperson", company = "Completed Company AG")
        assertEquals(
            listOf("Completed Company AG"),
            visitorbookEntityRest.getAutoCompletionForProperty("company", "completed comp"),
        )
        assertTrue(visitorbookEntityRest.getAutoCompletionForProperty("lastname", "Companyp").isEmpty())
    }

    /** The pinned filter shows the visitors with a visit lacking its arrival or departure time. */
    @Test
    fun `the incomplete visits filter shows visitors with a visit lacking a time`() {
        logon(createOrgaUser())
        val completeId = createVisitor("Complete")
        val openId = createVisitor("Departureopen")
        visitorbookEntityRest.insertEntry(post(entry(completeId, LocalDate.of(2026, 10, 1), departed = "17:00")))
        visitorbookEntityRest.insertEntry(post(entry(openId, LocalDate.of(2026, 10, 1), departed = "17:00")))
        // An older visit counts as well, not only the latest one.
        visitorbookEntityRest.insertEntry(post(entry(openId, LocalDate.of(2026, 9, 1))))

        val incomplete = listedIds(incompleteVisits = true)
        assertTrue(incomplete.contains(openId))
        assertFalse(incomplete.contains(completeId))
        assertTrue(listedIds(incompleteVisits = false).containsAll(listOf(openId, completeId)), "Unticked: all.")
    }

    /** The ids of the visitors listed with the incomplete visits filter (ticked or not). */
    private fun listedIds(incompleteVisits: Boolean): List<Long?> {
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry(VisitorbookEntityRest.INCOMPLETE_VISITS_FILTER).also {
            it.value.value = incompleteVisits.toString()
        })
        return visitorbookEntityRest.getList(MockHttpServletRequest(), filter, doNotStore = true)
            .resultSet.map { (it as Visitorbook).id }
    }

    /** A member of the organization team with the visitor book right, as the right is granted in production. */
    private fun createOrgaUser(): PFUserDO {
        val username = "${javaClass.simpleName}.orgaUser"
        logon(TEST_ADMIN_USER)
        userService.getInternalByUsername(username)?.let { return it }
        val user = PFUserDO().also {
            it.firstname = "Orga"
            it.lastname = "User"
            it.username = username
            it.addRight(UserRightDO(UserRightId.ORGA_VISITORBOOK, UserRightValue.READWRITE))
        }
        initTestDB.addUser(user)
        val group = getGroup(ProjectForgeGroup.ORGA_TEAM.toString())
        group.assignedUsers!!.add(user)
        groupDao.update(group)
        return user
    }

    private fun createVisitor(lastname: String, firstname: String = "Visitor", company: String? = null): Long {
        return visitorbookDao.insert(VisitorbookDO().also {
            it.firstname = firstname
            it.lastname = lastname
            it.company = company
            it.visitortype = VisitorType.NORMAL
        })
    }

    private fun post(entry: VisitorbookEntry) = PostData(entry, watchFieldsTriggered = null, serverData = null)

    private fun entry(visitorbookId: Long, dateOfVisit: LocalDate?, departed: String? = null) =
        VisitorbookEntry(dateOfVisit = dateOfVisit, arrived = "09:00", departed = departed, visitorbookId = visitorbookId)

    private fun entries(response: ResponseEntity<*>): List<VisitorbookEntry> {
        assertEquals(HttpStatus.OK, response.statusCode)
        return (response.body as VisitorbookEntityRest.Entries).entries
    }

    private fun validationFields(response: ResponseEntity<*>): List<String> {
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        return (response.body as ResponseAction).validationErrors!!.mapNotNull { it.fieldId }.sorted()
    }
}
