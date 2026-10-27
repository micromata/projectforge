/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.business.fibu.kost.reallocation.Kost2ShareSource
import java.math.BigDecimal
import java.time.LocalDate

class Buchungssatz(
        var satznr: String? = null,
        var year: Int? = null,
        var month: Int? = null,
        var datum: LocalDate? = null,
        var betrag: BigDecimal? = null,
        var menge: String? = null,
        var beleg: String? = null,
        var sh: SHType? = null,
        var text: String? = null,
        var comment: String? = null,
        var kost1: Kost1? = null,
        var kost2: Kost2? = null,
        var konto: Konto? = null,
        var gegenKonto: Konto? = null,
        /**
         * True, if the record is reallocated (has kost2 shares). For a share of the effective view (report
         * drill-down), [betrag] and [kost2] are the ones of the share, [originalBetrag] and [originalKost2] the ones
         * of the stored record.
         */
        var reallocated: Boolean = false,
        var originalBetrag: BigDecimal? = null,
        var originalKost2: Kost2? = null,
        /** The kost2 shares of a reallocated record (only filled for the edit view). */
        var shares: List<Kost2Share>? = null,
) : BaseDTO<BuchungssatzDO>() {

    /** A kost2 share of a reallocated record. */
    class Kost2Share(
        var kost2: Kost2? = null,
        var betrag: BigDecimal? = null,
        var source: Kost2ShareSource? = null,
        var runId: Long? = null,
        var runDescription: String? = null,
    )

    override fun copyFrom(src: BuchungssatzDO) {
        super.copyFrom(src)
        if (src.year != null && src.month != null) {
            this.satznr = src.formattedSatzNummer
        }
        this.kost1 = src.kost1?.let {
            Kost1(it)
        }
        this.kost2 = src.kost2?.let {
            Kost2(it)
        }
        this.konto = src.konto?.let {
            Konto(it)
        }
        this.gegenKonto = src.gegenKonto?.let {
            Konto(it)
        }
        src.originalRecord?.let { original ->
            this.reallocated = true
            this.originalBetrag = original.betrag
            this.originalKost2 = original.kost2?.let { Kost2(it) }
        }
    }
}
