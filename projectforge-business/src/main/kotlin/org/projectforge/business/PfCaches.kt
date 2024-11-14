/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business

import jakarta.annotation.PostConstruct
import org.projectforge.business.address.AddressbookCache
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.*
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskTree
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Helper cache for avoiding lazy loading of entities. For convenient access to most caches.
 * Ideal for usage by scripts.
 */
@Service
class PfCaches {
    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var addressbookCache: AddressbookCache

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var teamCalCache: TeamCalCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @PostConstruct
    private fun init() {
        instance = this
    }

    /**
     * Fills the user and kost1 of the given employee.
     * @param employee The employee to fill.
     * @return The filled employee for chaining.
     */
    fun populate(employee: EmployeeDO): EmployeeDO {
        employee.user = getUserIfNotInitialized(employee.user)
        employee.kost1 = getKost1IfNotInitialized(employee.kost1)
        return employee
    }

    /**
     * Fills the user, kost2, project and customer of the given kost2.
     * @param kost2 The kost2 to fill.
     * @return The filled kost2 for chaining.
     */
    fun populate(kost2: Kost2DO): Kost2DO {
        val projekt = getProjektByKost2(kost2.id)
        val kunde = getKunde(projekt?.kunde?.nummer)
        projekt?.let { it.kunde = kunde }
        kost2.let { it.projekt = projekt }
        return kost2
    }

    /**
     * Fills the task, kunde, konto, projectManager, ... of the given project.
     * @param project The project to fill.
     * @return The filled project for chaining.
     */
    fun populate(project: ProjektDO): ProjektDO {
        project.headOfBusinessManager = getUserIfNotInitialized(project.headOfBusinessManager)
        project.konto = getKontoIfNotInitialized(project.konto)
        project.kunde = getKundeIfNotInitialized(project.kunde)
        project.projectManager = getUserIfNotInitialized(project.projectManager)
        project.projektManagerGroup = getGroupIfNotInitialized(project.projektManagerGroup)
        project.salesManager = getUserIfNotInitialized(project.salesManager)
        project.task = getTaskIfNotInitialized(project.task)
        return project
    }

    /**
     * Fills the user, kost2, project and customer of the given timesheet.
     * @param timesheet The timesheet to fill.
     * @return The filled timesheet for chaining.
     */
    fun populate(timesheet: TimesheetDO): TimesheetDO {
        timesheet.user = getUser(timesheet.userId)
        timesheet.task = getTask(timesheet.taskId)
        timesheet.kost2 = getKost2(timesheet.kost2Id)?.also { populate(it) }
        return timesheet
    }

    /**
     * Fills the employee, manager, replacement and other replacements of the given vacation.
     * @param vacation The vacation to fill.
     * @return The filled kost2 for chaining.
     */
    fun populate(vacation: VacationDO): VacationDO {
        vacation.employee = getEmployeeIfNotInitialized(vacation.employee)
        vacation.manager = getEmployeeIfNotInitialized(vacation.manager)
        vacation.replacement = getEmployeeIfNotInitialized(vacation.replacement)
        vacation.otherReplacements =
            vacation.otherReplacements?.mapNotNull { getEmployeeIfNotInitialized(it) }?.toMutableSet()
        return vacation
    }

    /**
     * Gets the kost2 from the KostCache first and then fills the user, kost2, project and customer.
     * @param kost2Id The kost2Id to fill.
     */
    fun getAndPopulateKost2(kost2Id: Long?): Kost2DO? {
        return getKost2(kost2Id)?.also { populate(it) }
    }

    fun getAddressbook(addressbookId: Long?): AddressbookDO? {
        return addressbookCache.getAddressbook(addressbookId)
    }

    fun getAddressbookIfNotInitialized(addressbook: AddressbookDO?): AddressbookDO? {
        return addressbookCache.getAddressbookIfNotInitialized(addressbook)
    }

    fun getEmployee(employeeId: Long?): EmployeeDO? {
        return employeeCache.getEmployee(employeeId)
    }

    fun getEmployeeIfNotInitialized(employee: EmployeeDO?): EmployeeDO? {
        return employeeCache.getEmployeeIfNotInitialized(employee)?.also {
            it.user = getUserIfNotInitialized(it.user)
        }
    }

    fun getEmployeeByUserId(userId: Long?): EmployeeDO? {
        return employeeCache.getEmployeeByUserId(userId)
    }

    fun getGroup(groupId: Long?): GroupDO? {
        return userGroupCache.getGroup(groupId)
    }

    fun getGroupIfNotInitialized(groupDO: GroupDO?): GroupDO? {
        return userGroupCache.getGroupIfNotInitialized(groupDO)
    }

    fun getKonto(konto: Long?): KontoDO? {
        return kontoCache.getKonto(konto)
    }

    fun getKontoIfNotInitialized(konto: KontoDO?): KontoDO? {
        return kontoCache.getKontoIfNotInitialized(konto)
    }

    fun getKost1(kost1Id: Long?): Kost1DO? {
        return kostCache.getKost1(kost1Id)
    }

    fun getKost1IfNotInitialized(kost1DO: Kost1DO?): Kost1DO? {
        return kostCache.getKost1IfNotInitialized(kost1DO)
    }

    fun getKost2(kost2Id: Long?): Kost2DO? {
        return kostCache.getKost2(kost2Id)
    }

    fun getKost2IfNotInitialized(kost2DO: Kost2DO?): Kost2DO? {
        return kostCache.getKost2IfNotInitialized(kost2DO)
    }

    fun getKost2Art(kost2ArtId: Long?): Kost2ArtDO? {
        return kostCache.getKost2Art(kost2ArtId)
    }

    fun getKost2ArtIfNotInitialized(kost2ArtDO: Kost2ArtDO?): Kost2ArtDO? {
        return kostCache.getKost2ArtIfNotInitialized(kost2ArtDO)
    }

    fun getKunde(kundeId: Long?): KundeDO? {
        return kundeCache.getKunde(kundeId)
    }

    fun getKundeByKost2(kost2Id: Long?): KundeDO? {
        val projekt = getProjektByKost2(kost2Id) ?: return null
        return getKunde(projekt.kunde?.nummer)
    }

    fun getKundeIfNotInitialized(kunde: KundeDO?): KundeDO? {
        return kundeCache.getKundeIfNotInitialized(kunde)
    }

    fun getProjekt(projektId: Long?): ProjektDO? {
        return projektCache.getProjekt(projektId)
    }

    fun getProjektByKost2(kost2Id: Long?): ProjektDO? {
        val projektId = getKost2(kost2Id)?.projekt?.id ?: return null
        return getProjekt(projektId)
    }

    fun getProjektIfNotInitialized(projektDO: ProjektDO?): ProjektDO? {
        return projektCache.getProjektIfNotInitialized(projektDO)
    }

    fun getTask(taskId: Long?): TaskDO? {
        return taskTree.getTaskById(taskId)
    }

    fun getTaskIfNotInitialized(taskDO: TaskDO?): TaskDO? {
        return taskTree.getTaskIfNotInitialized(taskDO)
    }

    fun getTeamCal(calId: Long?): TeamCalDO? {
        return teamCalCache.getCalendar(calId)
    }

    fun getTeamCalIfNotInitialized(teamCal: TeamCalDO?): TeamCalDO? {
        return teamCalCache.getCalendarIfNotInitialized(teamCal)
    }

    fun getUser(userId: Long?): PFUserDO? {
        return userGroupCache.getUser(userId)
    }

    fun getUserByEmployeeId(employeeId: Long?): PFUserDO? {
        return employeeCache.getUserByEmployee(employeeId)
    }

    fun getUserIfNotInitialized(userDO: PFUserDO?): PFUserDO? {
        return userGroupCache.getUserIfNotInitialized(userDO)
    }

    companion object {
        @JvmStatic
        lateinit var instance: PfCaches
            private set

        @JvmStatic
        fun internalSetupForTestCases() {
            instance = PfCaches()
            instance.addressbookCache = AddressbookCache()
            instance.employeeCache = EmployeeCache()
            instance.kontoCache = KontoCache()
            instance.kostCache = KostCache()
            instance.kundeCache = KundeCache()
            instance.projektCache = ProjektCache()
            instance.taskTree = TaskTree()
            instance.teamCalCache = TeamCalCache()
            instance.userGroupCache = UserGroupCache()
        }
    }
}
