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
import org.projectforge.business.configuration.DomainService
import org.projectforge.files.FileInfo
import org.projectforge.files.FileObject
import org.projectforge.files.FileSizeChecker
import org.projectforge.files.store.FileStore
import org.projectforge.framework.integration.IntegrationConfig
import org.projectforge.framework.integration.PublicUrlFetcher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.min

private val log = KotlinLogging.logger {}

/**
 * The previews of links in chat messages ([ChatLinkPreview]): the page of the first link of a message is fetched by
 * the server in the background after the message was sent or edited ([ChatService]), its title, description and image
 * are shown below the message.
 *
 * The browser never loads anything from the linked site (the Content Security Policy allows no external hosts, and
 * the readers' addresses stay unknown to it): the image is stored as a thumbnail in the file store, below the
 * conversation, and served by the chat with the access of the message. The pages are fetched by
 * [PublicUrlFetcher] (public addresses only, see SSRF), every url at most once per [CACHE_MILLIS].
 *
 * Switched off by `projectforge.chat.linkPreview.enabled=false` (no request to any other site at all).
 */
@Service
open class ChatLinkPreviewService {
    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var fileStore: FileStore

    @Value("\${projectforge.chat.linkPreview.enabled:true}")
    open var enabled: Boolean = true

    /** A few threads: a slow site doesn't delay the previews of others. Tests run inline. */
    var executor: Executor = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "ChatLinkPreview").also { it.isDaemon = true }
    }

    /** Fetches the page of the url with its thumbnail (JPEG), null if none. Replaced by tests. */
    internal var fetcher: (String) -> Fetched? = { fetch(it) }

    class Fetched(val page: ChatLinkPreview.Page, val thumbnail: Thumbnail?)

    /** A JPEG of at most [THUMBNAIL_SIZE] pixels. */
    class Thumbnail(val content: ByteArray, val width: Int, val height: Int)

    private class Cached(val time: Long, val fetched: Fetched?)

    /** The fetched pages by url, also the failed ones (null). */
    private val cache = ConcurrentHashMap<String, Cached>()

    /** The link of the html of a message to show a preview of, null if none (or switched off). */
    open fun previewUrl(html: String?): String? {
        if (!enabled) {
            return null
        }
        return ChatLinkPreview.firstUrl(html, ownHost)
    }

    /**
     * The preview of the url now (for the message being written), from the cache if fetched before: sending the message
     * then takes it from there. Null if the page has none (or switched off).
     */
    open fun fetchNow(url: String): Fetched? {
        if (!enabled) {
            return null
        }
        return getFetched(url)
    }

    /**
     * Fetches the preview of the url in the background and gives it to [apply] (not for none). If [apply] doesn't take
     * it (the message changed meanwhile), its image is deleted again.
     */
    open fun schedule(conversationId: Long, url: String, apply: (ChatLinkPreview) -> Boolean) {
        executor.execute {
            try {
                val fetched = getFetched(url) ?: return@execute
                val page = fetched.page
                val thumbnail = fetched.thumbnail
                val imageFileId = thumbnail?.let { storeImage(conversationId, it.content) }
                val preview = ChatLinkPreview(
                    url, page.title, page.description, page.siteName, imageFileId,
                    imageWidth = imageFileId?.let { thumbnail.width },
                    imageHeight = imageFileId?.let { thumbnail.height },
                )
                if (!apply(preview)) {
                    deleteImage(conversationId, preview)
                }
            } catch (ex: Exception) {
                log.warn(ex) { "Preview of chat link not created: ${ex.message}" }
            }
        }
    }

    /** The thumbnail (JPEG) of a preview of the conversation, null if not found. No access check. */
    open fun readImage(conversationId: Long, fileId: String): ByteArray? {
        if (!FILE_ID_PATTERN.matches(fileId)) {
            return null
        }
        val fileObject = fileStore.getFileInfo(path(conversationId), IMAGE_NODE, fileId = fileId) ?: return null
        if (!fileStore.retrieveFile(fileObject)) {
            return null
        }
        return fileObject.content
    }

    /** Deletes the thumbnail of the preview, if any. No access check. */
    open fun deleteImage(conversationId: Long, preview: ChatLinkPreview?) {
        val fileId = preview?.imageFileId ?: return
        try {
            fileStore.deleteFile(FileObject(path(conversationId), IMAGE_NODE, fileId = fileId))
        } catch (ex: Exception) {
            log.error(ex) { "Thumbnail of chat link preview (conversation #$conversationId) not deleted: ${ex.message}" }
        }
    }

    /** Deletes the thumbnails of all previews of the conversation (deleted). No access check. */
    open fun deleteAll(conversationId: Long) {
        try {
            fileStore.deleteAllFilesBelow(path(conversationId))
        } catch (ex: Exception) {
            log.error(ex) { "Thumbnails of chat conversation #$conversationId not deleted: ${ex.message}" }
        }
    }

    private val ownHost: String?
        get() = runCatching { domainService.plainDomain }.getOrNull()

    private fun getFetched(url: String): Fetched? {
        val now = System.currentTimeMillis()
        cache[url]?.takeIf { now - it.time < CACHE_MILLIS }?.let { return it.fetched }
        val fetched = fetcher(url)
        if (cache.size >= MAX_CACHE_SIZE) {
            cache.entries.removeIf { now - it.value.time >= CACHE_MILLIS }
            if (cache.size >= MAX_CACHE_SIZE) {
                cache.clear()
            }
        }
        cache[url] = Cached(now, fetched)
        return fetched
    }

    private fun fetch(url: String): Fetched? {
        val response = PublicUrlFetcher.fetch(
            IntegrationConfig.LINK_PREVIEW,
            url,
            accept = "text/html,application/xhtml+xml;q=0.9",
            acceptContentType = { type -> type?.lowercase()?.let { it.startsWith("text/html") || it.startsWith("application/xhtml") } == true },
            maxBytes = MAX_PAGE_BYTES,
            maxMillis = MAX_MILLIS,
            truncate = true, // The meta data is in the head.
        ) ?: return null
        val page = ChatLinkPreview.parse(response.body, response.contentType, response.url) ?: return null
        val thumbnail = page.imageUrl?.let { imageUrl ->
            PublicUrlFetcher.fetch(
                IntegrationConfig.LINK_PREVIEW,
                imageUrl,
                accept = "image/png,image/jpeg,image/gif",
                acceptContentType = { type -> type?.lowercase()?.startsWith("image/") == true },
                maxBytes = MAX_IMAGE_BYTES,
                maxMillis = MAX_MILLIS,
            )?.let { thumbnail(it.body) }
        }
        return Fetched(page, thumbnail)
    }

    private object ThumbnailSizeChecker : FileSizeChecker {
        override val maxFileSize: Long = MAX_IMAGE_BYTES

        override fun checkSize(file: FileInfo, data: Any?, displayUserMessage: Boolean) {
            if ((file.size ?: 0) > maxFileSize) {
                throw IllegalStateException("Thumbnail of chat link preview too large.")
            }
        }
    }

    private fun storeImage(conversationId: Long, thumbnail: ByteArray): String? {
        return try {
            val fileObject = FileObject(path(conversationId), IMAGE_NODE, fileInfo = FileInfo(fileName = "preview.jpg"))
            fileObject.content = thumbnail
            fileStore.storeFile(fileObject, ThumbnailSizeChecker)
            fileObject.fileId
        } catch (ex: Exception) {
            log.error(ex) { "Thumbnail of chat link preview (conversation #$conversationId) not stored: ${ex.message}" }
            null
        }
    }

    companion object {
        const val CACHE_MILLIS = 60 * 60 * 1000L
        private const val MAX_CACHE_SIZE = 200
        private const val MAX_MILLIS = 8_000L
        private const val MAX_PAGE_BYTES = 512L * 1024
        private const val MAX_IMAGE_BYTES = 5L * 1024 * 1024

        /** The maximum width and height of a thumbnail. */
        const val THUMBNAIL_SIZE = 640

        /** The maximum size of an image to make a thumbnail of, in pixels (decoded about 100 MB). */
        private const val MAX_PIXELS = 25_000_000L

        private const val IMAGE_NODE = "images"

        private val FILE_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,100}")

        /** The location of the thumbnails of a conversation in the file store. */
        internal fun path(conversationId: Long) = "chat.linkPreview/$conversationId"

        /**
         * The image (PNG, JPEG, GIF, checked by its content) as a JPEG of at most [THUMBNAIL_SIZE] pixels, without its
         * metadata. Null if it isn't such an image, too large or not decodable.
         */
        internal fun thumbnail(content: ByteArray): Thumbnail? {
            val type = ChatFileService.detectType(content)
            if (type == null || type == ChatFileService.ImageType.WEBP) {
                return null
            }
            val pixels = ChatFileService.pixels(content)
            if (pixels == null || pixels > MAX_PIXELS) {
                return null
            }
            return try {
                val image = ImageIO.read(content.inputStream()) ?: return null
                val scale = min(1.0, min(THUMBNAIL_SIZE.toDouble() / image.width, THUMBNAIL_SIZE.toDouble() / image.height))
                val width = (image.width * scale).toInt().coerceAtLeast(1)
                val height = (image.height * scale).toInt().coerceAtLeast(1)
                val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                output.createGraphics().apply {
                    setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                    color = java.awt.Color.WHITE // Background of transparent images.
                    fillRect(0, 0, width, height)
                    drawImage(image, 0, 0, width, height, null)
                    dispose()
                }
                val writer = ImageIO.getImageWritersByFormatName("jpg").next()
                val out = ByteArrayOutputStream()
                try {
                    ImageIO.createImageOutputStream(out).use { stream ->
                        writer.output = stream
                        val param = writer.defaultWriteParam.also {
                            it.compressionMode = ImageWriteParam.MODE_EXPLICIT
                            it.compressionQuality = 0.82f
                        }
                        writer.write(null, IIOImage(output, null, null), param)
                    }
                } finally {
                    writer.dispose()
                }
                Thumbnail(out.toByteArray(), width, height)
            } catch (ex: Exception) {
                log.debug { "Thumbnail of chat link preview not created: ${ex.message}" }
                null
            }
        }
    }
}
