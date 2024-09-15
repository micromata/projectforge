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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import java.util.*

private val log = KotlinLogging.logger {}

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
 *  type                | character(1)                |           | not null | N, V
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
class PfHistoryAttrDO {
    @get:GeneratedValue
    @get:Column(name = "pk")
    @get:Id
    var id: Long? = null

    /**
     * User id (same as modifiedBy and master.modifiedBy)
     */
    @get:Column(name = "createdby")
    var createdBy: String? = null

    /**
     * Same as modifiedAt and master.modifiedAt.
     */
    @get:Column(name = "createdat")
    var createdAt: Date? = null

    /**
     * User id (same as createdBy and master.createdBy)
     */
    @get:Column(name = "modifiedby")
    var modifiedBy: String? = null

    /**
     * Same as createdAt and master.createdAt.
     */
    @get:Column(name = "modifiedat")
    var modifiedAt: Date? = null

    /**
     * The new value.
     */
    @get:Column(name = "value", length = 100000)
    var value: String? = null

    /**
     * The new value.
     */
    @get:Column(name = "old_value", length = 100000)
    var oldValue: String? = null

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
    var propertyName: String? = null

    /**
     * de.micromata.genome.util.strings.converter.ConvertedStringTypes
     * N (Null) or V (String)
     */
    @get:Column(name = "type", length = 1)
    var type: String? = null

    /**
     * Used values (until summer 2024):
     *  boolean
     *  de.micromata.fibu.AuftragsArt
     *  de.micromata.fibu.AuftragsPositionsArt
     *  de.micromata.fibu.AuftragsPositionsStatus
     *  de.micromata.fibu.AuftragsStatus
     *  de.micromata.fibu.EmployeeStatus
     *  de.micromata.fibu.kost.KostentraegerStatus
     *  de.micromata.fibu.KundeStatus
     *  de.micromata.fibu.ProjektStatus
     *  de.micromata.fibu.RechnungStatus
     *  de.micromata.fibu.RechnungTyp
     *  de.micromata.genome.db.jpa.history.entities.PropertyOpType
     *  de.micromata.projectforge.address.AddressStatus
     *  de.micromata.projectforge.address.ContactStatus
     *  de.micromata.projectforge.address.FormOfAddress
     *  de.micromata.projectforge.book.BookStatus
     *  de.micromata.projectforge.core.Priority
     *  de.micromata.projectforge.humanresources.HRPlanningEntryStatus
     *  de.micromata.projectforge.orga.PostType
     *  de.micromata.projectforge.scripting.ScriptParameterType
     *  de.micromata.projectforge.task.TaskDO
     *  de.micromata.projectforge.task.TaskStatus
     *  de.micromata.projectforge.task.TimesheetBookingStatus
     *  de.micromata.projectforge.user.PFUserDO
     *  int
     *  java.lang.Boolean
     *  java.lang.Integer
     *  java.lang.Short
     *  java.lang.String
     *  java.math.BigDecimal
     *  java.sql.Date
     *  java.sql.Timestamp
     *  java.time.LocalDate
     *  java.util.Date
     *  java.util.Locale
     *  net.fortuna.ical4j.model.Date
     *  net.fortuna.ical4j.model.DateTime
     *  org.projectforge.address.AddressStatus
     *  org.projectforge.address.ContactStatus
     *  org.projectforge.address.FormOfAddress
     *  org.projectforge.book.BookStatus
     *  org.projectforge.book.BookType
     *  org.projectforge.business.address.AddressbookDO
     *  org.projectforge.business.address.AddressDO
     *  org.projectforge.business.address.AddressDO_$$_jvst148_2f
     *  org.projectforge.business.address.AddressDO_$$_jvst16a_2f
     *  org.projectforge.business.address.AddressDO_$$_jvst174_2f
     *  ...
     *  org.projectforge.business.fibu.ProjektDO$HibernateProxy$L5sN8U45
     *  org.projectforge.business.fibu.ProjektDO$HibernateProxy$LKb2NJnZ
     *  org.projectforge.business.fibu.ProjektDO$HibernateProxy$mRBJJfeJ
     *  ...
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$op5ygU4h
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$OTs8u37E
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$PEKvYn0M
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$pL6vOROg
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$pryblTxE
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$PWGeegv6
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$PZtCR99n
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$Q4HYrXiF
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$QEL1umFQ
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$qFzJgJXc
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$qjRoWiJQ
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$QjYlbJSR
     *  org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$QkjMNYcg
     *  ...
     *  org.projectforge.framework.time.TimeNotation
     *  org.projectforge.gantt.GanttDependencyType
     *  org.projectforge.gantt.GanttObjectType
     *  org.projectforge.gantt.GanttRelationType
     *  org.projectforge.humanresources.HRPlanningEntryStatus
     *  org.projectforge.orga.PostType
     *  org.projectforge.plugins.banking.BankAccountDO
     *  org.projectforge.plugins.ffp.model.FFPAccountingDO
     *  org.projectforge.plugins.ffp.model.FFPEventDO
     *  org.projectforge.plugins.marketing.AddressCampaignDO
     *  org.projectforge.plugins.marketing.AddressCampaignDO_$$_jvst148_26
     *  org.projectforge.plugins.marketing.AddressCampaignDO_$$_jvstba1_26
     *  org.projectforge.plugins.marketing.AddressCampaignDO_$$_jvstc5d_26
     *  org.projectforge.plugins.skillmatrix.SkillDO
     *  org.projectforge.plugins.skillmatrix.SkillDO_$$_jvstf4d_19
     *  org.projectforge.plugins.skillmatrix.SkillRating
     *  org.projectforge.plugins.teamcal.event.ReminderActionType
     *  org.projectforge.plugins.teamcal.event.ReminderDurationUnit
     *  org.projectforge.plugins.todo.ToDoStatus
     *  org.projectforge.plugins.todo.ToDoType
     *  org.projectforge.scripting.ScriptParameterType
     *  org.projectforge.task.TaskDO
     *  org.projectforge.task.TaskStatus
     *  org.projectforge.task.TimesheetBookingStatus
     *  org.projectforge.user.PFUserDO
     *  org.projectforge.user.UserRightValue
     *  void
     */
    @get:Column(name = "property_type_class", length = 128)
    var propertyTypeClass: String? = null
}
