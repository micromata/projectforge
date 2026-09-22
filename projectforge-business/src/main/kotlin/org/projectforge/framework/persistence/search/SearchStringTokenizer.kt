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

package org.projectforge.framework.persistence.search

import mu.KotlinLogging
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

private val log = KotlinLogging.logger {}

/**
 * Turns what the user typed into terms the Lucene index actually holds.
 *
 * A field annotated with `@FullTextField` and no analyzer of its own is indexed by the Hibernate Search
 * default analyzer, and that is Lucene's [StandardAnalyzer] (`LuceneDefaultAnalysisConfigurer`). Its
 * tokenizer treats a hyphen as a word separator, so `dhl-pop` is stored as the two terms `dhl` and `pop` —
 * there is no term beginning with `dhl-pop`.
 *
 * That matters because a search string carries wildcards: `dhl-pop*` is parsed as a wildcard query on a
 * *single* term, and Lucene does not tokenize a wildcard term (it only normalizes it, i.e. lowercases it).
 * So the query asked for something the index cannot contain, while `dhl pop` (two words, hence `dhl* pop*`)
 * matched. The same holds for anything else the analyzer splits, an email address in the user picker for
 * instance.
 *
 * [expandWord] therefore offers both readings of a word, the raw one and the split one. The raw one has to
 * stay, because not every searched field is analyzed this way: `KundeDO.name` uses `customAnalyzer`
 * (a `WhitespaceTokenizer` — it splits only on whitespace, so `K+S` stays one term and a prefix of a word
 * matches), and `TaskDO.title` or `TimesheetDO.reference` use `jiraPreservingAnalyzer` (a `ClassicTokenizer`, which
 * keeps `ACME-1234` in one piece). Splitting alone would break those, splitting or matching whole covers
 * both — and needs no reindex.
 *
 * @see MyAnalysisConfigurer for the analyzers of this application.
 */
object SearchStringTokenizer {
    /**
     * The Hibernate Search default analyzer. Analyzers are thread safe and meant to be reused, so one
     * instance for the lifetime of the application.
     */
    private val analyzer = StandardAnalyzer()

    /**
     * The terms the index holds for [text], i.e. [text] run through the analyzer of the searched fields.
     *
     * @return The terms, lower cased, in the order of their appearance. Empty if [text] holds nothing the
     * analyzer considers a word (`-`, `###`).
     */
    fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        try {
            // The field name is irrelevant: the analyzer is chosen here, not looked up by field.
            analyzer.tokenStream("searchString", text).use { stream ->
                val term = stream.addAttribute(CharTermAttribute::class.java)
                stream.reset()
                while (stream.incrementToken()) {
                    tokens.add(term.toString())
                }
                stream.end()
            }
        } catch (ex: Exception) {
            // Analyzing a search string must not fail a search: the caller falls back to the word as typed.
            log.error(ex) { "Can't tokenize search string '$text': ${ex.message}" }
            return emptyList()
        }
        return tokens
    }

    /**
     * One word of a search string as a Lucene query, matching it either as typed or as the terms the index
     * holds for it.
     *
     * A word that is one term of the index anyway is answered as before, `dhl` -> `dhl*`, so a search whose
     * words hold no separator is not changed at all. A word the analyzer reads differently gets both readings:
     * `dhl-pop` -> `(dhl-pop* (+dhl* +pop*))`, `dhl-` -> `(dhl-* (+dhl*))`. The terms are required among
     * themselves (all of `dhl` and `pop` must be found), the two readings are alternatives (either matches).
     *
     * @param word One word of the search string, i.e. without whitespace. Lucene operators and syntax are
     * none of this function's business — the caller decides what is a word (see [HibernateSearchFilterUtils]).
     * @param wildcard The wildcard character, `*` for full text search and `%` for the criteria search. The
     * query syntax of the alternatives only exists in the full text search, so anything but `*` is answered
     * as the word plus the wildcard.
     * @param required Whether the word must match, i.e. whether the answer is prefixed by `+`.
     * @return The query for this word, the word itself if the analyzer finds no term in it at all.
     */
    fun expandWord(word: String, wildcard: String = "*", required: Boolean = false): String {
        val prefix = if (required) "+" else ""
        if (wildcard != "*") {
            return "$prefix$word$wildcard"
        }
        val tokens = tokenize(word)
        if (tokens.isEmpty()) {
            // Nothing to search for, e. g. '-' or '###': the word as typed, for the parser to deal with.
            return word
        }
        // Nothing was split -> the query stays the one this application always built.
        return alternatives(word, tokens, wildcard, prefix) ?: "$prefix$word$wildcard"
    }

    /**
     * [expandWord], but only for a word the analyzer reads differently than it was typed.
     *
     * For everything else this answers null, so that a caller with a more careful notion of what a word is
     * (see [HibernateSearchFilterUtils.modifySearchString], which knows about Lucene operators) can keep its
     * own handling for those.
     *
     * @return The query matching the word as typed or as the terms of the index, null if the word is one term
     * of the index as typed, if the analyzer finds no term in it at all, or if [wildcard] is not `*`.
     */
    fun expandSplitWord(word: String, wildcard: String = "*", required: Boolean = false): String? {
        if (wildcard != "*") {
            return null
        }
        return alternatives(word, tokenize(word), wildcard, if (required) "+" else "")
    }

    /**
     * `(word* (+t1* +t2*))`: match the word as typed, or all of the terms the index holds for it.
     *
     * The alternative is needed whenever the analyzer reads the word differently than it was typed - not only
     * when it splits it. A word ending in a separator is the case that made this obvious: the index holds `dhl`
     * for `dhl-`, so the wildcard term `dhl-*` matches nothing, while `dhl` and `dhl-p` both work.
     *
     * @return null if there is nothing to offer an alternative to, i.e. if the word is one term as typed.
     */
    private fun alternatives(word: String, tokens: List<String>, wildcard: String, prefix: String): String? {
        if (tokens.isEmpty() || (tokens.size == 1 && tokens.first() == word.lowercase())) {
            return null
        }
        val split = tokens.joinToString(" ") { "+$it$wildcard" }
        if (!word.first().isLetterOrDigit()) {
            // A word beginning with anything else would be an operator of the parser rather than a word
            // ('-dhl' is a prohibited clause), so only the terms of the index are offered for it.
            return "$prefix($split)"
        }
        return "$prefix($word$wildcard ($split))"
    }
}
