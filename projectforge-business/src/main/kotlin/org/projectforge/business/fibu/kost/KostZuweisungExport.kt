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

package org.projectforge.business.fibu.kost

import mu.KotlinLogging
import org.projectforge.business.excel.ContentProvider
import org.projectforge.business.excel.ExportColumn
import org.projectforge.business.excel.ExportWorkbook
import org.projectforge.business.excel.I18nExportColumn
import org.projectforge.business.excel.PropertyMapping
import org.projectforge.business.fibu.AbstractRechnungDO
import org.projectforge.business.fibu.AbstractRechnungsPositionDO
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.ProjektFormatter
import org.projectforge.business.fibu.RechnungCalculator
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungService
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.common.StringHelper
import org.projectforge.common.extensions.isZeroOrNull
import org.projectforge.export.MyXlsContentProvider
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.CurrencyHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.web.WicketSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

/**
 * For excel export.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class KostZuweisungExport {
    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var rechnungService: RechnungService

    private inner class MyContentProvider(workbook: ExportWorkbook?) : MyXlsContentProvider(workbook) {
        override fun newInstance(): ContentProvider {
            return MyContentProvider(this.workbook)
        }
    }

    private enum class InvoicesCol(theTitle: String, width: Int) {
        BRUTTO("fibu.common.brutto", MyXlsContentProvider.LENGTH_CURRENCY),
        VAT("fibu.common.vat", MyXlsContentProvider.LENGTH_PERCENT),
        KONTO("fibu.buchungssatz.konto", 14),
        REFERENZ("fibu.common.reference", MyXlsContentProvider.LENGTH_STD),
        DATE("date", MyXlsContentProvider.LENGTH_DATE),
        GEGENKONTO("fibu.buchungssatz.gegenKonto", 14),
        KOST1("fibu.kost1", MyXlsContentProvider.LENGTH_KOSTENTRAEGER),
        KOST2("fibu.kost2", MyXlsContentProvider.LENGTH_KOSTENTRAEGER),
        TEXT("description", MyXlsContentProvider.LENGTH_EXTRA_LONG),
        BETREFF("fibu.rechnung.betreff", MyXlsContentProvider.LENGTH_EXTRA_LONG),
        KORREKTUR("fibu.common.fehlBetrag", MyXlsContentProvider.LENGTH_CURRENCY);

        val theTitle: String?

        val width: Int

        init {
            this.theTitle = theTitle
            this.width = width.toShort().toInt()
        }
    }

    /**
     * Export all cost assignments of the given invoices as excel list.
     */
    fun exportRechnungen(list: List<AbstractRechnungDO>, sheetTitle: String): ByteArray? {
        rechnungService.fetchPositionen(list)
        val kostZuweisungenMap = rechnungService.selectKostzuweisungen(list).groupBy { it.rechnungsPosition?.id }
        val kostZuweisungen = mutableListOf<KostZuweisungDO>()
        list.forEach { rechnung ->
            rechnung.abstractPositionen?.forEach { position ->
                val zuweisungen = kostZuweisungenMap[position.id]
                if (zuweisungen.isNullOrEmpty() || zuweisungen.all { it.netto.isZeroOrNull() }) {
                    kostZuweisungen.add(KostZuweisungDO().also { // Empty kostzuweisung:
                        it.rechnungsPosition = position as RechnungsPositionDO
                    })
                } else {
                    kostZuweisungen.addAll(zuweisungen.filter { !it.netto.isZeroOrNull() })
                }
            }
        }
        return export(list, kostZuweisungen, sheetTitle)
    }

    /**
     * Export all cost assignments of the given invoices as excel list.
     */
    fun exportEingangsRechnungen(list: List<AbstractRechnungDO>, sheetTitle: String): ByteArray? {
        rechnungService.fetchPositionen(list)
        val kostZuweisungenMap =
            rechnungService.selectKostzuweisungen(list).groupBy { it.eingangsrechnungsPosition?.id }
        val kostZuweisungen = mutableListOf<KostZuweisungDO>()
        list.forEach { rechnung ->
            rechnung.abstractPositionen?.forEach { position ->
                val zuweisungen = kostZuweisungenMap[position.id]
                if (zuweisungen.isNullOrEmpty() || zuweisungen.all { it.netto.isZeroOrNull() }) {
                    kostZuweisungen.add(KostZuweisungDO().also { // Empty kostzuweisung:
                        it.eingangsrechnungsPosition = position as EingangsrechnungsPositionDO
                    })
                } else {
                    kostZuweisungen.addAll(zuweisungen.filter { !it.netto.isZeroOrNull() })
                }
            }
        }
        return export(list, kostZuweisungen, sheetTitle)
    }

    /**
     * Exports the filtered list as table.
     */
    fun export(
        invoices: List<AbstractRechnungDO>,
        kostZuweisungen: List<KostZuweisungDO>,
        sheetTitle: String?
    ): ByteArray? {
        log.info("Exporting kost zuweisung list.")
        val xls = ExportWorkbook()
        val contentProvider: ContentProvider = MyContentProvider(xls)
        // create a default Date format and currency column
        xls.setContentProvider(contentProvider)

        val sheet = xls.addSheet(sheetTitle)
        sheet.createFreezePane(0, 1)

        val cols = arrayOfNulls<ExportColumn>(InvoicesCol.entries.size)
        var i = 0
        for (col in InvoicesCol.entries) {
            cols[i++] = I18nExportColumn(col, col.theTitle, col.width)
        }

        // column property names
        sheet.setColumns(*cols)

        val sheetProvider = sheet.getContentProvider()
        sheetProvider.putFormat(InvoicesCol.BRUTTO, "#,##0.00;[Red]-#,##0.00")
        sheetProvider.putFormat(InvoicesCol.VAT, "#0%")
        sheetProvider.putFormat(InvoicesCol.KORREKTUR, "#,##0.00;[Red]-#,##0.00")
        sheetProvider.putFormat(InvoicesCol.KOST1, "#")
        sheetProvider.putFormat(InvoicesCol.KOST2, "#")
        sheetProvider.putFormat(InvoicesCol.DATE, "dd.MM.yyyy")

        invoices.forEach { RechnungCalculator.calculate(it, false) }

        for (zuweisung in kostZuweisungen) {
            val mapping = PropertyMapping()
            val position: AbstractRechnungsPositionDO?
            val rechnung: AbstractRechnungDO?
            val referenz: String?
            val text: String?
            if (zuweisung.rechnungsPosition != null) {
                position = zuweisung.rechnungsPosition
                rechnung = (position as RechnungsPositionDO).rechnung
                val r = rechnung
                referenz = r!!.nummer.toString()
                text = ProjektFormatter.Companion.formatProjektKundeAsString(r.projekt, r.kunde, r.kundeText)
            } else {
                position = zuweisung.eingangsrechnungsPosition
                rechnung = (position as EingangsrechnungsPositionDO).eingangsrechnung
                val r = rechnung
                referenz = r!!.referenz
                text = r.kreditor
            }

            val grossSum = position.info.grossSum

            var korrektur: BigDecimal? = null
            if (grossSum.compareTo(position.info.kostZuweisungGrossSum) != 0) {
                korrektur = CurrencyHelper.getGrossAmount(position.info.kostZuweisungNetFehlbetrag, position.vat)
                if (NumberHelper.isZeroOrNull(korrektur)) {
                    korrektur = null
                }
            }
            mapping.add(InvoicesCol.BRUTTO, zuweisung.brutto)
            if (NumberHelper.isNotZero(position.vat)) {
                mapping.add(InvoicesCol.VAT, position.vat)
            } else {
                mapping.add(InvoicesCol.VAT, BigDecimal.ZERO)
            }
            var konto: KontoDO? = null
            if (rechnung is RechnungDO) {
                konto = kontoCache.getKonto(rechnung)
            } else if (rechnung is EingangsrechnungDO) {
                konto = kontoCache.getKontoIfNotInitialized(rechnung.konto)
            }
            val kontoNummer = if ((konto != null)) konto.nummer else null
            mapping.add(InvoicesCol.KONTO, if (kontoNummer != null) kontoNummer else "")
            mapping.add(InvoicesCol.REFERENZ, StringHelper.removeNonDigitsAndNonASCIILetters(referenz))
            mapping.add(InvoicesCol.DATE, rechnung.datum)
            mapping.add(InvoicesCol.GEGENKONTO, "")
            mapping.add(InvoicesCol.KOST1, if (zuweisung.kost1 != null) zuweisung.kost1!!.nummer else "")
            mapping.add(InvoicesCol.KOST2, if (zuweisung.kost2 != null) zuweisung.kost2!!.nummer else "")
            mapping.add(InvoicesCol.TEXT, text)
            mapping.add(InvoicesCol.BETREFF, rechnung.betreff)
            mapping.add(InvoicesCol.KORREKTUR, korrektur)
            sheet.addRow(mapping.getMapping(), 0)
        }
        addAccounts(xls, contentProvider)
        return xls.getAsByteArray()
    }

    private enum class AccountsCol(theTitle: String, width: Int) {
        NUMBER("fibu.konto.nummer", 16),  //
        NAME("fibu.konto.bezeichnung", MyXlsContentProvider.LENGTH_STD),  //
        STATUS("status", 14),  //
        DATE_OF_LAST_MODIFICATION("lastUpdate", MyXlsContentProvider.LENGTH_TIMESTAMP),  //
        DATE_OF_CREATION("created", MyXlsContentProvider.LENGTH_TIMESTAMP),  //
        DESCRIPTION("comment", MyXlsContentProvider.LENGTH_EXTRA_LONG);

        val theTitle: String?

        val width: Int

        init {
            this.theTitle = theTitle
            this.width = width.toShort().toInt()
        }
    }

    private fun addAccounts(xls: ExportWorkbook, contentProvider: ContentProvider?) {
        val sheet = xls.addSheet(ThreadLocalUserContext.getLocalizedString("fibu.konto.konten"))
        sheet.createFreezePane(0, 1)

        val cols = arrayOfNulls<ExportColumn>(AccountsCol.entries.size)
        var i = 0
        for (col in AccountsCol.entries) {
            cols[i++] = I18nExportColumn(col, col.theTitle, col.width)
        }

        // column property names
        sheet.setColumns(*cols)

        val sheetProvider = sheet.getContentProvider()
        sheetProvider.putFormat(AccountsCol.DATE_OF_LAST_MODIFICATION, "dd.MM.yyyy HH:mm")
        sheetProvider.putFormat(AccountsCol.DATE_OF_CREATION, "dd.MM.yyyy HH:mm")
        sheetProvider.putFormat(AccountsCol.NUMBER, "#")

        val filter = QueryFilter()
        filter.addOrder(SortProperty.Companion.desc("lastUpdate"))
        val list = WicketSupport.get(KontoDao::class.java).select(filter)

        val mapping = PropertyMapping()
        for (konto in list) {
            mapping.add(AccountsCol.NUMBER, konto.nummer)
            mapping.add(AccountsCol.NAME, konto.bezeichnung)
            mapping.add(AccountsCol.DATE_OF_LAST_MODIFICATION, konto.lastUpdate)
            mapping.add(AccountsCol.DATE_OF_CREATION, konto.created)
            var status = ""
            if (konto.deleted) {
                status = ThreadLocalUserContext.getLocalizedString("deleted")
            } else if (konto.status != null) {
                status = ThreadLocalUserContext.getLocalizedString(konto.status!!.i18nKey)
            }
            mapping.add(AccountsCol.STATUS, status)
            mapping.add(AccountsCol.DESCRIPTION, konto.description)
            sheet.addRow(mapping.getMapping(), 0)
        }
    }
}
