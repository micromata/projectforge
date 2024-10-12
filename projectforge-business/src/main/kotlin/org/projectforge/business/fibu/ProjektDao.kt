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
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// private val log = KotlinLogging.logger {}

@Service
open class ProjektDao : BaseDao<ProjektDO>(ProjektDO::class.java) {
    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

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
    fun setKunde(projekt: ProjektDO, kundeId: Long?) {
        persistenceService.runReadOnly { context ->
            setKunde(projekt, kundeId, context)
        }
    }

    /**
     * @param projekt
     * @param kundeId If null, then kunde will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setKunde(projekt: ProjektDO, kundeId: Long?, context: PfPersistenceContext) {
        if (kundeId == null) {
            projekt.kunde = null
        } else {
            val kunde = kundeDao.getOrLoad(kundeId, context)
            projekt.kunde = kunde
        }
    }

    /**
     * @param projekt
     * @param taskId  If null, then task will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setTask(projekt: ProjektDO, taskId: Long?) {
        return persistenceService.runReadOnly { context ->
            setTask(projekt, taskId, context)
        }
    }

    /**
     * @param projekt
     * @param taskId  If null, then task will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setTask(projekt: ProjektDO, taskId: Long?, context: PfPersistenceContext) {
        if (taskId == null) {
            projekt.task = null
        } else {
            val task = taskDao.getOrLoad(taskId, context)
            projekt.task = task
        }
    }

    fun setProjektManagerGroup(projekt: ProjektDO, groupId: Long?) {
        return persistenceService.runReadOnly { context ->
            setProjektManagerGroup(projekt, groupId, context)
        }
    }

    fun setProjektManagerGroup(projekt: ProjektDO, groupId: Long?, context: PfPersistenceContext) {
        if (groupId == null) {
            projekt.projektManagerGroup = null
        } else {
            val group = groupDao.getOrLoad(groupId, context)
            projekt.projektManagerGroup = group
        }
    }

    /**
     * Initializes the projekt (projektManagerGroup), so any LazyInitializationException are avoided.
     *
     * @param projekt Null safe.
     */
    fun initializeProjektManagerGroup(projekt: ProjektDO?, context: PfPersistenceContext) {
        if (projekt == null) {
            return
        }
        // Needed because Hibernate Search rolls back because the project manager group is not loadable.
        Hibernate.initialize(projekt)
        val projectManagerGroup = projekt.projektManagerGroup
        if (projectManagerGroup != null) {
            val group = groupDao.internalGetById(projectManagerGroup.id, context)
            projekt.projektManagerGroup = group
            //Hibernate.initialize(projectManagerGroup); // Does not work.
        }
    }

    fun getProjekt(kunde: KundeDO?, nummer: Long): ProjektDO? {
        return persistenceService.runReadOnly { context ->
            getProjekt(kunde, nummer, context)
        }
    }

    fun getProjekt(kunde: KundeDO?, nummer: Long, context: PfPersistenceContext): ProjektDO? {
        return context.selectSingleResult(
            "SELECT p FROM ProjektDO p WHERE p.kunde = :kunde and p.nummer = :nummer",
            ProjektDO::class.java,
            Pair("kunde", kunde),
            Pair("nummer", nummer)
        )
    }

    fun getProjekt(intern_kost2_4: Int, nummer: Int): ProjektDO? {
        return persistenceService.runReadOnly { context ->
            getProjekt(intern_kost2_4, nummer, context)
        }
    }

    fun getProjekt(intern_kost2_4: Int, nummer: Int, context: PfPersistenceContext): ProjektDO? {
        return context.selectNamedSingleResult(
            ProjektDO.FIND_BY_INTERNKOST24_AND_NUMMER,
            ProjektDO::class.java,
            Pair("internKost24", intern_kost2_4),
            Pair("nummer", nummer),
        )
    }

    override fun getList(filter: BaseSearchFilter, context: PfPersistenceContext): List<ProjektDO> {
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
        return getList(queryFilter, context)
    }

    fun getKundenProjekte(kundeId: Int?, context: PfPersistenceContext): List<ProjektDO?>? {
        if (kundeId == null) {
            return null
        }
        val queryFilter = QueryFilter()
        queryFilter.add(eq("kunde.id", kundeId))
        queryFilter.addOrder(asc("nummer"))
        return getList(queryFilter, context)
    }

    override fun onSaveOrModify(obj: ProjektDO, context: PfPersistenceContext) {
        if (obj.kunde != null) {
            // Ein Kundenprojekt kann keine interne Kundennummer haben:
            obj.internKost2_4 = null
        }
        if (obj.status == ProjektStatus.NONE) {
            obj.status = null
        }
        super.onSaveOrModify(obj, context)
    }

    override fun afterSaveOrModify(obj: ProjektDO, context: PfPersistenceContext) {
        obj.taskId?.let { taskId ->
            taskTree.internalSetProject(taskId, obj)
        }
        super.afterSaveOrModify(obj, context)
    }

    override fun afterUpdate(obj: ProjektDO, dbObj: ProjektDO?, context: PfPersistenceContext) {
        if (dbObj?.taskId != null && obj.taskId == null) {
            // Project task was removed:
            taskTree.internalSetProject(dbObj.taskId!!, null)
        }
        super.afterUpdate(obj, dbObj, context)
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
