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

package org.projectforge.business.humanresources

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.*
import org.hibernate.search.bridge.builtin.IntegerBridge
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektFormatter
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.Priority
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.ObjectHelper
import java.math.BigDecimal
import javax.persistence.*

/**
 * @author Mario Groß (m.gross@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_HR_PLANNING_ENTRY", indexes = [javax.persistence.Index(name = "idx_fk_t_hr_planning_entry_planning_fk", columnList = "planning_fk"), javax.persistence.Index(name = "idx_fk_t_hr_planning_entry_projekt_fk", columnList = "projekt_fk"), javax.persistence.Index(name = "idx_fk_t_hr_planning_entry_tenant_id", columnList = "tenant_id")])
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class HRPlanningEntryDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @IndexedEmbedded(depth = 3)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "planning_fk", nullable = false)
    open var planning: HRPlanningDO? = null

    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "projekt_fk")
    open var projekt: ProjektDO? = null

    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "status", length = 20)
    open var status: HRPlanningEntryStatus? = null

    @PropertyInfo(i18nKey = "hr.planning.priority")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var priority: Priority? = null

    @PropertyInfo(i18nKey = "hr.planning.probability.short")
    @Field(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column
    open var probability: Int? = null

    /**
     * Ohne Wochentagszuordnung.
     */
    /**
     * @return Hours without assigned day of week (unspecified). This means, it doesn't matter on which day of week the
     * job will be done.
     */
    @PropertyInfo(i18nKey = "hr.planning.unassignedHours")
    @get:Column(scale = 2, precision = 5)
    open var unassignedHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "calendar.shortday.monday")
    @get:Column(scale = 2, precision = 5)
    open var mondayHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "calendar.shortday.tuesday")
    @get:Column(scale = 2, precision = 5)
    open var tuesdayHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "calendar.shortday.wednesday")
    @get:Column(scale = 2, precision = 5)
    open var wednesdayHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "calendar.shortday.thursday")
    @get:Column(scale = 2, precision = 5)
    open var thursdayHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "calendar.shortday.friday")
    @get:Column(scale = 2, precision = 5)
    open var fridayHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "hr.planning.weekend")
    @get:Column(scale = 2, precision = 5)
    open var weekendHours: BigDecimal? = null

    @PropertyInfo(i18nKey = "hr.planning.description")
    @Field
    @get:Column(length = 4000)
    open var description: String? = null

    val planningId: Int?
        @Transient
        get() = if (this.planning == null) {
            null
        } else this.planning!!.id

    val shortDescription: String?
        @Transient
        get() = StringUtils.abbreviate(description, 50)

    val projektId: Int?
        @Transient
        get() = if (this.projekt == null) {
            null
        } else this.projekt!!.id

    val projektName: String?
        @Transient
        get() = if (this.projekt == null) {
            ""
        } else this.projekt!!.name

    val projektNameOrStatus: String?
        @Transient
        get() = if (this.status != null) {
            ThreadLocalUserContext.getLocalizedString(status!!.i18nKey)
        } else {
            projektName
        }

    /**
     * Gets the customer of the project.
     *
     * @see ProjektFormatter.formatProjektKundeAsString
     */
    val projektKundeAsString: String
        @Transient
        get() = ProjektFormatter.formatProjektKundeAsString(this.projekt, null, null)

    /**
     * @return The total duration of all assigned hours (unassigned hours, monday, tuesday...)
     */
    val totalHours: BigDecimal
        @Transient
        get() {
            var duration = BigDecimal.ZERO
            if (this.unassignedHours != null) {
                duration = duration.add(this.unassignedHours!!)
            }
            if (this.mondayHours != null) {
                duration = duration.add(this.mondayHours!!)
            }
            if (this.tuesdayHours != null) {
                duration = duration.add(this.tuesdayHours!!)
            }
            if (this.wednesdayHours != null) {
                duration = duration.add(this.wednesdayHours!!)
            }
            if (this.thursdayHours != null) {
                duration = duration.add(this.thursdayHours!!)
            }
            if (this.fridayHours != null) {
                duration = duration.add(this.fridayHours!!)
            }
            if (this.weekendHours != null) {
                duration = duration.add(this.weekendHours!!)
            }
            return duration
        }

    val isEmpty: Boolean
        @Transient
        get() = ObjectHelper.isEmpty(this.description, this.mondayHours, this.tuesdayHours, this.wednesdayHours,
                this.thursdayHours,
                this.fridayHours, this.weekendHours, this.priority, this.probability, this.projekt)

    override fun equals(other: Any?): Boolean {
        if (other is HRPlanningEntryDO) {
            val o = other as HRPlanningEntryDO?
            return if (this.id != null || o!!.id != null) {
                this.id == o!!.id
            } else {
                hasNoFieldChanges(o)
            }
        }
        return false
    }

    fun hasNoFieldChanges(other: HRPlanningEntryDO): Boolean {
        if (this.status != null && other.status == null || this.status == null && other.status != null) {
            return false
        }
        if (this.status != null && other.status != null) {
            if (this.status != other.status) {
                return false
            }
        }
        if (this.projektId != null && other.projektId == null || this.projektId == null && other.projektId != null) {
            return false
        }
        if (this.projektId != null && other.projektId != null) {
            if (this.projektId != other.projektId) {
                return false
            }
        }
        if (this.unassignedHours != null && other.unassignedHours == null || this.unassignedHours == null && other.unassignedHours != null) {
            return false
        }
        if (this.unassignedHours != null && other.unassignedHours != null) {
            if (this.unassignedHours!!.compareTo(other.unassignedHours!!) != 0) {
                return false
            }
        }
        if (this.mondayHours != null && other.mondayHours == null || this.mondayHours == null && other.mondayHours != null) {
            return false
        }
        if (this.mondayHours != null && other.mondayHours != null) {
            if (this.mondayHours!!.compareTo(other.mondayHours!!) != 0) {
                return false
            }
        }
        if (this.tuesdayHours != null && other.tuesdayHours == null || this.tuesdayHours == null && other.tuesdayHours != null) {
            return false
        }
        if (this.tuesdayHours != null && other.tuesdayHours != null) {
            if (this.tuesdayHours!!.compareTo(other.tuesdayHours!!) != 0) {
                return false
            }
        }
        if (this.wednesdayHours != null && other.wednesdayHours == null || this.wednesdayHours == null && other.wednesdayHours != null) {
            return false
        }
        if (this.wednesdayHours != null && other.wednesdayHours != null) {
            if (this.wednesdayHours!!.compareTo(other.wednesdayHours!!) != 0) {
                return false
            }
        }
        if (this.thursdayHours != null && other.thursdayHours == null || this.thursdayHours == null && other.thursdayHours != null) {
            return false
        }
        if (this.thursdayHours != null && other.thursdayHours != null) {
            if (this.thursdayHours!!.compareTo(other.thursdayHours!!) != 0) {
                return false
            }
        }
        if (this.fridayHours != null && other.fridayHours == null || this.fridayHours == null && other.fridayHours != null) {
            return false
        }
        if (this.fridayHours != null && other.fridayHours != null) {
            if (this.fridayHours!!.compareTo(other.fridayHours!!) != 0) {
                return false
            }
        }
        if (this.weekendHours != null && other.weekendHours == null || this.weekendHours == null && other.weekendHours != null) {
            return false
        }
        if (this.weekendHours != null && other.weekendHours != null) {
            if (this.weekendHours!!.compareTo(other.weekendHours!!) != 0) {
                return false
            }
        }
        return this.isDeleted == other.isDeleted
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        if (id != null) {
            hcb.append(id)
        } else {
            if (planningId != null) {
                hcb.append(planningId!!)
            }
            if (projektId != null) {
                hcb.append(projektId!!)
            }
            if (status != null) {
                hcb.append(status)
            }
        }
        return hcb.toHashCode()
    }

    @Transient
    override fun getShortDisplayName(): String? {
        return if (projekt != null) projekt!!.name else ""
    }

    /**
     * Clones this entry (without id's).
     *
     * @return
     */
    fun newClone(): HRPlanningEntryDO {
        val entry = HRPlanningEntryDO()
        entry.copyValuesFrom(this, "id")
        return entry
    }
}
