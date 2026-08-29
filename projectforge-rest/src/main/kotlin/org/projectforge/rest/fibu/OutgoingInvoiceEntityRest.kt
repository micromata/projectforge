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

package org.projectforge.rest.fibu

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.AbstractRechnungDO
import org.projectforge.business.fibu.AuftragAndRechnungDaoHelper
import org.projectforge.business.fibu.EInvoiceExportService
import org.projectforge.business.fibu.EInvoiceSellerConfig
import org.projectforge.business.fibu.InvoiceConfiguration
import org.projectforge.business.fibu.InvoiceService
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.PeriodOfPerformanceValidator
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.RechnungCalculator
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsStatistik
import org.projectforge.business.fibu.SearchFilterWithPeriodOfPerformance
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.KostZuweisungExport
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.SortPropertyComparator
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.FileCheck
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.ValidationUtils
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.core.saveOrUpdate
import org.projectforge.rest.dto.Kost2
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Rechnung
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The list of outgoing invoices (Debitorenrechnungen), layout free.
 *
 * `open` for the same reason [OrderEntityRest] is: Wicket asks for this bean through `WicketSupport`,
 * which proxies it (`RechnungEditForm` reads the attachment settings and the access checker from here).
 *
 * Answers the list, its statistics, its two exports, the multi selection the mass update page runs on
 * ([RechnungMultiSelectedPageRest]) and the read/write path of the hand built edit page of
 * `/next/invoice/[id]` — including its [recalculate]. It carries no `createEditLayout` any more, and with it
 * the legacy React page of this entity is gone: it only ever showed a read only header plus the attachment
 * list. Wicket's edit page is still reachable and writes through the same [RechnungDao].
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/outgoingInvoice")
open class OutgoingInvoiceEntityRest : // open: proxied by Wicket's WicketSupport.
    AbstractDTOEntityRest<RechnungDO, Rechnung, RechnungDao>(
        RechnungDao::class.java,
        "fibu.rechnung.title",
        // The recurring invoice is what this exists for. CLONE, not AUTOSAVE: the copy is offered for
        // editing and saved by the user, as `RechnungEditPage.cloneData` does it.
        cloneSupport = CloneSupport.CLONE,
    ) {

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var kostZuweisungExport: KostZuweisungExport

    @Autowired
    private lateinit var kostCache: KostCache

    /**
     * The two caches [getActiveKost2] and [checkKost2] resolve their arguments with, injected rather than taken
     * from [PfCaches.instance]: that static is replaced by `PfCaches.internalSetupForTestCases`, which builds
     * caches that never had their `persistenceService` injected, so every read through it fails in a test.
     */
    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var invoiceService: InvoiceService

    @Autowired
    private lateinit var sellerConfig: EInvoiceSellerConfig

    @Autowired
    private lateinit var invoiceConfig: InvoiceConfiguration

    @Autowired
    private lateinit var eInvoiceExportService: EInvoiceExportService

    @PostConstruct
    private fun postConstruct() {
        enableJcr()
    }

    /**
     * Builds a fresh [RechnungDO] instead of mutating the persisted one, for the reason
     * [OrderEntityRest.transformForDB] spells out: the persistence layer merges the posted object over the
     * database row, and `RechnungRight.hasAccess(obj, oldObj)` compares the two.
     *
     * Because of that merge, every field the DTO doesn't carry ends up as null in the database — the five
     * attachment columns are such fields (written by the attachment endpoints, not by this form), so they
     * are copied back from the database row, as `RechnungEditPage.update` does. So is `uiStatusAsXml`,
     * which belongs to the Wicket form alone.
     *
     * [RechnungDO.nummer] is deliberately *not* assigned here, unlike the order's: `RechnungDao` assigns it
     * itself on the transition from [org.projectforge.business.fibu.RechnungStatus.GEPLANT] to any other
     * status, and a `GUTSCHRIFTSANZEIGE_DURCH_KUNDEN` must have no number at all.
     */
    override fun transformForDB(dto: Rechnung): RechnungDO {
        val rechnungDO = RechnungDO()
        dto.copyTo(rechnungDO)
        if (rechnungDO.kunde != null) {
            // A customer chosen from the list wins over the free text one, see RechnungEditPage.onSaveOrUpdate.
            rechnungDO.kundeText = null
        }
        assignNumbersAndIndicesToNewRows(rechnungDO)
        dto.id?.let { id ->
            baseDao.find(id, checkAccess = false)?.let { dbObj ->
                rechnungDO.attachmentsCounter = dbObj.attachmentsCounter
                rechnungDO.attachmentsNames = dbObj.attachmentsNames
                rechnungDO.attachmentsIds = dbObj.attachmentsIds
                rechnungDO.attachmentsSize = dbObj.attachmentsSize
                rechnungDO.attachmentsLastUserAction = dbObj.attachmentsLastUserAction
                // Which position rows the Wicket form shows collapsed (`RechnungDao.writeUiStatusToXml`).
                // Another field the DTO doesn't carry, and this form has no use for: the collapsed state of
                // a row is the user's, not the invoice's, so projectforge-next keeps it in the browser
                // instead. Copied back so a save from here doesn't clear what Wicket remembered.
                rechnungDO.uiStatusAsXml = dbObj.uiStatusAsXml
            }
        }
        return rechnungDO
    }

    override fun transformFromDB(obj: RechnungDO, editMode: Boolean): Rechnung {
        // editMode is true only for the single-item detail/edit/clone endpoints, never for a list row, so
        // this is the one place to keep order book users out of the detail view without emptying their list.
        if (editMode) {
            checkSingleInvoiceDetailAccess()
        }
        val rechnung = Rechnung()
        // Only the edit page needs the positions with their cost assignments, and only it can afford them:
        // both collections are lazy, so mapping them is a query per invoice.
        if (editMode) {
            rechnung.copyFromWithCollections(obj)
        } else {
            rechnung.copyFrom(obj)
            rechnung.project?.displayName = obj.projekt?.name
        }
        rechnung.deleteAccess = baseDao.hasLoggedInUserDeleteAccess(obj, obj, false)
        rechnung.writeAccess = if (obj.id == null) {
            baseDao.hasLoggedInUserInsertAccess(obj, false)
        } else {
            baseDao.hasLoggedInUserUpdateAccess(obj, obj, false)
        }
        // What hides the cost assignments of a position, as `AbstractRechnungEditForm` hides its whole
        // cost table where no cost ids are configured.
        rechnung.costConfigured = Configuration.instance.isCostConfigured
        // ensuredInfo, not info: for a new invoice (see [newBaseDTO]) that lateinit would throw.
        val info = obj.ensuredInfo
        val kost1Sorted = info.sortedKost1
        rechnung.kost1List = RechnungInfo.numbersAsString(kost1Sorted)
        rechnung.kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
        val kost2Sorted = info.sortedKost2
        rechnung.kost2List = RechnungInfo.numbersAsString(kost2Sorted)
        rechnung.kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
        return rechnung
    }

    /**
     * Presets the date, the status and the type of a new invoice, as `RechnungEditPage.onPreEdit` does.
     *
     * The date is required: `RechnungDao.onInsertOrModify` refuses a null one, and every other date of the
     * form (due date, discount maturity) is derived from it. The status and the type have no default in
     * [RechnungDO] either, and Wicket gets away without a preset only because its two drop downs cannot be
     * empty (`setNullValid(false)`) — which silently makes the first constant the answer. The first
     * constant of the status is `GEPLANT` though, whose invoice gets no number, so `GESTELLT` is named here.
     */
    override fun newBaseDTO(request: HttpServletRequest?): Rechnung {
        val rechnung = super.newBaseDTO(request)
        rechnung.datum = LocalDate.now()
        rechnung.status = RechnungStatus.GESTELLT
        rechnung.typ = RechnungTyp.RECHNUNG
        return rechnung
    }

    /**
     * A new invoice built from this one, as `RechnungEditPage.cloneData` builds it — the recurring monthly
     * invoice is what a clone is for.
     *
     * @see prepareInvoiceClone for what a clone is and isn't.
     */
    override fun prepareClone(dto: Rechnung): Rechnung {
        return prepareInvoiceClone(super.prepareClone(dto), LocalDate.now())
    }

    /**
     * Hands a new invoice its number, as `RechnungEditPage.onSaveOrUpdate` does it.
     *
     * It has to happen here and not in [transformForDB]: a number is spent once handed out, so it may only
     * be taken when the invoice is actually about to be inserted — and this hook is the last step before
     * `insertOrUpdate` (see `AbstractPagesRestUtils.saveOrUpdate`), while `transformForDB` also runs for
     * every validation and every recalculation.
     *
     * [RechnungDao] assigns a number itself only on the transition of a *stored* invoice out of
     * [RechnungStatus.GEPLANT]; for a new one it insists the number is already there and is the next free
     * one (`validation.required.valueNotPresent`, then
     * `fibu.rechnung.error.rechnungsNummerIstNichtFortlaufend`). So without this, no invoice could be
     * created through this form at all — neither a clone nor a plain new one.
     *
     * The two exceptions are Wicket's, and both are the same rule seen twice: a number says an invoice was
     * issued. A [RechnungStatus.GEPLANT] one is not issued yet (it gets its number when it leaves that
     * status), and a [RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN] is the customer's document rather than
     * ours, so it must have none at all
     * (`fibu.rechnung.error.gutschriftsanzeigeDarfKeineRechnungsnummerHaben`). A number the client sent is
     * left alone: overwriting it would hide rather than report a mismatch, which [RechnungDao] checks for.
     *
     * Only a *new* invoice, though — that is the whole gap this fills, and for a stored one the number is the
     * user's to correct (editable in the form, as in Wicket): [RechnungDao.getNextNumber] would hand back the very
     * number that is stored, so filling in here would silently undo the removal of a number issued by mistake.
     * A stored invoice that stays issued and has none is [RechnungDao]'s to refuse
     * (`validation.required.valueNotPresent`), which says what happened instead of hiding it.
     */
    override fun onBeforeSave(request: HttpServletRequest, obj: RechnungDO, postData: PostData<Rechnung>) {
        if (obj.id == null &&
            obj.nummer == null &&
            obj.typ != RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN &&
            obj.status != RechnungStatus.GEPLANT
        ) {
            obj.nummer = baseDao.getNextNumber(obj)
        }
    }

    /**
     * The rules the period of performance adds on top of the field rules, which are validated generically
     * for the invoice and its nested rows alike ([ValidationUtils.validateFields]).
     *
     * The same rules the order has, applied through the same [PeriodOfPerformanceValidator] — they used to
     * live in the Wicket form of both (`PeriodOfPerformanceHelper`), which is why neither [RechnungDao] nor
     * this class enforced them before: a hand built form has no date panels to hang them on.
     *
     * Deleted positions are left out: such a position is only posted so the persistence layer doesn't remove
     * it physically, and its dates are none of the user's business anymore.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Rechnung) {
        super.validate(validationErrors, dto)
        PeriodOfPerformanceValidator.validate(
            periodOfPerformanceBegin = dto.periodOfPerformanceBegin,
            periodOfPerformanceEnd = dto.periodOfPerformanceEnd,
            positions = dto.positionen?.filter { !it.deleted }?.map { position ->
                PeriodOfPerformanceValidator.Position(
                    type = position.periodOfPerformanceType,
                    begin = position.periodOfPerformanceBegin,
                    end = position.periodOfPerformanceEnd,
                )
            },
        ).forEach { error ->
            val message =
                error.labelKey?.let { translateMsg(error.messageKey, translate(it)) } ?: translate(error.messageKey)
            validationErrors.add(ValidationError(message, fieldId = error.fieldId, messageId = error.messageKey))
        }
    }

    /**
     * Everything the hand built form has to know before the user touches it, in one read.
     *
     * All four values are configuration and none of them belongs to an invoice, so none travels on the DTO.
     * Wicket asks its four sources one by one while it builds the page; a hand built form would need a
     * request each, on every mount, for values that change about once per installation.
     *
     * Read only, so the select access of the category is what has to be checked here — nothing is written.
     */
    @GetMapping("formDefaults")
    fun getFormDefaults(): FormDefaults {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        return FormDefaults(
            defaultVat = Configuration.instance.getPercentValue(ConfigurationParam.FIBU_DEFAULT_VAT),
            bankAccounts = sellerConfig.bankAccounts.map { BankAccount(value = it.iban, label = it.displayName) },
            eInvoiceConfigured = sellerConfig.isConfigured(),
            templateVariants = invoiceService.getTemplateVariants().toList(),
        )
    }

    /**
     * The values [getFormDefaults] answers.
     */
    class FormDefaults(
        /**
         * The VAT rate a new position starts with, `fibu.defaultVAT` (as a fraction, i.e. 0.19 for 19 %).
         * Null where the installation configured none, in which case the field simply starts empty.
         */
        val defaultVat: BigDecimal?,
        /** The seller's bank accounts, for the `sellerBankAccount` select of the e-invoice address block. */
        val bankAccounts: List<BankAccount>,
        /**
         * Whether the seller address is complete enough for an e-invoice export — the condition under which
         * Wicket offers its e-invoice menu at all (`RechnungEditPage.addEInvoiceMenu`).
         */
        val eInvoiceConfigured: Boolean,
        /**
         * The variants of the Word invoice template, one entry per export the form offers. A single empty
         * string means the installation has no custom template, i.e. exactly one, unnamed variant.
         */
        val templateVariants: List<String>,
    )

    /**
     * One entry of the `sellerBankAccount` select. The value is the IBAN and not an id, because that is what
     * the column holds and what `EInvoiceSellerConfig.findBankAccount` looks an account up by — the accounts
     * come from the application configuration and have no ids.
     */
    class BankAccount(val value: String, val label: String)

    /**
     * The cost units of a project, for the Kost2 preselection of a new cost assignment
     * (`RechnungCostEditTablePanel.newKostZuweisung` takes the first one).
     *
     * Answered by the project rather than looked up in the browser: which cost units belong to a project
     * follows from its number range, area and number ([ProjektDO.nummernkreis] and friends), and the invoice
     * DTO carries its project the `copyFromMinimal` way, which omits all three.
     *
     * An empty list where the project has no area — [KostCache] cannot be asked without one, and a project
     * in that state has no cost units to offer.
     */
    @GetMapping("activeKost2")
    fun getActiveKost2(@RequestParam("projektId") projektId: Long?): List<Kost2> {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val projekt = projektCache.getProjekt(projektId) ?: return emptyList()
        val bereich = projekt.bereich ?: return emptyList()
        return kostCache.getActiveKost2(projekt.nummernkreis, bereich, projekt.teilbereich)
            .sorted()
            .map { Kost2(it) }
    }

    /**
     * Whether a cost unit belongs to the project (or, for an invoice naming none, to the customer) of the
     * invoice — the question `RechnungEditForm.onRenderCostRow` answers by outlining the field in Wicket.
     *
     * Server side for the same reason [getActiveKost2] is: the comparison is over the number range, the area
     * and the number of the project or the customer, and neither `Project` nor `Customer` carries those on
     * the wire (`Customer` has no number range at all). Three integers the caches hold anyway against
     * widening two DTOs other pages use — and the rule stays next to Wicket's copy of it.
     *
     * `matchesInvoice` is true wherever Wicket wouldn't warn, which includes the two cases it leaves alone:
     * an invoice with neither project nor customer, and a cost unit that cannot be resolved.
     *
     * **One deliberate difference.** For an invoice naming only a customer, Wicket compares the customer
     * number against `Kost2DO.teilbereich` — digits 5-6, which are the *project* number — and leaves
     * `bereich`, the digits a customer number actually occupies ([KundeDO.kost]), uncompared. So it warns
     * about nearly every cost unit of a customer-only invoice, and stays quiet about cost units of other
     * customers. Here the customer number is compared against `bereich`, which is the question the form is
     * asking.
     */
    @GetMapping("kost2Check")
    fun checkKost2(
        @RequestParam("kost2Id") kost2Id: Long?,
        @RequestParam("projektId", required = false) projektId: Long?,
        @RequestParam("kundeId", required = false) kundeId: Long?,
    ): Kost2Check {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val kost2 = kostCache.getKost2(kost2Id) ?: return Kost2Check(matchesInvoice = true)
        // The project wins over the customer, as the Wicket form reads them: a project already names its
        // customer, and its area narrows the answer further.
        val projekt = projektCache.getProjekt(projektId)
        val numberRange: Int
        // -1 means "don't compare": a customer names the digits 2-4 of a cost unit but not the 5-6.
        var area = -1
        var number = -1
        if (projekt != null) {
            numberRange = projekt.nummernkreis
            area = projekt.bereich ?: -1
            number = projekt.nummer
        } else {
            val kunde = kundeCache.getKunde(kundeId) ?: return Kost2Check(matchesInvoice = true)
            numberRange = kunde.nummernkreis
            area = kunde.nummer?.toInt() ?: return Kost2Check(matchesInvoice = true)
        }
        val differs = when {
            numberRange >= 0 && kost2.nummernkreis != numberRange -> true
            area >= 0 && kost2.bereich != area -> true
            else -> number >= 0 && kost2.teilbereich != number
        }
        return Kost2Check(matchesInvoice = !differs)
    }

    /**
     * The answer of [checkKost2] — an object rather than a bare boolean so the client reads a name instead of
     * a `true` whose meaning has to be looked up.
     */
    class Kost2Check(val matchesInvoice: Boolean)

    /**
     * The invoice as the Word document of the configured template — Wicket's "Export invoice" menu
     * (`RechnungEditPage.addExportMenu`, one entry per variant of [InvoiceService.getTemplateVariants]).
     *
     * **By id, i.e. the stored invoice — where Wicket exports the unsaved form state**
     * (`setDefaultFormProcessing(false)` on its submit link). Two reasons, and the first is the decisive one:
     * [InvoiceService] reads far more than the form posts. `getInvoiceFilename` walks
     * `konto.bezeichnung` → `kunde.konto.bezeichnung` → `kunde.name` → `kundeText`, and the address block
     * walks the same chain; a posted DTO carries account and customer as ids only, so a DTO based export
     * would have to resolve all of it from the caches a second time or quietly produce thinner names and
     * addresses. The second: a Word document leaves the house, and exporting a state that is in nobody's
     * database is a source of error rather than a feature — Wicket's behaviour is an artefact of its submit
     * model, not a decision. So the client offers the export only for a stored invoice, as Wicket omits its
     * menu for a new one.
     *
     * The access check is [RechnungDao]'s own, through `find` — the category right alone would let anyone
     * with the finance right export an invoice they may not read. An unknown id answers 404 rather than an
     * empty document; that this doesn't throw instead is what `RechnungDao.find` had to be made null safe
     * for.
     *
     * [RechnungCalculator.calculate] rather than [AbstractRechnungDO.ensuredInfo], and the same call Wicket
     * makes before building its page (`AbstractRechnungEditForm`): the document reads the sums of every single
     * position (`position.info.netSum`), and those are `lateinit` too. `ensuredInfo` would not fill them — the
     * load already put an invoice level [RechnungInfo] from the cache on the object (`RechnungDao.afterLoad`),
     * so it considers the work done while every position still throws.
     *
     * The payment terms in days, which Wicket derives before building its page (`RechnungEditPage`,
     * `recalculate()`), are none of this endpoint's business: [InvoiceService] derives the discount terms of
     * the document from the stored dates itself, so that every caller of it prints the same invoice.
     */
    @GetMapping("$EXPORT_WORD_PATH/{id}")
    fun exportInvoiceWord(
        @PathVariable("id") id: Long,
        @RequestParam("variant", required = false) variant: String?,
    ): ResponseEntity<*> {
        checkSingleInvoiceDetailAccess()
        log.info { "Exporting invoice #$id as Word document, variant='${variant ?: ""}'." }
        val invoice = baseDao.find(id) ?: return ResponseEntity.notFound().build<Any>()
        RechnungCalculator.calculate(invoice)
        val document = invoiceService.getInvoiceWordDocument(invoice, variant)
            ?: return ResponseEntity.notFound().build<Any>()
        return RestUtils.downloadFile(invoiceService.getInvoiceFilename(invoice), document.toByteArray())
    }

    /**
     * Which file is stored as *the* invoice PDF of this invoice, or that there is none.
     *
     * The invoice PDF is a JCR attachment like any other, marked as this one by its description
     * ([EInvoiceExportService.INVOICE_PDF_MARKER]); the ZUGFeRD export takes it as the document to embed the
     * XML into, and converts the Word template only where it is missing (`exportAsZUGFeRD`). So it is not a
     * second attachment list — it is one file with a role, which is why it has endpoints of its own instead
     * of a description the user could type.
     *
     * `sizeHumanReadable` travels formatted, as it comes from [Attachment]: it is the backend's own rendering
     * in the user's locale, and formatting bytes a second time in the browser would be a second place to be
     * wrong. The `fileId` travels so the form can offer the file for download through the generic attachment
     * route rather than through an endpoint of its own (see [InvoicePdfInfo]).
     */
    @GetMapping("$INVOICE_PDF_PATH/{id}/info")
    fun getInvoicePdfInfo(@PathVariable("id") id: Long): InvoicePdfState {
        checkEInvoiceReadAccess(id)
        return InvoicePdfState(eInvoiceExportService.getUploadedInvoicePdfInfo(id))
    }

    /**
     * Stores the given PDF as the invoice PDF, replacing the one that was there
     * (`EInvoiceExportService.uploadInvoicePdf` deletes it first — there is exactly one per invoice).
     *
     * Checked by [FileCheck] rather than by the extension alone, which is all Wicket looks at
     * (`RechnungEditForm.processInvoicePdfUpload`): the same check every other upload of the application
     * makes, and it names a size limit as well. Its answer is a translated text, so a refusal is reported as
     * 400 with that text rather than as a silently ignored upload — Wicket drops a non-PDF without a word.
     *
     * Answers the new state, so the client needs no second call for what it just wrote.
     */
    @PostMapping("$INVOICE_PDF_PATH/{id}")
    fun uploadInvoicePdf(
        @PathVariable("id") id: Long,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<*> {
        val invoice = checkInvoicePdfWriteAccess(id)
        val filename = file.originalFilename ?: "unknown"
        log.info { "Uploading invoice PDF '$filename' (${file.size} bytes) for invoice #${invoice.id}." }
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(translate("file.upload.error.noFileSelected"))
        }
        FileCheck.checkFile(filename, file.size, "pdf", megaBytes = MAX_INVOICE_PDF_MEGA_BYTES)?.let { error ->
            return ResponseEntity.badRequest().body(error)
        }
        eInvoiceExportService.uploadInvoicePdf(id, filename, file.inputStream.use { it.readBytes() })
        return ResponseEntity.ok(InvoicePdfState(eInvoiceExportService.getUploadedInvoicePdfInfo(id)))
    }

    /**
     * Removes the invoice PDF, so the ZUGFeRD export converts the Word template again.
     *
     * Answers the new (empty) state for the same reason the upload answers the new one, and does nothing
     * where there was none — the outcome the caller asked for is reached either way.
     */
    @DeleteMapping("$INVOICE_PDF_PATH/{id}")
    fun deleteInvoicePdf(@PathVariable("id") id: Long): InvoicePdfState {
        checkInvoicePdfWriteAccess(id)
        log.info { "Deleting the invoice PDF of invoice #$id." }
        eInvoiceExportService.deleteUploadedInvoicePdf(id)
        return InvoicePdfState(eInvoiceExportService.getUploadedInvoicePdfInfo(id))
    }

    /**
     * The invoice PDF of an invoice, or `null` where none is stored.
     *
     * A wrapper and not a nullable body: an endpoint answering `null` sends an empty body, which is not JSON
     * and cannot be told apart from a truncated answer. `{"pdf":null}` says "there is none".
     */
    class InvoicePdfState(pdf: Attachment?) {
        val pdf: InvoicePdfInfo? = pdf?.let {
            InvoicePdfInfo(name = it.name, sizeHumanReadable = it.sizeHumanReadable, fileId = it.fileId)
        }
    }

    /**
     * What the form shows of the stored invoice PDF: its name, its size, and the `fileId` to download it by.
     *
     * The `fileId` travels so that no second download path has to exist for this one file: it lies in the
     * invoice's regular attachment node (`AttachmentsService.DEFAULT_NODE`), so
     * `AttachmentsServicesRest.download` already serves it, under the same access check as its siblings. An
     * endpoint of our own here would be a second answer to a question already answered.
     */
    class InvoicePdfInfo(val name: String?, val sizeHumanReadable: String?, val fileId: String?)

    /**
     * What stands between this invoice and an e-invoice of it — Wicket's error line above its two export
     * buttons (`RechnungEditForm.EInvoiceModalDialog`, which runs the same [EInvoiceExportService.validate]).
     *
     * Read before the export rather than only reported by it: both exports throw on the first problem, and a
     * download that answers 500 says nothing about which field to correct. So the form's e-invoice section asks
     * this, lists what comes back and offers the exports only for an empty list — the same order Wicket's
     * buttons follow. A list, never a gate: the section stays usable while something is missing, because
     * missing fields are what the user is there to fill in.
     *
     * A list of sentences and not of keys, because [EInvoiceExportService.validate] translates them itself —
     * every caller puts them in front of a user unchanged, so a key would only have to be resolved twice.
     *
     * The stored invoice, as everything else on this path: the ZUGFeRD export reads the JCR by the invoice
     * id, so there is no posted state it could validate instead.
     */
    @GetMapping("$E_INVOICE_PATH/{id}/validate")
    fun validateEInvoice(@PathVariable("id") id: Long): EInvoiceValidation {
        val invoice = checkEInvoiceReadAccess(id)
        return EInvoiceValidation(
            configured = sellerConfig.isConfigured(),
            errors = eInvoiceExportService.validate(invoice),
        )
    }

    /**
     * Saves the posted invoice and lets the caller ask [validateEInvoice] again — Wicket's
     * `fibu.rechnung.eInvoice.saveAndOpen`.
     *
     * Everything on the e-invoice path works on the *stored* invoice (the ZUGFeRD export reads the JCR by the
     * invoice id, so it could not do otherwise), while the form on screen may have unsaved changes. This is the
     * one button that closes that gap: it writes what the user sees, so the checklist below it and the two
     * exports speak about the same invoice.
     *
     * A write of its own rather than the plain save, because of what it must *not* do: a save takes the user
     * back to the list, and correcting an e-invoice means staying on the page. `POST /{entity}/{action}` is the
     * shape projectforge-next has for exactly that (see lib/rs/submit-meta.ts, `BookServicesRest.lendOut`).
     *
     * Deliberately no export and no validation of its own: it saves, nothing more. What is still missing comes
     * from [validateEInvoice], which the client asks afterwards — one endpoint, one job.
     */
    @PostMapping("saveAndCheckEInvoice")
    fun saveAndCheckEInvoice(
        request: HttpServletRequest,
        @RequestBody postData: PostData<Rechnung>,
    ): ResponseEntity<ResponseAction> {
        // The groups of the e-invoice functions, since this is one of them. The write access to *this* invoice is
        // `baseDao.insertOrUpdate`'s own check, as it is for the regular save.
        checkEInvoiceAccess()
        sessionCsrfService.validateCsrfToken(request, postData, "Save for the e-invoice check")?.let { return it }
        val dbObj = transformForDB(postData.data)
        // `validate(dbObj, postData)` and not `validate(dbObj)`: the invoice has rules of its own beyond the
        // annotated fields (the period of performance, see `validate` above), and the regular save runs both.
        return saveOrUpdate(request, baseDao, dbObj, postData, this, validate(dbObj, postData))
    }

    /**
     * The invoice as XRechnung, i.e. the XML alone — Wicket's `fibu.rechnung.exportEInvoice` button.
     *
     * 400 with the validation errors where the invoice isn't exportable: the client checks first, so this is
     * the case of a state that changed in between, and the errors are more useful than a bare status. The
     * check is repeated here rather than trusted, because a download URL can be called on its own.
     */
    @GetMapping("$E_INVOICE_PATH/{id}/xrechnung")
    fun exportXRechnung(@PathVariable("id") id: Long): ResponseEntity<*> {
        val invoice = checkEInvoiceReadAccess(id)
        log.info { "Exporting invoice #$id as XRechnung." }
        return exportEInvoice(invoice) {
            RestUtils.downloadFile(
                eInvoiceExportService.getExportFilename(invoice),
                eInvoiceExportService.exportAsXRechnung(invoice),
            )
        }
    }

    /**
     * The invoice as a ZUGFeRD PDF, i.e. a PDF carrying the same XML — Wicket's `fibu.rechnung.exportZUGFeRD`.
     *
     * The document it embeds into is the uploaded invoice PDF, and the Word template converted to PDF only
     * where none was uploaded (`exportAsZUGFeRD`); the regular attachments of the invoice are embedded as
     * files, the marked invoice PDF is not. All of that is read from the JCR by the invoice id — which is why
     * this endpoint could not work on a posted state even if it wanted to.
     */
    @GetMapping("$E_INVOICE_PATH/{id}/zugferd")
    fun exportZugferd(@PathVariable("id") id: Long): ResponseEntity<*> {
        val invoice = checkEInvoiceReadAccess(id)
        log.info { "Exporting invoice #$id as a ZUGFeRD PDF." }
        return exportEInvoice(invoice) {
            RestUtils.downloadFile(
                eInvoiceExportService.getZUGFeRDExportFilename(invoice),
                eInvoiceExportService.exportAsZUGFeRD(invoice),
            )
        }
    }

    /**
     * Whether an e-invoice of this invoice can be built, and what is missing if not.
     *
     * `configured` is the seller side of it, i.e. `projectforge.einvoice.seller.*`: it is an installation's
     * setting rather than a field of this invoice, and nobody editing an invoice can fix it — so the client
     * says so instead of listing it as one problem among the invoice's own.
     */
    class EInvoiceValidation(val configured: Boolean, val errors: List<String>)

    /**
     * Runs an export, and answers 400 with the validation errors where the invoice turns out not to be
     * exportable.
     *
     * The sums have to be there first, for the reason [exportInvoiceWord] spells out: the XML states the net
     * amount of every position, and those live in a `lateinit` [RechnungInfo] that the load doesn't fill.
     *
     * The check before the call rather than a `catch` around it: [EInvoiceExportService] answers a refusal by
     * throwing `IllegalStateException` (it is Wicket's service, and Wicket checks beforehand), and an exception
     * whose message has to be parsed is a worse answer than the list it was built from.
     */
    private fun exportEInvoice(invoice: RechnungDO, export: () -> ResponseEntity<*>): ResponseEntity<*> {
        RechnungCalculator.calculate(invoice)
        val errors = eInvoiceExportService.validate(invoice)
        if (errors.isNotEmpty()) {
            log.info { "Invoice #${invoice.id} is not exportable as an e-invoice: ${errors.joinToString("; ")}" }
            return ResponseEntity.badRequest().body(errors.joinToString("\n"))
        }
        return export()
    }

    /**
     * The groups the e-invoice functions require, as `EInvoiceCheckerPageRest.checkAccess` requires them.
     *
     * On top of the select right of the category: reading an invoice and handling the document that leaves
     * the house for it are two different permissions, and the classic frontends only ever hid the menu entry
     * — projectforge-next builds its own menu, so a hidden entry keeps nobody out.
     */
    private fun checkEInvoiceAccess() {
        accessChecker.checkIsLoggedInUserMemberOfGroup(*UserRightService.FIBU_ORGA_GROUPS)
    }

    /**
     * The "no detail view yet" layer on top of [RechnungDao]'s access: order book users (right
     * [org.projectforge.business.user.UserRightId.PM_ORDER_BOOK] without the invoice right) may read the
     * filtered, read-only invoice *list* only. [RechnungDao] already bounds them to the invoices of orders
     * they may see - this keeps them out of every endpoint that serves a *single* invoice's detail or its
     * document (word export, edit/clone layout), which stays reserved for the invoice right (or the
     * controlling group, which reads all invoices). Throws if the user has neither.
     */
    private fun checkSingleInvoiceDetailAccess() {
        if (!hasSingleInvoiceDetailAccess()) {
            throw AccessException("access.exception.userHasNotRight")
        }
    }

    /**
     * Whether the logged in user has real (single-invoice) access: the controlling group, or the invoice
     * right ([RechnungDao.USER_RIGHT_ID]) read-only or read-write. Order book users (only [PM_ORDER_BOOK])
     * answer false — they get the filtered read-only list, but no detail view (see
     * [checkSingleInvoiceDetailAccess] and [listUpdateAccess]).
     */
    private fun hasSingleInvoiceDetailAccess(): Boolean {
        if (accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.CONTROLLING_GROUP)) {
            return true
        }
        return accessChecker.hasLoggedInUserRight(
            RechnungDao.USER_RIGHT_ID, false, UserRightValue.READONLY, UserRightValue.READWRITE
        )
    }

    /**
     * Whether a row of the list may be opened at all — the entity-wide question projectforge-next reads as
     * `canOpen` (see ListMetaData.userAccess.update / useEditTargets). Order book users may only read the
     * filtered list, so their rows don't open a detail view (which [checkSingleInvoiceDetailAccess] refuses
     * anyway); real invoice/controlling users keep the row click as before.
     */
    override fun listUpdateAccess(): Boolean {
        return hasSingleInvoiceDetailAccess()
    }

    /**
     * The invoice, if the user may change the file that represents it: the groups above, plus write access to
     * this very invoice.
     *
     * The second check is [RechnungDao]'s own and is the one that matters — uploading a PDF changes what an
     * e-invoice of this invoice looks like, so it is a write to the invoice even though no column of it moves.
     * `find` is the read check; without it an unknown id would be answered by an upload into a JCR node
     * nobody owns.
     */
    private fun checkInvoicePdfWriteAccess(id: Long): RechnungDO {
        val invoice = checkEInvoiceReadAccess(id)
        baseDao.hasLoggedInUserUpdateAccess(invoice, invoice, throwException = true)
        return invoice
    }

    /**
     * The invoice, if the user may see its e-invoice: the groups above plus [RechnungDao]'s own read check.
     *
     * An unknown id is an [AccessException] rather than a 404, unlike on the Word export: these endpoints
     * answer *about* an invoice (its PDF, its validation state), and "there is none" and "you may not see it"
     * are the same answer to that question — telling them apart would say whether the id exists.
     */
    private fun checkEInvoiceReadAccess(id: Long): RechnungDO {
        checkEInvoiceAccess()
        return baseDao.find(id) ?: throw AccessException("access.exception.userHasNotRight")
    }

    /**
     * The sums of an invoice as they are right now in the form, i.e. computed on the posted state, not on
     * the stored one.
     *
     * Needed because a hand built form has to show the same sums the list and the Wicket page show, and
     * those are calculated server side by [RechnungCalculator] from the whole invoice — how a position is
     * rounded before it enters a sum (`roundPositionsBeforeSum`, German law) is its rule, and a client
     * re-implementing that would drift. The cache is no help either: it answers the stored positions of an
     * invoice whose form the user has changed, and nothing at all for one without an id.
     *
     * Deleted positions may be posted untouched — [RechnungCalculator] skips them itself.
     *
     * Not a `saveOrUpdate` in disguise: nothing is written, so the read access has to be checked here.
     */
    @PostMapping("recalculate")
    fun recalculate(@RequestBody postData: PostData<Rechnung>): InvoiceSums {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val invoice = RechnungDO()
        postData.data.copyTo(invoice)
        val info = Rechnung.calculateInvoiceInfo(invoice)
        return InvoiceSums(
            netSum = info.netSum,
            vatAmount = info.vatAmount,
            grossSum = info.grossSum,
            grossSumWithDiscount = info.grossSumWithDiscount,
            kostZuweisungenNetSum = info.kostZuweisungenNetSum,
            kostZuweisungenFehlbetrag = info.kostZuweisungenFehlbetrag,
            bezahlt = info.isBezahlt,
            ueberfaellig = info.isUeberfaellig,
            positions = info.positions?.map { position ->
                PositionSums(
                    number = position.number,
                    netSum = position.netSum,
                    vatAmount = position.vatAmount,
                    grossSum = position.grossSum,
                    kostZuweisungNetSum = position.kostZuweisungNetSum,
                    kostZuweisungNetFehlbetrag = position.kostZuweisungNetFehlbetrag,
                )
            },
        )
    }

    /**
     * The sums [recalculate] answers, one flat object — the invoice's own plus one entry per position.
     */
    class InvoiceSums(
        val netSum: BigDecimal,
        val vatAmount: BigDecimal,
        val grossSum: BigDecimal,
        val grossSumWithDiscount: BigDecimal,
        val kostZuweisungenNetSum: BigDecimal,
        /**
         * How much of [netSum] is not assigned to a cost unit yet. A hint for the user only: `RechnungDao`
         * performs no validation of the cost assignment sums, so an invoice with a difference saves fine.
         */
        val kostZuweisungenFehlbetrag: BigDecimal,
        val bezahlt: Boolean,
        val ueberfaellig: Boolean,
        val positions: List<PositionSums>?,
    )

    class PositionSums(
        val number: Short,
        val netSum: BigDecimal,
        val vatAmount: BigDecimal,
        val grossSum: BigDecimal,
        val kostZuweisungNetSum: BigDecimal,
        /** The per position counterpart of [InvoiceSums.kostZuweisungenFehlbetrag], which Wicket paints red. */
        val kostZuweisungNetFehlbetrag: BigDecimal,
    )

    /**
     * Opts the invoice list into the lean row of [Rechnung.copyFrom4ListRow]: only the columns
     * `invoice.page.tsx` renders, instead of the positions and the dozen fields only the edit form reads.
     */
    override fun newDTO(): Rechnung {
        return Rechnung()
    }

    /**
     * Adds the statistics of the whole result set, the ones the Wicket list shows above its table
     * (`AbstractRechnungListForm.addStatistics`).
     *
     * Computed here rather than in the browser: none of the sums is a property of the [Rechnung] DTO, and
     * how a discount, a partial payment and a foreign currency enter them is [RechnungsStatistik]'s
     * business - a second implementation on the client would be a second answer.
     *
     * Sent as data, not as the markdown [ResultSet.resultInfo] carries for the legacy React app: the hand
     * built next page formats currency in the user's locale and takes its colours from css tokens (see
     * InvoiceStatisticsLine there).
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<RechnungDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        if (resultSet.offset == null) {
            // Non-paged POST list: the result set is the whole result, so its statistics are the whole result's.
            result.statistics = buildStatistics(resultSet.resultSet, magicFilter)
        }
        // Server-side paged: resultSet.resultSet is one page; the whole-result statistics were computed over the
        // full id list in aggregate() and carried through the DTO transform, so they are left as they are here.
        return result
    }

    /**
     * The statistics of the given invoices, plus - when the client asked for it and the invoice-date filter is
     * a bounded range - the same figures for the period a year earlier (see [previousYearFilter]). Shared by
     * the non-paged [postProcessResultSet] and the server-side paged [aggregate], so both show the same sums.
     */
    private fun buildStatistics(invoices: List<RechnungDO>, magicFilter: MagicFilter): InvoiceStatistics {
        val statistics = InvoiceStatistics(baseDao.buildStatistik(invoices))
        previousYearFilter(magicFilter)?.let { previousFilter ->
            // The same query one year back, run through the same pipeline (getObjectList + filterList)
            // as the list itself, so the two sums differ only by the period.
            val previousInvoices = filterList(getObjectList(this, baseDao, previousFilter), previousFilter)
            statistics.previousYear = InvoiceStatistics(baseDao.buildStatistik(previousInvoices))
        }
        return statistics
    }

    /**
     * The sums the list shows above its table, plus the two averages of the payment target.
     *
     * `bruttoWithDiscount` travels although the Wicket page shows it only where it differs from the gross
     * sum: whether a line is worth a row is the client's decision there as here, and the two are compared
     * as numbers either way.
     */
    class InvoiceStatistics(statistics: RechnungsStatistik) {
        val counter: Int = statistics.counter
        val counterPaid: Int = statistics.counterBezahlt
        val brutto: BigDecimal = statistics.brutto
        val bruttoWithDiscount: BigDecimal = statistics.bruttoMitSkonto
        val netto: BigDecimal = statistics.netto
        val paid: BigDecimal = statistics.gezahlt
        val open: BigDecimal = statistics.offen
        val overdue: BigDecimal = statistics.ueberfaellig
        val discount: BigDecimal = statistics.skonto

        /** Agreed payment target in days, averaged over the invoices - what was asked for. */
        val paymentTargetAverage: Int = statistics.zahlungszielAverage

        /** Actual payment target in days, weighted by the gross sum - what the customers did. */
        val actualPaymentTargetAverage: Int = statistics.tatsaechlichesZahlungzielAverage

        /**
         * Invoices whose amount could not be converted into the system currency, so their own amount
         * entered the sums above (see `AbstractRechnungsStatistik.convertToSystemCurrency`). Empty for
         * every installation with a single currency, and the reason the client shows a warning.
         */
        val currencyConversionWarnings: List<String> = statistics.currencyConversionWarningsList

        /**
         * The same figures for the same period one year earlier, or null unless the client asked for the
         * comparison and the invoice-date filter is a bounded range (see [previousYearFilter]). Only the
         * top-level object carries it; the one nested here is always null.
         */
        var previousYear: InvoiceStatistics? = null
    }

    /**
     * Adds the three filters the Wicket list has and no property of [RechnungDO] yields.
     *
     * The payment state ([LIST_TYPE_FILTER]) is Wicket's radio group over `RechnungFilter.listType`, and
     * the period of performance is its second time period panel: the first is a predicate over
     * [RechnungInfo], the second over two date columns at once, so neither is a search field.
     *
     * The third ([INCOMPLETE_FILTER]) grew out of Wicket's `showKostZuweisungStatus` checkbox - there a
     * pure display switch marking the rows whose cost assignments don't add up, here the question itself:
     * show only the invoices something is still missing from, the missing account included (see
     * [IncompleteInvoiceFilter]). Offered only where this installation expects either, since it would
     * otherwise match every invoice or none.
     *
     * The status is opened by default, as in the order list: it is what an invoice list is narrowed by
     * first. The completeness filter as well, as it is a standing question of whoever keeps the books -
     * and unlike the others it says nothing about the list while it is unchecked.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        val statusFilter = elements.find { it is UIFilterElement && it.id == RechnungDO::status.name }
        (statusFilter as? UIFilterElement)?.defaultFilter = true
        // The invoice date opens by default too: an invoice list is read by period as much as by status.
        val datumFilter = elements.find { it is UIFilterElement && it.id == RechnungDO::datum.name }
        (datumFilter as? UIFilterElement)?.defaultFilter = true
        elements.add(
            UIFilterListElement(
                LIST_TYPE_FILTER,
                // Its own label, not the status of RechnungDO: the two sit next to each other in the filter
                // bar, and this one asks whether the money arrived (unpaid, delinquent, paid), not which of
                // the seven states the invoice is in.
                label = translate("fibu.rechnung.filter.paymentStatus"),
                multi = false,
                defaultFilter = true,
            ).also { element ->
                // Hand built rather than from an enum: `listType` is a set of string constants of
                // `RechnungFilter`, and its fourth value (`all`) is "no filter", which is the pill's absence.
                element.values = listOf(
                    UISelectValue(LIST_TYPE_UNPAID, translate("fibu.rechnung.filter.unbezahlt")),
                    UISelectValue(LIST_TYPE_OVERDUE, translate("fibu.rechnung.filter.ueberfaellig")),
                    UISelectValue(LIST_TYPE_PAID, translate("fibu.rechnung.status.bezahlt")),
                )
            }
        )
        // The two ends of the period of performance are one question, as the Wicket panel asks it: one
        // label, two dates. As two independent range filters they are four dates for a single time window,
        // and each of them alone matches invoices whose *other* end lies outside it.
        elements.removeIf { it is UIFilterElement && it.id in PERIOD_OF_PERFORMANCE_FIELDS }
        elements.add(
            UIFilterElement(
                PERIOD_OF_PERFORMANCE_FILTER,
                UIFilterElement.FilterType.DATE,
                label = translate("fibu.periodOfPerformance"),
            )
        )
        if (IncompleteInvoiceFilter.isOffered(Configuration.instance.isCostConfigured, invoiceConfig.accountRequired)) {
            elements.add(
                UIFilterElement(
                    INCOMPLETE_FILTER,
                    UIFilterElement.FilterType.BOOLEAN,
                    label = translate("fibu.rechnung.filter.incomplete"),
                    defaultFilter = true,
                )
            )
        }
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<RechnungDO>>? {
        val filters = mutableListOf<CustomResultFilter<RechnungDO>>()
        val listTypeEntry = source.entries.find { it.field == LIST_TYPE_FILTER }
        listTypeEntry?.synthetic = true // No property of RechnungDO, so the database cannot answer it.
        listTypeEntry?.value?.values?.firstOrNull { it.isNotBlank() }?.let { listType ->
            filters.add(PaymentStateFilter(listType, rechnungCache))
        }
        val incompleteEntry = source.entries.find { it.field == INCOMPLETE_FILTER }
        // Neither of the two reasons is a column: the difference is computed by RechnungCalculator, and the
        // account may be inherited from project or customer.
        incompleteEntry?.synthetic = true
        if (incompleteEntry?.isTrueValue == true) {
            filters.add(
                IncompleteInvoiceFilter(
                    costConfigured = Configuration.instance.isCostConfigured,
                    accountRequired = invoiceConfig.accountRequired,
                    // The account the export would use, not the invoice's own: an invoice without one is
                    // booked to the account of its project or its customer, so it lacks nothing.
                    accountOf = { kontoCache.getKonto(it) },
                    // From the cache, not ensuredInfo: this filter runs before afterLoad puts the cached info
                    // on the row (see IncompleteInvoiceFilter and PaymentStateFilter).
                    infoOf = { rechnungCache.getRechnungInfo(it.id) },
                )
            )
        }
        addPeriodOfPerformanceCriterion(target, source)
        return filters
    }

    /**
     * Turns the single [PERIOD_OF_PERFORMANCE_FILTER] entry into the overlap criterion Wicket's
     * "Leistungszeitraum" panel uses (see [AuftragAndRechnungDaoHelper]): an invoice matches if its own
     * period reaches into the window asked for. Filtering `periodOfPerformanceBegin` by the window
     * instead would hide a two-year invoice from a one-month window it runs right through.
     */
    private fun addPeriodOfPerformanceCriterion(target: QueryFilter, source: MagicFilter) {
        val entry = source.entries.find { it.field == PERIOD_OF_PERFORMANCE_FILTER } ?: return
        entry.synthetic = true
        val filter = object : SearchFilterWithPeriodOfPerformance {
            override val periodOfPerformanceStartDate = PFDayUtils.parseDate(entry.value.fromValue)
            override val periodOfPerformanceEndDate = PFDayUtils.parseDate(entry.value.toValue)
        }
        AuftragAndRechnungDaoHelper.createCriterionForPeriodOfPerformance(filter).ifPresent { target.add(it) }
    }

    /**
     * Takes the columns no `ORDER BY` can express out of the query - [filterList] sorts by those.
     *
     * Without it the whole `ORDER BY` is lost: `addOrder` swallows the exception an unknown property
     * causes, so the list comes back in whatever order the database produced and the sort the user asked
     * for silently does nothing.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        target.sortProperties.removeIf { COMPUTED_SORT_PROPERTIES.containsKey(it.property) }
    }

    /**
     * Sorts the loaded invoices by the columns that are no database column, the way the Wicket list page
     * has always done it (`MyListPageSortableDataProvider`).
     *
     * The two sums and the translated status live in [RechnungInfo], which `RechnungCache` answered for
     * every row that was loaded anyway - a map lookup per comparison, not a query. The customer and the
     * project sort by the very string their cell shows, `displayName`, which `KostFormatter` composes of
     * number and name ("473 - Air Liquide"); the numbers are left padded, so ordering by the string is
     * ordering by the number. Sorting by `kunde.name` instead produces a list that looks unsorted to
     * whoever reads it, since the numbers lead every cell.
     *
     * The whole result set is loaded (there is no server side paging yet, see
     * `MIGRATION-list-paging.md`), so sorting it here is sorting all of it.
     */
    override fun filterList(resultSet: MutableList<RechnungDO>, filter: MagicFilter): List<RechnungDO> {
        val computed = filter.sortProperties.filter { COMPUTED_SORT_PROPERTIES.containsKey(it.property) }
        if (computed.isEmpty()) {
            return resultSet
        }
        // The invoice number as the last criterion, so equal values keep a deterministic order instead of
        // shifting between two requests over the same data.
        val sortProperties = computed + SortProperty.desc(RechnungDO::nummer.name)
        return SortPropertyComparator.sort(resultSet, sortProperties) { invoice, property ->
            COMPUTED_SORT_PROPERTIES[property]?.invoke(invoice)
        }
    }

    /**
     * The server-side paging counterpart of [filterList] (see `MIGRATION-list-paging.md`): orders the
     * materialized id list once per (session, filter), so a page is a slice of an already sorted list.
     *
     * The customer and the project sort by a `displayName` no [RechnungInfo] holds, so - unlike the order
     * book, which sorts its ids from [AuftragsCache] alone - this loads the matching invoices and reuses
     * [filterList]'s comparator over them, which makes the paged order byte-for-byte the non-paged one. The
     * load is cached by [getListPage] (once per filter, not once per page) and happens only when a computed
     * column is sorted on - a database column is ordered by the query itself and the ids come pre-sorted.
     */
    override fun sortIds(ids: LongArray, filter: MagicFilter): LongArray {
        val computed = filter.sortProperties.filter { COMPUTED_SORT_PROPERTIES.containsKey(it.property) }
        if (computed.isEmpty()) {
            return ids
        }
        val sorted = filterList(getListByIds(ids.toList()).toMutableList(), filter)
        return sorted.mapNotNull { it.id }.toLongArray()
    }

    /**
     * The whole-result statistics of a server-side paged invoice list: computed over the full id list, not
     * over the single page [postProcessResultSet] sees, so the footer shows the sums of every matching
     * invoice (see `MIGRATION-list-paging.md`). [RechnungsStatistik] converts foreign currencies from the
     * loaded [RechnungDO], which no cache holds, so the matching invoices are loaded here - the same work
     * [postProcessResultSet] does over its (complete) result set on the non-paged `POST list`.
     */
    override fun aggregate(ids: LongArray, filter: MagicFilter): Any? {
        return buildStatistics(getListByIds(ids.toList()), filter)
    }

    /**
     * The filtered list as the Excel file Wicket's "Excel export" produces (`RechnungListPage`).
     *
     * The rows come from [getResultList], i.e. through the same pipeline the list itself uses (the
     * synthetic payment state filter, then [filterList]) - `getObjectList` directly would export invoices
     * the list doesn't show.
     *
     * An empty result answers 404 rather than a file, as the order export does: Wicket reports it as a
     * form error, and a file saying "nothing to export" looks like a successful export in the download
     * folder.
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting outgoing invoices as Excel file.")
        val invoices = getResultList(filter)
        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        ExcelUtils.prepareWorkbook().use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("fibu.rechnungen"))
            val currencyStyle = workbook.createOrGetCellStyle("currency")
            currencyStyle.dataFormat = workbook.createDataFormat().getFormat(CURRENCY_FORMAT)
            ExcelUtils.registerColumn(sheet, RechnungDO::nummer, 10)
            sheet.registerColumn(translate("fibu.kunde"), COL_CUSTOMER).withSize(30)
            sheet.registerColumn(translate("fibu.projekt"), COL_PROJECT).withSize(30)
            ExcelUtils.registerColumn(sheet, RechnungDO::betreff, 40)
            ExcelUtils.registerColumn(sheet, RechnungDO::datum)
            ExcelUtils.registerColumn(sheet, RechnungDO::faelligkeit)
            ExcelUtils.registerColumn(sheet, RechnungDO::bezahlDatum)
            sheet.registerColumn(translate("fibu.rechnung.status"), COL_STATUS).withSize(16)
            sheet.registerColumn(translate("fibu.common.netto"), COL_NET_SUM).withSize(14)
            sheet.registerColumn(translate("fibu.common.brutto"), COL_GROSS_SUM).withSize(14)
            ExcelUtils.registerColumn(sheet, RechnungDO::zahlBetrag, 14)
            sheet.registerColumn(translate("fibu.konto.nummer"), COL_ACCOUNT).withSize(10)
            sheet.registerColumn(translate("fibu.konto.bezeichnung"), COL_ACCOUNT_TEXT).withSize(30)
            ExcelUtils.registerColumn(sheet, RechnungDO::bemerkung, 40)
            ExcelUtils.addHeadRow(sheet)
            invoices.forEach { invoice ->
                val row = sheet.createRow()
                row.autoFillFromObject(invoice)
                // The related customer or, for an invoice naming none, the free text - the same fallback
                // the list's cell and `KundeFormatter` make.
                row.getCell(COL_CUSTOMER)?.setCellValue(
                    PfCaches.instance.getKundeIfNotInitialized(invoice.kunde)?.displayName ?: invoice.kundeText
                )
                row.getCell(COL_PROJECT)
                    ?.setCellValue(PfCaches.instance.getProjektIfNotInitialized(invoice.projekt)?.displayName)
                row.getCell(COL_STATUS)?.setCellValue(invoice.status?.let { translate(it.i18nKey) })
                val info = invoice.ensuredInfo
                row.getCell(COL_NET_SUM)?.setCellValue(info.netSum)?.setCellStyle(currencyStyle)
                row.getCell(COL_GROSS_SUM)?.setCellValue(info.grossSum)?.setCellStyle(currencyStyle)
                ExcelUtils.getCell(row, RechnungDO::zahlBetrag)?.setCellStyle(currencyStyle)
                val konto = kontoCache.getKonto(invoice)
                konto?.nummer?.let { row.getCell(COL_ACCOUNT)?.setCellValue(it) }
                row.getCell(COL_ACCOUNT_TEXT)?.setCellValue(konto?.bezeichnung)
            }
            sheet.setAutoFilter()
            val filename = "ProjectForge-${translate("fibu.common.debitor")}" +
                    "_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
            return RestUtils.downloadFile(filename, workbook.asByteArrayOutputStream.toByteArray())
        }
    }

    /**
     * The same invoices with one row per cost assignment, i.e. `RechnungListPage.exportExcelWithCostAssignments`.
     *
     * Answers 404 where no cost ids are configured, as the Wicket menu entry is simply absent there:
     * without them every row of the sheet would be the invoice itself, which the export above already is.
     */
    @PostMapping(EXPORT_COST_ASSIGNMENTS_PATH)
    fun exportCostAssignmentsAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting cost assignments of outgoing invoices as Excel file.")
        if (!Configuration.instance.isCostConfigured) {
            return ResponseEntity.notFound().build<Any>()
        }
        val invoices: List<AbstractRechnungDO> = getResultList(filter)
        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val debitor = translate("fibu.common.debitor")
        val xls = kostZuweisungExport.exportRechnungen(invoices, debitor)
        if (xls == null || xls.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val filename = "ProjectForge-$debitor-${translate("menu.fibu.kost")}" +
                "_${DateHelper.getDateAsFilenameSuffix(Date())}.xls"
        return RestUtils.downloadFile(filename, xls)
    }

    /**
     * Wicket's radio group over `RechnungFilter.listType`, as a filter of the result list.
     *
     * A [CustomResultFilter] and not a query criterion: whether an invoice is paid or overdue follows from
     * [RechnungInfo] - the sum of its positions against what was paid, and the due date against today -
     * which is why `RechnungDao.select` filters it in memory as well.
     *
     * The [RechnungInfo] comes from [RechnungCache] and not from [AbstractRechnungDO.ensuredInfo], for the
     * reason the cache exists at all: a custom result filter runs inside `DBQuery.select`, i.e. *before*
     * `BaseDao.select` fires `RechnungDao.afterLoad` that would put the cached info on the row. So `ensuredInfo`
     * would find `info` still uninitialized and fall back to [RechnungCalculator.calculate], lazily loading
     * that invoice's positions and cost assignments - one query pair per invoice of the whole result set, the
     * very N+1 storm the cache is there to avoid. The cache is filled once at startup and holds every invoice,
     * so a lookup by id answers without touching the database; only an invoice created after the last refresh
     * misses, and for that single row the fallback is fine.
     */
    private class PaymentStateFilter(
        private val listType: String,
        private val rechnungCache: RechnungCache,
    ) : CustomResultFilter<RechnungDO> {
        override fun match(list: MutableList<RechnungDO>, element: RechnungDO): Boolean {
            val info = rechnungCache.getRechnungInfo(element.id) ?: element.ensuredInfo
            return when (listType) {
                LIST_TYPE_UNPAID -> !info.isBezahlt
                LIST_TYPE_PAID -> info.isBezahlt
                LIST_TYPE_OVERDUE -> info.isUeberfaellig
                else -> true
            }
        }
    }

    companion object {
        /**
         * Turns a copy of an invoice into a new one, the way `RechnungEditPage.cloneData` does it.
         *
         * A clone is not a duplicate: it is a fresh invoice with the same content. So everything the *first*
         * invoice earned is dropped, and everything derived from a date is derived again from today:
         *
         * - No number. A number is spent once handed out, and `RechnungDao.onInsertOrModify` assigns the next
         *   free one on the transition out of [RechnungStatus.GEPLANT].
         * - Dated today, with the due date and the discount maturity re-derived from the agreed payment
         *   targets in days — copying the old dates would produce an invoice overdue on the day it is written.
         *   A target that isn't stated stays unstated: no discount term means no maturity.
         * - Nothing paid: no payment date, no paid amount, and [RechnungStatus.GESTELLT] as the status, which
         *   is also what [newBaseDTO] presets.
         * - Every position and every cost assignment loses its id, so the save writes new rows and
         *   [assignNumbersAndIndicesToNewRows] numbers them from 1 (`RechnungsPositionDO.newClone` and
         *   `KostZuweisungDO.newClone` drop nothing but the id either). Positions the user had marked as
         *   deleted are left out entirely — they are on their way out of the *old* invoice and have no
         *   meaning in a new one.
         * - No attachments: the files live in JCR under the old invoice's id and are not copied, as Wicket
         *   copies none.
         *
         * The read-only sums are left as they are: the form asks [recalculate] for them as soon as it is
         * shown, and they are no part of what is saved.
         *
         * `internal` and in the companion object rather than a method: it needs nothing of the instance, and
         * [today] as a parameter is what makes it testable without a Spring context (`RechnungDtoTest`).
         *
         * @param dto The copy, already stripped of id, deleted flag and timestamps by
         * `AbstractEntityRest.prepareClone`.
         */
        internal fun prepareInvoiceClone(dto: Rechnung, today: LocalDate): Rechnung {
            dto.nummer = null
            dto.datum = today
            dto.faelligkeit = today.plusDays((dto.zahlungsZielInTagen ?: 0).toLong())
            // Only where a discount was agreed at all: a `null` term is no term, and `today.plusDays(0)`
            // would turn it into a discount maturing on the day the clone is written.
            dto.discountMaturity = dto.discountZahlungsZielInTagen?.let { today.plusDays(it.toLong()) }
            dto.zahlBetrag = null
            dto.bezahlDatum = null
            dto.status = RechnungStatus.GESTELLT
            dto.attachments = null
            dto.attachmentsCounter = null
            dto.attachmentsSize = null
            dto.positionen = dto.positionen?.filter { !it.deleted }?.onEach { position ->
                position.id = null
                position.kostZuweisungen?.forEach { it.id = null }
            }?.toMutableList()
            return dto
        }

        /**
         * Gives every posted row that has no id yet its number: the positions of the invoice, and the cost
         * assignments of each position.
         *
         * Both are `@OrderColumn`s the client cannot assign, and both identify a row inside its collection:
         * `RechnungsPositionDO` has `UNIQUE(rechnung_fk, number)` with `@ListIndexBase(1)`, and
         * `KostZuweisungDO.index` is the order column of a position's assignments (0-based, as
         * `AbstractRechnungsPositionDO.addKostZuweisung` assigns it).
         *
         * The next free number is taken from the **stored** rows only: whatever number the client gave a new
         * row is its guess and is about to be replaced, so counting those in would leave a gap for every new
         * row. A stored row the client marked deleted still counts — it stays in the database, so its number
         * is never reused, and a gap is the record of what was deleted.
         *
         * No renumbering map is needed, unlike the order's: nothing refers to an invoice position by number
         * the way a payment schedule refers to an order position.
         *
         * `internal` and in the companion object rather than a private method: it needs nothing of the
         * instance, and this is what the numbering of a whole posted invoice can be tested through without a
         * Spring context (`RechnungDtoTest`).
         */
        internal fun assignNumbersAndIndicesToNewRows(invoice: RechnungDO) {
            val positions = invoice.positionen ?: return
            var nextNumber = (positions.filter { it.id != null }.maxOfOrNull { it.number } ?: 0).toInt()
            positions.filter { it.id == null }.forEach { position ->
                position.number = (++nextNumber).toShort()
            }
            positions.forEach { position ->
                val assignments = position.kostZuweisungen ?: return@forEach
                // -1, not 0: the index is 0-based, so the first assignment of a position has to become 0.
                var nextIndex = (assignments.filter { it.id != null }.maxOfOrNull { it.index } ?: -1).toInt()
                assignments.filter { it.id == null }.forEach { assignment ->
                    assignment.index = (++nextIndex).toShort()
                }
            }
        }

        /**
         * Id of the payment state filter - a pseudo field standing for `RechnungFilter.listType`, whose
         * values are the three constants below (`RechnungFilter.FILTER_*`).
         */
        internal const val LIST_TYPE_FILTER = "listType"
        private const val LIST_TYPE_UNPAID = "unbezahlt"
        private const val LIST_TYPE_PAID = "bezahlt"
        private const val LIST_TYPE_OVERDUE = "ueberfaellig"

        /** The invoice-date property ([AbstractRechnungDO.datum]) whose range the previous-year comparison shifts. */
        private const val DATE_FIELD = "datum"

        /** [MagicFilter.extended] flag by which the client asks for the previous-year comparison. */
        internal const val PREVIOUS_YEAR_COMPARISON = "previousYearComparison"

        /**
         * The filter of the previous-year comparison - the same one shifted twelve months back - or null when
         * it does not apply.
         *
         * It applies only when the client asked for it ([PREVIOUS_YEAR_COMPARISON] in [MagicFilter.extended])
         * and the invoice-date filter ([AbstractRechnungDO.datum], the field [DATE_FIELD]) is a bounded range:
         * "the same period a year earlier" has no meaning without both a start and an end. Both bounds move by a
         * year while every other criterion stays, so the two statistics answer the same question one year apart.
         *
         * The [MagicFilterEntry.Value.periodKind] is dropped on the clone: a range shifted by hand is no longer
         * "this month", and the flag is removed so a re-select cannot recurse. Static and [internal] so the
         * shift can be asserted without a Spring context (see OutgoingInvoicePreviousYearFilterTest).
         */
        internal fun previousYearFilter(magicFilter: MagicFilter): MagicFilter? {
            val requested = magicFilter.extended[PREVIOUS_YEAR_COMPARISON].let { it == true || it == "true" }
            if (!requested) {
                return null
            }
            val datum = magicFilter.entries.find { it.field == DATE_FIELD }
            val from = parseIsoDate(datum?.value?.fromValue) ?: return null
            val to = parseIsoDate(datum?.value?.toValue) ?: return null
            return magicFilter.clone().also { clone ->
                clone.extended.remove(PREVIOUS_YEAR_COMPARISON)
                clone.entries.find { it.field == DATE_FIELD }?.value?.let { value ->
                    value.fromValue = from.minusYears(1).toString()
                    value.toValue = to.minusYears(1).toString()
                    value.periodKind = null
                }
            }
        }

        private fun parseIsoDate(value: String?): LocalDate? =
            value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        /**
         * Id of the combined period-of-performance filter, standing for a criterion over
         * [PERIOD_OF_PERFORMANCE_FIELDS] - see [addPeriodOfPerformanceCriterion].
         */
        internal const val PERIOD_OF_PERFORMANCE_FILTER = "periodOfPerformance"

        /** The two date properties the combined filter replaces in the filter field list. */
        private val PERIOD_OF_PERFORMANCE_FIELDS = setOf(
            RechnungDO::periodOfPerformanceBegin.name,
            RechnungDO::periodOfPerformanceEnd.name,
        )

        /** Sub path of the cost assignment export, as `invoice-list-actions.tsx` calls it. */
        internal const val EXPORT_COST_ASSIGNMENTS_PATH = "exportCostAssignmentsAsExcel"

        /** Sub path of the Word export, as `lib/rs/invoice.ts` calls it (the invoice id follows). */
        internal const val EXPORT_WORD_PATH = "exportInvoiceWord"

        /** Sub path of the three invoice PDF endpoints, as `lib/rs/invoice-pdf.ts` calls them. */
        internal const val INVOICE_PDF_PATH = "invoicePdf"

        /** Sub path of the e-invoice validation and its two exports, as `lib/rs/invoice.ts` calls them. */
        internal const val E_INVOICE_PATH = "eInvoice"

        /**
         * Size limit of the invoice PDF, the same [FileCheck] limit the e-invoice checker page applies.
         *
         * Well below what the storage itself refuses (`EInvoiceExportService` checks 50 MB): a scanned or
         * exported invoice of that size is a mistake, and saying so on the upload is more useful than
         * discovering it in the JCR.
         */
        private const val MAX_INVOICE_PDF_MEGA_BYTES = 20L

        private const val CURRENCY_FORMAT = "#,##0.00;[Red]-#,##0.00"

        /** Aliases of the Excel columns that are no property of [RechnungDO]. */
        private const val COL_CUSTOMER = "customer"
        private const val COL_PROJECT = "project"
        private const val COL_STATUS = "statusAsString"
        private const val COL_NET_SUM = "netSum"
        private const val COL_GROSS_SUM = "grossSum"
        private const val COL_ACCOUNT = "kontoNummer"
        private const val COL_ACCOUNT_TEXT = "kontoBezeichnung"

        /** Sort ids of the customer and the project column, as `invoice.page.tsx` declares them. */
        private const val CUSTOMER_SORT_PROPERTY = "kunde.displayName"
        private const val PROJECT_SORT_PROPERTY = "projekt.displayName"

        /**
         * The sort ids no database column can answer, and the value each one sorts by (see [filterList]).
         *
         * Keyed by what `invoice.page.tsx` declares its columns as, which for the three DTO fields is the
         * DTO's property name.
         */
        private val COMPUTED_SORT_PROPERTIES = mapOf<String, (RechnungDO) -> Comparable<*>?>(
            Rechnung::netSum.name to { it.ensuredInfo.netSum },
            Rechnung::grossSumWithDiscount.name to { it.ensuredInfo.grossSumWithDiscount },
            Rechnung::kostZuweisungenFehlbetrag.name to { it.ensuredInfo.kostZuweisungenFehlbetrag },
            Rechnung::statusAsString.name to { invoice -> invoice.status?.let { translate(it.i18nKey) } },
            CUSTOMER_SORT_PROPERTY to { invoice ->
                PfCaches.instance.getKundeIfNotInitialized(invoice.kunde)?.displayName ?: invoice.kundeText
            },
            PROJECT_SORT_PROPERTY to { invoice ->
                PfCaches.instance.getProjektIfNotInitialized(invoice.projekt)?.displayName
            },
        )
    }
}
