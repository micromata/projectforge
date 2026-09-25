"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { fetchPhoneCallData } from "@/lib/rs/phone-call";
import { PhoneCallForm } from "./phone-call-form";
import type { PhoneCallParams } from "./types";

function positiveNumber(
  params: URLSearchParams,
  key: string
): number | undefined {
  const value = Number(params.get(key));
  return value > 0 ? value : undefined;
}

/** The Wicket page this one replaces, kept mounted as the "classic version" escape hatch. */
function legacyUrl(query: PhoneCallParams): string {
  const params = new URLSearchParams();
  if (query.addressId != null) params.set("addressId", String(query.addressId));
  if (query.number) params.set("number", query.number);
  if (query.callerPage) params.set("callerPage", query.callerPage);
  const s = params.toString();
  return s ? `wa/phoneCall?${s}` : "wa/phoneCall";
}

/**
 * The "Direct call" page ("Direktwahl Telefonanlage", `/next/phoneCall`), successor of Wicket's `wa/phoneCall`.
 * A standalone, non-entity form: it dials a number through the reused Sipgate telephone system.
 *
 * The deep-link `?addressId=&number=&callerPage=` seeds the call — the entry point is the phone-number link
 * on an address. The form is rendered only once the initial data (the prefilled number, the resolved address
 * and the user's phone / caller ids) has arrived, so it can seed its own state.
 */
export function PhoneCallPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const query: PhoneCallParams = {
    addressId: positiveNumber(params, "addressId"),
    number: params.get("number") ?? undefined,
    callerPage: params.get("callerPage") ?? undefined,
  };

  const data = useQuery({
    queryKey: ["phoneCall", query],
    queryFn: ({ signal }) => fetchPhoneCallData(query, signal),
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.phoneCall")}
        title={t("address.phoneCall.title")}
        legacyUrl={legacyUrl(query)}
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
        {data.data && <PhoneCallForm initial={data.data} />}
      </div>
    </PageShell>
  );
}
