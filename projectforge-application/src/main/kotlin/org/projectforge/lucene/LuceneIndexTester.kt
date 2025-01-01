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

package org.projectforge.lucene

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Terms
import org.apache.lucene.index.TermsEnum
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths

fun main() {
    // Aufruf
    LuceneIndexTester().analyzeIndex("${System.getProperty("user.home")}/ProjectForge/hibernateSearch/AddressDO")

}
class LuceneIndexTester {

    fun analyzeIndex(indexPath: String) {
        FSDirectory.open(Paths.get(indexPath)).use { directory ->
            DirectoryReader.open(directory).use { reader ->
                for (leaf in reader.leaves()) {
                    val leafReader = leaf.reader()
                    val fieldInfos = leafReader.fieldInfos

                    for (fieldInfo in fieldInfos) {
                        val sb = StringBuilder()
                        sb.append("Field ").append(fieldInfo.name).append(": ")
                        // Abrufen der Term-Daten für das Feld
                        val terms: Terms? = leafReader.terms(fieldInfo.name)
                        if (terms != null) {
                            sb.append(" Number of Terms=").append(terms.size() ?: "unknown")
                            val termsEnum: TermsEnum = terms.iterator()
                            var docFrequencySum = 0L

                            while (termsEnum.next() != null) {
                                docFrequencySum += termsEnum.docFreq()
                            }
                            sb.append(", total document frequency=").append(docFrequencySum)
                        }
                        println(sb.toString())
                    }
                }
            }
        }
    }
}
