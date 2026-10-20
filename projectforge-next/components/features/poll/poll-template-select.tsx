"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { fetchPollTemplateQuestions, fetchPollTemplates } from "@/lib/rs/poll";
import { toast } from "@/lib/toast";
import type { QuestionRow } from "./types";

/** Takes the questions of a template into the poll (appended, with new uids). */
export function PollTemplateSelect({
  onQuestions,
}: {
  onQuestions: (questions: QuestionRow[]) => void;
}) {
  const t = useTranslations();
  // Remounts the select after each choice, so the same template can be taken twice.
  const [round, setRound] = useState(0);
  const templates = useQuery({
    queryKey: ["poll", "templates"],
    queryFn: ({ signal }) => fetchPollTemplates(signal),
  });
  const list = templates.data ?? [];
  if (list.length === 0) return null;
  return (
    <Select
      key={round}
      onValueChange={(value) => {
        const template = list[Number(value)];
        setRound((r) => r + 1);
        if (!template) return;
        fetchPollTemplateQuestions(template)
          .then((questions) => {
            onQuestions(questions);
            toast.success(t("poll.questionTemplate.taken"));
          })
          .catch((error: unknown) =>
            toast.error(error instanceof Error ? error.message : String(error))
          );
      }}
    >
      <SelectTrigger
        size="sm"
        className="w-auto"
        aria-label={t("poll.questionTemplate._")}
      >
        <SelectValue placeholder={t("poll.questionTemplate._")} />
      </SelectTrigger>
      <SelectContent>
        {list.map((template, index) => (
          <SelectItem key={index} value={String(index)}>
            {template.title}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
