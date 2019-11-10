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

import de.micromata.genome.db.jpa.history.api.WithHistory
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.bridge.builtin.IntegerBridge
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.util.*
import javax.persistence.*

@Entity
@Indexed
@Table(name = "T_FIBU_KONTO", uniqueConstraints = [UniqueConstraint(columnNames = ["nummer", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_konto_tenant_id", columnList = "tenant_id")])
@WithHistory
@NamedQueries(
        NamedQuery(name = KontoDO.FIND_BY_NUMMER, query = "from KontoDO where nummer=:nummer"))
open class KontoDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @PropertyInfo(i18nKey = "fibu.konto.nummer")
    @Field(analyze = Analyze.NO, bridge = FieldBridge(impl = IntegerBridge::class))
    @get:Column(name = "nummer", nullable = false)
    open var nummer: Int? = null

    @PropertyInfo(i18nKey = "fibu.konto.bezeichnung")
    @Field
    @get:Column(length = 255, nullable = false)
    open var bezeichnung: String? = null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(name = "description", length = 4000, nullable = true)
    open var description: String? = null

    @PropertyInfo(i18nKey = "status")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 10)
    open var status: KontoStatus? = null

    /**
     * Formats the account as string: "[nummer] [title]", e. g. "11000 Micromata GmbH"
     */
    fun formatKonto(): String {
        return formatKonto(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is KontoDO) {
            return if (!Objects.equals(this.nummer, other.nummer)) {
                false
            } else Objects.equals(this.bezeichnung, other.bezeichnung)
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(this.nummer)
        hcb.append(this.bezeichnung)
        return hcb.toHashCode()
    }

    @Transient
    override fun getShortDisplayName(): String {
        return nummer.toString()
    }

    companion object {
        internal const val FIND_BY_NUMMER = "KontoDO_FindByNummer"

        /**
         * Formats the account as string: "[nummer] [title]", e. g. "11000 Micromata GmbH"
         *
         * @param konto
         */
        fun formatKonto(konto: KontoDO): String {
            return konto.nummer.toString() + " " + konto.bezeichnung
        }
    }
}
