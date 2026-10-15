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

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.sql.Connection
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.sql.DataSource

/**
 * Stores the content in `pf_files.t_attachment_chunk`, split into chunks of [CHUNK_SIZE] bytes. Chunks are streamed in
 * both directions, so files of 100 MB and more never have to be held in memory (pgjdbc would read a single bytea
 * value completely).
 */
internal class DbBlobStore(private val dataSource: DataSource) {
    class WriteResult(val storedSize: Long, val chunkCount: Int)

    /**
     * Writes the payload as chunks of the given (already inserted) file. Must be called inside the transaction of the
     * insert, so a failure (e. g. the maximum file size is exceeded) leaves no chunks behind.
     */
    fun write(conn: Connection, attachmentPk: Long, compression: CompressionType, payload: InputStream): WriteResult {
        val sink = ChunkOutputStream(conn, attachmentPk)
        val out: OutputStream = if (compression == CompressionType.GZIP) GZIPOutputStream(sink, BUFFER_SIZE) else sink
        out.use {
            payload.copyTo(it, BUFFER_SIZE)
        }
        return WriteResult(sink.storedSize, sink.chunkCount)
    }

    /**
     * @return The payload (decompressed, if compressed).
     */
    fun read(meta: FileMeta): InputStream {
        val stored = ChunkInputStream(dataSource, meta.pk!!, meta.chunkCount)
        return if (meta.compression == CompressionType.GZIP) GZIPInputStream(stored, BUFFER_SIZE) else stored
    }

    /**
     * Deletes all chunks of the given file (the foreign key cascades too, but HSQLDB tables created by older tests may
     * lack it, so delete explicitly).
     */
    fun delete(conn: Connection, attachmentPk: Long) {
        conn.prepareStatement("DELETE FROM $CHUNK_TABLE WHERE attachment_fk=?").use { ps ->
            ps.setLong(1, attachmentPk)
            ps.executeUpdate()
        }
    }

    /**
     * Collects the written bytes and inserts them chunk by chunk.
     */
    private class ChunkOutputStream(private val conn: Connection, private val attachmentPk: Long) : OutputStream() {
        private val buffer = ByteArray(CHUNK_SIZE)
        private var pos = 0
        var chunkCount = 0
            private set
        var storedSize = 0L
            private set
        private var closed = false

        override fun write(b: Int) {
            buffer[pos++] = b.toByte()
            if (pos == CHUNK_SIZE) {
                flushChunk()
            }
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var offset = off
            var remaining = len
            while (remaining > 0) {
                val n = minOf(remaining, CHUNK_SIZE - pos)
                System.arraycopy(b, offset, buffer, pos, n)
                pos += n
                offset += n
                remaining -= n
                if (pos == CHUNK_SIZE) {
                    flushChunk()
                }
            }
        }

        override fun close() {
            if (closed) {
                return
            }
            closed = true
            if (pos > 0) {
                flushChunk()
            }
        }

        private fun flushChunk() {
            conn.prepareStatement("INSERT INTO $CHUNK_TABLE (attachment_fk, seq, chunk_data) VALUES (?, ?, ?)").use { ps ->
                ps.setLong(1, attachmentPk)
                ps.setInt(2, chunkCount)
                ps.setBytes(3, if (pos == CHUNK_SIZE) buffer else buffer.copyOf(pos))
                ps.executeUpdate()
            }
            storedSize += pos
            ++chunkCount
            pos = 0
        }
    }

    /**
     * Reads the chunks one by one, each by a short query of its own. So no database connection is held while the
     * client downloads slowly.
     */
    private class ChunkInputStream(
        private val dataSource: DataSource,
        private val attachmentPk: Long,
        private val chunkCount: Int,
    ) : InputStream() {
        private var chunk: ByteArray? = null
        private var pos = 0
        private var nextSeq = 0

        override fun read(): Int {
            val current = currentChunk() ?: return -1
            return current[pos++].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) {
                return 0
            }
            val current = currentChunk() ?: return -1
            val n = minOf(len, current.size - pos)
            System.arraycopy(current, pos, b, off, n)
            pos += n
            return n
        }

        private fun currentChunk(): ByteArray? {
            var current = chunk
            while (current == null || pos >= current.size) {
                if (nextSeq >= chunkCount) {
                    return null
                }
                current = loadChunk(nextSeq++)
                chunk = current
                pos = 0
            }
            return current
        }

        private fun loadChunk(seq: Int): ByteArray {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT chunk_data FROM $CHUNK_TABLE WHERE attachment_fk=? AND seq=?").use { ps ->
                    ps.setLong(1, attachmentPk)
                    ps.setInt(2, seq)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) {
                            throw IOException("Chunk #$seq of file with pk=$attachmentPk not found (file deleted meanwhile?).")
                        }
                        return rs.getBytes(1)
                    }
                }
            }
        }
    }

    companion object {
        const val CHUNK_TABLE = "${FileMetaDao.SCHEMA}.t_attachment_chunk"
        const val CHUNK_SIZE = 1024 * 1024
        private const val BUFFER_SIZE = 64 * 1024
    }
}
