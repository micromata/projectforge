"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { ChatConversationPage } from "@/components/features/chat/chat-conversation-page";

/** `/chat/{id}`: the conversation with the id. */
export function ChatConversationPageClient() {
  const raw = useRouteParams<{ id: string }>("/chat/[id]")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return <ChatConversationPage key={id} conversationId={id} />;
}
