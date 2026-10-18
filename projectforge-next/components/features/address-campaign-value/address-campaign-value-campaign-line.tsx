"use client";

import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { AddressCampaignValueStatistics } from "./types";

/**
 * The selected campaign above the list (and above the selection of the mass update), as the React list showed
 * it in its alert box — or, while none is selected, that the list stays empty until one is picked in the
 * campaign filter. Beside it the comparison campaign, and that an inactive campaign can't be changed any more.
 */
export function AddressCampaignValueCampaignLine({
  statistics,
  isFetching,
}: {
  statistics: AddressCampaignValueStatistics | undefined;
  isFetching?: boolean;
}) {
  const t = useTranslations();
  if (!statistics) return null;
  const title = statistics.campaignTitle;
  return (
    <div
      className={cn(
        "border-b px-4 py-1.5 text-[13px]",
        isFetching && "opacity-60",
        title ? "bg-muted/40" : "bg-warning/10 text-warning"
      )}
    >
      {title ? (
        <>
          <span className="text-muted-foreground">
            {t(leafKeyOf("plugins.marketing.addressCampaign", t.has))}:
          </span>{" "}
          <span className="font-semibold text-brand-pink">{title}</span>
          {statistics.campaignActive === false && (
            <span className="ml-3 text-warning">
              {t("plugins.marketing.addressCampaign.inactiveReadOnly")}
            </span>
          )}
          {statistics.comparisonCampaignTitle && (
            <>
              <span className="mx-2 text-muted-foreground">·</span>
              <span className="text-muted-foreground">
                {t("plugins.marketing.addressCampaignValue.comparison")}:
              </span>{" "}
              <span className="font-semibold">
                {statistics.comparisonCampaignTitle}
              </span>
            </>
          )}
        </>
      ) : (
        t("plugins.marketing.addressCampaign.noCampaignSelected")
      )}
    </div>
  );
}
