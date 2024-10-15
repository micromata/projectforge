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

package org.projectforge.business.task

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.MapUtils
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.hibernate.Hibernate
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionVO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper.greaterZero
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.Serializable
import java.io.StringWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Holds the complete task list in a tree. It will be initialized by the values read from the database. Any changes will
 * be written to this tree and to the database.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class TaskTree : AbstractCache(TICKS_PER_HOUR),
    Serializable {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @PostConstruct
    private fun postConstruct() {
        if (backingInstance != null) {
            log.warn("Oups, shouldn't instantiate TaskTree twice")
            return
        }
        backingInstance = this
        auftragDao.registerTaskTree(this)
    }

    /**
     * Time of last modification in milliseconds from 1970-01-01.
     */
    var timeOfLastModification: Long = 0

    /**
     * For faster searching of entries.
     */
    private var taskMap = mutableMapOf<Long, TaskNode>()

    /**
     * The root node of all tasks. The only node with parent null.
     */
    private var root: TaskNode? = null

    private var orderPositionReferences: Map<Long?, Set<AuftragsPositionVO>?>? = null

    private var orderPositionReferencesDirty = true

    val rootTaskNode: TaskNode
        get() {
            checkRefresh()
            return this.root!!
        }

    /**
     * Adds the given node as child of the given parent.
     */
    @Synchronized
    private fun addTaskNode(node: TaskNode, parent: TaskNode?): TaskNode {
        checkRefresh()
        if (parent != null) {
            node.setParent(parent)
            parent.addChild(node)
        }
        updateTimeOfLastModification()
        return node
    }

    /**
     * Adds a new node with the given data. The given Task holds all data and the information (id) of the parent node of
     * the node to add. Will be called by TaskDAO after inserting a new task.
     */
    fun addTaskNode(task: TaskDO): TaskNode {
        checkRefresh()
        val node = TaskNode()
        node.setTask(task)
        val parent = getTaskNodeById(task.parentTaskId)
        if (parent != null) {
            node.setParent(parent)
        } else if (root == null) {
            // this is the root node:
            root = node
        } else if (node.id != root!!.id) {
            // This node is not the root node:
            node.setParent(root)
        }
        synchronized(taskMap) {
            taskMap[node.id!!] = node
        }
        val timesheet = TimesheetDO()
        timesheet.task = task
        val bookable = timesheetDao.checkTaskBookable(
            timesheet, oldTimesheet = null, OperationType.INSERT, throwException = false,
            checkTaskTreeRefresh = false,
        )
        node.bookableForTimesheets = bookable
        return addTaskNode(node, parent)
    }

    /**
     * @param taskId
     * @param ancestorTaskId
     * @return
     * @see TaskNode.getPathToAncestor
     */
    fun getPath(taskId: Long?, ancestorTaskId: Long?): List<TaskNode> {
        checkRefresh()
        if (taskId == null) {
            return EMPTY_LIST
        }
        val taskNode = getTaskNodeById(taskId) ?: return EMPTY_LIST
        return taskNode.getPathToAncestor(ancestorTaskId)
    }

    /**
     * Returns the path to the root node in an ArrayList.
     *
     * @see .getPath
     */
    fun getPathToRoot(taskId: Long?): List<TaskNode> {
        return getPath(taskId, null)
    }

    fun getAncestorTaskIs(taskId: Long?): MutableList<Long?> {
        val resultList = getPathToRoot(taskId)
        val result: MutableList<Long?> = ArrayList()
        for (node in resultList) {
            result.add(node.id)
        }
        return result
    }

    fun getAncestorAndDescendantTaskIs(taskId: Long?, includeSelf: Boolean): List<Long?> {
        val result = getAncestorTaskIs(taskId)
        result.addAll(getDescendantTaskIds(taskId, includeSelf))
        return result
    }

    fun getDescendantTaskIds(taskId: Long?, includeSelf: Boolean): List<Long?> {
        val resultList = getDescendants(taskId, includeSelf)
        val result: MutableList<Long?> = ArrayList()
        for (node in resultList) {
            result.add(node.id)
        }
        return result
    }

    fun getDescendants(taskId: Long?, includeSelf: Boolean): List<TaskNode> {
        val resultList: MutableList<TaskNode> = ArrayList()
        val node = getTaskNodeById(taskId) ?: return resultList
        if (includeSelf) {
            resultList.add(node)
        }
        addDescendants(resultList, node)
        return resultList
    }

    private fun addDescendants(resultList: MutableList<TaskNode>, node: TaskNode) {
        for (child in node.getChildren()) {
            resultList.add(child)
        }
    }

    /**
     * All task nodes are stored in an HashMap for faster searching.
     * @param taskId Task id.
     * @param checkRefresh If true, the task tree will be refreshed (if expired) before searching the task. Default is true.
     */
    @JvmOverloads
    fun getTaskNodeById(taskId: Long?, checkRefresh: Boolean = true): TaskNode? {
        if (taskId == null) {
            return null
        }
        if (checkRefresh) {
            checkRefresh()
        }
        synchronized(taskMap) {
            return taskMap[taskId]
        }
    }

    fun getTaskById(id: Long?): TaskDO? {
        checkRefresh()
        val node = getTaskNodeById(id)
        if (node != null) {
            return node.getTask()
        }
        return null
    }

    /**
     * Gets the project, which is assigned to the task or if not found to the parent task or grand parent task etc.
     *
     * @param taskId
     * @return null, if now project is assigned to this task or ancestor tasks.
     */
    fun getProjekt(taskId: Long?): ProjektDO? {
        if (taskId == null) {
            return null
        }
        val node = getTaskNodeById(taskId)
        return node?.getProjekt()
    }

    fun internalSetProject(taskId: Long, projekt: ProjektDO?) {
        val node = getTaskNodeById(taskId)
            ?: throw InternalErrorException("Could not found task with id $taskId in internalSetProject")
        node.projekt = projekt
    }

    /**
     * recursive = true.
     *
     * @param taskId
     * @return
     * @see .getKost2List
     */
    fun getKost2List(taskId: Long?): List<Kost2DO>? {
        val node = getTaskNodeById(taskId)
        return getKost2List(node, true)
    }

    /**
     * Get the available and active Kost2DOs of the task, or if not available of the first found ancestor tasks if
     * available. Kost2 are defined over assigned projects and kost2s. If project or Kost2DO not assigned for a task, then
     * the project or task of the parent task will be assumed. If the parent task has no project or task the grand parent
     * task will be taken and so on (recursive until root task).
     *
     * @param taskId
     * @param recursive If true then search the ancestor task for cost definitions if current task haven't.
     * @return Available Kost2DOs or null, if no Kost2DO found.
     */
    fun getKost2List(taskId: Long?, recursive: Boolean): List<Kost2DO?>? {
        val node = getTaskNodeById(taskId)
        return getKost2List(node, recursive)
    }

    /**
     * @param projekt          If not initialized then the project is get from the data base.
     * @param task             Only needed for output if an entry (Kost2) of the blackWhiteList cannot be found.
     * @param blackWhiteList
     * @param kost2IsBlackList
     * @return
     */
    fun getKost2List(
        projekt: ProjektDO?, task: TaskDO, blackWhiteList: Array<String>?,
        kost2IsBlackList: Boolean
    ): List<Kost2DO>? {
        var useProjekt = projekt
        val kost2List = mutableListOf<Kost2DO>()
        val wildcard = blackWhiteList != null && blackWhiteList.size == 1 && "*" == blackWhiteList[0]
        if (useProjekt != null && !Hibernate.isPropertyInitialized(useProjekt, "kunde")) {
            useProjekt = projektDao.find(useProjekt.id, checkAccess = false)
        }
        if (useProjekt != null) {
            kostCache.getActiveKost2(
                useProjekt.nummernkreis,
                useProjekt.bereich!!,
                useProjekt.nummer
            )?.run {
                forEach { kost2 ->
                    if (wildcard) { // black-white-list is "*".
                        if (kost2IsBlackList) {
                            return@run // Do not add any entry.
                        } else {
                            kost2List.add(kost2) // Add all entries.
                        }
                    } else if (blackWhiteList == null || blackWhiteList.size == 0) {
                        // Add all (either black nor white entry is given):
                        kost2List.add(kost2)
                    } else {
                        val no = kost2.formattedNumber
                        var add = kost2IsBlackList // false for white list and true for black list at default.
                        for (item in blackWhiteList) {
                            if (no.endsWith(item)) {
                                if (kost2IsBlackList) {
                                    // Black list entry matches, so do not add entry:
                                    add = false
                                    break
                                } else {
                                    // White list entry matches, so add entry:
                                    add = true
                                    break
                                }
                            }
                        }
                        if (add) {
                            kost2List.add(kost2)
                        }

                    }
                }
            }
        } else if (!kost2IsBlackList && blackWhiteList != null) {
            // Add all given KoSt2DOs.
            var infoLogDone = false
            for (item in blackWhiteList) {
                val kost2 = kostCache.getKost2(item)
                if (kost2 != null) {
                    kost2List.add(kost2)
                } else if (!infoLogDone) {
                    if (item.length <= 2) {
                        log.info("Given kost2 not found: '" + item + "' of white list '" + task.kost2BlackWhiteList + "'. Project not linked anymore to task? Task id=" + task.id + ", task=" + task)
                    } else {
                        log.info("Given kost2 not found: '" + item + "' of white list '" + task.kost2BlackWhiteList + "'. Specified at task with id " + task.id + ": " + task)
                    }
                    infoLogDone = true
                }
            }
        }
        return if (kost2List.isNotEmpty()) {
            kost2List.sorted()
        } else {
            null
        }
    }

    private fun getKost2List(node: TaskNode?, recursive: Boolean): List<Kost2DO>? {
        node ?: return null
        val task = node.getTask()
        val blackWhiteList = task.kost2BlackWhiteItems
        val projekt =
            node.getProjekt(blackWhiteList != null) // If black-white-list is null then do not search for projekt of
        // ancestor tasks.
        val list = getKost2List(projekt, task, blackWhiteList, task.kost2IsBlackList)
        return list
            ?: if (node.parent != null && recursive) {
                getKost2List(node.parent, recursive)
            } else {
                null
            }
    }

    /**
     * Should be called after modification of a time sheet assigned to the given task id.
     *
     * @param taskId
     */
    fun resetTotalDuration(taskId: Long) {
        val node = getTaskNodeById(taskId)
        if (node == null) {
            log.error("Task id '$taskId' not found.")
            return
        }
        node.totalDuration = -1
    }

    /**
     * After changing a task this method will be called by TaskDao for updating the task and the task tree.
     *
     * @param task Updating the existing task in the taskTree. If not exist, a new task will be added.
     */
    fun addOrUpdateTaskNode(task: TaskDO): TaskNode {
        checkRefresh()
        requireNotNull(task.id)
        val node = getTaskNodeById(task.id) ?: return addTaskNode(task)
        node.setTask(task)
        if (task.parentTaskId != null && task.parentTaskId != node.getParent().id) {
            log.debug { "Task hierarchy was changed for task: $task" }
            val oldParent = node.getParent()
            requireNotNull(oldParent)
            oldParent.removeChild(node)
            val newParent = getTaskNodeById(task.parentTaskId)
            node.setParent(newParent)
            newParent!!.addChild(node)
        }
        updateTimeOfLastModification()
        return node
    }

    /**
     * Sets an explicit task group access for the given task (stored in the given groupTaskAccess). This method will be
     * called by AccessDao after inserting or updating GroupTaskAccess to the database.
     *
     * @see GroupTaskAccessDO
     */
    fun setGroupTaskAccess(groupTaskAccess: GroupTaskAccessDO) {
        checkRefresh()
        val taskId = groupTaskAccess.taskId
        val node = getTaskNodeById(taskId, false)!!
        node.setGroupTaskAccess(groupTaskAccess)
    }

    /**
     * Removes an explicit task group access for the given task (stored in the given groupTaskAccess). This method will be
     * called by AccessDao after deleting GroupTaskAccess from the database.
     *
     * @see GroupTaskAccessDO
     */
    fun removeGroupTaskAccess(groupTaskAccess: GroupTaskAccessDO) {
        checkRefresh()
        val taskId = groupTaskAccess.taskId
        val node: TaskNode
        synchronized(taskMap) {
            node = taskMap[taskId]!!
        }
        node.removeGroupTaskAccess(groupTaskAccess.groupId)
    }

    override fun toString(): String {
        if (root == null) {
            return "<empty/>"
        }
        val document = DocumentHelper.createDocument()
        val root = document.addElement("root")
        this.root!!.addXMLElement(root)
        // Pretty print the document to System.out
        val sw = StringWriter()
        var result = ""
        val writer = XMLWriter(sw, OutputFormat.createPrettyPrint())
        try {
            writer.write(document)
            result = sw.toString()
        } catch (ex: IOException) {
            log.error(ex.message, ex)
        } finally {
            try {
                writer.close()
            } catch (ex: IOException) {
                log.error("Error while closing xml writer: " + ex.message, ex)
            }
        }
        return result
    }

    /**
     * Has the current logged in user select access to the given task?
     *
     * @param node
     * @return
     */
    fun hasSelectAccess(node: TaskNode): Boolean {
        return taskDao.hasLoggedInUserSelectAccess(node.getTask(), false)
    }

    /**
     * @see .isRootNode
     */
    fun isRootNode(node: TaskNode): Boolean {
        return isRootNode(node.getTask())
    }

    /**
     * @param task The task to check (required as not null).
     * @return true, if the given task has the same id as the task tree's root node, otherwise false;
     */
    fun isRootNode(task: TaskDO): Boolean {
        if (root == null && task.parentTaskId == null) {
            // First task, so it should be the root node.
            return true
        }
        checkRefresh()
        if (task.id == null) {
            // Node has no id, so it can't be the root node.
            return false
        }
        return root!!.id == task.id
    }

    /**
     * Should be called after manipulations of any order position if a task reference was changed. This method declares
     * the reference map as dirty, therefore before the next usage the map will be rebuild from the database.
     */
    fun refreshOrderPositionReferences() {
        synchronized(this) {
            this.orderPositionReferencesDirty = true
        }
    }

    /**
     * Does any order position entry with a task reference exist?
     */
    fun hasOrderPositionsEntries(): Boolean {
        checkRefresh()
        return (MapUtils.isNotEmpty(orderPositionEntries))
    }

    private val orderPositionEntries: Map<Long?, Set<AuftragsPositionVO>?>?
        get() {
            synchronized(this) {
                if (this.orderPositionReferencesDirty) {
                    this.orderPositionReferences = auftragDao.taskReferences
                    this.orderPositionReferences?.let { orderPositionReferences ->
                        resetOrderPersonDays(root!!)
                        orderPositionReferences.forEach { (key, value) ->
                            val node = getTaskNodeById(key)
                            node!!.orderedPersonDays = null
                            if (CollectionUtils.isNotEmpty(value)) {
                                for (pos in value!!) {
                                    if (pos.personDays == null) {
                                        return@forEach
                                    }
                                    if (node.orderedPersonDays == null) {
                                        node.orderedPersonDays = BigDecimal.ZERO
                                    }
                                    node.orderedPersonDays = node.orderedPersonDays.add(pos.personDays)
                                }
                            }
                        }
                    }
                    this.orderPositionReferencesDirty = false
                }
                return this.orderPositionReferences
            }
        }

    private fun resetOrderPersonDays(node: TaskNode) {
        node.orderedPersonDays = null
        if (node.hasChildren()) {
            for (child in node.getChildren()) {
                resetOrderPersonDays(child)
            }
        }
    }

    /**
     * @param taskId
     * @return Set of all order positions assigned to the given task.
     */
    fun getOrderPositionEntries(taskId: Long?): Set<AuftragsPositionVO>? {
        checkRefresh()
        return orderPositionEntries!![taskId]
    }

    /**
     * @param taskId
     * @return
     */
    fun getOrderPositionsUpwards(taskId: Long?): Set<AuftragsPositionVO> {
        val set: MutableSet<AuftragsPositionVO> = TreeSet()
        addOrderPositionsUpwards(set, taskId)
        return set
    }

    private fun addOrderPositionsUpwards(set: MutableSet<AuftragsPositionVO>, taskId: Long?) {
        val set2 = getOrderPositionEntries(taskId)
        if (CollectionUtils.isNotEmpty(set2)) {
            set.addAll(set2!!)
        }
        val task = getTaskById(taskId)
        if (task?.parentTaskId != null) {
            addOrderPositionsUpwards(set, task.parentTaskId)
        }
    }

    /**
     * @param taskId
     * @param recursive if true also all descendant tasks will be searched for assigned order positions.
     * @return
     */
    fun hasOrderPositions(taskId: Long?, recursive: Boolean): Boolean {
        if (taskId == null) { // For new tasks.
            return false
        }
        if (CollectionUtils.isNotEmpty(getOrderPositionEntries(taskId))) {
            return true
        }
        if (recursive) {
            val node = getTaskNodeById(taskId)
            if (node != null && node.hasChildren()) {
                for (child in node.getChildren()) {
                    if (hasOrderPositions(child.id, recursive)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * @param taskId
     * @return True, if the given task has order positions or any ancestor task has an order position.
     */
    fun hasOrderPositionsUpwards(taskId: Long?): Boolean {
        if (hasOrderPositions(taskId, false)) {
            return true
        }
        val task = getTaskNodeById(taskId)
        if (task != null && task.parentId != null) {
            return hasOrderPositionsUpwards(task.parentId)
        }
        return false
    }

    /**
     * @param taskId
     * @see .getPersonDays
     */
    fun getPersonDays(taskId: Long?): BigDecimal? {
        val node = getTaskNodeById(taskId)
        return getPersonDays(node)
    }

    /**
     * @param node
     * @return The ordered person days or if not found the defined max hours. If both not found, the get the sum of all
     * diect or null if both not found.
     */
    fun getPersonDays(node: TaskNode?): BigDecimal? {
        checkRefresh()
        if (node == null || node.isDeleted) {
            return null
        }
        if (hasOrderPositions(node.id, true)) {
            return getOrderedPersonDaysSum(node)
        }
        val maxHours = node.getTask().maxHours
        if (maxHours != null) {
            return BigDecimal(maxHours).divide(DateHelper.HOURS_PER_WORKING_DAY, 2, RoundingMode.HALF_UP)
        }
        if (!node.hasChildren()) {
            return null
        }
        var result: BigDecimal? = null
        for (child in node.getChildren()) {
            val childPersonDays = getPersonDays(child)
            if (childPersonDays != null) {
                if (result == null) {
                    result = BigDecimal.ZERO
                }
                result = result!!.add(childPersonDays)
            }
        }
        return result
    }

    /**
     * @return The sum of all ordered person days. This method checks the given node and all sub-nodes for assigned order
     * positions.
     */
    fun getOrderedPersonDaysSum(node: TaskNode): BigDecimal? {
        var personDays: BigDecimal? = null
        if (node.orderedPersonDays != null) {
            personDays = node.orderedPersonDays
        }
        if (node.hasChildren()) {
            for (child in node.getChildren()) {
                val childPersonDays = getOrderedPersonDaysSum(child)
                if (childPersonDays != null) {
                    personDays = if (personDays == null) {
                        childPersonDays
                    } else {
                        personDays.add(childPersonDays)
                    }
                }
            }
        }
        return personDays
    }

    fun getPersonDaysNode(node: TaskNode?): TaskNode? {
        if (node == null) {
            return null
        }
        if (node.orderedPersonDays != null) {
            return node
        }
        if (greaterZero(node.getTask().maxHours)) {
            return node
        }
        return getPersonDaysNode(node.getParent())
    }

    /**
     * Reads the sum of all time sheet durations grouped by task id and set the total duration of found taskNodes.
     */
    private fun readTotalDurations() {
        val list = taskDao.readTotalDurations()
        for (res in list) {
            val taskId = res[1] as Long
            val node = getTaskNodeById(taskId, false)
            if (node == null) {
                log.warn { "Task not found: $taskId" }
            } else {
                if (res[0] is Int) {
                    node.totalDuration = (res[0] as Int).toLong()
                } else {
                    node.totalDuration = (res[0] as Long)
                }
            }
        }
    }

    /**
     * Reads the sum of all time sheet durations grouped by task id and set the total duration of found taskNodes.
     */
    fun readTotalDuration(taskId: Long) {
        val duration = taskDao.readTotalDuration(taskId)
        val node = getTaskNodeById(taskId)
        if (node == null) {
            log.warn("Task not found: $taskId")
        } else {
            node.totalDuration = duration
        }
    }

    /**
     * Should only called by test suite!
     */
    fun clear() {
        this.root = null
        this.setExpired()
    }

    /**
     * All tasks from database will be read and cached into this TaskTree. Also all explicit group task access' will be
     * read from database and will be cached in this tree (implicit access' will be created too).<br></br>
     * The generation of the task tree will be done manually, not by hibernate because the task hierarchy is very
     * sensible. Manipulations of the task tree should be done carefully for single task nodes.
     *
     * @see org.projectforge.framework.cache.AbstractCache.refresh
     */
    public override fun refresh() {
        log.info("Initializing task tree ...")
        val saved = persistenceService.saveStatsState()
        persistenceService.runIsolatedReadOnly { _ ->
            var newRoot: TaskNode? = null
            val nTaskMap = mutableMapOf<Long, TaskNode>()
            val taskList = taskDao.selectAll(checkAccess = false)
            log.debug("Loading list of tasks ...")
            // First create all nodes and put them into the map:
            // The root node is the first node without parent node.
            for (task in taskList) {
                val node = TaskNode().apply { setTask(task) }
                nTaskMap[node.taskId] = node
                if (!node.isRootNode) {
                    continue
                }
                if (newRoot != null) {
                    log.error { "Duplicate root node found: ${newRoot!!.id} and ${node.id}" }
                    node.setParent(newRoot) // Set the second root task as child task of first read root task.
                } else {
                    log.debug { "Root note found: $node" }
                    newRoot = node
                }
            }

            if (newRoot == null) {
                throw IllegalArgumentException("No root task found. Corrupted data-base or not correct initialized one.")
            }
            this.root = newRoot
            log.debug { "Creating tree for " + taskList.size + " tasks ..." }
            taskList.forEach { task ->
                val node = nTaskMap[task.id]!!
                val parentNode = task.parentTaskId?.let { nTaskMap[it] }
                if (parentNode != null) {
                    node.setParent(parentNode)
                    parentNode.addChild(node)
                    updateTimeOfLastModification()
                } else {
                    log.debug { "Processing root node:$node" }
                }
            }
            log.debug { root.toString() }

            // Now read all explicit group task access' from the database:
            accessDao.selectAll(checkAccess = false).forEach { access ->
                val node = nTaskMap.get(access.taskId)!!
                node.setGroupTaskAccess(access)
                log.debug { access.toString() }
            }
            // Now read all projects with their references to tasks:
            projektDao.selectAll(checkAccess = false).forEach { project ->
                if (project.deleted || project.taskId == null) {
                    return@forEach
                }
                val node = nTaskMap[project.taskId]
                node.let { n ->
                    if (n == null) {
                        log.error { "Oups, should not occur: project references a non existing task: $project" }
                    } else {
                        n.projekt = project
                    }
                }
            }
            log.debug { this.toString() }
            this.taskMap = nTaskMap
            readTotalDurations()
            refreshOrderPositionReferences()
            // Now update the status: bookable for time sheets:
            val timesheet = TimesheetDO()
            taskList.forEach { task ->
                val node = nTaskMap[task.id]
                timesheet.task = task
                val bookable = timesheetDao.checkTaskBookable(
                    timesheet,
                    oldTimesheet = null,
                    OperationType.INSERT,
                    throwException = false,
                    checkTaskTreeRefresh = false,
                )
                node!!.bookableForTimesheets = bookable
            }
        }
        log.info("Initializing task tree done. stats=${persistenceService.formatStats(saved)}")
    }

    private fun updateTimeOfLastModification() {
        this.timeOfLastModification = Date().time
    }

    companion object {
        private const val serialVersionUID = 3748005966442878168L

        const val USER_PREFS_KEY_OPEN_TASKS: String = "openTasks"

        private val EMPTY_LIST: List<TaskNode> = ArrayList()

        @JvmStatic
        val instance: TaskTree
            get() = backingInstance!!

        private var backingInstance: TaskTree? = null
    }
}
