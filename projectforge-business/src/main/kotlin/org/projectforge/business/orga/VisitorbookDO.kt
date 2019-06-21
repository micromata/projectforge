/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import java.io.Serializable
import java.util.ArrayList
import java.util.HashSet

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Transient

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Index
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.hibernate.search.annotations.Store
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.attr.impl.HibernateSearchAttrSchemaFieldInfoProvider
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.micromata.genome.db.jpa.history.api.HistoryProperty
import de.micromata.genome.db.jpa.history.impl.TimependingHistoryPropertyConverter
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr
import de.micromata.genome.jpa.ComplexEntity
import de.micromata.genome.jpa.ComplexEntityVisitor
import de.micromata.mgc.jpa.hibernatesearch.api.HibernateSearchInfo
import de.micromata.mgc.jpa.hibernatesearch.bridges.TimeableListFieldBridge

@Entity
@Indexed
@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider::class, param = "visitorbook")
@Table(name = "t_orga_visitorbook", indexes = [javax.persistence.Index(name = "idx_fk_t_orga_visitorbook_tenant_id", columnList = "tenant_id")])
@AUserRightId("ORGA_VISITORBOOK")
class VisitorbookDO : DefaultBaseDO(), EntityWithTimeableAttr<Int, VisitorbookTimedDO>, ComplexEntity, EntityWithConfigurableAttr {

    @Field(index = Index.YES, store = Store.NO)
/* TOKENIZED */
    @PropertyInfo(i18nKey = "orga.visitorbook.lastname")
    @get:Column(name = "lastname", length = 30, nullable = false)
    var lastname: String? = null

    @Field(index = Index.YES, store = Store.NO)
/* TOKENIZED */
    @PropertyInfo(i18nKey = "orga.visitorbook.firstname")
    @get:Column(name = "firstname", length = 30, nullable = false)
    var firstname: String? = null

    @Field(index = Index.YES, store = Store.NO)
/* TOKENIZED */
    @PropertyInfo(i18nKey = "orga.visitorbook.company")
    @get:Column(name = "company")
    var company: String? = null

    @PropertyInfo(i18nKey = "orga.visitorbook.contactPerson")
    @IndexedEmbedded(depth = 2, includePaths = ["user.firstname", "user.lastname"])
    var contactPersons: Set<EmployeeDO>? = null
        @ManyToMany(targetEntity = EmployeeDO::class, cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
        @JoinTable(name = "T_ORGA_VISITORBOOK_EMPLOYEE", joinColumns = [JoinColumn(name = "VISITORBOOK_ID")], inverseJoinColumns = [JoinColumn(name = "EMPLOYEE_ID")], indexes = [javax.persistence.Index(name = "idx_fk_t_orga_visitorbook_employee_id", columnList = "visitorbook_id"), javax.persistence.Index(name = "idx_fk_t_orga_employee_employee_id", columnList = "employee_id")])
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

    @Field(store = Store.YES)
    @FieldBridge(impl = TimeableListFieldBridge::class)
    @IndexedEmbedded(depth = 2)
    private var timeableAttributes: MutableList<VisitorbookTimedDO> = ArrayList()

    override fun copyValuesFrom(source: BaseDO<out Serializable>, vararg ignoreFields: String): ModificationStatus {
        var modificationStatus = super.copyValuesFrom(source, "timeableAttributes")
        val src = source as VisitorbookDO
        modificationStatus = modificationStatus
                .combine(BaseDaoJpaAdapter.copyTimeableAttribute(this, src))
        return modificationStatus
    }

    override fun visit(visitor: ComplexEntityVisitor) {
        for (et in timeableAttributes) {
            et.visit(visitor)
        }
    }

    @Transient
    override fun getAttrSchemaName(): String {
        return "visitorbook"
    }

    override fun addTimeableAttribute(row: VisitorbookTimedDO) {
        row.visitor = this
        timeableAttributes.add(row)
    }

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "visitor")
    @Fetch(FetchMode.SELECT)
    @HistoryProperty(converter = TimependingHistoryPropertyConverter::class)
    override fun getTimeableAttributes(): List<VisitorbookTimedDO> {
        return timeableAttributes
    }

    fun setTimeableAttributes(timeableAttributes: MutableList<VisitorbookTimedDO>) {
        this.timeableAttributes = timeableAttributes
    }

    companion object {
        private val serialVersionUID = -1208597049289694757L

        private val LOG = LoggerFactory.getLogger(VisitorbookDO::class.java)
    }

}
