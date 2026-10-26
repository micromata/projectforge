"use client";

import { useId } from "react";
import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";
import type { PollAnswerData } from "./types";
import { RefName } from "@/components/shared/ref-list";

/** Whose answers are shown: the logged-in user's, or those of an attendee (full access only). */
export function PollOwnerSelect({
  data,
  onOwnerChange,
}: {
  data: PollAnswerData;
  onOwnerChange: (ownerId: number | null) => void;
}) {
  const t = useTranslations();
  const id = useId();
  const me = useCurrentUserRef();
  return (
    <div className="flex items-center gap-2">
      <Label htmlFor={id} className="text-sm">
        {t("poll.answer.for")}
      </Label>
      <Select
        value={String(data.answerOwner.id)}
        onValueChange={(value) =>
          onOwnerChange(Number(value) === me?.id ? null : Number(value))
        }
      >
        <SelectTrigger id={id} size="sm" className="w-auto min-w-48">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {/* The logged-in user may not be an attendee, but answers for themselves anyway. */}
          {me && !data.attendees?.some((a) => a.id === me.id) && (
            <SelectItem value={String(me.id)}>{me.displayName}</SelectItem>
          )}
          {data.attendees?.map((attendee) => (
            <SelectItem key={attendee.id} value={String(attendee.id)}>
              <RefName value={attendee} />
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
