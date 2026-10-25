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

/** An image of the message being written: uploading, uploaded (fileId) or failed. */
export interface PendingImage {
  key: string;
  name: string;
  /** Object url of the local file, for the preview. */
  previewUrl: string;
  fileId: string | null;
  error: string | null;
}
