/** A user or group as shown by the chat (the pickers' EntityRef). */
export interface ChatRef {
  id: number;
  displayName: string;
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
  imageIds: string[];
  reactions: ChatReaction[];
  replyCount: number;
  lastReplyAt: string | null;
  replyAuthors: ChatRef[];
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
  maxImagesPerMessage: number;
  maxImageSize: number;
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
}
