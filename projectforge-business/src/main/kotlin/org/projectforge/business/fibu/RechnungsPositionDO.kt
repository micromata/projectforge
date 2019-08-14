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

import com.fasterxml.jackson.annotation.JsonManagedReference
import org.hibernate.search.annotations.*
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.common.anots.PropertyInfo
import java.sql.Date
import javax.persistence.*

/**
 * Repräsentiert eine Position innerhalb eine Rechnung.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_fibu_rechnung_position", uniqueConstraints = [UniqueConstraint(columnNames = ["rechnung_fk", "number"])], indexes = [javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_position_auftrags_position_fk", columnList = "auftrags_position_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_position_rechnung_fk", columnList = "rechnung_fk"), javax.persistence.Index(name = "idx_fk_t_fibu_rechnung_position_tenant_id", columnList = "tenant_id")])
class RechnungsPositionDO : AbstractRechnungsPositionDO() {

    @get:JsonManagedReference
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "rechnung_fk", nullable = false)
    var rechnung : RechnungDO? = null

    @PropertyInfo(i18nKey = "fibu.auftrag.position")
    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "auftrags_position_fk")
    var auftragsPosition: AuftragsPositionDO? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.type")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "period_of_performance_type", length = 10)
    var periodOfPerformanceType: PeriodOfPerformanceType? = PeriodOfPerformanceType.SEEABOVE

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.from")
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_begin")
    var periodOfPerformanceBegin: Date? = null

    @PropertyInfo(i18nKey = "fibu.periodOfPerformance.to")
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "period_of_performance_end")
    var periodOfPerformanceEnd: Date? = null

    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @get:JoinColumn(name = "rechnungs_pos_fk")
    @get:OrderColumn(name = "index")
    override var kostZuweisungen: MutableList<KostZuweisungDO>? = null

    override val rechnungId: Int?
        @Transient
        get() = rechnung?.id

    @Transient
    override fun setThis(kostZuweisung: KostZuweisungDO) {
        kostZuweisung.rechnungsPosition = this
    }

    override fun newInstance(): AbstractRechnungsPositionDO {
        return RechnungsPositionDO()
    }
}
