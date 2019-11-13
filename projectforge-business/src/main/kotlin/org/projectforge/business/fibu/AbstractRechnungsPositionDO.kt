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

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal
import javax.persistence.*

@MappedSuperclass
abstract class AbstractRechnungsPositionDO : DefaultBaseDO(), ShortDisplayNameCapable {
    @PropertyInfo(i18nKey = "fibu.rechnung.nummer")
    @get:Column
    open var number: Short = 0

    @PropertyInfo(i18nKey = "fibu.rechnung.text")
    @get:Column(name = "s_text", length = 1000)
    open  var text: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.menge")
    @get:Column(scale = 5, precision = 18)
    open var menge: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.position.einzelNetto")
    @get:Column(name = "einzel_netto", scale = 2, precision = 18)
    open var einzelNetto: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.mehrwertSteuerSatz")
    @get:Column(scale = 5, precision = 10)
    open var vat: BigDecimal? = null

    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @get:JoinColumn(name = "rechnungs_pos_fk")
    @get:OrderColumn(name = "index")
    open var kostZuweisungen: MutableList<KostZuweisungDO>? = null

    @get:Transient
    abstract val rechnungId: Int?

    fun getKostZuweisung(idx: Int): KostZuweisungDO? {
        return kostZuweisungen?.getOrNull(idx)
    }

    fun addKostZuweisung(kostZuweisung: KostZuweisungDO) {
        val zuweisungen = this.ensureAndGetKostzuweisungen()
        // Get the highest used number + 1 or take 0 for the first position.
        val nextIndex = zuweisungen.maxBy { it.index }?.index?.plus(1)?.toShort() ?: 0
        kostZuweisung.index = nextIndex
        kostZuweisung.setRechnungsPosition(this)
        zuweisungen.add(kostZuweisung)
    }

    fun deleteKostZuweisung(idx: Int) {
        val zuweisung = getKostZuweisung(idx) ?: return
        if (!isKostZuweisungDeletable(zuweisung)) {
            log.error("Deleting of cost assignements which are already persisted (a id / pk already exists) or not are not the last entry is not supported. Do nothing.")
            return
        }
        this.kostZuweisungen!!.remove(zuweisung)
    }

    fun ensureAndGetKostzuweisungen(): MutableList<KostZuweisungDO> {
        if (this.kostZuweisungen == null)
            kostZuweisungen = mutableListOf()
        return kostZuweisungen!!
    }

    fun isKostZuweisungDeletable(zuweisung: KostZuweisungDO?): Boolean {
        if (zuweisung == null)
            return false
        if (!checkKostZuweisungId(zuweisung)) {
            log.error("Oups, given cost assignment is not assigned to this invoice position.")
            return false
        }
        return zuweisung.id == null
    }

    abstract protected fun checkKostZuweisungId(zuweisung: KostZuweisungDO): Boolean

    @get:PropertyInfo(i18nKey = "fibu.common.netto")
    val netSum: BigDecimal
        @Transient
        get() = RechnungCalculator.calculateNetSum(this)

    val vatAmount: BigDecimal
        @Transient
        get() = RechnungCalculator.calculateVatAmountSum(this)

    val bruttoSum: BigDecimal
        @Transient
        get() = RechnungCalculator.calculateGrossSum(this)

    val kostZuweisungNetSum: BigDecimal
        @Transient
        get() = RechnungCalculator.kostZuweisungenNetSum(this)

    val kostZuweisungNetFehlbetrag: BigDecimal
        @Transient
        get() = RechnungCalculator.kostZuweisungenNetFehlbetrag(this)

    val kostZuweisungGrossSum: BigDecimal
        @Transient
        get() = RechnungCalculator.kostZuweisungenGrossSum(this)

    val isEmpty: Boolean
        @Transient
        get() = text.isNullOrEmpty() || NumberHelper.isZeroOrNull(einzelNetto)

    @Transient
    override fun getShortDisplayName(): String {
        return number.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || !(other is AbstractRechnungsPositionDO)) return false
        return this.number == other.number && this.rechnungId == other.rechnungId
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(number).append(rechnungId).toHashCode()
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AbstractRechnungsPositionDO::class.java)
    }
}
