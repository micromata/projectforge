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

package org.projectforge.business.fibu

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import de.micromata.genome.db.jpa.history.api.HistoryProperty
import de.micromata.genome.db.jpa.history.impl.TabAttrHistoryPropertyConverter
import de.micromata.genome.db.jpa.history.impl.TimependingHistoryPropertyConverter
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO
import de.micromata.genome.jpa.ComplexEntity
import de.micromata.genome.jpa.ComplexEntityVisitor
import de.micromata.mgc.jpa.hibernatesearch.api.HibernateSearchInfo
import de.micromata.mgc.jpa.hibernatesearch.bridges.TimeableListFieldBridge
import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.search.annotations.*
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.anots.StringAlphanumericSort
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO
import org.projectforge.framework.persistence.attr.impl.HibernateSearchAttrSchemaFieldInfoProvider
import org.projectforge.framework.persistence.history.ToStringFieldBridge
import org.projectforge.framework.persistence.jpa.impl.BaseDaoJpaAdapter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.Constants
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

/**
 * Repräsentiert einen Mitarbeiter. Ein Mitarbeiter ist einem ProjectForge-Benutzer zugeordnet und enthält
 * buchhalterische Angaben.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider::class, param = "employee")
@Table(name = "t_fibu_employee", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_employee_kost1_id", columnList = "kost1_id"), javax.persistence.Index(name = "idx_fk_t_fibu_employee_user_id", columnList = "user_id"), javax.persistence.Index(name = "idx_fk_t_fibu_employee_tenant_id", columnList = "tenant_id")])
@AUserRightId("HR_EMPLOYEE")
@NamedQueries(
        NamedQuery(name = EmployeeDO.FIND_BY_USER_ID, query = "from EmployeeDO where user.id=:userId and tenant.id=:tenantId"),
        NamedQuery(name = EmployeeDO.FIND_BY_LASTNAME_AND_FIRST_NAME, query = "from EmployeeDO where user.lastname=:lastname and user.firstname=:firstname"))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class EmployeeDO : DefaultBaseWithAttrDO<EmployeeDO>(), EntityWithTimeableAttr<Int, EmployeeTimedDO>, ComplexEntity, EntityWithConfigurableAttr, Comparable<Any> {
    // The class must be declared as open for mocking in VacationServiceTest.

    private val LOG = LoggerFactory.getLogger(EmployeeDO::class.java)

    /**
     * The ProjectForge user assigned to this employee.
     */
    @PropertyInfo(i18nKey = "fibu.employee.user")
    @IndexedEmbedded(depth = 1, includePaths = ["firstname", "lastname", "description", "organization"])
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "user_id", nullable = false)
    open var user: PFUserDO? = null

    /**
     * Dem Benutzer zugeordneter Kostenträger Kost1 für den Monatsreport.
     */
    @PropertyInfo(i18nKey = "fibu.kost1")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "kost1_id", nullable = true)
    open var kost1: Kost1DO? = null

    @Deprecated("Don't use the status field anymore, this is replaced by the status within the internalattrschema.xml")
    @PropertyInfo(i18nKey = "status")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "employee_status", length = 30)
    open var status: EmployeeStatus? = null

    @PropertyInfo(i18nKey = "address.positionText")
    @Field
    @get:Column(name = "position_text", length = 244)
    open var position: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.eintrittsdatum")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "eintritt")
    open var eintrittsDatum: Date? = null

    @PropertyInfo(i18nKey = "fibu.employee.austrittsdatum")
    @Field
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "austritt")
    open var austrittsDatum: Date? = null

    @PropertyInfo(i18nKey = "fibu.employee.division")
    @Field
    @get:Column(length = 255)
    open var abteilung: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.staffNumber")
    @Field
    @StringAlphanumericSort
    @get:Column(length = 255)
    open var staffNumber: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.urlaubstage")
    @Field(analyze = Analyze.NO)
    @get:Column
    open var urlaubstage: Int? = null // Open needed for mocking in VacationServiceTest

    @Field(store = Store.YES)
    @FieldBridge(impl = TimeableListFieldBridge::class)
    @IndexedEmbedded(depth = 2)
    private var timeableAttributes: MutableList<EmployeeTimedDO> = ArrayList()

    @PropertyInfo(i18nKey = "fibu.employee.wochenstunden")
    @Field(analyze = Analyze.NO)
    @FieldBridge(impl = ToStringFieldBridge::class)
    @get:Column(name = "weekly_working_hours", scale = 5, precision = 10)
    open var weeklyWorkingHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.employee.birthday")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column
    open var birthday: Date? = null

    @PropertyInfo(i18nKey = "fibu.employee.accountHolder")
    @Field
    @get:Column(length = 255, name = "account_holder")
    open var accountHolder: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.iban")
    @Field
    @get:Column(length = 50)
    open var iban: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.bic")
    @Field
    @get:Column(length = 11)
    open var bic: String? = null

    @PropertyInfo(i18nKey = "gender")
    @Field
    @Convert(converter = GenderConverter::class)
    @get:Column
    // use the GenderConverter instead of @Enumerated to persist the correct ISO/IEC 5218 integer representation of the gender
    open var gender: Gender? = null

    @PropertyInfo(i18nKey = "fibu.employee.street")
    @Field
    @get:Column(length = 255)
    open var street: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.zipCode")
    @Field
    @get:Column(length = 255)
    open var zipCode: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.city")
    @Field
    @get:Column(length = 255)
    open var city: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.country")
    @Field
    @get:Column(length = 255)
    open var country: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.state")
    @Field
    @get:Column(length = 255)
    open var state: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = Constants.COMMENT_LENGTH)
    open var comment: String? = null

    val kost1Id: Int?
        @Transient
        get() = kost1?.id

    val userId: Int?
        @Transient
        get() = user?.id

    override fun copyValuesFrom(source: BaseDO<out Serializable>, vararg ignoreFields: String): ModificationStatus {
        var modificationStatus = super.copyValuesFrom(source, "timeableAttributes")
        val src = source as EmployeeDO
        modificationStatus = modificationStatus
                .combine(BaseDaoJpaAdapter.copyTimeableAttribute(this, src))
        return modificationStatus
    }

    override fun visit(visitor: ComplexEntityVisitor) {
        super.visit(visitor)
        for (et in timeableAttributes) {
            et.visit(visitor)
        }
    }

    @Transient
    override fun getAttrSchemaName(): String {
        return "employee"
    }

    override fun addTimeableAttribute(row: EmployeeTimedDO) {
        row.employee = this
        timeableAttributes.add(row)
    }

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "employee")
    // fetch mode, only returns one
    @Fetch(FetchMode.SELECT)
    // unfortunatelly this does work. Date is not valid for order (only integral types)
    //  @OrderColumn(name = "startTime")
    @HistoryProperty(converter = TimependingHistoryPropertyConverter::class)
    override fun getTimeableAttributes(): List<EmployeeTimedDO> {
        return timeableAttributes
    }

    open fun setTimeableAttributes(timeableAttributes: MutableList<EmployeeTimedDO>) {
        this.timeableAttributes = timeableAttributes
    }

    @Transient
    override fun getAttrEntityClass(): Class<out JpaTabAttrBaseDO<EmployeeDO, Int>> {
        return EmployeeAttrDO::class.java
    }

    @Transient
    override fun getAttrEntityWithDataClass(): Class<out JpaTabAttrBaseDO<EmployeeDO, Int>> {
        return EmployeeAttrWithDataDO::class.java
    }

    @Transient
    override fun getAttrDataEntityClass(): Class<out JpaTabAttrDataBaseDO<out JpaTabAttrBaseDO<EmployeeDO, Int>, Int>> {
        return EmployeeAttrDataDO::class.java
    }

    override fun createAttrEntity(key: String, type: Char, value: String): JpaTabAttrBaseDO<EmployeeDO, Int> {
        return EmployeeAttrDO(this, key, type, value)
    }

    override fun createAttrEntityWithData(key: String, type: Char, value: String): JpaTabAttrBaseDO<EmployeeDO, Int> {
        return EmployeeAttrWithDataDO(this, key, type, value)
    }

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "parent", targetEntity = EmployeeAttrDO::class, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "propertyName")
    @HistoryProperty(converter = TabAttrHistoryPropertyConverter::class)
    override fun getAttrs(): Map<String, JpaTabAttrBaseDO<EmployeeDO, Int>> {
        return super.getAttrs()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EmployeeDO)
            return false
        if (other.pk == null) {
            return false
        }
        return if (this.pk == other.pk) {
            true
        } else super.equals(other)
    }

    override fun hashCode(): Int {
        return if (pk != null) 31 * pk.hashCode() else super.hashCode()
    }

    override operator fun compareTo(other: Any): Int {
        if (other !is EmployeeDO) {
            return 0
        }
        if (this.user == other.user) return 0
        val u1 = this.user
        val u2 = other.user
        if (u1 == null) return -1
        if (u2 == null) return 1
        var result = StringUtils.compare(u1.lastname, u2.lastname)
        if (result == 0) {
            result = StringUtils.compare(u1.firstname, u2.firstname)
        }
        return result
    }

    companion object {
        internal const val FIND_BY_USER_ID = "EmployeeDO_FindByUserId"
        internal const val FIND_BY_LASTNAME_AND_FIRST_NAME = "EmployeeDO_FindByLastnameAndFirstname"
    }
}
