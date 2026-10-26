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

import java.util.Date

/** A user or group as shown by the chat (and as chosen by the pickers of the next app). */
class ChatRef(val id: Long, val displayName: String)

/** A file of a message (stored in the data transfer). */
class ChatFile(
    /** The data transfer area of the file: the area of the conversation, for direct messages the personal box. */
    val areaId: Long,
    val fileId: String,
    val name: String,
    val size: Long,
    /** E.g. "1.2 MB". */
    val sizeFormatted: String,
    /** An image (PNG, JPEG, GIF, WebP), shown as thumbnail. */
    val image: Boolean,
    /** False, if the file expired or was deleted in the data transfer. */
    val available: Boolean,
)

/** The reactions of a message with one emoji. */
class ChatReaction(
    val emoji: String,
    val count: Int,
    /** The names of the users who reacted. */
    val users: List<String>,
    /** Reacted the logged-in user? */
    val mine: Boolean,
)

/**
 * A message as shown to the logged-in user: the content of a deleted message is never sent, of a hidden one only to
 * moderators.
 */
class ChatMessage(
    val id: Long,
    val conversationId: Long,
    val threadRootId: Long?,
    val author: ChatRef,
    val created: Date?,
    val editedAt: Date?,
    val deleted: Boolean,
    val hidden: Boolean,
    /** Name of the moderator and reason, for moderators only. */
    val hiddenBy: String? = null,
    val hiddenReason: String? = null,
    /** Sanitized HTML, null for a deleted message (and a hidden one, except for moderators). */
    val content: String?,
    val files: List<ChatFile>,
    val reactions: List<ChatReaction>,
    val replyCount: Int,
    val lastReplyAt: Date?,
    /** The authors of the replies (thread summary), at most a few. */
    val replyAuthors: List<ChatRef>,
    /** Follows the logged-in user the thread of this message? */
    val following: Boolean,
    /** Is it a message of the logged-in user (editable, deletable)? */
    val own: Boolean,
    /** Is the logged-in user mentioned? */
    val mentionsMe: Boolean,
    val changeSeq: Long,
)

class ChatConversation(
    val id: Long,
    val hostType: String,
    val hostId: Long,
    val channel: String,
    val title: String,
    /** Path of the next app of the host object. */
    val link: String?,
    val canWrite: Boolean,
    val isModerator: Boolean,
    val muted: Boolean,
    /** The id of the newest message read by the logged-in user before, for the "new" line. */
    val lastReadMessageId: Long?,
    /** The newest message or reply, to mark the conversation read up to it. */
    val newestMessageId: Long?,
    val changeSeq: Long,
    val me: ChatRef,
    /** The other one of a direct message, null for other conversations. */
    val directPartner: ChatRef?,
    /** Can files be attached (data transfer available)? */
    val filesEnabled: Boolean,
    val maxFilesPerMessage: Int,
    /** The largest file (bytes) the conversation takes now, null if unknown (no write access) or no files. */
    val maxFileSize: Long?,
    val maxFileSizeFormatted: String?,
    /** The largest request (bytes): all files of a message together. */
    val maxRequestSize: Long,
)

class ChatMessagesPage(val messages: List<ChatMessage>, val hasMore: Boolean, val changeSeq: Long)

class ChatThread(val root: ChatMessage, val replies: List<ChatMessage>, val changeSeq: Long)

class ChatChanges(val messages: List<ChatMessage>, val changeSeq: Long, val complete: Boolean)

class ChatOpenRequest(val hostType: String? = null, val hostId: Long? = null, val channel: String? = null)

/** The conversation id, null if there is none yet and the user may not write (nothing to show). */
class ChatOpenResult(val conversationId: Long?)

/** The message part of a message sent (multipart, with the files). */
class ChatSendRequest(val html: String? = null, val threadRootId: Long? = null)

class ChatEditRequest(val html: String? = null)

class ChatHideRequest(val hidden: Boolean = true, val reason: String? = null)

class ChatReactionRequest(val emoji: String? = null)

class ChatReadRequest(val messageId: Long? = null)

class ChatFlagRequest(val value: Boolean = true)

/** The translated message of a refused call (HTTP 400). */
class ChatErrorResult(val message: String)

/** A room in the list of the logged-in user's rooms. */
class ChatRoomEntry(
    val id: Long,
    val name: String,
    val description: String?,
    val conversationId: Long?,
    val participantCount: Int,
    val unread: Long,
    val lastMessageAt: Date?,
    val canEdit: Boolean,
)

/**
 * A room as edited. Owner and the right to edit are given by the server only, also whether files can be exchanged
 * and the choices of their expiry.
 */
class ChatRoom(
    var id: Long? = null,
    var name: String? = null,
    var description: String? = null,
    var owner: ChatRef? = null,
    var moderators: List<ChatRef> = emptyList(),
    var users: List<ChatRef> = emptyList(),
    var groups: List<ChatRef> = emptyList(),
    var canEdit: Boolean = false,
    /** The days the files of the room are kept (data transfer area of its conversation). */
    var fileExpiryDays: Int? = null,
    var fileExpiryDaysChoices: List<ChatChoice> = emptyList(),
    var filesEnabled: Boolean = false,
)

/** A choice of a select, with its translated label. */
class ChatChoice(val value: Int, val label: String)

class ChatRoomSaveResult(val id: Long, val conversationId: Long?)

/** A direct message in the list of the logged-in user's ones. */
class ChatDirectEntry(
    val conversationId: Long,
    /** The other one. */
    val partner: ChatRef,
    val unread: Long,
    val lastMessageAt: Date?,
    /** False, if the other one has no system access any more (read-only). */
    val canWrite: Boolean,
)

class ChatDirectOpenRequest(val userId: Long? = null)

class ChatDirectOpenResult(val conversationId: Long)

/** Defaults of a new room: expiry of its files, its choices, files possible. */
class ChatRoomDefaults(val fileExpiryDays: Int?, val fileExpiryDaysChoices: List<ChatChoice>, val filesEnabled: Boolean)

/**
 * A confidential message of a direct chat ([org.projectforge.business.chat.ChatSecretRelay]) as one of its two sides
 * sees it: never the ciphertext (only [ChatSecretPayload] by take).
 */
class ChatSecretSession(
    val id: String,
    val conversationId: Long,
    /** OFFERED, ACCEPTED, DELIVERED or TAKEN (received by the recipient, nothing of it left). */
    val state: String,
    val sender: ChatRef,
    val recipient: ChatRef,
    /** Is the logged-in user the sender? */
    val mine: Boolean,
    /** The public key of the recipient (raw P-256, base64url), after accepting. */
    val recipientKey: String?,
    val expiresAt: Date,
)

class ChatSecretOfferRequest(val conversationId: Long? = null)

class ChatSecretAcceptRequest(val publicKey: String? = null)

/** The ciphertext (AES-GCM) with the public key of the sender and the iv, all base64url. */
class ChatSecretPayload(val senderKey: String? = null, val iv: String? = null, val ciphertext: String? = null)
