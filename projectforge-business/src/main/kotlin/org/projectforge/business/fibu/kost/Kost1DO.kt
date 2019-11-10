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

import de.micromata.genome.db.jpa.history.api.WithHistory
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.ClassBridge
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.*

@Entity
@Indexed
@ClassBridge(name = "nummer", impl = HibernateSearchKost1Bridge::class)
@Table(name = "T_FIBU_KOST1", uniqueConstraints = [UniqueConstraint(columnNames = ["nummernkreis", "bereich", "teilbereich", "endziffer", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_kost1_tenant_id", columnList = "tenant_id")])
@WithHistory
@NamedQueries(
        NamedQuery(name = Kost1DO.FIND_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER,
                query = "from Kost1DO where nummernkreis=:nummernkreis and bereich=:bereich and teilbereich=:teilbereich and endziffer=:endziffer"),
        NamedQuery(name = Kost1DO.FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER,
                query = "from Kost1DO where nummernkreis=:nummernkreis and bereich=:bereich and teilbereich=:teilbereich and endziffer=:endziffer and id!=:id"))
open class Kost1DO : DefaultBaseDO(), ShortDisplayNameCapable {

    @PropertyInfo(i18nKey = "status")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    open var kostentraegerStatus: KostentraegerStatus? = null

    /**
     * Nummernkreis entspricht der ersten Ziffer.
     *
     * @return
     */
    @get:Column(name = "nummernkreis", length = 1)
    open var nummernkreis: Int = 0

    /**
     * Bereich entspricht der 2.-4. Ziffer.
     *
     * @return
     */
    @get:Column(name = "bereich", length = 3)
    open var bereich: Int = 0

    /**
     * Teilbereich entspricht der 5.-6. Ziffer.
     *
     * @return
     */
    @get:Column(name = "teilbereich", length = 2)
    open var teilbereich: Int = 0

    @get:Column(name = "endziffer", length = 2)
    open var endziffer: Int = 0

    /**
     * Optionale Kommentare zum Kostenträger.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 4000)
    open var description: String? = null

    /**
     * Format: #.###.##.##
     *
     * @see KostFormatter.format
     */
    val formattedNumber: String
        @Transient
        get() = KostFormatter.format(this)

    /**
     * @see KostFormatter.getKostAsInt
     */
    val nummer: Int?
        @Transient
        get() = KostFormatter.getKostAsInt(nummernkreis, bereich, teilbereich, endziffer)

    @Transient
    override fun getShortDisplayName(): String {
        return KostFormatter.format(this)
    }

    /**
     * return true if nummernkreis, bereich, teilbereich and endziffer is equal, otherwise false;
     *
     * @see java.lang.Object.equals
     */

    override fun equals(other: Any?): Boolean {
        if (other is Kost1DO) {
            return (this.nummernkreis == other.nummernkreis && this.bereich == other.bereich
                    && this.teilbereich == other.teilbereich && this.endziffer == other.endziffer)
        }
        return false
    }

    /**
     * Uses HashCodeBuilder with property nummernkreis, bereich, teilbereich and endziffer.
     *
     * @see java.lang.Object.hashCode
     * @see HashCodeBuilder.append
     */
    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(this.nummernkreis).append(this.bereich).append(this.teilbereich).append(this.endziffer)
        return hcb.toHashCode()
    }

    companion object {
        const val FIND_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER = "Kost1DO_FindByNKBereichTeilbereichEndziffer"
        const val FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER = "Kost1DO_FindOtherByNKBereichTeilbereichEndziffer"
    }
}
