"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { ADDRESS_CAMPAIGN_VALUE_PAGE } from "@/components/features/address-campaign-value/address-campaign-value.page";

export function AddressCampaignValueEditPageClient() {
  const raw = useRouteParams<{ id: string }>(
    "/address-campaign-value/[id]"
  )?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  // "new" adds the value of an address without one (`?campaignId=&addressId=`, see the list's row click). A
  // synthetic (negative) id never reaches here: the list opens those as new.
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage
        page={ADDRESS_CAMPAIGN_VALUE_PAGE}
        id={isNew ? null : id}
      />
    </PageShell>
  );
}
