import type { ChatRef } from "@/lib/rs/chat-types";

/** The open suggestion list of a mention (`@…`) in the chat editor. */
export interface MentionSuggestionState {
  items: ChatRef[];
  /** The highlighted item, chosen by Enter. */
  index: number;
  /** Where the `@…` is, to place the list at. */
  rect: DOMRect | null;
  /** Inserts the chosen user as mention. */
  command: (item: { id: string; label: string }) => void;
}

/** What the chat editor offers its parent (via ref). */
export interface ChatEditorHandle {
  clear: () => void;
  focus: () => void;
  isEmpty: () => boolean;
  getHtml: () => string;
}

/** A file of the message being written, sent with it (uploaded only then). */
export interface PendingFile {
  key: string;
  file: File;
  /** Object url of a local image, for the preview; null for other files. */
  previewUrl: string | null;
}
