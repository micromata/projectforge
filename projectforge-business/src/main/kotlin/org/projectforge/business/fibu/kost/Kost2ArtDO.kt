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

package org.projectforge.business.fibu.kost

import java.math.BigDecimal

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.lucene.analysis.standard.ClassicAnalyzer
import org.hibernate.search.annotations.Analyzer
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.IManualIndex
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO

/**
 * Die letzten beiden Ziffern (Endziffern) eines Kostenträgers repräsentieren die Kostenart. Anhand der Endziffer kann
 * abgelesen werden, um welche Art von Kostenträger es sich handelt (fakturiert/nicht fakturiert, Akquise, Wartung etc.)
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_FIBU_KOST2ART", indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_kost2art_tenant_id", columnList = "tenant_id")])
@Analyzer(impl = ClassicAnalyzer::class)
class Kost2ArtDO : AbstractHistorizableBaseDO<Int>(), Comparable<Kost2ArtDO>, IManualIndex {

    /**
     * Zweistellige Endziffer von KOST2
     */
    @PropertyInfo(i18nKey = "fibu.kost2art.nummer")
    private var id: Int? = null

    @PropertyInfo(i18nKey = "name")
    @Field
    @get:Column(length = 255, nullable = false)
    var name: String? = null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 5000)
    var description: String? = null

    /**
     * Werden die Aufwendungen nach außen fakturiert, d. h. stehen den Ausgaben auch Einnahmen entgegen (i. d. R.
     * Kundenrechnungen oder Fördermaßnahmen).
     */
    @PropertyInfo(i18nKey = "fibu.fakturiert")
    @get:Column(nullable = false)
    var fakturiert: Boolean = false

    @PropertyInfo(i18nKey = "fibu.kost2art.workFraction")
    @get:Column(name = "work_fraction", scale = 5, precision = 10)
    var workFraction: BigDecimal? = null

    /**
     * Wenn true, dann wird diese Kostenart für Projekte als Standardendziffer für Kostenträger vorgeschlagen.
     */
    @PropertyInfo(i18nKey = "fibu.kost2art.projektStandard")
    @get:Column(name = "projekt_standard")
    var projektStandard: Boolean = false

    /**
     * Zweistellige Endziffer von KOST2
     */
    @Id
    @Column(name = "pk")
    override fun getId(): Int? {
        return id
    }

    /**
     * Muss größer als 0 und kleiner als 100 sein, sonst wird ein Validierungsfehler geworfen.
     *
     * @param id
     */
    override fun setId(id: Int?) {
        this.id = id
    }

    fun withId(id: Int?): Kost2ArtDO {
        setId(id)
        return this
    }

    /**
     * return true if id is equal, otherwise false;
     *
     * @see java.lang.Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (other is Kost2ArtDO) {
            val o = other as Kost2ArtDO?
            return this.id == o!!.id
        }
        return false
    }

    /**
     * Uses HashCodeBuilder with property id.
     *
     * @see java.lang.Object.hashCode
     * @see HashCodeBuilder.append
     */
    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(this.id)
        return hcb.toHashCode()
    }

    override fun compareTo(other: Kost2ArtDO): Int {
        return id!!.compareTo(other.id!!)
    }
}
