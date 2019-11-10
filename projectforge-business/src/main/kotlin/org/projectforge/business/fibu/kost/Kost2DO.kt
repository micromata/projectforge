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

import com.fasterxml.jackson.annotation.JsonCreator
import de.micromata.genome.db.jpa.history.api.WithHistory
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.ClassBridge
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import javax.persistence.*

@Entity
@Indexed
@ClassBridge(name = "nummer", impl = HibernateSearchKost2Bridge::class)
@Table(name = "T_FIBU_KOST2", uniqueConstraints = [UniqueConstraint(columnNames = ["nummernkreis", "bereich", "teilbereich", "kost2_art_id", "tenant_id"])], indexes = [Index(name = "idx_fk_t_fibu_kost2_kost2_art_id", columnList = "kost2_art_id"), Index(name = "idx_fk_t_fibu_kost2_projekt_id", columnList = "projekt_id"), Index(name = "idx_fk_t_fibu_kost2_tenant_id", columnList = "tenant_id")])
@WithHistory
@NamedQueries(
        NamedQuery(name = Kost2DO.FIND_BY_NK_BEREICH_TEILBEREICH_KOST2ART,
                query = "from Kost2DO where nummernkreis=:nummernkreis and bereich=:bereich and teilbereich=:teilbereich and kost2Art.id=:kost2ArtId"),
        NamedQuery(name = Kost2DO.FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_KOST2ART,
                query = "from Kost2DO where nummernkreis=:nummernkreis and bereich=:bereich and teilbereich=:teilbereich and kost2Art.id=:kost2ArtId and id!=:id"),
        NamedQuery(name = Kost2DO.FIND_ACTIVES_BY_NK_BEREICH_TEILBEREICH,
                query = "from Kost2DO where nummernkreis=:nummernkreis and bereich=:bereich and teilbereich=:teilbereich and kost2Art.id=:kost2ArtId and (kostentraegerStatus='ACTIVE' or kostentraegerStatus is null) order by kost2Art.id"))
open class Kost2DO() : DefaultBaseDO(), ShortDisplayNameCapable, Comparable<Kost2DO> {

    companion object {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        @JvmStatic
        fun createFrom(value: Int): Kost2DO {
            val kost2 = Kost2DO()
            kost2.id = value
            return kost2
        }

        internal const val FIND_BY_NK_BEREICH_TEILBEREICH_KOST2ART = "Kost2DO_FindByNKBereichTeilbereichKost2Art"
        internal const val FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_KOST2ART = "Kost2DO_FindOtherByNKBereichTeilbereichKost2Art"
        internal const val FIND_ACTIVES_BY_NK_BEREICH_TEILBEREICH = "Kost2DO_FindActivesByNKBereichTeilbereich"
    }

    @PropertyInfo(i18nKey = "status")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var kostentraegerStatus: KostentraegerStatus? = null

    /**
     * Nummernkreis entspricht der ersten Ziffer.
     */
    @get:Column(name = "nummernkreis")
    open var nummernkreis: Int = 0

    /**
     * Bereich entspricht der 2.-4. Ziffer.
     */
    @get:Column(name = "bereich")
    open var bereich: Int = 0

    /**
     * Teilbereich entspricht der 5.-6. Ziffer.
     */
    @get:Column(name = "teilbereich")
    open var teilbereich: Int = 0

    @PropertyInfo(i18nKey = "fibu.kost2.art")
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "kost2_art_id", nullable = false)
    open var kost2Art: Kost2ArtDO? = null

    @PropertyInfo(i18nKey = "fibu.kost2.workFraction")
    @get:Column(name = "work_fraction", scale = 5, precision = 10)
    open var workFraction: BigDecimal? = null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 4000)
    open var description: String? = null

    /**
     * Optionale Kommentare zum Kostenträger.
     */
    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = 4000)
    open var comment: String? = null

    /**
     * Projekt kann gegeben sein. Wenn Kostenträger zu einem Projekt hinzugehört, dann sind auf jeden Fall die ersten 6
     * Ziffern identisch mit der Projektnummer.
     */
    @PropertyInfo(i18nKey = "fibu.projekt")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "projekt_id")
    open var projekt: ProjektDO? = null

    /**
     * @see KostFormatter.getKostAsInt
     */
    val nummer: Int?
        @Transient
        get() = KostFormatter.getKostAsInt(nummernkreis, bereich, teilbereich, kost2Art!!.id!!)

    /**
     * Format: #.###.##.##
     *
     * @see KostFormatter.format
     */
    val formattedNumber: String
        @Transient
        get() = KostFormatter.format(this)

    /**
     * @see KostFormatter.formatToolTip
     */
    val toolTip: String
        @Transient
        get() = KostFormatter.formatToolTip(this)

    val kost2ArtId: Int?
        @Transient
        get() = if (this.kost2Art == null) {
            null
        } else kost2Art!!.id

    val projektId: Int?
        @Transient
        get() = if (this.projekt == null) {
            null
        } else projekt!!.id

    /**
     * @see KostFormatter.format
     * @see org.projectforge.framework.persistence.api.ShortDisplayNameCapable.getShortDisplayName
     */
    @Transient
    override fun getShortDisplayName(): String {
        return KostFormatter.format(this)
    }

    @Transient
    fun isEqual(nummernkreis: Int, bereich: Int, teilbereich: Int, kost2Art: Int): Boolean {
        return (this.nummernkreis == nummernkreis
                && this.bereich == bereich
                && this.teilbereich == teilbereich
                && this.kost2Art!!.id == kost2Art)
    }

    /**
     * return true if nummernkreis, bereich, teilbereich and kost2Art is equal, otherwise false;
     *
     * @see java.lang.Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (other is Kost2DO) {
            val o = other as Kost2DO?
            if (this.nummernkreis == o!!.nummernkreis && this.bereich == o.bereich
                    && this.teilbereich == o.teilbereich) {
                return this.kost2Art == o.kost2Art
            }
        }
        return false
    }

    /**
     * Uses HashCodeBuilder with property nummernkreis, bereich, teilbereich and kost2Art.
     *
     * @see java.lang.Object.hashCode
     * @see HashCodeBuilder.append
     * @see HashCodeBuilder.append
     */
    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(this.nummernkreis).append(this.bereich).append(this.teilbereich).append(this.kost2Art)
        return hcb.toHashCode()
    }

    /**
     * Compares shortDisplayName.
     *
     * @see .getShortDisplayName
     * @see java.lang.Comparable.compareTo
     */
    override fun compareTo(other: Kost2DO): Int {
        return this.shortDisplayName.compareTo(other.shortDisplayName)
    }
}
