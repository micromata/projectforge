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

import jakarta.servlet.http.HttpServletRequest
import org.hibernate.search.mapper.orm.Search
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.rest.core.getList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Guards the fix for the reported "inverted" full text search on the outgoing invoice list.
 *
 * The customer report: with a payment state filter selected, searching `post` showed invoices *without* "Post"
 * in the customer name while `-post` showed exactly the "Deutsche Post" customers — the results looked swapped.
 *
 * The cause was the analyzer on [KundeDO.name]: it used to be indexed with a `KeywordTokenizer`, so the whole
 * name was a *single* term and a search term "post" only ever matched a *prefix of the whole name* — never the
 * "post" *inside* "Deutsche Post DHL …". So `post` missed those customers and `-post` (a prohibited-only
 * clause = every doc lacking the term "post") surfaced exactly them. The fix indexes the name with a
 * `WhitespaceTokenizer` (see `MyAnalysisConfigurer`), so each word of the name is its own term while special
 * characters stay attached — a word inside the name matches, and a name like "K+S" is still found.
 *
 * The two invoice groups below mirror the screenshots: "DHL Freight" invoices carry "Post" in a tokenized
 * field (subject), so they match `post` via the subject; the "Deutsche Post" invoices carry "post" only in the
 * customer name and used to go missing — they are the regression this test locks down.
 */
class OutgoingInvoiceSearchStatusTest : AbstractTestBase() {
    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        logon(TEST_FINANCE_USER)
        val session = MockHttpSession()
        request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getSession(false)).thenReturn(session)
    }

    @Test
    fun `a full text search finds a customer by a word inside its name, and the state filter never inverts that`() {
        // Anonymized customers; "acmeplus" is a word unique to this test, so the searches below are unaffected
        // by invoices other tests may have left in the shared database.
        //
        // Customer 1: the search word "acmeplus" occurs only *inside* the customer name.
        val nameKunde = insertKunde(810, "AcmePlus Widgets AG")
        // Customer 2: no "acmeplus" in the name; it appears only in a tokenized field (the subject).
        val subjectKunde = insertKunde(820, "Zonko Logistics GmbH")

        val nameIds = insertInvoices(nameKunde, subject = "Sammelrechnung", count = 4).toSortedSet()
        val subjectIds = insertInvoices(subjectKunde, subject = "AcmePlus Express delivery", count = 3).toSortedSet()
        val all = (nameIds + subjectIds).toSortedSet()
        rebuildIndex()

        // State alone keeps every one of them (all are unpaid).
        assertTrue(listIds(status = UNPAID).containsAll(all), "The state filter alone keeps every unpaid invoice of this test.")

        // The invariant: "acmeplus" finds every invoice with that word (in the name OR in the subject), and the
        // state filter only narrows that set — it never turns it into the complement. Before the fix the name
        // match was missing, so this returned only the subject matches.
        assertEquals(all, listIds(search = "acmeplus"), "\"acmeplus\" must find the customer by name AND the invoices carrying it in the subject.")
        assertEquals(all, listIds(search = "acmeplus", status = UNPAID), "\"acmeplus\" with a state filter is the same set intersected with unpaid, not inverted.")

        // "-acmeplus" excludes everything matching "acmeplus"; none of these invoices may leak into that result.
        assertTrue(listIds(search = "-acmeplus", status = UNPAID).none { it in all }, "\"-acmeplus\" excludes every \"acmeplus\" match; it must not surface those customers.")
    }

    @Test
    fun `a customer name with a special character is still found, and a word of it matches too`() {
        // The whitespace tokenizer keeps "Acme+Plus" as one term (the standard analyzer would split it at "+").
        // "kwatt" is a word unique to this test, so the assertions are robust against other tests' data.
        val specialKunde = insertKunde(830, "Acme+Plus Kwatt GmbH")
        val ids = insertInvoices(specialKunde, subject = "Lieferung", count = 2).toSortedSet()
        rebuildIndex()

        assertEquals(ids, listIds(search = "Acme+Plus"), "The special-character name \"Acme+Plus\" is still found as one term.")
        assertEquals(ids, listIds(search = "kwatt"), "A word inside the name (\"kwatt\") matches its customer.")
    }

    /** Runs the list exactly as the `POST list` endpoint does (state filter as a custom result filter). */
    private fun listIds(search: String? = null, status: String? = null): Set<Long> {
        val filter = MagicFilter()
        filter.searchString = search
        if (status != null) {
            filter.entries.add(MagicFilterEntry(field = OutgoingInvoiceEntityRest.LIST_TYPE_FILTER).also {
                it.value.values = arrayOf(status)
            })
        }
        return getList(request, outgoingInvoiceEntityRest, rechnungDao, filter).resultSet.mapNotNull { it.id }.toSortedSet()
    }

    private fun rebuildIndex() {
        persistenceService.runReadOnly { context ->
            Search.session(context.em).massIndexer().startAndWait()
        }
    }

    private fun insertKunde(nummer: Long, name: String): KundeDO {
        val kunde = KundeDO()
        kunde.nummer = nummer
        kunde.name = name
        kundeDao.insert(kunde, checkAccess = false)
        return kunde
    }

    private fun insertInvoices(kunde: KundeDO, subject: String, count: Int): List<Long> {
        return (1..count).map {
            val invoice = RechnungDO().also {
                it.datum = LocalDate.now()
                it.faelligkeit = LocalDate.now()
                it.status = RechnungStatus.GESTELLT
                it.kunde = kunde
                it.betreff = subject
                it.addPosition(RechnungsPositionDO().also { pos ->
                    pos.menge = BigDecimal.ONE
                    pos.einzelNetto = BigDecimal.TEN
                    pos.vat = BigDecimal.ZERO
                    pos.text = "Pos"
                })
            }
            invoice.nummer = rechnungDao.getNextNumber(invoice)
            rechnungDao.insert(invoice, checkAccess = false)
            invoice.id!!
        }
    }

    companion object {
        private const val UNPAID = "unbezahlt"
    }
}
