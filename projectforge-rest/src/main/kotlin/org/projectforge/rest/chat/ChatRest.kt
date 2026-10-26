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
import org.projectforge.business.chat.ChatDirectChatHost
import org.projectforge.business.chat.ChatFileService
import org.projectforge.business.chat.ChatMessageDao
import org.projectforge.business.chat.ChatService
import org.projectforge.common.FormatterUtils
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
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.util.unit.DataSize
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * The conversations of the chat (PF-CHAT) for the chat panel and page of projectforge-next.
 *
 * Every endpoint checks the access by the conversation taken from the database ([ChatService], [ChatAccess]): the
 * host of the conversation decides who reads, writes and moderates. Ids of the request (message, thread root, file)
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
    private lateinit var directHost: ChatDirectChatHost

    @Autowired
    private lateinit var fileService: ChatFileService

    @Value("\${spring.servlet.multipart.max-request-size:110MB}")
    private lateinit var maxRequestSizeConfig: String

    @AccessChecked("ChatService.open: read access of the host to the host object, created only with write access")
    @PostMapping("open")
    fun open(@RequestBody request: ChatOpenRequest): ChatOpenResult {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val hostType = request.hostType ?: throw AccessException(user, NO_ACCESS)
        val hostId = request.hostId ?: throw AccessException(user, NO_ACCESS)
        val channel = request.channel ?: throw AccessException(user, NO_ACCESS)
        return ChatOpenResult(chatService.open(hostType, hostId, channel)?.conversationId)
    }

    /**
     * The conversations of the host type with at least one message (e.g. the chats of the data transfer areas), newest
     * first.
     */
    @AccessChecked("ChatService.listConversations: read access of the host per conversation")
    @GetMapping("list/{hostType}")
    fun listConversations(@PathVariable hostType: String): List<ChatHostEntry> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val contexts = chatService.listConversations(hostType)
        val unread = chatService.countUnread(contexts.map { it.conversationId }, user.id!!)
        return contexts.map { context ->
            ChatHostEntry(
                conversationId = context.conversationId,
                hostId = context.hostId,
                title = context.title,
                link = context.host.link(context.hostId),
                unread = unread[context.conversationId] ?: 0,
                lastMessageAt = context.conversation.lastMessageAt,
                canWrite = context.canWrite,
            )
        }
    }

    @AccessChecked("ChatService.requireContext: read access of the host to the conversation")
    @GetMapping("conversation/{id}")
    fun getConversation(@PathVariable id: Long): ChatConversation {
        val context = chatService.requireContext(id)
        val state = chatService.getMemberState(id, context.user.id!!)
        val maxFileSize = fileService.maxFileSize(context)
        return ChatConversation(
            id = id,
            hostType = context.conversation.hostType!!,
            hostId = context.hostId,
            channel = context.channel,
            title = context.title,
            description = context.host.description(context.hostId, context.channel),
            notice = context.host.notice(context.hostId, context.channel, context.user),
            link = context.host.link(context.hostId),
            canWrite = context.canWrite,
            isModerator = context.isModerator,
            muted = state?.muted == true,
            lastReadMessageId = state?.lastReadMessageId,
            newestMessageId = chatService.newestMessageId(id),
            changeSeq = chatService.currentSeq(id),
            me = converter.ref(context.user.id),
            directPartner = if (context.conversation.hostType == ChatDirectChatHost.HOST_TYPE) {
                directHost.partnerId(context.hostId, context.user.id)?.let { converter.ref(it) }
            } else null,
            filesEnabled = fileService.filesEnabled(context),
            maxFilesPerMessage = fileService.maxPerMessage,
            maxFileSize = maxFileSize,
            maxFileSizeFormatted = maxFileSize?.let { FormatterUtils.formatBytes(it) },
            maxRequestSize = DataSize.parse(maxRequestSizeConfig).toBytes(),
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
        // No buffering by a reverse proxy (nginx), see the admin guide; no-transform: no proxy compresses (and so
        // buffers) the stream, e.g. the dev server of the next app.
        response.setHeader("X-Accel-Buffering", "no")
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
        log.debug { "Chat events of conversation #$id subscribed by user #${context.user.id} (last event: $lastEventId)." }
        return eventPublisher.subscribe(context, chatService.currentSeq(id))
    }

    /** A message without files (JSON). */
    @AccessChecked("ChatService.send: write access of the host to the conversation; thread root of this conversation only")
    @PostMapping("conversation/{id}/messages", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun send(@PathVariable id: Long, @RequestBody request: ChatSendRequest): ChatMessage {
        val message = chatService.send(id, request.threadRootId, request.html)
        return converter.convert(chatService.requireContext(id), message)
    }

    /**
     * A message with files (multipart: the part `message` as JSON, the parts `files`): the files are stored in the
     * data transfer with the message, not before ([ChatFileService.store]).
     */
    @AccessChecked("ChatService.send: write access of the host to the conversation; thread root of this conversation only")
    @PostMapping("conversation/{id}/messages", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendWithFiles(
        @PathVariable id: Long,
        @RequestPart("message") request: ChatSendRequest,
        @RequestPart("files", required = false) files: List<MultipartFile>?,
    ): ChatMessage {
        val uploads = files.orEmpty().map { ChatFileService.Upload(it.originalFilename, it.bytes) }
        val message = chatService.send(id, request.threadRootId, request.html, uploads)
        return converter.convert(chatService.requireContext(id), message)
    }

    @AccessChecked("ChatService.edit: own message only, write access of the host to its conversation (from the database)")
    @PutMapping("message/{id}")
    fun edit(@PathVariable id: Long, @RequestBody request: ChatEditRequest): ChatMessage {
        val message = chatService.edit(id, request.html)
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

    /** The thread of the message was opened: resolves the notifications about mentions in it. */
    @AccessChecked("ChatService.markThreadRead: read access of the host to the conversation, root of this conversation only, own notifications only")
    @PostMapping("conversation/{id}/thread/{rootId}/read")
    fun markThreadRead(@PathVariable id: Long, @PathVariable rootId: Long): ResponseEntity<Unit> {
        chatService.markThreadRead(id, rootId)
        return ResponseEntity.noContent().build()
    }

    @AccessChecked("ChatService.markUnread: read access of the host to the conversation, message of this conversation only, own state only")
    @PostMapping("conversation/{id}/unread")
    fun markUnread(@PathVariable id: Long, @RequestBody request: ChatReadRequest): ResponseEntity<Unit> {
        val messageId = request.messageId ?: throw AccessException(ThreadLocalUserContext.requiredLoggedInUser, NO_ACCESS)
        chatService.markUnread(id, messageId)
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
        return chatService.mentionCandidates(context, search).map { converter.ref(it) }
    }

    /**
     * The messages of others unread since [after] (the read state when the conversation was opened), oldest first:
     * for jumping to the next one, also in a thread.
     */
    @AccessChecked("ChatService.requireContext: read access of the host to the conversation; own read state only")
    @GetMapping("conversation/{id}/unreadMessages")
    fun unreadMessages(@PathVariable id: Long, @RequestParam("after") after: Long): List<ChatUnreadMessage> {
        val context = chatService.requireContext(id)
        return chatService.selectUnread(context, after).map { (messageId, rootId) -> ChatUnreadMessage(messageId, rootId) }
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

    /**
     * A file of a message of the conversation (not of a deleted one, of a hidden one for moderators only); no external
     * link or token. Images (checked by their content when sent) are shown inline unless [download], any other file
     * is always an attachment of unknown type, so the browser never shows (or runs) it.
     */
    @AccessChecked("ChatFileService.download: read access of the host to the conversation, file of a visible message of it")
    @GetMapping("conversation/{id}/file/{fileId}")
    fun downloadFile(
        @PathVariable id: Long,
        @PathVariable fileId: String,
        @RequestParam("download", required = false) download: Boolean?,
    ): ResponseEntity<InputStreamResource> {
        val context = chatService.requireContext(id)
        val (attachment, file) = fileService.download(context, fileId) ?: return ResponseEntity.notFound().build()
        val (fileObject, inputStream) = file
        val imageType = if (attachment.image) ChatFileService.ImageType.ofExtension(fileObject.fileExtension) else null
        val disposition = if (imageType == null || download == true) ContentDisposition.attachment() else ContentDisposition.inline()
        val contentType = imageType?.let { MediaType.parseMediaType(it.mimeType) } ?: MediaType.APPLICATION_OCTET_STREAM
        return ResponseEntity.ok()
            .contentType(contentType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                disposition.filename(fileObject.fileName ?: attachment.name, Charsets.UTF_8).build().toString(),
            )
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
