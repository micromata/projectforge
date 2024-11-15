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

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.time.LocalDate
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.projectforge.framework.persistence.history.NoHistory

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_CONTRACT")
@NamedQueries(
        NamedQuery(name = ContractDO.FIND_OTHER_BY_NUMBER, query = "from ContractDO where number=:number and id<>:id"),
        NamedQuery(name = ContractDO.SELECT_MIN_MAX_DATE, query = "select min(date), max(date) from ContractDO"))
open class ContractDO : DefaultBaseDO(), AttachmentsInfo {

    @PropertyInfo(i18nKey = "'C-", additionalI18nKey = "legalAffaires.contract.number", tooltip = "fibu.tooltip.nummerWirdAutomatischVergeben")
    @GenericField // was: @FullTextField(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column(name = "number")
    open var number: Int? = null

    @PropertyInfo(i18nKey = "date")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "c_date")
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.validity.from")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "valid_from")
    open var validFrom: LocalDate? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.validity.until")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "valid_until")
    open var validUntil: LocalDate? = null

    @PropertyInfo(i18nKey = "title", required = true)
    @FullTextField
    @get:Column(length = 1000)
    open var title: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.coContractorA")
    @FullTextField
    @get:Column(length = 1000, name = "co_contractor_a")
    open var coContractorA: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.contractPersonA")
    @FullTextField
    @get:Column(length = 1000, name = "contract_person_a")
    open var contractPersonA: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.signerA")
    @FullTextField
    @get:Column(length = 1000, name = "signer_a")
    open var signerA: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.coContractorB")
    @FullTextField
    @get:Column(length = 1000, name = "co_contractor_b")
    open var coContractorB: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.contractPersonB")
    @FullTextField
    @get:Column(length = 1000, name = "contract_person_b")
    open var contractPersonB: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.signerB")
    @FullTextField
    @get:Column(length = 1000, name = "signer_b")
    open var signerB: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.signing")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "signing_date")
    open var signingDate: LocalDate? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.type")
    @FullTextField
    @get:Column(length = 100)
    open var type: String? = null

    @PropertyInfo(i18nKey = "status")
    @FullTextField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 100)
    open var status: ContractStatus? = null

    @PropertyInfo(i18nKey = "text")
    @FullTextField
    @get:Column(length = 4000)
    open var text: String? = null

    @PropertyInfo(i18nKey = "fibu.common.reference")
    @FullTextField
    @get:Column
    open var reference: String? = null

    @PropertyInfo(i18nKey = "filing")
    @FullTextField
    @get:Column(length = 1000)
    open var filing: String? = null

    @PropertyInfo(i18nKey = "resubmissionOnDate")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "resubmission_on_date")
    open var resubmissionOnDate: LocalDate? = null

    @PropertyInfo(i18nKey = "dueDate")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "due_date")
    open var dueDate: LocalDate? = null

    @JsonIgnore
    @FullTextField
    @NoHistory
    @get:Column(length = 10000, name = "attachments_names")
    override var attachmentsNames: String? = null

    @JsonIgnore
    @FullTextField
    @NoHistory
    @get:Column(length = 10000, name = "attachments_ids")
    override var attachmentsIds: String? = null

    @JsonIgnore
    @NoHistory
    @get:Column(length = 10000, name = "attachments_counter")
    override var attachmentsCounter: Int? = null

    @JsonIgnore
    @NoHistory
    @get:Column(length = 10000, name = "attachments_size")
    override var attachmentsSize: Long? = null

    @PropertyInfo(i18nKey = "attachment")
    @JsonIgnore
    @get:Column(length = 10000, name = "attachments_last_user_action")
    override var attachmentsLastUserAction: String? = null

    companion object {
        internal const val FIND_OTHER_BY_NUMBER = "ContractDO_FindOtherByNumber"

        internal const val SELECT_MIN_MAX_DATE = "ContractDO_SelectMinMaxDate"
    }
}
