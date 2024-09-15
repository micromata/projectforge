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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.framework.json.JsonUtils
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Stores history.
 *
 *  pk             | bigint                      |           | not null |
 *  createdat      | timestamp without time zone |           | not null | -- equals to modifiedat
 *  createdby      | character varying(60)       |           | not null | -- equals to modifiedby
 *  modifiedat     | timestamp without time zone |           | not null |
 *  modifiedby     | character varying(60)       |           | not null |
 *  updatecounter  | integer                     |           | not null | -- always 0
 *  entity_id      | bigint                      |           | not null |
 *  entity_name    | character varying(255)      |           | not null | -- Full qualified class, e.g. org.projectforge.business.task.TaskDO
 *  entity_optype  | character varying(32)       |           |          | Insert | Update
 *  transaction_id | character varying(64)       |           |          | -- the_10415 (for example) or null
 *  user_comment   | character varying(2000)     |           |          | -- always 0
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Table(
    name = "t_pf_history",
    //indexes = [Index(
    //    name = "ix_pf_history_ent",
    //    columnList = "ENTITY_ID,ENTITY_NAME"
    //), Index(name = "ix_pf_history_mod", columnList = "MODIFIEDAT")]
)
@Indexed
//@ClassBridge(impl = HistoryMasterClassBridge::class)
class PfHistoryMasterDO : HistoryEntry<Long> {
    @get:GeneratedValue
    @get:Column(name = "pk")
    @get:Id
    override var id: Long? = null

    /**
     * Full qualified class name, e. g. org.projectforge.business.task.TaskDO.
     */
    @get:Column(name = "entity_name", length = 255)
    @GenericField // was @Field(analyze = Analyze.NO, store = Store.NO)
    override var entityName: String? = null

    @get:Column(name = "entity_id")
    //@GenericField // was @get:Field(analyze = Analyze.NO, store = Store.YES, index = Indexed.YES)
    override var entityId: Long? = null

    /**
     * Insert or Update. EntityOpType, was: de.micromata.genome.db.jpa.history.entities.PropertyOpType
     */
    @get:Column(name = "entity_optype", length = 32)
    override var entityOpType: EntityOpType? = null

    /**
     * User id (same as modifiedBy)
     */
    @get:Column(name = "createdby", length = 60)
    //@GenericField // was: @get:Field(analyze = Analyze.NO, store = Store.NO, index = Indexed.YES)
    var createdBy: String? = null

    /**
     * Same as modifiedAt.
     */
    @get:Column(name = "createdat")
    //@GenericField
    var createdAt: Date? = null

    /**
     * User id (same as createdBy)
     */
    @get:Column(name = "modifiedby", length = 60)
    //@GenericField // was: @get:Field(analyze = Analyze.NO, store = Store.NO, index = Indexed.YES)
    override var modifiedBy: String? = null

    /**
     * Same
     */
    @get:Column(name = "modifiedat")
    //@GenericField
    override var modifiedAt: Date? = null

    /**
     * Not in use.
     * the_10415 (for example) or null
     */
    @get:Column(name = "transaction_id", length = 64)
    var transactionId: String? = null

    /*@get:MapKey(name = "propertyName")
    @get:OneToMany(
        cascade = [CascadeType.ALL],
        mappedBy = "parent",
        //targetEntity = PfHistoryAttrDO::class,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )*/
    @get:Transient
    var attributes: MutableMap<String, Any?>? = null

    /* @get:Transient
     val attrEntityClass: Class<out Any?>
         get() = PfHistoryAttrDO::class.java

     @get:Transient
     val attrEntityWithDataClass: Class<out Any?>
         get() = PfHistoryAttrWithDataDO::class.java
 */
    //@get:Transient
    //val attrDataEntityClass: Class<out Any?>
    //get() = PfHistoryAttrDataDO::class.java

    /*
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
    */
    @get:Transient
    override val diffEntries: List<DiffEntry>?
        get() {
            log.entry("******* diffEntries not yet implemented")
            return null
        }

    override fun toString(): String {
        return JsonUtils.toJson(this)
    }
}

/**
 * entity_names
 *  org.projectforge.business.address.AddressbookDO
 *  org.projectforge.business.address.AddressDO
 *  org.projectforge.business.book.BookDO
 *  org.projectforge.business.fibu.AuftragDO
 *  org.projectforge.business.fibu.AuftragsPositionDO
 *  org.projectforge.business.fibu.EingangsrechnungDO
 *  org.projectforge.business.fibu.EingangsrechnungsPositionDO
 *  org.projectforge.business.fibu.EmployeeDO
 *  org.projectforge.business.fibu.EmployeeSalaryDO
 *  org.projectforge.business.fibu.KontoDO
 *  org.projectforge.business.fibu.kost.BuchungssatzDO
 *  org.projectforge.business.fibu.kost.Kost1DO
 *  org.projectforge.business.fibu.kost.Kost2ArtDO
 *  org.projectforge.business.fibu.kost.Kost2DO
 *  org.projectforge.business.fibu.kost.KostZuweisungDO
 *  org.projectforge.business.fibu.KundeDO
 *  org.projectforge.business.fibu.PaymentScheduleDO
 *  org.projectforge.business.fibu.ProjektDO
 *  org.projectforge.business.fibu.RechnungDO
 *  org.projectforge.business.fibu.RechnungsPositionDO
 *  org.projectforge.business.humanresources.HRPlanningDO
 *  org.projectforge.business.humanresources.HRPlanningEntryDO
 *  org.projectforge.business.orga.ContractDO
 *  org.projectforge.business.orga.PostausgangDO
 *  org.projectforge.business.orga.PosteingangDO
 *  org.projectforge.business.orga.VisitorbookDO
 *  org.projectforge.business.poll.PollDO
 *  org.projectforge.business.poll.PollResponseDO
 *  org.projectforge.business.scripting.ScriptDO
 *  org.projectforge.business.task.TaskDO
 *  org.projectforge.business.teamcal.admin.model.TeamCalDO
 *  org.projectforge.business.teamcal.event.model.CalEventDO
 *  org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO
 *  org.projectforge.business.teamcal.event.model.TeamEventDO
 *  org.projectforge.business.timesheet.TimesheetDO
 *  org.projectforge.business.vacation.model.LeaveAccountEntryDO
 *  org.projectforge.business.vacation.model.RemainingLeaveDO
 *  org.projectforge.business.vacation.model.VacationCalendarDO
 *  org.projectforge.business.vacation.model.VacationDO
 *  org.projectforge.framework.access.GroupTaskAccessDO
 *  org.projectforge.framework.configuration.entities.ConfigurationDO
 *  org.projectforge.framework.persistence.user.entities.GroupDO
 *  org.projectforge.framework.persistence.user.entities.PFUserDO
 *  org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
 *  org.projectforge.framework.persistence.user.entities.UserPasswordDO
 *  org.projectforge.framework.persistence.user.entities.UserRightDO
 *  org.projectforge.plugins.banking.BankAccountDO
 *  org.projectforge.plugins.banking.BankAccountRecordDO
 *  org.projectforge.plugins.eed.model.EmployeeConfigurationDO
 *  org.projectforge.plugins.ffp.model.FFPAccountingDO
 *  org.projectforge.plugins.ffp.model.FFPDebtDO
 *  org.projectforge.plugins.ffp.model.FFPEventDO
 *  org.projectforge.plugins.licensemanagement.LicenseDO
 *  org.projectforge.plugins.liquidityplanning.LiquidityEntryDO
 *  org.projectforge.plugins.marketing.AddressCampaignDO
 *  org.projectforge.plugins.marketing.AddressCampaignValueDO
 *  org.projectforge.plugins.skillmatrix.SkillDO
 *  org.projectforge.plugins.skillmatrix.SkillEntryDO
 *  org.projectforge.plugins.skillmatrix.SkillRatingDO
 *  org.projectforge.plugins.skillmatrix.TrainingAttendeeDO
 *  org.projectforge.plugins.skillmatrix.TrainingDO
 *  org.projectforge.plugins.todo.ToDoDO
 */
