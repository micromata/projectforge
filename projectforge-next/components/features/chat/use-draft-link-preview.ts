"use client";

import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { previewChatLink } from "@/lib/rs/chat";

/** A link in the html being written, only to know when to ask the server (which decides on the link itself). */
const LINK_REGEX = /(?:https?:\/\/|www\.)[^\s<>"']+/i;

/**
 * The preview of the link of the message being written, shown above the editor as in other chats: asked for when a
 * link is typed (debounced), fetched by the server, which keeps it for sending. Removed by the author, the message is
 * sent without a preview ([linkPreview] false).
 */
export function useDraftLinkPreview(conversationId: number, html: string) {
  const candidate = html.match(LINK_REGEX)?.[0] ?? null;
  const link = useDebouncedValue(candidate, 500);
  // The query is keyed by the link, the text around it may change meanwhile: asked with the text as it is then.
  const htmlRef = useRef(html);
  useEffect(() => {
    htmlRef.current = html;
  }, [html]);
  const [dismissedUrl, setDismissedUrl] = useState<string | null>(null);
  const query = useQuery({
    queryKey: ["chatLinkPreview", conversationId, link],
    queryFn: async ({ signal }) =>
      (await previewChatLink(conversationId, htmlRef.current, signal)) ?? null,
    enabled: link != null,
    staleTime: 10 * 60_000,
    retry: false,
  });
  const data = candidate != null ? (query.data ?? null) : null;
  const dismissed = data != null && data.url === dismissedUrl;
  return {
    preview: dismissed ? null : data,
    dismiss: () => setDismissedUrl(data?.url ?? null),
    /** False if the author removed the preview. */
    linkPreview: !dismissed,
    reset: () => setDismissedUrl(null),
  };
}
