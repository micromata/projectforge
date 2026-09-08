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

package org.projectforge.ui.filter

import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.ElementInfo

/**
 * The cost unit (Kost2) list filter, shared by every list that embeds a `kost2` and wants to filter by
 * it — the time sheets, an invoice's positions, and so on.
 *
 * A single text field that filters directly *and* suggests concrete cost units:
 * - The free text the user types filters the list. A number-looking term (`6.300`) becomes a **prefix
 *   search** on the cost number, so trimming trailing segments widens the result
 *   (`6.300.00.00` → `6.300.00` project → `6.300` customer → `6` number range, as the legacy `nummer:…*`).
 *   Any other text is ORed across the indexed cost unit fields, so a project name or a description
 *   filters too. The customer is reached through the number prefix, not a field of its own.
 * - A type-ahead against `cost2/autosearch` (marked [AutoCompletion.Type.KOST2]) suggests concrete cost
 *   units by number, description, project or customer name. The next frontend swaps the plain text input
 *   for that suggesting field (FilterKost2Field); picking a suggestion inserts only its number, which
 *   stays editable.
 *
 * A page adds the field in `addMagicFilterElements` via [createFilterElement] and consumes it in
 * `preProcessMagicFilter` via [preProcess] — both take the property path to the embedded `kost2` (the
 * default `"kost2"` fits an entity whose own property is named so). The Lucene field paths follow that
 * path, so the entity must index `<path>.nummer`, `<path>.description` and `<path>.projekt.name`.
 */
object Kost2FilterUtils {
    /** The id of the cost unit filter element (and the entry consumed in [preProcess]). */
    fun fieldId(kost2Path: String = "kost2"): String = "$kost2Path.nummer"

    /**
     * The filter element to add in `addMagicFilterElements`: a STRING field labelled "Kost2 – Nummer",
     * carrying the [AutoCompletion.Type.KOST2] type-ahead against `cost2/autosearch`.
     */
    fun createFilterElement(kost2Path: String = "kost2"): UIFilterElement {
        val element = UIFilterElement(fieldId(kost2Path))
        element.label = LayoutListFilterUtils.getLabel(
            ElementInfo(
                "number",
                i18nKey = "fibu.kost2.number",
                parent = ElementInfo("kost2", i18nKey = "fibu.kost2"),
            )
        )
        element.autoCompletion = AutoCompletion<Long>(
            url = AutoCompletion.getAutoCompletionUrl("cost2"),
            type = AutoCompletion.Type.KOST2.name,
        )
        return element
    }

    /**
     * Consumes the cost unit entry in `preProcessMagicFilter` (marks it synthetic) and adds the search to
     * [target]: a prefix search on the number for a number-looking term, else an OR over the indexed cost
     * unit fields. Does nothing if the entry is absent or empty. Returns true if an entry was consumed.
     */
    fun preProcess(target: QueryFilter, source: MagicFilter, kost2Path: String = "kost2"): Boolean {
        val entry = source.entries.find { it.field == fieldId(kost2Path) } ?: return false
        entry.synthetic = true
        val term = entry.value.value?.trim()?.trim('*', '%')?.trim()
        if (term.isNullOrEmpty()) {
            return true
        }
        val predicate = if (term.matches(NUMBER_REGEX)) {
            // Prefix on the keyword number field, so trimming trailing segments widens the search.
            DBPredicate.FullSearch("$term*", arrayOf("$kost2Path.nummer"))
        } else {
            DBPredicate.FullSearch(
                term,
                arrayOf("$kost2Path.nummer", "$kost2Path.description", "$kost2Path.projekt.name"),
                autoWildcardSearch = true,
            )
        }
        target.add(predicate)
        return true
    }

    /** Digits and dots only — a term that looks like a (partial) cost number. */
    private val NUMBER_REGEX = Regex("[0-9.]+")
}
