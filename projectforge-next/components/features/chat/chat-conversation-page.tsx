"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { chatConversationQueryKey, fetchChatConversation } from "@/lib/rs/chat";
import { ChatPanel } from "./chat-panel";

/**
 * A conversation as a page of its own (`/next/chat/{id}`, CTX-04): the title of its host object with the way
 * back to it, the conversation full size. `?message={id}` points at a message (MSG-05).
 */
export function ChatConversationPage({
  conversationId,
}: {
  conversationId: number;
}) {
  const t = useTranslations();
  const searchParams = useSearchParams();
  const highlight = Number(searchParams.get("message"));
  const query = useQuery({
    queryKey: chatConversationQueryKey(conversationId),
    queryFn: ({ signal }) => fetchChatConversation(conversationId, signal),
    refetchOnWindowFocus: false,
  });
  return (
    <PageShell>
      <PageTitleRow
        category={t("chat.title")}
        title={query.data?.title ?? t("chat.title")}
      >
        <Button asChild variant="outline">
          <Link href={query.data?.link ?? "/chat"}>
            <HugeiconsIcon icon={ArrowLeft01Icon} data-icon="inline-start" />
            {t("chat.backToHost")}
          </Link>
        </Button>
      </PageTitleRow>
      <div className="flex min-h-0 flex-1 flex-col">
        <ChatPanel
          conversationId={conversationId}
          highlightId={
            Number.isInteger(highlight) && highlight > 0 ? highlight : null
          }
        />
      </div>
    </PageShell>
  );
}
