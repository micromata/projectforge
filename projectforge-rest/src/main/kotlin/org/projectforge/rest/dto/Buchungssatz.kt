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
            val kost1 = Kost1()
            kost1.copyFrom(it)
            this.kost1 = kost1
        }
        src.kost2?.let {
            val kost2 = Kost2()
            kost2.copyFrom(it)
            this.kost2 = kost2
        }
        src.konto?.let {
            val konto = Konto()
            konto.copyFrom(it)
            this.konto = konto
        }
        src.gegenKonto?.let {
            val konto = Konto()
            konto.copyFrom(it)
            this.gegenKonto = konto
        }
    }
}
