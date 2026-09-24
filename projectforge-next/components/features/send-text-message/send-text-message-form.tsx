"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { SuggestInput } from "@/components/shared/suggest-input";
import { SectionCard } from "@/components/shared/section-card";
import { toast } from "@/lib/toast";
import { RsError } from "@/lib/rs/client";
import { sendTextMessage, suggestReceivers } from "@/lib/rs/send-text-message";
import type { SendTextMessageInitialData } from "./types";

/**
 * The receiver + message form, seeded once from the server's [initial] data (the prefilled number, the
 * initial message text and the max length). Plain `useState`, like the other standalone forms of this app
 * (the login, the task wizard): two fields, no entity and no client-side rule beyond "a number is given" —
 * everything else is the backend's (SmsSender).
 */
export function SendTextMessageForm({
  initial,
}: {
  initial: SendTextMessageInitialData;
}) {
  const t = useTranslations();
  const [phoneNumber, setPhoneNumber] = useState(initial.phoneNumber ?? "");
  const [message, setMessage] = useState(initial.message);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const send = useMutation({
    mutationFn: () => sendTextMessage({ phoneNumber, message }),
    onSuccess: (result) =>
      result.success
        ? toast.success(result.message)
        : toast.error(result.message),
    onError: (error) =>
      toast.error(error instanceof RsError ? error.message : String(error)),
  });

  const charactersLeft = initial.maxMessageSize - message.length;
  const canSend = phoneNumber.trim().length > 0 && !send.isPending;

  const reset = () => {
    setPhoneNumber("");
    setMessage(initial.message);
  };

  return (
    <SectionCard className="mx-auto w-full max-w-2xl">
      <FieldGroup>
        <Field>
          <FieldLabel htmlFor="sms-phone-number">
            {t("address.sendSms.phoneNumber")}
          </FieldLabel>
          <SuggestInput
            id="sms-phone-number"
            value={phoneNumber}
            onChange={setPhoneNumber}
            suggest={suggestReceivers}
            queryKey={["sendTextMessage-ac"]}
            // 0 so focusing the empty field already offers the recently used numbers.
            minChars={0}
            // Declare it as a phone field so the OS AutoFill doesn't offer a verification code /
            // saved credential over this box (Safari/Chrome ignore a bare autoComplete="off").
            autoComplete="tel"
            inputMode="tel"
            required
            autoFocus
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="sms-message">
            {t("address.sendSms.message")}
          </FieldLabel>
          <Textarea
            id="sms-message"
            value={message}
            maxLength={initial.maxMessageSize}
            rows={4}
            onChange={(event) => setMessage(event.target.value)}
          />
          <FieldDescription>
            {charactersLeft} {t("charactersLeft")}
          </FieldDescription>
        </Field>
        <div className="flex items-center gap-3">
          <Button type="button" variant="outline" onClick={reset}>
            {t("reset")}
          </Button>
          <Button
            type="button"
            disabled={!canSend}
            onClick={() => setConfirmOpen(true)}
          >
            {t("send")}
          </Button>
        </div>
      </FieldGroup>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={t("address.sendSms.title")}
        description={t("address.sendSms.sendMessageQuestion")}
        confirmLabel={t("send")}
        onConfirm={() => send.mutate()}
      />
    </SectionCard>
  );
}
