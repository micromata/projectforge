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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.CryptStreamUtils
import org.projectforge.common.MaxFileSizeExceeded
import org.projectforge.common.ZipMode
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.FileSizeChecker
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Date
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

/**
 * Replacement of the Oak based storage: the metadata of all files is stored in `pf_files.t_attachment`, the content
 * either chunked in the data base ([StorageType.DB]) or in the file system ([StorageType.FS], for registered paths such
 * as DataTransfer).
 *
 * The semantics are the same as of the former `OakStorage` (location by parent node path and rel path, files found by
 * id or file name, optional AES encryption by password, checksum "SHA256: <hex>" of the stored payload).
 *
 * @param mainNodeName The former JCR main node. Paths given with this prefix (e. g. "/ProjectForge/...") are
 * normalized.
 */
class FileStore internal constructor(
    dataSource: DataSource,
    val mainNodeName: String = "ProjectForge",
) {
    private val dao = FileMetaDao(dataSource)

    private val dbBlobStore = DbBlobStore(dataSource)

    private val fsBlobStore = FsBlobStore()

    private val random = SecureRandom()

    /**
     * Files below the given path are stored in the file system (in the given directory) instead of the data base.
     * @param path The path (e. g. "org.projectforge.plugins.datatransfer").
     */
    fun registerFileSystemPath(path: String, dir: File) {
        fsBlobStore.register(normalizePath(path), dir)
    }

    /**
     * @return true, if files of the given path are stored in the file system.
     */
    fun isFileSystemPath(path: String?): Boolean {
        return fsBlobStore.isResponsible(normalizePath(path))
    }

    val fileSystemDirs: List<File>
        get() = fsBlobStore.rootDirs

    /**
     * Registered file system paths (see [registerFileSystemPath]) and their directories.
     */
    val fileSystemPathDirs: Map<String, File>
        get() = fsBlobStore.pathDirs

    /**
     * Content of file should be given as [FileObject.content].
     * @param password Optional password for encryption. The password will not be stored in any kind!
     */
    fun storeFile(
        fileObject: FileObject,
        content: InputStream,
        fileSizeChecker: FileSizeChecker,
        user: String? = null,
        data: Any? = null,
        password: String? = null,
    ) {
        if (fileObject.size != null) { // file size already known:
            fileSizeChecker.checkSize(fileObject, data)
        }
        val parentNodePath = fileObject.parentNodePath
        val relPath = fileObject.relPath
        if (parentNodePath == null || relPath == null) {
            throw IllegalArgumentException("Parent node path and relPath not given. Can't determine location of file to store: $fileObject")
        }
        val fileId = fileObject.fileId ?: createRandomId()
        fileObject.fileId = fileId
        log.info { "Storing file: $fileObject" }
        val now = Date()
        if (fileObject.created == null) {
            // created should only be preset for test cases. So normally, use current date.
            fileObject.created = now
        }
        fileObject.createdByUser = user
        fileObject.lastUpdate = fileObject.created
        fileObject.lastUpdateByUser = user
        val encrypt = !password.isNullOrBlank()
        if (encrypt) {
            fileObject.aesEncrypted = true
        }
        val meta = createMeta(fileObject, parentNodePath, relPath)
        val payload = if (encrypt) CryptStreamUtils.pipeToEncryptedInputStream(content, password) else content
        // Some checkers have no fixed limit (e. g. DataTransfer: the free capacity of the area), they throw an
        // exception here. Their limit is checked only after streaming, as the JCR did.
        val maxFileSize = runCatching { fileSizeChecker.maxFileSize }.getOrDefault(Long.MAX_VALUE)
        payload.use {
            try {
                persist(meta, it, maxFileSize) { measured ->
                    fileObject.size = measured.size
                    // Check size again for the case, the fileObject didn't contain file size before processing the stream.
                    fileSizeChecker.checkSize(fileObject, data)
                }
            } catch (ex: MeasuringInputStream.SizeLimitExceeded) {
                fileObject.size = ex.size
                fileSizeChecker.checkSize(fileObject, data)
                // Shouldn't occur, the checker should have thrown an exception:
                throw MaxFileSizeExceeded(maxFileSize, ex.size, fileObject.fileName)
            }
        }
        fileObject.checksum = meta.checksum
    }

    /**
     * Stores the given payload 1:1 including all metadata (id, timestamps, users, encryption and zip mode). Used for
     * migrating files from the JCR. Nothing is done, if a file with the same id already exists (so an aborted
     * migration may simply be restarted).
     * @param payload The payload as stored before (e. g. still encrypted).
     * @return The result including the calculated checksum, or null if the file already existed.
     */
    fun importFile(fileObject: FileObject, payload: InputStream): ImportResult? {
        val fileId = fileObject.fileId ?: throw IllegalArgumentException("File id required for importing: $fileObject")
        val parentNodePath = fileObject.parentNodePath
        val relPath = fileObject.relPath
        if (parentNodePath == null || relPath == null) {
            throw IllegalArgumentException("Parent node path and relPath not given. Can't determine location of file to import: $fileObject")
        }
        dao.findByFileId(fileId)?.let {
            log.info { "File already imported (skipping): $it" }
            return null
        }
        validateFileId(fileId)
        val meta = createMeta(fileObject, parentNodePath, relPath)
        meta.createdBy = fileObject.createdByUser
        meta.lastUpdate = fileObject.lastUpdate ?: fileObject.created
        meta.lastUpdateBy = fileObject.lastUpdateByUser
        payload.use {
            persist(meta, it, Long.MAX_VALUE) {}
        }
        return ImportResult(meta.size ?: 0, meta.checksum, meta.storage)
    }

    class ImportResult(val size: Long, val checksum: String?, val storage: StorageType)

    fun deleteFile(fileObject: FileObject): Boolean {
        val meta = find(fileObject)
        if (meta == null) {
            log.info { "Nothing to delete, file doesn't exist: $fileObject" }
            return false
        }
        copyTo(meta, fileObject)
        log.info { "Deleting file: $fileObject" }
        delete(meta)
        return true
    }

    /**
     * Deletes all files of the given path and all of its descendants (e. g. of a deleted data transfer area).
     * @return The deleted files.
     */
    fun deleteAllFilesBelow(path: String): List<FileObject> {
        val list = dao.listBelow(normalizePath(path))
        list.forEach {
            log.info { "Deleting file: $it" }
            delete(it)
        }
        return list.map { it.toFileObject() }
    }

    /**
     * @return The names of the direct children of the given path, containing files (e. g. the ids of the data transfer
     * areas having files).
     */
    fun getChildNames(path: String): Set<String> {
        val normalized = normalizePath(path)
        return dao.listParentPathsBelow(normalized).map {
            it.removePrefix("$normalized/").substringBefore('/')
        }.toSet()
    }

    /**
     * @return list of file infos without content or null, if no file exists.
     */
    fun getFileInfos(parentNodePath: String?, relPath: String? = null): List<FileObject>? {
        val location = Location.of(this, parentNodePath, relPath)
        val list = dao.list(location.parentPath, location.relPath)
        if (list.isEmpty()) {
            return null
        }
        return list.map { it.toFileObject(parentNodePath, relPath) }
    }

    /**
     * @return file info without content.
     */
    fun getFileInfo(
        parentNodePath: String?,
        relPath: String? = null,
        fileId: String? = null,
        fileName: String? = null
    ): FileObject? {
        val location = Location.of(this, parentNodePath, relPath)
        return dao.find(location.parentPath, location.relPath, fileId, fileName)
            ?.toFileObject(parentNodePath, relPath)
    }

    /**
     * Change fileName and/or description if given.
     * @param updateLastUpdateInfo If true (default),
     * time stamp of last update and user of this update will be updated. Otherwise time stamp and user info will be left untouched.
     * @return new file info without content or null, if the file doesn't exist.
     */
    fun changeFileInfo(
        fileObject: FileObject,
        user: String,
        newFileName: String? = null,
        newDescription: String? = null,
        newZipMode: ZipMode? = null,
        updateLastUpdateInfo: Boolean = true,
    ): FileObject? {
        val meta = find(fileObject)
        if (meta == null) {
            log.error { "Can't change file info, file doesn't exist: $fileObject" }
            return null
        }
        var modified = false
        if (!newFileName.isNullOrBlank()) {
            log.info { "Changing file name to '$newFileName' for: $fileObject" }
            meta.fileName = newFileName
            modified = true
        }
        if (newDescription != null) {
            log.info { "Changing file description to '$newDescription' for: $fileObject" }
            meta.description = newDescription
            modified = true
        }
        if (newZipMode != null) {
            log.info { "Changing zip encryption algorithm to '$newZipMode' for: $fileObject" }
            meta.zipMode = newZipMode
            modified = true
        }
        if (modified && updateLastUpdateInfo) {
            meta.lastUpdateBy = user
            meta.lastUpdate = Date()
        }
        dao.updateInfo(meta)
        return meta.toFileObject(fileObject.parentNodePath, fileObject.relPath)
    }

    /**
     * Calculates the checksum of the stored payload and updates it.
     * @return The checksum or null, if the file doesn't exist.
     */
    fun checksum(fileObject: FileObject): String? {
        val meta = find(fileObject)
        if (meta == null) {
            log.error { "Can't calculate checksum, file doesn't exist: $fileObject" }
            return null
        }
        meta.checksum = MeasuringInputStream(openPayload(meta), Long.MAX_VALUE).use { istream ->
            istream.transferTo(OutputStream.nullOutputStream())
            istream.checksum
        }
        dao.updateInfo(meta)
        fileObject.checksum = meta.checksum
        return meta.checksum
    }

    /**
     * @return true, if the file was found. The content is set in [FileObject.content] (null, if a wrong or no password
     * was given for an encrypted file).
     */
    fun retrieveFile(fileObject: FileObject, password: String? = null): Boolean {
        val meta = find(fileObject)
        if (meta == null) {
            log.warn { "File not found in repository: $fileObject" }
            return false
        }
        copyTo(meta, fileObject)
        fileObject.content = openStream(meta, fileObject, password)?.use { it.readBytes() }
        return true
    }

    /**
     * The caller is responsible for closing the stream.
     */
    fun retrieveFileInputStream(fileObject: FileObject, password: String? = null): InputStream? {
        val meta = find(fileObject)
        if (meta == null) {
            log.warn { "File not found in repository: $fileObject" }
            return null
        }
        return openStream(meta, fileObject, password)
    }

    /**
     * @return true, if a file with the given location and id or name exists.
     */
    fun exists(fileObject: FileObject): Boolean {
        return find(fileObject) != null
    }

    /**
     * Deletes all files (data base and file system). Should only be used by test cases, which need an empty
     * repository (as a fresh JCR repository before).
     */
    fun internalClearForJunitTestCases() {
        dao.forEach { delete(it) }
    }

    class SanityResult(
        val fileObject: FileObject,
        val storage: StorageType,
        val error: String? = null,
    )

    /**
     * Checks for all files, that the content exists and matches size and checksum.
     * @param consumer Called for every file.
     */
    fun checkSanity(consumer: (SanityResult) -> Unit) {
        dao.forEach { meta ->
            val error = try {
                if (meta.storage == StorageType.FS && !fsBlobStore.exists(meta)) {
                    "File '${fsBlobStore.getFile(meta).absolutePath}' doesn't exist."
                } else {
                    MeasuringInputStream(openPayload(meta), Long.MAX_VALUE).use { istream ->
                        istream.transferTo(OutputStream.nullOutputStream())
                        when {
                            istream.count != meta.size -> "Size ${istream.count} differs from stored size ${meta.size}."
                            meta.checksum.isNullOrBlank() -> null // No checksum available (shouldn't occur).
                            istream.checksum != meta.checksum -> "Checksum '${istream.checksum}' differs from stored checksum '${meta.checksum}'."
                            else -> null
                        }
                    }
                }
            } catch (ex: Exception) {
                "Can't read content: ${ex::class.java.simpleName}: ${ex.message}"
            }
            consumer(SanityResult(meta.toFileObject(), meta.storage, error))
        }
    }

    class Statistics(val storage: StorageType, val count: Long, val size: Long, val storedSize: Long)

    fun getStatistics(): List<Statistics> {
        return StorageType.entries.map { storage ->
            val stats = dao.statistics(storage)
            Statistics(storage, stats.count, stats.size, stats.storedSize)
        }
    }

    /**
     * Normalizes the path: without leading main node and without leading or trailing '/'.
     */
    fun normalizePath(path: String?): String {
        var result = path?.trim('/') ?: return ""
        if (result == mainNodeName) {
            return ""
        }
        result = result.removePrefix("$mainNodeName/")
        return result.trim('/')
    }

    /**
     * The location of a file is given by parent node path and rel path, but only their concatenation matters (as in
     * the JCR). So the location is normalized: the last segment is the rel path (e. g. `attachments`), the segments
     * before are the parent path (e. g. `org.projectforge.fibu.RechnungDO/42`).
     */
    internal class Location(val parentPath: String, val relPath: String) {
        companion object {
            fun of(fileStore: FileStore, parentNodePath: String?, relPath: String?): Location {
                val parent = fileStore.normalizePath(parentNodePath)
                val rel = relPath?.trim('/') ?: ""
                val full = when {
                    parent.isEmpty() -> rel
                    rel.isEmpty() -> parent
                    else -> "$parent/$rel"
                }
                return if (full.contains('/')) {
                    Location(full.substringBeforeLast('/'), full.substringAfterLast('/'))
                } else {
                    Location("", full)
                }
            }
        }
    }

    private fun find(fileObject: FileObject): FileMeta? {
        val location = Location.of(this, fileObject.parentNodePath, fileObject.relPath)
        return dao.find(location.parentPath, location.relPath, fileObject.fileId, fileObject.fileName)
    }

    private fun createMeta(fileObject: FileObject, parentNodePath: String, relPath: String): FileMeta {
        val location = Location.of(this, parentNodePath, relPath)
        val storage = if (fsBlobStore.isResponsible(location.parentPath)) StorageType.FS else StorageType.DB
        return FileMeta(
            fileId = fileObject.fileId!!,
            parentPath = location.parentPath,
            relPath = location.relPath,
            fileName = fileObject.fileName,
            description = fileObject.description,
            storage = storage,
            // Content in the file system isn't compressed: fast access and no CPU costs for large files.
            compression = if (storage == StorageType.FS) {
                CompressionType.NONE
            } else {
                CompressionType.choose(fileObject.fileName, fileObject.aesEncrypted, fileObject.zipMode)
            },
            aesEncrypted = fileObject.aesEncrypted == true,
            zipMode = fileObject.zipMode,
            created = fileObject.created,
            createdBy = fileObject.createdByUser,
            lastUpdate = fileObject.lastUpdate,
            lastUpdateBy = fileObject.lastUpdateByUser,
        )
    }

    /**
     * Writes the payload and the metadata.
     * @param afterMeasured Called after the payload was written, but before committing. May throw an exception to
     * abort (e. g. if the file is too large).
     */
    private fun persist(meta: FileMeta, payload: InputStream, maxSize: Long, afterMeasured: (FileMeta) -> Unit) {
        val measuring = MeasuringInputStream(payload, maxSize)
        if (meta.storage == StorageType.DB) {
            try {
                dao.inTransaction { conn ->
                    dao.insert(conn, meta)
                    val result = dbBlobStore.write(conn, meta.pk!!, meta.compression, measuring)
                    meta.size = measuring.count
                    meta.storedSize = result.storedSize
                    meta.chunkCount = result.chunkCount
                    meta.checksum = measuring.checksum
                    afterMeasured(meta)
                    dao.updateContentInfo(conn, meta)
                }
            } catch (ex: Throwable) {
                meta.pk = null // Rolled back.
                throw ex
            }
        } else {
            val tmpFile = fsBlobStore.writeTemp(meta, measuring)
            try {
                meta.size = measuring.count
                meta.storedSize = measuring.count
                meta.checksum = measuring.checksum
                afterMeasured(meta)
                fsBlobStore.commit(meta, tmpFile)
            } catch (ex: Throwable) {
                fsBlobStore.discard(tmpFile)
                throw ex
            }
            try {
                dao.inTransaction { conn -> dao.insert(conn, meta) }
            } catch (ex: Throwable) {
                meta.pk = null
                fsBlobStore.delete(meta)
                throw ex
            }
        }
    }

    private fun delete(meta: FileMeta) {
        dao.inTransaction { conn ->
            if (meta.storage == StorageType.DB) {
                dbBlobStore.delete(conn, meta.pk!!)
            }
            dao.delete(conn, meta.pk!!)
        }
        if (meta.storage == StorageType.FS) {
            fsBlobStore.delete(meta)
        }
    }

    /**
     * @return The stored payload (decompressed, but still encrypted, if encrypted).
     */
    internal fun openPayload(meta: FileMeta): InputStream {
        return if (meta.storage == StorageType.DB) dbBlobStore.read(meta) else fsBlobStore.read(meta)
    }

    /**
     * @param password Must be given for encrypted file to decrypt.
     */
    private fun openStream(meta: FileMeta, fileObject: FileObject, password: String?): InputStream? {
        log.info { "Reading file from repository: $fileObject..." }
        if (meta.aesEncrypted && password.isNullOrBlank()) {
            log.error { "File is encrypted, but no password given to decrypt in repository: $fileObject" }
            return null
        }
        val payload = openPayload(meta)
        if (password.isNullOrBlank()) {
            return payload
        }
        return try {
            payload.use { CryptStreamUtils.pipeToDecryptedInputStream(it, password) }
        } catch (ex: Exception) {
            if (CryptStreamUtils.wasWrongPassword(ex)) {
                log.error { "Can't decrypt and retrieve file (wrong password) in repository: $fileObject" }
                null
            } else {
                throw ex
            }
        }
    }

    /**
     * Copies all fields from meta to fileObject, excluding content and path settings.
     */
    private fun copyTo(meta: FileMeta, fileObject: FileObject) {
        fileObject.copyFrom(meta.toFileObject() as FileInfo)
        fileObject.fileId = meta.fileId
    }

    private fun createRandomId(): String {
        while (true) {
            val sb = StringBuilder()
            repeat(RANDOM_ID_LENGTH) {
                sb.append(ALPHA_CHARSET[random.nextInt(ALPHA_CHARSET.size)])
            }
            val id = sb.toString()
            if (dao.findByFileId(id) == null) {
                return id
            }
        }
    }

    private fun validateFileId(fileId: String) {
        if (!FILE_ID_REGEX.matches(fileId)) {
            throw IllegalArgumentException("Invalid file id '$fileId'.")
        }
    }

    companion object {
        private const val RANDOM_ID_LENGTH = 20
        private val ALPHA_CHARSET: Array<Char> = ('a'..'z').toList().toTypedArray()
        private val FILE_ID_REGEX = Regex("[A-Za-z0-9_-]{1,40}")
    }
}
