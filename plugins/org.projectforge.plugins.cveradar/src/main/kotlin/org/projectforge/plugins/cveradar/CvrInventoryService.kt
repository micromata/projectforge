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


package org.projectforge.plugins.cveradar

import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrAreaDO
import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrConfidentiality
import org.projectforge.plugins.cveradar.model.CvrCriticality
import org.projectforge.plugins.cveradar.model.CvrInstanceDO
import org.projectforge.plugins.cveradar.model.CvrOrigin
import org.projectforge.plugins.cveradar.model.CvrSystemDO
import org.projectforge.plugins.cveradar.model.CvrSystemType
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.InstanceRequest
import org.projectforge.plugins.cveradar.rest.LibraryDiff
import org.projectforge.plugins.cveradar.rest.LibraryRequest
import org.projectforge.plugins.cveradar.rest.InventoryLevel
import org.projectforge.plugins.cveradar.rest.InventoryRow
import org.projectforge.plugins.cveradar.rest.InventoryTree
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.projectforge.plugins.cveradar.rest.TechnologyRequest
import org.projectforge.plugins.cveradar.rest.TechnologyView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The inventory (INV-01 ff.): areas as a tree of any depth, below them systems with instances and components.
 *
 * Who sees and changes what follows the groups along the tree: the group of an area counts for the area, everything
 * below it and its systems; a system may name a group of its own in addition. Members of these groups and the security
 * officer read and write. The security team reads systems that are *internal* only (confidential ones are for the
 * groups and the security officer, VIS-01). Whoever reads a node sees the areas above as a frame (code and name only),
 * without their other children. Parent, owner and ids always come from the database, never from the request.
 */
@Service
class CvrInventoryService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var areaDao: CvrAreaDao

    @Autowired
    private lateinit var catalogService: CvrCatalogService

    @Autowired
    private lateinit var libraryService: CvrLibraryService

    @Autowired
    private lateinit var componentDao: CvrComponentDao

    @Autowired
    private lateinit var instanceDao: CvrInstanceDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var systemDao: CvrSystemDao

    @Autowired
    private lateinit var technologyDao: CvrTechnologyDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** The nodes the user may see, flat and in tree order. */
    fun tree(user: PFUserDO): InventoryTree {
        val view = View(user, load())
        return view.tree()
    }

    // --- Areas ---

    fun createArea(user: PFUserDO, request: AreaRequest): Long {
        val view = View(user, load())
        val parent = request.parentId?.let { view.area(it) }
        val group = request.groupId?.let { findGroup(it) }
        if (parent == null) {
            // A group for a root is one of the creator (the officer chooses freely). Without a group only the officer
            // sees the area.
            if (group != null && !(view.officer || access.isMemberOf(user, group.id))) {
                throw UserException(ERROR_GROUP)
            }
        } else if (!view.canWriteArea(parent)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val area = CvrAreaDO().also {
            it.parent = parent
            it.code = checkedCode(request.code, view.model)
            it.name = checkedName(request.name)
            it.description = request.description.clean(TEXT_LENGTH)
            it.group = group
        }
        return areaDao.insert(area, checkAccess = false)
    }

    fun updateArea(user: PFUserDO, id: Long, request: AreaRequest) {
        val view = View(user, load())
        val area = view.area(id)
        if (!view.canWriteArea(area)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val group = request.groupId?.let { findGroup(it) }
        // Only a group of the editor can be newly assigned (the officer chooses freely); keeping or clearing it is free.
        if (group != null && group.id != area.groupId && !(view.officer || access.isMemberOf(user, group.id))) {
            throw UserException(ERROR_GROUP)
        }
        val stored = areaDao.find(id, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
        stored.code = checkedCode(request.code, view.model, exceptArea = area.id)
        stored.name = checkedName(request.name)
        stored.description = request.description.clean(TEXT_LENGTH)
        stored.group = group
        areaDao.update(stored, checkAccess = false)
    }

    /**
     * Moves an area or a system below [targetAreaId] (null: to the root, officer only). The user needs write access to
     * the node and to the target; below an area of others only the security officer may move (the groups of the target
     * would see the node).
     */
    fun move(user: PFUserDO, level: InventoryLevel, id: Long, targetAreaId: Long?) {
        val view = View(user, load())
        val target = targetAreaId?.let { view.area(it) }
        when (level) {
            InventoryLevel.AREA -> {
                val area = view.area(id)
                checkMove(view, view.canWriteArea(area), target)
                // Not into itself or one of its descendants.
                var up: CvrAreaDO? = target
                var guard = 0
                while (up != null && guard++ < MAX_DEPTH) {
                    if (up.id == area.id) {
                        throw UserException(ERROR_CYCLE)
                    }
                    up = view.model.areas[up.parentId]
                }
                val stored = areaDao.find(id, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
                stored.parent = target
                areaDao.update(stored, checkAccess = false)
            }

            InventoryLevel.SYSTEM -> {
                val system = view.system(id)
                if (target == null) {
                    throw UserException(ERROR_TARGET)
                }
                checkMove(view, view.canWriteSystem(system), target)
                val stored = systemDao.find(id, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
                stored.area = target
                systemDao.update(stored, checkAccess = false)
            }

            else -> throw UserException(ERROR_TARGET)
        }
    }

    private fun checkMove(view: View, mayWriteNode: Boolean, target: CvrAreaDO?) {
        val allowed = view.officer || (mayWriteNode && target != null && view.canWriteArea(target))
        if (!allowed) {
            throw AccessException(view.user, CveRadarAccess.NO_ACCESS)
        }
    }

    // --- Systems ---

    /** A new system gets its default instance (INV-04). */
    fun createSystem(user: PFUserDO, request: SystemRequest): Long {
        val view = View(user, load())
        val area = view.area(request.areaId ?: throw UserException(ERROR_NOT_FOUND))
        if (!view.canWriteArea(area)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val system = CvrSystemDO().also {
            applySystem(it, request, view.model)
            it.area = area
        }
        val id = systemDao.insert(system, checkAccess = false)
        instanceDao.insert(
            CvrInstanceDO().also { it.system = system; it.exposed = false },
            checkAccess = false,
        )
        return id
    }

    fun updateSystem(user: PFUserDO, id: Long, request: SystemRequest) {
        val view = View(user, load())
        val system = view.system(id)
        if (!view.canWriteSystem(system)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val stored = systemDao.find(id, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
        applySystem(stored, request, view.model, except = system)
        systemDao.update(stored, checkAccess = false)
        // An instance may only raise what the system lowered (INV-07): the system's value is the floor.
        view.model.instances.values.filter { it.systemId == id }.forEach { instance ->
            val floorC = stored.confidentiality!!
            val floorK = stored.criticality!!
            val raisedC = instance.confidentiality?.takeIf { it < floorC }
            val raisedK = instance.criticality?.takeIf { it < floorK }
            if (raisedC != null || raisedK != null) {
                val storedInstance = instanceDao.find(instance.id, checkAccess = false) ?: return@forEach
                if (raisedC != null) storedInstance.confidentiality = null
                if (raisedK != null) storedInstance.criticality = null
                instanceDao.update(storedInstance, checkAccess = false)
            }
        }
    }

    private fun applySystem(system: CvrSystemDO, request: SystemRequest, model: Model, except: CvrSystemDO? = null) {
        system.code = checkedCode(request.code, model, exceptSystem = except?.id)
        system.name = checkedName(request.name)
        system.description = request.description.clean(TEXT_LENGTH)
        system.type = request.type ?: CvrSystemType.APPLICATION
        system.criticality = request.criticality ?: CvrCriticality.MEDIUM
        system.confidentiality = request.confidentiality ?: CvrConfidentiality.INTERNAL
        system.group = request.groupId?.let { findGroup(it) }
    }

    // --- Instances ---

    /**
     * A new instance needs a code. The default instance of the system (without code) makes room if nothing hangs below
     * it: from now on the system has explicit instances.
     */
    fun createInstance(user: PFUserDO, request: InstanceRequest): Long {
        val view = View(user, load())
        val system = view.system(request.systemId ?: throw UserException(ERROR_NOT_FOUND))
        if (!view.canWriteSystem(system)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val instance = CvrInstanceDO().also {
            applyInstance(it, request, system, view.model)
            it.system = system
        }
        if (instance.code == null) {
            throw UserException(ERROR_CODE)
        }
        val id = instanceDao.insert(instance, checkAccess = false)
        view.model.instances.values
            .filter { it.systemId == system.id && it.code == null }
            .filter { default -> view.model.components.values.none { it.instanceId == default.id } }
            .forEach { instanceDao.markAsDeleted(instanceDao.find(it.id, checkAccess = false)!!, checkAccess = false) }
        return id
    }

    fun updateInstance(user: PFUserDO, id: Long, request: InstanceRequest) {
        val view = View(user, load())
        val instance = view.instance(id)
        val system = view.model.systems[instance.systemId] ?: throw UserException(ERROR_NOT_FOUND)
        if (!view.canWriteSystem(system)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val stored = instanceDao.find(id, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
        applyInstance(stored, request, system, view.model, except = instance)
        instanceDao.update(stored, checkAccess = false)
    }

    private fun applyInstance(
        instance: CvrInstanceDO,
        request: InstanceRequest,
        system: CvrSystemDO,
        model: Model,
        except: CvrInstanceDO? = null,
    ) {
        val code = request.code?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        // The default instance (no code) keeps having none.
        instance.code = if (except != null && except.code == null) null else code?.also {
            if (!CODE_REGEX.matches(it)) throw UserException(ERROR_CODE)
            if (model.instances.values.any { other -> other.systemId == system.id && other.code == it && other.id != except?.id }) {
                throw UserException(ERROR_CODE_EXISTS, it)
            }
        }
        instance.name = request.name.clean(255)
        instance.site = request.site.clean(100)
        instance.stage = request.stage.clean(100)
        instance.tenant = request.tenant.clean(255)
        instance.exposed = request.exposed
        instance.contact = request.contactId?.let { userGroupCache.getUser(it) }?.takeIf { !it.deleted }
        // Only raised, never lowered below the system (INV-07).
        instance.confidentiality = request.confidentiality?.takeIf { it > system.confidentiality!! }
        instance.criticality = request.criticality?.takeIf { it > system.criticality!! }
    }

    // --- Components and their technologies ---

    fun createComponent(user: PFUserDO, request: ComponentRequest): Long {
        val view = View(user, load())
        val instance = request.instanceId?.let { view.instance(it) }
        val system = view.model.systems[instance?.systemId ?: request.systemId] ?: throw UserException(ERROR_NOT_FOUND)
        if (!view.canWriteSystem(system)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val component = CvrComponentDO().also {
            // Of one instance, or of the baseline of the system: never both.
            if (instance != null) it.instance = instance else it.system = system
            it.code = checkedComponentCode(request.code, view.model, system.id, instance?.id)
            it.name = checkedName(request.name)
        }
        val id = componentDao.insert(component, checkAccess = false)
        syncTechnologies(component, request.technologies, emptyList())
        return id
    }

    fun updateComponent(user: PFUserDO, id: Long, request: ComponentRequest) {
        val view = View(user, load())
        val component = view.component(id)
        val system = view.model.systems[view.systemIdOf(component)] ?: throw UserException(ERROR_NOT_FOUND)
        if (!view.canWriteSystem(system)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val stored = componentDao.find(id, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
        stored.code = checkedComponentCode(request.code, view.model, system.id, component.instanceId, except = component)
        stored.name = checkedName(request.name)
        componentDao.update(stored, checkAccess = false)
        syncTechnologies(stored, request.technologies, view.model.technologies.filter { it.componentId == id })
    }

    fun previewLibraries(user: PFUserDO, componentId: Long, request: LibraryRequest): LibraryDiff =
        libraryService.preview(writableComponent(user, componentId), request)

    fun applyLibraries(user: PFUserDO, componentId: Long, request: LibraryRequest): LibraryDiff =
        libraryService.apply(writableComponent(user, componentId), request)

    /** The stored component, if the user may change its system; the component always comes from the database. */
    private fun writableComponent(user: PFUserDO, id: Long): CvrComponentDO {
        val view = View(user, load())
        val system = view.model.systems[view.systemIdOf(view.component(id))] ?: throw UserException(ERROR_NOT_FOUND)
        if (!view.canWriteSystem(system)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        return componentDao.find(id, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
    }

    /** Takes over the technologies of the request: the ones with an id of this component are changed, the rest created, missing ones removed. */
    private fun syncTechnologies(
        component: CvrComponentDO,
        requested: List<TechnologyRequest>,
        existing: List<CvrTechnologyDO>,
    ) {
        // Libraries read from lists (INV-26) are not part of the dialog.
        val editable = existing.filter { it.origin !in CvrOrigin.LIBRARY_LISTS }
        val byId = editable.associateBy { it.id }
        val kept = mutableSetOf<Long>()
        requested.forEach { request ->
            val product = request.product.clean(255) ?: throw UserException(ERROR_TECHNOLOGY)
            val technology = request.id?.let { byId[it] }?.let { technologyDao.find(it.id, checkAccess = false) }
                ?: CvrTechnologyDO().also { it.component = component; it.origin = CvrOrigin.MANUAL }
            technology.kind = request.kind ?: CvrTechnologyKind.PRODUCT
            technology.vendor = request.vendor.clean(100)
            technology.product = product
            technology.version = request.version.clean(100)
            technology.ecosystem = request.ecosystem.clean(50)
            technology.cpe = request.cpe.clean(500)
            technology.purl = request.purl.clean(500)
            technology.catalogNode = request.catalogNodeId?.let { catalogService.checkedNode(it) }
            if (technology.id == null) {
                technology.originDate = LocalDate.now()
                technologyDao.insert(technology, checkAccess = false)
            } else {
                kept.add(technology.id!!)
                technologyDao.update(technology, checkAccess = false)
            }
        }
        editable.filter { it.id !in kept }.forEach {
            technologyDao.markAsDeleted(technologyDao.find(it.id, checkAccess = false)!!, checkAccess = false)
        }
    }

    // --- Deleting ---

    /** Only a node without children is deleted (INV-20); the default instance of a system and the technologies go with it. */
    fun delete(user: PFUserDO, level: InventoryLevel, id: Long) {
        val view = View(user, load())
        val model = view.model
        when (level) {
            InventoryLevel.AREA -> {
                val area = view.area(id)
                if (!view.canWriteArea(area)) throw AccessException(user, CveRadarAccess.NO_ACCESS)
                if (model.areas.values.any { it.parentId == id } || model.systems.values.any { it.areaId == id }) {
                    throw UserException(ERROR_NOT_EMPTY)
                }
                areaDao.markAsDeleted(areaDao.find(id, checkAccess = false)!!, checkAccess = false)
            }

            InventoryLevel.SYSTEM -> {
                val system = view.system(id)
                if (!view.canWriteSystem(system)) throw AccessException(user, CveRadarAccess.NO_ACCESS)
                val instances = model.instances.values.filter { it.systemId == id }
                val ownComponents = model.components.values.any { it.systemId == id || it.instanceId in instances.map { i -> i.id } }
                if (ownComponents || instances.any { it.code != null }) {
                    throw UserException(ERROR_NOT_EMPTY)
                }
                instances.forEach { instanceDao.markAsDeleted(instanceDao.find(it.id, checkAccess = false)!!, checkAccess = false) }
                systemDao.markAsDeleted(systemDao.find(id, checkAccess = false)!!, checkAccess = false)
            }

            InventoryLevel.INSTANCE -> {
                val instance = view.instance(id)
                val system = model.systems[instance.systemId] ?: throw UserException(ERROR_NOT_FOUND)
                if (!view.canWriteSystem(system)) throw AccessException(user, CveRadarAccess.NO_ACCESS)
                if (model.components.values.any { it.instanceId == id }) throw UserException(ERROR_NOT_EMPTY)
                // The last instance stays: every system has at least one (INV-04).
                if (model.instances.values.count { it.systemId == system.id } <= 1) throw UserException(ERROR_LAST_INSTANCE)
                instanceDao.markAsDeleted(instanceDao.find(id, checkAccess = false)!!, checkAccess = false)
            }

            InventoryLevel.COMPONENT -> {
                val component = view.component(id)
                val system = model.systems[view.systemIdOf(component)] ?: throw UserException(ERROR_NOT_FOUND)
                if (!view.canWriteSystem(system)) throw AccessException(user, CveRadarAccess.NO_ACCESS)
                model.technologies.filter { it.componentId == id }.forEach {
                    technologyDao.markAsDeleted(technologyDao.find(it.id, checkAccess = false)!!, checkAccess = false)
                }
                componentDao.markAsDeleted(componentDao.find(id, checkAccess = false)!!, checkAccess = false)
            }
        }
    }

    // --- Helpers ---

    private fun findGroup(id: Long): GroupDO {
        return userGroupCache.getGroup(id)?.takeIf { !it.deleted } ?: throw UserException(ERROR_GROUP)
    }

    /** Area and system codes share one name space (INV-06); [exceptArea] and [exceptSystem] are the node being changed. */
    private fun checkedCode(code: String?, model: Model, exceptArea: Long? = null, exceptSystem: Long? = null): String {
        val normalized = code?.trim()?.uppercase()?.takeIf { CODE_REGEX.matches(it) } ?: throw UserException(ERROR_CODE)
        val taken = model.areas.values.any { it.code == normalized && it.id != exceptArea } ||
                model.systems.values.any { it.code == normalized && it.id != exceptSystem }
        if (taken) {
            throw UserException(ERROR_CODE_EXISTS, normalized)
        }
        return normalized
    }

    private fun checkedComponentCode(
        code: String?,
        model: Model,
        systemId: Long?,
        instanceId: Long?,
        except: CvrComponentDO? = null,
    ): String {
        val normalized = code?.trim()?.uppercase()?.takeIf { CODE_REGEX.matches(it) } ?: throw UserException(ERROR_CODE)
        val siblings = model.components.values.filter {
            it.id != except?.id && if (instanceId != null) it.instanceId == instanceId else it.systemId == systemId
        }
        if (siblings.any { it.code == normalized }) {
            throw UserException(ERROR_CODE_EXISTS, normalized)
        }
        return normalized
    }

    private fun checkedName(name: String?): String = name.clean(255) ?: throw UserException(ERROR_NAME)

    private fun String?.clean(maxLength: Int): String? = this?.trim()?.take(maxLength)?.takeIf { it.isNotEmpty() }

    /** All non-deleted nodes; the inventory is small enough to read it at once. */
    private fun load(): Model {
        fun <T> all(entity: String, clazz: Class<T>) =
            persistenceService.executeQuery("from $entity e where e.deleted = false", clazz)
        return Model(
            areas = all("CvrAreaDO", CvrAreaDO::class.java).associateBy { it.id!! },
            systems = all("CvrSystemDO", CvrSystemDO::class.java).associateBy { it.id!! },
            instances = all("CvrInstanceDO", CvrInstanceDO::class.java).associateBy { it.id!! },
            components = all("CvrComponentDO", CvrComponentDO::class.java).associateBy { it.id!! },
            technologies = all("CvrTechnologyDO", CvrTechnologyDO::class.java),
            catalogNames = all("CvrCatalogNodeDO", CvrCatalogNodeDO::class.java).associate { it.id!! to it.name },
        )
    }

    private class Model(
        val areas: Map<Long, CvrAreaDO>,
        val systems: Map<Long, CvrSystemDO>,
        val instances: Map<Long, CvrInstanceDO>,
        val components: Map<Long, CvrComponentDO>,
        val technologies: List<CvrTechnologyDO>,
        val catalogNames: Map<Long, String?>,
    )

    /** The inventory as one user sees it. */
    private inner class View(val user: PFUserDO, val model: Model) {
        val officer = access.isSecurityOfficer(user)
        private val team = access.isSecurityTeam(user)

        fun area(id: Long): CvrAreaDO = model.areas[id] ?: throw UserException(ERROR_NOT_FOUND)

        fun system(id: Long): CvrSystemDO = model.systems[id] ?: throw UserException(ERROR_NOT_FOUND)

        fun instance(id: Long): CvrInstanceDO = model.instances[id] ?: throw UserException(ERROR_NOT_FOUND)

        fun component(id: Long): CvrComponentDO = model.components[id] ?: throw UserException(ERROR_NOT_FOUND)

        fun systemIdOf(component: CvrComponentDO): Long? =
            component.systemId ?: model.instances[component.instanceId]?.systemId

        /** The area and its ancestors, nearest first (guarded against a cycle in the data). */
        fun chain(area: CvrAreaDO?): List<CvrAreaDO> {
            val result = mutableListOf<CvrAreaDO>()
            var up = area
            while (up != null && result.size < MAX_DEPTH) {
                result.add(up)
                up = model.areas[up.parentId]
            }
            return result
        }

        private fun isMember(groupIds: Collection<Long?>): Boolean =
            officer || groupIds.any { access.isMemberOf(user, it) }

        fun canWriteArea(area: CvrAreaDO): Boolean = isMember(chain(area).map { it.groupId })

        fun canWriteSystem(system: CvrSystemDO): Boolean =
            isMember(chain(model.areas[system.areaId]).map { it.groupId } + system.groupId)

        fun canReadSystem(system: CvrSystemDO): Boolean =
            canWriteSystem(system) || (team && system.confidentiality == CvrConfidentiality.INTERNAL)

        fun canReadInstance(instance: CvrInstanceDO): Boolean {
            val system = model.systems[instance.systemId] ?: return false
            return canWriteSystem(system) ||
                    (team && canReadSystem(system) &&
                            (instance.confidentiality ?: system.confidentiality) == CvrConfidentiality.INTERNAL)
        }

        fun tree(): InventoryTree {
            val readableAreas = model.areas.values.filter { canWriteArea(it) }.map { it.id!! }.toSet()
            val readableSystems = model.systems.values.filter { canReadSystem(it) }.map { it.id!! }.toSet()
            // The areas to show: those readable and the ancestors of every readable area and system.
            val visibleAreas = mutableSetOf<Long>()
            (readableAreas.map { model.areas[it] } + readableSystems.map { model.areas[model.systems[it]!!.areaId] })
                .forEach { start -> chain(start).forEach { visibleAreas.add(it.id!!) } }
            val rows = mutableListOf<InventoryRow>()
            val techByComponent = model.technologies.groupBy { it.componentId }
            val childAreas = model.areas.values.filter { it.id in visibleAreas }.groupBy { it.parentId }
            val systemsByArea = model.systems.values.filter { it.id in readableSystems }.groupBy { it.areaId }
            fun addArea(area: CvrAreaDO, parentKey: String?, depth: Int) {
                if (depth > MAX_DEPTH) return
                val readable = area.id in readableAreas
                val canEdit = readable
                rows.add(areaRow(area, parentKey, readable, canEdit))
                val key = "A${area.id}"
                childAreas[area.id].orEmpty().sortedBy { it.name?.lowercase() }.forEach { addArea(it, key, depth + 1) }
                systemsByArea[area.id].orEmpty().sortedBy { it.name?.lowercase() }.forEach { system ->
                    val canEditSystem = canWriteSystem(system)
                    rows.add(systemRow(system, key, canEditSystem))
                    val systemKey = "S${system.id}"
                    val instances = model.instances.values.filter { it.systemId == system.id }
                    val explicit = instances.any { it.code != null }
                    if (explicit) {
                        instances.filter { canReadInstance(it) }.sortedBy { it.code ?: "" }.forEach { instance ->
                            rows.add(instanceRow(instance, system, systemKey, canEditSystem))
                            val instanceKey = "I${instance.id}"
                            model.components.values.filter { it.instanceId == instance.id }.sortedBy { it.code }
                                .forEach { rows.add(componentRow(it, instance, system, instanceKey, canEditSystem, techByComponent)) }
                        }
                    }
                    model.components.values.filter { it.systemId == system.id }.sortedBy { it.code }
                        .forEach { rows.add(componentRow(it, null, system, systemKey, canEditSystem, techByComponent)) }
                }
            }
            childAreas[null].orEmpty().sortedBy { it.name?.lowercase() }.forEach { addArea(it, null, 0) }
            return InventoryTree(
                rows,
                officer,
                model.instances.values.mapNotNull { it.site }.distinct().sorted(),
                model.instances.values.mapNotNull { it.stage }.distinct().sorted(),
            )
        }

        /** The nearest group on the way up: the own one, then the areas'. */
        private fun effectiveGroup(own: GroupDO?, area: CvrAreaDO?, ownCode: String?): Pair<String?, String?> {
            if (own != null) return Pair(groupName(own.id), ownCode)
            val from = chain(area).firstOrNull { it.groupId != null } ?: return Pair(null, null)
            return Pair(groupName(from.groupId), from.code)
        }

        private fun groupName(id: Long?): String? = userGroupCache.getGroup(id)?.name

        private fun areaRow(area: CvrAreaDO, parentKey: String?, readable: Boolean, canEdit: Boolean): InventoryRow {
            if (!readable) {
                return InventoryRow("A${area.id}", area.id!!, InventoryLevel.AREA, parentKey, area.code, area.code, area.name, true)
            }
            val (effective, from) = effectiveGroup(area.group, model.areas[area.parentId], area.code)
            return InventoryRow(
                "A${area.id}", area.id!!, InventoryLevel.AREA, parentKey, area.code, area.code, area.name, false, canEdit,
                description = area.description, groupId = area.groupId, groupName = groupName(area.groupId),
                effectiveGroupName = effective, effectiveGroupFrom = from, lastUpdate = area.lastUpdate,
            )
        }

        private fun systemRow(system: CvrSystemDO, parentKey: String, canEdit: Boolean): InventoryRow {
            val area = model.areas[system.areaId]
            val (effective, from) = effectiveGroup(system.group, area, system.code)
            return InventoryRow(
                "S${system.id}", system.id!!, InventoryLevel.SYSTEM, parentKey, system.code, system.code, system.name, false,
                canEdit, description = system.description, groupId = system.groupId, groupName = groupName(system.groupId),
                effectiveGroupName = effective, effectiveGroupFrom = from, systemType = system.type,
                criticality = system.criticality, confidentiality = system.confidentiality, lastUpdate = system.lastUpdate,
            )
        }

        private fun instanceRow(instance: CvrInstanceDO, system: CvrSystemDO, parentKey: String, canEdit: Boolean): InventoryRow {
            val contact = userGroupCache.getUser(instance.contact?.id)
            return InventoryRow(
                "I${instance.id}", instance.id!!, InventoryLevel.INSTANCE, parentKey,
                system.code + (instance.code?.let { "-$it" } ?: ""), instance.code, instance.name ?: system.name, false,
                canEdit, systemType = system.type,
                criticality = instance.criticality ?: system.criticality,
                confidentiality = instance.confidentiality ?: system.confidentiality,
                site = instance.site, stage = instance.stage, tenant = instance.tenant, exposed = instance.exposed,
                contactId = contact?.id, contactName = contact?.displayName, defaultInstance = instance.code == null,
                lastUpdate = instance.lastUpdate,
            )
        }

        private fun componentRow(
            component: CvrComponentDO,
            instance: CvrInstanceDO?,
            system: CvrSystemDO,
            parentKey: String,
            canEdit: Boolean,
            techByComponent: Map<Long?, List<CvrTechnologyDO>>,
        ): InventoryRow {
            val base = system.code + (instance?.code?.let { "-$it" } ?: "")
            return InventoryRow(
                "C${component.id}", component.id!!, InventoryLevel.COMPONENT, parentKey, "$base-${component.code}",
                component.code, component.name, false, canEdit,
                technologies = techByComponent[component.id].orEmpty().sortedBy { it.product?.lowercase() }.map {
                    TechnologyView(it.id!!, it.kind, it.vendor, it.product, it.version, it.ecosystem, it.cpe, it.purl, it.origin,
                        it.catalogNodeId, it.catalogNodeId?.let { id -> model.catalogNames[id] })
                },
                lastUpdate = component.lastUpdate,
            )
        }
    }

    companion object {
        private const val MAX_DEPTH = 100
        private const val TEXT_LENGTH = 4000

        private val CODE_REGEX = Regex("""^[A-Z0-9][A-Z0-9._-]{0,49}$""")

        const val ERROR_NOT_FOUND = "plugins.cveradar.inventory.error.notFound"
        const val ERROR_CODE = "plugins.cveradar.inventory.error.code"
        const val ERROR_CODE_EXISTS = "plugins.cveradar.inventory.error.codeExists"
        const val ERROR_NAME = "plugins.cveradar.inventory.error.name"
        const val ERROR_GROUP = "plugins.cveradar.inventory.error.group"
        const val ERROR_CYCLE = "plugins.cveradar.inventory.error.cycle"
        const val ERROR_TARGET = "plugins.cveradar.inventory.error.target"
        const val ERROR_NOT_EMPTY = "plugins.cveradar.inventory.error.notEmpty"
        const val ERROR_LAST_INSTANCE = "plugins.cveradar.inventory.error.lastInstance"
        const val ERROR_TECHNOLOGY = "plugins.cveradar.inventory.error.technology"
    }
}
