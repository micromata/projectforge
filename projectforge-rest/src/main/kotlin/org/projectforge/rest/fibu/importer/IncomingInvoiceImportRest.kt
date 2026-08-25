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

package org.projectforge.rest.fibu.importer

import mu.KotlinLogging
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.ImportPairEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * The layout-free, JSON returning incoming-invoice (Kreditor) CSV import, the hand built sibling of the
 * UILayout based [EingangsrechnungUploadPageRest] / [IncomingInvoicePosImportPageRest] pair.
 *
 * It is a thin concrete subclass of [AbstractImportRest]: the DATEV CSV parsing, the access right and the
 * job enqueueing are lifted from the two legacy classes and call the same collaborators; everything else —
 * the endpoints and the [org.projectforge.rest.importer.ImportView] wire shape — lives in the base.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/incomingInvoiceImport")
class IncomingInvoiceImportRest :
    AbstractImportRest<EingangsrechnungPosImportDTO, EingangsrechnungImportStorage>() {

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    override val fileExtensions = arrayOf("csv")

    override val maxFileUploadSizeMB = 10L // in MB

    override fun checkRight() {
        accessChecker.hasLoggedInUserRight(EingangsrechnungDao.USER_RIGHT_ID, true, UserRightValue.READWRITE)
    }

    override fun proceedUpload(inputStream: InputStream, filename: String): EingangsrechnungImportStorage {
        if (filename.endsWith("xls", ignoreCase = true) || filename.endsWith("xlsx", ignoreCase = true)) {
            throw IllegalArgumentException("Excel format not supported for incoming invoices. Please use CSV format.")
        }
        val storage = EingangsrechnungImportStorage(DATEV_IMPORT_SETTINGS)
        storage.filename = filename
        // Parse the CSV with the consolidated importer that handles all processing in one step.
        IncomingInvoiceCsvImporter(kostCache, kontoCache).parse(inputStream, storage, storage.importSettings.charSet)
        return storage
    }

    override fun import(
        storage: EingangsrechnungImportStorage,
        selectedEntries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>,
    ): Int {
        log.info { "Enqueueing import of #${selectedEntries.size} incoming-invoice entries." }
        return jobHandler.addJob(
            EingangsrechnungImportJob(
                eingangsrechnungDao,
                selectedEntries,
                importStorage = storage,
            )
        ).id
    }

    override fun extraViewMeta(storage: EingangsrechnungImportStorage): Map<String, Any> {
        return mapOf("isPositionBasedImport" to storage.isPositionBasedImport)
    }

    companion object {
        /**
         * DATEV CSV field mappings with German column names and formatting, lifted verbatim from
         * [EingangsrechnungUploadPageRest].
         */
        private val DATEV_IMPORT_SETTINGS = """
            encoding=UTF-8
            bemerkung=Bemerkung|Freier Text|Notiz
            besonderheiten=Besonderheiten
            betreff=Betreff|Ware/Leistung
            bezahlDatum=BezahltAm|Bezahlt|gezahlt_am|:dd.MM.yyyy|:dd.MM.yy
            bic=BIC
            currency=WKZ|Währung
            customernr=Kunden-Nr.|Kundennummer
            datum=Datum|Rechnungsdatum|:dd.MM.yyyy|:dd.MM.yy
            discountMaturity=Fällig mit Skonto 1|Skonto Fälligkeit|Skto_Fällig_am|:dd.MM.yyyy|:dd.MM.yy
            discountPercent=Skonto 1 in %|Skto_Proz|Skonto Prozent|Skonto in %|:#.##0,0#|:#0
            faelligkeit=Fällig_am|Fällig ohne Skonto|Fälligkeit|:dd.MM.yyyy|:dd.MM.yy
            grossSum=Rechnungsbetrag|Betrag|:#.##0,0#|:#0,0#
            zahlBetrag=Zahlbetrag|g:#,##0.0#|:#0.0#
            zahlungsZielInTagen=Zahlungsziel|:#0
            taxRate=Steuer in %|Steuer%|#.##0,0#|:#0
            iban=IBAN
            konto=LieferantKonto|Geschäftspartner-Konto
            kreditor=Geschäftspartner-Name|Kreditor|LieferantName
            paymentType=Zahlungsart|Belegtyp
            receiver=Empfänger
            referenz=Rechnungs-Nr.|Referenz|Interne Re.-Nr.|RENR
            leistungsdatum=Leistungsdatum|:dd.MM.yyyy|:dd.MM.yy
            kost1=KOST 1|KOST1
            kost2=KOST 2|KOST2
            periode=Periode
            iban=IBAN
            bic=BIC
        """.trimIndent()
    }
}
