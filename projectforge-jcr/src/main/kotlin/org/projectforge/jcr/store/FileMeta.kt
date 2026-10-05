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

import org.projectforge.common.ZipMode
import org.projectforge.jcr.FileObject
import java.util.Date

/**
 * One row of `pf_files.t_attachment`: the metadata of a stored file.
 */
internal class FileMeta(
    var pk: Long? = null,
    var fileId: String,
    /** Normalized parent node path (without main node), e. g. `org.projectforge.fibu.RechnungDO/123`. */
    var parentPath: String,
    /** Normalized relative path (list id), e. g. `attachments`. */
    var relPath: String,
    var fileName: String? = null,
    var description: String? = null,
    /** Size of the payload (after an optional AES encryption, before compression). */
    var size: Long? = null,
    /** Size of the stored content (after compression). */
    var storedSize: Long? = null,
    var chunkCount: Int = 0,
    var compression: CompressionType = CompressionType.NONE,
    var storage: StorageType = StorageType.DB,
    /** SHA-256 of the payload, formatted as "SHA256: <hex>" (as before in the JCR). */
    var checksum: String? = null,
    var aesEncrypted: Boolean = false,
    var zipMode: ZipMode? = null,
    var created: Date? = null,
    var createdBy: String? = null,
    var lastUpdate: Date? = null,
    var lastUpdateBy: String? = null,
) {
    /**
     * @param parentNodePath The parent node path as given by the caller (the stored one is normalized).
     * @param relPath The relative path as given by the caller.
     */
    fun toFileObject(
        parentNodePath: String? = parentPath,
        relPath: String? = this.relPath.ifEmpty { null },
    ): FileObject {
        val meta = this
        return FileObject(parentNodePath, relPath, fileId).also {
            it.fileName = meta.fileName
            it.description = meta.description
            it.size = meta.size
            it.checksum = meta.checksum
            it.aesEncrypted = meta.aesEncrypted
            it.zipMode = meta.zipMode
            it.created = meta.created
            it.createdByUser = meta.createdBy
            it.lastUpdate = meta.lastUpdate
            it.lastUpdateByUser = meta.lastUpdateBy
        }
    }

    override fun toString(): String {
        return "location=[$parentPath/$relPath],id=[$fileId],fileName=[$fileName],size=[$size],storedSize=[$storedSize],storage=[$storage],compression=[$compression]"
    }
}
