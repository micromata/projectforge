package org.projectforge.business.fibu

import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.business.teamcal.admin.model.HibernateSearchUsersGroupsBridge

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchProjektTypeBinder : TypeBinder {
    override fun bind(context: TypeBindingContext) {
        context.dependencies().useRootOnly()

        val bridge: TypeBridge<ProjektDO> = HibernateSearchProjectKostBridge()
        context.bridge(ProjektDO::class.java, bridge)
    }
}
