"use client";

import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import { Button } from "@/components/ui/button";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import type { AddressCampaignValueDetail } from "./types";

/**
 * Above the form, the campaign and the address the value is given for, as a business card — what the React
 * form showed in its two alert boxes. Read only: the address is changed in the address form, not here.
 *
 * Below it the value the address has in the comparison campaign of the list (e.g. last year's), with a
 * button adopting it; and, for an inactive campaign, that its values can't be changed any more.
 */
export function AddressCampaignValueEditIntro() {
  const t = useTranslations();
  const data = useEntityData<AddressCampaignValueDetail>();
  if (!data) return null;
  const inactive = data.addressCampaign?.active === false;
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
        {inactive && (
          <span className="ml-3 text-warning">
            {t("plugins.marketing.addressCampaign.inactiveReadOnly")}
          </span>
        )}
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
      {data.comparisonCampaignTitle && !inactive && (
        <ComparisonLine data={data} />
      )}
    </div>
  );
}

/**
 * The comparison campaign's value and comment of the address. "Adopt" takes the value (if this campaign
 * offers it) and appends the comment on a new line to the form's own — the same rule as the mass update
 * (`AddressCampaignValueMassUpdateRest.appendComment`).
 */
function ComparisonLine({ data }: { data: AddressCampaignValueDetail }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const value = data.comparisonValue ?? null;
  const comment = data.comparisonComment?.trim() || null;
  const known =
    value != null && (data.addressCampaign?.valuesArray ?? []).includes(value);
  const adopt = () => {
    if (known) form.setFieldValue("value" as never, value as never);
    if (comment) {
      const own = (
        (form.getFieldValue("comment" as never) as string | null) ?? ""
      ).trimEnd();
      if (!own.includes(comment)) {
        form.setFieldValue(
          "comment" as never,
          (own ? `${own}\n${comment}` : comment) as never
        );
      }
    }
  };
  return (
    <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
      <span className="text-muted-foreground">
        {data.comparisonCampaignTitle}:
      </span>
      <span className="font-semibold">{value ?? "—"}</span>
      {comment && (
        <span className="whitespace-pre-line text-muted-foreground">
          – {comment}
        </span>
      )}
      {(known || comment) && (
        <Button type="button" variant="outline" size="sm" onClick={adopt}>
          {t("plugins.marketing.addressCampaignValue.adopt")}
        </Button>
      )}
    </div>
  );
}
