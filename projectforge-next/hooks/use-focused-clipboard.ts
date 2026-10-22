"use client";

import { useEffect, useRef, type RefObject } from "react";

export interface FocusedClipboardHandlers {
  onCopy: (data: DataTransfer) => void;
  onPaste: (data: DataTransfer) => void;
  /** Cutting: copying what [onCopy] would, then taking it away. */
  onCut?: (data: DataTransfer) => void;
}

/**
 * Copy, cut and paste for a control that is no text field — a box of chips — while the focus is in it.
 *
 * Listened for on the document rather than on the element: with nothing editable focused, the browser
 * sends the clipboard events to the body, not to the focused button. The event's own
 * `clipboardData` is used, not `navigator.clipboard`: it asks for no permission and carries a MIME type
 * of our own, which is what tells a copied list of references from any text.
 */
export function useFocusedClipboard(
  ref: RefObject<HTMLElement | null>,
  handlers: FocusedClipboardHandlers
): void {
  // The latest handlers, so the listeners are added once and still see the current value.
  const latest = useRef(handlers);
  useEffect(() => {
    latest.current = handlers;
  });

  useEffect(() => {
    const focusedHere = () =>
      ref.current != null && ref.current.contains(document.activeElement);
    const copy = (event: ClipboardEvent) => {
      if (!focusedHere() || !event.clipboardData) return;
      event.preventDefault();
      latest.current.onCopy(event.clipboardData);
    };
    const paste = (event: ClipboardEvent) => {
      if (!focusedHere() || !event.clipboardData) return;
      event.preventDefault();
      latest.current.onPaste(event.clipboardData);
    };
    const cut = (event: ClipboardEvent) => {
      const onCut = latest.current.onCut;
      if (!onCut || !focusedHere() || !event.clipboardData) return;
      event.preventDefault();
      onCut(event.clipboardData);
    };
    document.addEventListener("copy", copy);
    document.addEventListener("cut", cut);
    document.addEventListener("paste", paste);
    return () => {
      document.removeEventListener("copy", copy);
      document.removeEventListener("cut", cut);
      document.removeEventListener("paste", paste);
    };
  }, [ref]);
}
