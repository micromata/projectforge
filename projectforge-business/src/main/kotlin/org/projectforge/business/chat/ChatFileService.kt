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

package org.projectforge.business.chat

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.common.FormatterUtils
import org.projectforge.common.i18n.UserException
import org.projectforge.datatransfer.DataTransferAreaHost
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.files.FileObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

/**
 * The files of chat messages (PF-CHAT ATT), of any type, stored in the data transfer ([DataTransferBridge]):
 * - in a synthetic data transfer area per conversation ([ChatConversationDO.fileAreaId]), whose access is the one of
 *   the conversation (this service is its [DataTransferAreaHost]): its readers see the area in the data transfer and
 *   download its files, except those of hidden messages (moderators only). Its files expire as configured (rooms:
 *   [ChatRoomChatHost], see [saveArea]), the clean-up job of the data transfer deletes them.
 * - in an existing area given by the host ([ChatHost.usesOwnFileArea]): for direct messages the personal box of the
 *   recipient, where both see the file (its owner and its uploader); for a data transfer area the area itself.
 *
 * The size of a file is limited by the area (upload limit, free capacity). Images (PNG, JPEG, GIF, WebP, checked by
 * their content) are shown as thumbnails; the metadata (EXIF, GPS) of JPEG and PNG images is removed by encoding
 * them again. Without the data transfer plugin (or on the gateway) no files can be exchanged.
 */
@Service
open class ChatFileService : DataTransferAreaHost {
    enum class ImageType(val mimeType: String, val extension: String) {
        PNG("image/png", "png"), JPEG("image/jpeg", "jpg"), GIF("image/gif", "gif"), WEBP("image/webp", "webp");

        companion object {
            fun ofExtension(extension: String?): ImageType? {
                val ext = extension?.lowercase() ?: return null
                return entries.find { it.extension == ext } ?: if (ext == "jpeg") JPEG else null
            }
        }
    }

    /** A file of a message to be sent. */
    class Upload(val fileName: String?, val content: ByteArray)

    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var dataTransferBridge: DataTransferBridge

    @Autowired
    private lateinit var hostRegistry: ChatHostRegistry

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** The maximum number of files of a message. */
    @Value("\${projectforge.chat.file.maxPerMessage:10}")
    open var maxPerMessage: Int = 10

    /** The synthetic areas of the conversations: area id -> conversation id. */
    private val areas: ConcurrentHashMap<Long, Long> by lazy {
        ConcurrentHashMap<Long, Long>().also { map ->
            persistenceService.executeQuery(
                "select c.fileAreaId, c.id from ChatConversationDO c where c.fileAreaId is not null",
                Array<Any?>::class.java,
            ).forEach { row -> map[(row[0] as Number).toLong()] = (row[1] as Number).toLong() }
        }
    }

    @PostConstruct
    private fun postConstruct() {
        dataTransferBridge.registerAreaHost(this)
    }

    /** Can files be exchanged in the chat (data transfer available)? */
    open val filesAvailable: Boolean
        get() = dataTransferBridge.fileStorageAvailable

    /** Does the host of the conversation take files at all (independent of the data transfer)? */
    open fun filesAllowed(context: ChatAccess.Context): Boolean {
        return context.host.filesAllowed(context.hostId, context.channel)
    }

    /** Can files be exchanged in the conversation (allowed by the host, data transfer available)? */
    open fun filesEnabled(context: ChatAccess.Context): Boolean {
        return filesAvailable && filesAllowed(context)
    }

    /**
     * The largest file the conversation takes now (bytes), null if no files can be exchanged or its area doesn't exist
     * yet (see [ensureArea]).
     */
    open fun maxFileSize(context: ChatAccess.Context): Long? {
        val areaId = targetAreaId(context, create = false) ?: return null
        return dataTransferBridge.getMaxUploadSize(areaId)
    }

    /** Creates the area of the conversation, if missing (not for a host with its own area). No access check. */
    open fun ensureArea(context: ChatAccess.Context) {
        targetAreaId(context, create = true)
    }

    /**
     * Stores the files of a message of the user: in the area of the conversation (created if missing), or in the
     * area of the host ([ChatHost.fileAreaId], e.g. the personal box of the recipient of a direct message). The caller checked the write access. On an error, the files
     * stored before are deleted again.
     */
    open fun store(context: ChatAccess.Context, uploads: List<Upload>): List<ChatAttachment> {
        if (uploads.isEmpty()) {
            return emptyList()
        }
        if (uploads.size > maxPerMessage) {
            throw UserException(ERROR_TOO_MANY, maxPerMessage)
        }
        // Checked here, not only by hiding the button: a posted file must not land in a conversation without files.
        if (!filesAllowed(context)) {
            throw UserException(ERROR_NOT_ALLOWED)
        }
        val areaId = targetAreaId(context, create = true) ?: throw UserException(ERROR_UNAVAILABLE)
        val maxSize = dataTransferBridge.getMaxUploadSize(areaId) ?: 0
        val prepared = uploads.map { upload ->
            if (upload.content.size > maxSize) {
                throw UserException(ERROR_TOO_LARGE, safeName(upload.fileName), FormatterUtils.formatBytes(maxSize))
            }
            prepare(upload)
        }
        val stored = mutableListOf<ChatAttachment>()
        try {
            prepared.forEach { (name, content, image) ->
                val file = dataTransferBridge.addFile(areaId, name, content)
                stored += ChatAttachment(file.areaId, file.fileId, file.name, file.size, image)
            }
        } catch (ex: Exception) {
            delete(stored)
            throw ex
        }
        return stored
    }

    /** Deletes the files (of a deleted message, or of one not sent after all). No access check. */
    open fun delete(attachments: List<ChatAttachment>) {
        attachments.forEach { attachment ->
            try {
                dataTransferBridge.deleteFile(attachment.areaId, attachment.fileId)
            } catch (ex: Exception) {
                log.error(ex) { "Chat file '${attachment.name}' (area #${attachment.areaId}) not deleted: ${ex.message}" }
            }
        }
    }

    /**
     * Creates or updates the area of the conversation: its name (shown in the data transfer) and the days its files are
     * kept (null: unchanged, 30 days for a new one). Nothing for a host with its own area ([ChatHost.usesOwnFileArea]).
     * No access check: the caller decides.
     */
    open fun saveArea(conversationId: Long, title: String, expiryDays: Int?) {
        val conversation = chatAccess.findConversation(conversationId) ?: return
        if (hostRegistry.get(conversation.hostType)?.usesOwnFileArea != false) {
            return
        }
        synchronized(this) {
            val areaId = chatAccess.findConversation(conversationId)?.fileAreaId
            val saved = dataTransferBridge.saveSyntheticArea(areaId, areaName(title), expiryDays) ?: return
            if (areaId == null) {
                setAreaId(conversationId, saved)
            }
        }
    }

    /**
     * The days the files of the conversation are kept: fixed by its host, otherwise those of its area, null if it has
     * none (yet).
     */
    open fun fileExpiryDays(context: ChatAccess.Context): Int? {
        return context.host.fileExpiryDays(context.hostId, context.channel) ?: getExpiryDays(context.conversationId)
    }

    /** The days the files of the conversation are kept, null if it has no area (yet). */
    open fun getExpiryDays(conversationId: Long): Int? {
        val areaId = chatAccess.findConversation(conversationId)?.fileAreaId ?: return null
        return dataTransferBridge.getExpiryDays(areaId)
    }

    open fun getExpiryDaysChoices(): List<Int> = dataTransferBridge.getExpiryDaysChoices()

    /** Deletes the area of the conversation with its files (with the conversation). No access check. */
    open fun deleteArea(conversation: ChatConversationDO) {
        val areaId = conversation.fileAreaId ?: return
        try {
            dataTransferBridge.deleteArea(areaId)
        } catch (ex: Exception) {
            log.error(ex) { "Data transfer area #$areaId of chat conversation #${conversation.id} not deleted: ${ex.message}" }
        }
        areas.remove(areaId)
    }

    /**
     * The file of a message of the conversation and its content, if the user may see the message: not deleted, hidden
     * ones only for moderators. Null if not found or expired.
     */
    open fun download(context: ChatAccess.Context, fileId: String): Pair<ChatAttachment, Pair<FileObject, InputStream>>? {
        val (message, attachment) = findAttachment(context.conversationId, fileId) ?: return null
        if (!isVisible(context, message)) {
            return null
        }
        val file = dataTransferBridge.getFile(attachment.areaId, attachment.fileId) ?: return null
        return Pair(attachment, file)
    }

    /** The ids of the files still there (not expired or deleted in the data transfer) of the given areas. */
    open fun existingFileIds(areaIds: Collection<Long>): Set<String> {
        return areaIds.toSet().flatMapTo(mutableSetOf()) { dataTransferBridge.getFileIds(it) }
    }

    override fun owns(areaId: Long): Boolean = areas.containsKey(areaId)

    override fun hasReadAccess(areaId: Long, user: PFUserDO): Boolean = context(areaId, user) != null

    /** The files of visible messages; files of no message (any more) for moderators only. */
    override fun isFileAccessible(areaId: Long, fileId: String, user: PFUserDO): Boolean {
        val context = context(areaId, user) ?: return false
        val message = findAttachment(context.conversationId, fileId)?.first ?: return context.isModerator
        return isVisible(context, message)
    }

    /** Moderators and the author of the message of the file. */
    override fun hasDeleteAccess(areaId: Long, fileId: String, user: PFUserDO): Boolean {
        val context = context(areaId, user) ?: return false
        if (context.isModerator) {
            return true
        }
        return findAttachment(context.conversationId, fileId)?.first?.authorId == user.id
    }

    private fun context(areaId: Long, user: PFUserDO): ChatAccess.Context? {
        val conversationId = areas[areaId] ?: return null
        return chatAccess.getContext(conversationId, user)
    }

    private fun isVisible(context: ChatAccess.Context, message: ChatMessageDO): Boolean {
        return !message.deleted && (!message.isHidden || context.isModerator)
    }

    /** The message of the conversation with the file, null if none. */
    private fun findAttachment(conversationId: Long, fileId: String): Pair<ChatMessageDO, ChatAttachment>? {
        if (!FILE_ID_PATTERN.matches(fileId)) {
            return null
        }
        return persistenceService.executeQuery(
            "from ChatMessageDO m where m.conversationId = :conversationId and m.attachments like :fileId",
            ChatMessageDO::class.java,
            Pair("conversationId", conversationId),
            Pair("fileId", "%\"$fileId\"%"),
        ).firstNotNullOfOrNull { message ->
            message.readAttachments().find { it.fileId == fileId }?.let { Pair(message, it) }
        }
    }

    /**
     * The area the files of the user go to: the one of the host if it has its own ([ChatHost.fileAreaId], e.g. the
     * personal box of the recipient of a direct message), the area of the conversation otherwise (created if missing
     * and [create]). Null if no files can be exchanged.
     */
    private fun targetAreaId(context: ChatAccess.Context, create: Boolean): Long? {
        if (!dataTransferBridge.fileStorageAvailable) {
            return null
        }
        if (context.host.usesOwnFileArea) {
            return context.host.fileAreaId(context.hostId, context.channel, context.user)
        }
        chatAccess.findConversation(context.conversationId)?.fileAreaId?.let { return it }
        if (!create) {
            return null
        }
        val title = context.host.title(context.hostId, context.channel) ?: context.title
        saveArea(context.conversationId, title, context.host.fileExpiryDays(context.hostId, context.channel))
        return chatAccess.findConversation(context.conversationId)?.fileAreaId
    }

    private fun setAreaId(conversationId: Long, areaId: Long) {
        persistenceService.runInTransaction { pc ->
            pc.executeUpdate(
                "update ChatConversationDO c set c.fileAreaId = :areaId where c.id = :id",
                Pair("areaId", areaId),
                Pair("id", conversationId),
            )
        }
        areas[areaId] = conversationId
    }

    /** Name, content and image flag of the file as stored: images checked and without metadata. */
    private fun prepare(upload: Upload): Triple<String, ByteArray, Boolean> {
        val type = detectType(upload.content) ?: return Triple(safeName(upload.fileName), upload.content, false)
        val stripped = stripMetadata(upload.content, type)
        val baseName = safeName(upload.fileName).substringBeforeLast('.').takeIf { it.isNotBlank() } ?: "image"
        return Triple("$baseName.${type.extension}", stripped, true)
    }

    companion object {
        const val ERROR_TOO_MANY = "chat.file.error.tooMany"
        const val ERROR_TOO_LARGE = "chat.file.error.tooLarge"
        const val ERROR_UNAVAILABLE = "chat.file.error.unavailable"
        const val ERROR_NOT_ALLOWED = "chat.file.error.notAllowed"

        /** The file ids of the data transfer (random, alphanumeric). */
        private val FILE_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,100}")

        private val UNSAFE_CHARS = Regex("[^\\p{L}\\p{N}._ -]")

        /** The name of the area in the data transfer lists. */
        internal fun areaName(title: String) = "Chat: $title"

        /** The file name without path and unsafe characters, "file" if nothing is left. */
        internal fun safeName(fileName: String?): String {
            val name = fileName?.substringAfterLast('/')?.substringAfterLast('\\')?.replace(UNSAFE_CHARS, "_")
                ?.trim()?.take(150)
            return name?.takeIf { it.isNotBlank() && it.any { c -> c != '.' && c != '_' } } ?: "file"
        }

        /** The maximum size of a PNG or JPEG image in pixels (about 160 MB decoded). */
        internal const val MAX_PIXELS = 40_000_000L

        /** The size of the image in pixels by its header (not decoded), null if not readable. */
        internal fun pixels(content: ByteArray): Long? {
            return try {
                ImageIO.createImageInputStream(content.inputStream()).use { input ->
                    val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
                    try {
                        reader.input = input
                        reader.getWidth(0).toLong() * reader.getHeight(0)
                    } finally {
                        reader.dispose()
                    }
                }
            } catch (ex: Exception) {
                log.debug { "Size of chat image not readable: ${ex.message}" }
                null
            }
        }

        /** The type of the image by its content (magic bytes), null for anything else. */
        fun detectType(content: ByteArray): ImageType? {
            fun startsWith(vararg bytes: Int, offset: Int = 0): Boolean {
                if (content.size < offset + bytes.size) return false
                return bytes.indices.all { content[offset + it] == bytes[it].toByte() }
            }
            return when {
                startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> ImageType.PNG
                startsWith(0xFF, 0xD8, 0xFF) -> ImageType.JPEG
                startsWith(0x47, 0x49, 0x46, 0x38, 0x37, 0x61) || startsWith(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) -> ImageType.GIF
                startsWith(0x52, 0x49, 0x46, 0x46) && startsWith(0x57, 0x45, 0x42, 0x50, offset = 8) -> ImageType.WEBP
                else -> null
            }
        }

        /**
         * The image encoded again without its metadata (JPEG, PNG). GIF and WebP are kept as they are (animations),
         * also an image that can't be decoded (e.g. a CMYK JPEG), which is logged.
         *
         * An image of more than [MAX_PIXELS] is refused before decoding: a small file may declare a huge size, whose
         * decoding would need gigabytes of memory (decompression bomb).
         */
        internal fun stripMetadata(content: ByteArray, type: ImageType): ByteArray {
            if (type != ImageType.JPEG && type != ImageType.PNG) {
                return content
            }
            val pixels = pixels(content)
            if (pixels != null && pixels > MAX_PIXELS) {
                throw UserException("chat.image.error.tooManyPixels", MAX_PIXELS / 1_000_000)
            }
            return try {
                val image = ImageIO.read(content.inputStream()) ?: return content.also {
                    log.warn { "Chat image ($type) not decodable, stored with its metadata." }
                }
                val output = if (type == ImageType.JPEG && image.colorModel.hasAlpha()) {
                    BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB).also {
                        it.createGraphics().apply { drawImage(image, 0, 0, null); dispose() }
                    }
                } else image
                val out = ByteArrayOutputStream()
                if (!ImageIO.write(output, if (type == ImageType.JPEG) "jpg" else "png", out)) {
                    return content
                }
                out.toByteArray()
            } catch (ex: Exception) {
                log.warn { "Chat image ($type) not re-encoded, stored with its metadata: ${ex.message}" }
                content
            }
        }
    }
}
