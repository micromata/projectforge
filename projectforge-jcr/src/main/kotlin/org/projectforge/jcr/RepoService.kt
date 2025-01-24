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

import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import javax.jcr.Node

private val log = KotlinLogging.logger {}

@Service
open class RepoService {
    private var repoStore: OakStorage? = null

    /**
     * JDBC URL for RDB storage, e.g.: `jdbc:postgresql://localhost:5432/your_database`.
     * If empty, the segmentTarStore will be used.
     */
    @Value("\${projectforge.jcr.rdb.jdbc.url:}")
    var jdbcUrl: String? = null
        private set

    @Value("\${projectforge.jcr.rdb.jdbc.user:}")
    var jdbcUser: String? = null
        private set

    @Value("\${projectforge.jcr.rdb.jdbc.password:}")
    internal var jdbcPassword: String? = null
        private set

    val mainNodeName: String?
        get() = repoStore?.mainNodeName

    val fileStoreLocation: File?
        get() = (repoStore as? SegmentTarStorage)?.fileStoreLocation

    open fun cleanup() {
        repoStore?.cleanup()
    }

    @PreDestroy
    fun shutdown() {
        log.info { "Shutting down JCR repositories..." }
        repoStore?.shutdown()
    }

    /**
     * Should only be called by test cases if you need to initialize a repo multiple times.
     */
    fun internalResetForJunitTestCases() {
        repoStore = null
    }

    /**
     * @param mainNodeName All activities (working with nodes) will done under topNode. TopNode should be given for backing up and
     * restoring. By default, "ProjectForge" is used.
     */
    @JvmOverloads
    fun init(repositoryDir: File, mainNodeName: String = "ProjectForge") {
        synchronized(this) {
            if (repoStore != null) {
                throw IllegalArgumentException("Can't initialize segmentTarStore twice! segmentTarStore=$repoStore")
            }
            if (mainNodeName.isBlank()) {
                throw IllegalArgumentException("Top node shouldn't be empty!")
            }
            if (jdbcUrl.isNullOrBlank()) {
                repoStore = SegmentTarStorage(mainNodeName, repositoryDir)
            } else {
                repoStore = RDBStorage(mainNodeName, this)
            }
        }
    }

    /**
     * @param parentNodePath Path, nodes are separated by '/', e. g. "world/germany". The nodes of this path must already exist.
     * For creating top level nodes (direct child of main node), set parentNode to null, empty string or "/".
     * @param relPath Sub node parent node to create if not exists. Null value results in nop.
     */
    open fun ensureNode(parentNodePath: String?, relPath: String? = null): Node? {
        return repoStore!!.ensureNode(parentNodePath, relPath)
    }

    @JvmOverloads
    open fun storeProperty(
        parentNodePath: String?,
        relPath: String?,
        name: String,
        value: String,
        ensureRelNode: Boolean = true
    ) {
        repoStore!!.storeProperty(parentNodePath, relPath, name, value, ensureRelNode)
    }

    open fun retrievePropertyString(parentNodePath: String?, relPath: String?, name: String): String? {
        return repoStore!!.retrievePropertyString(parentNodePath, relPath, name)
    }

    /**
     * Content of file should be given as [FileObject.content].
     * @param password Optional password for encryption. The password will not be stored in any kind!
     */
    @JvmOverloads
    open fun storeFile(
        fileObject: FileObject,
        fileSizeChecker: FileSizeChecker,
        user: String? = null,
        password: String? = null,
    ) {
        repoStore!!.storeFile(fileObject, fileSizeChecker, user, password)
    }

    /**
     * @param password Optional password for encryption. The password will not be stored in any kind!
     */
    @JvmOverloads
    open fun storeFile(
        fileObject: FileObject,
        content: InputStream,
        fileSizeChecker: FileSizeChecker,
        user: String? = null,
        /**
         * Optional data e. g. for fileSizeChecker of data transfer area size.
         */
        data: Any? = null,
        password: String? = null,
    ) {
        repoStore!!.storeFile(fileObject, content, fileSizeChecker, user, data, password)
    }


    open fun deleteFile(fileObject: FileObject): Boolean {
        return repoStore!!.deleteFile(fileObject)
    }

    open fun deleteNode(nodeInfo: NodeInfo): Boolean {
        return repoStore!!.deleteNode(nodeInfo)
    }

    /**
     * @return list of file infos without content.
     */
    @JvmOverloads
    open fun getFileInfos(parentNodePath: String?, relPath: String? = null): List<FileObject>? {
        return repoStore!!.getFileInfos(parentNodePath, relPath)
    }

    /**
     * @return file info without content.
     */
    @JvmOverloads
    open fun getFileInfo(
        parentNodePath: String?,
        relPath: String? = null,
        fileId: String? = null,
        fileName: String? = null
    ): FileObject? {
        return repoStore!!.getFileInfo(parentNodePath, relPath, fileId, fileName)
    }

    /**
     * Change fileName and/or description if given.
     * @param updateLastUpdateInfo If true (default),
     * time stamp of last update and user of this update will be updated. Otherwise time stamp and user info will be left untouched.
     * @return new file info without content.
     */
    @JvmOverloads
    open fun changeFileInfo(
        fileObject: FileObject,
        user: String,
        newFileName: String? = null,
        newDescription: String? = null,
        newZipMode: ZipMode? = null,
        updateLastUpdateInfo: Boolean = true,
    ): FileObject? {
        return repoStore!!.changeFileInfo(
            fileObject,
            user,
            newFileName,
            newDescription,
            newZipMode,
            updateLastUpdateInfo
        )
    }

    /**
     * Returns the already calculated checksum or calculates it, if not given.
     * @return new file info including checksum without content.
     */
    open fun checksum(fileObject: FileObject): String? {
        return repoStore!!.checksum(fileObject)
    }

    @JvmOverloads
    open fun getNodeInfo(absPath: String, recursive: Boolean = false): NodeInfo {
        return repoStore!!.getNodeInfo(absPath, recursive)
    }

    internal fun getFileInfos(
        filesNode: Node?,
        parentNodePath: String? = null,
        relPath: String? = null
    ): List<FileObject>? {
        return repoStore!!.getFileInfos(filesNode, parentNodePath, relPath)
    }

    internal fun findFile(filesNode: Node?, fileId: String?, fileName: String? = null): Node? {
        return repoStore!!.findFile(filesNode, fileId, fileName)
    }

    @JvmOverloads
    open fun retrieveFile(fileObject: FileObject, password: String? = null): Boolean {
        return repoStore!!.retrieveFile(fileObject, password)
    }

    open fun retrieveFileInputStream(fileObject: FileObject, password: String? = null): InputStream? {
        return repoStore!!.retrieveFileInputStream(fileObject, password)
    }

    internal fun getFileContent(
        node: Node?,
        fileObject: FileObject,
        password: String? = null,
        useEncryptedFile: Boolean = false,
    ): ByteArray? {
        return repoStore!!.getFileContent(node, fileObject, password, useEncryptedFile)
    }

    /**
     * @param password Must be given for encrypted file to decrypt (if useEncryptedFile isn't true)
     * @param useEncryptedFile If true, work with encrypted file directly without password and decryption.
     * Used by internal checksum and backup functionality.
     */
    internal fun getFileInputStream(
        node: Node?,
        fileObject: FileObject,
        suppressLogInfo: Boolean = false,
        password: String? = null,
        useEncryptedFile: Boolean = false,
    ): InputStream? {
        return repoStore!!.getFileInputStream(node, fileObject, suppressLogInfo, password, useEncryptedFile)
    }

    internal fun getFileSize(node: Node?, fileObject: FileObject, suppressLogInfo: Boolean = false): Long? {
        return repoStore!!.getFileSize(node, fileObject, suppressLogInfo)
    }

    internal fun getNode(
        session: SessionWrapper,
        parentNodePath: String?,
        relPath: String? = null,
        ensureRelNode: Boolean = true
    ): Node {
        return repoStore!!.getNode(session, parentNodePath, relPath, ensureRelNode)
    }

    internal fun getNodeOrNull(
        session: SessionWrapper,
        parentNodePath: String?,
        relPath: String? = null,
        ensureRelNode: Boolean = true
    ): Node? {
        return repoStore!!.getNodeOrNull(session, parentNodePath, relPath, ensureRelNode)
    }

    fun getAbsolutePath(nodePath: String?): String {
        return repoStore!!.getAbsolutePath(nodePath)
    }

    internal fun ensureNode(parentNode: Node, relPath: String?): Node {
        return repoStore!!.ensureNode(parentNode, relPath)
    }

    internal fun <T> runInSession(method: (sessionWrapper: SessionWrapper) -> T): T {
        return repoStore!!.runInSession(method)
    }

}
