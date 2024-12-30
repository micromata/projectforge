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

package org.projectforge.business.fibu.kost

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.Constants
import org.projectforge.business.fibu.AbstractRechnungsPositionDO
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.utils.CurrencyHelper
import org.projectforge.framework.utils.NumberFormatter
import java.math.BigDecimal

/**
 * Rechnungen (Ein- und Ausgang) sowie Gehaltssonderzahlungen werden auf Kost1 und Kost2 aufgeteilt. Einer Rechnung
 * können mehrere KostZuweisungen zugeordnet sein. Die Summe aller Einzelkostzuweisung sollte dem Betrag der
 * Rechnung/Gehaltszahlung entsprechen.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "T_FIBU_KOST_ZUWEISUNG",
    uniqueConstraints = [UniqueConstraint(columnNames = ["index", "rechnungs_pos_fk", "kost1_fk", "kost2_fk"]), UniqueConstraint(
        columnNames = ["index", "eingangsrechnungs_pos_fk", "kost1_fk", "kost2_fk"]
    ), UniqueConstraint(columnNames = ["index", "employee_salary_fk", "kost1_fk", "kost2_fk"])],
    indexes = [Index(
        name = "idx_fk_t_fibu_kost_zuweisung_eingangsrechnungs_pos_fk",
        columnList = "eingangsrechnungs_pos_fk"
    ), Index(
        name = "idx_fk_t_fibu_kost_zuweisung_employee_salary_fk",
        columnList = "employee_salary_fk"
    ), Index(
        name = "idx_fk_t_fibu_kost_zuweisung_kost1_fk",
        columnList = "kost1_fk"
    ), Index(
        name = "idx_fk_t_fibu_kost_zuweisung_kost2_fk",
        columnList = "kost2_fk"
    ), Index(name = "idx_fk_t_fibu_kost_zuweisung_rechnungs_pos_fk", columnList = "rechnungs_pos_fk")]
)
open class KostZuweisungDO : DefaultBaseDO(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = "$index:${kost1?.displayName}|${kost2?.displayName}:${NumberFormatter.formatCurrency(netto)}"

    /**
     * Die Kostzuweisungen sind als Array organisiert. Dies stellt den Index der Kostzuweisung dar. Der Index ist für
     * Gehaltszahlungen ohne Belang.
     *
     * @return
     */
    @get:Column
    open var index: Short = 0

    @PropertyInfo(i18nKey = "fibu.common.netto")
    @get:Column(scale = 2, precision = 12)
    open var netto: BigDecimal? = null

    @PropertyInfo(i18nKey = "fibu.kost1")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kost1_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var kost1: Kost1DO? = null

    @PropertyInfo(i18nKey = "fibu.kost2")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "kost2_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var kost2: Kost2DO? = null

    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "rechnungs_pos_fk", nullable = true)
    @JsonBackReference
    @JsonSerialize(using = IdOnlySerializer::class)
    open var rechnungsPosition: RechnungsPositionDO? = null
        set(rechnungsPosition) {
            if (rechnungsPosition != null && (this.eingangsrechnungsPosition != null || this.employeeSalary != null)) {
                throw IllegalStateException("eingangsRechnung or employeeSalary already given!")
            }
            field = rechnungsPosition
        }

    @IndexedEmbedded(includeDepth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "eingangsrechnungs_pos_fk", nullable = true)
    @JsonBackReference
    @JsonSerialize(using = IdOnlySerializer::class)
    open var eingangsrechnungsPosition: EingangsrechnungsPositionDO? = null
        set(eingangsrechnungsPosition) {
            if (eingangsrechnungsPosition != null && (this.rechnungsPosition != null || this.employeeSalary != null)) {
                throw IllegalStateException("rechnungsPosition or employeeSalary already given!")
            }
            field = eingangsrechnungsPosition
        }

    fun setAbstractRechnungsPosition(position: AbstractRechnungsPositionDO) {
        if (position is RechnungsPositionDO)
            rechnungsPosition = position
        else
            eingangsrechnungsPosition = position as EingangsrechnungsPositionDO
    }

    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "employee_salary_fk", nullable = true)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var employeeSalary: EmployeeSalaryDO? = null
        set(employeeSalary) {
            if (employeeSalary != null && (this.eingangsrechnungsPosition != null || this.rechnungsPosition != null)) {
                throw IllegalStateException("eingangsRechnung or rechnungsPosition already given!")
            }
            field = employeeSalary
        }

    @FullTextField
    @get:Column(length = Constants.COMMENT_LENGTH)
    open var comment: String? = null

    /**
     * Calculates gross amount using the vat from the invoice position.
     *
     * @return Gross amount if vat found otherwise net amount.
     * @see .getRechnungsPosition
     * @see .getEingangsrechnungsPosition
     */
    val brutto: BigDecimal
        @Transient
        get() {
            val vat: BigDecimal?
            when {
                this.rechnungsPosition != null -> vat = this.rechnungsPosition!!.vat
                this.eingangsrechnungsPosition != null -> vat = this.eingangsrechnungsPosition!!.vat
                else -> vat = null
            }
            return CurrencyHelper.getGrossAmount(this.netto, vat)
        }

    val kost1Id: Long?
        @Transient
        get() = if (this.kost1 == null) {
            null
        } else kost1!!.id

    val kost2Id: Long?
        @Transient
        get() = if (this.kost2 == null) {
            null
        } else kost2!!.id

    /**
     * @return true if betrag is zero or not given.
     */
    val isEmpty: Boolean
        @Transient
        get() = netto == null || netto!!.compareTo(BigDecimal.ZERO) == 0

    /**
     * If empty then no error will be returned.
     *
     * @return error message (i18n key) or null if no error is given.
     */
    @Transient
    fun hasErrors(): String? {
        if (isEmpty) {
            return null
        }
        var counter = 0
        if (rechnungsPosition?.id != null) {
            counter++
        }
        if (eingangsrechnungsPosition?.id != null) {
            counter++
        }
        if (employeeSalary?.id != null) {
            counter++
        }
        return if (counter != 1) {
            "fibu.kostZuweisung.error.genauEinFinanzobjektErwartet" // i18n key
        } else null
    }

    override fun equals(other: Any?): Boolean {
        if (other is KostZuweisungDO) {
            val o = other as KostZuweisungDO?
            if (this.index != o!!.index) {
                return false
            }
            if (this.rechnungsPosition?.id != o.rechnungsPosition?.id) {
                return false
            }
            if (this.eingangsrechnungsPosition?.id != o.eingangsrechnungsPosition?.id) {
                return false
            }
            return this.employeeSalary?.id == o.employeeSalary?.id
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(index)
        if (rechnungsPosition != null) {
            hcb.append(rechnungsPosition?.id)
        }
        if (eingangsrechnungsPosition != null) {
            hcb.append(eingangsrechnungsPosition?.id)
        }
        if (employeeSalary != null) {
            hcb.append(employeeSalary?.id)
        }
        return hcb.toHashCode()
    }

    /**
     * Clones this cost assignment (without id's).
     *
     * @return
     */
    fun newClone(): KostZuweisungDO {
        val kostZuweisung = KostZuweisungDO()
        kostZuweisung.copyValuesFrom(this, "id")
        return kostZuweisung
    }
}
