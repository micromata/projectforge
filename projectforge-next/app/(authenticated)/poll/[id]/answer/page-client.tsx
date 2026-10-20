"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PollAnswerPage } from "@/components/features/poll/poll-answer-page";

export function PollAnswerPageClient() {
  const raw = useRouteParams<{ id: string }>("/poll/[id]/answer")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();
  return <PollAnswerPage pollId={id} />;
}
