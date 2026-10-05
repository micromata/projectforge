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
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.sql.Types
import java.util.Date
import javax.sql.DataSource

/**
 * Plain JDBC access to `pf_files.t_attachment` (the schema is created by Flyway, in test cases by hbm2ddl through
 * [PfFileDO]). JDBC instead of JPA, because the content is streamed chunk by chunk (see [DbBlobStore]) and the
 * files are independent of the JPA transactions of the entities (as the JCR was before).
 */
internal class FileMetaDao(private val dataSource: DataSource) {
    fun <T> withConnection(block: (Connection) -> T): T {
        return dataSource.connection.use(block)
    }

    /**
     * Runs the block in a transaction of its own (commit on success, rollback on any exception).
     */
    fun <T> inTransaction(block: (Connection) -> T): T {
        return dataSource.connection.use { conn ->
            val autoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                val result = block(conn)
                conn.commit()
                result
            } catch (ex: Throwable) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = autoCommit
            }
        }
    }

    fun insert(conn: Connection, meta: FileMeta): Long {
        conn.prepareStatement(
            "INSERT INTO $TABLE (file_id, parent_path, rel_path, file_name, description, file_size, stored_size," +
                    " chunk_count, compression_type, storage_type, checksum, aes_encrypted, zip_mode, created, created_by," +
                    " last_update, last_update_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            var i = 0
            ps.setString(++i, meta.fileId)
            ps.setString(++i, meta.parentPath)
            ps.setString(++i, meta.relPath)
            ps.setString(++i, meta.fileName)
            ps.setString(++i, meta.description)
            setLong(ps, ++i, meta.size)
            setLong(ps, ++i, meta.storedSize)
            ps.setInt(++i, meta.chunkCount)
            ps.setString(++i, meta.compression.name)
            ps.setString(++i, meta.storage.name)
            ps.setString(++i, meta.checksum)
            ps.setBoolean(++i, meta.aesEncrypted)
            ps.setString(++i, meta.zipMode?.name)
            setDate(ps, ++i, meta.created)
            ps.setString(++i, meta.createdBy)
            setDate(ps, ++i, meta.lastUpdate)
            ps.setString(++i, meta.lastUpdateBy)
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                rs.next()
                return rs.getLong(1).also { meta.pk = it }
            }
        }
    }

    /**
     * Updates the sizes and the checksum (known after the content was written).
     */
    fun updateContentInfo(conn: Connection, meta: FileMeta) {
        conn.prepareStatement(
            "UPDATE $TABLE SET file_size=?, stored_size=?, chunk_count=?, checksum=? WHERE pk=?"
        ).use { ps ->
            setLong(ps, 1, meta.size)
            setLong(ps, 2, meta.storedSize)
            ps.setInt(3, meta.chunkCount)
            ps.setString(4, meta.checksum)
            ps.setLong(5, meta.pk!!)
            ps.executeUpdate()
        }
    }

    fun updateInfo(meta: FileMeta) {
        withConnection { conn ->
            conn.prepareStatement(
                "UPDATE $TABLE SET file_name=?, description=?, zip_mode=?, checksum=?, last_update=?, last_update_by=? WHERE pk=?"
            ).use { ps ->
                ps.setString(1, meta.fileName)
                ps.setString(2, meta.description)
                ps.setString(3, meta.zipMode?.name)
                ps.setString(4, meta.checksum)
                setDate(ps, 5, meta.lastUpdate)
                ps.setString(6, meta.lastUpdateBy)
                ps.setLong(7, meta.pk!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(conn: Connection, pk: Long) {
        conn.prepareStatement("DELETE FROM $TABLE WHERE pk=?").use { ps ->
            ps.setLong(1, pk)
            ps.executeUpdate()
        }
    }

    fun findByFileId(fileId: String): FileMeta? {
        return query("SELECT $COLUMNS FROM $TABLE WHERE file_id=?", fileId).firstOrNull()
    }

    fun find(parentPath: String, relPath: String, fileId: String?, fileName: String?): FileMeta? {
        if (fileId != null) {
            query("SELECT $COLUMNS FROM $TABLE WHERE parent_path=? AND rel_path=? AND file_id=?", parentPath, relPath, fileId)
                .firstOrNull()?.let { return it }
        }
        if (fileName != null) {
            return query(
                "SELECT $COLUMNS FROM $TABLE WHERE parent_path=? AND rel_path=? AND file_name=? ORDER BY pk",
                parentPath, relPath, fileName,
            ).firstOrNull()
        }
        return null
    }

    fun list(parentPath: String, relPath: String): List<FileMeta> {
        return query("SELECT $COLUMNS FROM $TABLE WHERE parent_path=? AND rel_path=? ORDER BY pk", parentPath, relPath)
    }

    /**
     * All files below the given path (parent path is equal to or starts with "<path>/").
     */
    fun listBelow(path: String): List<FileMeta> {
        return query(
            "SELECT $COLUMNS FROM $TABLE WHERE parent_path=? OR parent_path LIKE ? ESCAPE '!' ORDER BY pk",
            path, "${escapeLike(path)}/%",
        )
    }

    /**
     * Distinct parent paths below the given path (parent path starts with "<path>/").
     */
    fun listParentPathsBelow(path: String): List<String> {
        return withConnection { conn ->
            conn.prepareStatement(
                "SELECT DISTINCT parent_path FROM $TABLE WHERE parent_path LIKE ? ESCAPE '!'"
            ).use { ps ->
                ps.setString(1, "${escapeLike(path)}/%")
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<String>()
                    while (rs.next()) {
                        result.add(rs.getString(1))
                    }
                    result
                }
            }
        }
    }

    /**
     * Iterates over all files (ordered by pk), page by page, so the whole table is never held in memory.
     */
    fun forEach(storage: StorageType? = null, consumer: (FileMeta) -> Unit) {
        var lastPk = Long.MIN_VALUE
        while (true) {
            val page = if (storage == null) {
                query("SELECT $COLUMNS FROM $TABLE WHERE pk>? ORDER BY pk FETCH FIRST $PAGE_SIZE ROWS ONLY", lastPk)
            } else {
                query(
                    "SELECT $COLUMNS FROM $TABLE WHERE pk>? AND storage_type=? ORDER BY pk FETCH FIRST $PAGE_SIZE ROWS ONLY",
                    lastPk, storage.name,
                )
            }
            if (page.isEmpty()) {
                return
            }
            page.forEach(consumer)
            lastPk = page.last().pk!!
        }
    }

    class Statistics(val count: Long, val size: Long, val storedSize: Long)

    fun statistics(storage: StorageType): Statistics {
        return withConnection { conn ->
            conn.prepareStatement(
                "SELECT COUNT(*), SUM(file_size), SUM(stored_size) FROM $TABLE WHERE storage_type=?"
            ).use { ps ->
                ps.setString(1, storage.name)
                ps.executeQuery().use { rs ->
                    rs.next()
                    Statistics(rs.getLong(1), rs.getLong(2), rs.getLong(3))
                }
            }
        }
    }

    private fun query(sql: String, vararg params: Any): List<FileMeta> {
        return withConnection { conn ->
            conn.prepareStatement(sql).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is Long -> ps.setLong(index + 1, param)
                        else -> ps.setString(index + 1, param.toString())
                    }
                }
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<FileMeta>()
                    while (rs.next()) {
                        result.add(read(rs))
                    }
                    result
                }
            }
        }
    }

    private fun read(rs: ResultSet): FileMeta {
        return FileMeta(
            pk = rs.getLong("pk"),
            fileId = rs.getString("file_id"),
            parentPath = rs.getString("parent_path"),
            relPath = rs.getString("rel_path"),
            fileName = rs.getString("file_name"),
            description = rs.getString("description"),
            size = getLong(rs, "file_size"),
            storedSize = getLong(rs, "stored_size"),
            chunkCount = rs.getInt("chunk_count"),
            compression = CompressionType.valueOf(rs.getString("compression_type")),
            storage = StorageType.valueOf(rs.getString("storage_type")),
            checksum = rs.getString("checksum"),
            aesEncrypted = rs.getBoolean("aes_encrypted"),
            zipMode = rs.getString("zip_mode")?.let { ZipMode.valueOf(it) },
            created = rs.getTimestamp("created")?.let { Date(it.time) },
            createdBy = rs.getString("created_by"),
            lastUpdate = rs.getTimestamp("last_update")?.let { Date(it.time) },
            lastUpdateBy = rs.getString("last_update_by"),
        )
    }

    private fun getLong(rs: ResultSet, column: String): Long? {
        val value = rs.getLong(column)
        return if (rs.wasNull()) null else value
    }

    private fun setLong(ps: PreparedStatement, index: Int, value: Long?) {
        if (value == null) ps.setNull(index, Types.BIGINT) else ps.setLong(index, value)
    }

    private fun setDate(ps: PreparedStatement, index: Int, value: Date?) {
        if (value == null) ps.setNull(index, Types.TIMESTAMP) else ps.setTimestamp(index, Timestamp(value.time))
    }

    private fun escapeLike(str: String): String {
        return str.replace("!", "!!").replace("%", "!%").replace("_", "!_")
    }

    companion object {
        const val SCHEMA = "pf_files"
        const val TABLE = "$SCHEMA.t_attachment"
        private const val PAGE_SIZE = 500
        private const val COLUMNS =
            "pk, file_id, parent_path, rel_path, file_name, description, file_size, stored_size, chunk_count," +
                    " compression_type, storage_type, checksum, aes_encrypted, zip_mode, created, created_by, last_update," +
                    " last_update_by"
    }
}
