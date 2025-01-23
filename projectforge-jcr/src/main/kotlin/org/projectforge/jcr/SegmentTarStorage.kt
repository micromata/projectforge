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

package org.projectforge.jcr

import mu.KotlinLogging
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.jcr.Jcr
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders
import org.apache.jackrabbit.oak.segment.file.FileStore
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder
import org.projectforge.common.FormatterUtils
import java.io.File
import java.io.OutputStream

private val log = KotlinLogging.logger {}

/**
 * JCR repository with segment node store and tar file store.
 * @param mainNodeName The name of the top node.
 * @param fileStoreLocation The location of the file store (existing directory).
 *
 */
internal class SegmentTarStorage(
    mainNodeName: String,
    val fileStoreLocation: File,
) : OakStorage(mainNodeName) {
    /**
     * For information.
     */
    class FileStoreInfo(repoStore: SegmentTarStorage) {
        val approximateSize = FormatterUtils.formatBytes(repoStore.fileStore?.stats?.approximateSize)
        val tarFileCount = repoStore.fileStore?.stats?.tarFileCount
        val location = repoStore.fileStoreLocation.absolutePath

        override fun toString(): String {
            return PFJcrUtils.toJson(this)
        }
    }

    private var fileStore: FileStore? = null

    override fun onLogout() {
        fileStore?.flush()
    }

    override fun afterSessionClose() {
        fileStore?.close()
    }

    override fun shutdown() {
        log.info { "Shutting down jcr filestore repository '$mainNodeName' in ${fileStoreLocation.absolutePath}..." }
        fileStore?.let {
            it.flush()
            it.compactFull()
            it.cleanup()
            log.info { "Jcr stats: ${FileStoreInfo(this)}" }
            it.close()
        }
        nodeStore.let {
            //log.warn { "Method not yet implemented: ${it.javaClass}.dispose()" }
            /*if (it is DocumentNodeStore) {
              it.dispose()
            }*/
        }
    }

    override fun cleanup() {
        log.info { "Cleaning JCR repository up..." }
        fileStore?.let {
            it.flush()
            it.compactFull()
            it.cleanup()
        }
    }

    init {
        if (mainNodeName.isBlank()) {
            throw IllegalArgumentException("Top node shouldn't be empty!")
        }
        log.debug { "Setting system property: derby.stream.error.field=${DerbyUtil::class.java.name}.DEV_NULL" }
        System.setProperty("derby.stream.error.field", "${DerbyUtil::class.java.name}.DEV_NULL")
        log.info { "Initializing JCR repository with main node '$mainNodeName' in: ${fileStoreLocation.absolutePath}" }

        FileStoreBuilder.fileStoreBuilder(fileStoreLocation).build().let { fileStore ->
            this.fileStore = fileStore
            nodeStore = SegmentNodeStoreBuilders.builder(fileStore).build()
            repository = Jcr(Oak(nodeStore)).createRepository()
        }

        runInSession { session ->
            if (!session.rootNode.hasNode(mainNodeName)) {
                log.info { "Creating top level node '$mainNodeName'." }
                session.rootNode.addNode(mainNodeName)
            }
            session.save()
        }
    }

    // https://stackoverflow.com/questions/1004327/getting-rid-of-derby-log
    object DerbyUtil {
        @JvmField
        val DEV_NULL: OutputStream = object : OutputStream() {
            override fun write(b: Int) {}
        }
    }
}
