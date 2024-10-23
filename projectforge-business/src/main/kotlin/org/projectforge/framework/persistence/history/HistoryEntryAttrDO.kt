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

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.framework.persistence.api.HibernateUtils
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

/**
 * Stores history attributes.
 *
 * Table t_pf_history_attr
 *  withdata            | character(1)                |           | not null | -- 0, 1 (only used by mgc), attr_data concatinated to attr.value.
 *  pk                  | bigint                      |           | not null |
 *  createdat           | timestamp without time zone |           | not null | -- equals to modifiedat and parent.modifiedat
 *  createdby           | character varying(60)       |           | not null | -- equals to modifiedby and parent.modifiedby
 *  modifiedat          | timestamp without time zone |           | not null | -- equals to createdat and parent.createdat
 *  modifiedby          | character varying(60)       |           | not null | -- equals to createdby and parent.createdby
 *  updatecounter       | integer                     |           | not null | -- always 0
 *  value               | character varying(3000)     |           |          |
 *  propertyname        | character varying(255)      |           |          |
 *  type                | character(1)                |           | not null | N, V (not needed, value is null if type == N, otherwise V)
 *  property_type_class | character varying(128)      |           |          |
 *  master_fk           | bigint                      |           | not null |
 *
 *  propertyname:
 *  - timeableAttributes.timeofvisit.2017-09-28 00:00:00:000.arrive:op
 *  - timeableAttributes.timeofvisit.2021-10-04 00:00:00:000.arrive:nv
 *  - timeableAttributes.timeofvisit.2022-09-20 00:00:00:000.startTime:op
 *  - attrs.previousyearleave:op
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Table(
    name = "t_pf_history_attr",
    //indexes = [Index(
    //    name = "ix_pf_history_ent",
    //    columnList = "ENTITY_ID,ENTITY_NAME"
    //), Index(name = "ix_pf_history_mod", columnList = "MODIFIEDAT")]
)
@Indexed
//@ClassBridge(impl = HistoryMasterClassBridge::class)
class HistoryEntryAttrDO : HistoryEntryAttr {
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    @get:Id
    override var id: Long? = null

    // TODO: rename master_fk -> parent_fk later. This is a legacy name. If we change it, we have to change the database schema and have no rollback for the upcoming major release 7.6.
    @JsonBackReference
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "master_fk", nullable = false)
    var parent: HistoryEntryDO? = null

    /**
     * The new value.
     */
    @get:Column(name = "value", length = 100000)
    override var value: String? = null

    /**
     * The new value.
     */
    @get:Column(name = "old_value", length = 100000)
    override var oldValue: String? = null

    /**
     * Insert, Update (new field after MGC migration). In MGC version it was one additional entry with property_type_class
     * de.micromata.genome.db.jpa.history.entities.PropertyOpType.
     */
    @get:Column(name = "optype", length = 32)
    @get:Enumerated(EnumType.STRING)
    override var opType: PropertyOpType? = null

    /**
     * With MGC:
     *   The property name with the suffix {:nv|:ov|:op}.
     *   - :nv: New value.
     *   - :ov: Old value.
     *   - :op: Operation, value is Insert or Update (property_type_class is de.micromata.genome.db.jpa.history.entities.PropertyOpType)
     *   For timeable attributes the start time of validity period is included:
     *   - timeableAttributes.timeofvisit.2023-07-12 00:00:00:000.depart:op
     *   - timeableAttributes.employeeannualleave.2021-11-15 00:00:00:000.employeeannualleavedays:op
     *   - timeableAttributes.employeeannualleave.2022-03-01 00:00:00:000.employeeannualleavedays:ov
     *
     * Without MGC: The property name.
     */
    @get:Column(name = "propertyname", length = 255)
    override var propertyName: String? = null

    @get:Column(name = "property_type_class", length = 128)
    override var propertyTypeClass: String? = null

    fun setPropertyTypeClass(clazz: Class<*>) {
        propertyTypeClass = HibernateUtils.getUnifiedClassname(clazz)
    }

    fun setPropertyTypeClass(kClass: KClass<*>) {
        propertyTypeClass = HibernateUtils.getUnifiedClassname(kClass.java)
    }

    /**
     * Serializes the old and new value to a string by using [HistoryValueHandlerRegistry].
     */
    fun serializeAndSet(oldValue: Any?, newValue: Any?) {
        val handler = HistoryValueHandlerRegistry.getHandler(propertyTypeClass)
        if (oldValue != null) {
            this.oldValue = serializeValue(handler, oldValue)
        }
        if (newValue != null) {
            this.value = serializeValue(handler, newValue)
        }
    }

    private fun serializeValue(handler: HistoryValueHandler<*>, value: Any?): String? {
        value ?: return null
        if (value is Collection<*>) {
            if (value.isEmpty()) {
                return ""
            }
            val serialized = value.filterNotNull().map { obj ->
                handler.serialize(obj) ?: "null"
            }
            return serialized.sorted().joinToString(",")
        }
        return handler.serialize(value)
    }

    companion object {
        /**
         * Creates a new HistoryEntryAttrDO. Referenced parent is set by [HistoryEntryDO.add].
         * The old and new value will be serialized to a string by using [HistoryValueHandlerRegistry].
         *
         * @param historyEntry The parent history entry, which is the parent of this attribute. [HistoryEntryDO.add] will be called.
         * @param oldValue The old value will be automatically serialized by [HistoryValueHandlerRegistry].
         * @param newValue The old value will be automatically serialized by [HistoryValueHandlerRegistry].
         */
        fun create(
            propertyTypeClass: Class<*>,
            propertyName: String?,
            opType: PropertyOpType,
            oldValue: Any? = null,
            newValue: Any? = null,
            historyEntry: HistoryEntryDO? = null,
        ): HistoryEntryAttrDO {
            val attr = HistoryEntryAttrDO()
            attr.setPropertyTypeClass(propertyTypeClass)
            attr.opType = opType
            attr.propertyName = propertyName
            attr.parent = historyEntry
            attr.serializeAndSet(oldValue = oldValue, newValue = newValue)
            historyEntry?.add(attr)
            return attr
        }

        fun create(
            property: KMutableProperty1<*, *>,
            propertyName: String?,
            opType: PropertyOpType,
            oldValue: Any? = null,
            newValue: Any? = null,
            historyEntry: HistoryEntryDO? = null,
        ): HistoryEntryAttrDO {
            val propertyTypeClass = (property.returnType.classifier as KClass<*>).java
            return create(
                propertyTypeClass = propertyTypeClass,
                propertyName = propertyName,
                opType = opType,
                oldValue = oldValue,
                newValue = newValue,
                historyEntry = historyEntry,
            )
        }
    }

    /*
     * All used property_type_class since 2001:
     *  [B (ByteArray)
     * boolean
     * de.micromata.fibu.AuftragsArt
     * de.micromata.fibu.AuftragsPositionsArt
     * de.micromata.fibu.AuftragsPositionsStatus
     * de.micromata.fibu.AuftragsStatus
     * de.micromata.fibu.EmployeeStatus
     * de.micromata.fibu.kost.KostentraegerStatus
     * de.micromata.fibu.KundeStatus
     * de.micromata.fibu.ProjektStatus
     * de.micromata.fibu.RechnungStatus
     * de.micromata.fibu.RechnungTyp
     * de.micromata.genome.db.jpa.history.entities.PropertyOpType
     * de.micromata.projectforge.address.AddressStatus
     * de.micromata.projectforge.address.ContactStatus
     * de.micromata.projectforge.address.FormOfAddress
     * de.micromata.projectforge.book.BookStatus
     * de.micromata.projectforge.core.Priority
     * de.micromata.projectforge.humanresources.HRPlanningEntryStatus
     * de.micromata.projectforge.orga.PostType
     * de.micromata.projectforge.scripting.ScriptParameterType
     * de.micromata.projectforge.task.TaskDO
     * de.micromata.projectforge.task.TaskStatus
     * de.micromata.projectforge.task.TimesheetBookingStatus
     * de.micromata.projectforge.user.PFUserDO
     * int
     * java.lang.Boolean
     * java.lang.Integer
     * java.lang.Short
     * java.lang.String
     * java.math.BigDecimal
     * java.sql.Date
     * java.sql.Timestamp
     * java.time.LocalDate
     * java.util.Date
     * java.util.Locale
     * net.fortuna.ical4j.model.Date
     * net.fortuna.ical4j.model.DateTime
     * org.projectforge.address.AddressStatus
     * org.projectforge.address.ContactStatus
     * org.projectforge.address.FormOfAddress
     * org.projectforge.book.BookStatus
     * org.projectforge.book.BookType
     * org.projectforge.business.address.AddressbookDO
     * org.projectforge.business.address.AddressDO
     * org.projectforge.business.address.AddressDO_$$_jvst148_2f
     * org.projectforge.business.address.AddressDO_$$*
     * org.projectforge.business.address.AddressDO$HibernateProxy$En8hKwh8
     * org.projectforge.business.address.AddressDO$HibernateProxy$*
     * org.projectforge.business.address.AddressStatus
     * org.projectforge.business.address.ContactStatus
     * org.projectforge.business.address.FormOfAddress
     * org.projectforge.business.book.BookStatus
     * org.projectforge.business.book.BookType
     * org.projectforge.business.fibu.AuftragsPositionDO
     * org.projectforge.business.fibu.AuftragsPositionDO$HibernateProxy$0HIvA23x
     * org.projectforge.business.fibu.AuftragsPositionDO$HibernateProxy$*
     * org.projectforge.business.fibu.AuftragsPositionsArt
     * org.projectforge.business.fibu.AuftragsPositionsPaymentType
     * org.projectforge.business.fibu.AuftragsPositionsStatus
     * org.projectforge.business.fibu.AuftragsStatus
     * org.projectforge.business.fibu.EingangsrechnungsPositionDO
     * org.projectforge.business.fibu.EmployeeDO
     * org.projectforge.business.fibu.EmployeeDO_$$_jvst77_1e
     * org.projectforge.business.fibu.EmployeeDO_$$*
     * org.projectforge.business.fibu.EmployeeSalaryType
     * org.projectforge.business.fibu.EmployeeStatus
     * org.projectforge.business.fibu.Gender
     * org.projectforge.business.fibu.IsoGender
     * org.projectforge.business.fibu.KontoDO
     * org.projectforge.business.fibu.KontoDO_$$_jvst148_1c
     * org.projectforge.business.fibu.KontoDO_$$*
     * org.projectforge.business.fibu.KontoDO$HibernateProxy$0DASBQzw
     * org.projectforge.business.fibu.KontoDO$HibernateProxy$*
     * org.projectforge.business.fibu.KontoStatus
     * org.projectforge.business.fibu.kost.Kost1DO
     * org.projectforge.business.fibu.kost.Kost2ArtDO
     * org.projectforge.business.fibu.kost.Kost2DO
     * org.projectforge.business.fibu.kost.Kost2DO$HibernateProxy$0epA96ZQ
     * org.projectforge.business.fibu.kost.Kost2DO$HibernateProxy$*
     * org.projectforge.business.fibu.kost.KostentraegerStatus
     * org.projectforge.business.fibu.kost.KostZuweisungDO
     * org.projectforge.business.fibu.kost.SHType
     * org.projectforge.business.fibu.KundeDO
     * org.projectforge.business.fibu.KundeDO$HibernateProxy$0M8kO4CB
     * org.projectforge.business.fibu.KundeDO$HibernateProxy$*
     * org.projectforge.business.fibu.KundeStatus
     * org.projectforge.business.fibu.ModeOfPaymentType
     * org.projectforge.business.fibu.PaymentScheduleDO
     * org.projectforge.business.fibu.PaymentType
     * org.projectforge.business.fibu.PeriodOfPerformanceType
     * org.projectforge.business.fibu.ProjektDO
     * org.projectforge.business.fibu.ProjektDO$HibernateProxy$2Zpfmf2Q
     * org.projectforge.business.fibu.ProjektDO$HibernateProxy$*
     * org.projectforge.business.fibu.ProjektStatus
     * org.projectforge.business.fibu.RechnungsPositionDO
     * org.projectforge.business.fibu.RechnungStatus
     * org.projectforge.business.fibu.RechnungTyp
     * org.projectforge.business.gantt.GanttObjectType
     * org.projectforge.business.gantt.GanttRelationType
     * org.projectforge.business.humanresources.HRPlanningEntryDO
     * org.projectforge.business.humanresources.HRPlanningEntryStatus
     * org.projectforge.business.orga.ContractStatus
     * org.projectforge.business.orga.PostType
     * org.projectforge.business.orga.VisitorType
     * org.projectforge.business.poll.PollDO
     * org.projectforge.business.poll.PollDO$State
     * org.projectforge.business.scripting.ScriptDO$ScriptType
     * org.projectforge.business.scripting.ScriptParameterType
     * org.projectforge.business.task.TaskDO
     * org.projectforge.business.task.TaskDO_$$_jvst148_1f
     * org.projectforge.business.task.TaskDO_$$*
     * org.projectforge.business.task.TaskDO$HibernateProxy$06x39fNa
     * org.projectforge.business.task.TaskDO$HibernateProxy$*
     * org.projectforge.business.teamcal.admin.model.TeamCalDO
     * org.projectforge.business.teamcal.admin.model.TeamCalDO_$$_jvst148_6
     * org.projectforge.business.teamcal.admin.model.TeamCalDO_$$*
     * org.projectforge.business.teamcal.admin.model.TeamCalDO$HibernateProxy$0igncMuL
     * org.projectforge.business.teamcal.admin.model.TeamCalDO$HibernateProxy$*
     * org.projectforge.business.teamcal.event.model.ReminderActionType
     * org.projectforge.business.teamcal.event.model.ReminderDurationUnit
     * org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO
     * org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus
     * org.projectforge.business.user.UserRightValue
     * org.projectforge.business.vacation.model.VacationDO
     * org.projectforge.business.vacation.model.VacationStatus
     * org.projectforge.common.i18n.Priority
     * org.projectforge.common.task.TaskStatus
     * org.projectforge.common.task.TimesheetBookingStatus
     * org.projectforge.common.TimeNotation
     * org.projectforge.core.Priority
     * org.projectforge.fibu.AuftragsPositionsArt
     * org.projectforge.fibu.AuftragsPositionsStatus
     * org.projectforge.fibu.AuftragsStatus
     * org.projectforge.fibu.EmployeeStatus
     * org.projectforge.fibu.KontoDO
     * org.projectforge.fibu.KontoStatus
     * org.projectforge.fibu.kost.KostentraegerStatus
     * org.projectforge.fibu.kost.SHType
     * org.projectforge.fibu.KundeStatus
     * org.projectforge.fibu.ModeOfPaymentType
     * org.projectforge.fibu.PaymentType
     * org.projectforge.fibu.PeriodOfPerformanceType
     * org.projectforge.fibu.ProjektStatus
     * org.projectforge.fibu.RechnungStatus
     * org.projectforge.fibu.RechnungTyp
     * org.projectforge.framework.access.AccessEntryDO
     * org.projectforge.framework.configuration.ConfigurationType
     * org.projectforge.framework.persistence.user.entities.Gender
     * org.projectforge.framework.persistence.user.entities.GroupDO
     * org.projectforge.framework.persistence.user.entities.GroupDO_$$_jvst148_37
     * org.projectforge.framework.persistence.user.entities.GroupDO_$$*
     * org.projectforge.framework.persistence.user.entities.GroupDO$HibernateProxy$EgauAiy8
     * org.projectforge.framework.persistence.user.entities.GroupDO$HibernateProxy$*
     * org.projectforge.framework.persistence.user.entities.PFUserDO
     * org.projectforge.framework.persistence.user.entities.PFUserDO_$$_jvst148_56
     * org.projectforge.framework.persistence.user.entities.PFUserDO_$$*
     * org.projectforge.framework.persistence.user.entities.PFUserDO$HibernateProxy$1BBv5CQJ
     * org.projectforge.framework.persistence.user.entities.PFUserDO$HibernateProxy$*
     * org.projectforge.framework.persistence.user.entities.TenantDO
     * org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvst148_1e
     * org.projectforge.framework.persistence.user.entities.TenantDO_$$*
     * org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$0CoFsucl
     * org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$*
     * org.projectforge.framework.time.TimeNotation
     * org.projectforge.gantt.GanttDependencyType
     * org.projectforge.gantt.GanttObjectType
     * org.projectforge.gantt.GanttRelationType
     * org.projectforge.humanresources.HRPlanningEntryStatus
     * org.projectforge.orga.PostType
     * org.projectforge.plugins.banking.BankAccountDO
     * org.projectforge.plugins.ffp.model.FFPAccountingDO
     * org.projectforge.plugins.ffp.model.FFPEventDO
     * org.projectforge.plugins.marketing.AddressCampaignDO
     * org.projectforge.plugins.marketing.AddressCampaignDO_$$_jvst148_26
     * org.projectforge.plugins.marketing.AddressCampaignDO_$$*
     * org.projectforge.plugins.skillmatrix.SkillDO
     * org.projectforge.plugins.skillmatrix.SkillDO_$$_jvstf4d_19
     * org.projectforge.plugins.skillmatrix.SkillRating
     * org.projectforge.plugins.teamcal.event.ReminderActionType
     * org.projectforge.plugins.teamcal.event.ReminderDurationUnit
     * org.projectforge.plugins.todo.ToDoStatus
     * org.projectforge.plugins.todo.ToDoType
     * org.projectforge.scripting.ScriptParameterType
     * org.projectforge.task.TaskDO
     * org.projectforge.task.TaskStatus
     * org.projectforge.task.TimesheetBookingStatus
     * org.projectforge.user.PFUserDO
     * org.projectforge.user.UserRightValue
     * void
     */
}
