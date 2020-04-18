package org.projectforge.rest.dto

import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.SHType

class Buchungssatz(
        var satznr: String? = null,
        var menge: String? = null,
        var beleg: String? = null,
        var sh: SHType? = null,
        var text: String? = null,
        var comment: String? = null,
        var kost1: Kost1? = null,
        var kost2: Kost2? = null,
        var konto: Konto? = null,
        var gegenKonto: Konto? = null
) : BaseDTO<BuchungssatzDO>() {

    override fun copyFrom(src: BuchungssatzDO) {
        super.copyFrom(src)
        if (src.year != null && src.month != null) {
            this.satznr = src.formattedSatzNummer
        }
        src.kost1?.let {
            this.kost1 = Kost1(it)
        }
        src.kost2?.let {
            this.kost2 = Kost2(it)
        }
        src.konto?.let {
            this.konto = Konto(it)
        }
        src.gegenKonto?.let {
            this.gegenKonto = Konto(it)
        }
    }
}
