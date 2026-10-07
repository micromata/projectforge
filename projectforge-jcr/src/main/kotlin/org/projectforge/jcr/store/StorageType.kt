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

package org.projectforge.jcr.store

/**
 * Where the content (binary) of a file is located. The metadata of all files is always stored in the database
 * (table `pf_files.t_attachment`).
 */
enum class StorageType {
    /** Content is stored compressed (or not) as chunks in `pf_files.t_attachment_chunk`. */
    DB,

    /** Content is stored as a plain file in the file system (e. g. data transfer files, which are big and short-lived). */
    FS,
}
