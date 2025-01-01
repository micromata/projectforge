/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.hibernate.query.sqm.tree.SqmNode.log
import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext
import java.util.function.Consumer

/**
 * TaskPathBridge for hibernate search to search in the parent task titles.
 * https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#_classbridge
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchTaskPathBridge : TypeBridge<TaskDO> {
    override fun write(target: DocumentElement, bridgedElement: TaskDO, context: TypeBridgeWriteContext) {
        val taskNode = TaskTree.instance.getTaskNodeById(bridgedElement.id)
        if (taskNode == null) {
            target.addValue("taskpath", "")
            return
        }
        val list = taskNode.pathToRoot
        val sb = StringBuilder()
        sb.append(bridgedElement.id).append(": ") // Adding the id for deserialization
        list.forEach(Consumer { node: TaskNode ->
            sb.append(node.getTask().title).append("|")
        })
        if (log.isDebugEnabled) {
            log.debug(sb.toString())
        }
        target.addValue("taskpath", sb.toString())
    }
}
