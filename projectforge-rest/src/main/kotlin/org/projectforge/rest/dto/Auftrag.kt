package org.projectforge.rest.dto

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.LocalDate

class Auftrag(
        var nummer: Int? = null,
        var kunde: Kunde? = Kunde(),
        var projekt: Projekt? = Projekt(),
        var titel: String? = null,
        var positionen: MutableList<AuftragsPositionDO>? = null,
        var personDays: BigDecimal? = null,
        var referenz: String? = null,
        var assignedPersons: String? = null,
        var erfassungsDatum: LocalDate? = null,
        var entscheidungsDatum: LocalDate? = null,
        var nettoSumme: BigDecimal? = null,
        var beauftragtNettoSumme: BigDecimal? = null,
        var fakturiertSum: BigDecimal? = null,
        var zuFakturierenSum: BigDecimal? = null,
        var periodOfPerformanceBegin: LocalDate? = null,
        var periodOfPerformanceEnd: LocalDate? = null,
        var probabilityOfOccurrence: Int? = null,
        var auftragsStatus: AuftragsStatus? = null
): BaseDTO<AuftragDO>() {
    var pos: String? = null

    var formattedNettoSumme: String? = null
    var formattedBeauftragtNettoSumme: String? = null
    var formattedFakturiertSum: String? = null
    var formattedZuFakturierenSum: String? = null

    override fun copyFrom(src: AuftragDO) {
        super.copyFrom(src)

        if(src.kunde != null){
            kunde!!.initialize(src.kunde!!)
        }

        if(src.projekt != null){
            projekt!!.initialize(src.projekt!!)
        }

        positionen = src.positionen
        personDays = src.personDays
        assignedPersons = src.assignedPersons
        formattedNettoSumme = formatBigDecimal(src.nettoSumme)
        formattedBeauftragtNettoSumme = formatBigDecimal(src.beauftragtNettoSumme)
        formattedFakturiertSum = formatBigDecimal(src.fakturiertSum)
        formattedZuFakturierenSum = formatBigDecimal(src.zuFakturierenSum)

        pos = "#" + positionen?.size
    }

    private fun formatBigDecimal(value: BigDecimal?): String {
        value ?: return ""
        val df = DecimalFormat("#,###.## €")
        return df.format(value)
    }
}
