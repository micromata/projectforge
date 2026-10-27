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


package org.projectforge.plugins.cveradar.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Date

/**
 * How far an import source got: the next run asks for the vulnerabilities modified since [lastModWatermark]. It is
 * advanced only after a time range was imported completely, so an aborted run resumes there (IMP-06).
 */
@Entity
@Table(name = "T_PLUGIN_CVERADAR_IMPORT_STATE")
open class CvrImportStateDO {
    /** The import source, e.g. [org.projectforge.plugins.cveradar.sync.NvdImportService.SOURCE]. */
    @get:Id
    @get:Column(name = "source", length = 20)
    open var source: String? = null

    /** Everything modified before this time is imported. Null before the first successful run. */
    @get:Column(name = "last_mod_watermark")
    open var lastModWatermark: Date? = null

    @get:Column(name = "last_run")
    open var lastRun: Date? = null

    /** The error of the last run, null if it succeeded. */
    @get:Column(name = "last_error", length = 4000)
    open var lastError: String? = null
}
