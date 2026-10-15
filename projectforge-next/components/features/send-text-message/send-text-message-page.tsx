"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { fetchSendTextMessageData } from "@/lib/rs/send-text-message";
import { SendTextMessageForm } from "./send-text-message-form";
import type { SendTextMessageParams } from "./types";

function initialNumber(
  params: URLSearchParams,
  key: string
): number | undefined {
  const value = Number(params.get(key));
  return value > 0 ? value : undefined;
}

/**
 * The "Send text message" page ("SMS senden", `/next/sendTextMessage`), successor of Wicket's `wa/sendSms`.
 * A standalone, non-entity form: it sends a short message to a phone number through the reused SmsSender.
 *
 * The deep-link `?addressId=&phoneType=` (or `?number=`) seeds the receiver — the entry point is the SMS
 * icon next to an address's mobile number. The form itself is rendered only once the initial data (the
 * prefilled number, the initial message text and the max length) has arrived, so it can seed its own state.
 */
export function SendTextMessagePage() {
  const t = useTranslations();
  const params = useSearchParams();
  const query: SendTextMessageParams = {
    addressId: initialNumber(params, "addressId"),
    phoneType: params.get("phoneType") ?? undefined,
    number: params.get("number") ?? undefined,
  };

  const data = useQuery({
    queryKey: ["sendTextMessage", query],
    queryFn: ({ signal }) => fetchSendTextMessageData(query, signal),
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.sendSms")}
        title={t("address.sendSms.title")}
      />
      <div className="flex flex-col gap-4 px-4 pb-6 pt-2">
        {data.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {data.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {data.data && <SendTextMessageForm initial={data.data} />}
      </div>
    </PageShell>
  );
}
