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

package org.projectforge.rest.importer

/**
 * The layout-free wire shape of an import in progress, returned by [AbstractImportRest]'s endpoints.
 *
 * It is the JSON analog of what the UILayout based [AbstractImportPageRest] renders into an ag-grid: the
 * filename and title of the upload, whether the data has already been reconciled with the database, the
 * aggregated counts ([info]), the entries to display, and any subclass specific view metadata ([meta]).
 *
 * A `null` [info] together with an empty [entries] and a `null` [filename] is how the frontend reads
 * "no import in progress".
 *
 * @param O The import DTO, reusing the same serializable [ImportEntry] shape the UILayout page emits.
 */
class ImportView<O : ImportPairEntry.Modified<O>>(
    /** The name of the uploaded file, or `null` if no import is in progress. */
    val filename: String? = null,
    /** The (translated) title of the import, e.g. including the target entity. */
    val title: String? = null,
    /** Whether [ImportStorage.reconcileImportStorage] has been called at least once. */
    val hasBeenReconciled: Boolean = false,
    /** The aggregated counts and detected/unknown columns, or `null` if no import is in progress. */
    val info: ImportStorageInfo? = null,
    /** The entries to display, already filtered by the display options that were passed. */
    val entries: List<ImportEntry<O>> = emptyList(),
    /** Subclass specific view metadata, e.g. `isPositionBasedImport` for the incoming-invoice import. */
    val meta: Map<String, Any>? = null,
)
