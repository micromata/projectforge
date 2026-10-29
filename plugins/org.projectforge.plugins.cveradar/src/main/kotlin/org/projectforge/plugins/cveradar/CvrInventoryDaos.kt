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

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrAreaDO
import org.projectforge.plugins.cveradar.model.CvrCatalogEdgeDO
import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrInstanceDO
import org.projectforge.plugins.cveradar.model.CvrSystemDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.springframework.stereotype.Service

/**
 * The daos of the inventory only store (with history). Who may see or change which node depends on the groups along
 * the tree, which [CvrInventoryService] decides; the daos themselves refuse every access, so nothing reaches them
 * but the service (`checkAccess = false`).
 */
abstract class CvrInventoryBaseDao<O : ExtendedBaseDO<Long>>(clazz: Class<O>) : BaseDao<O>(clazz) {
    override fun hasAccess(
        user: PFUserDO,
        obj: O?,
        oldObj: O?,
        operationType: OperationType,
        throwException: Boolean,
    ): Boolean = false

    override fun hasInsertAccess(user: PFUserDO): Boolean = false
}

@Service
class CvrAreaDao : CvrInventoryBaseDao<CvrAreaDO>(CvrAreaDO::class.java) {
    override fun newInstance(): CvrAreaDO = CvrAreaDO()
}

@Service
class CvrSystemDao : CvrInventoryBaseDao<CvrSystemDO>(CvrSystemDO::class.java) {
    override fun newInstance(): CvrSystemDO = CvrSystemDO()
}

@Service
class CvrInstanceDao : CvrInventoryBaseDao<CvrInstanceDO>(CvrInstanceDO::class.java) {
    override fun newInstance(): CvrInstanceDO = CvrInstanceDO()
}

@Service
class CvrComponentDao : CvrInventoryBaseDao<CvrComponentDO>(CvrComponentDO::class.java) {
    override fun newInstance(): CvrComponentDO = CvrComponentDO()
}

@Service
class CvrTechnologyDao : CvrInventoryBaseDao<CvrTechnologyDO>(CvrTechnologyDO::class.java) {
    override fun newInstance(): CvrTechnologyDO = CvrTechnologyDO()
}

/** The catalogue is read and changed through [CvrCatalogService] only, which checks the security team. */
@Service
class CvrCatalogNodeDao : CvrInventoryBaseDao<CvrCatalogNodeDO>(CvrCatalogNodeDO::class.java) {
    override fun newInstance(): CvrCatalogNodeDO = CvrCatalogNodeDO()
}

@Service
class CvrCatalogEdgeDao : CvrInventoryBaseDao<CvrCatalogEdgeDO>(CvrCatalogEdgeDO::class.java) {
    override fun newInstance(): CvrCatalogEdgeDO = CvrCatalogEdgeDO()
}
