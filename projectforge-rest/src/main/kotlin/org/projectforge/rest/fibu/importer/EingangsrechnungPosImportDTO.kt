/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.fibu.importer

import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.PaymentType
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.Konto
import org.projectforge.rest.dto.Kost1
import org.projectforge.rest.dto.Kost2
import org.projectforge.rest.importer.ImportPairEntry
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KProperty

/**
 * Data Transfer Object for importing incoming invoice positions.
 * Datev (Liefernantenrechnung, alle Positionen) Excel format (Blatt Blatt 1 - lieferantenrechnungen):
 * * Periode (01.05.2025-31.05.2025)
 * * Betrag (brutto)
 * * Währung (EUR, USD, ...)
 * * Datum (dd.MM.)
 * * RENR (Rechnungs-Nr.)
 * * LieferantName
 * * LieferantKonto
 * * Fällig_am
 * * gezahlt_am
 * * Ware/Leistung (Text)
 * * Steuer%
 * * KOST1
 * * KOST2
 * export (nicht alle Positionen, nur Summe), Blatt 1 - export_final:
 * * Geschäftspartner-Name
 * * Geschäftspartner-Konto
 * * Rechnungsbetrag (brutto)
 * * WKZ (Währung)
 * * Rechnungs-Nr.
 * * Rechnungsdatum
 * * Bezahlt (ja oder leeres Feld)
 * * BezahltAm
 * * Skonto-Betrag 1
 * * Fällig mit Skonto 1
 * * Skonto 1 in %
 * * Fällig ohne Skonto
 * * IBAN
 * * BIC
 * * Belegtyp (Ueberweisungen, Kreditkartenbelege, Lastschrift, ...)
 */
class EingangsrechnungPosImportDTO(
    var kreditor: String? = null, // LieferantName,
    var konto: Konto? = null, // LieferantKonto
    var referenz: String? = null, // RENR, Rechnungs-Nr.
    var betreff: String? = null, // Ware/Leistung
    var datum: LocalDate? = null,
    var faelligkeit: LocalDate? = null,
    var bezahlDatum: LocalDate? = null,
    var vatAmountSum: BigDecimal? = null,
    var grossSum: BigDecimal? = null,
    var currency: String? = null, // Währung (EUR, USD, ...)
    var zahlBetrag: BigDecimal? = null,
    var discountMaturity: LocalDate? = null, // Fällig mit Skonto 1
    var discountPercent: BigDecimal? = null, // Skonto 1 in %
    var iban: String? = null,
    var bic: String? = null,
    var receiver: String? = null,
    var paymentType: PaymentType? = null,
    var customernr: String? = null,
    var bemerkung: String? = null,
    var kost1: Kost1? = null,
    var kost2: Kost2? = null,
    var periodFrom: LocalDate? = null,
    var periodUntil: LocalDate? = null,
) : BaseDTO<EingangsrechnungDO>(), ImportPairEntry.Modified<EingangsrechnungPosImportDTO> {

    override val properties: Array<KProperty<*>>
        get() = arrayOf(
            EingangsrechnungDO::kreditor,
            EingangsrechnungDO::referenz,
            EingangsrechnungDO::betreff,
            EingangsrechnungDO::datum,
            EingangsrechnungDO::faelligkeit,
            EingangsrechnungDO::bezahlDatum,
            EingangsrechnungDO::zahlBetrag,
            EingangsrechnungDO::iban,
            EingangsrechnungDO::bic,
            EingangsrechnungDO::receiver,
            EingangsrechnungDO::paymentType,
            EingangsrechnungDO::customernr,
            EingangsrechnungDO::bemerkung
        )

    override fun copyFrom(src: EingangsrechnungDO) {
        super.copyFrom(src)
        this.kreditor = src.kreditor
        this.referenz = src.referenz
        this.betreff = src.betreff
        this.datum = src.datum
        this.faelligkeit = src.faelligkeit
        this.bezahlDatum = src.bezahlDatum
        this.zahlBetrag = src.zahlBetrag
        this.iban = src.iban
        this.bic = src.bic
        this.receiver = src.receiver
        this.paymentType = src.paymentType
        this.customernr = src.customernr
        this.bemerkung = src.bemerkung
        // this.kost1 = src.kost1
        // this.kost2 = src.kost2
    }

    override fun copyTo(obj: EingangsrechnungDO) {
        if (this.id != null) obj.id = this.id
        obj.kreditor = this.kreditor
        obj.referenz = this.referenz
        obj.betreff = this.betreff
        obj.datum = this.datum
        obj.faelligkeit = this.faelligkeit
        obj.bezahlDatum = this.bezahlDatum
        obj.zahlBetrag = this.zahlBetrag
        obj.iban = this.iban
        obj.bic = this.bic
        obj.receiver = this.receiver
        obj.paymentType = this.paymentType
        obj.customernr = this.customernr
        obj.bemerkung = this.bemerkung
    }
}
