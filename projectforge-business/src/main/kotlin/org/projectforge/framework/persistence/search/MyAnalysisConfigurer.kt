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

import org.apache.lucene.analysis.classic.ClassicTokenizerFactory
import org.apache.lucene.analysis.core.FlattenGraphFilterFactory
import org.apache.lucene.analysis.core.LowerCaseFilterFactory
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory
import org.apache.lucene.analysis.de.GermanNormalizationFilterFactory
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory
import org.apache.lucene.analysis.standard.StandardTokenizerFactory
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer
import org.hibernate.search.engine.backend.analysis.AnalyzerNames

class MyAnalysisConfigurer : LuceneAnalysisConfigurer {

    // Every analyzer ends with GermanNormalizationFilter and ASCIIFoldingFilter, so a search ignores accents and
    // the German umlaut spellings: "Andre" finds "André", "Mueller" and "Muller" find "Müller", "Strasse" finds
    // "Straße" (and vice versa). GermanNormalizationFilter maps ä/ae -> a, ö/oe -> o, ü/ue -> u (not "ue" after
    // "q") and ß -> ss, ASCIIFoldingFilter does the rest (é -> e, ç -> c, ø -> o, ...).
    // Both filters are normalizing filters, so the query parser applies them to wildcard terms ("andré*") as
    // well, which it doesn't tokenize. The index and the query are therefore normalized the same way. Side
    // effect: "ae", "oe", "ue" inside a word are normalized too ("Michael" -> "michal"), so "Michal" also finds
    // "Michael". See SearchStringTokenizer.normalize for the same chain outside of Lucene queries.
    // A changed analyzer only applies to entries indexed after the change: the index has to be rebuilt.

    override fun configure(context: LuceneAnalysisConfigurationContext) {
        // The analyzer of every @FullTextField without an analyzer of its own (e.g. names of users and
        // addresses). It replaces Lucene's StandardAnalyzer (standard tokenizer, lower case, no stop words) by
        // the same plus the normalization.
        context.analyzer(AnalyzerNames.DEFAULT)
            .custom()
            .tokenizer(StandardTokenizerFactory::class.java)
            .tokenFilter(LowerCaseFilterFactory::class.java)
            .tokenFilter(GermanNormalizationFilterFactory::class.java)
            .tokenFilter(ASCIIFoldingFilterFactory::class.java)

        // Splits only on whitespace, so every word of a name becomes its own term while special characters
        // stay attached (e.g. "A+B Energy" -> "a+b", "energy"). This is what lets a search find a word *inside*
        // a customer name ("post" finds "Muster Post AG", "+muster -post" excludes it) while still matching
        // a name that holds a character the standard analyzer would strip. A KeywordTokenizer used to index the
        // whole name as a single term, so only a prefix of the whole name matched and "post" missed "Muster
        // Post" entirely. Prefix matching still works via the wildcard expansion in SearchStringTokenizer.
        context.analyzer("customAnalyzer")
            .custom()
            .tokenizer(WhitespaceTokenizerFactory::class.java) // One term per word, keeping special characters.
            .tokenFilter(LowerCaseFilterFactory::class.java) // Optional for case-insensitive search
            .tokenFilter(SynonymGraphFilterFactory::class.java)
            .param("synonyms", "luceneSynonyms.txt")
            .param("ignoreCase", "true")
            .param("expand", "true")
            .tokenFilter(GermanNormalizationFilterFactory::class.java)
            .tokenFilter(ASCIIFoldingFilterFactory::class.java)

        // Technical names such as group names ("PF_Controlling", "a.b"): the standard tokenizer keeps a word with
        // an underscore or a dot between letters as one term, so "controlling" didn't find "PF_Controlling".
        // The word parts are indexed additionally, the original term is preserved: "controlling", "pf",
        // "pf_con*" and "PF_Controlling" all match. Case changes and digits don't split ("MyGroup1" stays).
        context.analyzer("wordPartsAnalyzer")
            .custom()
            .tokenizer(StandardTokenizerFactory::class.java)
            .tokenFilter(WordDelimiterGraphFilterFactory::class.java)
            .param("preserveOriginal", "1")
            .param("generateWordParts", "1")
            .param("generateNumberParts", "1")
            .param("splitOnCaseChange", "0")
            .param("splitOnNumerics", "0")
            .tokenFilter(FlattenGraphFilterFactory::class.java) // Required after a graph filter at index time.
            .tokenFilter(LowerCaseFilterFactory::class.java)
            .tokenFilter(GermanNormalizationFilterFactory::class.java)
            .tokenFilter(ASCIIFoldingFilterFactory::class.java)

        // Analyzer that preserves JIRA issue patterns (e.g., ACME-1234)
        // Uses WhitespaceTokenizer to keep JIRA issues as single tokens
        context.analyzer("jiraPreservingAnalyzer")
            .custom()
            .tokenizer(ClassicTokenizerFactory::class.java) // Preserves ACME-1234.
            .tokenFilter(LowerCaseFilterFactory::class.java) // Make search case-insensitive
            .tokenFilter(GermanNormalizationFilterFactory::class.java)
            .tokenFilter(ASCIIFoldingFilterFactory::class.java)

        /*        context.analyzer("customAnalyzer")
                    .custom()
                    .tokenizer(StandardTokenizerFactory::class.java)
                    .tokenFilter(LowerCaseFilterFactory::class.java)
                    .tokenFilter(KeywordRepeatFilterFactory::class.java)
                    .tokenFilter(PorterStemFilterFactory::class.java)
                    .tokenFilter(TrimFilterFactory::class.java)
                    .tokenFilter(SnowballPorterFilterFactory::class.java).param("language", "German")
                    .tokenFilter(RemoveDuplicatesTokenFilterFactory::class.java)*/
    }
}
