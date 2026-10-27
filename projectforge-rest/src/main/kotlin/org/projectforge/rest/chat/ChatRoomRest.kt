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

import org.projectforge.business.chat.ChatFileService
import org.projectforge.business.chat.ChatRoomChatHost
import org.projectforge.business.chat.ChatRoomDO
import org.projectforge.business.chat.ChatRoomDao
import org.projectforge.business.chat.ChatService
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.RestError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The chat rooms (PF-CHAT-ROOM) for the chat page of projectforge-next: the rooms of the logged-in user, editing and
 * deleting them. The access is decided by [ChatRoomDao] for the room stored in the database; the owner is the
 * logged-in user on insert and never changed. New participants must be users and groups the logged-in user may see
 * (ROOM-05), the ones already in the room stay. The files of a room are kept in the data transfer area of its
 * conversation for the days chosen here ([ChatFileService]).
 */
@RestController
@RequestMapping("${Rest.URL}/chatRoom")
class ChatRoomRest {
    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var converter: ChatMessageConverter

    @Autowired
    private lateinit var fileService: ChatFileService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** The rooms the logged-in user takes part in, the ones with the newest messages first. */
    @AccessChecked("ChatRoomDao.hasAccess (select): participants of each room only")
    @GetMapping("list")
    fun list(): List<ChatRoomEntry> {
        val userId = ThreadLocalUserContext.requiredLoggedInUserId
        val rooms = chatRoomDao.selectAllNotDeleted()
        val conversations = chatService.findConversations(ChatRoomChatHost.HOST_TYPE, rooms.mapNotNull { it.id })
        val unread = chatService.countUnread(conversations.values.mapNotNull { it.id }, userId)
        return rooms.map { room ->
            val conversation = conversations[room.id]
            ChatRoomEntry(
                id = room.id!!,
                name = room.name ?: "",
                description = room.description,
                conversationId = conversation?.id,
                participantCount = chatRoomDao.getParticipantIds(room).size,
                unread = conversation?.id?.let { unread[it] } ?: 0,
                lastMessageAt = conversation?.lastMessageAt,
                canEdit = chatRoomDao.isModerator(room, userId),
            )
        }.sortedWith(compareByDescending<ChatRoomEntry> { it.lastMessageAt }.thenBy { it.name.lowercase() })
    }

    /** The defaults of a new room: expiry of its files and the choices, whether files can be exchanged at all. */
    @AccessChecked("No data: static choices and the availability of the data transfer only")
    @GetMapping("defaults")
    fun defaults(): ChatRoomDefaults {
        return ChatRoomDefaults(
            fileExpiryDays = DEFAULT_FILE_EXPIRY_DAYS,
            fileExpiryDaysChoices = expiryDaysChoices(),
            filesEnabled = fileService.filesAvailable,
        )
    }

    @AccessChecked("ChatRoomDao.find: participants of the room only")
    @GetMapping("{id}")
    fun get(@PathVariable id: Long): ChatRoom {
        val room = chatRoomDao.find(id) ?: throw AccessException(ThreadLocalUserContext.requiredLoggedInUser, NO_ACCESS)
        return toDto(room)
    }

    /**
     * Creates (no id) or updates the room. Opens its conversation, so the answer has its id.
     */
    @AccessChecked("ChatRoomDao.insert (owner = logged-in user) / update (owner or moderator of the stored room); new participants visible to the user")
    @PostMapping("save")
    fun save(@RequestBody dto: ChatRoom): ChatRoomSaveResult {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val name = dto.name?.trim()
        if (name.isNullOrEmpty()) {
            throw UserException("chat.room.error.nameRequired")
        }
        // Rich text (HTML): cut off, its markup would be broken.
        val description = dto.description?.trim()?.takeIf { it.isNotEmpty() }
        if ((description?.length ?: 0) > DESCRIPTION_MAX_LENGTH) {
            throw UserException("chat.room.error.descriptionTooLong")
        }
        val stored = dto.id?.let { chatRoomDao.find(it, checkAccess = false) ?: throw AccessException(user, NO_ACCESS) }
        val userIds = visibleUserIds(dto.users, stored?.participantUserIds)
        val moderatorIds = visibleUserIds(dto.moderators, stored?.moderatorUserIds)
        val groupIds = visibleGroupIds(dto.groups, stored?.participantGroupIds)
        val room = (stored ?: ChatRoomDO()).also {
            it.name = name.take(255)
            it.description = description
            it.participantUserIds = ChatRoomDO.toCsv(userIds)
            it.moderatorUserIds = ChatRoomDO.toCsv(moderatorIds)
            it.participantGroupIds = ChatRoomDO.toCsv(groupIds)
        }
        val id = if (stored == null) {
            room.ownerId = user.id
            chatRoomDao.insert(room)
        } else {
            chatRoomDao.update(room)
            stored.id!!
        }
        val conversationId = chatService.open(ChatRoomChatHost.HOST_TYPE, id, ChatRoomChatHost.CHANNEL)?.conversationId
        conversationId?.let { fileService.saveArea(it, name, dto.fileExpiryDays) } // Name and expiry of its files.
        return ChatRoomSaveResult(id, conversationId)
    }

    @AccessChecked("ChatService.deleteRoom: ChatRoomDao.markAsDeleted (owner or moderator of the stored room)")
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        chatService.deleteRoom(id)
        return ResponseEntity.noContent().build()
    }

    private fun toDto(room: ChatRoomDO): ChatRoom {
        return ChatRoom(
            id = room.id,
            name = room.name,
            description = room.description,
            owner = converter.ref(room.ownerId),
            moderators = ChatRoomDO.toIds(room.moderatorUserIds).map { converter.ref(it) },
            users = ChatRoomDO.toIds(room.participantUserIds).map { converter.ref(it) },
            groups = ChatRoomDO.toIds(room.participantGroupIds).map { groupId ->
                val group = userGroupCache.getGroup(groupId)
                ChatRef(groupId, group?.name ?: "?", deleted = group?.deleted == true)
            },
            canEdit = chatRoomDao.isModerator(room, ThreadLocalUserContext.requiredLoggedInUserId),
            fileExpiryDays = chatService.findConversations(ChatRoomChatHost.HOST_TYPE, listOf(room.id!!))[room.id]
                ?.let { fileService.getExpiryDays(it.id!!) } ?: DEFAULT_FILE_EXPIRY_DAYS,
            fileExpiryDaysChoices = expiryDaysChoices(),
            filesEnabled = fileService.filesAvailable,
        )
    }

    private fun expiryDaysChoices(): List<ChatChoice> {
        return fileService.getExpiryDaysChoices().map { days ->
            ChatChoice(days, if (days == 1) translate("duration.days.one") else translateMsg("duration.days", days))
        }
    }

    /** The posted users: the ones already stored, and new ones the logged-in user may see (ROOM-05). */
    private fun visibleUserIds(refs: List<ChatRef>, storedCsv: String?): Set<Long> {
        val stored = ChatRoomDO.toIds(storedCsv)
        return refs.map { it.id }.filter { id ->
            id in stored || userGroupCache.getUser(id)?.let { user ->
                !user.deleted && userDao.hasLoggedInUserSelectAccess(user, false)
            } == true
        }.toSet()
    }

    /** The posted groups: the ones already stored, and new ones the logged-in user may see (ROOM-05). */
    private fun visibleGroupIds(refs: List<ChatRef>, storedCsv: String?): Set<Long> {
        val stored = ChatRoomDO.toIds(storedCsv)
        return refs.map { it.id }.filter { id ->
            id in stored || userGroupCache.getGroup(id)?.let { group ->
                !group.deleted && groupDao.hasLoggedInUserSelectAccess(group, false)
            } == true
        }.toSet()
    }

    @ExceptionHandler(AccessException::class)
    fun handleAccessException(ex: AccessException): ResponseEntity<RestError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(RestError(status = HttpStatus.FORBIDDEN.value(), message = translateMsg(ex)))
    }

    @ExceptionHandler(UserException::class)
    fun handleUserException(ex: UserException): ResponseEntity<ChatErrorResult> {
        return ResponseEntity.badRequest().body(ChatErrorResult(translateMsg(ex)))
    }

    companion object {
        private const val NO_ACCESS = "access.exception.noAccess"
        private const val DEFAULT_FILE_EXPIRY_DAYS = 30

        /** The length of the column [ChatRoomDO.description]. */
        const val DESCRIPTION_MAX_LENGTH = 4000
    }
}
