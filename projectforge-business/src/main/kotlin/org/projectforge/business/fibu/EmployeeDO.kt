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

package org.projectforge.business.fibu

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.Constants
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.anots.StringAlphanumericSort
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Repräsentiert einen Mitarbeiter. Ein Mitarbeiter ist einem ProjectForge-Benutzer zugeordnet und enthält
 * buchhalterische Angaben.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
//@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider::class, param = "employee")
@Table(
    name = "t_fibu_employee",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id"])],
    indexes = [jakarta.persistence.Index(
        name = "idx_fk_t_fibu_employee_kost1_id",
        columnList = "kost1_id"
    ), jakarta.persistence.Index(name = "idx_fk_t_fibu_employee_user_id", columnList = "user_id")]
)
@AUserRightId("HR_EMPLOYEE")
@NamedQueries(
    NamedQuery(name = EmployeeDO.FIND_BY_USER_ID, query = "from EmployeeDO where user.id=:userId"),
    NamedQuery(name = EmployeeDO.GET_EMPLOYEE_ID_BY_USER_ID, query = "select id from EmployeeDO where user.id=:userId"),
    NamedQuery(
        name = EmployeeDO.FIND_BY_LASTNAME_AND_FIRST_NAME,
        query = "from EmployeeDO where user.lastname=:lastname and user.firstname=:firstname"
    )
)
open class EmployeeDO : DefaultBaseDO(), Comparable<Any>, DisplayNameCapable {
    // The class must be declared as open for mocking in VacationServiceTest.

    override val displayName: String
        @Transient
        get() = "${user?.getFullname()}"

    /**
     * The ProjectForge user assigned to this employee.
     */
    @PropertyInfo(i18nKey = "fibu.employee.user")
    @IndexedEmbedded(
        includeDepth = 1,
        includePaths = ["username", "firstname", "lastname", "description", "organization"]
    )
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "user_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    /**
     * Dem Benutzer zugeordneter Kostenträger Kost1 für den Monatsreport.
     */
    @PropertyInfo(i18nKey = "fibu.kost1")
    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "kost1_id", nullable = true)
    open var kost1: Kost1DO? = null

    /**
     * Field will be set by EmployeeDao automatically from validity period attr.
     * Field for convenience only. It's not persisted.
     */
    @PropertyInfo(i18nKey = "fibu.employee.status")
    @get:Transient
    open var status: EmployeeStatus? = null
        internal set

    /**
     * Field will be set by EmployeeDao automatically from validity period attr.
     * Field for convenience only. It's not persisted.
     */
    @PropertyInfo(i18nKey = "fibu.employee.urlaubstage")
    @get:Transient
    open var annualLeave: BigDecimal? = null
        internal set

    /**
     * Field will be set by EmployeeDao automatically from validity period attr.
     * Field for convenience only. It's not persisted.
     */
    @PropertyInfo(i18nKey = "fibu.employee.wochenstunden")
    @get:Transient
    open var weeklyWorkingHours: BigDecimal? = null
        internal set

    @PropertyInfo(i18nKey = "address.positionText")
    @FullTextField
    @get:Column(name = "position_text", length = 244)
    open var position: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.eintrittsdatum")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "eintritt")
    open var eintrittsDatum: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.employee.austrittsdatum")
    @GenericField
    @get:Column(name = "austritt")
    open var austrittsDatum: LocalDate? = null

    @PropertyInfo(i18nKey = "fibu.employee.division")
    @FullTextField
    @get:Column(length = 255)
    open var abteilung: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.staffNumber")
    @FullTextField
    @StringAlphanumericSort
    @get:Column(length = 255)
    open var staffNumber: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.COMMENT_LENGTH)
    open var comment: String? = null

    /**
     * @return True, if employee isn't deleted and eintritt/austritt date isn't given or not before/after now.
     */
    val active: Boolean
        @Transient
        get() {
            if (deleted == true) {
                return false
            }
            val now = LocalDate.now()
            eintrittsDatum?.let { eintritt ->
                if (eintritt.isAfter(now)) {
                    return false
                }
            }
            austrittsDatum?.let { austritt ->
                if (austritt.isBefore(now)) {
                    return false
                }
            }
            return true
        }

    override fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): EntityCopyStatus {
        val modificationStatus = super.copyValuesFrom(src, "timeableAttributes")
        // val src = source as EmployeeDO
        log.warn("*** To be implemented: EmployeeDO.copyValuesFrom for timeableAttributes")
        return modificationStatus
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EmployeeDO)
            return false
        if (other.id == null) {
            return false
        }
        return if (this.id == other.id) {
            true
        } else super.equals(other)
    }

    override fun hashCode(): Int {
        return if (id != null) 31 * id.hashCode() else super.hashCode()
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
        internal const val GET_EMPLOYEE_ID_BY_USER_ID = "EmployeeDO_GetEmployeeIdByUserId"
        internal const val FIND_BY_LASTNAME_AND_FIRST_NAME = "EmployeeDO_FindByLastnameAndFirstname"
    }
}
