"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Field, FieldLabel } from "@/components/ui/field";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { SuggestInput } from "@/components/shared/suggest-input";
import { SectionCard } from "@/components/shared/section-card";
import { toast } from "@/lib/toast";
import { RsError } from "@/lib/rs/client";
import { placeCall } from "@/lib/rs/phone-call";
import { PhoneCallAddressPanel } from "./phone-call-address-panel";
import { useAddressSelection } from "./use-address-selection";
import type { PhoneCallInitialData } from "./types";

/**
 * The number + "my phone" / "my caller id" form, seeded once from the server's [initial] data. Plain
 * `useState`, like the other standalone forms of this app (the SMS page, the login): the call itself is
 * the backend's (SipgateDirectCallService), so there is no entity and no client-side rule beyond "a number
 * and a phone are chosen". The resolved address (if any) is shown beside the form with its clickable numbers.
 */
export function PhoneCallForm({ initial }: { initial: PhoneCallInitialData }) {
  const t = useTranslations();
  const [phoneNumber, setPhoneNumber] = useState(initial.phoneNumber ?? "");
  const [myPhoneId, setMyPhoneId] = useState(
    initial.recentMyPhoneId ?? initial.myPhoneIds[0] ?? ""
  );
  const [myCallerId, setMyCallerId] = useState(
    initial.recentMyCallerId ?? initial.callerIds[0] ?? ""
  );
  const [confirmOpen, setConfirmOpen] = useState(false);
  const { suggest, onCommit, activeAddress } = useAddressSelection(
    initial.address ?? null,
    setPhoneNumber
  );

  const call = useMutation({
    mutationFn: () => placeCall({ phoneNumber, myPhoneId, myCallerId }),
    onSuccess: (result) =>
      result.success
        ? toast.success(result.message)
        : toast.error(result.message),
    onError: (error) =>
      toast.error(error instanceof RsError ? error.message : String(error)),
  });

  if (!initial.sipgateConfigured) {
    return (
      <SectionCard className="mx-auto w-full max-w-2xl">
        <p className="text-sm text-muted-foreground">
          {t("address.directCall.noPhoneDefined")}
        </p>
      </SectionCard>
    );
  }

  const canCall =
    phoneNumber.trim().length > 0 && myPhoneId.length > 0 && !call.isPending;

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-4 md:flex-row">
      <SectionCard className="min-w-0 flex-1">
        <div className="flex flex-col gap-4">
          <Field>
            <FieldLabel htmlFor="phone-call-number">
              {t("address.phoneCall.number.label")}
            </FieldLabel>
            <SuggestInput
              id="phone-call-number"
              value={phoneNumber}
              onChange={setPhoneNumber}
              suggest={suggest}
              onCommit={onCommit}
              queryKey={["phoneCall-ac"]}
              // 0 so focusing the empty field already offers the recently called numbers.
              minChars={0}
              autoComplete="tel"
              inputMode="tel"
              required
              autoFocus
            />
          </Field>
          <Field>
            <FieldLabel htmlFor="phone-call-my-phone">
              {t("address.myCurrentPhoneId")}
            </FieldLabel>
            <Select value={myPhoneId} onValueChange={setMyPhoneId}>
              <SelectTrigger id="phone-call-my-phone">
                <SelectValue
                  placeholder={t("user.personalPhoneIdentifiers.pleaseDefine")}
                />
              </SelectTrigger>
              <SelectContent>
                {initial.myPhoneIds.map((id) => (
                  <SelectItem key={id} value={id}>
                    {id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field>
            <FieldLabel htmlFor="phone-call-caller-id">
              {t("address.myCurrentCallerId")}
            </FieldLabel>
            <Select value={myCallerId} onValueChange={setMyCallerId}>
              <SelectTrigger id="phone-call-caller-id">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {initial.callerIds.map((id) => (
                  <SelectItem key={id} value={id}>
                    {id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
        </div>
        <div className="mt-4 flex items-center gap-3">
          <Button
            type="button"
            disabled={!canCall}
            onClick={() => setConfirmOpen(true)}
          >
            {t("address.directCall.call")}
          </Button>
        </div>
      </SectionCard>

      {activeAddress && (
        <PhoneCallAddressPanel
          address={activeAddress}
          onPick={(number) => {
            setPhoneNumber(number);
            setConfirmOpen(true);
          }}
        />
      )}

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={t("address.phoneCall.title")}
        description={t("address.directCall.confirm", { arg0: myPhoneId })}
        confirmLabel={t("address.directCall.call")}
        onConfirm={() => call.mutate()}
      />
    </div>
  );
}
