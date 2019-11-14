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

import org.hibernate.search.annotations.*
import org.hibernate.search.bridge.builtin.IntegerBridge
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.sql.Date
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_CONTRACT",
        uniqueConstraints = [UniqueConstraint(columnNames = ["number", "tenant_id"])],
        indexes = [javax.persistence.Index(name = "idx_fk_t_contract_tenant_id", columnList = "tenant_id")])
@NamedQueries(
        NamedQuery(name = ContractDO.FIND_OTHER_BY_NUMBER, query = "from ContractDO where number=:number and id<>:id"),
        NamedQuery(name = ContractDO.SELECT_MIN_MAX_DATE, query = "select min(date), max(date) from ContractDO"))
open class ContractDO : DefaultBaseDO() {

    // TODO: Support int input field
    @PropertyInfo(i18nKey = "legalAffaires.contract.number")
    @Field(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column(name = "number")
    open var number: Int? = null

    @PropertyInfo(i18nKey = "date")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "c_date")
    open var date: Date? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.validity")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "valid_from")
    open var validFrom: Date? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.validity")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "valid_until")
    open var validUntil: Date? = null

    @PropertyInfo(i18nKey = "title", required = true)
    @Field
    @get:Column(length = 1000)
    open var title: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.coContractorA")
    @Field
    @get:Column(length = 1000, name = "co_contractor_a")
    open var coContractorA: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.contractPersonA")
    @Field
    @get:Column(length = 1000, name = "contract_person_a")
    open var contractPersonA: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.signerA")
    @Field
    @get:Column(length = 1000, name = "signer_a")
    open var signerA: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.coContractorB")
    @Field
    @get:Column(length = 1000, name = "co_contractor_b")
    open var coContractorB: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.contractPersonB")
    @Field
    @get:Column(length = 1000, name = "contract_person_b")
    open var contractPersonB: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.signerB")
    @Field
    @get:Column(length = 1000, name = "signer_b")
    open var signerB: String? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.signing")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "signing_date")
    open var signingDate: Date? = null

    @PropertyInfo(i18nKey = "legalAffaires.contract.type")
    @Field
    @get:Column(length = 100)
    open var type: String? = null

    @PropertyInfo(i18nKey = "status")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 100)
    open var status: ContractStatus? = null

    @PropertyInfo(i18nKey = "text")
    @Field
    @get:Column(length = 4000)
    open var text: String? = null

    @PropertyInfo(i18nKey = "fibu.common.reference")
    @Field
    @get:Column
    open var reference: String? = null

    @PropertyInfo(i18nKey = "filing")
    @Field
    @get:Column(length = 1000)
    open var filing: String? = null

    @PropertyInfo(i18nKey = "resubmissionOnDate")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "resubmission_on_date")
    open var resubmissionOnDate: Date? = null

    @PropertyInfo(i18nKey = "dueDate")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "due_date")
    open var dueDate: Date? = null

    companion object {
        internal const val FIND_OTHER_BY_NUMBER = "ContractDO_FindOtherByNumber"

        internal const val SELECT_MIN_MAX_DATE = "ContractDO_SelectMinMaxDate"
    }
}
