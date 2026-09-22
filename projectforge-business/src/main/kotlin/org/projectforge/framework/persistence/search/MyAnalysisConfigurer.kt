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
import org.apache.lucene.analysis.core.LowerCaseFilterFactory
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer

class MyAnalysisConfigurer : LuceneAnalysisConfigurer {

    override fun configure(context: LuceneAnalysisConfigurationContext) {
        // Splits only on whitespace, so every word of a name becomes its own term while special characters
        // stay attached (e.g. "K+S Energy" -> "k+s", "energy"). This is what lets a search find a word *inside*
        // a customer name ("post" finds "Deutsche Post AG", "+deutsche -post" excludes it) while still matching
        // a name that holds a character the standard analyzer would strip. A KeywordTokenizer used to index the
        // whole name as a single term, so only a prefix of the whole name matched and "post" missed "Deutsche
        // Post" entirely. Prefix matching still works via the wildcard expansion in SearchStringTokenizer.
        context.analyzer("customAnalyzer")
            .custom()
            .tokenizer(WhitespaceTokenizerFactory::class.java) // One term per word, keeping special characters.
            .tokenFilter(LowerCaseFilterFactory::class.java) // Optional for case-insensitive search
            .tokenFilter(SynonymGraphFilterFactory::class.java)
            .param("synonyms", "luceneSynonyms.txt")
            .param("ignoreCase", "true")
            .param("expand", "true")

        // Analyzer that preserves JIRA issue patterns (e.g., ACME-1234)
        // Uses WhitespaceTokenizer to keep JIRA issues as single tokens
        context.analyzer("jiraPreservingAnalyzer")
            .custom()
            .tokenizer(ClassicTokenizerFactory::class.java) // Preserves ACME-1234.
            .tokenFilter(LowerCaseFilterFactory::class.java) // Make search case-insensitive

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
