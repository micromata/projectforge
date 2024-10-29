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
