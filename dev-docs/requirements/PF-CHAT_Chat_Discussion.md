# PF-CHAT Requirements - Chat (discussion about any object)

| Field | Value |
|---|---|
| COMP-ID | PF-CHAT |
| DOC-ID | PF-CHAT-REQ |
| Status | Draft 0.6 (data transfer areas as chat rooms, direct message from a personal box) |
| Owner | Kai Reinhard |
| Date | 2026-10-26 |
| Stage | R1 in implementation (ProjectForge 9.0.16) |
| Used by | chat rooms and direct messages (built-in hosts, PF-CHAT-ROOM, PF-CHAT-DM); data transfer areas (PF-CHAT-DT); [PF-CVR – CVE-Radar](PF-CVR_CVE-Radar_Vulnerability_Management.md); later invoices, orders and others |

---

## 1. Context and objective

Questions and discussions about an object in ProjectForge, such as a vulnerability, an invoice or an order, happen
today in mails and messengers outside PF. They are not found later, are not visible to whoever takes over, and
do not follow the access rules of the object.

**Objective:** a generic chat module, modelled on Google Chat, that any module can embed for its objects:
- conversations about an object, with replies to a single message as a thread of their own;
- messages written and edited with the rich text editor of PF, with files attached;
- access taken entirely from the object the conversation belongs to;
- chat rooms with participants (users and/or groups) for discussions that belong to no other object;
- mentions with `@` and suggestions, notifying the mentioned users;
- confidential conversations (e.g. to send a password) in direct messages, end-to-end encrypted and never stored.

The chat is a module of its own: it knows nothing about vulnerabilities or invoices. The embedding module (the
*host*) tells it who may read and write. The first host is the chat room (PF-CHAT-ROOM), a small built-in host
whose objects are rooms with participants; the CVE-Radar follows.

**Measurable benefit (targets):**
- Every discussion about an embedded object is found at the object, complete with files and threads.
- A new message is visible to the other participants within 3 s, without a reload.
- Embedding the chat into a new host needs no change of the chat module.

## 2. Stakeholders and roles

| Role | Description |
|---|---|
| Participant | A user allowed by the host to read a conversation; may write if the host allows writing. |
| Moderator | Defined by the host (e.g. the security team for vulnerabilities); may hide messages of others. |
| Host developer | Integrates the chat into a module by implementing the chat context. |
| PF administrator | Gets no access to conversations by their role alone; access comes from the host only. |

## 3. System context

```
 Host module (chat rooms, CVE-Radar, invoices, orders …)
   └ chat context: title, link, channels, read/write check, moderators, participants, retention
        │
        ▼
 ┌───────────────────────────┐ ── files ───▶ data transfer: an area per conversation, personal boxes (DM)
 │ PF-CHAT                   │ ── notify ──▶ notification system (in-app, mail, digest)
 │ conversations, messages,  │ ── live ────▶ browser via SSE (SseEmitter), polling as fallback
 │ threads, read states      │
 └───────────────────────────┘
        ▲
 projectforge-next: chat panel (embedded at the host object) and chat page per conversation
```

**Scope boundaries:**
- Conversations always belong to a host object. Free discussions use a chat room (PF-CHAT-ROOM) as host object,
  private ones a direct message (PF-CHAT-DM), whose host object is the pair of users.
- No video or voice calls, no screen sharing, no e-mail-to-chat, no integration of external messengers.
- The chat has no access rules of its own; it never widens what the host allows.

## 4. Glossary

| Term | Meaning |
|---|---|
| Host | The module that embeds the chat for its objects (e.g. CVE-Radar for vulnerabilities). |
| Host object | The object a conversation belongs to (e.g. vulnerability `CVE-2026-90101`, invoice 4711). |
| Chat context | The adapter a host registers: title, link, channels, access checks, moderators, retention of its objects. |
| Channel | A conversation kind of a host, e.g. *internal* and *public* for a vulnerability; one conversation per host object and channel. |
| Conversation | The messages about one host object in one channel. |
| Message | A post with author, time and rich text, optionally with files. |
| Thread | The replies to one message, shown apart from the conversation; one level deep. |
| Mention | `@user` in a message, chosen from suggestions; notifies the user. |
| Chat room | A host object of the built-in host *chat room*: name, description, owner, moderators and participants (users and groups). |
| Attachment | A file of a message (any type), stored in the data transfer; images are shown as thumbnails. |
| Direct message | A conversation of two users (built-in host *direct*), see PF-CHAT-DM. |
| Read state | Per user and conversation the id of the newest message read (one mark for the conversation and its threads); everything of others after it is unread. |
| Confidential conversation | A live session of the two browsers of a direct message: the texts are encrypted in one browser for the other, relayed by the server in memory and deleted when taken, see PF-CHAT-CONF. |

## 5. Requirements

Priority by MoSCoW: **M** = Must, **S** = Should, **C** = Could, **W** = Won't (this time).

### 5.1 PF-CHAT-CTX – Embedding into a host

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| CTX-01 | A host must be able to register a chat context for its object type, providing per host object: title, deep link, channels, read check, write check, moderators and the users to notify. | M | adapter interface, no change of the chat module per host |
| CTX-02 | The system must keep exactly one conversation per host object and channel and create it when a user with write access opens it the first time; opening without write access creates nothing; an unknown host object must be refused. | M | |
| CTX-03 | The frontend must offer a chat panel component that a host page embeds (e.g. as a tab or side panel of the host object). | M | shared component in `projectforge-next` |
| CTX-04 | Every conversation must also be reachable as a page of its own (`/next/chat/{id}`) showing the title of the host object with a link back to it. | M | links in notifications |
| CTX-05 | A conversation must follow the life cycle of its host object: read-only when the host says so (e.g. closed vulnerability), archived and deleted together with it. | M | |
| CTX-06 | A host should be able to show the number of conversations and unread messages per object in its lists (e.g. a chat column in the invoice list). | S | |
| CTX-07 | Free rooms and team rooms are chat rooms of the built-in host PF-CHAT-ROOM; private conversations of two users are direct messages of the built-in host PF-CHAT-DM. | M | decisions 2, 9, 12 |
| CTX-08 | A cleanup job should remove conversations and files whose host object no longer exists (e.g. orphaned files in the data transfer storage) and apply the retention rule of a host. | S | later; CTX-05 stays the rule |

### 5.2 PF-CHAT-MSG – Messages

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| MSG-01 | Messages must be written in a rich text editor and stored as sanitized HTML: bold, italic, underline, strike-through, text colours, lists, links, inline code and code blocks of several lines (no headings or tables). Enter sends, Shift+Enter starts a new line (paragraph, list item); ``` followed by Enter or a space starts a code block, in which Enter starts a new line, Tab indents and Ctrl/Cmd+Enter sends. Pasted markdown is formatted, code fences included. | M | the TipTap editor of PF with the chat's markup (`ChatEditor`); code without highlighting |
| MSG-02 | Every message must show author, time and, after a change, the mark "edited" with the time of the last change. | M | |
| MSG-03 | Authors must be able to edit their own messages with the rich text editor; every version is historised and visible to moderators. | M | `DefaultBaseDO` history |
| MSG-04 | Authors must be able to delete their own messages; a deleted message leaves the placeholder "Message deleted" so threads stay readable. | M | soft delete |
| MSG-05 | Every message must have a deep link that opens the conversation (or thread) at this message; the link can be copied. | M | |
| MSG-06 | A conversation must open with the newest 50 messages and load older ones when scrolling up; long conversations stay fluent (virtualised list). | M | |
| MSG-07 | Messages must be able to mention users: typing `@` opens suggestions, completed while typing, only from users with read access to the conversation; the choice inserts a mention chip. The server checks the mentioned users against the readers and notifies each newly mentioned user once, also after an edit. | M | NOT-03, decision 10 |
| MSG-08 | A message should be able to quote another message of the same conversation (with link to it). | S | by reference (`quotedMessageId`, checked by the server): author, time and excerpt are resolved when shown, so a deleted or hidden message shows its placeholder; a click shows the message, no notification |
| MSG-09 | An unsent draft should be kept per user and conversation. | S | |
| MSG-10 | Users should be able to react to messages and thread replies with emoji, as in Google Chat: quick choice of recently used emoji, per emoji the count and (on hover) who reacted; a second click takes the own reaction back. Reactions are not notified. | S | decision 7 |

### 5.3 PF-CHAT-THR – Threads

Modelled on Google Chat: the conversation shows the messages; a reply to a single message starts a thread of its
own instead of interleaving with the conversation.

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| THR-01 | Users must be able to reply to any message of a conversation (not to a deleted or hidden one, checked on the server); the replies form the thread of this message. | M | |
| THR-02 | Threads are one level deep: a reply within a thread belongs to the same thread, it does not start a new one. | M | decision |
| THR-03 | In the conversation, a message with a thread must show the number of replies, the time of the last reply and the participants (avatars/initials), without deleted or hidden replies; a click opens the thread. | M | |
| THR-04 | A thread must open in a side panel beside the conversation (full screen on small devices), with its own editor and attachments as in the conversation. | M | mobile first |
| THR-05 | The author of a message and everybody who replied follow its thread automatically; users must be able to follow and unfollow a thread. | M | NOT-02 |
| THR-06 | A reply could additionally be posted into the conversation ("also send to conversation"). | C | as in Google Chat |
| THR-07 | Moving a message or thread into another conversation is not planned. | W | |

### 5.4 PF-CHAT-ATT – Files

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| ATT-01 | Users must be able to attach files of any type to a message by pasting (e.g. a screenshot), drag & drop or the file dialog, at most 10 per message (configurable). They are stored when the message is sent, not before. | M | nothing unsent in the data transfer |
| ATT-02 | Files must be stored in the data transfer: in a synthetic area per conversation, for direct messages in the personal box of the recipient, for a data transfer area in the area itself (DT-03). The synthetic area appears in the data transfer list of the readers of the conversation as "Chat: …"; nobody uploads into it or administers it there. | M | decision 5 |
| ATT-03 | Access to a file of a synthetic area must be checked against the read check of the conversation (CTX-01), not against member lists of the data transfer; files of hidden messages for moderators only; deleting a file there is up to its author and the moderators. No external link or token. | M | `DataTransferAreaHost` |
| ATT-04 | Files expire as the area says: a room chooses how long (default 30 days), a personal box keeps them as it does; an expired file stays listed with the message as "no longer available". Deleting a message deletes its files, deleting a room its area. | M | clean-up job of the data transfer |
| ATT-05 | The size of a file is limited by the area (upload limit, free capacity). Images (PNG, JPEG, GIF, WebP) are recognised by content; all other files are always sent as download of unknown type (`attachment`, `nosniff`). | M | untrusted input |
| ATT-06 | Images must be shown as thumbnails in the message and opened full size in a viewer (lightbox) with download; other files as download with name and size. Each available file also links to its data transfer area (the area of the conversation, or the personal box for direct messages). | M | access of the data transfer, see ATT-03 |
| ATT-07 | Metadata (EXIF, GPS location) of images should be removed on upload. | S | privacy |
| ATT-08 | Without the data transfer plugin (or on the gateway) no files can be exchanged; the composer and the room dialog say so. | M | |

### 5.5 PF-CHAT-NOT – Unread messages and notifications

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| NOT-01 | The system must keep per user and conversation which messages are read (read state) and show a "new" line at the first unread message, in the conversation as in a thread; a conversation never read shows it at the first message of others. A message with unread replies of others shows a mark "new" until its thread is opened. The conversation is read up to its newest message (replies included) when shown at its end in a visible tab. Unread are the messages of others, not deleted or hidden; replies only in threads the user follows or is mentioned in. | M | one read state, decision 14 |
| NOT-02 | Users must be notified about new messages in conversations they take part in and threads they follow, via the existing notification system (in-app, mail, digest). | M | `business/notification/` |
| NOT-03 | Mentions must be notified at once and shown as a badge at the menu until read: a mention in the conversation by reading the conversation, a mention in a thread by opening the thread. | M | |
| NOT-04 | The host must be able to add recipients per message type (e.g. the security team for every new message of a vulnerability). | M | CTX-01 |
| NOT-05 | Notifications and mails must name only the conversation, its host title and the author; content goes to the mail only if the host allows it. | M | confidentiality (e.g. PF-CVR VIS-07) |
| NOT-06 | New messages, edits, deletions and reactions must appear to the other participants without a reload, while a chat panel or page is open: via server-sent events (Spring `SseEmitter`, event id = change sequence of the conversation) with a heartbeat (default every 25 s) so proxies keep the connection; on reconnect, missed events since the last event id are delivered. After repeated failures of the SSE connection the client falls back to polling automatically (about every 3 s while the tab is active, every 30 s in the background). Who has the conversation open by SSE counts as watching it and isn't notified about its new messages; a tab hidden for 30 s closes its connection (reopened and brought up to date when shown), so a forgotten tab doesn't swallow the notifications. | M | NFR-03, decision 6 |
| NOT-07 | Users should be able to mute a conversation (except mentions). | S | |
| NOT-08 | WebSockets and presence or typing indicators ("is typing …") are not planned; sending goes via ordinary POST requests. | W | decision 6 |
| NOT-09 | Users should be able to mark a message of others as unread (menu of the message): the read state goes back to the message before it, the "new" line moves there and the conversation isn't marked read again until the user writes or opens it anew. Notifications already resolved stay resolved. | S | decision 14 |
| NOT-10 | A conversation should show at its top right a badge with the messages unread when it was opened (NOT-01, replies included) not seen yet; a message counts as seen once shown, in the conversation or its thread. A click on the badge jumps to the next one: a reply opens its thread at it, an older message is loaded if needed. | S | |

### 5.6 PF-CHAT-SRCH – Search

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| SRCH-01 | Users must be able to search the messages of a conversation (incl. threads) by text and author; hits jump to the message. | M | |
| SRCH-02 | A search across all conversations of a user should be offered where the host allows indexing; hits are filtered by the read check of each conversation. | S | the host decides; confidential hosts may exclude it |

### 5.7 PF-CHAT-SEC – Access and security

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| SEC-01 | Read and write access must come from the chat context of the host only, checked on the server for every endpoint (`@AccessChecked`), including attachments, search, notifications and SSE. | M | CLAUDE.md rules |
| SEC-02 | Conversation, host object and author must be taken from the database and the logged-in user, never from the request; an edit or delete must not move a message to another conversation or author. | M | |
| SEC-03 | State changing calls (send, edit, delete, read state, follow) must use POST/PUT/DELETE, never GET. | M | CSRF |
| SEC-04 | Message HTML must be sanitized on the server before storing and rendered sanitized in the client (`RichText`); links open with `rel="noopener noreferrer"`. | M | "text stays text" |
| SEC-05 | Moderators must be able to hide a message of others with a reason; hiding and the reason are historised and visible to moderators. Others see neither its content nor its files or reactions. | M | |
| SEC-06 | A PF administrator must get no access to conversations through the admin role alone. | M | |
| SEC-07 | Every access rule comes with a test (refused for an ordinary user, allowed for the user it is meant for). | M | |
| SEC-08 | A conversation should be exportable (PDF or HTML incl. threads and images) by users with read access, for audits and evidence. | S | |

### 5.8 PF-CHAT-ROOM – Chat rooms

Chat rooms are the built-in first host: a room is a host object with one channel, its access comes from its
participants.

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| ROOM-01 | Every user with system access must be able to create a chat room with name, description and participants (users and/or groups); the creator becomes its owner. | M | |
| ROOM-02 | Participants of a room are its owner, its moderators, the users and the members of the groups listed; group membership is resolved at the time of access, so new group members take part at once. | M | `UserGroupCache` |
| ROOM-03 | Participants may read and write in the room; owner and moderators may edit and delete it and are its moderators in the sense of SEC-05. | M | |
| ROOM-04 | The owner of a room is taken from the database and the logged-in user; an edit must not change it. | M | SEC-02 |
| ROOM-05 | Users and groups can be chosen as participants only as far as the user may see them in the user and group selection. | M | no widening of visibility |
| ROOM-06 | A user must see a list of the rooms they take part in and open each one as conversation page (CTX-04). | M | minimal in R1 |
| ROOM-07 | Deleting a room deletes its conversation, messages and files (its data transfer area). | M | CTX-05 |
| ROOM-08 | A chat area should show the rooms as a side bar with unread counts per room, as in Google Chat, and offer the room management there. | S | R2 |
| ROOM-09 | The messages of rooms are not part of notification mails (NOT-05). | M | |
| ROOM-10 | The room dialog must offer how long the files of the room are kept (choices of the data transfer, default 30 days). | M | ATT-04 |

### 5.9 PF-CHAT-DM – Direct messages

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| DM-01 | Every user with system access must always be able to write privately to another user: from the chat page (choosing the user), from a user found in the menu search (button "Start chat / send file", Ctrl/Cmd+Enter; the users of the user selection, also for whom may not open the user pages, there the row starts the chat) and from every conversation (clicking an author). | M | decision 12 |
| DM-02 | A direct message can be started only with users the user may see in the user selection (as ROOM-05), not with oneself. | M | no widening of visibility |
| DM-03 | A pair of users has exactly one conversation, whoever starts it; each sees the name of the other one as title. | M | |
| DM-04 | Both read and write as long as both have system access, without checking the visibility again (whoever got a message may answer); without the system access of the other one it is read-only. Nobody moderates, administrators have no access. | M | |
| DM-05 | The chat page must list the direct messages with at least one message (newest first, unread counts); an opened, empty one isn't listed. | M | |
| DM-06 | New messages are notified as in rooms (in the app at once, by mail if not seen within an hour, once until read) with a text of their own, without the content. | M | NOT-05 |

### 5.10 PF-CHAT-DT – Data transfer areas

Every data transfer area is a chat room of its own: a host of the data transfer plugin (`DataTransferChatHost`), the
host object is the area.

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| DT-01 | Every ordinary data transfer area must have a chat, opened by a button "Chat" on the page of the area; no personal box, no synthetic area (e.g. of a chat conversation) and none on the gateway. | M | decision 15 |
| DT-02 | The users with access to the area inside PF read and write (admins, access users, members of the access groups, with system access); its admins moderate. External users of the area never see the chat (the public page of the area has nothing of it); in an area with external access, the chat says so: they see only the files with their descriptions. All of them are notified about new messages (NOT-02), the content isn't part of the mails (NOT-05). | M | `DataTransferAreaDao.hasInternalAccess` |
| DT-03 | Files of its messages go into the area itself (its expiry, upload limit and capacity), not into a synthetic area; no files if the external access of the area runs via the gateway (no upload into it in the main instance). Hiding a message hides it in the chat, not its file in the area (everybody with access to the area, external users too, still sees it); deleting the message deletes its file. | M | ATT-02, ATT-08 |
| DT-04 | Deleting the area deletes its conversation with all messages (CTX-05). | M | |
| DT-05 | The personal box of another user must offer a button "Direct message", which opens the direct message with its owner (PF-CHAT-DM, DM-02 applies); the files go into the owner's personal box (ATT-02). The own box has none. | M | |
| DT-06 | The chat page must list the chats of the areas with at least one message the user may read, newest first, with unread counts, narrowed by its search box. | M | as DM-05 |
| DT-07 | The buttons "Chat" and "Direct message" should show the number of unread messages of the conversation (NOT-01). | S | |

### 5.11 PF-CHAT-CONF – Confidential conversations

Modelled on an OTR session: for a password or a key that must not stay in the chat, the mail or the database.

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| CONF-01 | In a direct message, a user must be able to start a confidential conversation: each browser makes a key pair per session (WebCrypto, ECDH P-256, private key not extractable, in memory only); the session key is derived from both (ECDH → HKDF-SHA-256 → AES-GCM, the session id as info), every message is encrypted with a fresh IV and the session id, the sender's role and a counter as additional data. | M | decision 13 |
| CONF-02 | The server relays only the public keys and the ciphertext, in memory: nothing goes into the database, the log, the chat history or a notification. | M | single node, as the SSE |
| CONF-03 | The flow is offer with the initiator's key → accept with the recipient's key → both send and take messages. Each step only by its side and in its state; the recipient is the partner of the direct chat from the database. | M | SEC-02 |
| CONF-04 | A message is taken by the other side once and deleted with it; the sender sees that it was read. The counter of a side must rise (the server and the receiving browser drop a replayed or reordered message); at most 20 messages per direction wait untaken. | M | |
| CONF-05 | Either side can end the conversation, the other one learns it. It ends by itself after 10 minutes without a message, after 2 hours at the latest, and when a side isn't seen for 2 minutes (page closed or reloaded: its key is gone). | M | |
| CONF-06 | Both sides must see a fingerprint of both public keys; the dialog says what this protects against (a server reading or forging texts, if the sides compare it) and what not (the screen or clipboard of a side). | M | |
| CONF-07 | The open chat of the other side is woken by a contentless event; a recipient without the chat open gets an in-app banner naming only the initiator (no mail, it would come too late), resolved once no offer is open anymore. | M | NOT-05 |
| CONF-08 | Limits: at most 5 open sessions per initiator, 1,000 overall, 16 KB ciphertext per message. | M | |
| CONF-09 | Confidential conversations in rooms, with several participants, with files, or kept for later reading are not planned. | W | |

### 5.12 Non-functional requirements

| ID | Requirement | Prio |
|---|---|---|
| NFR-01 | UI bilingual DE/EN, German texts without the formal "Sie". | M |
| NFR-02 | Frontend in `projectforge-next`, mobile first (conversation and thread usable on a phone). | M |
| NFR-03 | A new message is visible to other online participants within 3 s. | M |
| NFR-04 | A conversation with 10,000 messages opens in < 2 s and scrolls fluently. | S |
| NFR-05 | Module in the core (`org.projectforge.business.chat` / `org.projectforge.rest.chat`), not a plugin, so core entities such as invoices and orders can embed it; plugins embed it the same way. | M |
| NFR-06 | Database changes via Flyway, versioned by the release that ships them. | M |
| NFR-07 | Operation: the SSE path must not be buffered by a reverse proxy and its read timeout must exceed the heartbeat (nginx e.g. `proxy_buffering off; proxy_read_timeout 120s;`); the admin documentation says so. | M |

## 6. Reuse of existing building blocks (for the implementation)

| Need | Building block |
|---|---|
| Rich text editing and rendering | `components/shared/rich-text-editor.tsx`, `components/shared/rich-text.tsx` (sanitized) |
| File storage | `AttachmentsService` / JCR, file storage of `org.projectforge.plugins.datatransfer` |
| Live updates | Spring `SseEmitter` (`SseEmitterTool` polls and has no event ids, so not reused) |
| Notifications, digest, badge | `business/notification/` (`NotificationDO`, rule handler, in-app/mail senders) |
| History of edits | `DefaultBaseDO` history, `HistoryUserCommentSupport` |
| Users | `PFUserDO`, `UserGroupCache` for mentions and room participants |
| User and group selection | `user/autosearch`, `group/autosearch`, `EntityMultiAutocomplete` |

To be built: chat context interface and registry, conversation/message/read-state entities, chat rooms, chat panel and
page, thread side panel, mention suggestions, file upload and image viewer.

## 7. Integration examples

| Host | Channels | Read / write | Remark |
|---|---|---|---|
| Chat room | *main* | participants of the room (ROOM-02) | first host, built in |
| Direct message | *main* | the two users (DM-04) | built in |
| Data transfer area | *main* | the users with access to the area inside PF (DT-02) | data transfer plugin, files in the area |
| CVE-Radar vulnerability | *internal*, *public* | internal: ST and affected groups; public: recipients of the client advisory | [PF-CVR-DISC](PF-CVR_CVE-Radar_Vulnerability_Management.md#511-pf-cvr-disc--discussion) |
| CVE-Radar finding | *internal* | visibility of the finding (PF-CVR VIS-09) | system-specific discussion |
| Invoice (later) | *internal* | as the invoice (`RechnungDao`) | questions about an invoice |
| Order (later) | *internal* | as the order (`AuftragDao`) | discussion about an order |

## 8. Decisions (as of 0.6)

| # | Topic | Decision | Requirements |
|---|---|---|---|
| 1 | Module | Generic core module embedded by hosts; no access rules of its own | CTX-01, NFR-05 |
| 2 | Scope | Conversations always belong to a host object; free discussions take place in chat rooms, private ones in direct messages | CTX-07 |
| 3 | Threads | As in Google Chat: replies to a message, one level deep | THR-01, THR-02 |
| 4 | Editing | Existing rich text editor, own messages editable and historised | MSG-01, MSG-03 |
| 5 | Files | All types, in the data transfer: a synthetic area per conversation (access from the conversation, expiry chosen by the room), the personal box of the recipient for direct messages; none without the plugin | ATT-01 – ATT-08 |
| 6 | Live updates | SSE with heartbeat, automatic fallback to polling; no WebSocket | NOT-06, NOT-08, NFR-07 |
| 7 | Reactions | Emoji reactions as in Google Chat | MSG-10 |
| 8 | Retention | Kept until the host object is deleted; a cleanup job follows later | CTX-05, CTX-08 |
| 9 | Chat rooms | Built-in first host with participants (users and groups); the chat area follows in R2 | ROOM-01 – ROOM-09 |
| 10 | Mentions | `@` with suggestions from the readers of the conversation; checked on the server; notified at once | MSG-07, NOT-03 |
| 11 | Reactions storage | Stored with the message (emoji → user ids), not historised | MSG-10 |
| 12 | Direct messages | Built-in host with the pair of users as host object; started only with visible users, then open to both | DM-01 – DM-06 |
| 13 | Confidential conversations | A live session of the two browsers (like OTR), end-to-end encrypted, relayed in memory and deleted when taken, until one side ends it; direct messages only | CONF-01 – CONF-09 |
| 14 | Read state | One mark per user and conversation (the newest message read), for the conversation and its threads; replies count as unread only in threads followed or with a mention; marking unread moves it back; the menu badges come from the notifications and aren't restored by marking unread | NOT-01, NOT-03, NOT-09 |
| 15 | Data transfer areas | Every ordinary area is a chat room of the users with access, its files go into the area; the personal box of another user leads to the direct message with its owner | DT-01 – DT-07 |

## 9. Implementation status (review of R1, 2026-10-26)

Implemented in R1: CTX-01 – CTX-05, CTX-07, MSG-01 – MSG-05, MSG-07, MSG-09, MSG-10, THR-01 – THR-05, ATT-01 – ATT-08,
NOT-01 – NOT-07, NOT-09, NOT-10, SEC-01 – SEC-07, ROOM-01 – ROOM-07, ROOM-09, ROOM-10, DM-01 – DM-06, CONF-01 – CONF-08,
DT-01 – DT-07.

Deviations and gaps:

| Requirement | State |
|---|---|
| MSG-06, NFR-04 | Paging by 50 and loading older messages is implemented, the list is not virtualised; 10,000 messages are not measured yet. |
| SRCH-01 | Search by text in the UI; the author filter exists only in the REST endpoint (`authorId`), not in the search dialog. Hidden messages aren't found, also not by moderators. |
| ATT-07 | Metadata removed for JPEG and PNG (encoded again); GIF and WebP are kept as they are (animations). |
| CTX-06 | Unread counts in the lists of the chat page (rooms, direct messages, data transfer areas) and at the chat buttons of a data transfer area (DT-07); not in the lists of a host (e.g. the data transfer list). |
| SRCH-02 | The search box of the chat page also searches the messages of all conversations (at least three characters, the newest 1,000 conversations with a match are checked, `GET /rs/chat/search`): one entry per conversation with the number of matches and the newest match as preview, filtered by the read check of each conversation; a host excludes its conversations by `ChatHost.searchableAcrossConversations` (all hosts take part so far). A match older than the first page of the conversation opens it without jumping. |
| MSG-08, THR-06, CTX-08, ROOM-08, SEC-08 | Open (S/C, R2 or later). |

## 10. Open points

None at the moment. The findings of the review of R1 are fixed:
- a chat open in a background tab no longer suppresses the notifications (NOT-06);
- replies count as unread only in threads followed or with a mention, mentions in threads are resolved by opening
  the thread (NOT-01, NOT-03, decision 14);
- a conversation never read shows the "new" line (NOT-01);
- no replies to deleted or hidden messages (THR-01); no reactions or reply authors of hidden messages for others,
  hidden messages don't count as unread (SEC-05, THR-03); deleting the last reply updates the time of the last
  reply; marking unread waits for a read state still on its way.

## 11. Next steps

1. Release cut R1 (ProjectForge 9.0.16): chat core (CTX, MSG incl. mentions and reactions, THR, ATT, NOT-01 – NOT-07,
   NOT-09, SRCH-01, SEC) with the chat room as first host and a minimal room list (ROOM-01 – ROOM-07, ROOM-09,
   ROOM-10), direct messages (DM), confidential conversations (CONF).
2. R2: chat area (ROOM-08), search across conversations (SRCH-02), author filter in the search (SRCH-01), virtualised
   list (MSG-06, NFR-04), quoting (MSG-08), export (SEC-08), user guide.
3. PF-CVR DISC on top of R1; the cleanup job (CTX-08) later.
