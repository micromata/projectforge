package org.projectforge.rest.dto

import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.SHType

class Buchungssatz(
        var satznr: String? = null,
        var menge: String? = null,
        var beleg: String? = null,
        var sh: SHType? = null,
        var text: String? = null,
        var comment: String? = null
): BaseDTO<BuchungssatzDO>(){
    var kost1: Kost1? = Kost1()
    var kost2: Kost2? = Kost2()
    var konto: Konto? = Konto()
    var gegenKonto: Konto? = Konto()

    fun initialize(obj: BuchungssatzDO){
        copyFrom(obj)

        if(obj.year != null && obj.month != null){
            this.satznr = obj.formattedSatzNummer
        }

        if(obj.kost1 != null){
            this.kost1!!.copyFrom(obj.kost1!!)
        }

        if(obj.kost2 != null){
            this.kost2!!.copyFrom(obj.kost2!!)
        }

        if(obj.konto != null){
            this.konto!!.initialize(obj.konto!!)
        }

        if(obj.gegenKonto != null){
            this.gegenKonto!!.initialize(obj.gegenKonto!!)
        }
    }
}