"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { SectionCard } from "@/components/shared/section-card";
import { toast } from "@/lib/toast";
import { RsError } from "@/lib/rs/client";
import { sendFeedback } from "@/lib/rs/feedback";
import type { FeedbackInitialData } from "./types";

const MAX_DESCRIPTION_LENGTH = 4000;

/**
 * The feedback form, seeded once from the server's [initial] data (the read-only receiver and sender).
 * Plain `useState`, like the other standalone forms of this app (the SMS page, the task wizard): a single
 * free-text field, no entity and no client-side rule beyond "a text is given" — the rest is the backend's.
 */
export function FeedbackForm({ initial }: { initial: FeedbackInitialData }) {
  const t = useTranslations();
  const router = useRouter();
  const [description, setDescription] = useState("");

  const send = useMutation({
    mutationFn: () => sendFeedback({ description }),
    onSuccess: (result) => {
      if (result.success) {
        // Stay on the page and clear the field (like the SMS page): a redirect after send is where the
        // success toast got lost — the feedback page is usually reached by a full navigation, so
        // router.back() left the app onto a 404 and router.push carries the risk of unmounting the toast.
        toast.success(result.message);
        setDescription("");
      } else {
        toast.error(result.message);
      }
    },
    onError: (error) =>
      toast.error(error instanceof RsError ? error.message : String(error)),
  });

  const canSend = description.trim().length > 0 && !send.isPending;

  return (
    <SectionCard className="w-full">
      <FieldGroup>
        <Field>
          <FieldLabel>{t("feedback.receiver")}</FieldLabel>
          <p className="text-sm text-muted-foreground">{initial.receiver}</p>
        </Field>
        <Field>
          <FieldLabel>{t("feedback.sender")}</FieldLabel>
          <p className="text-sm text-muted-foreground">{initial.sender}</p>
        </Field>
        <Field>
          <FieldLabel htmlFor="feedback-description">
            {t("description")}
          </FieldLabel>
          <Textarea
            id="feedback-description"
            value={description}
            maxLength={MAX_DESCRIPTION_LENGTH}
            rows={8}
            onChange={(event) => setDescription(event.target.value)}
            autoFocus
          />
        </Field>
        <div className="flex items-center gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push("/")}
          >
            {t("cancel")}
          </Button>
          <Button
            type="button"
            disabled={!canSend}
            onClick={() => send.mutate()}
          >
            {t("send")}
          </Button>
        </div>
      </FieldGroup>
    </SectionCard>
  );
}
