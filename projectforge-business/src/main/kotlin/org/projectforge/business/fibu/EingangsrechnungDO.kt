/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import com.fasterxml.jackson.annotation.JsonBackReference
import de.micromata.genome.db.jpa.history.api.WithHistory
import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
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
            javax.persistence.Index(name = "idx_fk_t_fibu_eingangsrechnung_konto_id", columnList = "konto_id"),
            javax.persistence.Index(name = "idx_fk_t_fibu_eingangsrechnung_tenant_id", columnList = "tenant_id")
        ])
// @AssociationOverride(name="positionen", joinColumns=@JoinColumn(name="eingangsrechnung_fk"))
@WithHistory(noHistoryProperties = ["lastUpdate", "created"], nestedEntities = [EingangsrechnungsPositionDO::class])
class EingangsrechnungDO : AbstractRechnungDO<EingangsrechnungsPositionDO>(), Comparable<EingangsrechnungDO> {

    /**
     * Referenz / Eingangsrechnungsnummer des Kreditors.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "fibu.common.reference")
    @Field
    @get:Column(length = 1000)
    var referenz: String? = null

    @PropertyInfo(i18nKey = "fibu.common.creditor")
    @Field
    @get:Column(length = 255)
    var kreditor: String? = null

    @PropertyInfo(i18nKey = "fibu.payment.type")
    @Field(bridge = FieldBridge(impl = HibernateSearchPaymentTypeBridge::class))
    @get:Column(length = 20, name = "payment_type")
    @get:Enumerated(EnumType.STRING)
    var paymentType: PaymentType? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.customernr")
    @Field
    @get:Column
    var customernr: String? = null

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @JsonBackReference
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, mappedBy = "eingangsrechnung", targetEntity = EingangsrechnungsPositionDO::class)
    @get:OrderColumn(name = "number") // was IndexColumn(name = "number", base = 1)
    @get:ListIndexBase(1)
    override var positionen: MutableList<EingangsrechnungsPositionDO>? = null

    /**
     * (this.status == EingangsrechnungStatus.BEZAHLT && this.bezahlDatum != null && this.zahlBetrag != null)
     */
    override val isBezahlt: Boolean
        @Transient
        get() = if (this.netSum == null || this.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else this.bezahlDatum != null && this.zahlBetrag != null

    override fun compareTo(other: EingangsrechnungDO): Int {
        var r = this.datum!!.compareTo(other.datum!!)
        if (r != 0) {
            return -r
        }
        var s1 = StringUtils.defaultString(this.kreditor)
        var s2 = StringUtils.defaultString(other.kreditor)
        r = s1.compareTo(s2)
        if (r != 0) {
            return -r
        }
        s1 = StringUtils.defaultString(this.referenz)
        s2 = StringUtils.defaultString(other.referenz)
        return s1.compareTo(s2)
    }
}
