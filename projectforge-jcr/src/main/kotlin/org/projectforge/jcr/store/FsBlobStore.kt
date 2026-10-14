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
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val log = KotlinLogging.logger {}

/**
 * Stores the content of files below a registered path (e. g. DataTransfer) in the file system:
 * `<root>/<parent path below the registered path>/<rel path>/<file id>`. The metadata is stored in the data base as for
 * all other files ([StorageType.FS]).
 *
 * The content is compressed as in the data base ([FileMeta.compression], but with [Deflater.BEST_SPEED], files may have
 * 10 GB) and afterwards encrypted, if a [cipher] is given ([FileMeta.encryption]). Files stored before keep their
 * format, both are given by the metadata of each file.
 *
 * Files are written to a temporary file first and moved afterwards, so a file is either complete or not there.
 * @param cipher Encrypts new files, if given (`projectforge.files.fileSystemEncryptionKey`).
 */
internal class FsBlobStore(private val cipher: FileSystemCipher? = null) {
    private class Root(val path: String, val dir: File)

    private val roots = mutableListOf<Root>()

    /**
     * @param path Normalized path (e. g. `org.projectforge.plugins.datatransfer`).
     */
    fun register(path: String, dir: File) {
        synchronized(roots) {
            roots.removeIf { it.path == path }
            roots.add(Root(path, dir))
        }
        log.info { "Files below '$path' are stored in the file system (projectforge.files.store=db only, ${if (cipher != null) "encrypted" else "not encrypted"}): ${dir.absolutePath}" }
    }

    val registeredPaths: List<String>
        get() = synchronized(roots) { roots.map { it.path } }

    val rootDirs: List<File>
        get() = synchronized(roots) { roots.map { it.dir } }

    /**
     * Registered paths and their directories.
     */
    val pathDirs: Map<String, File>
        get() = synchronized(roots) { roots.associate { it.path to it.dir } }

    /**
     * @param parentPath Normalized parent path.
     */
    fun isResponsible(parentPath: String): Boolean {
        return findRoot(parentPath) != null
    }

    /**
     * True, if new files are encrypted.
     */
    val encrypting: Boolean
        get() = cipher != null

    /**
     * Writes the payload to a temporary file in the target directory, compressed and encrypted as given by the meta.
     * @return The temporary file, to be moved by [commit] or deleted by [discard]. Its length is the stored size.
     */
    fun writeTemp(meta: FileMeta, payload: InputStream): File {
        val file = getFile(meta)
        val dir = file.parentFile
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory) {
            throw IllegalStateException("Can't create directory '${dir.absolutePath}'.")
        }
        val tmpFile = File(dir, ".${meta.fileId}.tmp")
        try {
            var out: OutputStream = tmpFile.outputStream()
            try {
                if (meta.encryption == EncryptionType.AES_CTR) {
                    out = requireCipher(meta).encrypt(out)
                }
                if (meta.compression == CompressionType.GZIP) {
                    out = object : GZIPOutputStream(out, BUFFER_SIZE) {
                        init {
                            def.setLevel(Deflater.BEST_SPEED)
                        }
                    }
                }
            } catch (ex: Throwable) {
                out.close()
                throw ex
            }
            out.use {
                payload.copyTo(it, BUFFER_SIZE)
            }
        } catch (ex: Throwable) {
            tmpFile.delete()
            throw ex
        }
        return tmpFile
    }

    fun commit(meta: FileMeta, tmpFile: File) {
        val target = getFile(meta).toPath()
        try {
            Files.move(tmpFile.toPath(), target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmpFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun discard(tmpFile: File) {
        if (tmpFile.exists() && !tmpFile.delete()) {
            log.warn { "Can't delete temporary file '${tmpFile.absolutePath}'." }
        }
    }

    /**
     * @return The payload (decrypted and decompressed, if so stored).
     */
    fun read(meta: FileMeta): InputStream {
        var istream: InputStream = getFile(meta).inputStream()
        try {
            if (meta.encryption == EncryptionType.AES_CTR) {
                istream = requireCipher(meta).decrypt(istream)
            }
            if (meta.compression == CompressionType.GZIP) {
                istream = GZIPInputStream(istream, BUFFER_SIZE)
            }
        } catch (ex: Throwable) {
            istream.close()
            throw ex
        }
        return istream
    }

    fun exists(meta: FileMeta): Boolean {
        return getFile(meta).exists()
    }

    fun delete(meta: FileMeta) {
        val file = getFile(meta)
        if (file.exists() && !file.delete()) {
            log.error { "Can't delete file '${file.absolutePath}': $meta" }
            return
        }
        // Remove empty directories up to the root (e. g. of deleted areas):
        val root = findRoot(meta.parentPath)?.dir?.canonicalFile ?: return
        var dir: File? = file.parentFile?.canonicalFile
        while (dir != null && dir != root && dir.startsWith(root) && dir.list()?.isEmpty() == true) {
            dir.delete()
            dir = dir.parentFile
        }
    }

    fun getFile(meta: FileMeta): File {
        val root = findRoot(meta.parentPath)
            ?: throw IllegalArgumentException("No file system root registered for '${meta.parentPath}'.")
        val segments = mutableListOf<String>()
        meta.parentPath.removePrefix(root.path).split('/').filter { it.isNotEmpty() }.forEach { segments.add(it) }
        meta.relPath.split('/').filter { it.isNotEmpty() }.forEach { segments.add(it) }
        segments.add(meta.fileId)
        var file = root.dir
        segments.forEach { segment ->
            if (segment == "." || segment == ".." || segment.contains('\\') || segment.contains('\u0000')) {
                throw IllegalArgumentException("Invalid path segment '$segment': $meta")
            }
            file = File(file, segment)
        }
        return file
    }

    private fun requireCipher(meta: FileMeta): FileSystemCipher {
        return cipher
            ?: throw IllegalStateException("File is encrypted, but projectforge.files.fileSystemEncryptionKey isn't configured: $meta")
    }

    private fun findRoot(parentPath: String): Root? {
        synchronized(roots) {
            return roots.firstOrNull { parentPath == it.path || parentPath.startsWith("${it.path}/") }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
