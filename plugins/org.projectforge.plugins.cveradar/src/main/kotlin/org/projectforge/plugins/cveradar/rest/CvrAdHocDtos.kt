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

package org.projectforge.plugins.cveradar.rest

import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.plugins.cveradar.LibraryFormat

/** The files of an ad-hoc check: lists of libraries ([LibraryFile.text]) or zips with lists ([LibraryFile.data]). */
class AdHocRequest(val files: List<LibraryFile> = emptyList())

/**
 * Where an ad-hoc check stands: the progress per file (packages checked of all), and the totals once it is
 * [AbstractJob.Status.FINISHED].
 */
class AdHocStatus(
    val jobId: Int,
    val status: AbstractJob.Status,
    /** Why a failed check failed (an already translated text). */
    val error: String?,
    val files: List<AdHocFileStatus>,
    val packageCount: Int,
    val checkedCount: Int,
    /** The vulnerabilities and the matches found so far. */
    val vulnerabilityCount: Int,
    val hitCount: Int,
)

/** [format] null: not recognised, no packages. */
class AdHocFileStatus(
    val name: String,
    val format: LibraryFormat?,
    val total: Int,
    val checked: Int,
    val hitCount: Int,
    val done: Boolean,
)
