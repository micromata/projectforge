"use client";

import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import { useEntityData } from "@/components/shared/form/form-context";
import type { AddressCampaignValueDetail } from "./types";

/**
 * Above the form, the campaign and the address the value is given for, as a business card — what the React
 * form showed in its two alert boxes. Read only: the address is changed in the address form, not here.
 */
export function AddressCampaignValueEditIntro() {
  const t = useTranslations();
  const data = useEntityData<AddressCampaignValueDetail>();
  if (!data) return null;
  const fullName = [data.firstName, data.name].filter(Boolean).join(" ");
  return (
    <div className="grid gap-2 text-sm">
      <div>
        <span className="text-muted-foreground">
          {t(leafKeyOf("plugins.marketing.addressCampaign", t.has))}:
        </span>{" "}
        <span className="font-semibold text-brand-pink">
          {data.addressCampaign?.title ?? "—"}
        </span>
      </div>
      <div className="rounded-md border bg-muted/40 px-4 py-3">
        {fullName && <div className="font-semibold">{fullName}</div>}
        {data.organization && <div>{data.organization}</div>}
        {data.formattedAddress && (
          <div className="whitespace-pre-line">{data.formattedAddress}</div>
        )}
        {data.email && <div>{data.email}</div>}
        {!fullName && !data.organization && !data.formattedAddress && (
          <div className="text-muted-foreground">
            {t(
              "plugins.marketing.addressCampaignValue.error.addressOrCampaignNotGiven"
            )}
          </div>
        )}
      </div>
    </div>
  );
}
