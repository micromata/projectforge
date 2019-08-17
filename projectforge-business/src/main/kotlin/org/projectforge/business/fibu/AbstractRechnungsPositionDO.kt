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

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.utils.CurrencyHelper
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal
import javax.persistence.Column
import javax.persistence.MappedSuperclass
import javax.persistence.Transient

/**
 * Repräsentiert eine Position innerhalb eine Rechnung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@MappedSuperclass
abstract class AbstractRechnungsPositionDO: DefaultBaseDO(), ShortDisplayNameCapable {

    private val log = org.slf4j.LoggerFactory
            .getLogger(AbstractRechnungsPositionDO::class.java)

    @PropertyInfo(i18nKey = "fibu.rechnung.nummer")
    @get:Column
    var number: Short = 0

    @PropertyInfo(i18nKey = "fibu.rechnung.text")
    @get:Column(name = "s_text", length = 1000)
    var text: String? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.menge")
    @get:Column(scale = 5, precision = 18)
    var menge: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.position.einzelNetto")
    @get:Column(name = "einzel_netto", scale = 2, precision = 18)
    var einzelNetto: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.rechnung.mehrwertSteuerSatz")
    @get:Column(scale = 5, precision = 10)
    var vat: BigDecimal? = null

    @get:Transient
    abstract var kostZuweisungen: MutableList<KostZuweisungDO>?

    @get:Transient
    abstract val rechnungId: Int?

    @get:PropertyInfo(i18nKey = "fibu.common.netto")
    val netSum: BigDecimal
        @Transient
        get() = if (this.menge != null) {
            if (this.einzelNetto != null) {
                CurrencyHelper.multiply(this.menge, this.einzelNetto)
            } else {
                BigDecimal.ZERO
            }
        } else {
            (if (this.einzelNetto != null) this.einzelNetto else BigDecimal.ZERO)!!
        }

    val bruttoSum: BigDecimal
        @Transient
        get() {
            val netSum = netSum
            return if (vat != null) {
                netSum.add(CurrencyHelper.multiply(netSum, vat))
            } else {
                netSum
            }
        }

    val vatAmount: BigDecimal
        @Transient
        get() {
            val netSum = netSum
            return if (vat != null) {
                CurrencyHelper.multiply(netSum, vat)
            } else {
                BigDecimal.ZERO
            }
        }

    /**
     * @return The total net sum of all assigned cost entries multiplied with the vat of this position.
     */
    val kostZuweisungGrossSum: BigDecimal
        @Transient
        get() = CurrencyHelper.getGrossAmount(kostZuweisungsNetSum, vat)

    /**
     * @return The net value as sum of all cost assignements.
     */
    val kostZuweisungsNetSum: BigDecimal
        @Transient
        get() {
            var sum = BigDecimal.ZERO
            if (CollectionUtils.isNotEmpty(this.kostZuweisungen)) {
                for (zuweisung in this.kostZuweisungen!!) {
                    sum = NumberHelper.add(sum, zuweisung.netto)
                }
            }
            return sum
        }

    val kostZuweisungNetFehlbetrag: BigDecimal
        @Transient
        get() = kostZuweisungsNetSum.subtract(netSum)

    val isEmpty: Boolean
        @Transient
        get() = if (!StringUtils.isBlank(text)) {
            false
        } else !NumberHelper.isNotZero(einzelNetto)

    /**
     * @param index Index of the cost assignment not index of collection.
     * @return KostZuweisungDO with given index or null, if not exist.
     */
    fun getKostZuweisung(index: Int): KostZuweisungDO? {
        if (kostZuweisungen == null) {
            log.error("Can't get cost assignment with index $index because no cost assignments given.")
            return null
        }
        for (zuweisung in kostZuweisungen!!) {
            if (index == zuweisung.index.toInt()) {
                return zuweisung
            }
        }
        log.error("Can't found cost assignment with index $index")
        return null
    }

    fun addKostZuweisung(kostZuweisung: KostZuweisungDO): AbstractRechnungsPositionDO {
        ensureAndGetKostzuweisungen()
        var index: Short = 0
        for (zuweisung in kostZuweisungen!!) {
            if (zuweisung.index >= index) {
                index = zuweisung.index
                index++
            }
        }
        kostZuweisung.index = index
        setThis(kostZuweisung)
        this.kostZuweisungen!!.add(kostZuweisung)
        return this
    }

    /**
     * kostZuweisung.setEingangsrechnungsPosition(this);
     *
     * @param kostZuweisung
     */
    abstract fun setThis(kostZuweisung: KostZuweisungDO)

    abstract fun newInstance(): AbstractRechnungsPositionDO

    /**
     * Does only work for not already persisted entries (meaning entries without an id / pk) and only the last entry of
     * the list. Otherwise this method logs an error message and do nothing else.
     *
     * @param idx
     * @see .isKostZuweisungDeletable
     */
    fun deleteKostZuweisung(idx: Int): AbstractRechnungsPositionDO {
        val zuweisung = getKostZuweisung(idx) ?: return this
        if (!isKostZuweisungDeletable(zuweisung)) {
            log
                    .error(
                            "Deleting of cost assignements which are already persisted (a id / pk already exists) or not are not the last entry is not supported. Do nothing.")
            return this
        }
        this.kostZuweisungen!!.remove(zuweisung)
        return this
    }

    /**
     * Only the last entry of cost assignments is deletable if not already persisted (no id/pk given).
     *
     * @param zuweisung
     * @return
     */
    fun isKostZuweisungDeletable(zuweisung: KostZuweisungDO?): Boolean {
        if (zuweisung == null) {
            return false
        }
        if (this is EingangsrechnungsPositionDO && zuweisung.eingangsrechnungsPositionId != this.id || this is RechnungsPositionDO && zuweisung.rechnungsPositionId != this.id) {
            log.error("Oups, given cost assignment is not assigned to this invoice position.")
            return false
        }
        return zuweisung.id == null
        // if (zuweisung.getIndex() + 1 < this.kostZuweisungen.size()) {
        // return false;
        // }
    }

    fun ensureAndGetKostzuweisungen(): List<KostZuweisungDO>? {
        if (this.kostZuweisungen == null) {
            kostZuweisungen = ArrayList()
        }
        return kostZuweisungen
    }

    /**
     * Clones this including cost assignments and order position (without id's).
     *
     * @return
     */
    fun newClone(): AbstractRechnungsPositionDO {
        val rechnungsPosition = newInstance()
        rechnungsPosition.copyValuesFrom(this, "id", "kostZuweisungen")
        if (this.kostZuweisungen != null) {
            for (origKostZuweisung in this.kostZuweisungen!!) {
                val kostZuweisung = origKostZuweisung.newClone()
                rechnungsPosition.addKostZuweisung(kostZuweisung)
            }
        }
        return rechnungsPosition
    }

    override fun equals(other: Any?): Boolean {
        if (other is AbstractRechnungsPositionDO) {
            val o = other as AbstractRechnungsPositionDO?
            if (this.number != o!!.number) {
                return false
            }
            return this.rechnungId == o.rechnungId
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(number)
        if (rechnungId != null) {
            hcb.append(rechnungId!!)
        }
        return hcb.toHashCode()
    }

    @Transient
    override fun getShortDisplayName(): String {
        return number.toString()
    }
}
