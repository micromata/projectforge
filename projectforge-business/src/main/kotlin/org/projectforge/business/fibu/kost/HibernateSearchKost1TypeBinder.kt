package org.projectforge.business.fibu.kost

import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchKost1TypeBinder : TypeBinder {
    override fun bind(context: TypeBindingContext) {
        context.dependencies().useRootOnly()

        val bridge: TypeBridge<Kost1DO> = HibernateSearchKost1Bridge()
        context.bridge(Kost1DO::class.java, bridge)
    }
}
