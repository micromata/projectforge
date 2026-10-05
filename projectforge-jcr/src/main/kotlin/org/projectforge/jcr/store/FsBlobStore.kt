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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val log = KotlinLogging.logger {}

/**
 * Stores the content of files below a registered path (e. g. DataTransfer) in the file system, uncompressed:
 * `<root>/<parent path below the registered path>/<rel path>/<file id>`. The metadata is stored in the data base as for
 * all other files ([StorageType.FS]).
 *
 * Files are written to a temporary file first and moved afterwards, so a file is either complete or not there.
 */
internal class FsBlobStore {
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
        log.info { "Files below '$path' are stored in the file system: ${dir.absolutePath}" }
    }

    val registeredPaths: List<String>
        get() = synchronized(roots) { roots.map { it.path } }

    val rootDirs: List<File>
        get() = synchronized(roots) { roots.map { it.dir } }

    /**
     * @param parentPath Normalized parent path.
     */
    fun isResponsible(parentPath: String): Boolean {
        return findRoot(parentPath) != null
    }

    /**
     * Writes the payload to a temporary file in the target directory.
     * @return The temporary file, to be moved by [commit] or deleted by [discard].
     */
    fun writeTemp(meta: FileMeta, payload: InputStream): File {
        val file = getFile(meta)
        val dir = file.parentFile
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory) {
            throw IllegalStateException("Can't create directory '${dir.absolutePath}'.")
        }
        val tmpFile = File(dir, ".${meta.fileId}.tmp")
        try {
            tmpFile.outputStream().use { out ->
                payload.copyTo(out, BUFFER_SIZE)
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

    fun read(meta: FileMeta): InputStream {
        return getFile(meta).inputStream()
    }

    fun exists(meta: FileMeta): Boolean {
        return getFile(meta).exists()
    }

    fun length(meta: FileMeta): Long {
        return getFile(meta).length()
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

    private fun findRoot(parentPath: String): Root? {
        synchronized(roots) {
            return roots.firstOrNull { parentPath == it.path || parentPath.startsWith("${it.path}/") }
        }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
