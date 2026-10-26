/** A user or group as shown by the chat (the pickers' EntityRef). */
export interface ChatRef {
  id: number;
  displayName: string;
}

/** A file of a message, stored in the data transfer (ChatFileService). */
export interface ChatFile {
  /** The data transfer area of the file: the conversation's area, for direct messages the personal box. */
  areaId: number;
  fileId: string;
  name: string;
  size: number;
  sizeFormatted: string;
  /** A PNG, JPEG, GIF or WebP image (checked by its content): shown as thumbnail. */
  image: boolean;
  /** False if it expired or was deleted in the data transfer. */
  available: boolean;
}

export interface ChatReaction {
  emoji: string;
  count: number;
  users: string[];
  mine: boolean;
}

/**
 * A message as the logged-in user sees it (ChatRest/ChatMessageConverter): no content of a deleted message,
 * of a hidden one only for moderators.
 */
export interface ChatMessage {
  id: number;
  conversationId: number;
  threadRootId: number | null;
  author: ChatRef;
  created: string | null;
  editedAt: string | null;
  deleted: boolean;
  hidden: boolean;
  hiddenBy: string | null;
  hiddenReason: string | null;
  content: string | null;
  files: ChatFile[];
  reactions: ChatReaction[];
  replyCount: number;
  lastReplyAt: string | null;
  replyAuthors: ChatRef[];
  /** The newest reply of others in the thread, for marking it as having unread replies. */
  newestReplyOfOthersId: number | null;
  following: boolean;
  own: boolean;
  mentionsMe: boolean;
  changeSeq: number;
}

export interface ChatConversation {
  id: number;
  hostType: string;
  hostId: number;
  channel: string;
  title: string;
  /** Path of the next app of the host object, e.g. `/chat`. */
  link: string | null;
  canWrite: boolean;
  isModerator: boolean;
  muted: boolean;
  lastReadMessageId: number | null;
  newestMessageId: number | null;
  changeSeq: number;
  me: ChatRef;
  /** The other one of a direct message, null for other conversations. */
  directPartner: ChatRef | null;
  /** Can files be attached (data transfer plugin active)? */
  filesEnabled: boolean;
  maxFilesPerMessage: number;
  /** The largest file the conversation takes now (bytes), null if unknown: the server checks it anyway. */
  maxFileSize: number | null;
  maxFileSizeFormatted: string | null;
  /** The largest request (bytes): all files of a message together. */
  maxRequestSize: number;
}

export interface ChatMessagesPage {
  messages: ChatMessage[];
  hasMore: boolean;
  changeSeq: number;
}

export interface ChatThread {
  root: ChatMessage;
  replies: ChatMessage[];
  changeSeq: number;
}

export interface ChatChanges {
  messages: ChatMessage[];
  changeSeq: number;
  complete: boolean;
}

export interface ChatRoomEntry {
  id: number;
  name: string;
  description: string | null;
  conversationId: number | null;
  participantCount: number;
  unread: number;
  lastMessageAt: string | null;
  canEdit: boolean;
}

export interface ChatRoom {
  id: number | null;
  name: string;
  description: string | null;
  owner?: ChatRef | null;
  moderators: ChatRef[];
  users: ChatRef[];
  groups: ChatRef[];
  canEdit?: boolean;
  /** The days the files of the room are kept in its data transfer area. */
  fileExpiryDays?: number | null;
  fileExpiryDaysChoices?: ChatChoice[];
  filesEnabled?: boolean;
}

/** A choice of a select with its translated label. */
export interface ChatChoice {
  value: number;
  label: string;
}

/** Defaults of a new room (ChatRoomRest.defaults). */
export interface ChatRoomDefaults {
  fileExpiryDays: number | null;
  fileExpiryDaysChoices: ChatChoice[];
  filesEnabled: boolean;
}

/** A direct message in the list of the user's ones. */
export interface ChatDirectEntry {
  conversationId: number;
  partner: ChatRef;
  unread: number;
  lastMessageAt: string | null;
  /** False if the other one has no system access any more (read-only). */
  canWrite: boolean;
}
