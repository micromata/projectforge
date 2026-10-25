# PF-CHAT Requirements - Chat (discussion about any object)

| Field | Value |
|---|---|
| COMP-ID | PF-CHAT |
| DOC-ID | PF-CHAT-REQ |
| Status | Draft 0.2 (open points decided) |
| Owner | Kai Reinhard |
| Date | 2026-10-24 |
| Stage | Specification only (release cut follows) |
| Used by | [PF-CVR – CVE-Radar](PF-CVR_CVE-Radar_Vulnerability_Management.md) (first host); later invoices, orders and others |

---

## 1. Context and objective

Questions and discussions about an object in ProjectForge, such as a vulnerability, an invoice or an order, happen
today in mails and messengers outside PF. They are not found later, are not visible to whoever takes over, and
do not follow the access rules of the object.

**Objective:** a generic chat module, modelled on Google Chat, that any module can embed for its objects:
- conversations about an object, with replies to a single message as a thread of their own;
- messages written and edited with the rich text editor of PF, with images attached;
- access taken entirely from the object the conversation belongs to.

The chat is a module of its own: it knows nothing about vulnerabilities or invoices. The embedding module (the
*host*) tells it who may read and write. The first host is the CVE-Radar.

**Measurable benefit (targets):**
- Every discussion about an embedded object is found at the object, complete with images and threads.
- A new message is visible to the other participants within 3 s, without a reload.
- Embedding the chat into a new host needs no change of the chat module.

## 2. Stakeholders and roles

| Role | Description |
|---|---|
| Participant | A user allowed by the host to read a conversation; may write if the host allows writing. |
| Moderator | Defined by the host (e.g. the security team for vulnerabilities); may hide messages of others. |
| Host developer | Integrates the chat into a module by implementing the chat context. |
| PF administrator | Gets no access to conversations by his role alone; access comes from the host only. |

## 3. System context

```
 Host module (CVE-Radar, invoices, orders …)
   └ chat context: title, link, channels, read/write check, moderators, participants, retention
        │
        ▼
 ┌───────────────────────────┐ ── images ──▶ file storage of the data transfer module (JCR, AttachmentsService)
 │ PF-CHAT                   │ ── notify ──▶ notification system (in-app, mail, digest)
 │ conversations, messages,  │ ── live ────▶ browser via SSE (SseEmitterTool), polling as fallback
 │ threads, read states      │
 └───────────────────────────┘
        ▲
 projectforge-next: chat panel (embedded at the host object) and chat page per conversation
```

**Scope boundaries:**
- Conversations always belong to a host object. Free rooms, team rooms and direct messages are not part of this
  version (see CTX-07).
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
| Message | A post with author, time and rich text, optionally with images. |
| Thread | The replies to one message, shown apart from the conversation; one level deep. |
| Mention | `@user` in a message; notifies the user. |
| Attachment | An image stored in the data transfer storage and shown with the message. |

## 5. Requirements

Priority by MoSCoW: **M** = Must, **S** = Should, **C** = Could, **W** = Won't (this time).

### 5.1 PF-CHAT-CTX – Embedding into a host

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| CTX-01 | A host must be able to register a chat context for its object type, providing per host object: title, deep link, channels, read check, write check, moderators and the users to notify. | M | adapter interface, no change of the chat module per host |
| CTX-02 | The system must keep exactly one conversation per host object and channel and create it on the first message; an unknown host object must be refused. | M | |
| CTX-03 | The frontend must offer a chat panel component that a host page embeds (e.g. as a tab or side panel of the host object). | M | shared component in `projectforge-next` |
| CTX-04 | Every conversation must also be reachable as a page of its own (`/next/chat/{id}`) showing the title of the host object with a link back to it. | M | links in notifications |
| CTX-05 | A conversation must follow the life cycle of its host object: read-only when the host says so (e.g. closed vulnerability), archived and deleted together with it. | M | |
| CTX-06 | A host should be able to show the number of conversations and unread messages per object in its lists (e.g. a chat column in the invoice list). | S | |
| CTX-07 | Free rooms, team rooms and direct messages without a host object are not planned. | W | decision |
| CTX-08 | A cleanup job should remove conversations and images whose host object no longer exists (e.g. orphaned files in the data transfer storage) and apply the retention rule of a host. | S | later; CTX-05 stays the rule |

### 5.2 PF-CHAT-MSG – Messages

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| MSG-01 | Messages must be written in the existing rich text editor (`RichTextEditor`) and stored as sanitized HTML. | M | same editor as elsewhere in PF |
| MSG-02 | Every message must show author, time and, after a change, the mark "edited" with the time of the last change. | M | |
| MSG-03 | Authors must be able to edit their own messages with the rich text editor; every version is historised and visible to moderators. | M | `DefaultBaseDO` history |
| MSG-04 | Authors must be able to delete their own messages; a deleted message leaves the placeholder "Message deleted" so threads stay readable. | M | soft delete |
| MSG-05 | Every message must have a deep link that opens the conversation (or thread) at this message; the link can be copied. | M | |
| MSG-06 | A conversation must open with the newest 50 messages and load older ones when scrolling up; long conversations stay fluent (virtualised list). | M | |
| MSG-07 | Messages should be able to mention users (`@`, suggestions only from users with read access to the conversation); the mentioned user is notified. | S | NOT-03 |
| MSG-08 | A message should be able to quote another message of the same conversation (with link to it). | S | |
| MSG-09 | An unsent draft should be kept per user and conversation. | S | |
| MSG-10 | Users should be able to react to messages and thread replies with emoji, as in Google Chat: quick choice of recently used emoji, per emoji the count and (on hover) who reacted; a second click takes the own reaction back. Reactions are not notified. | S | decision 7 |

### 5.3 PF-CHAT-THR – Threads

Modelled on Google Chat: the conversation shows the messages; a reply to a single message starts a thread of its
own instead of interleaving with the conversation.

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| THR-01 | Users must be able to reply to any message of a conversation; the replies form the thread of this message. | M | |
| THR-02 | Threads are one level deep: a reply within a thread belongs to the same thread, it does not start a new one. | M | decision |
| THR-03 | In the conversation, a message with a thread must show the number of replies, the time of the last reply and the participants (avatars/initials); a click opens the thread. | M | |
| THR-04 | A thread must open in a side panel beside the conversation (full screen on small devices), with its own editor and attachments as in the conversation. | M | mobile first |
| THR-05 | The author of a message and everybody who replied follow its thread automatically; users must be able to follow and unfollow a thread. | M | NOT-02 |
| THR-06 | A reply could additionally be posted into the conversation ("also send to conversation"). | C | as in Google Chat |
| THR-07 | Moving a message or thread into another conversation is not planned. | W | |

### 5.4 PF-CHAT-ATT – Images

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| ATT-01 | Users must be able to attach images to a message by pasting (e.g. a screenshot), drag & drop or the file dialog, several per message. | M | |
| ATT-02 | Images must be stored in the file storage of the data transfer module (JCR via `AttachmentsService`), in a technical area per conversation that does not appear in the data transfer lists. | M | reuse, no second file store |
| ATT-03 | Access to an image must be checked against the read check of the conversation (CTX-01), not against the member lists of the data transfer area; images must not be reachable by an external link or token. | M | |
| ATT-04 | Images must not expire as data transfer files do; they are kept and deleted with their message and conversation (CTX-05). | M | |
| ATT-05 | Only PNG, JPEG, GIF and WebP must be accepted, checked by content rather than by file name; size and number per message are configurable (default 10 MB, 10 images). | M | untrusted input |
| ATT-06 | Images must be shown as thumbnails in the message and opened full size in a viewer (lightbox) with download. | M | |
| ATT-07 | Metadata (EXIF, GPS location) should be removed on upload. | S | privacy |
| ATT-08 | Other file types (PDF, office documents) could be attached later under the same rules. | C | |

### 5.5 PF-CHAT-NOT – Unread messages and notifications

| ID | Requirement | Prio | Remark |
|---|---|---|---|
| NOT-01 | The system must keep per user and conversation which messages are read and show a "new" line at the first unread message. | M | |
| NOT-02 | Users must be notified about new messages in conversations they take part in and threads they follow, via the existing notification system (in-app, mail, digest). | M | `business/notification/` |
| NOT-03 | Mentions must be notified at once and shown as a badge at the menu until read. | M | |
| NOT-04 | The host must be able to add recipients per message type (e.g. the security team for every new message of a vulnerability). | M | CTX-01 |
| NOT-05 | Notifications and mails must name only the conversation, its host title and the author; content goes to the mail only if the host allows it. | M | confidentiality (e.g. PF-CVR VIS-07) |
| NOT-06 | New messages, edits, deletions and reactions must appear to the other participants without a reload, while a chat panel or page is open: via server-sent events (`SseEmitterTool`) with a heartbeat (default every 25 s) so proxies keep the connection; on reconnect, missed events since the last event id are delivered. After repeated failures of the SSE connection the client falls back to polling automatically (about every 3 s while the tab is active, every 30 s in the background). | M | NFR-03, decision 6 |
| NOT-07 | Users should be able to mute a conversation (except mentions). | S | |
| NOT-08 | WebSockets and presence or typing indicators ("is typing …") are not planned; sending goes via ordinary POST requests. | W | decision 6 |

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
| SEC-05 | Moderators must be able to hide a message of others with a reason; hiding and the reason are historised and visible to moderators. | M | |
| SEC-06 | A PF administrator must get no access to conversations through the admin role alone. | M | |
| SEC-07 | Every access rule comes with a test (refused for an ordinary user, allowed for the user it is meant for). | M | |
| SEC-08 | A conversation should be exportable (PDF or HTML incl. threads and images) by users with read access, for audits and evidence. | S | |

### 5.8 Non-functional requirements

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
| Live updates | `projectforge-rest/.../core/SseEmitterTool.kt` |
| Notifications, digest, badge | `business/notification/` (`NotificationDO`, rule handler, in-app/mail senders) |
| History of edits | `DefaultBaseDO` history, `HistoryUserCommentSupport` |
| Users | `PFUserDO`, user cache for mentions |

To be built: chat context interface and registry, conversation/message/read-state entities, chat panel and page,
thread side panel, image upload and viewer.

## 7. Integration examples

| Host | Channels | Read / write | Remark |
|---|---|---|---|
| CVE-Radar vulnerability | *internal*, *public* | internal: ST and affected groups; public: recipients of the client advisory | [PF-CVR-DISC](PF-CVR_CVE-Radar_Vulnerability_Management.md#511-pf-cvr-disc--discussion) |
| CVE-Radar finding | *internal* | visibility of the finding (PF-CVR VIS-09) | system-specific discussion |
| Invoice (later) | *internal* | as the invoice (`RechnungDao`) | questions about an invoice |
| Order (later) | *internal* | as the order (`AuftragDao`) | discussion about an order |

## 8. Decisions (as of 0.2)

| # | Topic | Decision | Requirements |
|---|---|---|---|
| 1 | Module | Generic core module embedded by hosts; no access rules of its own | CTX-01, NFR-05 |
| 2 | Scope | Conversations always belong to a host object; no free rooms or direct messages | CTX-07 |
| 3 | Threads | As in Google Chat: replies to a message, one level deep | THR-01, THR-02 |
| 4 | Editing | Existing rich text editor, own messages editable and historised | MSG-01, MSG-03 |
| 5 | Images | Data transfer storage, access from the conversation, no expiry | ATT-02 – ATT-04 |
| 6 | Live updates | SSE with heartbeat, automatic fallback to polling; no WebSocket | NOT-06, NOT-08, NFR-07 |
| 7 | Reactions | Emoji reactions as in Google Chat | MSG-10 |
| 8 | Retention | Kept until the host object is deleted; a cleanup job follows later | CTX-05, CTX-08 |

## 9. Open points

None at the moment.

## 10. Next steps

1. Review of this version 0.2 together with PF-CVR 0.7.
2. Release cut: PF-CHAT R1 (CTX, MSG incl. reactions, THR, ATT, NOT-01 – NOT-06, SEC) before PF-CVR DISC; the cleanup job (CTX-08) later.
