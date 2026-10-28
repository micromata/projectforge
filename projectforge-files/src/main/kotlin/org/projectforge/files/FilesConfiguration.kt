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


package org.projectforge.files

import org.projectforge.files.store.FileStore
import org.projectforge.files.store.FileSystemCipher
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Provides the [FileStore] (all attachments, documents and DataTransfer files) on the data source of ProjectForge.
 */
@Configuration
open class FilesConfiguration {
    /**
     * If given, files of the file system storage (e. g. DataTransfer) are encrypted at rest with a key derived from it.
     */
    @Value("\${projectforge.files.fileSystemEncryptionKey:}")
    private var fileSystemEncryptionKey: String = ""

    @Bean
    open fun fileStore(dataSource: DataSource): FileStore {
        return FileStore(dataSource, FileStore.MAIN_NODE_NAME, FileSystemCipher.create(fileSystemEncryptionKey))
    }
}
