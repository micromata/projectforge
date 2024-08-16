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

package org.projectforge.framework.persistence.history

import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.framework.persistence.history.entities.PfHistoryAttrDO
import org.projectforge.framework.persistence.history.entities.PfHistoryAttrDataDO
import org.projectforge.framework.persistence.history.entities.PfHistoryAttrWithDataDO
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Stores history.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Table(
    name = "t_pf_history",
    indexes = [Index(
        name = "ix_pf_history_ent",
        columnList = "ENTITY_ID,ENTITY_NAME"
    ), Index(name = "ix_pf_history_mod", columnList = "MODIFIEDAT")]
)
@Indexed
//@ClassBridge(impl = HistoryMasterClassBridge::class)
//@HibernateSearchInfo(param = "oldValue")
//@JpaXmlPersist(beforePersistListener = PfHistoryMasterXmlBeforePersistListener::class)
class PfHistoryMasterDO : HistoryEntry<Long> {
    @get:GeneratedValue
    @get:Column(name = "pk")
    @get:Id
    override var id: Long? = null

    @get:Transient
    @get:GenericField // was @Field(analyze = Analyze.NO, store = Store.NO)
    override var entityName: String? = null

    @get:Transient
    @get:GenericField // was @get:Field(analyze = Analyze.NO, store = Store.YES, index = Indexed.YES)
    override var entityId: Long? = null

    @get:Transient
    @get:GenericField // was: @get:Field(analyze = Analyze.NO, store = Store.NO, index = Indexed.YES)
    override var modifiedBy: String? = null

    @get:Transient
    @get:GenericField
    override val modifiedAt: Date? = null

    override var entityOpType: EntityOpType? = null

    @get:Transient
    @get:FullTextField
    var oldValue: String? = null // was @get:Field(analyze = Analyze.YES, store = Store.NO, index = Indexed.YES)

    @get:MapKey(name = "propertyName")
    @get:OneToMany(
        cascade = [CascadeType.ALL],
        mappedBy = "parent",
        targetEntity = PfHistoryAttrDO::class,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    var attributes: MutableMap<String, Any?>? = null

    @get:Transient
    val attrEntityClass: Class<out Any?>
        get() = PfHistoryAttrDO::class.java

    @get:Transient
    val attrEntityWithDataClass: Class<out Any?>
        get() = PfHistoryAttrWithDataDO::class.java

    @get:Transient
    val attrDataEntityClass: Class<out Any?>
        get() = PfHistoryAttrDataDO::class.java


    fun createAttrEntity(key: String?, type: Char, value: String?): PfHistoryAttrDO {
        return PfHistoryAttrDO(this, key, type, value)
    }

    fun createAttrEntityWithData(
        key: String?,
        type: Char,
        value: String?
    ): PfHistoryAttrWithDataDO {
        return PfHistoryAttrWithDataDO(this, key, type, value)
    }

    @get:Transient
    override val diffEntries: List<DiffEntry>?
        get() {
            log.entry("******* diffEntries not yet implemented")
            return null
        }
}
