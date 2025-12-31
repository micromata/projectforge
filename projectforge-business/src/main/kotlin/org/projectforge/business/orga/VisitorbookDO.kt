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

package org.projectforge.business.orga

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.Constants
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdsOnlySerializer
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import java.io.Serializable

private val log = KotlinLogging.logger {}

@Entity
@Indexed
//@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider::class, param = "visitorbook")
@Table(name = "t_orga_visitorbook")
@NamedEntityGraph(
    name = VisitorbookDO.ENTITY_GRAPH_WITH_CONTACT_EMPLOYEES,
    attributeNodes = [NamedAttributeNode(value = "contactPersons", subgraph = "contactPersonIds")],
    subgraphs = [NamedSubgraph(
        name = "contactPersonIds",
        attributeNodes = [NamedAttributeNode(value = "id")]
    )]
)
@AUserRightId("ORGA_VISITORBOOK")
open class VisitorbookDO : DefaultBaseDO() {

    @FullTextField
    @PropertyInfo(i18nKey = "orga.visitorbook.lastname")
    @get:Column(name = "lastname", length = 30, nullable = false)
    open var lastname: String? = null

    @FullTextField
    @PropertyInfo(i18nKey = "orga.visitorbook.firstname")
    @get:Column(name = "firstname", length = 30, nullable = false)
    open var firstname: String? = null

    @FullTextField
    @PropertyInfo(i18nKey = "orga.visitorbook.company")
    @get:Column(name = "company")
    open var company: String? = null

    @PropertyInfo(i18nKey = "orga.visitorbook.contactPersons")
    @IndexedEmbedded(includeDepth = 2, includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToMany(fetch = FetchType.LAZY)
    @get:JoinTable(
        name = "T_ORGA_VISITORBOOK_EMPLOYEE",
        joinColumns = [JoinColumn(name = "VISITORBOOK_ID", referencedColumnName = "PK")],
        inverseJoinColumns = [JoinColumn(name = "EMPLOYEE_ID", referencedColumnName = "PK")],
        indexes = [jakarta.persistence.Index(
            name = "idx_fk_t_orga_visitorbook_employee_id",
            columnList = "visitorbook_id"
        ), jakarta.persistence.Index(name = "idx_fk_t_orga_employee_employee_id", columnList = "employee_id")]
    )
    @JsonSerialize(using = IdsOnlySerializer::class)
    open var contactPersons: Set<EmployeeDO>? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(name = "comment", length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "orga.visitorbook.visitortype")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "visitor_type", nullable = false)
    open var visitortype: VisitorType? = null

    @get:OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        fetch = FetchType.LAZY, orphanRemoval = false,
        mappedBy = "visitorbook", targetEntity = VisitorbookEntryDO::class
    )
    @NoHistory
    // @HistoryProperty(converter = TimependingHistoryPropertyConverter::class)
    @JsonSerialize(using = IdsOnlySerializer::class)
    open var entries: MutableList<VisitorbookEntryDO>? = null

    fun addEntry(entry: VisitorbookEntryDO) {
        entries?.add(entry) ?: run {
            entries = mutableListOf(entry)
        }
    }

    companion object {
        const val ENTITY_GRAPH_WITH_CONTACT_EMPLOYEES = "VisitorbookDO.contactEmployees"
    }
}
