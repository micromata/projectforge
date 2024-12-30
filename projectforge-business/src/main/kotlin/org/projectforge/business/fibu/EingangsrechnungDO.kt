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

package org.projectforge.business.fibu

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.history.PersistenceBehavior
import org.projectforge.framework.utils.StringComparator

/**
 * Eingehende Rechnungen.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "t_fibu_eingangsrechnung",
    indexes = [
        Index(name = "idx_fk_t_fibu_eingangsrechnung_konto_id", columnList = "konto_id")
    ]
)
// @AssociationOverride(name="positionen", joinColumns=@JoinColumn(name="eingangsrechnung_fk"))
//@WithHistory(noHistoryProperties = ["lastUpdate", "created"], nestedEntities = [EingangsrechnungsPositionDO::class])
@NamedQueries(
    NamedQuery(
        name = EingangsrechnungDO.SELECT_MIN_MAX_DATE,
        query = "select min(datum), max(datum) from EingangsrechnungDO"
    )
)
open class EingangsrechnungDO : AbstractRechnungDO(), Comparable<EingangsrechnungDO>, DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = if (referenz.isNullOrBlank()) "$kreditor" else "$kreditor: $referenz"

    @PropertyInfo(i18nKey = "fibu.rechnung.receiver")
    @FullTextField
    @get:Column
    open var receiver: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.iban")
    @FullTextField
    @get:Column(length = 50)
    open var iban: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.bic")
    @FullTextField
    @get:Column(length = 11)
    open var bic: String? = null

    /**
     * Referenz / Eingangsrechnungsnummer des Kreditors.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "fibu.common.reference")
    @FullTextField
    @get:Column(length = 1000)
    open var referenz: String? = null

    @PropertyInfo(i18nKey = "fibu.common.creditor")
    @FullTextField
    @get:Column(length = 255)
    open var kreditor: String? = null

    @PropertyInfo(i18nKey = "fibu.payment.type")
    @GenericField(valueBridge = ValueBridgeRef(type = HibernateSearchPaymentTypeBridge::class))
    @get:Column(length = 20, name = "payment_type")
    @get:Enumerated(EnumType.STRING)
    open var paymentType: PaymentType? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.customernr")
    @FullTextField
    @get:Column
    open var customernr: String? = null

    val ibanFormatted: String?
        @Transient
        get() = IBANUtils.format(iban)

    @JsonManagedReference
    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
        orphanRemoval = false,
        fetch = FetchType.LAZY,
        mappedBy = "eingangsrechnung",
        targetEntity = EingangsrechnungsPositionDO::class,
    )
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    override var positionen: MutableList<EingangsrechnungsPositionDO>? = null

    override val abstractPositionen: List<AbstractRechnungsPositionDO>?
        @Transient
        get() = positionen

    override fun ensureAndGetPositionen(): MutableList<out AbstractRechnungsPositionDO> {
        if (this.positionen == null) {
            positionen = mutableListOf()
        }
        return positionen!!
    }

    override fun addPositionWithoutCheck(position: AbstractRechnungsPositionDO) {
        position as EingangsrechnungsPositionDO
        this.positionen!!.add(position)
        position.eingangsrechnung = this
    }


    override fun setAbstractRechnung(position: AbstractRechnungsPositionDO) {
        position as EingangsrechnungsPositionDO
        position.eingangsrechnung = this
    }

    override fun compareTo(other: EingangsrechnungDO): Int {
        var cmp = compareValues(this.datum, other.datum)
        if (cmp != 0) return cmp
        cmp = StringComparator.compare(this.kreditor, other.kreditor)
        if (cmp != 0) return cmp
        return StringComparator.compare(this.referenz, other.referenz)
    }

    companion object {
        internal const val SELECT_MIN_MAX_DATE = "EingangsrechnungDO_SelectMinMaxDate"
    }
}
