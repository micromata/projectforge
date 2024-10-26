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

package org.projectforge.business.orga

import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.io.Serializable

private val log = KotlinLogging.logger {}

@Entity
@Indexed
//@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider::class, param = "visitorbook")
@Table(name = "t_orga_visitorbook")
@AUserRightId("ORGA_VISITORBOOK")
class VisitorbookDO : DefaultBaseDO() {

    @FullTextField
    @PropertyInfo(i18nKey = "orga.visitorbook.lastname")
    @get:Column(name = "lastname", length = 30, nullable = false)
    var lastname: String? = null

    @FullTextField
    @PropertyInfo(i18nKey = "orga.visitorbook.firstname")
    @get:Column(name = "firstname", length = 30, nullable = false)
    var firstname: String? = null

    @FullTextField
    @PropertyInfo(i18nKey = "orga.visitorbook.company")
    @get:Column(name = "company")
    var company: String? = null

    @PropertyInfo(i18nKey = "orga.visitorbook.contactPerson")
    @IndexedEmbedded(includeDepth = 2, includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToMany(targetEntity = EmployeeDO::class, cascade = [CascadeType.MERGE], fetch = FetchType.LAZY)
    @get:JoinTable(
        name = "T_ORGA_VISITORBOOK_EMPLOYEE",
        joinColumns = [JoinColumn(name = "VISITORBOOK_ID")],
        inverseJoinColumns = [JoinColumn(name = "EMPLOYEE_ID")],
        indexes = [jakarta.persistence.Index(
            name = "idx_fk_t_orga_visitorbook_employee_id",
            columnList = "visitorbook_id"
        ), jakarta.persistence.Index(name = "idx_fk_t_orga_employee_employee_id", columnList = "employee_id")]
    )
    var contactPersons: Set<EmployeeDO>? = null
        get() {
            if (field == null) {
                this.contactPersons = HashSet()
            }
            return field
        }

    @PropertyInfo(i18nKey = "orga.visitorbook.visitortype")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "visitor_type", nullable = false)
    var visitortype: VisitorType? = null

    /*
    @FullTextField(store = Store.YES)
    //@FieldBridge(impl = TimeableListFieldBridge::class)
    @IndexedEmbedded(depth = 2)
    private var timeableAttributes: MutableList<VisitorbookTimedDO> = ArrayList()
*/
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "visitorbook", targetEntity = VisitorbookEntryDO::class)
    // @HistoryProperty(converter = TimependingHistoryPropertyConverter::class)
    var entries: MutableList<VisitorbookEntryDO>? = null

    override fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): EntityCopyStatus {
        var modificationStatus = super.copyValuesFrom(src, "timeableAttributes")
        //wval src = obj as VisitorbookDO
        log.error("Not yet implemented: copyValuesFrom")
        // modificationStatus = modificationStatus
        //    .combine(BaseDaoJpaAdapter.copyTimeableAttribute(this, src))
        return modificationStatus
    }

    fun addEntry(entry: VisitorbookEntryDO) {
        entries?.add(entry) ?: run {
            entries = mutableListOf(entry)
        }
    }
}
