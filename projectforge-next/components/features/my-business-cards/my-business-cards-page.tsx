"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import {
  fetchMyBusinessCards,
  MY_BUSINESS_CARDS_QUERY_KEY,
} from "@/lib/rs/my-business-cards";
import { BusinessCardEditor } from "./business-card-editor";
import { BusinessCardTile } from "./business-card-tile";
import type { BusinessCard } from "./types";

/**
 * The digital business cards of the logged-in user: one QR code (vCard) per card, several cards possible (e.g.
 * with and without the mobile number). A new card starts from the address that is most likely the user's own one.
 */
export function MyBusinessCardsPage() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: MY_BUSINESS_CARDS_QUERY_KEY,
    queryFn: ({ signal }) => fetchMyBusinessCards(signal),
  });
  // The card being edited: a stored one, a new one (no id) or none.
  const [editing, setEditing] = useState<BusinessCard | null>(null);
  const data = query.data;
  const atLimit = !!data && data.cards.length >= data.maxCards;
  const done = async () => {
    setEditing(null);
    await queryClient.invalidateQueries({
      queryKey: MY_BUSINESS_CARDS_QUERY_KEY,
    });
  };

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.myAccount")}
        title={t("menu.myBusinessCards")}
      >
        {data && !editing && (
          <HintTooltip
            text={
              atLimit
                ? t("businessCard.limit", { arg0: data.maxCards })
                : undefined
            }
            openOnTap
          >
            <span>
              <Button
                variant="outline"
                disabled={atLimit}
                onClick={() =>
                  setEditing({ ...(data.suggestion ?? {}), id: null })
                }
              >
                <HugeiconsIcon icon={Add01Icon} />
                {t("businessCard.new")}
              </Button>
            </span>
          </HintTooltip>
        )}
      </PageTitleRow>
      {query.isPending && (
        <p className="px-4 text-sm text-muted-foreground">{t("loading")}</p>
      )}
      {query.isError && (
        <p className="px-4 text-sm text-destructive">
          {t("access.exception.noAccess")}
        </p>
      )}
      {data && editing && (
        <BusinessCardEditor
          initial={editing}
          suggestionAddressId={data.suggestion?.addressId ?? null}
          gatewayConfigured={data.gatewayConfigured}
          onDone={() => void done()}
        />
      )}
      {data && !editing && (
        <div className="flex flex-wrap gap-4 px-4">
          {data.cards.length === 0 && (
            <p className="text-sm text-muted-foreground">
              {t("businessCard.none")}
            </p>
          )}
          {data.cards.map((info) => (
            <BusinessCardTile
              key={info.card.id}
              info={info}
              onEdit={() => setEditing(info.card)}
            />
          ))}
        </div>
      )}
    </PageShell>
  );
}
