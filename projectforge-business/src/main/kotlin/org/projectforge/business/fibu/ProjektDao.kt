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

package org.projectforge.business.fibu

import org.hibernate.Hibernate
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

// private val log = KotlinLogging.logger {}

@Repository
class ProjektDao : BaseDao<ProjektDO>(ProjektDO::class.java) {
    @Autowired
    private val kundeDao: KundeDao? = null

    @Autowired
    private val groupDao: GroupDao? = null

    @Autowired
    private val taskDao: TaskDao? = null

    @Autowired
    private val taskTree: TaskTree? = null

    init {
        this.supportAfterUpdate = true
        userRightId = USER_RIGHT_ID
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    /**
     * @param projekt
     * @param kundeId If null, then kunde will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setKunde(projekt: ProjektDO, kundeId: Int?) {
        if (kundeId == null) {
            projekt.kunde = null
        } else {
            val kunde = kundeDao!!.getOrLoad(kundeId)
            projekt.kunde = kunde
        }
    }

    /**
     * @param projekt
     * @param taskId  If null, then task will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setTask(projekt: ProjektDO, taskId: Int?) {
        if (taskId == null) {
            projekt.task = null
        } else {
            val task = taskDao!!.getOrLoad(taskId)
            projekt.task = task
        }
    }

    fun setProjektManagerGroup(projekt: ProjektDO, groupId: Int?) {
        if (groupId == null) {
            projekt.projektManagerGroup = null
        } else {
            val group = groupDao!!.getOrLoad(groupId)
            projekt.projektManagerGroup = group
        }
    }

    /**
     * Initializes the projekt (projektManagerGroup), so any LazyInitializationException are avoided.
     *
     * @param projekt Null safe.
     */
    fun initializeProjektManagerGroup(projekt: ProjektDO?) {
        if (projekt == null) {
            return
        }
        // Needed because Hibernate Search rolls back because the project manager group is not loadable.
        Hibernate.initialize(projekt)
        val projectManagerGroup = projekt.projektManagerGroup
        if (projectManagerGroup != null) {
            val group = groupDao!!.internalGetById(projectManagerGroup.id)
            projekt.projektManagerGroup = group
            //Hibernate.initialize(projectManagerGroup); // Does not work.
        }
    }

    fun getProjekt(kunde: KundeDO?, nummer: Int): ProjektDO? {
        return persistenceService.selectSingleResult(
            "SELECT p FROM ProjektDO p WHERE p.kunde = :kunde and p.nummer = :nummer",
            ProjektDO::class.java,
            Pair("kunde", kunde),
            Pair("nummer", nummer)
        )
    }

    fun getProjekt(intern_kost2_4: Int, nummer: Int): ProjektDO? {
        return persistenceService.selectSingleResult(
            ProjektDO.FIND_BY_INTERNKOST24_AND_NUMMER,
            ProjektDO::class.java,
            Pair("internKost24", intern_kost2_4),
            Pair("nummer", nummer),
            namedQuery = true,
        )
    }

    override fun getList(filter: BaseSearchFilter): List<ProjektDO> {
        val myFilter = if (filter is ProjektFilter) {
            filter
        } else {
            ProjektFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (myFilter.isEnded) {
            queryFilter.add(eq("status", ProjektStatus.ENDED))
        } else if (myFilter.isNotEnded) {
            queryFilter.add(or(ne("status", ProjektStatus.ENDED), isNull("status")))
        }
        queryFilter.addOrder(asc("internKost2_4")).addOrder(asc("kunde.nummer")).addOrder(asc("nummer"))
        return getList(queryFilter)
    }

    fun getKundenProjekte(kundeId: Int?): List<ProjektDO?>? {
        if (kundeId == null) {
            return null
        }
        val queryFilter = QueryFilter()
        queryFilter.add(eq("kunde.id", kundeId))
        queryFilter.addOrder(asc("nummer"))
        return getList(queryFilter)
    }

    override fun onSaveOrModify(obj: ProjektDO) {
        if (obj.kunde != null) {
            // Ein Kundenprojekt kann keine interne Kundennummer haben:
            obj.internKost2_4 = null
        }
        if (obj.status == ProjektStatus.NONE) {
            obj.status = null
        }
        super.onSaveOrModify(obj)
    }

    override fun afterSaveOrModify(obj: ProjektDO) {
        if (obj.taskId != null) {
            taskTree!!.internalSetProject(obj.taskId, obj)
        }
        super.afterSaveOrModify(obj)
    }

    override fun afterUpdate(obj: ProjektDO, dbObj: ProjektDO?) {
        if (dbObj?.taskId != null && obj.taskId == null) {
            // Project task was removed:
            taskTree!!.internalSetProject(dbObj.taskId, null)
        }
        super.afterUpdate(obj, dbObj)
    }

    override fun newInstance(): ProjektDO {
        return ProjektDO()
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.PM_PROJECT
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "kunde.name", "kunde.division", "projektManagerGroup.name",
            "headOfBusinessManager.username", "headOfBusinessManager.firstname", "headOfBusinessManager.lastname",
            "salesManager.username", "salesManager.firstname", "salesManager.lastname",
        )
    }
}
