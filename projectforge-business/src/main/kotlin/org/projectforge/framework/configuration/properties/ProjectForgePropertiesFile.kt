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

package org.projectforge.framework.configuration.properties

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties

private val log = KotlinLogging.logger {}

/**
 * Reads and writes the admin's projectforge.properties. The file is never overwritten without a backup: before every
 * write a copy `projectforge.properties.<yyyy-MM-dd_HH-mm-ss>.bak` is placed next to it, and if the backup fails,
 * nothing is written. The content is read and written as ISO-8859-1, like Spring reads it, so untouched lines stay
 * byte by byte the same.
 */
class ProjectForgePropertiesFile(
    val file: File,
    private val metadata: PropertiesMetadata,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    class Snapshot(
        val exists: Boolean,
        val writable: Boolean,
        val content: String,
        /** SHA-256 of the content, for detecting changes made in the meantime. */
        val version: String,
        val lines: List<PropertiesFileLine>,
        /** The values as Spring sees them. */
        val values: Map<String, String>,
    )

    /** The file was modified since the admin loaded it. */
    class ConflictException : RuntimeException("projectforge.properties was modified in the meantime.")

    fun read(): Snapshot {
        val bytes = if (file.exists()) file.readBytes() else ByteArray(0)
        val content = String(bytes, Charsets.ISO_8859_1)
        val writable = if (file.exists()) file.canWrite() && file.parentFile.canWrite() else file.parentFile?.canWrite() == true
        return Snapshot(file.exists(), writable, content, sha256(bytes), PropertiesFileParser.parse(content), load(bytes))
    }

    /**
     * Writes the changes. Returns the backup file (null, if the file didn't exist before).
     * @throws ConflictException if [expectedVersion] isn't the version of the file anymore.
     */
    @Synchronized
    fun save(changes: List<PropertyChange>, expectedVersion: String): File? {
        val snapshot = read()
        if (snapshot.version != expectedVersion) {
            throw ConflictException()
        }
        val newContent = ProjectForgePropertiesWriter(metadata).write(snapshot.content, changes)
        val newBytes = newContent.toByteArray(Charsets.ISO_8859_1)
        verify(snapshot.values, changes, newBytes)
        val backup = if (snapshot.exists) backup() else null
        try {
            // Written in place (not moved), so the file keeps its permissions and a bind-mounted file (docker) works.
            Files.write(file.toPath(), newBytes)
        } catch (ex: IOException) {
            backup?.let { Files.copy(it.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            throw ex
        }
        if (load(file.readBytes()) != load(newBytes)) {
            backup?.let { Files.copy(it.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            throw IllegalStateException("Written ${file.name} couldn't be read back, the backup is restored.")
        }
        log.info {
            "${file.absolutePath} written (keys: ${changes.joinToString { it.key }}), backup: ${backup?.name ?: "-"}."
        }
        return backup
    }

    /** Self check: the new content must contain exactly the old values with the changes applied. */
    private fun verify(oldValues: Map<String, String>, changes: List<PropertyChange>, newBytes: ByteArray) {
        val expected = oldValues.toMutableMap()
        changes.forEach { change ->
            if (change.remove) {
                expected.remove(change.key)
            } else if (change.value != null) {
                expected[change.key] = change.value
            }
        }
        val actual = load(newBytes)
        if (actual != expected) {
            val diff = (expected.keys + actual.keys).filter { expected[it] != actual[it] }
            throw IllegalStateException("Internal error while building ${file.name}, nothing written (keys: ${diff.joinToString()}).")
        }
    }

    /** A backup written by [save]. [timestamp]: when it was written (taken from its name, else the file's). */
    class Backup(val file: File, val timestamp: LocalDateTime)

    /** The backups next to the file ([save]), newest first. */
    fun backups(): List<Backup> {
        val prefix = "${file.name}."
        val files = file.parentFile?.listFiles { f -> f.isFile && f.name.startsWith(prefix) && f.name.endsWith(".bak") }
            ?: return emptyList()
        return files.map { f ->
            val name = f.name.removePrefix(prefix).removeSuffix(".bak")
            val timestamp = runCatching {
                LocalDateTime.parse(name.take(BACKUP_TIMESTAMP_LENGTH), BACKUP_TIMESTAMP_FORMAT)
            }.getOrNull() ?: LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault())
            Backup(f, timestamp)
        }.sortedWith(
            // Backups of the same second: `…-2.bak`, `…-3.bak` were written after `….bak` (see [backup]).
            compareByDescending<Backup> { it.timestamp }
                .thenByDescending { backupCounter(it.file.name) }
                .thenByDescending { it.file.name }
        )
    }

    /** 1 for `….bak`, n for `…-n.bak`. */
    private fun backupCounter(name: String): Int =
        Regex("""-(\d+)\.bak$""").find(name.substringAfter("${file.name}.").drop(BACKUP_TIMESTAMP_LENGTH))
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1

    private fun backup(): File {
        val timestamp = now().format(BACKUP_TIMESTAMP_FORMAT)
        var backup = File(file.parentFile, "${file.name}.$timestamp.bak")
        var counter = 2
        while (backup.exists()) {
            backup = File(file.parentFile, "${file.name}.$timestamp-${counter++}.bak")
        }
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        if (!backup.readBytes().contentEquals(file.readBytes())) {
            throw IOException("Backup ${backup.absolutePath} differs from ${file.name}, nothing written.")
        }
        return backup
    }

    companion object {
        const val FILENAME = "projectforge.properties"

        private val BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

        private const val BACKUP_TIMESTAMP_LENGTH = "yyyy-MM-dd_HH-mm-ss".length

        internal fun load(bytes: ByteArray): Map<String, String> {
            val properties = Properties()
            properties.load(ByteArrayInputStream(bytes)) // ISO-8859-1, as Spring reads it.
            return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
        }

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
