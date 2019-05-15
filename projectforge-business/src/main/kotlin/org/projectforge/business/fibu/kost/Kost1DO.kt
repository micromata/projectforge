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

package org.projectforge.business.fibu.kost

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.UniqueConstraint

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.ClassBridge
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Index
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.Store
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO

import de.micromata.genome.db.jpa.history.api.WithHistory
import org.projectforge.common.anots.PropertyInfo

@Entity
@Indexed
@ClassBridge(name = "nummer", impl = HibernateSearchKost1Bridge::class)
@Table(name = "T_FIBU_KOST1", uniqueConstraints = [UniqueConstraint(columnNames = ["nummernkreis", "bereich", "teilbereich", "endziffer", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_kost1_tenant_id", columnList = "tenant_id")])
@WithHistory
class Kost1DO : DefaultBaseDO(), ShortDisplayNameCapable {

    @PropertyInfo(i18nKey = "status")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 30)
    var kostentraegerStatus: KostentraegerStatus? = null

    /**
     * Nummernkreis entspricht der ersten Ziffer.
     *
     * @return
     */
    @get:Column(name = "nummernkreis", length = 1)
    var nummernkreis: Int = 0

    /**
     * Bereich entspricht der 2.-4. Ziffer.
     *
     * @return
     */
    @get:Column(name = "bereich", length = 3)
    var bereich: Int = 0

    /**
     * Teilbereich entspricht der 5.-6. Ziffer.
     *
     * @return
     */
    @get:Column(name = "teilbereich", length = 2)
    var teilbereich: Int = 0

    @get:Column(name = "endziffer", length = 2)
    var endziffer: Int = 0

    /**
     * Optionale Kommentare zum Kostenträger.
     *
     * @return
     */
    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 4000)
    var description: String? = null

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

    override fun equals(o: Any?): Boolean {
        if (o is Kost1DO) {
            val other = o as Kost1DO?
            return (this.nummernkreis == other!!.nummernkreis && this.bereich == other.bereich
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
        private val serialVersionUID = -6534347300453425760L
    }
}
