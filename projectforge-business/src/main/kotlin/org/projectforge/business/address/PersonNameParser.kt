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

package org.projectforge.business.address

/**
 * Parsed name with titles, form of address, first name and last name.
 */
data class ParsedName(
    val name: String,                 // Nachname
    val firstName: String,            // Vorname(n)
    val formOfAddress: String?,       // Anrede (Herr, Frau, Mr, ...)
    val titles: List<String>          // Titel, z. B. ["Dipl.-Phys.", "Dr."]
)

/**
 * Parser for extracting names with titles and forms of address.
 */
object NameParser {
    // 1) Anreden (international, erweiterbar)
    private val salutationRe = Regex(
        pattern = """^(?:\s*)(herr|frau|fr|mr|mrs|ms|miss|mx|sir|dame|m\.|mme|mlle|sr|sra|srta|señor|señora|señorita|sig\.?|sig\.ra|signor|signora|don|doña)\.?\s+""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    // 2) Titel-Bausteine
    // Einzel-Token wie Dr., Prof., Mag., Ing., PhD, MD, MBA, BSc, MSc, etc.
    private const val titleTokenCore = """Prof|Dr|Mag|Ing|Dipl|Lic|PhD|MD|DDS|DVM|CPA|MBA|M\.?Sc|B\.?Sc|M\.?A|B\.?A|B\.?Eng|M\.?Eng|M\.?Phil|LL\.?M|LL\.?B"""
    private val titleToken = """(?:$titleTokenCore)\.?"""

    // "Dipl."-Ketten inkl. Fach: Dipl.-Phys., Dipl.-Ing., Dipl. Kfm., …
    private val diplChain = """Dipl\.?(?:[-\s]+[A-Za-zÄÖÜäöüß]{2,15}\.?)"""

    // Zusätze wie "rer. nat.", "iur.", "med.", "h.c.", "habil." nach Dr./Prof. etc.
    private val specToken = """(?:rer\.?\s?nat\.?|rer\.?\s?pol\.?|iur\.?|jur\.?|med\.?|phil\.?|theol\.?|oec\.?|techn\.?|habil\.?|h\.?c\.?)"""

    // Vollständige Titelkette (am Anfang), z. B. "Prof. Dr. med.", "Dipl.-Ing.", "Dr.-Ing.", …
    private val titleComposite = Regex(
        pattern = """^(?:\s*)((?:(?:$titleToken|$diplChain)(?:[-\s]+(?:$titleToken|$diplChain))*)(?:[-\s]+$specToken(?:[-\s]+$specToken)*)?)\s+""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun parse(input: String): ParsedName {
        var s = input.trim().replace(Regex("""\s+"""), " ")

        // A) Anrede am Anfang?
        var formOfAddress: String? = null
        salutationRe.find(s)?.let { m ->
            formOfAddress = m.groupValues[1].trim().trimEnd('.')
            s = s.removePrefix(m.value).trimStart()
        }

        // B) Titelkette direkt danach?
        val titles = mutableListOf<String>()
        titleComposite.find(s)?.let { m ->
            val block = m.groupValues[1].trim()
            // Dipl.-…-Blöcke zuerst als Ganzes
            val diplRegex = Regex(diplChain, RegexOption.IGNORE_CASE)
            var rest = block
            diplRegex.findAll(block).forEach { hit ->
                titles += normalizeTitle(hit.value)
                rest = rest.replace(hit.value, " ")
            }
            // Restliche Einzel-Titel/Spezialzusätze
            rest.split(Regex("""[-\s]+"""))
                .filter { it.isNotBlank() }
                .forEach { titles += normalizeTitle(it) }

            // vom String entfernen
            s = s.removePrefix(m.value).trimStart()
        }

        // C) Restlicher Personenname -> Vorname(n) + Nachname (letztes Token = Nachname)
        val parts = s.split(" ").filter { it.isNotBlank() }
        val (firstName, lastName) = when {
            parts.isEmpty() -> "" to ""
            parts.size == 1 -> "" to parts[0]
            else -> parts.dropLast(1).joinToString(" ") to parts.last()
        }

        // Dubletten entfernen, Reihenfolge beibehalten
        val dedupedTitles = titles.fold(mutableListOf<String>()) { acc, t ->
            if (acc.none { it.equals(t, ignoreCase = true) }) acc.add(t)
            acc
        }

        return ParsedName(
            name = lastName,
            firstName = firstName,
            formOfAddress = formOfAddress,
            titles = dedupedTitles
        )
    }

    private fun normalizeTitle(raw: String): String {
        // leichte Normalisierung: Punkte bei bekannten Tokens hinzufügen/erhalten, Mehrfachtrenner bereinigen
        val t = raw.trim().replace(Regex("""\s+"""), " ")
        // Beispiele für leichte Normalisierung
        return when (t.lowercase()) {
            "prof" -> "Prof."
            "dr" -> "Dr."
            "mag" -> "Mag."
            "ing" -> "Ing."
            "phd" -> "PhD"
            "md" -> "MD"
            "mba" -> "MBA"
            "msc" -> "MSc"
            "bsc" -> "BSc"
            "ma" -> "MA"
            "ba" -> "BA"
            else -> t
        }
    }
}
