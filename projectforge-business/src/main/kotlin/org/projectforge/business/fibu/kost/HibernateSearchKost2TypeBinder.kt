package org.projectforge.business.fibu.kost

import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchKost2TypeBinder : TypeBinder {
    override fun bind(context: TypeBindingContext) {
        context.dependencies().useRootOnly()

        val bridge: TypeBridge<Kost2DO> = HibernateSearchKost2Bridge()
        context.bridge(Kost2DO::class.java, bridge)
    }
}
