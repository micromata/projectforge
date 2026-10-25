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

package org.projectforge.rest.chat

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import org.projectforge.business.chat.ChatAccess
import org.projectforge.business.chat.ChatImageService
import org.projectforge.business.chat.ChatMessageDao
import org.projectforge.business.chat.ChatService
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistoryFormatService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.RestError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * The conversations of the chat (PF-CHAT) for the chat panel and page of projectforge-next.
 *
 * Every endpoint checks the access by the conversation taken from the database ([ChatService], [ChatAccess]): the
 * host of the conversation decides who reads, writes and moderates. Ids of the request (message, thread root, image)
 * are only accepted within this conversation. State changing calls are POST, PUT or DELETE (CSRF).
 *
 * A refused call answers 403 (no access) or 400 with the translated message ([ChatErrorResult]).
 */
@RestController
@RequestMapping("${Rest.URL}/chat")
class ChatRest {
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var chatMessageDao: ChatMessageDao

    @Autowired
    private lateinit var converter: ChatMessageConverter

    @Autowired
    private lateinit var eventPublisher: ChatEventPublisher

    @Autowired
    private lateinit var historyFormatService: HistoryFormatService

    @Autowired
    private lateinit var imageService: ChatImageService

    @AccessChecked("ChatService.open: read access of the host to the host object, created only with write access")
    @PostMapping("open")
    fun open(@RequestBody request: ChatOpenRequest): ChatOpenResult {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val hostType = request.hostType ?: throw AccessException(user, NO_ACCESS)
        val hostId = request.hostId ?: throw AccessException(user, NO_ACCESS)
        val channel = request.channel ?: throw AccessException(user, NO_ACCESS)
        return ChatOpenResult(chatService.open(hostType, hostId, channel)?.conversationId)
    }

    @AccessChecked("ChatService.requireContext: read access of the host to the conversation")
    @GetMapping("conversation/{id}")
    fun getConversation(@PathVariable id: Long): ChatConversation {
        val context = chatService.requireContext(id)
        val state = chatService.getMemberState(id, context.user.id!!)
        return ChatConversation(
            id = id,
            hostType = context.conversation.hostType!!,
            hostId = context.hostId,
            channel = context.channel,
            title = context.title,
            link = context.host.link(context.hostId),
            canWrite = context.canWrite,
            isModerator = context.isModerator,
            muted = state?.muted == true,
            lastReadMessageId = state?.lastReadMessageId,
            changeSeq = chatService.currentSeq(id),
            me = converter.ref(context.user.id),
            maxImagesPerMessage = imageService.maxPerMessage,
            maxImageSize = imageService.maxSize,
        )
    }

    /** The newest messages of the conversation older than [before] (paging upwards), newest first. */
    @AccessChecked("ChatService.requireContext: read access of the host to the conversation")
    @GetMapping("conversation/{id}/messages")
    fun getMessages(
        @PathVariable id: Long,
        @RequestParam("before", required = false) before: Long?,
        @RequestParam("limit", required = false) limit: Int?,
    ): ChatMessagesPage {
        val context = chatService.requireContext(id)
        val seq = chatService.currentSeq(id)
        val pageSize = (limit ?: PAGE_SIZE).coerceIn(1, ChatService.MAX_PAGE_SIZE)
        val messages = chatService.selectMessages(context, before, pageSize + 1)
        return ChatMessagesPage(
            converter.convert(context, messages.take(pageSize)),
            hasMore = messages.size > pageSize,
            changeSeq = seq,
        )
    }

    @AccessChecked("ChatService.requireContext: read access of the host to the conversation; root of this conversation only")
    @GetMapping("conversation/{id}/thread/{rootId}")
    fun getThread(@PathVariable id: Long, @PathVariable rootId: Long): ChatThread {
        val context = chatService.requireContext(id)
        val seq = chatService.currentSeq(id)
        val (root, replies) = chatService.selectThread(context, rootId)
            ?: throw AccessException(context.user, NO_ACCESS)
        return ChatThread(converter.convert(context, root), converter.convert(context, replies), seq)
    }

    /** The messages changed since the given sequence (live updates, reconnect, polling). */
    @AccessChecked("ChatService.requireContext: read access of the host to the conversation")
    @GetMapping("conversation/{id}/changes")
    fun getChanges(@PathVariable id: Long, @RequestParam("since") since: Long): ChatChanges {
        val context = chatService.requireContext(id)
        val changes = chatService.selectChanges(context, since)
        return ChatChanges(converter.convert(context, changes.messages), changes.changeSeq, changes.complete)
    }

    /**
     * Server-sent events of the conversation: an event (id = change sequence) per change, see [ChatEventPublisher].
     * Read only, so GET.
     */
    @AccessChecked("ChatService.requireContext: read access of the host to the conversation, checked again per event")
    @GetMapping("conversation/{id}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun events(
        @PathVariable id: Long,
        @RequestHeader("Last-Event-ID", required = false) lastEventId: String?,
        response: HttpServletResponse,
    ): SseEmitter {
        val context = chatService.requireContext(id)
        // No buffering by a reverse proxy (nginx), see the admin guide.
        response.setHeader("X-Accel-Buffering", "no")
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
        log.debug { "Chat events of conversation #$id subscribed by user #${context.user.id} (last event: $lastEventId)." }
        return eventPublisher.subscribe(context, chatService.currentSeq(id))
    }

    @AccessChecked("ChatService.send: write access of the host to the conversation; thread root of this conversation only")
    @PostMapping("conversation/{id}/messages")
    fun send(@PathVariable id: Long, @RequestBody request: ChatSendRequest): ChatMessage {
        val message = chatService.send(id, request.threadRootId, request.html, request.imageIds)
        return converter.convert(chatService.requireContext(id), message)
    }

    @AccessChecked("ChatService.edit: own message only, write access of the host to its conversation (from the database)")
    @PutMapping("message/{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: ChatEditRequest): ChatMessage {
        val message = chatService.edit(id, request.html, request.imageIds)
        return converter.convert(chatService.requireContext(message.conversationId), message)
    }

    @AccessChecked("ChatService.delete: own message only, write access of the host to its conversation (from the database)")
    @DeleteMapping("message/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        chatService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @AccessChecked("ChatService.hide: moderator of the conversation of the message (from the database)")
    @PostMapping("message/{id}/hide")
    fun hide(@PathVariable id: Long, @RequestBody request: ChatHideRequest): ResponseEntity<Unit> {
        chatService.hide(id, request.hidden, request.reason)
        return ResponseEntity.noContent().build()
    }

    @AccessChecked("ChatService.toggleReaction: write access of the host to the conversation of the message (from the database), own reaction only")
    @PostMapping("message/{id}/reaction")
    fun react(@PathVariable id: Long, @RequestBody request: ChatReactionRequest): ResponseEntity<Unit> {
        chatService.toggleReaction(id, request.emoji)
        return ResponseEntity.noContent().build()
    }

    @AccessChecked("ChatService.setFollowing: read access of the host to the conversation of the message (from the database), own state only")
    @PostMapping("message/{id}/follow")
    fun follow(@PathVariable id: Long, @RequestBody request: ChatFlagRequest): ResponseEntity<Unit> {
        chatService.setFollowing(id, request.value)
        return ResponseEntity.noContent().build()
    }

    /** The edits, deletion and hiding of the message, for moderators (MSG-03). */
    @AccessChecked("ChatMessageDao.hasHistoryAccess: moderator of the conversation of the message (from the database)")
    @GetMapping("message/{id}/history")
    fun history(@PathVariable id: Long): List<DisplayHistoryEntry> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val message = chatService.findMessage(id) ?: throw AccessException(user, NO_ACCESS)
        chatMessageDao.hasLoggedInUserHistoryAccess(message, throwException = true)
        return historyFormatService.selectAsDisplayEntries(chatMessageDao, message)
    }

    @AccessChecked("ChatService.markRead: read access of the host to the conversation, message of this conversation only, own state only")
    @PostMapping("conversation/{id}/read")
    fun markRead(@PathVariable id: Long, @RequestBody request: ChatReadRequest): ResponseEntity<Unit> {
        val messageId = request.messageId ?: throw AccessException(ThreadLocalUserContext.requiredLoggedInUser, NO_ACCESS)
        chatService.markRead(id, messageId)
        return ResponseEntity.noContent().build()
    }

    @AccessChecked("ChatService.setMuted: read access of the host to the conversation, own state only")
    @PostMapping("conversation/{id}/mute")
    fun mute(@PathVariable id: Long, @RequestBody request: ChatFlagRequest): ResponseEntity<Unit> {
        chatService.setMuted(id, request.value)
        return ResponseEntity.noContent().build()
    }

    /** The users who may be mentioned, see [ChatService.mentionCandidates]. */
    @AccessChecked("ChatService.requireContext: read access of the host to the conversation; candidates with read access only")
    @GetMapping("conversation/{id}/mentionCandidates")
    fun mentionCandidates(@PathVariable id: Long, @RequestParam("search", required = false) search: String?): List<ChatRef> {
        val context = chatService.requireContext(id)
        return chatService.mentionCandidates(context, search).map { ChatRef(it.id!!, it.getFullname()) }
    }

    @AccessChecked("ChatService.requireContext: read access of the host to the conversation")
    @GetMapping("conversation/{id}/search")
    fun search(
        @PathVariable id: Long,
        @RequestParam("q", required = false) q: String?,
        @RequestParam("authorId", required = false) authorId: Long?,
    ): List<ChatMessage> {
        val context = chatService.requireContext(id)
        return converter.convert(context, chatService.search(context, q, authorId))
    }

    @AccessChecked("ChatImageService.upload: write access of the host to the conversation")
    @PostMapping("conversation/{id}/image")
    fun uploadImage(@PathVariable id: Long, @RequestPart("file") file: MultipartFile): ChatImageResult {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        if (file.size > imageService.maxSize) {
            throw UserException("chat.image.error.tooLarge", imageService.maxSize / 1024 / 1024)
        }
        val attachment = imageService.upload(id, file.originalFilename, file.bytes, user)
        return ChatImageResult(attachment.fileId!!)
    }

    /** An image of the conversation, inline with its content type (checked on upload); no external link or token. */
    @AccessChecked("ChatImageService.download: read access of the host to the conversation")
    @GetMapping("conversation/{id}/image/{fileId}")
    fun downloadImage(
        @PathVariable id: Long,
        @PathVariable fileId: String,
        @RequestParam("download", required = false) download: Boolean?,
    ): ResponseEntity<InputStreamResource> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val (file, inputStream) = imageService.download(id, fileId, user)
            ?: return ResponseEntity.notFound().build()
        val type = ChatImageService.ImageType.ofExtension(file.fileExtension)
            ?: return ResponseEntity.notFound().build()
        val disposition = if (download == true) ContentDisposition.attachment() else ContentDisposition.inline()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(type.mimeType))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(file.fileName ?: "image.${type.extension}").build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
            .body(InputStreamResource(inputStream))
    }

    @ExceptionHandler(AccessException::class)
    fun handleAccessException(ex: AccessException): ResponseEntity<RestError> {
        log.info { "Chat: access denied: ${translateMsg(ex)}" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(RestError(status = HttpStatus.FORBIDDEN.value(), message = translateMsg(ex)))
    }

    @ExceptionHandler(UserException::class)
    fun handleUserException(ex: UserException): ResponseEntity<ChatErrorResult> {
        return ResponseEntity.badRequest().body(ChatErrorResult(translateMsg(ex)))
    }

    companion object {
        const val PAGE_SIZE = 50
        private const val NO_ACCESS = "access.exception.noAccess"
    }
}
