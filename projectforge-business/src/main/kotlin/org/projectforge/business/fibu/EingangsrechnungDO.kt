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
import de.micromata.genome.db.jpa.history.api.WithHistory
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.utils.StringComparator
import java.math.BigDecimal
import javax.persistence.*

/**
 * Eingehende Rechnungen.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_eingangsrechnung",
        indexes = [
            Index(name = "idx_fk_t_fibu_eingangsrechnung_konto_id", columnList = "konto_id"),
            Index(name = "idx_fk_t_fibu_eingangsrechnung_tenant_id", columnList = "tenant_id")
        ])
// @AssociationOverride(name="positionen", joinColumns=@JoinColumn(name="eingangsrechnung_fk"))
@WithHistory(noHistoryProperties = ["lastUpdate", "created"], nestedEntities = [EingangsrechnungsPositionDO::class])
@NamedQueries(
        NamedQuery(name = EingangsrechnungDO.SELECT_MIN_MAX_DATE, query = "select min(datum), max(datum) from EingangsrechnungDO"))
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class EingangsrechnungDO : AbstractRechnungDO(), Comparable<EingangsrechnungDO> {

    @PropertyInfo(i18nKey = "fibu.rechnung.receiver")
    @Field
    @get:Column
    open var receiver: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.iban")
    @Field
    @get:Column(length = 50)
    open var iban: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.bic")
    @Field
    @get:Column(length = 11)
    open var bic: String? = null


    /**
     * Referenz / Eingangsrechnungsnummer des Kreditors.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "fibu.common.reference")
    @Field
    @get:Column(length = 1000)
    open var referenz: String? = null

    @PropertyInfo(i18nKey = "fibu.common.creditor")
    @Field
    @get:Column(length = 255)
    open var kreditor: String? = null

    @PropertyInfo(i18nKey = "fibu.payment.type")
    @Field(bridge = FieldBridge(impl = HibernateSearchPaymentTypeBridge::class))
    @get:Column(length = 20, name = "payment_type")
    @get:Enumerated(EnumType.STRING)
    open var paymentType: PaymentType? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.customernr")
    @Field
    @get:Column
    open var customernr: String? = null

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "eingangsrechnung", targetEntity = EingangsrechnungsPositionDO::class)
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    open var positionen: MutableList<EingangsrechnungsPositionDO>? = null

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


    override fun setRechnung(position: AbstractRechnungsPositionDO) {
        position as EingangsrechnungsPositionDO
        position.eingangsrechnung = this
    }

    /**
     * (this.status == EingangsrechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null)
     */
    override val isBezahlt: Boolean
        @Transient
        get() = if (this.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else this.bezahlDatum != null && this.zahlBetrag != null

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
