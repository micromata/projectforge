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
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.FileSizeChecker
import org.projectforge.jcr.FileSizeStandardChecker
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

/**
 * The images of chat messages (PF-CHAT ATT), stored in the file storage of the data transfer module (JCR via
 * [AttachmentsService]) in a technical area per conversation ([JCR_PATH]), which appears in no data transfer list.
 * The access is the one of the conversation ([ChatAccess]): readers download, writers upload. Images don't expire,
 * they are deleted with the conversation.
 *
 * Only PNG, JPEG, GIF and WebP are accepted, checked by the content. Metadata (EXIF, GPS) of JPEG and PNG images is
 * removed by encoding them again.
 */
@Service
open class ChatImageService {
    enum class ImageType(val mimeType: String, val extension: String) {
        PNG("image/png", "png"), JPEG("image/jpeg", "jpg"), GIF("image/gif", "gif"), WEBP("image/webp", "webp");

        companion object {
            fun ofExtension(extension: String?): ImageType? {
                val ext = extension?.lowercase() ?: return null
                return entries.find { it.extension == ext } ?: if (ext == "jpeg") JPEG else null
            }
        }
    }

    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var repoService: RepoService

    @Value("\${projectforge.chat.image.maxSize:10MB}")
    private lateinit var maxSizeConfig: String

    /** The maximum number of images of a message. */
    @Value("\${projectforge.chat.image.maxPerMessage:10}")
    open var maxPerMessage: Int = 10

    open val maxSize: Long
        get() = DataSize.parse(maxSizeConfig).toBytes()

    private val accessChecker: AttachmentsAccessChecker by lazy { ImagesAccessChecker() }

    /**
     * Stores the image for the conversation; the user needs write access.
     * @return The id of the image, given with the message ([ChatService.send]).
     */
    open fun upload(conversationId: Long, fileName: String?, content: ByteArray, user: PFUserDO): Attachment {
        chatAccess.requireWriteContext(conversationId, user)
        if (content.size > maxSize) {
            throw UserException("chat.image.error.tooLarge", maxSize / 1024 / 1024)
        }
        val type = detectType(content) ?: throw UserException("chat.image.error.unsupportedType")
        val stripped = stripMetadata(content, type)
        val baseName = fileName?.substringBeforeLast('.')?.replace(UNSAFE_CHARS, "_")?.take(100)
            ?.takeIf { it.isNotBlank() } ?: "image"
        return attachmentsService.addAttachment(
            JCR_PATH,
            conversationId,
            FileInfo("$baseName.${type.extension}", fileSize = stripped.size.toLong()),
            stripped,
            enableSearchIndex = false,
            accessChecker = accessChecker,
        )
    }

    /** The ids of the images stored for the conversation (no access check). */
    open fun getImageIds(conversationId: Long): Set<String> {
        return attachmentsService.internalGetAttachments(JCR_PATH, conversationId).mapNotNullTo(mutableSetOf()) { it.fileId }
    }

    /**
     * The image and its content, if the user may read the conversation; null if not found.
     */
    open fun download(conversationId: Long, fileId: String, user: PFUserDO): Pair<FileObject, InputStream>? {
        chatAccess.requireContext(conversationId, user)
        return attachmentsService.getAttachmentInputStream(JCR_PATH, conversationId, fileId, accessChecker)
    }

    /** Deletes all images of the conversation (with the conversation). No access check. */
    open fun deleteAll(conversationId: Long) {
        val deleted = repoService.deleteAllFilesBelow(attachmentsService.getPath(JCR_PATH, conversationId))
        log.info { "${deleted.size} image(s) of chat conversation #$conversationId deleted." }
    }

    /** The access of the conversation for the user, the conversation id is the id of the attachments. */
    private inner class ImagesAccessChecker : AttachmentsAccessChecker {
        override val fileSizeChecker: FileSizeChecker
            get() = FileSizeStandardChecker(maxSize, "projectforge.chat.image.maxSize")

        private fun context(user: PFUserDO?, id: Any): ChatAccess.Context {
            user ?: throw AccessException("access.exception.noAccess")
            return chatAccess.requireContext((id as? Number)?.toLong() ?: id.toString().toLongOrNull(), user)
        }

        private fun writeContext(user: PFUserDO?, id: Any): ChatAccess.Context {
            return context(user, id).also {
                if (!it.canWrite) throw AccessException(user, "access.exception.noAccess")
            }
        }

        override fun checkSelectAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
            context(user, id)
        }

        override fun checkUploadAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
            writeContext(user, id)
        }

        override fun checkDownloadAccess(user: PFUserDO?, path: String, id: Any, file: FileObject, subPath: String?) {
            context(user, id)
        }

        /** Images aren't changed or deleted one by one. */
        override fun checkUpdateAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
            throw AccessException(user, "access.exception.noAccess")
        }

        override fun checkDeleteAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
            throw AccessException(user, "access.exception.noAccess")
        }

        override fun hasAccess(
            user: PFUserDO?,
            path: String,
            id: Any,
            subPath: String?,
            operationType: OperationType,
            attachment: Attachment,
        ): Boolean {
            return operationType == OperationType.SELECT && user != null &&
                    chatAccess.getContext((id as? Number)?.toLong(), user) != null
        }
    }

    companion object {
        const val JCR_PATH = "org.projectforge.chat"

        private val UNSAFE_CHARS = Regex("[^A-Za-z0-9._-]")

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
         */
        internal fun stripMetadata(content: ByteArray, type: ImageType): ByteArray {
            if (type != ImageType.JPEG && type != ImageType.PNG) {
                return content
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
