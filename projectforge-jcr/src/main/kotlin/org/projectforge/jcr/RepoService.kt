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

package org.projectforge.jcr

import jakarta.annotation.PreDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.ZipMode
import org.projectforge.jcr.store.FileStore
import org.projectforge.jcr.store.StorageType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import javax.jcr.Node
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

/**
 * Facade for storing files, the store is chosen by `projectforge.files.store`:
 * - jcr: all files are stored in the JCR (Apache Oak), as before. Files stored while db mode was active are still
 *   found in the new [FileStore].
 * - db: all files are stored by the new [FileStore]: metadata in the data base, content in the data base or, for paths
 *   registered by [registerFileSystemPath] (e. g. DataTransfer), in the file system. Files not yet migrated (see
 *   [RepoMigrationService]) are still found in the JCR.
 *
 * Migrated files are kept in the JCR as copies (for switching back or rolling back to a release without the new
 * store), but deleted there too, if deleted.
 */
@Service
open class RepoService {
    private var repoStore: OakStorage? = null

    @Autowired
    internal lateinit var repoConfig: RepoConfig

    /**
     * Data source of ProjectForge (for the new [FileStore]). Not available in plain unit tests of this module.
     */
    @Autowired(required = false)
    internal var dataSource: DataSource? = null

    /**
     * The new store (null, if no data source is available).
     */
    var fileStore: FileStore? = null
        private set

    private val fileSystemPaths = mutableMapOf<String, File>()

    /**
     * Store of all files: "jcr" (only the registered file system paths are stored by the [FileStore]) or "db" (all
     * files are stored by the [FileStore]). If not given, it's detected on [init], see [detectFilesStore].
     */
    @Value("\${projectforge.files.store:}")
    internal var filesStore: String = ""

    /**
     * True, if all files are stored by the [FileStore] (`projectforge.files.store=db`).
     */
    val allFilesInFileStore: Boolean
        get() = fileStore != null && filesStore.trim().equals(FILES_STORE_DB, ignoreCase = true)

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
        fileStore = null
    }

    /**
     * Files below the given path are stored in the file system (in the given directory) by the new [FileStore] instead
     * of the data base (db mode only).
     * @param path The path (e. g. jcrPath of DataTransfer).
     */
    fun registerFileSystemPath(path: String, dir: File) {
        synchronized(this) {
            fileSystemPaths[path] = dir
            fileStore?.registerFileSystemPath(path, dir)
        }
    }

    /**
     * The registered paths of the file system storage (see [registerFileSystemPath]).
     */
    val registeredFileSystemPaths: Set<String>
        get() = synchronized(this) { fileSystemPaths.keys.toSet() }

    /**
     * @param mainNodeName All activities (working with nodes) will done under topNode. TopNode should be given for backing up and
     * restoring. By default, "ProjectForge" is used.
     */
    @JvmOverloads
    fun init(repositoryDir: File, mainNodeName: String = "ProjectForge") {
        synchronized(this) {
            if (repoStore != null) {
                throw IllegalArgumentException("Can't initialize oak store twice! oak store=$repoStore")
            }
            if (mainNodeName.isBlank()) {
                throw IllegalArgumentException("Top node shouldn't be empty!")
            }
            if (repoConfig.dataSourceConfigured) {
                val dataSource = repoConfig.dataSource
                if (dataSource == null) {
                    log.error { "No available data source for Oak!" }
                    throw IllegalArgumentException("No available data source for Oak!")
                }
                repoStore = RDBStorage(mainNodeName, dataSource)
            } else {
                repoStore = SegmentTarStorage(mainNodeName, repositoryDir)
            }
            val ds = dataSource
            if (ds != null) {
                val store = FileStore(ds, mainNodeName).also { store ->
                    fileSystemPaths.forEach { (path, dir) -> store.registerFileSystemPath(path, dir) }
                }
                fileStore = store
                val mode = filesStore.trim().lowercase()
                filesStore = when (mode) {
                    FILES_STORE_JCR, FILES_STORE_DB -> mode
                    "" -> detectFilesStore(store).also {
                        log.info { "projectforge.files.store not configured, detected: $it" }
                    }

                    else -> {
                        log.error { "Unknown value '$filesStore' of projectforge.files.store (jcr or db expected), using jcr." }
                        FILES_STORE_JCR
                    }
                }
                log.info { "Files are stored ${if (allFilesInFileStore) "by the file store (projectforge.files.store=db)" else "in the JCR (projectforge.files.store=jcr), only files stored while db mode was active are read from the file store"}." }
            } else {
                log.warn { "No data source available, all files are stored in the JCR (OK for test cases)." }
            }
        }
    }

    /**
     * Used, if projectforge.files.store isn't configured:
     * - db, if the file store already contains files of the data base storage (files store db was used before).
     * - jcr, if the JCR already contains data (existing installation, files stay in the JCR until switched to db).
     * - db otherwise (new installation). In db mode, nothing is written to the JCR, so the detection remains stable.
     */
    private fun detectFilesStore(store: FileStore): String {
        if (store.getStatistics().any { it.storage == StorageType.DB && it.count > 0 }) {
            return FILES_STORE_DB
        }
        return if (repoStore!!.hasContent()) FILES_STORE_JCR else FILES_STORE_DB
    }

    /**
     * @return The new store, if all files are stored by the new store (projectforge.files.store=db), otherwise null
     * (JCR). New files are written into this store.
     */
    private fun newStore(): FileStore? {
        return if (allFilesInFileStore) fileStore else null
    }

    /**
     * @return The store containing the given file, if the file is served by the new store:
     * - db mode: the file exists in the new store (otherwise it isn't yet migrated and is found in the JCR).
     * - jcr mode: the file exists in the new store, but not in the JCR (stored while db mode was active).
     */
    private fun storeOf(fileObject: FileObject): FileStore? {
        val store = fileStore ?: return null
        if (!store.exists(fileObject)) {
            return null
        }
        if (allFilesInFileStore) {
            return store
        }
        return if (jcrExists(fileObject)) null else store
    }

    private fun jcrExists(fileObject: FileObject): Boolean {
        return jcrFallback(fileObject, false) {
            repoStore!!.getFileInfo(fileObject.parentNodePath, fileObject.relPath, fileObject.fileId, fileObject.fileName) != null
        }
    }

    /**
     * Calls the JCR for files not found in the new store. The JCR throws an IllegalArgumentException,
     * if the node doesn't exist (e. g. no file of this area was ever stored in the JCR).
     */
    private fun <T> jcrFallback(fileObject: FileObject, default: T, block: () -> T): T {
        return try {
            block()
        } catch (ex: IllegalArgumentException) {
            log.debug { "File not found in JCR: $fileObject (${ex.message})" }
            default
        }
    }

    /**
     * @param parentNodePath Path, nodes are separated by '/', e. g. "world/germany". The nodes of this path must already exist.
     * For creating top level nodes (direct child of main node), set parentNode to null, empty string or "/".
     * @param relPath Sub node parent node to create if not exists. Null value results in nop.
     */
    open fun ensureNode(parentNodePath: String?, relPath: String? = null): Node? {
        if (newStore() != null) {
            return null // Not needed by the new store.
        }
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
        newStore()?.let { store ->
            val content = fileObject.content ?: ByteArray(0) // Assuming 0 byte file if no content is given.
            store.storeFile(fileObject, content.inputStream(), fileSizeChecker, user, password = password)
            return
        }
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
        newStore()?.let { store ->
            store.storeFile(fileObject, content, fileSizeChecker, user, data, password)
            return
        }
        repoStore!!.storeFile(fileObject, content, fileSizeChecker, user, data, password)
    }


    /**
     * Deletes the file in the new store and in the JCR (migrated files are kept in the JCR as copies, see
     * [RepoMigrationService]).
     */
    open fun deleteFile(fileObject: FileObject): Boolean {
        fileStore?.let { store ->
            if (allFilesInFileStore || store.exists(fileObject)) {
                val deleted = store.exists(fileObject) && store.deleteFile(fileObject)
                val jcrDeleted = jcrExists(fileObject) && jcrFallback(fileObject, false) { repoStore!!.deleteFile(fileObject) }
                return deleted || jcrDeleted
            }
        }
        return repoStore!!.deleteFile(fileObject)
    }

    /**
     * @param path The path, e. g. "org.projectforge.plugins.datatransfer".
     * @return The names of the child nodes of the given path (e. g. the ids of all data transfer areas with files).
     */
    open fun getChildNames(path: String): Set<String> {
        val result = mutableSetOf<String>()
        fileStore?.let { store ->
            result.addAll(store.getChildNames(path))
        }
        repoStore!!.getNodeInfoOrNull(getAbsolutePath(path), true)?.children?.forEach { child ->
            child.name?.let { result.add(it) }
        }
        return result
    }

    /**
     * Deletes all files of the given path and its descendants (e. g. of a deleted data transfer area).
     * @param path The path, e. g. "org.projectforge.plugins.datatransfer/42".
     * @return The deleted files (without content).
     */
    open fun deleteAllFilesBelow(path: String): List<FileObject> {
        val result = mutableListOf<FileObject>()
        fileStore?.let { store ->
            result.addAll(store.deleteAllFilesBelow(path))
        }
        repoStore!!.getNodeInfoOrNull(getAbsolutePath(path), true)?.let { nodeInfo ->
            val jcrFiles = mutableListOf<FileObject>()
            collectFiles(nodeInfo, jcrFiles)
            // Migrated files exist in both stores:
            result.addAll(jcrFiles.filter { jcrFile -> result.none { it.fileId == jcrFile.fileId } })
            repoStore!!.deleteNode(nodeInfo)
        }
        return result
    }

    private fun collectFiles(nodeInfo: NodeInfo, result: MutableList<FileObject>) {
        nodeInfo.children?.forEach { child ->
            if (nodeInfo.name == OakStorage.NODENAME_FILES) {
                if (child.hasProperty(OakStorage.PROPERTY_FILENAME)) {
                    result.add(FileObject(child))
                }
            } else {
                collectFiles(child, result)
            }
        }
    }

    open fun deleteNode(nodeInfo: NodeInfo): Boolean {
        return repoStore!!.deleteNode(nodeInfo)
    }

    /**
     * @return list of file infos without content.
     */
    @JvmOverloads
    open fun getFileInfos(parentNodePath: String?, relPath: String? = null): List<FileObject>? {
        val store = fileStore ?: return repoStore!!.getFileInfos(parentNodePath, relPath)
        val storeFiles = store.getFileInfos(parentNodePath, relPath) ?: emptyList()
        val jcrFiles = repoStore!!.getFileInfos(parentNodePath, relPath) ?: emptyList()
        // Files existing in both (migrated files): db mode serves the new store, jcr mode the JCR.
        val (primary, secondary) = if (allFilesInFileStore) storeFiles to jcrFiles else jcrFiles to storeFiles
        val files = primary + secondary.filter { file -> primary.none { it.fileId == file.fileId } }
        return files.ifEmpty { null }
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
        val store = fileStore ?: return repoStore!!.getFileInfo(parentNodePath, relPath, fileId, fileName)
        if (allFilesInFileStore) {
            return store.getFileInfo(parentNodePath, relPath, fileId, fileName)
                ?: repoStore!!.getFileInfo(parentNodePath, relPath, fileId, fileName)
        }
        return repoStore!!.getFileInfo(parentNodePath, relPath, fileId, fileName)
            ?: store.getFileInfo(parentNodePath, relPath, fileId, fileName)
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
        storeOf(fileObject)?.let { store ->
            return store.changeFileInfo(fileObject, user, newFileName, newDescription, newZipMode, updateLastUpdateInfo)
        }
        if (allFilesInFileStore) {
            return jcrFallback(fileObject, null) {
                repoStore!!.changeFileInfo(
                    fileObject,
                    user,
                    newFileName,
                    newDescription,
                    newZipMode,
                    updateLastUpdateInfo
                )
            }
        }
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
        storeOf(fileObject)?.let { store ->
            return store.checksum(fileObject)
        }
        if (allFilesInFileStore) {
            return jcrFallback(fileObject, null) { repoStore!!.checksum(fileObject) }
        }
        return repoStore!!.checksum(fileObject)
    }

    @JvmOverloads
    open fun getNodeInfo(absPath: String, recursive: Boolean = false): NodeInfo {
        return repoStore!!.getNodeInfo(absPath, recursive)
    }

    /**
     * @return The node info or null, if the node doesn't exist.
     */
    open fun getNodeInfoOrNull(absPath: String, recursive: Boolean = false): NodeInfo? {
        return repoStore!!.getNodeInfoOrNull(absPath, recursive)
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
        storeOf(fileObject)?.let { store ->
            return store.retrieveFile(fileObject, password)
        }
        return repoStore!!.retrieveFile(fileObject, password)
    }

    open fun retrieveFileInputStream(fileObject: FileObject, password: String? = null): InputStream? {
        storeOf(fileObject)?.let { store ->
            return store.retrieveFileInputStream(fileObject, password)
        }
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

    companion object {
        const val FILES_STORE_JCR = "jcr"
        const val FILES_STORE_DB = "db"
    }
}
