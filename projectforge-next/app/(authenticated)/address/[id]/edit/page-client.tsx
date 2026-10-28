"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { ADDRESS_PAGE } from "@/components/features/address/address.page";
import { useAddressEditOutcome } from "@/components/features/address/use-address-edit-outcome";

/** The form of a stored address, a sub route of its view. */
export function AddressEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/address/[id]/edit")?.id;
  const id = Number(raw);
  const outcome = useAddressEditOutcome(
    Number.isInteger(id) && id > 0 ? id : null
  );
  if (raw === undefined) return null;
  if (!Number.isInteger(id) || id <= 0) notFound();
  return (
    <PageShell>
      <EntityEditPage page={ADDRESS_PAGE} id={id} outcome={outcome} />
    </PageShell>
  );
}
